package com.sourcepoint.mobile_core.asserters

import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.fail

fun assertIsEmpty(actual: String?, message: String = "Expected string $actual to be empty") {
    actual ?: { fail("Expected collection not to be empty, but it's null.") }
    assertTrue(actual!!.isEmpty(), message)
}

fun assertIsEmpty(actual: Collection<*>?, message: String = "Expected collection $actual to be empty") {
    actual ?: { fail("Expected collection not to be empty, but it's null.") }
    assertTrue(actual!!.isEmpty(), message)
}

fun assertIsEmpty(actual: Map<*, *>?, message: String = "Expected map $actual to be empty") {
    actual ?: { fail("Expected map not to be empty, but it's null.") }
    assertTrue(actual!!.isEmpty(), message)
}

fun assertNotEmpty(actual: String?, message: String = "Expected string $actual not to be empty") {
    actual ?: { fail("Expected string not to be empty, but it's null.") }
    assertTrue(actual!!.isNotEmpty(), message)
}

fun assertNotEmpty(actual: Collection<*>?, message: String = "Expected collection $actual not to be empty") {
    actual ?: { fail("Expected collection not to be empty, but it's null.") }
    assertTrue(actual!!.isNotEmpty(), message)
}

fun assertNotEmpty(actual: Map<*, *>?, message: String = "Expected map $actual not to be empty") {
    actual ?: { fail("Expected map not to be empty, but it's null.") }
    assertTrue(actual!!.isNotEmpty(), message)
}

fun <T> assertDoesNotContain(iterable: Iterable<T>?, element: T, message: String = "Expected $iterable not to contain $element") {
    iterable ?: { fail("Expected collection not to contain $element, but it's null.") }
    iterable!!.firstOrNull { it == element }?.let {
        fail(message)
    }
}

fun <T> assertContains(iterable: Iterable<T>?, element: T, message: String = "Expected $iterable not to contain $element") {
    iterable ?: { fail("Expected collection to contain $element, but it's null.") }
    kotlin.test.assertContains(iterable!!, element, message)
}

fun assertTrue(actual: Boolean?) {
    actual ?: { fail("Expected value to be true, but it's null") }
    assertTrue(actual!!)
}

fun assertFalse(actual: Boolean?) {
    actual ?: { fail("Expected value to be false, but it's null") }
    assertFalse(actual!!)
}
