/*
 * Copyright (c) 2019 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.validators;

import de.hybris.platform.b2b.enums.CheckoutPaymentType;
import de.hybris.platform.commercefacades.order.data.CartData;

import org.springframework.validation.Errors;
import org.springframework.validation.Validator;


/**
 * B2B cart validator. Checks if cart is calculated and if needed values are filled.
 */
public class B2BPlaceOrderCartValidator implements Validator
{
	@Override
	public boolean supports(final Class<?> clazz)
	{
		return CartData.class.equals(clazz);
	}

	@Override
	public void validate(final Object target, final Errors errors)
	{
		final CartData cart = (CartData) target;

		if (!cart.isCalculated())
		{
			errors.reject("cart.notCalculated");
		}

		if (cart.getDeliveryAddress() == null)
		{
			errors.reject("cart.deliveryAddressNotSet");
		}

		if (cart.getDeliveryMode() == null)
		{
			errors.reject("cart.deliveryModeNotSet");
		}

		if (CheckoutPaymentType.CARD.getCode().equals(cart.getPaymentType().getCode()))
		{
			if (cart.getPaymentInfo() == null)
			{
				errors.reject("cart.paymentInfoNotSet");
			}
		}
		else
		{
			if (cart.getCostCenter() == null)
			{
				errors.reject("cart.costCenterNotSet");
			}
		}
	}
}
