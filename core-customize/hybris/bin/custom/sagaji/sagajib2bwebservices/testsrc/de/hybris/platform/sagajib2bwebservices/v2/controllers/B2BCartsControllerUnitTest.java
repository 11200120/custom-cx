/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.b2bacceleratorfacades.api.cart.CartFacade;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipCartFieldValueSetter;
import de.hybris.platform.commercefacades.user.UserFacade;
import de.hybris.platform.commercewebservicescommons.dto.order.CartModificationListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import java.util.Collections;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class B2BCartsControllerUnitTest
{
	private static final String BASE_SITE_ID = "MY_BASE_SITE";
	private static final String FIELDS = "MY_FIELDS";

	@Mock
	private CartFacade cartFacade;
	@Mock
	protected UserFacade userFacade;
	@Mock
	protected DataMapper dataMapper;
	@Mock
	private Validator b2BCartAddressValidator;
	@Mock
	private Validator b2BOrderEntriesCreateValidator;
	@Mock
	private SkipCartFieldValueSetter skipCartFieldValueSetter;

	@InjectMocks
	private B2BCartsController controller;

	@Test
	public void testAddCartEntriesIsValidated()
	{
		final OrderEntryListWsDTO entries = new OrderEntryListWsDTO();
		entries.setOrderEntries(Collections.emptyList());

		controller.addCartEntries(BASE_SITE_ID, FIELDS, entries);

		verify(b2BOrderEntriesCreateValidator).validate(any(), any());

		verify(dataMapper).map(any(), eq(CartModificationListWsDTO.class), eq(FIELDS));
	}

	@Test
	public void testAddCartEntriesIsValidatedAndThrowsException()
	{
		final OrderEntryListWsDTO entries = new OrderEntryListWsDTO();

		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue("orderEntries", "entries is invalid", new String[] {}, null);
			return null;
		}).when(b2BOrderEntriesCreateValidator).validate(eq(entries), any());

		assertThatThrownBy(() -> controller.addCartEntries(BASE_SITE_ID, FIELDS, entries))
			.isInstanceOf(WebserviceValidationException.class);
		verify(b2BOrderEntriesCreateValidator).validate(any(), any());
		verifyNoInteractions(dataMapper);
	}
}
