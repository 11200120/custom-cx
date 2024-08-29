/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.sagajib2bwebservices.v2.helper;

import com.google.common.base.Strings;
import de.hybris.platform.b2bacceleratorfacades.order.B2BOrderFacade;
import de.hybris.platform.sagajib2bwebservices.util.DefaultSearchQueryCodec;
import de.hybris.platform.commercefacades.order.data.OrderHistoriesData;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commercefacades.search.data.SearchQueryData;
import de.hybris.platform.commerceservices.search.facetdata.FacetData;
import de.hybris.platform.commerceservices.search.facetdata.FacetValueData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchQueryData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchQueryTermData;
import de.hybris.platform.commercewebservicescommons.dto.order.OrderHistoryListWsDTO;
import de.hybris.platform.core.enums.OrderStatus;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static org.apache.commons.collections4.ListUtils.emptyIfNull;


@Component
public class B2BOrdersHelper extends AbstractHelper
{
	@Resource(name = "b2bOrderFacade")
	private B2BOrderFacade b2BOrderFacade;

	public OrderHistoryListWsDTO searchBranchOrderHistory(final String statuses, final String filters, final int currentPage, final int pageSize,
			final String sort, final String fields)
	{
		final OrderHistoriesData orderHistoriesData = searchBranchOrderHistory(statuses, filters, currentPage, pageSize, sort);
		return getDataMapper().map(orderHistoriesData, OrderHistoryListWsDTO.class, fields);
	}

	public OrderHistoriesData searchBranchOrderHistory(final String statuses, final String filters,
			final int currentPage, final int pageSize, final String sort)
	{
		var orderStatuses = extractOrderStatuses(statuses);
		var decodedFilters = new DefaultSearchQueryCodec().decodeQuery(filters);
		var pageableData = createPageableData(currentPage, pageSize, sort);
		var facetsFromFilters = getFacetsFromFilters(decodedFilters);
		var orderHistory = b2BOrderFacade.getPagedBranchOrderHistoryForStatuses(pageableData, decodedFilters, orderStatuses);

		return createOrderHistoriesData(orderHistory, facetsFromFilters);
	}

	protected OrderStatus[] extractOrderStatuses(final String statuses)
	{
		return Arrays.stream(Strings.nullToEmpty(statuses).split(","))
				.filter(not(String::isBlank))
				.map(String::trim)
				.distinct()
				.map(OrderStatus::valueOf)
				.toArray(OrderStatus[]::new);
	}

	protected OrderHistoriesData createOrderHistoriesData(final SearchPageData<OrderHistoryData> result, List<FacetData<SearchQueryData>> facets) {
		final OrderHistoriesData orderHistoriesData = new OrderHistoriesData();

		orderHistoriesData.setOrders(result.getResults());
		orderHistoriesData.setSorts(result.getSorts());
		orderHistoriesData.setPagination(result.getPagination());
		orderHistoriesData.setFacets(facets);

		return orderHistoriesData;
	}

	private List<FacetData<SearchQueryData>> getFacetsFromFilters(SolrSearchQueryData filters) {
		return emptyIfNull(filters.getFilterTerms())
				.stream()
				.map(B2BOrdersHelper::mapFilterTermToFacet)
				.collect(Collectors.toList());
	}

	private static FacetData<SearchQueryData> mapFilterTermToFacet(SolrSearchQueryTermData searchTerm) {
		var facet = new FacetData<SearchQueryData>();
		var facetValue = new FacetValueData<SearchQueryData>();
		facetValue.setName(searchTerm.getValue());
		facet.setName(searchTerm.getKey());
		facet.setValues(Arrays.asList(facetValue));
		return facet;
	}
}
