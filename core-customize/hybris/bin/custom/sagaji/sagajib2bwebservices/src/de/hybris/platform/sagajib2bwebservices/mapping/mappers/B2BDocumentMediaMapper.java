/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.mapping.mappers;

import de.hybris.platform.b2bwebservicescommons.dto.OrgDocumentAttachmentWsDTO;
import de.hybris.platform.webservicescommons.mapping.mappers.AbstractCustomMapper;

import ma.glasnost.orika.MappingContext;


public class B2BDocumentMediaMapper extends AbstractCustomMapper<String, OrgDocumentAttachmentWsDTO>
{
	@Override
	public void mapAtoB(final String a, final OrgDocumentAttachmentWsDTO b, final MappingContext context) {
		b.setId(a);
	}
}
