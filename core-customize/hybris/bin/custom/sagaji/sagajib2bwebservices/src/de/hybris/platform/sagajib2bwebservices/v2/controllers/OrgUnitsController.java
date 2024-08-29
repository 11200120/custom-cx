/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.sagajib2bwebservices.strategy.OrgUnitUserRoleManagementStrategy;
import de.hybris.platform.sagajib2bwebservices.strategy.OrgUnitUsersDisplayStrategy;
import de.hybris.platform.sagajib2bwebservices.strategy.UserRoleManagementStrategy;
import de.hybris.platform.sagajib2bwebservices.v2.helper.OrgUnitsHelper;
import de.hybris.platform.b2bcommercefacades.company.B2BUnitFacade;
import de.hybris.platform.b2bcommercefacades.company.data.B2BSelectionData;
import de.hybris.platform.b2bcommercefacades.company.data.B2BUnitData;
import de.hybris.platform.b2bcommercefacades.company.data.B2BUnitNodeData;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BApprovalProcessListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BSelectionDataWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BUnitNodeListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BUnitNodeWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BUnitWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUnitUserListWsDTO;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressListWsDTO;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commercewebservicescommons.dto.user.AddressWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.webservicescommons.errors.exceptions.AlreadyExistsException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import java.util.Map;
import java.util.List;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


@RestController
@RequestMapping(value = "/{baseSiteId}/users/{userId}")
@ApiVersion("v2")
@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
@Tag(name = "Organizational Unit Management")
public class OrgUnitsController extends BaseController
{
	private static final String OBJECT_NAME_ORG_UNIT = "OrgUnit";

	private static final String UNIT_NOT_FOUND_MESSAGE = "Organizational unit with id [%s] was not found";
	private static final String ROLE_NOT_FOUND_MESSAGE = "Supplied parameter [%s] is not valid";
	private static final String UNIT_ALREADY_EXISTS_MESSAGE = "Organizational unit with uid [%s] already exists";
	private static final String ADDRESS_NOT_FOUND_MESSAGE = "Address with id [%s] is not found";

	@Resource(name = "b2bUnitFacade")
	protected B2BUnitFacade b2bUnitFacade;

	@Resource(name = "wsUserFacade")
	private UserFacade wsUserFacade;

	@Resource(name = "b2BUnitWsDTOValidator")
	protected Validator b2BUnitWsDTOValidator;

	@Resource(name = "addressDTOValidator")
	private Validator addressDTOValidator;

	@Resource(name = "orgUnitsHelper")
	private OrgUnitsHelper orgUnitsHelper;

	@Resource(name = "userRoleManagementStrategyMap")
	protected Map<String, UserRoleManagementStrategy> userRoleManagementStrategyMap;

	@Resource(name = "orgUnitUsersDisplayStrategyMap")
	protected Map<String, OrgUnitUsersDisplayStrategy> orgUnitUsersDisplayStrategyMap;

