/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.strategy.impl;

import static de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP;

import de.hybris.platform.assistedserviceservices.constants.AssistedserviceservicesConstants;
import de.hybris.platform.assistedserviceservices.utils.AssistedServiceSession;
import de.hybris.platform.sagajib2bwebservices.strategy.CustomerManagerIdentificationStrategy;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;

import java.util.Arrays;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class AnnotationBasedCustomerManagerIdentificationStrategy implements CustomerManagerIdentificationStrategy
{
	protected UserService userService;
	protected SessionService sessionService;

	public void setASMSessionForAnnotation(final Secured securedAnnotation)
	{
		final boolean customerManagerHasAccess = Arrays.asList(securedAnnotation.value()).contains(ROLE_CUSTOMERMANAGERGROUP);
		if(customerManagerHasAccess)
		{
			setASMSessionAfterLogin();
		}
	}

	// This is a workaround to make the endpoint return the correct data to a customer manager.
	// Because QuoteFacade uses QuoteUserIdentificationStrategy to get current quote user
	// and getQuoteUserIdentificationStrategy().getCurrentQuoteUser() returns the ASM agent only if there is a ASM session.
	public void setASMSessionAfterLogin()
	{
		final Authentication auth = getAuth();
		if (hasRole(ROLE_CUSTOMERMANAGERGROUP, auth))
		{
			final AssistedServiceSession asmSession = new AssistedServiceSession();
			asmSession.setAgent(getUserService().getUserForUID(auth.getPrincipal().toString()));
			getSessionService().setAttribute(AssistedserviceservicesConstants.ASM_SESSION_PARAMETER, asmSession);
		}
	}

	public void setUserService(final UserService userService)
	{
		this.userService = userService;
	}

	public void setSessionService(final SessionService sessionService)
	{
		this.sessionService = sessionService;
	}

	protected Authentication getAuth()
	{
		return SecurityContextHolder.getContext().getAuthentication();
	}

	protected boolean hasRole(final String role, final Authentication auth)
	{
		if (auth != null)
		{
			for (final GrantedAuthority ga : auth.getAuthorities())
			{
				if (ga.getAuthority().equals(role))
				{
					return true;
				}
			}
		}
		return false;
	}

	public UserService getUserService()
	{
		return userService;
	}

	public SessionService getSessionService()
	{
		return sessionService;
	}
}
