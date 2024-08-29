/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 *
 */
package de.hybris.platform.sagajib2bwebservices.v2.helper;

import de.hybris.platform.sagajib2bwebservices.exceptions.CartValidationException;
import de.hybris.platform.sagajib2bwebservices.exceptions.QuoteAssemblingException;
import de.hybris.platform.sagajib2bwebservices.exceptions.QuoteException;
import de.hybris.platform.commercefacades.order.CartFacade;
import de.hybris.platform.commercefacades.order.QuoteFacade;
import de.hybris.platform.commercefacades.order.data.CartModificationData;
import de.hybris.platform.commercefacades.order.data.CommerceCartMetadata;
import de.hybris.platform.commercefacades.quote.data.QuoteData;
import de.hybris.platform.commercefacades.quote.data.QuoteListData;
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
import de.hybris.platform.core.enums.QuoteState;
import de.hybris.platform.core.model.order.CartModel;
import de.hybris.platform.core.model.order.QuoteModel;
import de.hybris.platform.core.model.user.UserModel;
import de.hybris.platform.order.CartService;
import de.hybris.platform.order.QuoteService;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class QuoteHelper extends AbstractHelper
{
	private static final String QUOTE_NOT_FOUND_MESSAGE = "Quote not found";
	private static final String QUOTE_ENTRY_NOT_FOUND_MESSAGE = "Quote entry not found";

	@Resource
	private CartLoaderStrategy cartLoaderStrategy;
	@Resource
	private CartFacade cartFacade;
	@Resource
	private QuoteFacade quoteFacade;
	@Resource
	private VoucherFacade voucherFacade;
	@Resource
	private QuoteService quoteService;
	@Resource
	private CartService cartService;
	@Resource
	private Populator<CartModel, QuoteWsDTO> cartModelToQuoteWsDTOPopulator;
	@Resource
	private QuoteUserTypeIdentificationStrategy quoteUserTypeIdentificationStrategy;
	@Resource
	private QuoteUserIdentificationStrategy quoteUserIdentificationStrategy;

	public QuoteWsDTO initiateQuote(final String cartId, final String fields) throws VoucherOperationException, CommerceCartModificationException
	{
		getCurrentQuoteUserType().filter(QuoteUserType.SELLERAPPROVER::equals)
				.ifPresent(userType -> {throw new AccessDeniedException("Access is denied");});

		getCartLoaderStrategy().loadCart(cartId);
		if ( !getCartFacade().hasEntries() )
		{
			throw new QuoteException("Empty carts are not allowed");
		}
		validateCart();
		removeVouchers();
		final QuoteData quoteData = getQuoteFacade().initiateQuote();

		getQuoteFacade().enableQuoteEdit(quoteData.getCode());
		return getQuoteWsDTO(quoteData, fields);
	}

	public QuoteWsDTO requote(final String quoteCode, final String fields)
	{
		try
		{
			getCurrentQuoteUserType().filter(QuoteUserType.SELLERAPPROVER::equals)
					.ifPresent(userType -> {throw new AccessDeniedException("Access is denied");});

			final QuoteData quoteData = getQuoteFacade().requote(quoteCode);

			getQuoteFacade().enableQuoteEdit(quoteData.getCode());
			return getQuoteWsDTO(quoteData, fields);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public QuoteWsDTO getQuote(final String quoteCode, final String fields)
	{
		try
		{
			final QuoteData quoteData = getQuoteFacade().getQuoteForCode(quoteCode);
			return getQuoteWsDTO(quoteData, fields);
		}
		catch(final ModelNotFoundException e)
		{
			throw new NotFoundException(QUOTE_NOT_FOUND_MESSAGE, "notFound", e);
		}
	}

	public QuoteListWsDTO getQuotes(final PageableData pageableData, final String fields)
	{
		final SearchPageData<QuoteData> searchPageData = getQuoteFacade().getPagedQuotes(pageableData);
		final QuoteListData quoteList = new QuoteListData();
		quoteList.setPagination(searchPageData.getPagination());
		quoteList.setQuotes(searchPageData.getResults());
		quoteList.setSorts(searchPageData.getSorts());
		quoteList.getQuotes().forEach(quoteData ->{
			final QuoteModel quoteModel = getQuoteService().getCurrentQuoteForCode(quoteData.getCode());
			final CartModel cartModel = quoteModel.getCartReference();
			if (cartModel != null){
				quoteData.setName(cartModel.getName());
				quoteData.setDescription(cartModel.getDescription());
			}
		});
		return getDataMapper().map(quoteList, QuoteListWsDTO.class, fields);
	}

	public void updateQuoteMetadata(final String quoteCode, final CommerceCartMetadata cartMetadata)
	{
		try
		{
			getQuoteFacade().enableQuoteEdit(quoteCode);
			getCartFacade().updateCartMetadata(cartMetadata);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void addCommentToQuote(final String quoteCode, final String text)
	{
		try
		{
			getQuoteFacade().enableQuoteEdit(quoteCode);
			getQuoteFacade().addComment(text);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void addCommentToQuoteEntry(final String quoteCode, final long entryNumber, final String text)
	{
		try
		{
			getQuoteFacade().enableQuoteEdit(quoteCode);
			getQuoteFacade().addEntryComment(entryNumber, text);
		}
		catch(final IllegalArgumentException e) // entry not found
		{
			if( "Parameter item can not be null".equals(e.getMessage()) )
			{
				throw new QuoteException(QUOTE_ENTRY_NOT_FOUND_MESSAGE, e);
			}
			throw e;
		}
		catch(final ModelNotFoundException e) // quote not found
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void applyDiscount(final String quoteCode, final String discountType, final Double discountRate)
	{
		try
		{
			getQuoteFacade().enableQuoteEdit(quoteCode);
			getQuoteFacade().applyQuoteDiscount(discountRate, discountType);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	protected QuoteWsDTO getQuoteWsDTO(final QuoteData quoteData, final String fields)
	{
		final QuoteWsDTO quoteWsDTO = getDataMapper().map(quoteData, QuoteWsDTO.class, fields);
		try
		{
			final Set<QuoteAction> quoteActionSet = getQuoteFacade().getAllowedActions(quoteData.getCode());
			final double quoteThreshold = getQuoteFacade().getQuoteRequestThreshold(quoteData.getCode());
			final List<String> quoteActions = quoteActionSet.stream()
					.filter(quoteAction -> !QuoteAction.VIEW.equals(quoteAction))
					.filter(quoteAction -> !QuoteAction.SAVE.equals(quoteAction))
					.filter(quoteAction -> !QuoteAction.DISCOUNT.equals(quoteAction))
					.filter(quoteAction -> !QuoteAction.ORDER.equals(quoteAction))
					.filter(quoteAction -> !QuoteAction.EXPIRED.equals(quoteAction))
					.map(QuoteAction::getCode)
					.collect(Collectors.toList());
			quoteWsDTO.setAllowedActions(quoteActions);
			quoteWsDTO.setThreshold(quoteThreshold);
			final QuoteState quoteState = quoteData.getState();
			final QuoteUserType userType = getCurrentQuoteUserType().orElse(null);
			if ((QuoteUserType.BUYER.equals(userType) && QuoteState.BUYER_SUBMITTED.equals(quoteState))
					|| QuoteState.SELLERAPPROVER_APPROVED.equals(quoteState))
			{
				return getDataMapper().map(quoteWsDTO, QuoteWsDTO.class, fields);
			}

			// we obtain the relatedCart
			final QuoteModel quoteModel = getQuoteService().getCurrentQuoteForCode(quoteData.getCode());
			if( quoteModel.getCartReference() != null )
			{
				if (QuoteState.BUYER_OFFER.equals(quoteState))
				{
					final String cartId = quoteModel.getCartReference().getCode();
					quoteWsDTO.setCartId(cartId);
					return getDataMapper().map(quoteWsDTO, QuoteWsDTO.class, fields);
				}
				cartModelToQuoteWsDTOPopulator.populate(quoteModel.getCartReference(), quoteWsDTO);
			}
		}
		catch (final RuntimeException e)
		{
			throw new QuoteAssemblingException(e);
		}
		return getDataMapper().map(quoteWsDTO, QuoteWsDTO.class, fields);
	}

	public void submitQuote(final String quoteCode) throws VoucherOperationException, CommerceCartModificationException
	{
		try
		{
			getQuoteFacade().enableQuoteEdit(quoteCode);
			validateCart();
			removeVouchers();
			getQuoteFacade().submitQuote(quoteCode);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void acceptAndPrepareCheckout(final String quoteCode)
	{
		try
		{
			// for issue CLC-2693/CXEC-107, we checked here as temp solution.
			if (!getQuoteFacade().getAllowedActions(quoteCode).contains(QuoteAction.CHECKOUT)) {
				throw new QuoteException("Action [CHECKOUT] is not allowed for quote code [" + quoteCode + "]");
			}
			getQuoteFacade().acceptAndPrepareCheckout(quoteCode);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void enableQuoteEdit(final String quoteCode)
	{
		try
		{
			getQuoteFacade().enableQuoteEdit(quoteCode);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void approveQuote(final String quoteCode)
	{
		try
		{
			getQuoteFacade().approveQuote(quoteCode);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void rejectQuote(final String quoteCode)
	{
		try
		{
			getQuoteFacade().rejectQuote(quoteCode);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public void cancelQuote(final String quoteCode)
	{
		try
		{
			setSessionCartFromQuote(quoteCode);
			getQuoteFacade().cancelQuote(quoteCode);
		}
		catch(final ModelNotFoundException e)
		{
			throw new QuoteException(QUOTE_NOT_FOUND_MESSAGE, e);
		}
	}

	public Optional<QuoteUserType> getCurrentQuoteUserType()
	{
		final UserModel userModel = getQuoteUserIdentificationStrategy().getCurrentQuoteUser();
		return getQuoteUserTypeIdentificationStrategy().getCurrentQuoteUserType(userModel);
	}

	protected void validateCart() throws CommerceCartModificationException
	{
		final List<CartModificationData> modifications = cartFacade.validateCartData();

		if (!modifications.isEmpty())
		{
			throw new CartValidationException(modifications);
		}
	}

	protected void removeVouchers() throws VoucherOperationException
	{
		final List<VoucherData> vouchers = getVoucherFacade().getVouchersForCart();
		for (final VoucherData voucher : vouchers)
		{
			getVoucherFacade().releaseVoucher(voucher.getCode());
		}
	}

	protected void setSessionCartFromQuote(String quoteCode)
	{
		final QuoteModel quoteModel = getQuoteService().getCurrentQuoteForCode(quoteCode);
		final CartModel cartModel = quoteModel.getCartReference();
		getCartService().setSessionCart(cartModel);
	}

	public CartFacade getCartFacade()
	{
		return cartFacade;
	}

	public void setCartFacade(final CartFacade cartFacade)
	{
		this.cartFacade = cartFacade;
	}

	public VoucherFacade getVoucherFacade()
	{
		return voucherFacade;
	}

	public void setVoucherFacade(final VoucherFacade voucherFacade)
	{
		this.voucherFacade = voucherFacade;
	}

	public QuoteService getQuoteService()
	{
		return quoteService;
	}

	public void setQuoteService(final QuoteService quoteService)
	{
		this.quoteService = quoteService;
	}

	public CartService getCartService()
	{
		return cartService;
	}

	public void setCartService(final CartService cartService)
	{
		this.cartService = cartService;
	}

	public CartLoaderStrategy getCartLoaderStrategy()
	{
		return this.cartLoaderStrategy;
	}

	public void setCartLoaderStrategy(final CartLoaderStrategy cartLoaderStrategy)
	{
		this.cartLoaderStrategy = cartLoaderStrategy;
	}

	public QuoteFacade getQuoteFacade()
	{
		return quoteFacade;
	}

	public void setQuoteFacade(final QuoteFacade quoteFacade)
	{
		this.quoteFacade = quoteFacade;
	}

	public QuoteUserTypeIdentificationStrategy getQuoteUserTypeIdentificationStrategy()
	{
		return quoteUserTypeIdentificationStrategy;
	}

	public void setQuoteUserTypeIdentificationStrategy(final QuoteUserTypeIdentificationStrategy quoteUserTypeIdentificationStrategy)
	{
		this.quoteUserTypeIdentificationStrategy = quoteUserTypeIdentificationStrategy;
	}

	public QuoteUserIdentificationStrategy getQuoteUserIdentificationStrategy()
	{
		return quoteUserIdentificationStrategy;
	}

	public void setQuoteUserIdentificationStrategy(final QuoteUserIdentificationStrategy quoteUserIdentificationStrategy)
	{
		this.quoteUserIdentificationStrategy = quoteUserIdentificationStrategy;
	}
}
