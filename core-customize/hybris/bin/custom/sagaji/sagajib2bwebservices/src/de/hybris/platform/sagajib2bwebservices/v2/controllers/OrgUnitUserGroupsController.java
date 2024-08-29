/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.b2b.model.B2BUserGroupModel;
import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.sagajib2bwebservices.v2.helper.OrgUnitUserGroupsHelper;
import de.hybris.platform.b2bapprovalprocessfacades.company.B2BPermissionFacade;
import de.hybris.platform.b2bcommercefacades.company.B2BUserGroupFacade;
import de.hybris.platform.b2bcommercefacades.company.data.B2BSelectionData;
import de.hybris.platform.b2bcommercefacades.company.data.B2BUserGroupData;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BPermissionListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BSelectionDataWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUnitUserGroupListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUnitUserGroupWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUnitUserListWsDTO;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.AlreadyExistsException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}")
@ApiVersion("v2")
@Tag(name = "Organizational Unit User Groups")
public class OrgUnitUserGroupsController extends BaseController
{
	private static final Logger LOG = LoggerFactory.getLogger(OrgUnitUserGroupsController.class);

	private static final String USERGROUP_NOT_FOUND_ERROR_MESSAGE = "Member Permission not found";
	private static final String USERGROUP_ALREADY_EXISTS_ERROR_MESSAGE = "Member Permission with the same id already exists";
	private static final String OBJECT_NAME_ORG_USERGROUP = "orgUserGroup";

	protected static final String ILLEGAL_ARGUMENT_ERROR_MESSAGE = "Illegal argument error.";

	@Resource(name = "orgUnitUserGroupsHelper")
	protected OrgUnitUserGroupsHelper orgUnitUserGroupsHelper;

	@Resource(name = "b2bUserGroupFacade")
	protected B2BUserGroupFacade b2bUserGroupFacade;

	@Resource(name = "b2bPermissionFacade")
	protected B2BPermissionFacade b2bPermissionFacade;

	@Resource(name = "orgUnitUserGroupWsDTOValidator")
	private Validator orgUnitUserGroupWsDTOValidator;

