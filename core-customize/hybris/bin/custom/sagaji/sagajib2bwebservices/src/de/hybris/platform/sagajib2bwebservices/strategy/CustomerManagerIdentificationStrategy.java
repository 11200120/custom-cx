/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.strategy;

import org.springframework.security.access.annotation.Secured;


public interface CustomerManagerIdentificationStrategy
{
	/**
	 * Helps the endpoint to return the correct data to a customer manager.
	 */
	void setASMSessionAfterLogin();

	/**
	 * Helps the endpoint to return the correct data if access is allowed to a customer manager.
	 */
	void setASMSessionForAnnotation(final Secured securedAnnotation);
}
