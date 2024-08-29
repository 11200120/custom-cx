/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import de.hybris.platform.b2bacceleratorfacades.order.data.ScheduledCartData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderData;
import de.hybris.platform.b2bwebservicescommons.dto.order.ReplenishmentOrderListWsDTO;
import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;


public class SkipReplenishmentOrderListFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final ReplenishmentOrderData replenishmentOrderData = new ReplenishmentOrderData();
		final ScheduledCartData cartData = new ScheduledCartData();
		cartData.setRootGroups(List.of(new EntryGroupData()));
		replenishmentOrderData.setReplenishmentOrders(List.of(cartData));
		final ReplenishmentOrderListWsDTO orderListWsDTO = getDataMapper().map(replenishmentOrderData, ReplenishmentOrderListWsDTO.class, fields);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(orderListWsDTO.getReplenishmentOrders().get(0).getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
