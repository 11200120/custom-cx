/*
 * Copyright (c) 2024 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.helper;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.commercefacades.data.SAPAttachmentData;
import de.hybris.platform.commercefacades.order.QuoteAttachmentFacade;
import de.hybris.platform.commercefacades.order.QuoteFacade;
import de.hybris.platform.commercefacades.quote.data.QuoteData;
import de.hybris.platform.servicelayer.exceptions.ModelNotFoundException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;


@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class QuoteAttachmentHelperTest {

    @Mock
    private QuoteFacade quoteFacade;

    @Mock
    private QuoteAttachmentFacade mockQuoteAttachmentFacade;

    @Test
    public void testGetQuoteAttachmentWhenQuoteExistsAndAttachmentExists() {
        // Mock the behavior of getQuoteForCode to return a QuoteData object
        String quoteCode = "quote123";
        String attachmentId = "attachment456";
        QuoteData mockQuoteData = new QuoteData();
        SAPAttachmentData sapAttachmentData = new SAPAttachmentData();
        sapAttachmentData.setId(attachmentId);
        List<SAPAttachmentData> sapAttachmentDataList=new ArrayList<>();
        sapAttachmentDataList.add(sapAttachmentData);
        mockQuoteData.setSapAttachments(sapAttachmentDataList);
        when(quoteFacade.getQuoteForCode(quoteCode)).thenReturn(mockQuoteData);

        // Mock the behavior of getQuoteAttachment to return some byte array
        byte[] expectedAttachment = new byte[0];
        when(mockQuoteAttachmentFacade.getQuoteAttachment(quoteCode, sapAttachmentData)).thenReturn(expectedAttachment);

        // Call the method under test
        QuoteAttachmentHelper quoteAttachmentHelperHelper = new QuoteAttachmentHelper();
        quoteAttachmentHelperHelper.setQuoteFacade(quoteFacade);
        quoteAttachmentHelperHelper.setQuoteAttachmentFacade(mockQuoteAttachmentFacade);
        byte[] actualAttachment = quoteAttachmentHelperHelper.getQuoteAttachment(quoteCode, attachmentId);

        // Assert the result
        assertArrayEquals(expectedAttachment, actualAttachment);
    }

    @Test
    public void testGetQuoteAttachmentWhenQuoteDoesNotExist() {
        // Mock the behavior of getQuoteForCode to throw ModelNotFoundException
        String quoteCode = "quote123";
        String attachmentId = "attachment456";
        when(quoteFacade.getQuoteForCode(quoteCode)).thenThrow(new ModelNotFoundException("No quote found"));

        // Call the method under test and assert that it throws NotFoundException
        QuoteAttachmentHelper quoteAttachmentHelperHelper = new QuoteAttachmentHelper();
        quoteAttachmentHelperHelper.setQuoteFacade(quoteFacade);
        quoteAttachmentHelperHelper.setQuoteAttachmentFacade(mockQuoteAttachmentFacade);
        assertThrows(NotFoundException.class, () -> quoteAttachmentHelperHelper.getQuoteAttachment(quoteCode, attachmentId));
    }
}
