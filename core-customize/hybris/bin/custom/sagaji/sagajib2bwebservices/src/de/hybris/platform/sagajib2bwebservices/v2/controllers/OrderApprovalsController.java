/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.sagajib2bwebservices.v2.helper.OrderApprovalsHelper;
import de.hybris.platform.b2bacceleratorfacades.exception.PrincipalAssignedValidationException;
import de.hybris.platform.b2bacceleratorfacades.order.B2BOrderFacade;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BOrderApprovalData;
import de.hybris.platform.b2bwebservicescommons.dto.order.OrderApprovalDecisionWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.OrderApprovalListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.OrderApprovalWsDTO;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.workflow.exceptions.WorkflowActionDecideException;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orderapprovals")
@ApiVersion("v2")
@Tag(name = "Order Approvals")
public class OrderApprovalsController extends BaseController
{
	protected static final String DEFAULT_PAGE_SIZE = "20";
	protected static final String DEFAULT_CURRENT_PAGE = "0";

	protected static final String RESOURCE_NOT_FOUND_ERROR_MESSAGE = "Requested resource cannot be found.";
	protected static final String ILLEGAL_ARGUMENT_ERROR_MESSAGE = "Illegal argument error.";
	protected static final String WORKFLOW_ACTION_DECIDE_ERROR_MESSAGE = "An error occurred during the execution of the approval workflow.";

	private static final Logger LOG = Logger.getLogger(OrderApprovalsController.class);

	@Resource(name = "b2bOrderFacade")
	private B2BOrderFacade orderFacade;

	@Resource(name = "orderApprovalsHelper")
	protected OrderApprovalsHelper orderApprovalsHelper;

	@Resource(name = "orderApprovalDecisionValidator")
	private Validator orderApprovalDecisionValidator;

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_B2BAPPROVERGROUP,
			SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(operationId = "getOrderApprovals", summary = "Retrieves the orders to approve.", description = "Retrieves the orders that the user is allowed to approve.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderApprovalListWsDTO getOrderApprovals(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return orderApprovalsHelper.searchApprovals(currentPage, pageSize, sort, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_B2BAPPROVERGROUP,
			SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(value = "/{orderApprovalCode}", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	@Operation(operationId = "getOrderApproval", summary = "Retrieves an order to approve.", description = "Retrieves the details of an order to approve.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderApprovalWsDTO getOrderApproval(
			@Parameter(description = "Order approval code.", required = true) @PathVariable final String orderApprovalCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		final B2BOrderApprovalData orderApprovalDetails = orderFacade.getOrderApprovalDetailsForCode(orderApprovalCode);
		return getDataMapper().map(orderApprovalDetails, OrderApprovalWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP, SecuredAccessConstants.ROLE_B2BAPPROVERGROUP,
			SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/{orderApprovalCode}/decision", produces = MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
	@Operation(operationId = "doMakeOrderApprovalDecision", summary = "Creates a decision for the order approval.", description = "Creates a decision (approval or rejection) for the order, which will trigger the next step in the approval workflow.")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	public OrderApprovalDecisionWsDTO orderApprovalDecision(
			@Parameter(description = "Order approval code.", required = true) @PathVariable final String orderApprovalCode,
			@Parameter(description = "The order approval decision. The approval decision field is mandatory, and the approval comment field is mandatory if the decision is 'rejected'.", required = true) @RequestBody final OrderApprovalDecisionWsDTO orderApprovalDecision,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		validate(orderApprovalDecision, "orderApproval", orderApprovalDecisionValidator);

		final B2BOrderApprovalData b2bOrderApprovalData = getDataMapper().map(orderApprovalDecision, B2BOrderApprovalData.class);
		b2bOrderApprovalData.setWorkflowActionModelCode(orderApprovalCode);

		orderFacade.setOrderApprovalDecision(b2bOrderApprovalData);

		return getDataMapper().map(b2bOrderApprovalData, OrderApprovalDecisionWsDTO.class, fields);
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ResponseBody
	@ExceptionHandler({ IllegalArgumentException.class })
	public ErrorListWsDTO handleIllegalArgumentException(final Exception ex)
	{
		LOG.debug("IllegalArgumentException", ex);
		return handleErrorInternal(IllegalArgumentException.class.getSimpleName(), ILLEGAL_ARGUMENT_ERROR_MESSAGE);
	}

	@ResponseStatus(value = HttpStatus.NOT_FOUND)
	@ResponseBody
	@ExceptionHandler({ NullPointerException.class, PrincipalAssignedValidationException.class })
	public ErrorListWsDTO handleNotFoundExceptions(final Exception ex)
	{
		LOG.debug("Internal error", ex);
		return handleErrorInternal(NotFoundException.class.getSimpleName(), RESOURCE_NOT_FOUND_ERROR_MESSAGE);
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ResponseBody
	@ExceptionHandler({ WorkflowActionDecideException.class })
	public ErrorListWsDTO handleWorkflowActionDecideException(final Exception ex)
	{
		LOG.debug("WorkflowActionDecideException", ex);
		return handleErrorInternal(WorkflowActionDecideException.class.getSimpleName(), WORKFLOW_ACTION_DECIDE_ERROR_MESSAGE);
	}
}
