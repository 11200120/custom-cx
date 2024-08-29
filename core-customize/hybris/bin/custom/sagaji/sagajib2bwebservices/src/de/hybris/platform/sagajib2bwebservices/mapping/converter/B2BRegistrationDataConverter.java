/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.mapping.converter;

import de.hybris.platform.b2bcommercefacades.data.B2BRegistrationData;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUserRegistrationDataWsDTO;
import de.hybris.platform.webservicescommons.mapping.WsDTOMapping;

import org.apache.commons.lang.StringUtils;

import ma.glasnost.orika.CustomConverter;
import ma.glasnost.orika.MappingContext;
import ma.glasnost.orika.metadata.Type;


@WsDTOMapping
public class B2BRegistrationDataConverter extends CustomConverter<OrgUserRegistrationDataWsDTO, B2BRegistrationData>
{
	@Override
	public B2BRegistrationData convert(final OrgUserRegistrationDataWsDTO orgUserRegistrationDataWsDTO,
			final Type<? extends B2BRegistrationData> type, final MappingContext mappingContext)
	{
		final var b2BRegistrationData = new B2BRegistrationData();
		b2BRegistrationData.setName(StringUtils
				.trim(StringUtils.trimToEmpty(orgUserRegistrationDataWsDTO.getFirstName()) + " " + StringUtils
						.trimToEmpty(orgUserRegistrationDataWsDTO.getLastName())));
		b2BRegistrationData.setEmail(orgUserRegistrationDataWsDTO.getEmail());
		b2BRegistrationData.setTitleCode(orgUserRegistrationDataWsDTO.getTitleCode());
		b2BRegistrationData.setMessage(orgUserRegistrationDataWsDTO.getMessage());
		return b2BRegistrationData;
	}

	@Override
	public boolean equals(final Object o)
	{
		if (this == o)
		{
			return true;
		}

		return o != null && getClass() == o.getClass();
	}

	@Override
	public int hashCode()
	{
		return getClass().hashCode();
	}
}
