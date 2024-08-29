/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.b2bacceleratorfacades.exception.CustomerAlreadyExistsException;
import de.hybris.platform.b2bacceleratorfacades.exception.RegistrationNotEnabledException;
import de.hybris.platform.b2bacceleratorfacades.registration.B2BRegistrationFacade;
import de.hybris.platform.b2bcommercefacades.data.B2BRegistrationData;
import de.hybris.platform.sagajib2bwebservices.exceptions.B2BRegistrationException;
import de.hybris.platform.sagajib2bwebservices.exceptions.RegistrationRequestCreateException;
import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUserRegistrationDataWsDTO;
import de.hybris.platform.commercefacades.customer.CustomerFacade;
import de.hybris.platform.commercefacades.user.data.CustomerData;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commerceservices.request.mapping.annotation.RequestMappingOverride;
import de.hybris.platform.commercewebservicescommons.annotation.CaptchaAware;
import de.hybris.platform.commercewebservicescommons.annotation.SecurePortalUnauthenticatedAccess;
import de.hybris.platform.commercewebservicescommons.annotation.SiteChannelRestriction;
import de.hybris.platform.commercewebservicescommons.constants.CommercewebservicescommonsConstants;
import de.hybris.platform.commercewebservicescommons.dto.user.UserWsDTO;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.util.Config;
import de.hybris.platform.webservicescommons.dto.error.ErrorListWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.AlreadyExistsException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.annotation.Secured;
import org.springframework.stereotype.Controller;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import static de.hybris.platform.sagajib2bwebservices.constants.Sagajib2bwebservicesConstants.OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH;
import static org.springframework.http.HttpStatus.CREATED;


/**
 * Main Controller for Users
 */
@Controller
@ApiVersion("v2")
@Tag(name = "B2B Users")
public class B2BUsersController extends BaseController
{
	private static final Logger LOG = LoggerFactory.getLogger(B2BUsersController.class);
	protected static final String API_COMPATIBILITY_B2B_CHANNELS = "api.compatibility.b2b.channels";
	private static final String USER_ALREADY_EXISTS_ERROR_KEY = "User already exists";
	private static final String REGISTRATION_NOT_ENABLED_ERROR_KEY = "Registration is not enabled";

	@Resource(name = "b2bCustomerFacade")
	private CustomerFacade customerFacade;
	@Resource(name = "dataMapper")
	protected DataMapper dataMapper;
	@Resource
	private B2BRegistrationFacade b2bRegistrationFacade;
	@Resource
	private Validator orgUserRegistrationDataValidator;
	@Resource
	private Validator orgUserRegistrationNameValidator;

	@Secured({ SecuredAccessConstants.ROLE_CUSTOMERGROUP, SecuredAccessConstants.ROLE_TRUSTED_CLIENT,
			SecuredAccessConstants.ROLE_CUSTOMERMANAGERGROUP })
	@GetMapping(value = OCC_REWRITE_OVERLAPPING_BASE_SITE_USER_PATH)
	@RequestMappingOverride(priorityProperty = "b2bocc.B2BUsersController.getUser.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@ResponseBody
	@Operation(operationId = "getOrgUser", summary = "Retrieves a B2B customer profile.")
	@ApiBaseSiteIdAndUserIdParam
	public UserWsDTO getOrgUser(
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final CustomerData customerData = customerFacade.getCurrentCustomer();
		final UserWsDTO dto = dataMapper.map(customerData, UserWsDTO.class, fields);
		return dto;
	}

	@SecurePortalUnauthenticatedAccess
	@PostMapping(value = "/{baseSiteId}/orgUsers", consumes = MediaType.APPLICATION_JSON)
	@ResponseStatus(CREATED)
	@RequestMappingOverride(priorityProperty = "b2bocc.B2BUsersController.getUser.priority")
	@SiteChannelRestriction(allowedSiteChannelsProperty = API_COMPATIBILITY_B2B_CHANNELS)
	@Operation(operationId = "createRegistrationRequest", summary = "Creates a registration request for a B2B customer.")	@ApiBaseSiteIdParam
	@Parameter(name = CommercewebservicescommonsConstants.CAPTCHA_TOKEN_HEADER, description = CommercewebservicescommonsConstants.CAPTCHA_TOKEN_HEADER_DESC, schema = @Schema(type = "string"), in = ParameterIn.HEADER)
	@CaptchaAware
	public void createRegistrationRequest(
			@Parameter(description = "Data object that contains information necessary to apply user registration", required = true) @RequestBody final OrgUserRegistrationDataWsDTO orgUserRegistrationData,
			@ApiFieldsParam @RequestParam(defaultValue = DEFAULT_FIELD_SET) final String fields)
	{
		validate(orgUserRegistrationData, "orgUserRegistrationData", getOrgUserRegistrationDataValidator());
		final var b2BRegistrationData = getDataMapper().map(orgUserRegistrationData, B2BRegistrationData.class);
		validate(b2BRegistrationData, "orgUserRegistrationData", getOrgUserRegistrationNameValidator());
		try
		{
			b2bRegistrationFacade.register(b2BRegistrationData);
		}
		catch (final CustomerAlreadyExistsException e)
		{
			if (isSecureCustomerRegistrationEnabled())
			{
				LOG.info("Account already exists, ignore it.");
			}
			else
			{
				throw new AlreadyExistsException(USER_ALREADY_EXISTS_ERROR_KEY);
			}
		}
		catch (final RegistrationNotEnabledException e)
		{
			throw new RegistrationRequestCreateException(REGISTRATION_NOT_ENABLED_ERROR_KEY, e);
		}
		catch (final UnknownIdentifierException e)
		{
			throw new RegistrationRequestCreateException(e.getMessage(), e);
		}
		catch (final RuntimeException re)
		{
			throw new B2BRegistrationException("Encountered an error when creating a registration request", re);
		}
	}

	@ResponseStatus(value = HttpStatus.BAD_REQUEST)
	@ResponseBody
	@ExceptionHandler({ RegistrationRequestCreateException.class })
	public ErrorListWsDTO handleRegistrationRequestCreateException(final Throwable ex)
	{
		if (LOG.isErrorEnabled())
		{
			LOG.error(sanitize(ex.getMessage()), ex);
		}
		return handleErrorInternal(ex.getClass().getSimpleName(), ex.getMessage());
	}

	@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
	@ResponseBody
	@ExceptionHandler({ B2BRegistrationException.class })
	public ErrorListWsDTO handleB2BRegistrationException(final Throwable ex)
	{
		if (LOG.isErrorEnabled())
		{
			LOG.error(sanitize(ex.getMessage()), ex);
		}
		return handleErrorInternal(ex.getClass().getSimpleName(), ex.getMessage());
	}

	public Validator getOrgUserRegistrationDataValidator()
	{
		return orgUserRegistrationDataValidator;
	}

	public void setOrgUserRegistrationDataValidator(final Validator orgUserRegistrationDataValidator)
	{
		this.orgUserRegistrationDataValidator = orgUserRegistrationDataValidator;
	}

	public Validator getOrgUserRegistrationNameValidator()
	{
		return orgUserRegistrationNameValidator;
	}

	public void setOrgUserRegistrationNameValidator(final Validator orgUserRegistrationNameValidator)
	{
		this.orgUserRegistrationNameValidator = orgUserRegistrationNameValidator;
	}

	private boolean isSecureCustomerRegistrationEnabled(){
		return Config.getBoolean("toggle.secure.customer.registration.enabled", false);
	}
}
