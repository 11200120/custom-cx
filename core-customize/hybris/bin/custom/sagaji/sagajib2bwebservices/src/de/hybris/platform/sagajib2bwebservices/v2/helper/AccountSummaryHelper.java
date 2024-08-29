/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.helper;


import de.hybris.platform.b2b.company.B2BCommerceUnitService;
import de.hybris.platform.b2b.model.B2BUnitModel;
import de.hybris.platform.b2bacceleratorfacades.company.impl.DefaultB2BAccountSummaryFacade;
import de.hybris.platform.b2bacceleratorfacades.data.AccountSummaryInfoData;
import de.hybris.platform.b2bacceleratorfacades.document.criteria.validator.DocumentCriteriaValidator;
import de.hybris.platform.b2bacceleratorfacades.document.data.AttachmentData;
import de.hybris.platform.b2bacceleratorfacades.document.data.B2BAmountBalanceData;
import de.hybris.platform.b2bacceleratorfacades.document.data.B2BDocumentData;
import de.hybris.platform.b2bacceleratorfacades.document.data.OrgDocumentListData;
import de.hybris.platform.b2bacceleratorservices.document.criteria.DefaultCriteria;
import de.hybris.platform.b2bacceleratorservices.document.criteria.FilterByCriteriaData;
import de.hybris.platform.b2bacceleratorservices.enums.DocumentStatus;
import de.hybris.platform.b2bacceleratorservices.model.B2BDocumentModel;
import de.hybris.platform.b2bacceleratorservices.model.DocumentMediaModel;
import de.hybris.platform.b2bcommercefacades.company.data.B2BUnitData;
import de.hybris.platform.b2bwebservicescommons.dto.company.AccountSummaryWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.AmountBalanceWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgDocumentListWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgUnitReferenceWsDTO;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.servicelayer.exceptions.UnknownIdentifierException;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;

import javax.annotation.Resource;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.microsoft.sqlserver.jdbc.StringUtils;


@Component
public class AccountSummaryHelper extends AbstractHelper
{
	private static final Logger LOG = LoggerFactory.getLogger(AccountSummaryHelper.class);
	@Resource(name = "defaultB2BAccountSummaryFacade")
	private DefaultB2BAccountSummaryFacade accountSummaryFacade;

	@Resource(name = "b2bCommerceUnitService")
	private B2BCommerceUnitService b2bCommerceUnitService;

	@Resource(name = "orgDocumentFilterByList")
	private Map<String, DefaultCriteria> orgDocumentFilterByList;

	@Resource(name = "documentValidatorMapping")
	private Map<String, DocumentCriteriaValidator> documentValidatorMapping;

	private static final String UNIT_NOT_FOUND = "Unit with id [%s] not found for current user";
	private static final String INVALID_INPUT = "%s is not a valid input";
	private static final String DOCUMENT_TYPE_FILTER_KEY = "orgDocumentType";

	/**
	 * @param b2bUnitCode
	 * @param b2bDocumentStatus
	 * @param filterKey
	 * @param filterByCriteriaData
	 * @param pageableData
	 * @param fields
	 * @return OrgDocumentListWsDTO based on the criteria
	 */
	public OrgDocumentListWsDTO searchOrgDocuments(final String b2bUnitCode, final String b2bDocumentStatus,
			final String filterKey, final FilterByCriteriaData filterByCriteriaData, final PageableData pageableData,
			final String fields)
	{
		validateCriterias(b2bUnitCode, b2bDocumentStatus, pageableData.getSort());
		validateFilterRanges(filterKey, filterByCriteriaData);

		final DefaultCriteria defaultCriteria = orgDocumentFilterByList.get(filterKey);

		final OrgDocumentListData<B2BDocumentData> searchPageData = accountSummaryFacade.getOrgDocumentList(b2bUnitCode,
				pageableData, filterByCriteriaData, defaultCriteria);

		return getDataMapper().map(searchPageData, OrgDocumentListWsDTO.class, fields);
	}

	public AccountSummaryWsDTO getAccountSummaryDetails(String unit)
	{
		AccountSummaryInfoData accountSummaryInfoData = accountSummaryFacade.getAccountSummaryInfoData(unit);

		B2BUnitData unitData = accountSummaryInfoData.getB2bUnitData();
		OrgUnitReferenceWsDTO orgUnit = getDataMapper().map(unitData, OrgUnitReferenceWsDTO.class);
		B2BAmountBalanceData b2BAmountBalanceData = accountSummaryInfoData.getAmountBalanceData();
		AmountBalanceWsDTO amountBalanceWsDTO = getDataMapper().map(b2BAmountBalanceData, AmountBalanceWsDTO.class);
		AccountSummaryWsDTO accountSummaryDTO = getDataMapper().map(accountSummaryInfoData, AccountSummaryWsDTO.class);
		accountSummaryDTO.setOrgUnit(orgUnit);
		accountSummaryDTO.setAmountBalance(amountBalanceWsDTO);
		accountSummaryDTO.setCreditLimit(accountSummaryInfoData.getFormattedCreditLimit());
		return accountSummaryDTO;
	}

