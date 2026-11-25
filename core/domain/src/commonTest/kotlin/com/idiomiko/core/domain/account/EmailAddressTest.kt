package com.idiomiko.core.domain.account

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class EmailAddressTest {
    @Test
    fun addressesAreTakenWithSpacesAroundThem() {
        for (email in listOf("you@example.com", "first.last+tag@mail.example.co.uk", " you@example.com ", "o'brien@example.ie", "anna@почта.рф", "x@a-b.io")) {
            assertTrue(EmailAddress.isValid(email), email)
        }
    }

    @Test
    fun whatIsNoAddressIsTurnedAway() {
        for (email in listOf(
            "", "zavanton", "zavanton@", "@example.com", "you@example", "you@example.c", "you@@example.com", "you@exa mple.com",
            "you me@example.com", ".you@example.com", "you.@example.com", "you..me@example.com", "you@-example.com", "you@example-.com",
            "you@example..com", "you@example.com.", "you@example.123", "you,me@example.com", "you@exam_ple.com",
        )) {
            assertFalse(EmailAddress.isValid(email), email)
        }
    }
}
