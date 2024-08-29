package de.hybris.platform.sagajib2bwebservices.v2.helper;


import de.hybris.bootstrap.annotations.UnitTest;

import de.hybris.platform.b2b.company.B2BCommerceUnitService;
import de.hybris.platform.b2b.model.B2BUnitModel;
import de.hybris.platform.b2bacceleratorfacades.company.impl.DefaultB2BAccountSummaryFacade;
import de.hybris.platform.b2bacceleratorfacades.data.AccountSummaryInfoData;
import de.hybris.platform.b2bacceleratorfacades.document.criteria.validator.DocumentCriteriaValidator;
import de.hybris.platform.b2bacceleratorfacades.document.data.B2BAmountBalanceData;
import de.hybris.platform.b2bacceleratorfacades.document.data.B2BDocumentData;
import de.hybris.platform.b2bacceleratorfacades.document.data.AttachmentData;
import de.hybris.platform.b2bacceleratorfacades.document.data.OrgDocumentListData;
import de.hybris.platform.b2bacceleratorservices.document.criteria.DefaultCriteria;
import de.hybris.platform.b2bacceleratorservices.document.criteria.FilterByCriteriaData;
import de.hybris.platform.b2bcommercefacades.company.data.B2BUnitData;
import de.hybris.platform.b2bwebservicescommons.dto.company.AccountSummaryWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.AmountBalanceWsDTO;
import de.hybris.platform.b2bacceleratorservices.model.B2BDocumentModel;
import de.hybris.platform.b2bacceleratorservices.model.DocumentMediaModel;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgDocumentListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUnitReferenceWsDTO;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.b2bacceleratorservices.document.criteria.SingleValueCriteria;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;



