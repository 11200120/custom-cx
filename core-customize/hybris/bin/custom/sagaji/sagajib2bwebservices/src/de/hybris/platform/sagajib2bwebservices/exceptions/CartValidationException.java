/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.sagajib2bwebservices.exceptions;

import de.hybris.platform.commercefacades.order.data.CartModificationData;

import java.util.List;

public class CartValidationException extends RuntimeException
{
	private final List<CartModificationData> modifications;

	public CartValidationException(final List<CartModificationData> modifications)
	{
		super();
		this.modifications = modifications;
	}

	public List<CartModificationData> getModifications()
	{
		return this.modifications;
	}
}
