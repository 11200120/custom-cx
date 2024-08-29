package de.hybris.platform.sagajib2bwebservices.v2.helper;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.b2bacceleratorfacades.order.B2BOrderFacade;
import de.hybris.platform.commercefacades.order.data.OrderHistoryData;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.commerceservices.search.solrfacetsearch.data.SolrSearchQueryData;
import de.hybris.platform.core.enums.OrderStatus;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class B2BOrdersHelperTest
{
	private static final int CURRENT_PAGE = 11;
	private static final int PAGE_SIZE = 200;
	private static final String STATUS1 = "STATUS1";
	private static final String STATUS2 = "STATUS2";
	private static final String STATUSES = "STATUS1,STATUS2";
	private static final String FILTERS = "::USER:TESTUSER:UNIT:TESTUNIT";
	private static final String SORT = "MY_SORT";
	private static final String FIELDS = "MY_FIELDS";

	@Mock
	private B2BOrderFacade b2BOrderFacade;

	@Mock
	private DataMapper dataMapper;

	@InjectMocks
	private B2BOrdersHelper b2BOrdersHelper;

	@Mock
	private SearchPageData<OrderHistoryData> searchPageData;

	@Captor
	private ArgumentCaptor<PageableData> pageableDataCaptor;

	@Captor
	private ArgumentCaptor<SolrSearchQueryData> solrSearchQueryDataArgumentCaptor;

	@Before
	public void setUp()
	{
		when(b2BOrderFacade.getPagedBranchOrderHistoryForStatuses(any(), any(), any())).thenReturn(searchPageData);
		when(searchPageData.getResults()).thenReturn(List.of());
	}

	@Test
	public void testSearchBranchOrderHistoryWithStatus()
	{
		b2BOrdersHelper.searchBranchOrderHistory(STATUSES, FILTERS, CURRENT_PAGE, PAGE_SIZE, SORT, FIELDS);
		verify(b2BOrderFacade).getPagedBranchOrderHistoryForStatuses(pageableDataCaptor.capture(),
				solrSearchQueryDataArgumentCaptor.capture(), eq(OrderStatus.valueOf(STATUS1)), eq(OrderStatus.valueOf(STATUS2)));

		final PageableData pageableDataCaptorValue = pageableDataCaptor.getValue();
		assertThat(pageableDataCaptorValue.getCurrentPage()).isEqualTo(CURRENT_PAGE);
		assertThat(pageableDataCaptorValue.getPageSize()).isEqualTo(PAGE_SIZE);
		assertThat(pageableDataCaptorValue.getSort()).isEqualTo(SORT);

		final SolrSearchQueryData solrSearchQueryData = solrSearchQueryDataArgumentCaptor.getValue();
		assertThat(solrSearchQueryData.getFilterTerms().size()).isEqualTo(2);
	}

	@Test
	public void testSearchBranchOrderHistoryWithoutStatus()
	{
		b2BOrdersHelper.searchBranchOrderHistory(null, FILTERS, CURRENT_PAGE, PAGE_SIZE, SORT, FIELDS);
		verify(b2BOrderFacade).getPagedBranchOrderHistoryForStatuses(any(PageableData.class), any());
	}
}
