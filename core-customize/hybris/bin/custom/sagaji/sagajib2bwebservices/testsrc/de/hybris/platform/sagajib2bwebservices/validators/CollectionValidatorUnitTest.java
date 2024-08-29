/*
 * Copyright (c) 2021 SAP SE or an SAP affiliate company. All rights reserved.
 */
package de.hybris.platform.sagajib2bwebservices.validators;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import de.hybris.bootstrap.annotations.UnitTest;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@UnitTest
@RunWith(MockitoJUnitRunner.class)
public class CollectionValidatorUnitTest
{
	@Mock
	private Validator validator;

	@Mock
	private Errors errors;

	@InjectMocks
	private CollectionValidator collectionValidator;

	private final Object fieldValue = List.of(new Object(), new Object());

	@Before
	public void setUp()
	{
		when(errors.getFieldValue(anyString())).thenReturn(fieldValue);

		collectionValidator.setFieldPath("foobar");
	}
	@Test
	public void testSupports()
	{
		assertThat(collectionValidator.supports(Object.class)).isTrue();
	}

	@Test
	public void testValidateErrorsIsNull()
	{
		assertThatThrownBy(() -> collectionValidator.validate(new Object(), null))
			.isInstanceOf(IllegalArgumentException.class)
			.hasMessage("Errors object must not be null");
	}

	@Test
	public void testValidateCollectionIsNull()
	{
		when(errors.getFieldValue(anyString())).thenReturn(null);
		collectionValidator.validate(new Object(), errors);

		verify(errors).getFieldValue("foobar");
		verifyNoMoreInteractions(validator);
	}

	@Test
	public void testValidateCollectionNotCollection()
	{
		when(errors.getFieldValue(anyString())).thenReturn(new Object());
		collectionValidator.validate(new Object(), errors);

		verify(errors).rejectValue("foobar", "Not a collection", new String[] { "foobar" }, "Not a collection");
		verify(errors).getFieldValue("foobar");
		verifyNoMoreInteractions(validator);
	}

	@Test
	public void testValidateCollectionAllElements()
	{
		collectionValidator.validate(new Object(), errors);

		verify(validator, times(2)).validate(any(), eq(errors));
		verify(errors).pushNestedPath("foobar[0]");
		verify(errors).pushNestedPath("foobar[1]");
		verify(errors, times(2)).popNestedPath();
	}
}
