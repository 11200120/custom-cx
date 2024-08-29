/*
 * Copyright (c) 2020 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.util;

import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchQueryData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchQueryTermData;
import org.apache.logging.log4j.util.Strings;

import java.util.ArrayList;
import java.util.List;


public class DefaultSearchQueryCodec implements SearchQueryCodec<SolrSearchQueryData>
{
	protected static final int NEXT_TERM = 2;
	
	@Override
	public SolrSearchQueryData decodeQuery(final String queryString)
	{
		final SolrSearchQueryData searchQuery = new SolrSearchQueryData();
		searchQuery.setFilterTerms(new ArrayList<>());

		if (Strings.isBlank(queryString))
		{
			return searchQuery;
		}

		final String[] parts = queryString.split(":");

		if (parts.length > 0)
		{
			searchQuery.setFreeTextSearch(parts[0]);
			if (parts.length > 1)
			{
				searchQuery.setSort(parts[1]);
			}
		}

		for (int i = NEXT_TERM; i < parts.length; i = i + NEXT_TERM)
		{
			final SolrSearchQueryTermData term = new SolrSearchQueryTermData();
			term.setKey(parts[i]);
			term.setValue(parts[i + 1]);
			searchQuery.getFilterTerms().add(term);
		}

		return searchQuery;
	}

	@Override
	public String encodeQuery(final SolrSearchQueryData searchQueryData)
	{
		if (searchQueryData == null)
		{
			return null;
		}

		final StringBuilder builder = new StringBuilder();
		builder.append((searchQueryData.getFreeTextSearch() == null) ? "" : searchQueryData.getFreeTextSearch());


		if (searchQueryData.getSort() != null //
				|| (searchQueryData.getFilterTerms() != null && !searchQueryData.getFilterTerms().isEmpty()))
		{
			builder.append(":");
			builder.append((searchQueryData.getSort() == null) ? "" : searchQueryData.getSort());
		}

		final List<SolrSearchQueryTermData> terms = searchQueryData.getFilterTerms();
		if (terms != null && !terms.isEmpty())
		{
			for (final SolrSearchQueryTermData term : searchQueryData.getFilterTerms())
			{
				builder.append(":");
				builder.append(term.getKey());
				builder.append(":");
				builder.append(term.getValue());
			}
		}

		//URLEncode?
		return builder.toString();
	}
}
