/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.skipfield;

import com.google.common.collect.Lists;
import de.hybris.platform.commercefacades.order.EntryGroupData;
import de.hybris.platform.commercefacades.quote.data.QuoteData;
import de.hybris.platform.commercefacades.quote.data.QuoteListData;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteListWsDTO;
import de.hybris.platform.commercewebservicescommons.skipfield.AbstractSkipFieldValueSetter;

import java.util.List;

import de.hybris.platform.commercefacades.data.SAPAttachmentData;
import org.apache.commons.collections.CollectionUtils;

import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_ORDER_ENTRYGROUPS;
import static de.hybris.platform.commercefacades.order.converters.populator.AbstractOrderPopulator.SKIP_SAPATTACHMENTS;


public class SkipQuoteListFieldValueSetter extends AbstractSkipFieldValueSetter
{
	@Override
	public void setValue(final String fields)
	{
		final QuoteListData listData = new QuoteListData();
		final QuoteData quoteData = new QuoteData();
		final List<SAPAttachmentData> sapAttachmentData= Lists.newArrayList(new SAPAttachmentData());
		quoteData.setSapAttachments(sapAttachmentData);
		quoteData.setRootGroups(List.of(new EntryGroupData()));
		listData.setQuotes(List.of(quoteData));
		final QuoteListWsDTO listWsDTO = getDataMapper().map(listData, QuoteListWsDTO.class, fields);
		final boolean skipSAPAttachments = org.springframework.util.CollectionUtils.isEmpty(listWsDTO.getQuotes().get(0).getSapAttachments());
		getSessionService().setAttribute(SKIP_SAPATTACHMENTS, skipSAPAttachments);
		final boolean skipEntryGroups = CollectionUtils.isEmpty(listWsDTO.getQuotes().get(0).getEntryGroups());
		getSessionService().setAttribute(SKIP_ORDER_ENTRYGROUPS, skipEntryGroups);
	}
}
