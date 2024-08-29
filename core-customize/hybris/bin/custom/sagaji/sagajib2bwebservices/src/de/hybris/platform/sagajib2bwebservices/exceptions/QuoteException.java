/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.exceptions;

/**
 * Generic Exception for Quotes. Mainly intended for generating the right REST status codes. It should be used
 * for generating BAD_REQUEST.
 */
public class QuoteException extends RuntimeException
{
	public QuoteException(final String message)
	{
		super(message);
	}

	public QuoteException(final String message, final Throwable t)
	{
		super(message, t);
	}
}
