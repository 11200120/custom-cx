/*
 * Copyright (c) 2023 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.v2.controllers;
import de.hybris.platform.b2bacceleratorfacades.document.data.AttachmentData;
import de.hybris.platform.b2bacceleratorservices.document.criteria.FilterByCriteriaData;
import de.hybris.platform.sagajib2bwebservices.security.SecuredAccessConstants;
import de.hybris.platform.sagajib2bwebservices.v2.helper.AccountSummaryHelper;
import de.hybris.platform.b2bwebservicescommons.dto.company.AccountSummaryWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.OrgDocumentListWsDTO;
import de.hybris.platform.commerceservices.request.mapping.annotation.ApiVersion;
import de.hybris.platform.commerceservices.search.pagedata.PageableData;
import de.hybris.platform.webservicescommons.errors.exceptions.NotFoundException;
import de.hybris.platform.webservicescommons.mapping.DataMapper;
import de.hybris.platform.webservicescommons.mapping.FieldSetLevelHelper;
import de.hybris.platform.webservicescommons.swagger.ApiBaseSiteIdAndUserIdParam;
import de.hybris.platform.webservicescommons.swagger.ApiFieldsParam;
import de.hybris.platform.webservicescommons.util.YSanitizer;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import org.apache.commons.lang.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;


@RestController
@RequestMapping(value = "/{baseSiteId}/users/{userId}/orgUnits/{orgUnitId}")
@ApiVersion("v2")
@Tag(name = "Organizational Unit Account Summary")
public class AccountSummaryController extends BaseController
{

	@Resource(name = "dataMapper")
	protected DataMapper dataMapper;

	@Resource(name = "accountSummaryHelper")
	private AccountSummaryHelper accountSummaryHelper;

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP })
	@Operation(operationId = "getOrgDocuments", summary = "Retrieves the financial organizational documents.", description = "Retrieves the list of financial documents for the organizational unit.")
	@GetMapping(value = "/orgDocuments", produces = "application/json")
	@ApiBaseSiteIdAndUserIdParam
	public OrgDocumentListWsDTO getDocumentsList(
			@Parameter(description = "Current result page. Default value is 0.") @RequestParam(value = "page", defaultValue = "0") final int page,
			@Parameter(description = "Number of results returned per page.",  example = "20") @RequestParam(value = "pageSize", defaultValue = "10") final int pageSize,
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable(value = "orgUnitId") final String orgUnitId,
			@Parameter(description = "Organizational document status. Possible values are: open, closed, and all.") @RequestParam(value = "status", defaultValue = "open") final String status,
			@Parameter(description = "Sorting method applied to the return results. Default value is: byCreatedAtDateAsc.", example = "byCreatedAtDateAsc") @RequestParam(required = false, defaultValue = "byCreatedAtDateAsc", value = "sort") final String sortCode,			@Parameter(description = "Lower limit for a specified range filter (for range filterByKeys: orgDocumentIdRange, createdAtDateRange (format: MM/dd/yyyy), dueAtDateRange (format: MM/dd/yyyy), amountRange (number) and openAmountRange (number).)") @RequestParam(required = false, defaultValue = StringUtils.EMPTY, value = "startRange") final String startRange,
			@Parameter(description = "Upper limit for a specified range filter (for range filterByKeys: orgDocumentIdRange, createdAtDateRange (format: MM/dd/yyyy), dueAtDateRange (format: MM/dd/yyyy), amountRange (number) and openAmountRange (number).)") @RequestParam(required = false, defaultValue = StringUtils.EMPTY, value = "endRange") final String endRange,
			@Parameter(description =
					"Filter to apply on the retrieved list of organizational documents. Possible values are: orgDocumentId, orgDocumentIdRange, orgDocumentType, createdAtDateRange, dueAtDateRange, "
							+ "amountRange, and openAmountRange.") @RequestParam(defaultValue = "orgDocumentId", value = "filterByKey") final String filterByKey,
			@Parameter(description = "Value for a specified filter (for single value filterByKeys: orgDocumentId and orgDocumentType.)") @RequestParam(required = false, defaultValue = StringUtils.EMPTY, value = "filterByValue") final String filterByValue,
			@ApiFieldsParam @RequestParam(required = false, defaultValue = FieldSetLevelHelper.DEFAULT_LEVEL) final String fields)
	{
		final FilterByCriteriaData filterByCriteriaData = accountSummaryHelper.createFilterByCriteriaData(filterByKey, status,
				YSanitizer.sanitize(filterByValue), YSanitizer.sanitize(startRange), YSanitizer.sanitize(endRange));
		final PageableData pageableData = new PageableData();
		pageableData.setPageSize(pageSize);
		pageableData.setCurrentPage(page);
		pageableData.setSort(sortCode);

		return accountSummaryHelper.searchOrgDocuments(orgUnitId, status, filterByKey, filterByCriteriaData, pageableData, fields);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP })
	@Operation(operationId = "getAccountSummary", summary = "Retrieves the account summary.", description = "Retrieves the account summary for the organizational unit.")
	@GetMapping(value = "/accountSummary", produces = "application/json")
	@ApiBaseSiteIdAndUserIdParam
	public AccountSummaryWsDTO getAccountSummary(
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable(value = "orgUnitId") final String orgUnitId)
	{
		return accountSummaryHelper.getAccountSummaryDetails(orgUnitId);
	}

	@Secured({ SecuredAccessConstants.ROLE_B2BADMINGROUP })
	@Operation(operationId = "getOrgDocumentAttachment", summary = "Retrieves the attachment of a document.", description = "Retrieves the attachment associated with a given organizational document for the given attachment identifier.")
	@GetMapping(value = "/orgDocuments/{orgDocumentId}/attachments/{orgDocumentAttachmentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ApiBaseSiteIdAndUserIdParam
	public @ResponseBody ResponseEntity<byte[]> getAttachmentForDocument(
			@Parameter(description = "Organizational unit identifier.", example = "RUSTIC", required = true) @PathVariable(value = "orgUnitId") final String orgUnitId,
			@Parameter(description = "Organizational document identifier.", example = "0000001", required = true) @PathVariable final String orgDocumentId,
			@Parameter(description = "Organizational document attachment identifier.", example = "0000001", required = true) @PathVariable final String orgDocumentAttachmentId)
			throws NotFoundException
	{
		final AttachmentData attachmentData = accountSummaryHelper.getMediaForDocument(orgUnitId, orgDocumentId, orgDocumentAttachmentId);

		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CONTENT_TYPE, attachmentData.getFileType());
		headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(attachmentData.getFileName()));
		return ResponseEntity.ok().headers(headers).body(attachmentData.getFileContent().getByteArray());
	}
}
