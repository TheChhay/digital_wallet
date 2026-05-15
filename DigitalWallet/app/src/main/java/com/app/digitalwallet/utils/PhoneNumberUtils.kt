package com.app.digitalwallet.utils

object PhoneNumberUtils {
    /**
     * Normalizes a phone number to the international E.164 format for Cambodia (+855).
     * Handles local formats (012...), international without plus (85512...), 
     * and redundant zeros (+855012...).
     */
    fun normalize(phone: String): String {
        // Remove all non-numeric characters except '+'
        val clean = phone.trim().replace(Regex("[^0-9+]"), "")
        
        if (clean.isEmpty()) return ""

        // Initial normalization to get it starting with a consistent prefix if possible
        var normalized = when {
            clean.startsWith("0") -> "+855" + clean.substring(1)
            clean.startsWith("855") -> "+$clean"
            !clean.startsWith("+") -> "+855$clean"
            else -> clean
        }

        // Handle the common mistake of +855 followed by a leading zero: +8550...
        if (normalized.startsWith("+8550")) {
            normalized = "+855" + normalized.substring(5)
        }

        return normalized
    }
}