	/**
	 * @param filterKey
	 * @param b2bDocumentStatus
	 * @param filterValue
	 * @param startRange
	 * @param endRange
	 * @return generated FilterByCriteriaData based on the params
	 */
	public FilterByCriteriaData createFilterByCriteriaData(final String filterKey, final String b2bDocumentStatus,
			final String filterValue, final String startRange, final String endRange)
	{
		final FilterByCriteriaData filterByCriteriaData = new FilterByCriteriaData();
		filterByCriteriaData.setDocumentStatus(b2bDocumentStatus.toUpperCase(Locale.ENGLISH));
		filterByCriteriaData.setStartRange(startRange);
		filterByCriteriaData.setEndRange(endRange);

		if (filterKey.equals(DOCUMENT_TYPE_FILTER_KEY))
		{
			filterByCriteriaData.setDocumentTypeCode(filterValue);
		}
		else
		{
			filterByCriteriaData.setFilterByValue(filterValue);
		}

		return filterByCriteriaData;
	}

	protected void validateCriterias(String b2bUnitCode, String b2bDocumentStatus, String sortCriteria)
	{
		final B2BUnitModel unitModel = b2bCommerceUnitService.getUnitForUid(b2bUnitCode);
		if (unitModel == null)
		{
			final String message = String.format(UNIT_NOT_FOUND, sanitize(b2bUnitCode));
			LOG.error(message);
			throw new UnknownIdentifierException(message);
		}

		final String sortingValue = accountSummaryFacade.getOrgDocumentToModelSortMap().get(sortCriteria);
		if (sortingValue == null)
		{
			logAndThrowErrorForInvalidInput(sortCriteria);
		}

		DocumentStatus documentStatus = null;
		for (DocumentStatus status : DocumentStatus.values())
		{
			if (status.name().equalsIgnoreCase(b2bDocumentStatus))
			{
				documentStatus = status;
				break;
			}
		}

		if (documentStatus == null && !"all".equalsIgnoreCase(b2bDocumentStatus))
		{
			logAndThrowErrorForInvalidInput(b2bDocumentStatus);
		}
	}

	/**
	 * @param filterKey
	 * @param filterByCriteriaData Checks if filter key and values are valid according to filterKey type
	 */
	protected void validateFilterRanges(final String filterKey, final FilterByCriteriaData filterByCriteriaData)
	{
		final String startRange = filterByCriteriaData.getStartRange();
		final String endRange = filterByCriteriaData.getEndRange();
		final String filterValue = filterByCriteriaData.getFilterByValue();

		if (!orgDocumentFilterByList.containsKey(filterKey))
		{
			logAndThrowErrorForInvalidInput(filterKey);
		}
		if (documentValidatorMapping.containsKey(filterKey) && !documentValidatorMapping.get(filterKey)
				.isValid(startRange, endRange))
		{
			logAndThrowErrorForInvalidInput("filter range");
		}

		if (filterKey.toLowerCase(Locale.ENGLISH).contains("range"))
		{
			if ((startRange.equals(StringUtils.EMPTY) && endRange.equals(StringUtils.EMPTY)) || !filterValue.equals(
					StringUtils.EMPTY))
			{
				logAndThrowErrorForInvalidInput("For this range filter key, filter value(s)");
			}
		}
		else
		{
			if (!startRange.equals(StringUtils.EMPTY) || !endRange.equals(StringUtils.EMPTY))
			{
				logAndThrowErrorForInvalidInput("For this filter key, range value");
			}
		}
	}

	protected void logAndThrowErrorForInvalidInput(final String invalidInput)
	{
		final String message = String.format(INVALID_INPUT, sanitize(invalidInput));
		LOG.error(message);
		throw new UnknownIdentifierException(message);
	}

	/**
	 * Returns an AttachmentData object containing media file, name, type associated with a b2b document.
	 *
	 * @param orgUnitId
	 * @param documentNumber
	 * @param documentMediaId
	 * @return attachmentData
	 * @throws NotFoundException
	 */
	public AttachmentData getMediaForDocument(String orgUnitId, String documentNumber, String documentMediaId) throws NotFoundException
	{
		// retrieve DocumentModel by documentNumber
		final B2BDocumentModel documentModel = accountSummaryFacade.getDocumentByIdForUnit(orgUnitId, documentNumber);

		// get documentMedia model and retrieve mediaId (media code)
		final DocumentMediaModel documentMediaModel = documentModel.getDocumentMedia();
		if (documentMediaModel == null)
		{
			throw new NotFoundException(
					"Document with id = %s in organizational unit %s doesn't have an attachment with id = %s.".formatted(
               documentNumber, orgUnitId, documentMediaId), "The document with the given id does not have any attachments.", documentNumber);
		}
		if (documentMediaId.compareTo(documentMediaModel.getCode()) == 0)
		{
			return accountSummaryFacade.getB2BDocumentAttachmentData(documentMediaModel);
		}
		else
		{
			throw new NotFoundException(
					"Attachment with id = %s not found with the given document identifier %s.".formatted(documentMediaId, documentNumber),
					"The document with the given id has an attachment, but it does not correspond to the provided attachment id.", documentMediaId);
		}
	}
}

