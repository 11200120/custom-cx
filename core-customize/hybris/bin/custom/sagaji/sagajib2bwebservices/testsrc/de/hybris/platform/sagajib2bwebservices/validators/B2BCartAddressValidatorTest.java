/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.validators;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commerceservices.strategies.DeliveryAddressesLookupStrategy;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.user.AddressModel;
import de.hybris.platform.order.CartService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Errors;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class B2BCartAddressValidatorTest
{
	@Mock
	private CartService cartService;

	@Mock
	private DeliveryAddressesLookupStrategy b2bDeliveryAddressesLookupStrategy;

	@Mock
	private Errors errors;

	@Mock
	private AddressModel addressModelMock;

	@InjectMocks
	private B2BCartAddressValidator b2BCartAddressValidator;

	private static final String ERROR_CODE = "cart.deliveryAddressInvalid";
	private static final String ADDRESS_ID = "8796158590999";

	@Test
	public void testSupports()
	{
		assertThat(b2BCartAddressValidator.supports(String.class)).isTrue();
	}

	@Test
	public void validateEmptyAddressId()
	{
		String addressId = "";
		b2BCartAddressValidator.validate(addressId, errors);
		verify(errors, times(1)).reject(ERROR_CODE);
	}

	@Test
	public void validateNoAddressInAddressBook()
	{
		CartModel cartModel = new CartModel();
		when(cartService.getSessionCart()).thenReturn(cartModel);
		when(b2bDeliveryAddressesLookupStrategy.getDeliveryAddressesForOrder(cartModel, true)).thenReturn(Collections.emptyList());
		b2BCartAddressValidator.validate(ADDRESS_ID, errors);
		verify(errors, times(1)).reject(ERROR_CODE);
	}

	@Test
	public void validateAddressIdNotInAddressBook()
	{
		CartModel cartModel = new CartModel();
		when(cartService.getSessionCart()).thenReturn(cartModel);
		List<AddressModel> addressList = new ArrayList<>();
		addressList.add(addressModelMock);
		when(addressModelMock.getPk()).thenReturn(PK.fromLong(123456789L));
		when(b2bDeliveryAddressesLookupStrategy.getDeliveryAddressesForOrder(cartModel, true)).thenReturn(addressList);
		b2BCartAddressValidator.validate(ADDRESS_ID, errors);
		verify(errors, times(1)).reject(ERROR_CODE);
	}

	@Test
	public void validateAddressIdInAddressBook()
	{
		CartModel cartModel = new CartModel();
		when(cartService.getSessionCart()).thenReturn(cartModel);
		List<AddressModel> addressList = new ArrayList<>();
		addressList.add(addressModelMock);
		when(addressModelMock.getPk()).thenReturn(PK.fromLong(8796158590999L));
		when(b2bDeliveryAddressesLookupStrategy.getDeliveryAddressesForOrder(cartModel, true)).thenReturn(addressList);
		b2BCartAddressValidator.validate(ADDRESS_ID, errors);
		verify(errors, times(0)).reject(ERROR_CODE);
	}
}
