/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import static de.hybris.platform.sagajib2bwebservices.constants.Sagajib2bwebservicesConstants.OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH;
import static de.hybris.platform.util.localization.Localization.getLocalizedString;

import de.hybris.platform.b2bacceleratorfacades.order.B2BOrderFacade;
import de.hybris.platform.b2bacceleratorfacades.order.impl.DefaultB2BCheckoutFacade;
import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CartFacade;
import de.hybris.platform.b2bacceleratorfacades.checkout.data.PlaceOrderData;
import de.hybris.platform.b2b.enums.CheckoutPaymentType;
import de.hybris.platform.sagajib2bwebservices.v2.helper.B2BOrdersHelper;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipOrderFieldValueSetter;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipReplenishmentOrderFieldValueSetter;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.ScheduleReplenishmentFormWsDTO;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CartModificationDataList;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.PaymentAuthorizationException;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.RequestParameterException;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.webservicescommons.validators.EnumValueValidator;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@Controller
@RequestMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH)
@ApiVersion("v2")
@Tag(name = "B2B Orders")
public class B2BOrdersController extends BaseController
{
	protected static final String API_COMPATIBILITY_B2B_CHANNELS = "api.compatibility.b2b.channels";
	private static final String CART_CHECKOUT_TERM_UNCHECKED = "cart.term.unchecked";
	private static final String OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM = "ScheduleReplenishmentForm";

	@Resource(name = "userFacade")
	protected UserFacade userFacade;

	@Resource(name = "defaultB2BAcceleratorCheckoutFacade")
	private DefaultB2BCheckoutFacade b2bCheckoutFacade;

	@Resource(name = "b2bCartFacade")
	private CartFacade cartFacade;

	@Resource(name = "b2bOrderFacade")
	private B2BOrderFacade b2bOrderFacade;

	@Resource(name = "cartLoaderStrategy")
	private CartLoaderStrategy cartLoaderStrategy;

	@Resource(name = "dataMapper")
	private DataMapper dataMapper;

	@Resource(name = "b2BPlaceOrderCartValidator")
	private Validator placeOrderCartValidator;

	@Resource(name = "scheduleReplenishmentFormWsDTOValidator")
	private Validator scheduleReplenishmentFormWsDTOValidator;

	@Resource(name = "b2BOrdersHelper")
	private B2BOrdersHelper b2BOrdersHelper;

