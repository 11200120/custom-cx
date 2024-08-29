/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderWsDTO;

import org.junit.Test;
import org.mockito.InjectMocks;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class SkipReplenishmentOrderFieldValueSetterTest extends BaseSkipFieldTest
{
	@InjectMocks
	private SkipReplenishmentOrderFieldValueSetter skipReplenishmentOrderFieldValueSetter;

	@Test
	public void testSetValue()
	{
		final ReplenishmentOrderWsDTO replenishmentOrderWsDTO = new ReplenishmentOrderWsDTO();
		when(dataMapper.map(anyObject(), eq(ReplenishmentOrderWsDTO.class), anyString())).thenReturn(replenishmentOrderWsDTO);
		skipReplenishmentOrderFieldValueSetter.setValue(FIELD_ENTRIES);
		verify(sessionService).setAttribute(SKIP_ORDER_ENTRYGROUPS, true);
	}

}
