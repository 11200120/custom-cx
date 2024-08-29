/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import com.google.common.collect.Lists;
import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.quote.data.QuoteData;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;
import de.hybris.platform.commercefacades.data.SAPAttachmentData;
import org.apache.commons.collections.CollectionUtils;

import java.util.List;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_SAPATTACHMENTS;


public class SkipQuoteFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final QuoteData quoteData = new QuoteData();
		final List<SAPAttachmentData> sapAttachmentData= Lists.newArrayList(new SAPAttachmentData());
		quoteData.setSapAttachments(sapAttachmentData);
		quoteData.setRootGroups(List.of(new EntryGroupData()));
		final QuoteWsDTO quoteWsDTO = getDataMapper().map(quoteData, QuoteWsDTO.class, fields);
		final boolean skipSAPAttachments = org.springframework.util.CollectionUtils.isEmpty(quoteWsDTO.getSapAttachments());
		final boolean skipEntryGroups = CollectionUtils.isEmpty(quoteWsDTO.getEntryGroups());
		getSessionService().setAttribute(SKIP_SAPATTACHMENTS, skipSAPAttachments);
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
