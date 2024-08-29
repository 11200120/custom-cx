/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.validators;

import de.hybris.platform.webservicescommons.validators.AbstractFieldValidator;

import java.util.Collection;

import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

public class CollectionValidator extends AbstractFieldValidator
{
	private Validator validator;

	@Override
	public boolean supports(final Class clazz)
	{
		return true;
	}

	@Override
	public void validate(final Object object, final Errors errors)
	{
		Assert.notNull(errors, "Errors object must not be null");

		final Object fieldValue = errors.getFieldValue(getFieldPath());

		if ( fieldValue == null )
		{
			return;
		}

		if (!(fieldValue instanceof Collection))
		{
			errors.rejectValue(getFieldPath(), "Not a collection", new String[] { getFieldPath() }, "Not a collection");
			return;
		}

		var i = 0;
		for(final Object e : (Collection)fieldValue)
		{
			errors.pushNestedPath(getFieldPath()+"["+(i++)+"]");
			validator.validate(e, errors);
			errors.popNestedPath();
		}
	}

	public void setValidator(final Validator validator)
	{
		this.validator = validator;
	}
}
