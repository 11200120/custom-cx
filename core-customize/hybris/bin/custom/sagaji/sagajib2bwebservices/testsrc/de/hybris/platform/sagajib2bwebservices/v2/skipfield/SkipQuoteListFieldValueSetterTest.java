/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteWsDTO;

import java.util.List;

import org.junit.Test;
import org.mockito.InjectMocks;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SkipQuoteListFieldValueSetterTest extends BaseSkipFieldTest
{
	@InjectMocks
	private SkipQuoteListFieldValueSetter skipQuoteListFieldValueSetter;

	@Test
	public void testSetValue()
	{
		final QuoteWsDTO quoteWsDTO = new QuoteWsDTO();
		final QuoteListWsDTO listWsDTO = new QuoteListWsDTO();
		listWsDTO.setQuotes(List.of(quoteWsDTO));
		when(dataMapper.map(anyObject(), eq(QuoteListWsDTO.class), anyString())).thenReturn(listWsDTO);
		skipQuoteListFieldValueSetter.setValue(FIELD_ENTRIES);
		verify(sessionService).setAttribute(SKIP_ORDER_ENTRYGROUPS, true);
	}

}
