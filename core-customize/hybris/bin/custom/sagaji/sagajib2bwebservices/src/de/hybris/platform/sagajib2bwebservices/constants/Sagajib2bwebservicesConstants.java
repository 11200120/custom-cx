/*
 *  
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.constants;

@SuppressWarnings({"deprecation","squid:CallToDeprecatedMethod"})
public class Sagajib2bwebservicesConstants extends GeneratedSagajib2bwebservicesConstants
{
	public static final String EXTENSIONNAME = "sagajib2bwebservices";

	public static final String OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH = "#{ ${occ.rewrite.overlapping.paths.enabled:false} ? '/{baseSiteId}/orgUsers/{userId}' : '/{baseSiteId}/users/{userId}'}";
	public static final String OCC_REWRITE_OVERLAPPING_PRODUCTS_PATH = "#{ ${occ.rewrite.overlapping.paths.enabled:false} ? '/{baseSiteId}/orgProducts' : '/{baseSiteId}/products'}";

	private Sagajib2bwebservicesConstants()
	{
		//empty
	}
	
	
}
