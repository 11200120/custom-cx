/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CartFacade;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BPaymentTypeData;
import de.hybris.platform.b2bcommercefacades.company.data.B2BCostCenterData;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipCartFieldValueSetter;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commercefacades.order.data.OrderEntryData;
import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercefacades.user.data.AddressData;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.CartWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdUserIdAndCartIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import java.util.ArrayList;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;

import static de.hybris.platform.sagajib2bwebservices.constants.Sagajib2bwebservicesConstants.OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH;


@Controller
@ApiVersion("v2")
@Tag(name = "B2B Carts")
public class B2BCartsController extends BaseController
{
	protected static final String API_COMPATIBILITY_B2B_CHANNELS = "api.compatibility.b2b.channels";

	@Resource(name = "b2bCartFacade")
	private CartFacade cartFacade;

	@Resource(name = "userFacade")
	protected UserFacade userFacade;

	@Resource(name = "dataMapper")
	protected DataMapper dataMapper;

	@Resource(name = "b2BCartAddressValidator")
	private Validator b2BCartAddressValidator;

	@Resource(name = "b2BOrderEntriesCreateValidator")
	private Validator b2BOrderEntriesCreateValidator;
	@Resource(name = "cartFieldValueSetter")
	private SkipCartFieldValueSetter skipCartFieldValueSetter;

