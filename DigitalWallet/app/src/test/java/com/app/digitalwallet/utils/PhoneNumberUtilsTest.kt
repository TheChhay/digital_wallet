package com.app.digitalwallet.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneNumberUtilsTest {

    @Test
    fun `normalize local format starting with 0`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("012345678"))
    }

    @Test
    fun `normalize international format without plus`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("85512345678"))
    }

    @Test
    fun `normalize international format with redundant zero`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("+855012345678"))
    }

    @Test
    fun `normalize number without prefix`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("12345678"))
    }

    @Test
    fun `normalize already correct format`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("+85512345678"))
    }

    @Test
    fun `normalize with spaces and dashes`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("012 345 678"))
        assertEquals("+85512345678", PhoneNumberUtils.normalize("012-345-678"))
    }

    @Test
    fun `normalize empty string`() {
        assertEquals("", PhoneNumberUtils.normalize(""))
    }

    @Test
    fun `normalize with mixed characters`() {
        assertEquals("+85512345678", PhoneNumberUtils.normalize("(012) 345-678"))
    }
}
