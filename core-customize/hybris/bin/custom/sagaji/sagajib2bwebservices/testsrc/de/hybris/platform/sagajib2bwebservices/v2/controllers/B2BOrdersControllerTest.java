/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.b2bacceleratorfacades.order.B2BOrderFacade;
import de.hybris.platform.sagajib2bwebservices.v2.helper.B2BOrdersHelper;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipOrderFieldValueSetter;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipReplenishmentOrderFieldValueSetter;
import de.hybris.platform.sagajib2bwebservices.validators.B2BPlaceOrderCartValidator;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CartFacade;
import de.hybris.platform.b2bacceleratorfacades.order.impl.DefaultB2BCheckoutFacade;
import de.hybris.platform.b2bacceleratorfacades.order.data.B2BPaymentTypeData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.order.ScheduleReplenishmentFormWsDTO;
import de.hybris.platform.commercefacades.order.data.*;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.CommerceCartModificationStatus;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.pagedata.PaginationWsDTO;
import de.hybris.platform.commercewebservicescommons.errors.exceptions.PaymentAuthorizationException;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.order.InvalidCartException;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.validators.EnumValueValidator;

import javax.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Validator;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class B2BOrdersControllerTest
{
	private static final String CART_ID = "MY_CART_ID";
	private static final String FIELDS = "MY_FIELDS";
	private static final String PAYMENT_CODE = "CARD";
	private static final int PAGE_SIZE = 20;
	private static final int CURRENT_PAGE = 0;
	private static final String PAGINATION_FIELDS = "MY_FIELDS,pagination";
	private static final String SORT = "SORT";
	private static final String STATUSES = "STATUSES";

	private static final String FILTERS = "FILTERS";
	private static final String CODE = "CODE";

	@Spy
	private OrderWsDTO orderWsDTO;
	@Spy
	private ReplenishmentOrderWsDTO replenishmentOrderWsDTO;
	@Mock
	private DataMapper dataMapper;
	@InjectMocks
	private B2BOrdersController controller;
	@Mock
	private CartData cartData;
	@Mock
	private UserFacade userFacade;
	@Mock
	private CartLoaderStrategy cartLoaderStrategy;
	@Mock
	private CartFacade cartFacade;
	@Mock
	private B2BPlaceOrderCartValidator placeOrderCartValidator;
	@Mock
	private DefaultB2BCheckoutFacade b2bCheckoutFacade;
	@Mock
	private Validator scheduleReplenishmentFormWsDTOValidator;
	@Mock
	private AbstractOrderData abstractOrderData;
	@Mock
	private B2BPaymentTypeData b2BPaymentTypeData;
	@Mock
	private ScheduleReplenishmentFormWsDTO scheduleReplenishmentFormWsDTO;
	@Mock
	private B2BOrdersHelper ordersHelper;
	@Mock
	private OrderHistoryListWsDTO orderHistoryList;
	@Mock
	private HttpServletResponse response;
	@Mock
	private EnumValueValidator orderStatusValueValidator;
	@Mock
	private PaginationWsDTO paginationWsDTO;
	@Mock
	private OrderHistoriesData orderHistoriesData;
	@Mock
	private B2BOrderFacade b2bOrderFacade;
	@Mock
	private OrderData orderData;
	@Mock
	private SkipOrderFieldValueSetter skipOrderFieldValueSetter;
	@Mock
	private SkipReplenishmentOrderFieldValueSetter skipReplenishmentOrderFieldValueSetter;

	@Test
	public void testPlaceOrgOrder() throws CommerceCartModificationException, PaymentAuthorizationException, InvalidCartException
	{
		when(userFacade.isAnonymousUser()).thenReturn(false);
		when(cartFacade.getCurrentCart()).thenReturn(cartData);
		when(dataMapper.map(abstractOrderData, OrderWsDTO.class, FIELDS)).thenReturn(orderWsDTO);
		when(cartData.getPaymentType()).thenReturn(b2BPaymentTypeData);
		when(b2BPaymentTypeData.getCode()).thenReturn(PAYMENT_CODE);
		when(b2bCheckoutFacade.authorizePayment(any())).thenReturn(true);
		when(b2bCheckoutFacade.placeOrder(any())).thenReturn(abstractOrderData);

		final OrderWsDTO initOrderWsDTO = controller.placeOrgOrder(CART_ID, true, FIELDS);

		verify(userFacade).isAnonymousUser();
		verify(cartLoaderStrategy).loadCart(CART_ID);
		verify(cartFacade).getCurrentCart();
		verify(cartFacade).validateCurrentCartData();

		verify(dataMapper).map(abstractOrderData, OrderWsDTO.class, FIELDS);
		assertThat(initOrderWsDTO).isSameAs(orderWsDTO);
	}

	@Test
	public void testPlaceOrgOrderCartValidationReturnModificationsList() throws CommerceCartModificationException
	{
		final List<CartModificationData> modifications = getNewCommerceCartModifications();

		when(userFacade.isAnonymousUser()).thenReturn(false);
		when(cartFacade.getCurrentCart()).thenReturn(cartData);
		when(cartFacade.validateCurrentCartData()).thenReturn(modifications);

		assertThatThrownBy(() -> controller.placeOrgOrder(CART_ID, true, FIELDS))
				.isInstanceOf(WebserviceValidationException.class)
				.hasMessage("Validation error");

		verifyNoMoreInteractions(cartData);
		verifyNoMoreInteractions(b2bCheckoutFacade);
		verifyNoMoreInteractions(dataMapper);
	}

	@Test
	public void testPlaceOrgOrderCartValidationReturnCommerceCartModificationException() throws CommerceCartModificationException
	{
		final CommerceCartModificationException commerceCartModificationException = new CommerceCartModificationException("Error when validating a cart");
		doThrow(commerceCartModificationException).when(cartFacade).validateCurrentCartData();
		when(userFacade.isAnonymousUser()).thenReturn(false);
		when(cartFacade.getCurrentCart()).thenReturn(cartData);

		assertThatThrownBy(() -> controller.placeOrgOrder(CART_ID, true, FIELDS))
				.isInstanceOf(InvalidCartException.class)
				.hasCause(commerceCartModificationException);

		verifyNoMoreInteractions(cartData);
		verifyNoMoreInteractions(b2bCheckoutFacade);
		verifyNoMoreInteractions(dataMapper);
	}

	@Test
	public void testPlaceReplenishmentOrder() throws CommerceCartModificationException, PaymentAuthorizationException, InvalidCartException
	{
		when(userFacade.isAnonymousUser()).thenReturn(false);
		when(cartFacade.getCurrentCart()).thenReturn(cartData);
		when(dataMapper.map(abstractOrderData, ReplenishmentOrderWsDTO.class, FIELDS)).thenReturn(replenishmentOrderWsDTO);
		when(cartData.getPaymentType()).thenReturn(b2BPaymentTypeData);
		when(b2BPaymentTypeData.getCode()).thenReturn(PAYMENT_CODE);
		when(b2bCheckoutFacade.authorizePayment(any())).thenReturn(true);
		when(b2bCheckoutFacade.placeOrder(any())).thenReturn(abstractOrderData);


		final ReplenishmentOrderWsDTO initReplenishmentOrderWsDTO = controller.createReplenishmentOrder(CART_ID, true,scheduleReplenishmentFormWsDTO,FIELDS);

		verify(userFacade).isAnonymousUser();
		verify(cartLoaderStrategy).loadCart(CART_ID);
		verify(cartFacade).validateCurrentCartData();

		verify(dataMapper).map(abstractOrderData, ReplenishmentOrderWsDTO.class, FIELDS);
		assertThat(initReplenishmentOrderWsDTO).isSameAs(replenishmentOrderWsDTO);
	}

	@Test
	public void testPlaceReplenishmentOrderCartValidationReturnModificationsList() throws CommerceCartModificationException
	{
		final List<CartModificationData> modifications = getNewCommerceCartModifications();

		when(userFacade.isAnonymousUser()).thenReturn(false);
		when(cartFacade.getCurrentCart()).thenReturn(cartData);
		when(cartFacade.validateCurrentCartData()).thenReturn(modifications);

		assertThatThrownBy(() -> controller.createReplenishmentOrder(CART_ID, true, scheduleReplenishmentFormWsDTO, FIELDS))
				.isInstanceOf(WebserviceValidationException.class)
				.hasMessage("Validation error");

		verifyNoMoreInteractions(cartData);
		verifyNoMoreInteractions(b2bCheckoutFacade);
		verifyNoMoreInteractions(dataMapper);
	}

	@Test
	public void testPlaceReplenishmentOrderCartValidationReturnCommerceCartModificationException() throws CommerceCartModificationException
	{
		final CommerceCartModificationException commerceCartModificationException = new CommerceCartModificationException("Error when validating a cart");
		doThrow(commerceCartModificationException).when(cartFacade).validateCurrentCartData();
		when(userFacade.isAnonymousUser()).thenReturn(false);
		when(cartFacade.getCurrentCart()).thenReturn(cartData);

		assertThatThrownBy(() -> controller.createReplenishmentOrder(CART_ID, true, scheduleReplenishmentFormWsDTO, FIELDS))
				.isInstanceOf(InvalidCartException.class)
				.hasCause(commerceCartModificationException);

		verifyNoMoreInteractions(cartData);
		verifyNoMoreInteractions(b2bCheckoutFacade);
		verifyNoMoreInteractions(dataMapper);
	}

	private List<CartModificationData> getNewCommerceCartModifications() {
		final List<CartModificationData> modifications = new ArrayList<>();
		final CartModificationData cartModificationData = new CartModificationData();
		cartModificationData.setStatusCode(CommerceCartModificationStatus.NO_STOCK);

		modifications.add(cartModificationData);

		return modifications;
	}

	@Test
	public void testGetUserBranchOrderHistory()
	{
		when(ordersHelper.searchBranchOrderHistory(Mockito.anyString(),Mockito.anyString(),Mockito.anyInt(),Mockito.anyInt(),Mockito.anyString(),Mockito.anyString())).thenReturn(orderHistoryList);
		when(orderHistoryList.getPagination()).thenReturn(paginationWsDTO);

		final OrderHistoryListWsDTO orderHistoryListWsDTO = controller.getUserBranchOrderHistory(STATUSES,FILTERS,CURRENT_PAGE,PAGE_SIZE,SORT,FIELDS,response);

		verify(ordersHelper).searchBranchOrderHistory(STATUSES,FILTERS,CURRENT_PAGE,PAGE_SIZE,SORT,PAGINATION_FIELDS);
		assertThat(orderHistoryListWsDTO).isSameAs(orderHistoryList);
	}

	@Test
	public void testGetBranchOrder()
	{
		when(b2bOrderFacade.getBranchOrderForCode(Mockito.anyString())).thenReturn(orderData);
		when(dataMapper.map(orderData, OrderWsDTO.class, FIELDS)).thenReturn(orderWsDTO);

		controller.getBranchOrder(CODE, FIELDS);

		verify(b2bOrderFacade).getBranchOrderForCode(CODE);
		verify(dataMapper).map(orderData, OrderWsDTO.class, FIELDS);
	}
}
