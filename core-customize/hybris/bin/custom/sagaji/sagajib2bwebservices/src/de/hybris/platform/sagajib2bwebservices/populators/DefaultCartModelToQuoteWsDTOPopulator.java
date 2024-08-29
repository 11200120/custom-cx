/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.populators;

import de.hybris.platform.commercefacades.order.data.CartData;
import de.hybris.platform.commercewebservicescommons.dto.comments.CommentWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderEntryWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.product.PriceWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteWsDTO;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.servicelayer.dto.converter.Converter;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import java.util.List;

import com.google.common.base.Preconditions;

public class DefaultCartModelToQuoteWsDTOPopulator implements Populator<CartModel, QuoteWsDTO>
{
	private DataMapper dataMapper;
	private Converter<CartModel, CartData> cartConverter;

	@Override
	public void populate(final CartModel cartModel, final QuoteWsDTO quoteWsDTO)
	{
		Preconditions.checkArgument(cartModel != null, "cartModel cannot be null");
		Preconditions.checkArgument(quoteWsDTO != null, "quoteWsDTO cannot be null");

		final CartData cartData = getCartConverter().convert(cartModel);
		if( cartData == null )
		{
			return;
		}

		quoteWsDTO.setName(cartData.getName());
		quoteWsDTO.setDescription(cartData.getDescription());
		final List<CommentWsDTO> comments = getDataMapper().mapAsList(cartData.getComments(), CommentWsDTO.class, "FULL");
		quoteWsDTO.setComments(comments);
		quoteWsDTO.setCartId(cartData.getCode());
		final List<OrderEntryWsDTO> entries = getDataMapper().mapAsList(cartData.getEntries(), OrderEntryWsDTO.class, null);
		quoteWsDTO.setEntries(entries);
		final PriceWsDTO quoteDiscounts = getDataMapper().map(cartData.getQuoteDiscounts(), PriceWsDTO.class, "FULL");
		quoteWsDTO.setQuoteDiscounts(quoteDiscounts);
		final PriceWsDTO orderDiscounts = getDataMapper().map(cartData.getOrderDiscounts(), PriceWsDTO.class, "FULL");
		quoteWsDTO.setOrderDiscounts(orderDiscounts);
		quoteWsDTO.setTotalItems(cartData.getTotalItems());
		final PriceWsDTO totalPrice = getDataMapper().map(cartData.getTotalPrice(), PriceWsDTO.class, "FULL");
		quoteWsDTO.setTotalPrice(totalPrice);
		final PriceWsDTO totalPriceWithTax = getDataMapper().map(cartData.getTotalPriceWithTax(), PriceWsDTO.class, "FULL");
		quoteWsDTO.setTotalPriceWithTax(totalPriceWithTax);
		quoteWsDTO.setUpdatedTime(cartModel.getModifiedtime());
		quoteWsDTO.setExpirationTime(cartModel.getExpirationTime());
		quoteWsDTO.setSapQuoteDiscountsType(cartData.getQuoteDiscountsType());
		quoteWsDTO.setSapQuoteDiscountsRate(cartData.getQuoteDiscountsRate());
	}

	public DataMapper getDataMapper()
	{
		return dataMapper;
	}

	public void setDataMapper(final DataMapper dataMapper)
	{
		this.dataMapper = dataMapper;
	}

	public Converter<CartModel, CartData> getCartConverter()
	{
		return cartConverter;
	}

	public void setCartConverter(final Converter<CartModel, CartData> cartConverter)
	{
		this.cartConverter = cartConverter;
	}
}
