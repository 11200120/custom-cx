/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import de.hybris.platform.commercefacades.product.data.ProductData;
import de.hybris.platform.commercefacades.search.ProductSearchFacade;
import de.hybris.platform.commercefacades.search.data.SearchQueryData;
import de.hybris.platform.commercefacades.search.data.SearchStateData;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commerceservices.search.facetdata.ProductCategorySearchPageData;
import de.hybris.platform.commerceservices.search.facetdata.ProductSearchPageData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductCategorySearchPageWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.search.facetdata.ProductSearchPageWsDTO;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetBuilder;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;

import javax.annotation.Resource;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;


@Controller
@RequestMapping(value = "/{baseSiteId}/categories")
@ApiVersion("v2")
@Tag(name = "B2B Categories")
public class B2BCategoriesController extends BaseController
{
	private static final String DEFAULT_PAGE_VALUE = "0";

	@Resource(name = "productSearchFacade")
	private ProductSearchFacade<ProductData> solrProductSearchFacade;

	@Resource(name = "fieldSetBuilder")
	private FieldSetBuilder fieldSetBuilder;

	@Resource(name = "dataMapper")
	protected DataMapper dataMapper;


	@GetMapping(value = "/{categoryId}/products")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	@Operation(operationId = "getProductsByCategory", summary = "Retrieves a list of products for a category.", description =
					"Retrieves a list of products and related product search data, such as available facets, available sorting, and spelling suggestions, for a category."
					+ " To enable spelling suggestions, you need to have indexed properties configured to be used for spell checking.")
	@ApiBaseSiteIdParam
	public ProductSearchPageWsDTO searchProducts(
			@Parameter(description = "Category identifier.", required = true) @PathVariable final String categoryId,
			@Parameter(description = "Formatted query string. It contains query criteria like free text search, facet. The format is <freeTextSearch>:<sort>:<facetKey1>:<facetValue1>:...:<facetKeyN>:<facetValueN>.") @RequestParam(required = false) final String query,
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(required = false, defaultValue = DEFAULT_PAGE_VALUE) final int currentPage,
			@Parameter(description = "Number of results returned per page.") @RequestParam(required = false, defaultValue = "20") final int pageSize,
			@Parameter(description = "Sorting method applied to the return results.") @RequestParam(required = false) final String sort,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{

		final SearchQueryData searchQueryData = new SearchQueryData();
		searchQueryData.setValue(query);

		final SearchStateData searchState = new SearchStateData();
		searchState.setQuery(searchQueryData);

		final PageableData pageable = new PageableData();
		pageable.setCurrentPage(currentPage);
		pageable.setPageSize(pageSize);
		pageable.setSort(sort);

		final ProductSearchPageData<SearchStateData, ProductData> sourceResult = solrProductSearchFacade
				.categorySearch(categoryId, searchState, pageable);

		if (sourceResult instanceof ProductCategorySearchPageData)
		{
			return dataMapper.map(sourceResult, ProductCategorySearchPageWsDTO.class, fields);
		}

		return dataMapper.map(sourceResult, ProductSearchPageWsDTO.class, fields);
	}

}