	@Operation(operationId = "addOrgCartEntry", hidden = true, summary = "Adds more quantity to the cart for a specific entry",
			description = "Adds more quantity to the cart for a specific entry based on it's product code, if the product is already in the cart the amount will be added to the existing quantity.")
	@RequestMappingOverride(priorityProperty = "b2bocc.CartResource.addCartEntry.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@PostMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH + "/carts/{cartId}/entries")
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO addCartEntry(
			@Parameter(description = "Base site identifier.", required = true) @PathVariable final String baseSiteId,
			@Parameter(description = "Code of the product to be added to the cart.", required = true) @RequestParam(required = true) final String code,
			@Parameter(description = "Amount to be added.", required = false) @RequestParam(required = false, defaultValue = "1") final long quantity,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final OrderEntryData orderEntry = getOrderEntryData(quantity, code, null);

		return dataMapper.map(cartFacade.addOrderEntry(orderEntry), CartModificationWsDTO.class, fields);
	}

	@Operation(operationId = "updateOrgCartEntry", hidden = true, summary = "Updates the total amount of a specific product",
			description = "Updates the total amount of a specific product in the cart based on the entryNumber.")
	@RequestMappingOverride(priorityProperty = "b2bocc.CartResource.updateCartEntry.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@PutMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH + "/carts/{cartId}/entries/{entryNumber}")
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO updateCartEntry(@Parameter(description = "Base site identifier.") @PathVariable final String baseSiteId,
			@Parameter(description = "The id of the entry in the cart.") @PathVariable final int entryNumber,
			@Parameter(description = "New quantity for this entry.") @RequestParam(required = true) final Long quantity,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		return updateCartEntry(baseSiteId, null, entryNumber, quantity, fields);
	}

	@Operation(operationId = "updateOrgCartProductEntry", hidden = true, summary = "Updates the total amount of a specific product"
			, description = "Updates the total amount of a specific product in the cart based either in the product code or the entryNumber.")
	@RequestMappingOverride(priorityProperty = "b2bocc.CartResource.updateCartEntryByProduct.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@PutMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH + "/carts/{cartId}/entries/")
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationWsDTO updateCartEntry(@Parameter(description = "Base site identifier.") @PathVariable final String baseSiteId,
			@Parameter(description = "Code of the product to be added to the cart, this code is not considered if an entryNumber is passed.") @RequestParam(required = false) final String product,
			@Parameter(description = "The id of the entry in the cart, this parameter takes precedence over the product code.") @RequestParam(required = false) final Integer entryNumber,
			@Parameter(description = "New quantity for this product.") @RequestParam(required = true) final Long quantity,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final OrderEntryData orderEntry = getOrderEntryData(quantity, product, entryNumber);

		return dataMapper.map(cartFacade.updateOrderEntry(orderEntry), CartModificationWsDTO.class, fields);
	}

	@Operation(operationId = "doAddOrgCartEntries", summary = "Creates additional quantity of a product in the cart.", description = "Creates additional quantity of a product in the cart. Use this endpoint to add the quantities of the existing products. To update the products’ quantities or add new products to the cart, use PUT /{baseSiteId}/orgUsers/{userId}/carts/{cartId}/entries/")
	@RequestMappingOverride(priorityProperty = "b2bocc.CartResource.addCartEntries.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@PostMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH
			+ "/carts/{cartId}/entries/", consumes = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationListWsDTO addCartEntries(
			@Parameter(description = "Base site identifier.") @PathVariable final String baseSiteId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields,
			@Parameter(description = "List of entries containing the amount to add and the product code or the entryNumber.") @RequestBody(required = true) final OrderEntryListWsDTO entries)
	{
		validate(entries, "orderEntries", b2BOrderEntriesCreateValidator);

		final List<OrderEntryData> cartEntriesData = convertToData(entries);
		final List<CartModificationData> resultList = cartFacade.addOrderEntryList(cartEntriesData);

		return dataMapper.map(getCartModificationDataList(resultList), CartModificationListWsDTO.class, fields);

	}

	@Operation(operationId = "replaceOrgCartEntries", summary = "Updates the quantity of the specified products in a cart.", description = "Adds specific products or overwrites the details of existing products in the cart, based either on the product code or the entry number. For existing products, attributes not provided in the request body will be defined again (set to null or default).")
	@RequestMappingOverride(priorityProperty = "b2bocc.CartResource.updateCartEntries.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@PutMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH
			+ "/carts/{cartId}/entries/", consumes = { MediaType.APPLICATION_JSON_VALUE })
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartModificationListWsDTO updateCartEntries(
			@Parameter(description = "Base site identifier.") @PathVariable final String baseSiteId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields,
			@Parameter(description = "List of entries containing the amount to add and the product code or the entryNumber.") @RequestBody(required = true) final OrderEntryListWsDTO entries)
	{
		final List<OrderEntryData> cartEntriesData = convertToData(entries);
		final List<CartModificationData> resultList = cartFacade.updateOrderEntryList(cartEntriesData);

		return dataMapper.map(getCartModificationDataList(resultList), CartModificationListWsDTO.class, fields);
	}

	@Operation(operationId = "replaceOrgCartCostCenter", summary = "Updates the cost center for the cart.", description = "Updates the cost center for the cart during the checkout process.")
	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PutMapping(value = "/{baseSiteId}/users/{userId}/carts/{cartId}/costcenter")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartWsDTO setCartCostCenter(
			@Parameter(description = "Cost center identifier.", example = "00000001", required = true) @RequestParam(required = true) final String costCenterId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		skipCartFieldValueSetter.setValue(fields);
		final B2BCostCenterData costCenter = new B2BCostCenterData();
		costCenter.setCode(costCenterId);

		CartData cartData = new CartData();
		cartData.setCostCenter(costCenter);

		cartData = cartFacade.update(cartData);

		return dataMapper.map(cartData, CartWsDTO.class, fields);
	}

	@Operation(operationId = "replaceOrgCartPaymentType", summary = "Updates the payment type for the cart.", description = "Updates the payment type for the cart during the checkout process. If the purchaseOrderNumber is not null, it will also be updated for the cart.")
	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PutMapping(value = "/{baseSiteId}/users/{userId}/carts/{cartId}/paymenttype")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartWsDTO setPaymentType(
			@Parameter(description = "Payment type choice. Examples:card,account", required = true) @RequestParam(required = true) final String paymentType,
			@Parameter(description = "Purchase order number to assign to the cart during the checkout process.") @RequestParam(required = false) final String purchaseOrderNumber,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		skipCartFieldValueSetter.setValue(fields);
		final B2BPaymentTypeData paymentTypeData = new B2BPaymentTypeData();
		paymentTypeData.setCode(paymentType);

		CartData cartData = new CartData();
		cartData.setPaymentType(paymentTypeData);

		if (purchaseOrderNumber != null)
		{
			cartData.setPurchaseOrderNumber(purchaseOrderNumber);
		}

		cartData = cartFacade.update(cartData);

		return dataMapper.map(cartData, CartWsDTO.class, fields);
	}

	@Operation(operationId = "replaceOrgCartDeliveryAddress", summary = "Updates the delivery address for a cart.", description = "The updated address country must be a valid delivery country for the current base store.")
	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PutMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH
			+ "/carts/{cartId}/addresses/delivery")
	@RequestMappingOverride(priorityProperty = "b2bocc.B2BOrdersController.setCartDeliveryAddress.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@ApiBaseSiteIdUserIdAndCartIdParam
	public CartWsDTO setCartDeliveryAddress(
			@Parameter(description = "The id of the address.", required = true) @RequestParam(required = true) final String addressId,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		skipCartFieldValueSetter.setValue(fields);
		validateCartAddress(addressId);
		final AddressData address = new AddressData();
		address.setId(addressId);

		CartData cartData = new CartData();
		cartData.setDeliveryAddress(address);

		cartData = cartFacade.update(cartData);

		return dataMapper.map(cartData, CartWsDTO.class, fields);
	}

	@Operation(operationId = "getCurrentOrgCart", summary = "Gets the current cart.", description = "Retrieves the current cart.  To get entryGroup information, set fields value as follows: fields=entryGroups(BASIC), fields=entryGroups(DEFAULT), or fields=entryGroups(FULL).")
	@GetMapping(value = "/{baseSiteId}/users/{userId}/carts/current")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	public CartWsDTO getCurrentCart(
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		if (userFacade.isAnonymousUser())
		{
			throw new AccessDeniedException("Access is denied");
		}
		skipCartFieldValueSetter.setValue(fields);
		return dataMapper.map(cartFacade.getCurrentCart(), CartWsDTO.class, fields);
	}

	protected CartModificationDataList getCartModificationDataList(final List<CartModificationData> result)
	{
		final CartModificationDataList cartModificationDataList = new CartModificationDataList();
		cartModificationDataList.setCartModificationList(result);
		return cartModificationDataList;
	}

	//TODO: Do this mapping automatically or put in a populator.
	protected List<OrderEntryData> convertToData(final OrderEntryListWsDTO entriesWS)
	{
		final List<OrderEntryData> entriesData = new ArrayList<>();

		for (final OrderEntryWsDTO entryDto : entriesWS.getOrderEntries())
		{
			final OrderEntryData entryData = getOrderEntryData(entryDto.getQuantity(), entryDto.getProduct().getCode(),
					entryDto.getEntryNumber());
			entriesData.add(entryData);
		}

		return entriesData;
	}

	protected OrderEntryData getOrderEntryData(final long quantity, final String productCode, final Integer entryNumber)
	{
		final OrderEntryData orderEntry = new OrderEntryData();
		orderEntry.setQuantity(quantity);
		orderEntry.setProduct(new ProductData());
		orderEntry.getProduct().setCode(productCode);
		orderEntry.setEntryNumber(entryNumber);

		return orderEntry;
	}

	protected void validateCartAddress(final String addressId)
	{
		final Errors errors = new BeanPropertyBindingResult(addressId, "address Id");
		b2BCartAddressValidator.validate(addressId, errors);
		if (errors.hasErrors())
		{
			throw new WebserviceValidationException(errors);
		}
	}
}