	@Resource(name = "orgUnitUserRoleManagementStrategyMap")
	protected Map<String, OrgUnitUserRoleManagementStrategy> orgUnitUserRoleManagementStrategyMap;

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getOrgUnit", summary = "Retrieves the organizational unit.", description = "Retrieves the organizational unit based on the specified identifier.")
	@GetMapping(value = "/orgUnits/{orgUnitId}", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public B2BUnitWsDTO getOrgUnit(
			@Parameter(description = "Organizational unit identifier.", example = "Rustic", required = true) @PathVariable final String orgUnitId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final B2BUnitData unitData = getUnitForUid(orgUnitId);
		return getDataMapper().map(unitData, B2BUnitWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orgUnits", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ResponseStatus(value = HttpStatus.CREATED)
	@Operation(operationId = "createOrgUnit", summary = "Creates a new organizational unit.", description = "Creates a new organizational unit. For example, the Rustic Organization used the word 'Rustic' to refer to their organizational unit.")
	@ApiBaseSiteIdAndUserIdParam
	public B2BUnitWsDTO createUnit(
			@Parameter(description = "Organizational unit object.", required = true) @RequestBody final B2BUnitWsDTO orgUnit,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		validate(orgUnit, OBJECT_NAME_ORG_UNIT, b2BUnitWsDTOValidator);

		if (b2bUnitFacade.getUnitForUid(orgUnit.getUid()) != null)
		{
			throw new AlreadyExistsException(String.format(UNIT_ALREADY_EXISTS_MESSAGE, orgUnit.getUid()));
		}

		final B2BUnitData unitData = getDataMapper().map(orgUnit, B2BUnitData.class);
		b2bUnitFacade.updateOrCreateBusinessUnit(unitData.getUid(), unitData);

		final B2BUnitData createdUnitData = b2bUnitFacade.getUnitForUid(unitData.getUid());
		return getDataMapper().map(createdUnitData, B2BUnitWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getAvailableParentUnits", summary = "Retrieves the available parent units.", description = "Retrieves a list of units which can be parents of current units.")
	@GetMapping(value = "/orgUnits/{orgUnitId}/availableParents", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public B2BUnitNodeListWsDTO getAvailableParentUnits(
			@Parameter(description = "Organizational unit identifier.", example = "Rustic", required = true) @PathVariable final String orgUnitId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		return orgUnitsHelper.getAvailableParentUnits(orgUnitId, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "updateOrgUnit", summary = "Updates the organizational unit.")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PatchMapping(value = "/orgUnits/{orgUnitId}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public void updateOrgUnit(
			@Parameter(description = "Organizational unit identifier.", example = "Rustic", required = true) @PathVariable final String orgUnitId,
			@Parameter(description = "Organizational unit object.", required = true) @RequestBody final B2BUnitWsDTO orgUnit)
	{
		// active status changes are handled later because b2bUnitFacade.updateOrCreateBusinessUnit overwrites it to true
		final Boolean isActive = orgUnit.getActive();
		orgUnit.setActive(null);

		final B2BUnitData unitData = getUnitForUid(orgUnitId);
		getDataMapper().map(orgUnit, unitData, false);

		final B2BUnitWsDTO unitToBeValidated = getDataMapper().map(unitData, B2BUnitWsDTO.class);
		validate(unitToBeValidated, OBJECT_NAME_ORG_UNIT, b2BUnitWsDTOValidator);

		b2bUnitFacade.updateOrCreateBusinessUnit(orgUnitId, unitData);

		if (isActive != null)
		{
			if (isActive)
			{
				b2bUnitFacade.enableUnit(unitData.getUid());
			}
			else
			{
				b2bUnitFacade.disableUnit(unitData.getUid());
			}
		}
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orgUnits/{orgUnitId}/addresses", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ResponseStatus(value = HttpStatus.CREATED)
	@Operation(operationId = "createOrgUnitAddress", summary = "Creates an address for the organizational unit.", description = "Creates a new address for the organizational unit.")
	@ApiBaseSiteIdAndUserIdParam
	public AddressWsDTO createOrgUnitAddress(
			@Parameter(description = "Address object.", required = true) @RequestBody final AddressWsDTO address,
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable final String orgUnitId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		validate(address, OBJECT_NAME_ORG_UNIT, addressDTOValidator);

		final AddressData addressData = getDataMapper().map(address, AddressData.class);
		addressData.setBillingAddress(false);
		addressData.setShippingAddress(true);

		b2bUnitFacade.addAddressToUnit(addressData, orgUnitId);
		return getDataMapper().map(addressData, AddressWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(value = "/orgUnits/{orgUnitId}/addresses", produces = MediaType.APPLICATION_JSON)
	@ResponseStatus(value = HttpStatus.OK)
	@Operation(operationId = "getOrgUnitAddresses", summary = "Retrieves the addresses of an organizational unit.", description = "Retrieves all the addresses of an organizational unit.")
	@ApiBaseSiteIdAndUserIdParam
	public AddressListWsDTO getOrgUnitAddresses(
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable final String orgUnitId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final B2BUnitData unitData = getUnitForUid(orgUnitId);
		final B2BUnitWsDTO unitDataWsDTO = getDataMapper().map(unitData, B2BUnitWsDTO.class, fields);
		final List<AddressWsDTO> addressList = unitDataWsDTO.getAddresses();
		final AddressListWsDTO addressDataList = new AddressListWsDTO();
		addressDataList.setAddresses(addressList);
		return getDataMapper().map(addressDataList, AddressListWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PatchMapping(value = "/orgUnits/{orgUnitId}/addresses/{addressId}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Operation(operationId = "updateOrgUnitAddress", summary = "Updates the organizational unit address.")
	@ApiBaseSiteIdAndUserIdParam
	public void updateOrgUnitAddress(@Parameter(description = "Address object.", required = true) @RequestBody final AddressWsDTO address,
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable final String orgUnitId,
			@Parameter(description = "Address identifier.", example = "8796093349911", required = true) @PathVariable final String addressId)
	{
		final AddressData addressData = getAddressData(addressId, orgUnitId);

		getDataMapper().map(address, addressData, false);
		addressData.setId(addressId);
		addressData.setBillingAddress(false);
		addressData.setShippingAddress(true);

		final AddressWsDTO addressToBeValidated = getDataMapper().map(addressData, AddressWsDTO.class);
		validate(addressToBeValidated, OBJECT_NAME_ORG_UNIT, addressDTOValidator);

		b2bUnitFacade.editAddressOfUnit(addressData, orgUnitId);
	}


	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "removeOrgUnitAddress", summary = "Deletes the organizational unit address.")
	@ResponseStatus(HttpStatus.OK)
	@DeleteMapping(value = "/orgUnits/{orgUnitId}/addresses/{addressId}", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public void removeOrgUnitAddress(
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable final String orgUnitId,
			@Parameter(description = "Address identifier.", example = "8796093349911", required = true) @PathVariable final String addressId)
	{
		b2bUnitFacade.removeAddressFromUnit(orgUnitId, addressId);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(value = "/orgUnits/{orgUnitId}/availableUsers/{roleId}", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "getOrgUnitUsers", summary = "Retrieves the users who belong to the organizational unit.", description = "Retrieves the users who belong to the organizational unit and can be assigned to a specific role. The 'selected' property of users who are already assigned to the role is true.")
	@ApiBaseSiteIdAndUserIdParam
	public OrgUnitUserListWsDTO getOrgUnitUsers(
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable final String orgUnitId,
			@Parameter(description = "Role that is returned. Example roles: b2bapprovergroup, b2badmingroup, b2bmanagergroup, or b2bcustomergroup.", required = true) @PathVariable final String roleId,
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.", example = "20") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the display search results.", example = "name") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{

		final OrgUnitUsersDisplayStrategy orgUnitUsersDisplayStrategy = getOrgUnitUsersDisplayStrategy(roleId);

		return orgUnitsHelper.convertPagedUsersForUnit(
				orgUnitUsersDisplayStrategy.getPagedUsersForUnit(currentPage, pageSize, sort, orgUnitId), fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orgCustomers/{orgCustomerId}/roles", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "doAddRoleToOrgCustomer", summary = "Creates a role for an organizational customer.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseStatus(value = HttpStatus.CREATED)
	@ResponseBody
	public B2BSelectionDataWsDTO addRoleToOrgCustomer(
			@Parameter(description = "Organizational customer to which the role will be added.", example = "ceff469f-25aa-4a10-99e3-da31245204e7", required = true) @PathVariable final String orgCustomerId,
			@Parameter(description = "Role that is added to the user.", example = "b2bapprovergroup", required = true) @RequestParam final String roleId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final UserRoleManagementStrategy userRoleManagementStrategy = getUserRoleManagementStrategy(roleId);
		final B2BSelectionData selectionData = userRoleManagementStrategy.addRoleToUser(wsUserFacade.getUserUID(orgCustomerId));
		return getDataMapper().map(selectionData, B2BSelectionDataWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@DeleteMapping(value = "/orgCustomers/{orgCustomerId}/roles/{roleId}", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "removeRoleFromOrgCustomer", summary = "Deletes the role from the organizational customer.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseBody
	public B2BSelectionDataWsDTO removeRoleFromOrgCustomer(
		@Parameter(description = "Organizational customer from which the role will be removed.", example = "ceff469f-25aa-4a10-99e3-da31245204e7", required = true) @PathVariable final String orgCustomerId,
			@Parameter(description = "Role that is removed from the user. Example roles: b2badmingroup, b2bmanagergroup, or b2bcustomergroup.", required = true) @PathVariable final String roleId,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final UserRoleManagementStrategy userRoleManagementStrategy = getUserRoleManagementStrategy(roleId);
		final B2BSelectionData selectionData = userRoleManagementStrategy
				.removeRoleFromUser(wsUserFacade.getUserUID(orgCustomerId));
		return getDataMapper().map(selectionData, B2BSelectionDataWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orgUnits/{orgUnitId}/orgCustomers/{orgCustomerId}/roles", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "doAddOrgUnitRoleToOrgCustomer", summary = "Adds an organizational unit dependent role to a specific organizational customer.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseStatus(value = HttpStatus.CREATED)
	public void addOrgUnitRoleToOrgCustomer(
			@Parameter(description = "Organizational unit identifier.", example = "Rustic", required = true) @PathVariable final String orgUnitId,
			@Parameter(description = "Organizational customer to which the role will be added.", example = "ceff469f-25aa-4a10-99e3-da31245204e7", required = true) @PathVariable final String orgCustomerId,
			@Parameter(description = "Role that is added to the user. Example role: b2bapprovergroup.", required = true) @RequestParam final String roleId)
	{
		final OrgUnitUserRoleManagementStrategy orgUnitUserRoleManagementStrategy = getOrgUnitUserRoleManagementStrategy(roleId);
		orgUnitUserRoleManagementStrategy.addRoleToUser(orgUnitId, wsUserFacade.getUserUID(orgCustomerId));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@DeleteMapping(value = "/orgUnits/{orgUnitId}/orgCustomers/{orgCustomerId}/roles/{roleId}", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "removeOrgUnitRoleFromOrgCustomer", summary = "Deletes an organizational, unit-dependent role from an organizational customer.")
	@ApiBaseSiteIdAndUserIdParam
	public void removeOrgUnitRoleFromOrgCustomer(
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable final String orgUnitId,
			@Parameter(description = "Organizational customer from which the role will be removed.", example = "ceff469f-25aa-4a10-99e3-da31245204e7", required = true) @PathVariable final String orgCustomerId,
			@Parameter(description = "Role that is removed from the user. Example roles: b2badmingroup, b2bmanagergroup, or b2bcustomergroup.", required = true) @PathVariable final String roleId)
	{
		final OrgUnitUserRoleManagementStrategy orgUnitUserRoleManagementStrategy = getOrgUnitUserRoleManagementStrategy(roleId);
		orgUnitUserRoleManagementStrategy.removeRoleFromUser(orgUnitId, wsUserFacade.getUserUID(orgCustomerId));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getOrgUnitsAvailableApprovalProcesses", summary = "Retrieves the available business approval processes.")
	@GetMapping(value = "/orgUnitsAvailableApprovalProcesses", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public B2BApprovalProcessListWsDTO getOrgUnitsAvailableApprovalProcesses(
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		return orgUnitsHelper.getApprovalProcesses();
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getOrgUnitsRootNodeTree", summary = "Retrieves the root organizational unit node.", description = "Retrieves the root organizational unit node and the child nodes associated with it.")
	@ResponseBody
	@GetMapping(value = "/orgUnitsRootNodeTree", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public B2BUnitNodeWsDTO getOrgUnitsRootNodeTree(
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final B2BUnitNodeData unitNodeData = b2bUnitFacade.getParentUnitNode();
		return getDataMapper().map(unitNodeData, B2BUnitNodeWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getAvailableOrgUnitNodes", summary = "Retrieves the available organizational unit nodes.")
	@ResponseBody
	@GetMapping(value = "/availableOrgUnitNodes", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public B2BUnitNodeListWsDTO getBranchNodes(
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		return orgUnitsHelper.getAvailableOrgUnitNodes(fields);
	}

	protected UserRoleManagementStrategy getUserRoleManagementStrategy(final String roleId)
	{
		final UserRoleManagementStrategy userRoleManagementStrategy = userRoleManagementStrategyMap.get(roleId);
		if (userRoleManagementStrategy == null)
		{
			throw new RequestParameterException(String.format(ROLE_NOT_FOUND_MESSAGE, sanitize(roleId)));
		}

		return userRoleManagementStrategy;
	}

	protected OrgUnitUserRoleManagementStrategy getOrgUnitUserRoleManagementStrategy(final String roleId)
	{
		final OrgUnitUserRoleManagementStrategy orgUnitUserRoleManagementStrategy = orgUnitUserRoleManagementStrategyMap
				.get(roleId);
		if (orgUnitUserRoleManagementStrategy == null)
		{
			throw new RequestParameterException(String.format(ROLE_NOT_FOUND_MESSAGE, sanitize(roleId)));
		}

		return orgUnitUserRoleManagementStrategy;
	}

	protected OrgUnitUsersDisplayStrategy getOrgUnitUsersDisplayStrategy(final String roleId)
	{
		final OrgUnitUsersDisplayStrategy orgUnitUsersDisplayStrategy = orgUnitUsersDisplayStrategyMap.get(roleId);
		if (orgUnitUsersDisplayStrategy == null)
		{
			throw new RequestParameterException(String.format(ROLE_NOT_FOUND_MESSAGE, sanitize(roleId)));
		}

		return orgUnitUsersDisplayStrategy;
	}

	protected B2BUnitData getUnitForUid(final String orgUnitId)
	{
		final B2BUnitData unitData = b2bUnitFacade.getUnitForUid(orgUnitId);
		if (unitData == null)
		{
			throw new NotFoundException(String.format(UNIT_NOT_FOUND_MESSAGE, sanitize(orgUnitId)));
		}
		return unitData;
	}

	protected AddressData getAddressData(final String addressId, final String orgUnitId)
	{
		final B2BUnitData unit = b2bUnitFacade.getUnitForUid(orgUnitId);

		final AddressData addressData = unit.getAddresses().stream()
				.filter(address -> StringUtils.equals(address.getId(), addressId)).findFirst()
				.orElseThrow(() -> new NotFoundException(String.format(ADDRESS_NOT_FOUND_MESSAGE, addressId)));

		return addressData;
	}
}
