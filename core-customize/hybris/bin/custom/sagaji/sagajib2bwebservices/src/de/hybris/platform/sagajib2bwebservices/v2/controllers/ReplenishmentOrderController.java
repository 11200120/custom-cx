/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.sagajib2bwebservices.v2.helper.ReplenishmentOrderHelper;
import de.hybris.platform.b2bacceleratorservices.model.process.ReplenishmentProcessModel;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipReplenishmentOrderListFieldValueSetter;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipReplenishmentOrderFieldValueSetter;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderWsDTO;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryListWsDTO;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


@Controller
@RequestMapping(value = "/{baseSiteId}/users/{userId}/replenishmentOrders")
@ApiVersion("v2")
@Tag(name = "Replenishment Order")
public class ReplenishmentOrderController extends BaseController
{

	@Resource(name = "replenishmentOrderHelper")
	private ReplenishmentOrderHelper replenishmentOrderHelper;
	@Resource(name = "skipReplenishmentOrderListFieldValueSetter")
	private SkipReplenishmentOrderListFieldValueSetter skipReplenishmentOrderListFieldValueSetter;
	@Resource(name = "skipReplenishmentOrderFieldValueSetter")
	private SkipReplenishmentOrderFieldValueSetter skipReplenishmentOrderFieldValueSetter;

	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	@GetMapping(produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "getReplenishmentOrders", summary = "Retrieves the replenishment orders.", description = "Retrieves the replenishment orders that are available by the customer.")
	public ReplenishmentOrderListWsDTO getReplenishmentOrders(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(defaultValue = ReplenishmentProcessModel.CODE) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipReplenishmentOrderListFieldValueSetter.setValue(fields);
		return replenishmentOrderHelper.searchReplenishments(currentPage, pageSize, sort, addPaginationField(fields));
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	@GetMapping(value = "/{replenishmentOrderCode}", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "getReplenishmentOrder",
            summary = "Retrieves the replenishment order.", description = "Retrieves the details of a replenishment order. To get entryGroup information, set fields value as follows: fields=entryGroups(BASIC), fields=entryGroups(DEFAULT), or fields=entryGroups(FULL).")
	public ReplenishmentOrderWsDTO getReplenishmentOrder(
			@Parameter(description = "Replenishment order code.", required = true) @PathVariable final String replenishmentOrderCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipReplenishmentOrderFieldValueSetter.setValue(fields);
		return replenishmentOrderHelper.searchReplenishment(replenishmentOrderCode, fields);
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	@PatchMapping(value = "/{replenishmentOrderCode}", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "updateReplenishmentOrder",
            summary = "Updates the replenishment order.",
            description = "Updates the replenishment order of the specified user using the specified code. The cancellation of the replenishment order is supported by setting the active property to FALSE. The cancellation cannot be reverted.")
	public ReplenishmentOrderWsDTO updateReplenishmentOrder(
			@Parameter(description = "Unique code for the replenishment order.", required = true) @PathVariable final String replenishmentOrderCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipReplenishmentOrderFieldValueSetter.setValue(fields);
		return replenishmentOrderHelper.cancelReplenishment(replenishmentOrderCode, fields);
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	@GetMapping(value = "/{replenishmentOrderCode}/orders", produces = MediaType.APPLICATION_JSON)
	@Operation(operationId = "getReplenishmentOrderHistory",
            summary = "Retrieves the history of the replenishment order.",
            description = "Retrieves the history data of the replenishment order that was placed by the customer.")
	public OrderHistoryListWsDTO getReplenishmentOrderHistory(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(defaultValue = ReplenishmentProcessModel.CODE) final String sort,
			@Parameter(description = "Replenishment order code.", required = true) @PathVariable final String replenishmentOrderCode,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		return replenishmentOrderHelper.searchOrderHistories(replenishmentOrderCode, currentPage, pageSize, sort,
				addPaginationField(fields));
	}

}
