/*******************************************************************************
 * Copyright (c) 2015-2025 Maxprograms.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 1.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/org/documents/epl-v10.html
 *
 * Contributors:
 *     Maxprograms - initial API and implementation
 *******************************************************************************/

package com.maxprograms.utils;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class TextUtilsTest {

	@Before
	public void setUp() {
		// Setup before each test
	}

	@Test
	public void testGetIndex_FindElement() {
		String[] items = { "apple", "banana", "cherry", "date" };
		int index = TextUtils.geIndex(items, "cherry");
		assertEquals("Should find index of cherry at position 2", 2, index);
	}

	@Test
	public void testGetIndex_NotFound() {
		String[] items = { "apple", "banana", "cherry" };
		int index = TextUtils.geIndex(items, "grape");
		assertEquals("Should return -1 when element not found", -1, index);
	}

	@Test
	public void testGetIndex_FirstElement() {
		String[] items = { "apple", "banana", "cherry" };
		int index = TextUtils.geIndex(items, "apple");
		assertEquals("Should find first element at index 0", 0, index);
	}

	@Test
	public void testGetIndex_LastElement() {
		String[] items = { "apple", "banana", "cherry" };
		int index = TextUtils.geIndex(items, "cherry");
		assertEquals("Should find last element", 2, index);
	}

	@Test
	public void testNormalise_RemoveExtraSpaces() {
		String input = "hello   world  how  are  you";
		String result = TextUtils.normalise(input);
		assertEquals("Should normalize multiple spaces to single space", "hello world how are you", result);
	}

	@Test
	public void testNormalise_RemoveLeadingTrailingSpaces() {
		String input = "   hello world   ";
		String result = TextUtils.normalise(input);
		assertEquals("Should trim leading and trailing spaces", "hello world", result);
	}

	@Test
	public void testNormalise_ReplaceNewlinesWithSpaces() {
		String input = "hello\nworld\nhow\nare\nyou";
		String result = TextUtils.normalise(input);
		assertEquals("Should replace newlines with spaces", "hello world how are you", result);
	}

	@Test
	public void testNormalise_MixedWhitespace() {
		String input = "  hello  \n  world  \n  test  ";
		String result = TextUtils.normalise(input);
		assertEquals("Should normalize mixed whitespace", "hello world test", result);
	}

	@Test
	public void testNormalise_NoTrim() {
		String input = "  hello world  ";
		String result = TextUtils.normalise(input, false);
		assertEquals("Should not trim when trim is false", " hello world ", result);
	}

	@Test
	public void testNormalise_EmptyString() {
		String input = "";
		String result = TextUtils.normalise(input);
		assertEquals("Should handle empty string", "", result);
	}

	@Test
	public void testPad_SingleDigit() {
		String result = TextUtils.pad(5, 2);
		assertEquals("Should pad single digit with zero", "05", result);
	}

	@Test
	public void testPad_AlreadyPadded() {
		String result = TextUtils.pad(42, 2);
		assertEquals("Should not pad when not needed", "42", result);
	}

	@Test
	public void testPad_MultipleZeros() {
		String result = TextUtils.pad(7, 4);
		assertEquals("Should pad with multiple zeros", "0007", result);
	}

	@Test
	public void testPad_Zero() {
		String result = TextUtils.pad(0, 3);
		assertEquals("Should handle zero padding", "000", result);
	}

	@Test
	public void testGetGMTtime_InvalidDate() {
		String tmxDate = "invalid";
		long result = TextUtils.getGMTtime(tmxDate);
		assertEquals("Should return 0 for invalid date", 0, result);
	}

	@Test
	public void testGetGMTtime_TooShortString() {
		String tmxDate = "2025";
		long result = TextUtils.getGMTtime(tmxDate);
		assertEquals("Should return 0 for too short date string", 0, result);
	}

	@Test
	public void testGetGMTtime_ReturnsLongValue() {
		// Test that getGMTtime returns a long value (even if 0 for invalid input)
		String tmxDate = "invalid";
		long result = TextUtils.getGMTtime(tmxDate);
		assertTrue("Result should be a valid long value", result >= 0);
	}

	@Test
	public void testDate2String_BasicDate() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2025, 1, 1, 14, 30); // February 1, 2025 at 14:30
		Date date = calendar.getTime();

		String result = TextUtils.date2string(date);

		// Result should be in format: YYYY-MM-DD HH:MM
		assertTrue("Should contain date portion", result.contains("2025-02-01"));
		assertTrue("Should contain time portion", result.contains("14:30"));
	}

	@Test
	public void testDate2String_PaddingFormat() {
		Calendar calendar = Calendar.getInstance();
		calendar.set(2025, 0, 5, 9, 5); // January 5, 2025 at 09:05
		Date date = calendar.getTime();

		String result = TextUtils.date2string(date);

		// Verify proper zero-padding
		assertEquals("Should properly format date with padding", "2025-01-05 09:05", result);
	}
}
