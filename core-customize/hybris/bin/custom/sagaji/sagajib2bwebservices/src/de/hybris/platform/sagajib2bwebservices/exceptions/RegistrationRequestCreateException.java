/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.exceptions;

public class RegistrationRequestCreateException extends RuntimeException
{
	public RegistrationRequestCreateException(final String message)
	{
		super(message);
	}

	public RegistrationRequestCreateException(final String message, final Throwable t)
	{
		super(message, t);
	}
}
