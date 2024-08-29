/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.mapping.mappers;


import de.hybris.platform.b2bacceleratorfacades.document.data.B2BDocumentData;
import de.hybris.platform.b2bwebservicescommons.dto.OrgDocumentAttachmentWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BOrgDocumentTypeWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.B2BOrgDocumentWsDTO;
import de.hybris.platform.webservicescommons.mapping.mappers.AbstractCustomMapper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import ma.glasnost.orika.MappingContext;


public class B2BDocumentMapper extends AbstractCustomMapper<B2BDocumentData, B2BOrgDocumentWsDTO>
{
	private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	@Override
	public void mapAtoB(final B2BDocumentData a, final B2BOrgDocumentWsDTO b, final MappingContext context) {

		//other fields are mapped automatically
		List<OrgDocumentAttachmentWsDTO> attachments = new ArrayList<>();
		b.setId(a.getDocumentNumber());
		b.setOrgDocumentType(mapperFacade.map(a.getDocumentType(), B2BOrgDocumentTypeWsDTO.class));
		OrgDocumentAttachmentWsDTO attachment = mapperFacade.map(a.getMediaId(), OrgDocumentAttachmentWsDTO.class);
		if (attachment != null) {
			attachments.add(mapperFacade.map(a.getMediaId(), OrgDocumentAttachmentWsDTO.class));
		}
		b.setAttachments(attachments);
		if (a.getDate() != null) {
			b.setCreatedAtDate(sdf.format(a.getDate()));
		}
		if (a.getDueDate() != null) {
			b.setDueAtDate(sdf.format(a.getDueDate()));
		}
	}

}
