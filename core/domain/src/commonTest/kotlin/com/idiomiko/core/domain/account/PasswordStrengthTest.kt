package com.idiomiko.core.domain.account

import com.idiomiko.core.domain.account.PasswordStrength.Level
import com.idiomiko.core.domain.account.PasswordStrength.Rule
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PasswordStrengthTest {
    @Test
    fun eachRuleIsMetOnItsOwn() {
        assertTrue(PasswordStrength.meets("12345678", Rule.LENGTH))
        assertFalse(PasswordStrength.meets("1234567", Rule.LENGTH))
        assertTrue(PasswordStrength.meets("abc4", Rule.DIGIT))
        assertTrue(PasswordStrength.meets("ABCd", Rule.LOWERCASE))
        assertTrue(PasswordStrength.meets("abcD", Rule.UPPERCASE))
        assertTrue(PasswordStrength.meets("abc@", Rule.SYMBOL))
        assertFalse(PasswordStrength.meets("abc d", Rule.SYMBOL), "a space is no symbol")
    }

    @Test
    fun theLevelRisesWithTheRulesMetAndTheLength() {
        assertEquals(Level.VERY_WEAK, PasswordStrength.level(""))
        assertEquals(Level.VERY_WEAK, PasswordStrength.level("abc"))
        assertEquals(Level.WEAK, PasswordStrength.level("abcdefgh"))
        assertEquals(Level.WEAK, PasswordStrength.level("abcdefg1"))
        assertEquals(Level.FAIR, PasswordStrength.level("Abcdefg1"))
        assertEquals(Level.GOOD, PasswordStrength.level("Abcdef1!"))
        assertEquals(Level.STRONG, PasswordStrength.level("Correct-horse-7"))
        assertTrue(PasswordStrength.meetsAll("Abcdef1!"))
        assertFalse(PasswordStrength.meetsAll("Abcdefg1"))
    }
}
