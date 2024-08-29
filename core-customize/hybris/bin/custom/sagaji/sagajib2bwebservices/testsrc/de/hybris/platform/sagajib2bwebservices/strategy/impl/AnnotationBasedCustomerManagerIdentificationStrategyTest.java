/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.strategy.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.assistedserviceservices.utils.AssistedServiceSession;
import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.core.PK;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.servicelayer.session.SessionService;
import de.hybris.platform.servicelayer.user.UserService;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class AnnotationBasedCustomerManagerIdentificationStrategyTest
{
	@Mock
	private UserService userService;
	@Mock
	private SessionService sessionService;
	@Mock
	private Authentication authentication;
	@Mock
	private SecurityContext securityContext;
	@Mock
	private UserModel userModel;

	@InjectMocks
	private AnnotationBasedCustomerManagerIdentificationStrategy strategy;

	@Captor
	private ArgumentCaptor<AssistedServiceSession> asmSessionCaptor;

	private final PK pk = PK.fromLong(1234);

	@Before
	public void setUp()
	{
		when(securityContext.getAuthentication()).thenReturn(authentication);
		SecurityContextHolder.setContext(securityContext);

		when(userModel.getPk()).thenReturn(pk);

		when(authentication.getPrincipal()).thenReturn("MY_USER_PRINCIPAL");
		when(userService.getUserForUID("MY_USER_PRINCIPAL")).thenReturn(userModel);
	}

	@Test
	public void testSetASMSessionForAnnotation4ROLE_CUSTOMERMANAGERGROUP()
	{
		final Secured securedAnnotation = mock(Secured.class);
		when(securedAnnotation.value()).thenReturn(
				new String[] { SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP });
		strategy.setASMSessionForAnnotation(securedAnnotation);

		verify(securityContext).getAuthentication();
	}

	@Test
	public void testNotSetASMSessionForAnnotation4ROLE_CUSTOMERGROUP()
	{
		final Secured securedAnnotation = mock(Secured.class);
		when(securedAnnotation.value()).thenReturn(new String[] { SecuredAccessConstants.ROLE_CUSTOMERGROUP });
		strategy.setASMSessionForAnnotation(securedAnnotation);

		verifyNoInteractions(userService);
		verifyNoInteractions(sessionService);
	}

	@Test
	public void testSetSessionForCustomerManager()
	{
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMERMANAGERGROUP"))).when(authentication).getAuthorities();

		strategy.setASMSessionAfterLogin();

		verify(userService).getUserForUID("MY_USER_PRINCIPAL");
		verify(sessionService).setAttribute(eq("ASM"), asmSessionCaptor.capture());

		assertThat(asmSessionCaptor.getValue()).hasFieldOrPropertyWithValue("agentPk", pk);
	}

	@Test
	public void testSetSessionForCustomerManagerShouldNotApplyForAnotherRole()
	{
		doReturn(List.of(new SimpleGrantedAuthority("ROLE_DIFFERENT"))).when(authentication).getAuthorities();

		strategy.setASMSessionAfterLogin();

		verifyNoInteractions(userService);
		verifyNoInteractions(sessionService);
	}
}
