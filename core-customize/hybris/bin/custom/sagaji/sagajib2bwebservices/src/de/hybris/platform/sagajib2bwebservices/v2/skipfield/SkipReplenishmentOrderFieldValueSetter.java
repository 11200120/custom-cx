/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderWsDTO;
import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipReplenishmentOrderFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final ScheduledCartData cartData = new ScheduledCartData();
		cartData.setRootGroups(List.of(new EntryGroupData()));
		final ReplenishmentOrderWsDTO orderWsDTO = getDataMapper().map(cartData, ReplenishmentOrderWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(orderWsDTO.getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
