/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.order.data.OrderData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipOrderFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final OrderData orderData = new OrderData();
		orderData.setRootGroups(List.of(new EntryGroupData()));
		final OrderWsDTO orderWsDTO = getDataMapper().map(orderData, OrderWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(orderWsDTO.getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