	@Resource(name = "wsUserFacade")
	private UserFacade wsUserFacade;

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getOrgUnitUserGroups", summary = "Retrieves the list of user groups in an organizational unit.", description = "Retrieves the details of the user group that is accessible by the base site.")
	@ResponseBody
	@GetMapping(value = "/orgUnitUserGroups", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public OrgUnitUserGroupListWsDTO getOrgUnitUserGroups(
			@Parameter(description = "Current result page. Default value is 0.", required = false) @RequestParam(value = "currentPage", defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.", example = "20", required = false) @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.", example = "uid", required = false) @RequestParam(value = "sort", defaultValue = B2BUserGroupModel.UID) final String sort,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		return orgUnitUserGroupsHelper.searchUserGroups(currentPage, pageSize, sort, addPaginationField(fields));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getOrgUnitUserGroup", summary = "Retrieves the details of the organizational unit user group.", description = "Retrieves the details of the user group that is accessible by the base site.")
	@ResponseBody
	@GetMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public OrgUnitUserGroupWsDTO getOrgUnitUserGroup(
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final B2BUserGroupData userGroupData = b2bUserGroupFacade.getB2BUserGroup(orgUnitUserGroupId);

		if (userGroupData == null)
		{
			throw new NotFoundException(USERGROUP_NOT_FOUND_ERROR_MESSAGE);
		}

		return getDataMapper().map(userGroupData, OrgUnitUserGroupWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "updateOrgUnitUserGroup", summary = "Updates the organizational unit user group.")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@PatchMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public void updateOrgUnitUserGroup(
			@Parameter(description = "Organizational unit user group object.", required = true) @RequestBody final OrgUnitUserGroupWsDTO orgUnitUserGroup,
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		final B2BUserGroupData userGroupData = b2bUserGroupFacade.getB2BUserGroup(orgUnitUserGroupId);
		if (userGroupData == null)
		{
			throw new NotFoundException(USERGROUP_NOT_FOUND_ERROR_MESSAGE);
		}

		getDataMapper().map(orgUnitUserGroup, userGroupData, false);

		final OrgUnitUserGroupWsDTO orgUnitUserGroupToBeValidated = getDataMapper().map(userGroupData, OrgUnitUserGroupWsDTO.class);
		if (!StringUtils.equals(orgUnitUserGroupId, orgUnitUserGroupToBeValidated.getUid()) && !isUserGroupIdUnique(
				orgUnitUserGroupToBeValidated.getUid()))
		{
			throw new AlreadyExistsException(USERGROUP_ALREADY_EXISTS_ERROR_MESSAGE);
		}
		validate(orgUnitUserGroupToBeValidated, OBJECT_NAME_ORG_USERGROUP, orgUnitUserGroupWsDTOValidator);

		b2bUserGroupFacade.updateUserGroup(orgUnitUserGroupId, userGroupData);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "createOrgUnitUserGroup", summary = "Creates a new user group in an organizational unit.", description = "Creates a new organizational unit user group.")
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(value = "/orgUnitUserGroups", produces = MediaType.APPLICATION_JSON, consumes = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public OrgUnitUserGroupWsDTO createOrgUnitUserGroup(
			@Parameter(description = "Organizational unit user group object.", required = true) @RequestBody final OrgUnitUserGroupWsDTO orgUnitUserGroup,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		validate(orgUnitUserGroup, OBJECT_NAME_ORG_USERGROUP, orgUnitUserGroupWsDTOValidator);

		if (!isUserGroupIdUnique(orgUnitUserGroup.getUid()))
		{
			throw new AlreadyExistsException(USERGROUP_ALREADY_EXISTS_ERROR_MESSAGE);
		}

		final B2BUserGroupData userGroupData = getDataMapper().map(orgUnitUserGroup, B2BUserGroupData.class);

		b2bUserGroupFacade.updateUserGroup(orgUnitUserGroup.getUid(), userGroupData);

		final B2BUserGroupData createdUserGroupData = b2bUserGroupFacade.getB2BUserGroup(orgUnitUserGroup.getUid());

		return getDataMapper().map(createdUserGroupData, OrgUnitUserGroupWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "removeOrgUnitUserGroup", summary = "Deletes the organizational unit user group.")
	@ResponseStatus(HttpStatus.OK)
	@DeleteMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public void removeOrgUnitUserGroup(
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		b2bUserGroupFacade.removeUserGroup(orgUnitUserGroupId);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getOrderApprovalPermissionsForOrgUnitUserGroup", summary = "Retrieves the order approval permissions.", description = "Retrieves the order approval permissions that can belong to the organizational unit user group. The 'selected' property of permissions that already belong to the user group is true.")
	@ResponseBody
	@GetMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/availableOrderApprovalPermissions", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public B2BPermissionListWsDTO getOrderApprovalPermissionsForOrgUnitUserGroup(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(value = "currentPage", defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.", example = "20") @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.", example = "name") @RequestParam(value = "sort", defaultValue = UserModel.NAME) final String sort,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields,
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		return orgUnitUserGroupsHelper
				.searchPermissionsForOrgUnitUserGroup(orgUnitUserGroupId, currentPage, pageSize, sort, addPaginationField(fields));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/orderApprovalPermissions", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "doAddOrderApprovalPermissionToOrgUnitUserGroup", summary = "Creates an order approval permission to a user group in the organizational unit.", description = "Adds an order approval permission to a specific organizational unit user group.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseStatus(value = HttpStatus.CREATED)
	@ResponseBody
	public B2BSelectionDataWsDTO addOrderApprovalPermissionToOrgUnitUserGroup(
			@Parameter(description = "Organizational unit user group to which the order approval permission will be added.", example = "b2bApprovers", required = true) @PathVariable final String orgUnitUserGroupId,
			@Parameter(description = "Order approval permission that will be added to the organizational unit user group.", example = "OrderLessThan3000", required = true) @RequestParam final String orderApprovalPermissionCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final B2BSelectionData selectionData = b2bPermissionFacade.addPermissionToUserGroup(orgUnitUserGroupId,
				orderApprovalPermissionCode);
		return getDataMapper().map(selectionData, B2BSelectionDataWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@DeleteMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/orderApprovalPermissions/{orderApprovalPermissionCode}", produces = MediaType.APPLICATION_JSON)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeOrderApprovalPermissionFromOrgUnitUserGroup", summary = "Deletes the order approval permission from the organizational unit user group.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseBody
	public B2BSelectionDataWsDTO removeOrderApprovalPermissionFromOrgUnitUserGroup(
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bOrderApprovers", required = true) @PathVariable final String orgUnitUserGroupId,
			@Parameter(description = "Order approval permission that will be removed from the organizational unit user group.", example = "approveLessThan100", required = true) @PathVariable final String orderApprovalPermissionCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final B2BSelectionData selectionData = b2bPermissionFacade.removePermissionFromUserGroup(orgUnitUserGroupId,
				orderApprovalPermissionCode);
		return getDataMapper().map(selectionData, B2BSelectionDataWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@Operation(operationId = "getAvailableOrgCustomersForUserGroup", summary = "Retrieves the organizational customers.", description = "Retrieves the organizational customers who can belong to the specified organizational unit user group. The 'selected' property of customers who already belong to the user group is true.")
	@ResponseBody
	@GetMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/availableOrgCustomers", produces = MediaType.APPLICATION_JSON)
	@ApiBaseSiteIdAndUserIdParam
	public OrgUnitUserListWsDTO getAvailableOrgCustomersForUserGroup(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(value = "currentPage", defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.", example = "20") @RequestParam(value = "pageSize", defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.", example = "name") @RequestParam(value = "sort", defaultValue = UserModel.NAME) final String sort,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields,
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		return orgUnitUserGroupsHelper
				.searchOrgCustomersForUserGroup(orgUnitUserGroupId, currentPage, pageSize, sort, addPaginationField(fields));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/members", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "doAddOrgCustomerToOrgUnitUserGroupMembers", summary = "Adds an organizational customer as a member of an organizational unit user group.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseStatus(value = HttpStatus.CREATED)
	public void addOrgCustomerToOrgUnitUserGroupMembers(
			@Parameter(description = "Organizational customer that will be added to the organizational unit user group.", example = "ceff469f-25aa-4a10-99e3-da31245204e7", required = true) @RequestParam final String orgCustomerId,
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		b2bUserGroupFacade.addMemberToUserGroup(orgUnitUserGroupId, wsUserFacade.getUserUID(orgCustomerId));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@DeleteMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/members/{orgCustomerId:.+}", produces = MediaType.APPLICATION_JSON)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeOrgCustomerFromOrgUnitUserGroupMembers", summary = "Deletes an organizational customer from the organizational unit group members.")
	@ApiBaseSiteIdAndUserIdParam
	public void removeOrgCustomerFromOrgUnitUserGroupMembers(
			@Parameter(description = "Organizational customer from which the role will be removed.",  example = "ceff469f-25aa-4a10-99e3-da31245204e7", required = true) @PathVariable final String orgCustomerId,
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		b2bUserGroupFacade.removeMemberFromUserGroup(orgUnitUserGroupId, wsUserFacade.getUserUID(orgCustomerId));
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@DeleteMapping(value = "/orgUnitUserGroups/{orgUnitUserGroupId}/members", produces = MediaType.APPLICATION_JSON)
	@ResponseStatus(HttpStatus.OK)
	@Operation(operationId = "removeOrgUnitUserGroupMembers", summary = "Deletes all organizational customers as user group members in the organizational unit.", description = "Deletes all organizational customers as user group members in the organizational unit, which deactivates the user group until a new member is added.")
	@ApiBaseSiteIdAndUserIdParam
	public void removeOrgUnitUserGroupMembers(
			@Parameter(description = "Organizational unit user group identifier.", example = "b2bapprovergroup", required = true) @PathVariable final String orgUnitUserGroupId)
	{
		b2bUserGroupFacade.disableUserGroup(orgUnitUserGroupId);
	}

	protected boolean isUserGroupIdUnique(String orgUnitUserGroupId)
	{
		return b2bUserGroupFacade.getUserGroupDataForUid(orgUnitUserGroupId) == null;
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ResponseBody
	@ExceptionHandler({ ModelSavingException.class, NullPointerException.class })
	public ErrorListWsDTO handleIllegalArgumentException(final Exception ex)
	{
		LOG.debug("IllegalArgumentException", ex);
		return handleErrorInternal(IllegalArgumentException.class.getSimpleName(), ILLEGAL_ARGUMENT_ERROR_MESSAGE);
	}
}