@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class AccountSummaryHelperUnitTest
{
	@Mock
	private DefaultB2BAccountSummaryFacade accountSummaryFacade;
	@Mock
	private B2BCommerceUnitService b2bCommerceUnitService;
	@Mock
	private Map<String, DefaultCriteria> orgDocumentFilterByList;
	@Mock
	private Map<String, DocumentCriteriaValidator> documentValidatorMapping;
	@Mock
	private DataMapper dataMapper;
	@InjectMocks
	private AccountSummaryHelper accountSummaryHelper;
	private final OrgDocumentListData<B2BDocumentData> orgDocumentListData = new OrgDocumentListData<>();
	private final static String DEFAULT_SORT = "byCreatedAtDateAsc";
	private final static String DATE = "DATE";
	private final static String FILTER_KEY = "orgDocumentId";
	private final static String FILTER_RANGE_KEY = "orgDocumentIdRange";
	private final static String START_RANGE = "START_RANGE";
	private final static String FILTER_VALUE = "ABC-1234";
	private final static String FIELDS = "DEFAULT";
	private final static String DOCUMENT_STATUS = "all";
	private final static String ORG_UNIT_ID = "unitId";
	private final int PAGE_SIZE = 10;
	private final int PAGE_NUMBER = 0;
	final PageableData pageableData = new PageableData();


	@Before
	public void setup()
	{
		when(accountSummaryFacade.getOrgDocumentList(any(), any(), any(), any())).thenReturn(orgDocumentListData);
		when(accountSummaryFacade.getOrgDocumentToModelSortMap()).thenReturn(Map.of(DEFAULT_SORT, DATE));

		when(b2bCommerceUnitService.getUnitForUid(any())).thenReturn(new B2BUnitModel());
		when(orgDocumentFilterByList.containsKey(any())).thenReturn(true);
		when(orgDocumentFilterByList.get(anyString())).thenReturn(new SingleValueCriteria(FILTER_KEY));

		DocumentCriteriaValidator mockValidator = (startValue, endValue) -> true;
		lenient().when(documentValidatorMapping.get(anyString())).thenReturn(mockValidator);

		pageableData.setPageSize(PAGE_SIZE);
		pageableData.setCurrentPage(PAGE_NUMBER);
		pageableData.setSort(DEFAULT_SORT);
	}

	@Test
	public void testHelperCallsAccountSummaryFacadeWithCorrectParameters()
	{
		when(dataMapper.map(any(), any(), anyString())).thenReturn(new OrgDocumentListWsDTO());

		final FilterByCriteriaData filterByCriteriaData = accountSummaryHelper.createFilterByCriteriaData(FILTER_KEY,
				DOCUMENT_STATUS, FILTER_VALUE, "", "");

		assertThat(accountSummaryHelper.searchOrgDocuments(ORG_UNIT_ID, DOCUMENT_STATUS, FILTER_KEY,
						filterByCriteriaData, pageableData, FIELDS)).isNotNull();

		final ArgumentMatcher<FilterByCriteriaData> filterByCriteriaDataArgumentMatcher = expected ->
				expected.getEndRange().equals("") && expected.getStartRange().equals("") && expected.getFilterByValue()
						.equals(FILTER_VALUE) && expected.getDocumentStatus().equals(DOCUMENT_STATUS.toUpperCase());

		final ArgumentMatcher<DefaultCriteria> defaultCriteriaArgumentMatcher = expected -> expected.getFilterByKey()
				.equals(FILTER_KEY);

		final ArgumentMatcher<PageableData> paginationDataArgumentMatcher = expected -> expected.getPageSize() == PAGE_SIZE
				&& expected.getCurrentPage() == PAGE_NUMBER && expected.getSort().equals(DEFAULT_SORT);

		verify(accountSummaryFacade, times(1)).getOrgDocumentList(eq(ORG_UNIT_ID), argThat(paginationDataArgumentMatcher),
				argThat(filterByCriteriaDataArgumentMatcher), argThat(defaultCriteriaArgumentMatcher));
	}

	@Test
	public void testCallingSearchOrgDocumentsWithWrongFilterRange()
	{
		final FilterByCriteriaData filterByCriteriaData = accountSummaryHelper.createFilterByCriteriaData(FILTER_KEY,
				DOCUMENT_STATUS, FILTER_VALUE, START_RANGE, "");

		assertThatThrownBy(() -> accountSummaryHelper.searchOrgDocuments(
						ORG_UNIT_ID, DOCUMENT_STATUS, FILTER_KEY, filterByCriteriaData, pageableData, FIELDS))
				.isInstanceOf(UnknownIdentifierException.class);
	}

	@Test
	public void testCallingSearchOrgDocumentsWithWrongFilterValue()
	{
		final FilterByCriteriaData filterByCriteriaData = accountSummaryHelper.createFilterByCriteriaData(FILTER_RANGE_KEY,
				DOCUMENT_STATUS, FILTER_VALUE, START_RANGE, "");

		assertThatThrownBy(() -> accountSummaryHelper.searchOrgDocuments(
						ORG_UNIT_ID, DOCUMENT_STATUS, FILTER_RANGE_KEY, filterByCriteriaData, pageableData, FIELDS))
				.isInstanceOf(UnknownIdentifierException.class);
	}

	@Test
	public void testCallingSearchOrgDocumentsWithInvalidUnitId()
	{
		when(b2bCommerceUnitService.getUnitForUid(any())).thenReturn(null);
		final String UNIT_ID = "invalid_unit";

		final FilterByCriteriaData filterByCriteriaData = accountSummaryHelper.createFilterByCriteriaData(FILTER_KEY,
				DOCUMENT_STATUS, FILTER_VALUE, "", "");

		assertThatThrownBy(() -> accountSummaryHelper.searchOrgDocuments(
						UNIT_ID, DOCUMENT_STATUS, FILTER_KEY, filterByCriteriaData, pageableData, FIELDS))
				.isInstanceOf(UnknownIdentifierException.class);
	}

	@Test
	public void testCallingGetAccountSummaryDetailsWithValidUnitID()
	{
		AccountSummaryInfoData accountSummaryInfoData = new AccountSummaryInfoData();
		AccountSummaryWsDTO accountSummaryWsDTO = new AccountSummaryWsDTO();
		B2BAmountBalanceData b2BAmountBalanceData = accountSummaryInfoData.getAmountBalanceData();
		B2BUnitData unitData = accountSummaryInfoData.getB2bUnitData();

		when(accountSummaryFacade.getAccountSummaryInfoData(any())).thenReturn(accountSummaryInfoData);
		when(dataMapper.map(accountSummaryInfoData, AccountSummaryWsDTO.class)).thenReturn(accountSummaryWsDTO);

		assertThat(accountSummaryHelper.getAccountSummaryDetails(ORG_UNIT_ID)).isNotNull();
		verify(accountSummaryFacade, times(1)).getAccountSummaryInfoData(ORG_UNIT_ID);
		verify(dataMapper).map(b2BAmountBalanceData, AmountBalanceWsDTO.class);
		verify(dataMapper).map(unitData, OrgUnitReferenceWsDTO.class);
	}

	@Test
	public void testCallingGetMediaForDocumentWithoutAttachment()
	{
		final String DOCUMENT_NUMBER = "document_number";
		final String DOCUMENT_MEDIA_ID = "media_id";

		when(accountSummaryFacade.getDocumentByIdForUnit(ORG_UNIT_ID, DOCUMENT_NUMBER)).thenReturn(new B2BDocumentModel());

		assertThatThrownBy(() -> accountSummaryHelper.getMediaForDocument(
						ORG_UNIT_ID, DOCUMENT_NUMBER, DOCUMENT_MEDIA_ID))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	public void testCallingGetMediaForDocumentWithWrongMediaID()
	{
		final String DOCUMENT_NUMBER = "document_number";
		final String CORRECT_MEDIA_ID = "correct_media_id";
		final String WRONG_MEDIA_ID = "wrong_media_id";

		final B2BDocumentModel b2bDocumentModel = new B2BDocumentModel();
		final DocumentMediaModel documentMediaModel = new DocumentMediaModel();
		documentMediaModel.setCode(CORRECT_MEDIA_ID);

		when(accountSummaryFacade.getDocumentByIdForUnit(ORG_UNIT_ID, DOCUMENT_NUMBER)).thenReturn(b2bDocumentModel);

		assertThatThrownBy(() -> accountSummaryHelper.getMediaForDocument(
				ORG_UNIT_ID, DOCUMENT_NUMBER, WRONG_MEDIA_ID))
				.isInstanceOf(NotFoundException.class);
	}

	@Test
	public void testCallingGetMediaForDocumentReturnsAttachmentData()
	{
		final String DOCUMENT_NUMBER = "document_number";
		final String DOCUMENT_MEDIA_ID = "media_id";

		final B2BDocumentModel b2bDocumentModel = new B2BDocumentModel();
		final DocumentMediaModel documentMediaModel = new DocumentMediaModel();
		documentMediaModel.setCode(DOCUMENT_MEDIA_ID);
		b2bDocumentModel.setDocumentMedia(documentMediaModel);

		when(accountSummaryFacade.getDocumentByIdForUnit(ORG_UNIT_ID, DOCUMENT_NUMBER)).thenReturn(b2bDocumentModel);
		when(accountSummaryFacade.getB2BDocumentAttachmentData(documentMediaModel)).thenReturn(new AttachmentData());

		assertThat(accountSummaryHelper.getMediaForDocument(
				ORG_UNIT_ID, DOCUMENT_NUMBER, DOCUMENT_MEDIA_ID)).isNotNull();
	}

}
