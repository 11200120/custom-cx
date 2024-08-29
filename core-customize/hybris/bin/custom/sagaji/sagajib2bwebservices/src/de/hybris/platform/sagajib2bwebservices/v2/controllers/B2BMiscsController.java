/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CheckoutFacade;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BPaymentTypeData;
import de.hybris.platform.b2bwebservicescommons.dto.order.B2BPaymentTypeListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.B2BPaymentTypeWsDTO;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import java.util.List;

import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;


@Controller
@ApiVersion("v2")
@Tag(name = "B2B Miscs")
public class B2BMiscsController extends BaseController
{
	@Resource(name = "b2bCheckoutFacade")
	private CheckoutFacade checkoutFacade;

	@Resource(name = "dataMapper")
	private DataMapper dataMapper;

	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@GetMapping(value = "/{baseSiteId}/paymenttypes")
	@ResponseBody
	@Operation(operationId = "getPaymentTypes", summary = "Retrieves the available payment types.", description = "Retrieves the payment types that are available during the B2B checkout process.")
	@ApiBaseSiteIdParam
	public B2BPaymentTypeListWsDTO getPaymentTypes(
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final List<? extends B2BPaymentTypeData> paymentTypeDatas = checkoutFacade.getPaymentTypes();

		final B2BPaymentTypeListWsDTO dto = new B2BPaymentTypeListWsDTO();
		dto.setPaymentTypes(dataMapper.mapAsList(paymentTypeDatas, B2BPaymentTypeWsDTO.class, fields));

		return dto;
	}
}
