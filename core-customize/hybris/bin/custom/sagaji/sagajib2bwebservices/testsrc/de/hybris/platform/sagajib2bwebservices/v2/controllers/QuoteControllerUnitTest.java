/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */

package de.hybris.platform.sagajib2bwebservices.v2.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.sagajib2bwebservices.v2.helper.QuoteHelper;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipQuoteListFieldValueSetter;
import de.hybris.platform.sagajib2bwebservices.v2.skipfield.SkipQuoteFieldValueSetter;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CommerceCartMetadata;
import de.hybris.platform.commercefacades.voucher.exceptions.VoucherOperationException;
import de.hybris.platform.commerceservices.enums.QuoteUserType;
import de.hybris.platform.commerceservices.order.CommerceCartModificationException;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.commercewebservicescommons.dto.comments.CreateCommentWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteActionWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteDiscountWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteListWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteMetadataWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteStarterWsDTO;
import de.hybris.platform.commercewebservicescommons.dto.quote.QuoteWsDTO;
import de.hybris.platform.webservicescommons.errors.exceptions.WebserviceValidationException;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class QuoteControllerUnitTest
{
	private static final String CART_ID = "MY_CART_ID";
	private static final String FIELDS = "MY_FIELDS";
	private static final String FIELDS_ENTRIES = "entries(FULL)";
	private static final String QUOTE_CODE = "MY_QUOTE_CODE";
	private static final String QUOTE_ACTION_SUBMIT = "SUBMIT";
	private static final String QUOTE_ACTION_CANCEL = "CANCEL";
	private static final String QUOTE_ACTION_APPROVE = "APPROVE";
	private static final String QUOTE_ACTION_REJECT = "REJECT";
	private static final String QUOTE_ACTION_CHECKOUT = "CHECKOUT";
	private static final String QUOTE_ACTION_EDIT = "EDIT";
	private static final String QUOTE_ACTION_UNKNOWN = "UNKNOWN";
	private static final int ENTRY_NUMBER = 2;

	private static final String QUOTE_NEW_NAME = "NEW NAME";
	private static final String QUOTE_NEW_DESCRIPTION = "NEW DESCRIPTION";
	private static final String QUOTE_NEW_COMMENT = "NEW COMMENT";

	private static final int CURRENT_PAGE = 11;
	private static final int PAGE_SIZE = 200;
	private static final String SORT = "MY_SORT";

	private final QuoteWsDTO quoteWsDTO = new QuoteWsDTO();
	private final Date expirationTime = new Date();

	@Mock
	private Validator quoteNameValidator;
	@Mock
	private Validator quoteDescriptionValidator;
	@Mock
	private Validator quoteCommentValidator;
	@Mock
	private Validator discountTypeValidator;
	@Mock
	private QuoteHelper quoteHelper;
	@Mock
	private SkipQuoteFieldValueSetter skipQuoteFieldValueSetter;
	@Mock
	private SkipQuoteListFieldValueSetter skipQuoteListFieldValueSetter;
	@InjectMocks
	private QuoteController controller;

	@Captor
	private ArgumentCaptor<PageableData> pageableDataCaptor;
	@Captor
	private ArgumentCaptor<CommerceCartMetadata> cartMetadataCaptor;

	@Before
	public void setUp()
	{
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.BUYER));
	}

	@Test
	public void testCreateQuoteQuoteCodeIsNullAndCartIdIsNotNull() throws VoucherOperationException, CommerceCartModificationException
	{
		when(quoteHelper.initiateQuote(anyString(), anyString())).thenReturn(quoteWsDTO);

		final QuoteStarterWsDTO quoteStarter = new QuoteStarterWsDTO();
		quoteStarter.setCartId(CART_ID);
		final QuoteWsDTO initQuoteWsDTO = controller.createQuote(quoteStarter, FIELDS);

		assertThat(initQuoteWsDTO).isSameAs(quoteWsDTO);
		verify(quoteHelper).initiateQuote(CART_ID, FIELDS);
	}

	@Test
	public void testCreateQuoteQuoteCodeIsNotNullAndCartIdIsNull() throws VoucherOperationException, CommerceCartModificationException
	{
		when(quoteHelper.requote(anyString(), anyString())).thenReturn(quoteWsDTO);

		final QuoteStarterWsDTO quoteStarter = new QuoteStarterWsDTO();
		quoteStarter.setQuoteCode(QUOTE_CODE);
		final QuoteWsDTO initQuoteWsDTO = controller.createQuote(quoteStarter, FIELDS);

		assertThat(initQuoteWsDTO).isSameAs(quoteWsDTO);
		verify(quoteHelper).requote(QUOTE_CODE, FIELDS);
	}

	@Test
	public void testCreateQuoteQuoteCodeIsNullAndCartIdIsNull()
	{
		final QuoteStarterWsDTO quoteStarter = new QuoteStarterWsDTO();

		assertThatThrownBy(() -> controller.createQuote(quoteStarter, FIELDS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Either cartId or quoteCode must be provided");
		verifyNoMoreInteractions(quoteHelper);
	}

	@Test
	public void testCreateQuoteQuoteCodeIsNotNullAndCartIdIsNotNull()
	{
		final QuoteStarterWsDTO quoteStarter = new QuoteStarterWsDTO();
		quoteStarter.setQuoteCode(QUOTE_CODE);
		quoteStarter.setCartId(CART_ID);

		assertThatThrownBy(() -> controller.createQuote(quoteStarter, FIELDS))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Either cartId or quoteCode must be provided");
		verifyNoMoreInteractions(quoteHelper);
	}

	@Test
	public void testGetQuote()
	{
		when(quoteHelper.getQuote(anyString(), anyString())).thenReturn(quoteWsDTO);

		final QuoteWsDTO initQuoteWsDTO = controller.getQuote(QUOTE_CODE, FIELDS);

		assertThat(initQuoteWsDTO).isSameAs(quoteWsDTO);
		verify(quoteHelper).getQuote(QUOTE_CODE, FIELDS);
	}

	@Test
	public void testGetQuoteShouldSkipEntries()
	{
		when(quoteHelper.getQuote(anyString(), anyString())).thenReturn(quoteWsDTO);
		final QuoteWsDTO initQuoteWsDTO = controller.getQuote(QUOTE_CODE, FIELDS_ENTRIES);

		assertThat(initQuoteWsDTO).isSameAs(quoteWsDTO);
		verify(skipQuoteFieldValueSetter).setValue(FIELDS_ENTRIES);
		verify(quoteHelper).getQuote(QUOTE_CODE, FIELDS_ENTRIES);
	}

	@Test
	public void testSubmitQuote() throws CommerceCartModificationException, VoucherOperationException
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_SUBMIT);

		controller.performQuoteAction(QUOTE_CODE, quoteAction);

		verify(quoteHelper).submitQuote(QUOTE_CODE);
	}

	@Test
	public void testGetQuotes()
	{
		final QuoteListWsDTO quoteListWsDTO = new QuoteListWsDTO();
		when(quoteHelper.getQuotes(any(), anyString())).thenReturn(quoteListWsDTO);

		final QuoteListWsDTO quoteList = controller.getQuotes(CURRENT_PAGE, PAGE_SIZE, SORT, FIELDS_ENTRIES);

		verify(skipQuoteListFieldValueSetter).setValue(FIELDS_ENTRIES);
		verify(quoteHelper).getQuotes(pageableDataCaptor.capture(), eq(FIELDS_ENTRIES));
		assertThat(quoteList).isSameAs(quoteListWsDTO);
		assertThat(pageableDataCaptor.getValue()).isNotNull()
				.hasFieldOrPropertyWithValue("currentPage", CURRENT_PAGE)
				.hasFieldOrPropertyWithValue("pageSize", PAGE_SIZE)
				.hasFieldOrPropertyWithValue("sort", SORT);
	}

	@Test
	public void testBuyerUpdateQuoteWhenNameIsNotNullAndDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);

		controller.updateQuote(QUOTE_CODE, quoteMetadata);

		verify(quoteNameValidator).validate(any(), any());
		verify(quoteDescriptionValidator).validate(any(), any());
		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.of(QUOTE_NEW_NAME))
				.hasFieldOrPropertyWithValue("description", Optional.of(QUOTE_NEW_DESCRIPTION));
	}

	@Test
	public void testBuyerUpdateQuoteWhenNameIsNullAndDescriptionIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(null);
		quoteMetadata.setDescription(null);

		controller.updateQuote(QUOTE_CODE, quoteMetadata);

		verifyNoInteractions(quoteNameValidator);
		verifyNoInteractions(quoteDescriptionValidator);
		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.empty())
				.hasFieldOrPropertyWithValue("description", Optional.empty());
	}

	@Test
	public void testBuyerUpdateQuoteWhenNameIsNullAndDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(null);
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);

		controller.updateQuote(QUOTE_CODE, quoteMetadata);

		verifyNoInteractions(quoteNameValidator);
		verify(quoteDescriptionValidator).validate(any(), any());
		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.empty())
				.hasFieldOrPropertyWithValue("description", Optional.of(QUOTE_NEW_DESCRIPTION));
	}

	@Test
	public void testBuyerUpdateQuoteWhenNameIsNotNullAndDescriptionIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		quoteMetadata.setDescription(null);

		controller.updateQuote(QUOTE_CODE, quoteMetadata);

		verify(quoteNameValidator).validate(any(), any());
		verifyNoInteractions(quoteDescriptionValidator);
		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.of(QUOTE_NEW_NAME))
				.hasFieldOrPropertyWithValue("description", Optional.empty());
	}

	@Test
	public void testBuyerUpdateQuoteWhenNameIsNullAndDescriptionIsInvalid()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(null);
		quoteMetadata.setDescription("invalidDescription");
		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue("description", "description is invalid", new String[] {}, null);
			return null;
		}).when(quoteDescriptionValidator).validate(eq(quoteMetadata), any());

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata))
				.isInstanceOf(WebserviceValidationException.class);
		verifyNoInteractions(quoteNameValidator);
		verify(quoteHelper).getCurrentQuoteUserType();
	}

	@Test
	public void testBuyerUpdateQuoteWhenNameIsInvalidAndDescriptionIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName("invalidName");
		quoteMetadata.setDescription(null);
		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue("name", "name is invalid", new String[] {}, null);
			return null;
		}).when(quoteNameValidator).validate(eq(quoteMetadata), any());

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata))
				.isInstanceOf(WebserviceValidationException.class);
		verifyNoInteractions(quoteDescriptionValidator);
		verify(quoteHelper).getCurrentQuoteUserType();
	}

	@Test
	public void testBuyerUpdateQuoteWhenExpirationTimeIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setExpirationTime(expirationTime);

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not allowed to change expiration date");
	}

	@Test
	public void testSellerUpdateQuoteWhenNameIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLER));

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not allowed to change name or description");
	}

	@Test
	public void testSellerUpdateQuoteWhenDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLER));

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not allowed to change name or description");
	}

	@Test
	public void testSellerUpdateQuoteWhenExpirationTimeIsNotNullAndNameAndDescriptionAreNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(null);
		quoteMetadata.setDescription(null);
		quoteMetadata.setExpirationTime(expirationTime);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLER));

		controller.updateQuote(QUOTE_CODE, quoteMetadata);

		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.empty())
				.hasFieldOrPropertyWithValue("description", Optional.empty())
				.hasFieldOrPropertyWithValue("expirationTime", Optional.of(expirationTime));
	}

	@Test
	public void testApproverUpdateQuote()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLERAPPROVER));

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(AccessDeniedException.class)
				.hasMessage("Access is denied");
	}

	@Test
	public void testUpdateQuoteWhenUserTypeIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setExpirationTime(expirationTime);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.empty());

		assertThatThrownBy(() -> controller.updateQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(AccessDeniedException.class)
				.hasMessage("Access is denied");
	}

	@Test
	public void testBuyerReplaceWhenQuoteNameIsNotNullAndDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);

		controller.replaceQuote(QUOTE_CODE, quoteMetadata);

		verify(quoteNameValidator).validate(any(), any());
		verify(quoteDescriptionValidator).validate(any(), any());
		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.of(QUOTE_NEW_NAME))
				.hasFieldOrPropertyWithValue("description", Optional.of(QUOTE_NEW_DESCRIPTION));
	}

	@Test
	public void testBuyerReplaceQuoteWhenNameIsNotNullAndDescriptionIsInvalid()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		quoteMetadata.setDescription("invalidDescription");
		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue("description", "description is invalid", new String[] {}, null);
			return null;
		}).when(quoteDescriptionValidator).validate(eq(quoteMetadata), any());

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata))
				.isInstanceOf(WebserviceValidationException.class);
		verify(quoteHelper).getCurrentQuoteUserType();
	}

	@Test
	public void testBuyerReplaceQuoteWhenNameIsInvalidAndDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName("invalidName");
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);
		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue("name", "name is invalid", new String[] {}, null);
			return null;
		}).when(quoteNameValidator).validate(eq(quoteMetadata), any());

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata))
				.isInstanceOf(WebserviceValidationException.class);
		verify(quoteHelper).getCurrentQuoteUserType();
	}

	@Test
	public void testBuyerReplaceQuoteWhenNameIsNullAndDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(null);
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Name is required.");
	}

	@Test
	public void testBuyerReplaceQuoteWhenNameIsNotNullAndDescriptionIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		quoteMetadata.setDescription(null);

		controller.replaceQuote(QUOTE_CODE, quoteMetadata);

		verify(quoteNameValidator).validate(any(), any());
		verifyNoInteractions(quoteDescriptionValidator);
		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull().hasFieldOrPropertyWithValue("name", Optional.of(QUOTE_NEW_NAME))
				.hasFieldOrPropertyWithValue("description", Optional.of(""));
	}

	@Test
	public void testBuyerReplaceQuoteWhenNameIsNullAndDescriptionIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(null);
		quoteMetadata.setDescription(null);

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Please provide the fields you want to edit");
	}

	@Test
	public void testBuyerReplaceQuoteWhenExpirationTimeIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setExpirationTime(expirationTime);

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not allowed to change expiration date");
	}

	@Test
	public void testSellerReplaceQuoteWhenNameIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setName(QUOTE_NEW_NAME);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLER));

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not allowed to change name or description");
	}

	@Test
	public void testSellerReplaceQuoteWhenDescriptionIsNotNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setDescription(QUOTE_NEW_DESCRIPTION);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLER));

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(IllegalArgumentException.class)
				.hasMessage("User not allowed to change name or description");
	}

	@Test
	public void testSellerReplaceQuoteWhenExpirationTimeIsNotNullAndNameAndDescriptionAreNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setExpirationTime(expirationTime);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLER));

		controller.replaceQuote(QUOTE_CODE, quoteMetadata);

		verify(quoteHelper).updateQuoteMetadata(eq(QUOTE_CODE), cartMetadataCaptor.capture());
		assertThat(cartMetadataCaptor.getValue()).isNotNull()
				.hasFieldOrPropertyWithValue("expirationTime", Optional.of(expirationTime));
	}

	@Test
	public void testApproverReplaceQuote()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.of(QuoteUserType.SELLERAPPROVER));

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(AccessDeniedException.class)
				.hasMessage("Access is denied");
	}

	@Test
	public void testReplaceQuoteWhenUserTypeIsNull()
	{
		final QuoteMetadataWsDTO quoteMetadata = new QuoteMetadataWsDTO();
		quoteMetadata.setExpirationTime(expirationTime);
		when(quoteHelper.getCurrentQuoteUserType()).thenReturn(Optional.empty());

		assertThatThrownBy(() -> controller.replaceQuote(QUOTE_CODE, quoteMetadata)).isInstanceOf(AccessDeniedException.class)
				.hasMessage("Access is denied");
	}

	@Test
	public void testCancelQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_CANCEL);

		controller.performQuoteAction(QUOTE_CODE, quoteAction);

		verify(quoteHelper).cancelQuote(QUOTE_CODE);
	}

	@Test
	public void testPerformQuoteActionWithUnknownAction()
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_UNKNOWN);

		assertThatThrownBy(() -> controller.performQuoteAction(QUOTE_CODE, quoteAction))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("Provided action not supported");
		verifyNoMoreInteractions(quoteHelper);
	}

	@Test
	public void testPerformQuoteActionWithNullAction()
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(null);

		assertThatThrownBy(() -> controller.performQuoteAction(QUOTE_CODE, quoteAction))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Provided action cannot be null");
		verifyNoMoreInteractions(quoteHelper);
	}

	@Test
	public void testCreateQuoteCommentWhenTextIsNotNull()
	{
		final CreateCommentWsDTO quoteComment = new CreateCommentWsDTO();
		quoteComment.setText(QUOTE_NEW_COMMENT);

		controller.createCommentForQuote(QUOTE_CODE, quoteComment);

		verify(quoteCommentValidator).validate(any(), any());
		verify(quoteHelper).addCommentToQuote(QUOTE_CODE, QUOTE_NEW_COMMENT);
	}

	@Test
	public void testCreateQuoteCommentWhenTextIsInvalid()
	{
		final CreateCommentWsDTO quoteComment = new CreateCommentWsDTO();
		quoteComment.setText("invalidText");
		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue("text", "comment is invalid", new String[] {}, null);
			return null;
		}).when(quoteCommentValidator).validate(eq(quoteComment), any());
		assertThatThrownBy(() -> controller.createCommentForQuote(QUOTE_CODE, quoteComment))
				.isInstanceOf(WebserviceValidationException.class);
		verifyNoMoreInteractions(quoteHelper);
	}

	@Test
	public void testCreateQuoteEntryComment()
	{
		final CreateCommentWsDTO quoteComment = new CreateCommentWsDTO();
		quoteComment.setText(QUOTE_NEW_COMMENT);
		controller.createQuoteEntryComment(QUOTE_CODE, ENTRY_NUMBER, quoteComment);
		verify(quoteHelper).addCommentToQuoteEntry(QUOTE_CODE, ENTRY_NUMBER, QUOTE_NEW_COMMENT);
	}

	@Test
	public void testCreateQuoteDiscount()
	{
		final QuoteDiscountWsDTO quoteDiscount = new QuoteDiscountWsDTO();
		final Double discountRate = 10.0;
		final String discountType = "PERCENT";
		quoteDiscount.setDiscountRate(discountRate);
		quoteDiscount.setDiscountType(discountType);

		controller.createQuoteDiscount(QUOTE_CODE, quoteDiscount);

		verify(discountTypeValidator).validate(eq(new String[] {"PERCENT"}), any());
		verify(quoteHelper).applyDiscount(QUOTE_CODE, discountType, discountRate);
	}

	@Test
	public void testCreateQuoteDiscountWithInvalidDiscountType()
	{
		final QuoteDiscountWsDTO quoteDiscount = new QuoteDiscountWsDTO();
		doAnswer(invocationOnMock -> {
			final Errors errors = invocationOnMock.getArgument(1);
			errors.rejectValue(null, "discount is invalid", new String[] {}, null);
			return null;
		}).when(discountTypeValidator).validate(any(), any());
		assertThatThrownBy(() -> controller.createQuoteDiscount(QUOTE_CODE, quoteDiscount))
			.isInstanceOf(WebserviceValidationException.class);
		verifyNoMoreInteractions(quoteHelper);
	}

	@Test
	public void testApproveQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_APPROVE);

		controller.performQuoteAction(QUOTE_CODE, quoteAction);
		verify(quoteHelper).approveQuote(QUOTE_CODE);
	}

	@Test
	public void testRejectQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_REJECT);

		controller.performQuoteAction(QUOTE_CODE, quoteAction);
		verify(quoteHelper).rejectQuote(QUOTE_CODE);
	}

	private List<CartModificationData> getNewCommerceCartModifications()
	{
		final List<CartModificationData> modifications = new ArrayList<>();
		final CartModificationData cartModificationData = new CartModificationData();

		modifications.add(cartModificationData);

		return modifications;
	}

	@Test
	public void testCheckoutQuote() throws VoucherOperationException, CommerceCartModificationException
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_CHECKOUT);
		controller.performQuoteAction(QUOTE_CODE, quoteAction);
		verify(quoteHelper).acceptAndPrepareCheckout(QUOTE_CODE);
	}

	@Test
	public void testEditQuote() throws CommerceCartModificationException, VoucherOperationException
	{
		final QuoteActionWsDTO quoteAction = new QuoteActionWsDTO();
		quoteAction.setAction(QUOTE_ACTION_EDIT);

		controller.performQuoteAction(QUOTE_CODE, quoteAction);
		verify(quoteHelper).enableQuoteEdit(QUOTE_CODE);
	}
}
