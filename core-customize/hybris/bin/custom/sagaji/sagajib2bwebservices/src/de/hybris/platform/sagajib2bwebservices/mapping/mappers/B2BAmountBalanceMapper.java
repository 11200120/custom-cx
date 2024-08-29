/*
 * Copyright (c) 2022 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.mapping.mappers;

import de.hybris.platform.b2bacceleratorfacades.document.data.B2BAmountBalanceData;
import de.hybris.platform.b2bacceleratorfacades.document.data.B2BNumberOfDayRangeData;
import de.hybris.platform.b2bwebservicescommons.dto.company.AmountBalanceWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.DueBalanceRangeWsDTO;
import de.hybris.platform.b2bwebservicescommons.dto.company.DayRangeWsDTO;
import de.hybris.platform.webservicescommons.mapping.mappers.AbstractCustomMapper;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import ma.glasnost.orika.MappingContext;

public class B2BAmountBalanceMapper extends AbstractCustomMapper<B2BAmountBalanceData, AmountBalanceWsDTO>
{
	@Override
	public void mapAtoB(final B2BAmountBalanceData a, final AmountBalanceWsDTO b, final MappingContext context) {
		Map<B2BNumberOfDayRangeData,String> numberOfDayRangeDataStringMap = a.getDueBalance();
		List<DueBalanceRangeWsDTO> dueBalanceRangeWsDTOList = new ArrayList<>();
		for (Map.Entry<B2BNumberOfDayRangeData, String> entry: numberOfDayRangeDataStringMap.entrySet()) {
			DueBalanceRangeWsDTO dto = new DueBalanceRangeWsDTO();
			DayRangeWsDTO dayRangeWsDTO = new DayRangeWsDTO();
			dayRangeWsDTO.setMinBoundary(entry.getKey().getMinBoundery());
			dayRangeWsDTO.setMaxBoundary(entry.getKey().getMaxBoundery());
			dto.setDayRange(dayRangeWsDTO);
			dto.setAmount(entry.getValue());
			dueBalanceRangeWsDTOList.add(dto);
		}
		b.setDueBalances(dueBalanceRangeWsDTOList);
	}

}
