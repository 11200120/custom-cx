/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.populators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.comment.data.CommentData;
import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercefacades.product.data.PriceData;
import de.hybris.platform.commercewebservicescommons.dto.comments.CommentWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.PriceWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteWsDTO;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class DefaultCartModelToQuoteWsDTOPopulatorTest
{
	private static final String CART_ID = "MY_CART_ID";
	private static final String CART_NAME = "NAME";
	private static final String CART_DESCRIPTION = "DESCRIPTION";
	private static final String CART_COMMENT = "COMMENT";

	private static final String QUOTE_DISCOUNTS_TYPE = "ABSOLUTE";
	private static final Double QUOTE_DISCOUNTS_RATE = Double.valueOf(10);
	@Mock
	private Converter<CartModel, CartData> cartConverter;
	@Mock
	private DataMapper dataMapper;
	@Mock
	private CartModel cartModel;

	@InjectMocks
	private DefaultCartModelToQuoteWsDTOPopulator populator;

	private final CartData cartData = new CartData();

	@Before
	public void setUp()
	{
		when(cartConverter.convert(cartModel)).thenReturn(cartData);
	}

	@Test
	public void testPopulate()
	{
		cartData.setName(CART_NAME);
		cartData.setDescription(CART_DESCRIPTION);
		cartData.setCode(CART_ID);
		final PriceData priceData = new PriceData();
		cartData.setQuoteDiscounts(priceData);
		int totalItems = 1;
		cartData.setTotalItems(totalItems);
		final PriceData totalPriceData = new PriceData();
		cartData.setTotalPrice(totalPriceData);
		final PriceData totalPriceWithTaxData = new PriceData();
		cartData.setTotalPriceWithTax(totalPriceWithTaxData);
		cartData.setQuoteDiscountsType(QUOTE_DISCOUNTS_TYPE);
		cartData.setQuoteDiscountsRate(QUOTE_DISCOUNTS_RATE);

		final CommentData commentData = new CommentData();
		commentData.setText(CART_COMMENT);
		cartData.setComments(List.of(commentData));

		final CommentWsDTO comment = new CommentWsDTO();
		comment.setText(CART_COMMENT);

		final OrderEntryWsDTO orderEntryWsDTO = new OrderEntryWsDTO();

		when(dataMapper.mapAsList(any(), eq(CommentWsDTO.class), eq("FULL"))).thenReturn(List.of(comment));
		when(dataMapper.mapAsList(any(), eq(OrderEntryWsDTO.class), eq(null))).thenReturn(List.of(orderEntryWsDTO));
		final PriceWsDTO quoteDiscounts = new PriceWsDTO();
		when(dataMapper.map(any(), eq(PriceWsDTO.class), eq("FULL"))).thenReturn(quoteDiscounts);
		final PriceWsDTO totalPrice = new PriceWsDTO();
		when(dataMapper.map(totalPriceData, PriceWsDTO.class, "FULL")).thenReturn(totalPrice);
		final PriceWsDTO totalPriceWithTax = new PriceWsDTO();
		when(dataMapper.map(totalPriceWithTaxData, PriceWsDTO.class, "FULL")).thenReturn(totalPriceWithTax);


		final QuoteWsDTO quoteWsDTO = new QuoteWsDTO();
		populator.populate(cartModel, quoteWsDTO);

		verify(dataMapper).mapAsList(any(), eq(OrderEntryWsDTO.class), eq(null));
		verify(dataMapper).mapAsList(any(), eq(CommentWsDTO.class), eq("FULL"));

		assertThat(quoteWsDTO)
			.hasFieldOrPropertyWithValue("name", CART_NAME)
			.hasFieldOrPropertyWithValue("description", CART_DESCRIPTION)
			.hasFieldOrPropertyWithValue("cartId", CART_ID)
			.hasFieldOrPropertyWithValue("comments", List.of(comment))
			.hasFieldOrPropertyWithValue("quoteDiscounts", quoteDiscounts)
			.hasFieldOrPropertyWithValue("totalItems", totalItems)
			.hasFieldOrPropertyWithValue("totalPrice", totalPrice)
			.hasFieldOrPropertyWithValue("totalPriceWithTax", totalPriceWithTax)
			.hasFieldOrPropertyWithValue("sapQuoteDiscountsRate", QUOTE_DISCOUNTS_RATE)
			.hasFieldOrPropertyWithValue("sapQuoteDiscountsType", QUOTE_DISCOUNTS_TYPE);

		assertThat(cartData.getComments().get(0)).hasFieldOrPropertyWithValue("text", CART_COMMENT);
		assertThat(quoteWsDTO.getComments().get(0)).hasFieldOrPropertyWithValue("text", CART_COMMENT);
		assertThat(quoteWsDTO.getEntries()).hasSize(1)
				.element(0)
				.isEqualTo(orderEntryWsDTO);
	}

	@Test
	public void testPopulateWithNullCartModel()
	{
		final QuoteWsDTO quoteWsDTO = new QuoteWsDTO();
		assertThatThrownBy(() -> populator.populate(null, quoteWsDTO))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("cartModel cannot be null");
	}

	@Test
	public void testPopulateWithNullQuoteWsDTO()
	{
		assertThatThrownBy(() -> populator.populate(cartModel, null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("quoteWsDTO cannot be null");
	}

	@Test
	public void testPopulateWithoutCartData()
	{
		when(cartConverter.convert(any())).thenReturn(null);

		final QuoteWsDTO quoteWsDTO = spy(new QuoteWsDTO());
		populator.populate(cartModel, quoteWsDTO);

		verifyNoMoreInteractions(quoteWsDTO);
	}
}
