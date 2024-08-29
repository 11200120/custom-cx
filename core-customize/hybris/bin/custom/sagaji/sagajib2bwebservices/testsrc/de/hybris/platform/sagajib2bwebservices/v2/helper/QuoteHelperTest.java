/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.v2.helper;

import static de.hybris.platform.core.enums.QuoteState.BUYER_OFFER;
import static de.hybris.platform.core.enums.QuoteState.BUYER_SUBMITTED;
import static de.hybris.platform.core.enums.QuoteState.SELLERAPPROVER_APPROVED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.sagajib2bwebservices.exceptions.CartValidationException;
import de.hybris.platform.sagajib2bwebservices.exceptions.QuoteAssemblingException;
import de.hybris.platform.sagajib2bwebservices.exceptions.QuoteException;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.QuoteAttachmentFacade;
import de.hybris.platform.commercefacades.order.QuoteFacade;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CommerceCartMetadata;
import de.hybris.platform.commercefacades.quote.data.QuoteData;
import de.hybris.platform.commercefacades.voucher.VoucherFacade;
import de.hybris.platform.commercefacades.voucher.data.VoucherData;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commerceservices.enums.QuoteAction;
import de.hybris.platform.commerceservices.enums.QuoteUserType;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.order.strategies.QuoteUserIdentificationStrategy;
import de.hybris.platform.commerceservices.order.strategies.QuoteUserTypeIdentificationStrategy;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commerceservices.search.pagedata.SearchPageData;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteWsDTO;
import de.hybris.platform.commercewebservicescommons.strategies.CartLoaderStrategy;
import de.hybris.platform.converters.Populator;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.QuoteModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.QuoteService;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class QuoteHelperTest
{
	private static final String CART_ID = "MY_CART_ID";
	private static final String QUOTE_CODE = "MY_QUOTE_CODE";
	private static final String VOUCHER_CODE = "MY_VOUCHER_CODE";
	private static final double QUOTE_THRESHOLD = 123456;
	private static final String FIELDS = "MY_FIELDS";

	private static final String QUOTE_NAME = "NAME";
	private static final String QUOTE_DESCRIPTION = "DESCRIPTION";
	private static final String QUOTE_COMMENT = "COMMENT";
	private static final int ENTRY_NUMBER = 2;

	private static final String DISCOUNT_TYPE = "MY_DISCOUNT";
	private static final double DISCOUNT_RATE = 11.1;

	private static final int CURRENT_PAGE = 11;
	private static final int PAGE_SIZE = 200;
	private static final String SORT = "MY_SORT";

	@Mock
	private QuoteFacade quoteFacade;
	@Mock
	private CartFacade cartFacade;
	@Mock
	private QuoteAttachmentFacade mockQuoteAttachmentFacade;
	@Mock
	private QuoteService quoteService;
	@Mock
	private VoucherFacade voucherFacade;
	@Mock
	private CartModel cartModel;
	@Mock
	private QuoteModel quoteModel;
	@InjectMocks
	private QuoteHelper quoteHelper;
	@Mock
	private CartService cartService;
	@Mock
	private CartLoaderStrategy cartLoaderStrategy;
	@Mock
	private DataMapper dataMapper;
	@Mock
	private QuoteUserTypeIdentificationStrategy quoteUserTypeIdentificationStrategy;
	@Mock
	private QuoteUserIdentificationStrategy quoteUserIdentificationStrategy;
	@Mock
	private Populator<CartModel, QuoteWsDTO> cartModelToQuoteWsDTOPopulator;
	@Captor
	private ArgumentCaptor<PageableData> pageableDataCaptor;

	private final VoucherData voucherData = new VoucherData();
	private final QuoteData quoteData = new QuoteData();
	private final QuoteWsDTO quoteWsDTO = new QuoteWsDTO();
	private final UserModel userModel = new UserModel();

	@Before
	public void setUp() throws CommerceCartModificationException
	{
		voucherData.setCode(VOUCHER_CODE);
		quoteData.setCode(QUOTE_CODE);
		quoteData.setSapAttachments(new ArrayList<>());

		when(voucherFacade.getVouchersForCart()).thenReturn(List.of());
		when(quoteService.getCurrentQuoteForCode(QUOTE_CODE)).thenReturn(quoteModel);

		when(voucherFacade.getVouchersForCart()).thenReturn(List.of());
		when(quoteService.getCurrentQuoteForCode(QUOTE_CODE)).thenReturn(quoteModel);
		when(quoteModel.getCartReference()).thenReturn(cartModel);

		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.CREATE));
		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);
		when(quoteFacade.getQuoteRequestThreshold(QUOTE_CODE)).thenReturn(QUOTE_THRESHOLD);

		when(cartFacade.hasEntries()).thenReturn(true);
		when(cartFacade.validateCartData()).thenReturn(Collections.emptyList());

		when(dataMapper.map(quoteData, QuoteWsDTO.class, FIELDS)).thenReturn(quoteWsDTO);
		when(dataMapper.map(quoteWsDTO, QuoteWsDTO.class, FIELDS)).thenReturn(quoteWsDTO);

		when(quoteUserIdentificationStrategy.getCurrentQuoteUser()).thenReturn(userModel);
		when(quoteUserTypeIdentificationStrategy.getCurrentQuoteUserType(userModel)).thenReturn(Optional.of(QuoteUserType.BUYER));
	}

	@Test
	public void testIfViewAndSaveAreNotReturnedInTheListOfAllowedActionsForCreate() throws CommerceCartModificationException, VoucherOperationException
	{
		when(quoteFacade.initiateQuote()).thenReturn(quoteData);
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.VIEW, QuoteAction.CREATE, QuoteAction.SAVE));

		quoteHelper.initiateQuote(QUOTE_CODE, FIELDS);

		assertThat(quoteWsDTO.getAllowedActions()).containsExactly("CREATE");
	}

	@Test
	public void testIfViewAndSaveAreNotReturnedInTheListOfAllowedActionsForGet() throws CommerceCartModificationException, VoucherOperationException
	{
		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.VIEW, QuoteAction.CREATE, QuoteAction.SAVE));

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		assertThat(quoteWsDTO.getAllowedActions()).containsExactly("CREATE");
	}

	@Test
	public void testIfDiscountAndExpiredAndOrderAreNotReturnedInTheListOfAllowedActionsForCreate() throws CommerceCartModificationException, VoucherOperationException
	{
		when(quoteFacade.initiateQuote()).thenReturn(quoteData);
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.DISCOUNT, QuoteAction.EXPIRED, QuoteAction.ORDER, QuoteAction.CREATE));

		quoteHelper.initiateQuote(QUOTE_CODE, FIELDS);

		assertThat(quoteWsDTO.getAllowedActions()).containsExactly("CREATE");
	}

	@Test
	public void testIfDiscountAndExpiredAndOrderAreNotReturnedInTheListOfAllowedActionsForGet()
	{
		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.DISCOUNT, QuoteAction.EXPIRED, QuoteAction.ORDER, QuoteAction.CREATE));

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		assertThat(quoteWsDTO.getAllowedActions()).containsExactly("CREATE");
	}

	@Test
	public void testCreateQuote() throws CommerceCartModificationException, VoucherOperationException
	{
		when(quoteFacade.initiateQuote()).thenReturn(quoteData);

		final QuoteWsDTO initQuoteWsDTO = quoteHelper.initiateQuote(CART_ID, FIELDS);

		verify(cartFacade).hasEntries();
		verify(cartLoaderStrategy).loadCart(CART_ID);
		verify(quoteFacade).initiateQuote();
		verify(quoteFacade).getAllowedActions(QUOTE_CODE);
		verify(quoteFacade).getQuoteRequestThreshold(QUOTE_CODE);
		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verify(dataMapper).map(quoteData, QuoteWsDTO.class, FIELDS);
		verify(dataMapper).map(quoteWsDTO, QuoteWsDTO.class, FIELDS);
		verify(cartModelToQuoteWsDTOPopulator).populate(cartModel, quoteWsDTO);

		assertThat(initQuoteWsDTO).isSameAs(quoteWsDTO);
		assertThat(quoteWsDTO).hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
							  .hasFieldOrPropertyWithValue("allowedActions", List.of("CREATE"));
	}

	@Test
	public void testCreateQuoteForApprover()
	{
		when(quoteUserTypeIdentificationStrategy.getCurrentQuoteUserType(userModel)).thenReturn(Optional.of(QuoteUserType.SELLERAPPROVER));

		assertThatThrownBy(() -> quoteHelper.initiateQuote(CART_ID, FIELDS))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Access is denied");

		verify(quoteFacade, never()).initiateQuote();
	}

	@Test
	public void testCreateQuoteWhenEmptyCartThrowsError()
	{
		when(cartFacade.hasEntries()).thenReturn(false);

		assertThatThrownBy(() -> quoteHelper.initiateQuote(CART_ID, FIELDS))
			.isInstanceOf(QuoteException.class)
			.hasMessage("Empty carts are not allowed");

		verifyNoMoreInteractions(quoteFacade);
		verifyNoMoreInteractions(dataMapper);
	}

	@Test
	public void testCreateQuoteWhenCartIsNotValid() throws CommerceCartModificationException
	{
		final List<CartModificationData> modifications = getNewCommerceCartModifications();
		doThrow(new CartValidationException(modifications)).when(cartFacade).validateCartData();

		assertThatThrownBy(() -> quoteHelper.initiateQuote(CART_ID, FIELDS))
			.isInstanceOf(CartValidationException.class)
			.hasFieldOrPropertyWithValue("modifications", modifications);

		verifyNoMoreInteractions(quoteFacade);
		verifyNoMoreInteractions(dataMapper);
	}

	public void testCreateQuoteWhenGetAllowedActionsThrowsError()
	{
		when(quoteFacade.initiateQuote()).thenReturn(quoteData);
		when(quoteFacade.getAllowedActions(any())).thenThrow(new RuntimeException("Error happened when get allowed actions"));

		assertThatThrownBy(() -> quoteHelper.initiateQuote(CART_ID, FIELDS))
			.isInstanceOf(QuoteAssemblingException.class)
			.hasCause(new RuntimeException("Error happened when get allowed actions"));
	}

	@Test
	public void testCreateQuoteWhenRemoveVoucherThrowsError() throws VoucherOperationException
	{
		final VoucherData voucherData = new VoucherData();
		voucherData.setCode(VOUCHER_CODE);
		when(voucherFacade.getVouchersForCart()).thenReturn(List.of(voucherData));
		doThrow(new VoucherOperationException("Couldn't release voucher: " + VOUCHER_CODE)).when(voucherFacade).releaseVoucher(VOUCHER_CODE);

		assertThatThrownBy(() -> quoteHelper.initiateQuote(CART_ID, FIELDS))
			.isInstanceOf(VoucherOperationException.class)
			.hasMessage("Couldn't release voucher: " + VOUCHER_CODE);
	}

	@Test
	public void testCreateQuoteQuoteCodeWhenValidateCartThrowsException() throws CommerceCartModificationException
	{
		doThrow(new CommerceCartModificationException("Error when validating a cart")).when(cartFacade).validateCartData();

		assertThatThrownBy(() -> quoteHelper.initiateQuote(CART_ID, FIELDS))
			.isInstanceOf(CommerceCartModificationException.class)
			.hasMessage("Error when validating a cart");

		verifyNoMoreInteractions(quoteFacade);
	}

	@Test
	public void testRequote()
	{
		when(quoteFacade.requote(QUOTE_CODE)).thenReturn(quoteData);

		final QuoteWsDTO initQuoteWsDTO = quoteHelper.requote(QUOTE_CODE, FIELDS);

		verify(quoteFacade).requote(QUOTE_CODE);
		verify(quoteFacade).getAllowedActions(QUOTE_CODE);
		verify(quoteFacade).getQuoteRequestThreshold(QUOTE_CODE);
		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verify(dataMapper).map(quoteData, QuoteWsDTO.class, FIELDS);
		verify(dataMapper).map(quoteWsDTO, QuoteWsDTO.class, FIELDS);
		verify(cartModelToQuoteWsDTOPopulator).populate(cartModel, quoteWsDTO);

		assertThat(initQuoteWsDTO).isSameAs(quoteWsDTO);
		assertThat(quoteWsDTO).hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
							  .hasFieldOrPropertyWithValue("allowedActions", List.of("CREATE"));
	}

	@Test
	public void testRequoteForApprover()
	{
		when(quoteUserTypeIdentificationStrategy.getCurrentQuoteUserType(userModel)).thenReturn(Optional.of(QuoteUserType.SELLERAPPROVER));

		assertThatThrownBy(() -> quoteHelper.requote(QUOTE_CODE, FIELDS))
			.isInstanceOf(AccessDeniedException.class)
			.hasMessage("Access is denied");

		verify(quoteFacade, never()).requote(any());
	}

	@Test
	public void testRequoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).requote(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.requote(QUOTE_CODE, FIELDS))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testGetQuoteWithCartData()
	{
		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		verify(quoteFacade).getQuoteForCode(QUOTE_CODE);
		verify(quoteFacade).getQuoteRequestThreshold(QUOTE_CODE);
		verify(quoteFacade).getAllowedActions(QUOTE_CODE);
		verify(dataMapper).map(quoteData, QuoteWsDTO.class, FIELDS);
		verify(dataMapper).map(quoteWsDTO, QuoteWsDTO.class, FIELDS);

		verify(cartModelToQuoteWsDTOPopulator).populate(cartModel, quoteWsDTO);

		assertThat(quoteWsDTO)
			.hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
			.hasFieldOrPropertyWithValue("allowedActions", List.of("CREATE"));
	}

	@Test
	public void testGetQuoteWithoutCartData()
	{
		quoteData.setName(QUOTE_NAME);
		quoteData.setDescription(QUOTE_DESCRIPTION);

		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);

		// this will copy name and description from quoteData to quoteWsDTO
		when(dataMapper.map(quoteData, QuoteWsDTO.class, FIELDS)).thenAnswer(invocationOnMock -> {
			quoteWsDTO.setName(quoteData.getName());
			quoteWsDTO.setDescription(quoteData.getDescription());
			quoteWsDTO.setSapAttachments(new ArrayList<>());
			return quoteWsDTO;
		});
		when(quoteModel.getCartReference()).thenReturn(null);

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		verify(quoteFacade).getQuoteForCode(QUOTE_CODE);
		verify(quoteFacade).getQuoteRequestThreshold(QUOTE_CODE);
		verify(quoteFacade).getAllowedActions(QUOTE_CODE);
		verify(dataMapper).map(quoteData, QuoteWsDTO.class, FIELDS);
		verify(dataMapper).map(quoteWsDTO, QuoteWsDTO.class, FIELDS);

		verify(cartModelToQuoteWsDTOPopulator, never()).populate(any(), any());

		assertThat(quoteWsDTO).isNotNull()
			.hasFieldOrPropertyWithValue("name", QUOTE_NAME)
			.hasFieldOrPropertyWithValue("description", QUOTE_DESCRIPTION)
			.hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
			.hasFieldOrPropertyWithValue("allowedActions", List.of("CREATE"))
			.hasFieldOrPropertyWithValue("cartId", null);
	}

	@Test
	public void testSellerGetQuoteWhenQuoteStateIsSellerApproverApproved()
	{
		quoteData.setName(QUOTE_NAME);
		quoteData.setDescription(QUOTE_DESCRIPTION);
		quoteData.setState(SELLERAPPROVER_APPROVED);

		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);
		when(quoteUserIdentificationStrategy.getCurrentQuoteUser()).thenReturn(userModel);
		when(quoteUserTypeIdentificationStrategy.getCurrentQuoteUserType(userModel)).thenReturn(Optional.of(QuoteUserType.SELLER));


		// this will copy name and description from quoteData to quoteWsDTO
		when(dataMapper.map(quoteData, QuoteWsDTO.class, FIELDS)).thenAnswer(invocationOnMock -> {
			quoteWsDTO.setName(quoteData.getName());
			quoteWsDTO.setDescription(quoteData.getDescription());
			quoteWsDTO.setSapAttachments(new ArrayList<>());
			return quoteWsDTO;
		});

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		verifyNoInteractions(cartModelToQuoteWsDTOPopulator);
		assertThat(quoteWsDTO).isNotNull()
				.hasFieldOrPropertyWithValue("name", QUOTE_NAME)
				.hasFieldOrPropertyWithValue("description", QUOTE_DESCRIPTION)
				.hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
				.hasFieldOrPropertyWithValue("cartId", null);
	}

	@Test
	public void testApproverGetQuoteWhenQuoteStateIsSellerApproverApproved()
	{
		quoteData.setName(QUOTE_NAME);
		quoteData.setDescription(QUOTE_DESCRIPTION);
		quoteData.setState(SELLERAPPROVER_APPROVED);

		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);
		when(quoteUserIdentificationStrategy.getCurrentQuoteUser()).thenReturn(userModel);
		when(quoteUserTypeIdentificationStrategy.getCurrentQuoteUserType(userModel)).thenReturn(Optional.of(QuoteUserType.SELLERAPPROVER));


		// this will copy name and description from quoteData to quoteWsDTO
		when(dataMapper.map(quoteData, QuoteWsDTO.class, FIELDS)).thenAnswer(invocationOnMock -> {
			quoteWsDTO.setName(quoteData.getName());
			quoteWsDTO.setDescription(quoteData.getDescription());
			quoteWsDTO.setSapAttachments(new ArrayList<>());
			return quoteWsDTO;
		});

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		verifyNoInteractions(cartModelToQuoteWsDTOPopulator);
		assertThat(quoteWsDTO).isNotNull()
				.hasFieldOrPropertyWithValue("name", QUOTE_NAME)
				.hasFieldOrPropertyWithValue("description", QUOTE_DESCRIPTION)
				.hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
				.hasFieldOrPropertyWithValue("cartId", null);
	}

	@Test
	public void testBuyerGetQuoteWhenQuoteStateIsBuyerSubmitted()
	{
		quoteData.setName(QUOTE_NAME);
		quoteData.setDescription(QUOTE_DESCRIPTION);
		quoteData.setState(BUYER_SUBMITTED);

		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);

		// this will copy name and description from quoteData to quoteWsDTO
		when(dataMapper.map(quoteData, QuoteWsDTO.class, FIELDS)).thenAnswer(invocationOnMock -> {
			quoteWsDTO.setName(quoteData.getName());
			quoteWsDTO.setDescription(quoteData.getDescription());
			quoteWsDTO.setSapAttachments(new ArrayList<>());
			return quoteWsDTO;
		});

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		verifyNoInteractions(cartModelToQuoteWsDTOPopulator);
		assertThat(quoteWsDTO).isNotNull()
				.hasFieldOrPropertyWithValue("name", QUOTE_NAME)
				.hasFieldOrPropertyWithValue("description", QUOTE_DESCRIPTION)
				.hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
				.hasFieldOrPropertyWithValue("cartId", null);
	}

	@Test
	public void testBuyerGetQuoteWhenCartDataExistsAndQuoteStateIsBuyerOffer()
	{
		quoteData.setName(QUOTE_NAME);
		quoteData.setDescription(QUOTE_DESCRIPTION);
		quoteData.setState(BUYER_OFFER);

		when(quoteFacade.getQuoteForCode(QUOTE_CODE)).thenReturn(quoteData);

		// this will copy name and description from quoteData to quoteWsDTO
		when(dataMapper.map(quoteData, QuoteWsDTO.class, FIELDS)).thenAnswer(invocationOnMock -> {
			quoteWsDTO.setName(quoteData.getName());
			quoteWsDTO.setDescription(quoteData.getDescription());
			return quoteWsDTO;
		});
		when(cartModel.getCode()).thenReturn(CART_ID);

		quoteHelper.getQuote(QUOTE_CODE, FIELDS);

		verify(quoteFacade).getQuoteForCode(QUOTE_CODE);
		verify(quoteFacade).getQuoteRequestThreshold(QUOTE_CODE);
		verify(quoteFacade).getAllowedActions(QUOTE_CODE);
		verify(dataMapper).map(quoteData, QuoteWsDTO.class, FIELDS);
		verify(dataMapper).map(quoteWsDTO, QuoteWsDTO.class, FIELDS);

		verifyNoInteractions(cartModelToQuoteWsDTOPopulator);

		assertThat(quoteWsDTO).isNotNull().hasFieldOrPropertyWithValue("name", QUOTE_NAME)
				.hasFieldOrPropertyWithValue("description", QUOTE_DESCRIPTION)
				.hasFieldOrPropertyWithValue("threshold", QUOTE_THRESHOLD)
				.hasFieldOrPropertyWithValue("allowedActions", List.of("CREATE"))
				.hasFieldOrPropertyWithValue("cartId", CART_ID);
	}

	@Test
	public void testGetQuoteWhenGetAllowedActionsThrowsError()
	{
		when(quoteFacade.getAllowedActions(any())).thenThrow(new RuntimeException("Error happened when get allowed actions"));

		assertThatThrownBy(() -> quoteHelper.getQuote(QUOTE_CODE, FIELDS))
			.isInstanceOf(QuoteAssemblingException.class)
			.hasCause(new RuntimeException("Error happened when get allowed actions"));
	}

	@Test
	public void testGetQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).getQuoteForCode(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.getQuote(QUOTE_CODE, FIELDS))
			.isInstanceOf(NotFoundException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testGetQuotes()
	{
		final QuoteListWsDTO quoteListWsDTO = new QuoteListWsDTO();
		final SearchPageData<QuoteData> searchPageData = new SearchPageData<>();
		final List<QuoteData> quoteDataList = new ArrayList<>();
		quoteDataList.add(quoteData);
		quoteData.setName(QUOTE_NAME);
		quoteData.setDescription(QUOTE_DESCRIPTION);
		searchPageData.setResults(quoteDataList);
		final PageableData pageableData = new PageableData();
		pageableData.setCurrentPage(CURRENT_PAGE);
		pageableData.setPageSize(PAGE_SIZE);
		pageableData.setSort(SORT);

		when(quoteFacade.getPagedQuotes(any())).thenReturn(searchPageData);
		when(dataMapper.map(any(), eq(QuoteListWsDTO.class), eq(FIELDS))).thenReturn(quoteListWsDTO);

		final QuoteListWsDTO quoteList = quoteHelper.getQuotes(pageableData, FIELDS);

		verify(quoteFacade).getPagedQuotes(pageableDataCaptor.capture());
		verify(dataMapper).map(any(), eq(QuoteListWsDTO.class), eq(FIELDS));
		assertThat(quoteList).isSameAs(quoteListWsDTO);
		assertThat(pageableDataCaptor.getValue()).isNotNull()
			.hasFieldOrPropertyWithValue("currentPage", CURRENT_PAGE)
			.hasFieldOrPropertyWithValue("pageSize", PAGE_SIZE)
			.hasFieldOrPropertyWithValue("sort", SORT);
	}

	@Test
	public void testUpdateQuote()
	{
		final CommerceCartMetadata cartMetadata = new CommerceCartMetadata();

		quoteHelper.updateQuoteMetadata(QUOTE_CODE, cartMetadata);

		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verify(cartFacade).updateCartMetadata(cartMetadata);
	}

	@Test
	public void testUpdateQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).enableQuoteEdit(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.updateQuoteMetadata(QUOTE_CODE, mock(CommerceCartMetadata.class)))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testCancelQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		quoteHelper.cancelQuote(QUOTE_CODE);

		verify(cartService).setSessionCart(any());
		verify(quoteFacade).cancelQuote(QUOTE_CODE);
	}

	@Test
	public void testCancelQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteService).getCurrentQuoteForCode(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.cancelQuote(QUOTE_CODE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testSubmitQuote() throws CommerceCartModificationException, VoucherOperationException
	{
		quoteHelper.submitQuote(QUOTE_CODE);

		verify(quoteFacade).submitQuote(QUOTE_CODE);
		verify(voucherFacade).getVouchersForCart();
		verify(cartFacade).validateCartData();
	}

	@Test
	public void testSubmitQuoteWhenCartIsNotValidThrowsError() throws CommerceCartModificationException
	{
		final List<CartModificationData> modifications = getNewCommerceCartModifications();
		doThrow(new CartValidationException(modifications)).when(cartFacade).validateCartData();

		assertThatThrownBy(() -> quoteHelper.submitQuote(QUOTE_CODE))
			.isInstanceOf(CartValidationException.class)
			.hasFieldOrPropertyWithValue("modifications", modifications);

		verify(cartFacade).validateCartData();
		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verifyNoMoreInteractions(quoteFacade);
	}

	@Test
	public void testSubmitQuoteWhenRemoveVoucherThrowsError() throws VoucherOperationException
	{
		final VoucherData voucherData = new VoucherData();
		voucherData.setCode(VOUCHER_CODE);
		when(voucherFacade.getVouchersForCart()).thenReturn(List.of(voucherData));
		doThrow(new VoucherOperationException("Couldn't release voucher: " + VOUCHER_CODE)).when(voucherFacade).releaseVoucher(VOUCHER_CODE);

		assertThatThrownBy(() -> quoteHelper.submitQuote(QUOTE_CODE))
			.isInstanceOf(VoucherOperationException.class)
			.hasMessage("Couldn't release voucher: " + VOUCHER_CODE);
	}

	@Test
	public void testSubmitQuoteWhenValidateCartThrowsError() throws CommerceCartModificationException
	{
		doThrow(new CommerceCartModificationException("Error when validating a cart")).when(cartFacade).validateCartData();

		assertThatThrownBy(() -> quoteHelper.submitQuote(QUOTE_CODE))
			.isInstanceOf(CommerceCartModificationException.class)
			.hasMessage("Error when validating a cart");
	}


	@Test
	public void testSubmitQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).enableQuoteEdit(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.submitQuote(QUOTE_CODE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testCreateQuoteComment()
	{
		quoteHelper.addCommentToQuote(QUOTE_CODE, QUOTE_COMMENT);

		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verify(quoteFacade).addComment(QUOTE_COMMENT);
	}

	@Test
	public void testCreateQuoteCommentWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).enableQuoteEdit(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.addCommentToQuote(QUOTE_CODE, QUOTE_COMMENT))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testCreateQuoteEntryComment()
	{
		quoteHelper.addCommentToQuoteEntry(QUOTE_CODE, ENTRY_NUMBER, QUOTE_COMMENT);

		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verify(quoteFacade).addEntryComment(ENTRY_NUMBER, QUOTE_COMMENT);
	}

	@Test
	public void testCreateQuoteEntryCommentWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).enableQuoteEdit(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.addCommentToQuoteEntry(QUOTE_CODE, ENTRY_NUMBER, QUOTE_COMMENT))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testCreateQuoteEntryCommentWhenEntryDoesNotExist()
	{
		doThrow(new IllegalArgumentException("Parameter item can not be null")).when(quoteFacade).addEntryComment(ENTRY_NUMBER, QUOTE_COMMENT);

		assertThatThrownBy(() -> quoteHelper.addCommentToQuoteEntry(QUOTE_CODE, ENTRY_NUMBER, QUOTE_COMMENT))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(IllegalArgumentException.class)
			.hasMessage("Quote entry not found");
	}

	@Test
	public void testCreateQuoteEntryCommentWhenTextIsTooLong()
	{
		doThrow(new IllegalArgumentException("text too long")).when(quoteFacade).addEntryComment(ENTRY_NUMBER, QUOTE_COMMENT);

		assertThatThrownBy(() -> quoteHelper.addCommentToQuoteEntry(QUOTE_CODE, ENTRY_NUMBER, QUOTE_COMMENT))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("text too long");
	}

	@Test
	public void testCreateQuoteDiscount()
	{
		quoteHelper.applyDiscount(QUOTE_CODE, DISCOUNT_TYPE, DISCOUNT_RATE);

		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
		verify(quoteFacade).applyQuoteDiscount(DISCOUNT_RATE, DISCOUNT_TYPE);
	}

	@Test
	public void testCreateQuoteDiscountWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).applyQuoteDiscount(any(), anyString());

		assertThatThrownBy(() -> quoteHelper.applyDiscount(QUOTE_CODE, DISCOUNT_TYPE, DISCOUNT_RATE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testApproveQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		quoteHelper.approveQuote(QUOTE_CODE);

		verify(quoteFacade).approveQuote(QUOTE_CODE);
	}

	@Test
	public void testApproveQuoteWhenFacadeThrowsError()
	{
		final String errMsg = "Error raised when try to approve A Quote";

		doThrow(new IllegalStateException(errMsg)).when(quoteFacade).approveQuote(anyString());
		assertThatThrownBy(() -> quoteHelper.approveQuote(QUOTE_CODE))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(errMsg);
	}

	@Test
	public void testApproveQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).approveQuote(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.approveQuote(QUOTE_CODE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testRejectQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		quoteHelper.rejectQuote(QUOTE_CODE);

		verify(quoteFacade).rejectQuote(QUOTE_CODE);
	}

	@Test
	public void testRejectQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).rejectQuote(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.rejectQuote(QUOTE_CODE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testRejectQuoteWhenFacadeThrowsError()
	{
		final String errMsg = "Error raised when try to reject A Quote";

		doThrow(new IllegalStateException(errMsg)).when(quoteFacade).rejectQuote(anyString());
		assertThatThrownBy(() -> quoteHelper.rejectQuote(QUOTE_CODE))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(errMsg);
	}

	@Test
	public void testCheckoutQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.CHECKOUT));
		quoteHelper.acceptAndPrepareCheckout(QUOTE_CODE);

		verify(quoteFacade).acceptAndPrepareCheckout(QUOTE_CODE);
	}

	@Test
	public void testCheckoutQuoteWhenNotAllowedToCheckout() throws VoucherOperationException, CommerceCartModificationException
	{
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.SUBMIT));
		assertThatThrownBy(() -> quoteHelper.acceptAndPrepareCheckout(QUOTE_CODE))
				.isInstanceOf(QuoteException.class)
				.hasMessage("Action [CHECKOUT] is not allowed for quote code [" + QUOTE_CODE + "]");
	}

	@Test
	public void testCheckoutQuoteWhenQuoteDoesNotExist()
	{
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.CHECKOUT));
		doThrow(ModelNotFoundException.class).when(quoteFacade).acceptAndPrepareCheckout(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.acceptAndPrepareCheckout(QUOTE_CODE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testCheckoutQuoteWhenFacadeThrowsError()
	{
		final String errMsg = "Error raised when try to checkout a quote";
		when(quoteFacade.getAllowedActions(QUOTE_CODE)).thenReturn(Set.of(QuoteAction.CHECKOUT));

		doThrow(new IllegalStateException(errMsg)).when(quoteFacade).acceptAndPrepareCheckout(anyString());
		assertThatThrownBy(() -> quoteHelper.acceptAndPrepareCheckout(QUOTE_CODE))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(errMsg);
	}

	@Test
	public void testEditQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		quoteHelper.enableQuoteEdit(QUOTE_CODE);

		verify(quoteFacade).enableQuoteEdit(QUOTE_CODE);
	}

	@Test
	public void testEditQuoteWhenQuoteDoesNotExist()
	{
		doThrow(ModelNotFoundException.class).when(quoteFacade).enableQuoteEdit(QUOTE_CODE);

		assertThatThrownBy(() -> quoteHelper.enableQuoteEdit(QUOTE_CODE))
			.isInstanceOf(QuoteException.class)
			.hasCauseInstanceOf(ModelNotFoundException.class)
			.hasMessage("Quote not found");
	}

	@Test
	public void testEditQuoteWhenFacadeThrowsError()
	{
		final String errMsg = "Error raised when try to edit a quote";

		doThrow(new IllegalStateException(errMsg)).when(quoteFacade).enableQuoteEdit(anyString());
		assertThatThrownBy(() -> quoteHelper.enableQuoteEdit(QUOTE_CODE))
			.isInstanceOf(IllegalStateException.class)
			.hasMessage(errMsg);
	}

	@Test
	public void testRemoveVouchers() throws VoucherOperationException
	{
		when(voucherFacade.getVouchersForCart()).thenReturn(List.of(voucherData));

		quoteHelper.removeVouchers();

		verify(voucherFacade).releaseVoucher(voucherData.getCode());
	}

	@Test
	public void testRemoveVouchersWithErrors() throws VoucherOperationException
	{
		when(voucherFacade.getVouchersForCart()).thenReturn(List.of(voucherData));

		doThrow(new VoucherOperationException("Couldn't release voucher")).when(voucherFacade).releaseVoucher(any());

		assertThatThrownBy(() -> quoteHelper.removeVouchers())
			.isInstanceOf(VoucherOperationException.class)
			.hasMessage("Couldn't release voucher");
	}

	@Test
	public void testValidateCartThrowsErrorWhenNotValid() throws CommerceCartModificationException
	{
		final List<CartModificationData> modifications = getNewCommerceCartModifications();
		when(cartFacade.validateCartData()).thenReturn(modifications);

		assertThatThrownBy(() -> quoteHelper.validateCart())
			.isInstanceOf(CartValidationException.class)
			.hasFieldOrPropertyWithValue("modifications", modifications);
	}

	@Test
	public void testValidateCartRethrowsError() throws CommerceCartModificationException
	{
		when(cartFacade.validateCartData()).thenThrow(new CommerceCartModificationException("MY_EXCEPTION"));

		assertThatThrownBy(() -> quoteHelper.validateCart())
			.isInstanceOf(CommerceCartModificationException.class)
			.hasMessage("MY_EXCEPTION");
	}

	@Test
	public void testValidateCart() throws CommerceCartModificationException
	{
		final List<CartModificationData> list = spy(new ArrayList<>());
		when(cartFacade.validateCartData()).thenReturn(list);

		quoteHelper.validateCart();

		verify(list).isEmpty();
	}

	@Test
	public void testSetSessionCartFromQuote()
	{
		when(quoteService.getCurrentQuoteForCode(QUOTE_CODE)).thenReturn(quoteModel);
		when(quoteModel.getCartReference()).thenReturn(cartModel);

		quoteHelper.setSessionCartFromQuote(QUOTE_CODE);

		verify(quoteService).getCurrentQuoteForCode(QUOTE_CODE);
		verify(quoteModel).getCartReference();
		verify(cartService).setSessionCart(cartModel);
	}

	private List<CartModificationData> getNewCommerceCartModifications()
	{
		final List<CartModificationData> modifications = new ArrayList<>();
		final CartModificationData cartModificationData = new CartModificationData();

		modifications.add(cartModificationData);

		return modifications;
	}
}