	@Resource(name = "orderStatusValueValidator")
	private EnumValueValidator orderStatusValueValidator;
	@Resource(name = "orderFieldValueSetter")
	private SkipOrderFieldValueSetter skipOrderFieldValueSetter;
	@Resource(name = "skipReplenishmentOrderFieldValueSetter")
	private SkipReplenishmentOrderFieldValueSetter skipReplenishmentOrderFieldValueSetter;

	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/orders")
	@RequestMappingOverride(priorityProperty = "b2bocc.B2BOrdersController.placeOrder.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	@Operation(operationId = "placeOrgOrder", summary = "Creates a B2B order.", description = "Creates a B2B order. By default the payment type is ACCOUNT. Set payment type to CARD if placing an order using credit card.")
	public OrderWsDTO placeOrgOrder(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", example = "00000110", required = true) @RequestParam(required = true) final String cartId,
			@Parameter(description = "Whether terms were accepted or not.", required = true) @RequestParam(required = true) final boolean termsChecked,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
			throws InvalidCartException, PaymentAuthorizationException
	{

		validateTerms(termsChecked);

		validateUser();

		skipOrderFieldValueSetter.setValue(fields);
		cartLoaderStrategy.loadCart(cartId);
		final CartData cartData = cartFacade.getCurrentCart();

		validateCart(cartData);
		validateAndAuthorizePayment(cartData);

		return dataMapper.map(b2bCheckoutFacade.placeOrder(new PlaceOrderData()), OrderWsDTO.class, fields);
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, "ROLE_CLIENT" })
	@PostMapping(value = "/cartFromOrder")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@ResponseBody
	@ResponseStatus(HttpStatus.CREATED)
	@Operation(operationId = "createCartFromOrder", summary = "Creates a cart from a previous order.", description = "Returns a list of modification applied to the new cart compared to original. e.g lower quantity was added")
	@ApiBaseSiteIdAndUserIdParam
	public CartModificationListWsDTO createCartFromOrder(
			@Parameter(description = "Order code.", example = "000002000", required = true) @RequestParam final String orderCode,
			@Parameter(description = "Response configuration. This is the list of fields that should be returned in the response body.", schema = @Schema(allowableValues = {"BASIC", "DEFAULT", "FULL"})) @RequestParam(defaultValue = "DEFAULT") final String fields,
			@Parameter(hidden = true) final HttpServletResponse response)
	{
		b2bCheckoutFacade.createCartFromOrder(orderCode);
		try
		{
			final List<CartModificationData> cartModifications = cartFacade.validateCurrentCartData();
			final CartModificationDataList cartModificationDataList = new CartModificationDataList();
			cartModificationDataList.setCartModificationList(cartModifications);
			response.setHeader("Location", "/current");
			return dataMapper.map(cartModificationDataList, CartModificationListWsDTO.class, fields);
		}
		catch (final CommerceCartModificationException e)
		{
			cartFacade.removeSessionCart();
			throw new IllegalArgumentException("Unable to create cart from the given order. Cart cannot be modified", e);
		}
	}

	@Secured(
	{ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_GUEST,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT })
	@PostMapping(value = "/replenishmentOrders", consumes =
	{ MediaType.APPLICATION_JSON_VALUE }, produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	@ResponseBody
	@ApiBaseSiteIdAndUserIdParam
	@Operation(operationId = "createReplenishmentOrder", summary = "Creates replenishment orders.", description = "Creates and schedules replenishment orders. By default, the payment type is ACCOUNT. Set payment type to CARD if placing an order using credit card.")
	public ReplenishmentOrderWsDTO createReplenishmentOrder(
			@Parameter(description = "Cart identifier: cart code for logged-in user, cart GUID for anonymous user, or 'current' for the last modified cart.", example = "00000110", required = true) @RequestParam(required = true) final String cartId,
			@Parameter(description = "Whether terms were accepted or not.", required = true) @RequestParam(required = true) final boolean termsChecked,
			@Parameter(description = "Schedule replenishment form object.", required = true) @RequestBody(required = true) final ScheduleReplenishmentFormWsDTO scheduleReplenishmentForm,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
			throws InvalidCartException, PaymentAuthorizationException
	{

		validateTerms(termsChecked);

		validateUser();
		skipReplenishmentOrderFieldValueSetter.setValue(fields);
		cartLoaderStrategy.loadCart(cartId);
		final CartData cartData = cartFacade.getCurrentCart();

		validateCart(cartData);
		validateAndAuthorizePayment(cartData);

		validateScheduleReplenishmentForm(scheduleReplenishmentForm);
		final PlaceOrderData placeOrderData = createPlaceOrderData(scheduleReplenishmentForm);

		return dataMapper.map(b2bCheckoutFacade.placeOrder(placeOrderData), ReplenishmentOrderWsDTO.class, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_TRUSTED_CLIENT, SecuredAccessConstants.ROLE_UNITORDERVIEWERGROUP })
	@GetMapping(value = "/orgUnits/orders")
	@ResponseBody
	@Operation(operationId = "getUserBranchOrderHistory", summary = "Retrieves the order history for the customer's organization branch.", description = "Retrieves the order history of a default organization branch for a base site. The response may display the results across multiple pages, when applicable.")
	@ApiBaseSiteIdAndUserIdParam
	public OrderHistoryListWsDTO getUserBranchOrderHistory(
			@Parameter(description = "Filters only certain order statuses. For example, statuses=CANCELLED,CHECKED_VALID would only return orders with status CANCELLED or CHECKED_VALID.") @RequestParam(required = false) final String statuses,
			@Parameter(description = "Serialized filters applied to the query in the following format: ::facetKey1:facetValue1:facetKey2:facetValue2. Correct values for facetKey are 'user' and 'unit'.") @RequestParam(required = false) final String filters,
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(defaultValue = DEFAULT_CURRENT_PAGE) final int currentPage,
			@Parameter(description = "The number of results returned per page.") @RequestParam(defaultValue = DEFAULT_PAGE_SIZE) final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields, final HttpServletResponse response)
	{
		validateStatusesEnumValue(statuses);

		final OrderHistoryListWsDTO orderHistoryList = b2BOrdersHelper
				.searchBranchOrderHistory(statuses, filters, currentPage, pageSize, sort, addPaginationField(fields));

		setTotalCountHeader(response, orderHistoryList.getPagination());

		return orderHistoryList;
	}

	@Secured({ SecuredAccessConstants.ROLE_TRUSTED_CLIENT, SecuredAccessConstants.ROLE_UNITORDERVIEWERGROUP })
	@GetMapping(value = "/orgUnits/orders/{code}")
	@ResponseBody
	@Operation(operationId = "getBranchOrder", summary = "Retrieves the order from the customer's organization branch.", description = "Retrieves the details of the order using a unique order code in the customer's default organization branch. To get entryGroup information, set fields value as follows: fields=entryGroups(BASIC), fields=entryGroups(DEFAULT), or fields=entryGroups(FULL).")
	@ApiBaseSiteIdAndUserIdParam
	public OrderWsDTO getBranchOrder(
			@Parameter(description = "Order code.", example = "00001000", required = true) @PathVariable final String code,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		skipOrderFieldValueSetter.setValue(fields);
		final OrderData orderData = b2bOrderFacade.getBranchOrderForCode(code);
		return dataMapper.map(orderData, OrderWsDTO.class, fields);
	}

	/**
	 * Checks if given statuses are valid
	 *
	 * @param statuses
	 */
	protected void validateStatusesEnumValue(final String statuses)
	{
		if (statuses == null)
		{
			return;
		}

		final String[] statusesStrings = statuses.split(",");
		validate(statusesStrings, "", orderStatusValueValidator);
	}
	protected void validateUser()
	{
		if (userFacade.isAnonymousUser())
		{
			throw new AccessDeniedException("Access is denied");
		}
	}

	protected void validateTerms(final boolean termsChecked)
	{
		if (!termsChecked)
		{
			throw new RequestParameterException(getLocalizedString(CART_CHECKOUT_TERM_UNCHECKED));
		}
	}

	protected void validateScheduleReplenishmentForm(final ScheduleReplenishmentFormWsDTO scheduleReplenishmentForm)
	{
		validate(scheduleReplenishmentForm, OBJECT_NAME_SCHEDULE_REPLENISHMENT_FORM, scheduleReplenishmentFormWsDTOValidator);
	}

	protected void validateAndAuthorizePayment(final CartData cartData)
			throws PaymentAuthorizationException
	{
		if (CheckoutPaymentType.CARD.getCode().equals(cartData.getPaymentType().getCode()) && !b2bCheckoutFacade.authorizePayment(null))
		{
				throw new PaymentAuthorizationException();
		}
	}

	protected void validateCart(final CartData cartData) throws InvalidCartException
	{
		final Errors errors = new BeanPropertyBindingResult(cartData, "sessionCart");
		placeOrderCartValidator.validate(cartData, errors);
		if (errors.hasErrors())
		{
			throw new WebserviceValidationException(errors);
		}

		try
		{
			final List<CartModificationData> modificationList = cartFacade.validateCurrentCartData();
			if(CollectionUtils.isNotEmpty(modificationList))
			{
				final CartModificationDataList cartModificationDataList = new CartModificationDataList();
				cartModificationDataList.setCartModificationList(modificationList);
				throw new WebserviceValidationException(cartModificationDataList);
			}
		}
		catch (final CommerceCartModificationException e)
		{
			throw new InvalidCartException(e);
		}
	}

	protected PlaceOrderData createPlaceOrderData(final ScheduleReplenishmentFormWsDTO scheduleReplenishmentForm)
	{
		final PlaceOrderData placeOrderData = new PlaceOrderData();
		dataMapper.map(scheduleReplenishmentForm, placeOrderData, false);
		if (scheduleReplenishmentForm != null)
		{
			placeOrderData.setReplenishmentOrder(Boolean.TRUE);
		}
		placeOrderData.setTermsCheck(Boolean.TRUE);
		return placeOrderData;
	}

}
