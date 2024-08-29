/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.helper;

import de.hybris.platform.commercefacades.data.SAPAttachmentData;
import de.hybris.platform.commercefacades.order.QuoteAttachmentFacade;
import de.hybris.platform.commercefacades.order.QuoteFacade;
import de.hybris.platform.commercefacades.quote.data.QuoteData;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Optional;

@Component
public class QuoteAttachmentHelper extends AbstractHelper{

    private static final String QUOTE_NOT_FOUND_MESSAGE = "Quote not found";

    @Resource
    private QuoteAttachmentFacade quoteAttachmentFacade;

    @Resource
    private QuoteFacade quoteFacade;

    /**
     * Method to get quote attachment based on quoteCode and attachmentId.
     * @param quoteCode Quote Code
     * @param attachmentId Attachment Id
     * @return byte[]
     */
    public byte[] getQuoteAttachment(final String quoteCode, final String attachmentId)
    {
        try
        {
            final QuoteData quoteData = getQuoteFacade().getQuoteForCode(quoteCode);
            Optional<SAPAttachmentData> sapAttachmentData = quoteData.getSapAttachments().stream()
                    .filter(attachment -> attachment.getId().equals(attachmentId))
                    .findFirst();
            if(sapAttachmentData.isPresent()) {
                return getQuoteAttachmentFacade().getQuoteAttachment(quoteCode,sapAttachmentData.get());
            }
            return new byte[0];
        }
        catch(final ModelNotFoundException e)
        {
            throw new NotFoundException(QUOTE_NOT_FOUND_MESSAGE, "notFound", e);
        }
    }
    public QuoteFacade getQuoteFacade()
    {
        return quoteFacade;
    }

    public void setQuoteFacade(final QuoteFacade quoteFacade)
    {
        this.quoteFacade = quoteFacade;
    }

    public QuoteAttachmentFacade getQuoteAttachmentFacade() {
        return quoteAttachmentFacade;
    }

    public void setQuoteAttachmentFacade(QuoteAttachmentFacade quoteAttachmentFacade) {
        this.quoteAttachmentFacade = quoteAttachmentFacade;
    }
}
