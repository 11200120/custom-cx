/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.sagajib2bwebservices.v2.helper.OrderApprovalPermissionsHelper;
import de.hybris.platform.sagajib2bwebservices.validators.B2BPermissionWsDTOValidator;
import de.hybris.platform.b2bapprovalprocessfacades.company.B2BPermissionFacade;
import de.hybris.platform.b2bapprovalprocessfacades.company.data.B2BPermissionData;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BPermissionListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BPermissionWsDTO;
import de.hybris.platform.commerceservices.customer.DuplicateUidException;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.servicelayer.exceptions.ModelSavingException;
import de.hybris.platform.webservicescommons.cache.CacheControl;
import de.hybris.platform.webservicescommons.cache.CacheControlDirective;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
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
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orderApprovalPermissions")
@CacheControl(directive = CacheControlDirective.PRIVATE)
@ApiVersion("v2")
@Tag(name = "Order Approval Permissions")
public class OrderApprovalPermissionsController extends BaseController
{
	private static final Logger LOG = LoggerFactory.getLogger(OrderApprovalPermissionsController.class);

	private static final String OBJECT_NAME_PERMISSION = "permission";
	private static final String PERMISSION_NOT_FOUND_MESSAGE = "Order Approval permission with id [%s] was not found";
	private static final String MODEL_SAVING_ERROR_MESSAGE = "Model saving error.";

	@Resource(name = "b2bPermissionFacade")
	protected B2BPermissionFacade b2bPermissionFacade;

	@Resource(name = "b2BPermissionWsDTOValidator")
	protected B2BPermissionWsDTOValidator b2BPermissionWsDTOValidator;

	@Resource(name = "orderApprovalPermissionsHelper")
	private OrderApprovalPermissionsHelper permissionsHelper;

	@Secured(
	{ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(operationId = "getOrderApprovalPermissions", summary = "Retrieves the order approval permissions.", description = "Retrieves the list of order approval permissions given to a user. Such permissions allow the approval to exceed order, budget, or time threshold. The response may display the results across multiple pages, when applicable.")
	@ApiBaseSiteIdAndUserIdParam
	public B2BPermissionListWsDTO getOrderApprovalPermissions(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return permissionsHelper.getPermissions(currentPage, pageSize, sort, addPaginationField(fields));
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(value = "/{orderApprovalPermissionCode}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(operationId = "getOrderApprovalPermission", summary = "Retrieves the order approval permission.")
	@ApiBaseSiteIdAndUserIdParam
	public B2BPermissionWsDTO getOrderApprovalPermission(
			@Parameter(description = "Order approval permission code.", required = true) @PathVariable final String orderApprovalPermissionCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final B2BPermissionData permissionData = getPermissionDetails(orderApprovalPermissionCode);
		return getDataMapper().map(permissionData, B2BPermissionWsDTO.class, fields);
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PatchMapping(value = "/{orderApprovalPermissionCode}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "updateOrderApprovalPermission", summary = "Updates the order approval permission.")
	@ApiBaseSiteIdAndUserIdParam
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public B2BPermissionWsDTO updateOrderApprovalPermission(
			@Parameter(description = "Order approval permission code.", required = true) @PathVariable final String orderApprovalPermissionCode,
			@Parameter(description = "Order Approval Permission object.", required = true) @RequestBody final B2BPermissionWsDTO orderApprovalPermission,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		if (orderApprovalPermission.getActive() != null)
		{
			b2bPermissionFacade.enableDisablePermission(orderApprovalPermissionCode, orderApprovalPermission.getActive());
			orderApprovalPermission.setActive(null);
		}

		final B2BPermissionData permissionData = getPermissionDetails(orderApprovalPermissionCode);

		getDataMapper().map(orderApprovalPermission, permissionData, false);
		permissionData.setOriginalCode(orderApprovalPermissionCode);

		b2bPermissionFacade.updatePermissionDetails(permissionData);

		B2BPermissionWsDTO toBeValidatedPermission = getDataMapper().map(permissionData, B2BPermissionWsDTO.class);
		validate(toBeValidatedPermission, OBJECT_NAME_PERMISSION, b2BPermissionWsDTOValidator);

		return getDataMapper().map(permissionData, B2BPermissionWsDTO.class, fields);
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@ResponseStatus(value = HttpStatus.CREATED)
	@Operation(operationId = "createOrderApprovalPermission", summary = "Creates an order approval permission.", description = "Creates an order approval permission for a user. These permissions allow the approval to exceed order, budget, or time threshold in a workflow.")
	@ApiBaseSiteIdAndUserIdParam
	public B2BPermissionWsDTO createOrderApprovalPermission(
			@Parameter(description = "Order Approval Permission object.", required = true) @RequestBody final B2BPermissionWsDTO orderApprovalPermission,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields) throws DuplicateUidException
	{
		validate(orderApprovalPermission, OBJECT_NAME_PERMISSION, b2BPermissionWsDTOValidator);

		final B2BPermissionData permissionData = getDataMapper().map(orderApprovalPermission, B2BPermissionData.class);

		b2bPermissionFacade.addPermission(permissionData);

		final B2BPermissionData savedPermissionData = b2bPermissionFacade.getPermissionDetails(permissionData.getCode());
		return getDataMapper().map(savedPermissionData, B2BPermissionWsDTO.class, fields);
	}

	protected B2BPermissionData getPermissionDetails(final String orderApprovalPermissionCode)
	{
		try
		{
			return b2bPermissionFacade.getPermissionDetails(orderApprovalPermissionCode);
		}
		catch (IllegalArgumentException e)
		{
			LOG.warn(String.format(PERMISSION_NOT_FOUND_MESSAGE, sanitize(orderApprovalPermissionCode)), e);
			throw new NotFoundException(String.format(PERMISSION_NOT_FOUND_MESSAGE, orderApprovalPermissionCode));
		}
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ResponseBody
	@ExceptionHandler(
	{ ModelSavingException.class })
	public ErrorListWsDTO handleModelSavingException(final Exception ex)
	{
		LOG.debug("ModelSavingException", ex);
		return handleErrorInternal(ModelSavingException.class.getSimpleName(), MODEL_SAVING_ERROR_MESSAGE);
	}
}
