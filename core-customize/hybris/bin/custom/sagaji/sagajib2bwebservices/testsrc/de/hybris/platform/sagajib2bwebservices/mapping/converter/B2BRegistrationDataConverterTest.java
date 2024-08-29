/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.sagajib2bwebservices.mapping.converter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.b2bcommercefacades.data.B2BRegistrationData;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUserRegistrationDataWsDTO;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;
import ma.glasnost.orika.metadata.TypeFactory;


/**
 * Test suite for {@link B2BRegistrationDataConverterTest}
 */
@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class B2BRegistrationDataConverterTest
{
	private static final String FIRST_NAME = "FirstName";
	private static final String LAST_NAME = "LastName";
	private static final String MESSAGE = "message";
	private static final String TITLE_CODE = "titleCode";
	private static final String EMAIL = "email@test.com";

	@Mock
	private MappingContext mappingContext;
	@InjectMocks
	private B2BRegistrationDataConverter b2BRegistrationDataConverter;

	@Test
	public void testConvertNormalData()
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = setupRegistrationData(FIRST_NAME, LAST_NAME);
		B2BRegistrationData result = b2BRegistrationDataConverter.convert(registrationDataWsDTO, TypeFactory.valueOf(B2BRegistrationData.class), mappingContext);

		verifyNoMoreInteractions(mappingContext);
		assertThat(result.getName()).isEqualTo(FIRST_NAME + " " + LAST_NAME);
		assertThat(result.getEmail()).isEqualTo(EMAIL);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getTitleCode()).isEqualTo(TITLE_CODE);
	}

	@Test
	public void testConvertDataWithEmptyFirstName()
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = setupRegistrationData("", LAST_NAME);
		B2BRegistrationData result = b2BRegistrationDataConverter.convert(registrationDataWsDTO, TypeFactory.valueOf(B2BRegistrationData.class), mappingContext);

		verifyNoMoreInteractions(mappingContext);
		assertThat(result.getName()).isEqualTo(LAST_NAME);
		assertThat(result.getEmail()).isEqualTo(EMAIL);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getTitleCode()).isEqualTo(TITLE_CODE);
	}

	@Test
	public void testConvertDataWithoutFirstName()
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = setupRegistrationData(null, LAST_NAME);
		B2BRegistrationData result = b2BRegistrationDataConverter.convert(registrationDataWsDTO, TypeFactory.valueOf(B2BRegistrationData.class), mappingContext);

		verifyNoMoreInteractions(mappingContext);
		assertThat(result.getName()).isEqualTo(LAST_NAME);
		assertThat(result.getEmail()).isEqualTo(EMAIL);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getTitleCode()).isEqualTo(TITLE_CODE);
	}

	@Test
	public void testConvertDataWithEmptyLastName()
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = setupRegistrationData(FIRST_NAME, "");

		B2BRegistrationData result = b2BRegistrationDataConverter.convert(registrationDataWsDTO, TypeFactory.valueOf(B2BRegistrationData.class), mappingContext);

		verifyNoMoreInteractions(mappingContext);
		assertThat(result.getName()).isEqualTo(FIRST_NAME);
		assertThat(result.getEmail()).isEqualTo(EMAIL);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getTitleCode()).isEqualTo(TITLE_CODE);
	}

	@Test
	public void testConvertDataWithoutLastName()
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = setupRegistrationData(FIRST_NAME, null);

		B2BRegistrationData result = b2BRegistrationDataConverter.convert(registrationDataWsDTO, TypeFactory.valueOf(B2BRegistrationData.class), mappingContext);

		verifyNoMoreInteractions(mappingContext);
		assertThat(result.getName()).isEqualTo(FIRST_NAME);
		assertThat(result.getEmail()).isEqualTo(EMAIL);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getTitleCode()).isEqualTo(TITLE_CODE);
	}

	@Test
	public void testConvertDataWithEmptyName()
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = setupRegistrationData(null, "");

		B2BRegistrationData result = b2BRegistrationDataConverter.convert(registrationDataWsDTO, TypeFactory.valueOf(B2BRegistrationData.class), mappingContext);

		verifyNoMoreInteractions(mappingContext);
		assertThat(result.getName()).isEmpty();
		assertThat(result.getEmail()).isEqualTo(EMAIL);
		assertThat(result.getMessage()).isEqualTo(MESSAGE);
		assertThat(result.getTitleCode()).isEqualTo(TITLE_CODE);
	}

	private OrgUserRegistrationDataWsDTO setupRegistrationData(String firstName, String lastName)
	{
		OrgUserRegistrationDataWsDTO registrationDataWsDTO = new OrgUserRegistrationDataWsDTO();
		registrationDataWsDTO.setFirstName(firstName);
		registrationDataWsDTO.setLastName(lastName);
		registrationDataWsDTO.setMessage(MESSAGE);
		registrationDataWsDTO.setEmail(EMAIL);
		registrationDataWsDTO.setTitleCode(TITLE_CODE);
		return registrationDataWsDTO;
	}

	@Test
	public void testEquals()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();
		final B2BRegistrationDataConverter converter2 = new B2BRegistrationDataConverter();

		assertThat(converter1).isEqualTo(converter2);
	}

	@Test
	public void testEqualsWithSameConverter()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();

		assertThat(converter1).isEqualTo(converter1);
	}

	@Test
	public void testEqualsWithDifferentConverter()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();
		final CustomConverter<OrgUserRegistrationDataWsDTO, B2BRegistrationData> converter2 = new CustomConverter<>()
		{
			@Override
			public B2BRegistrationData convert(final OrgUserRegistrationDataWsDTO source, final Type<? extends B2BRegistrationData> destinationType, final MappingContext mappingContext)
			{
				return null;
			}
		};

		assertThat(converter1).isNotEqualTo(converter2);
	}

	@Test
	public void testEqualsWithNullConverter()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();

		assertThat(converter1).isNotEqualTo(null);
	}

	@Test
	public void testHashCode()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();
		final B2BRegistrationDataConverter converter2 = new B2BRegistrationDataConverter();

		assertThat(converter1.hashCode()).isEqualTo(converter2.hashCode());
	}

	@Test
	public void testHashCodesWithSameConverter()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();

		assertThat(converter1.hashCode()).isEqualTo(converter1.hashCode());
	}

	@Test
	public void testHashCodeWithDifferentConverter()
	{
		final B2BRegistrationDataConverter converter1 = new B2BRegistrationDataConverter();
		final CustomConverter<OrgUserRegistrationDataWsDTO, B2BRegistrationData> converter2 = new CustomConverter<>()
		{
			@Override
			public B2BRegistrationData convert(final OrgUserRegistrationDataWsDTO source, final Type<? extends B2BRegistrationData> destinationType, final MappingContext mappingContext)
			{
				return null;
			}
		};

		assertThat(converter1.hashCode()).isNotEqualTo(converter2.hashCode());
	}
}
