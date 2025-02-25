package com.sourcepoint.mobile_core.asserters

import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.fail

fun assertIsEmpty(actual: String?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected collection not to be empty, but it's null.") }
    assertTrue(actual.isEmpty(), message ?: "Expected string $actual to be empty")
}

fun assertIsEmpty(actual: Collection<*>?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected collection not to be empty, but it's null.") }
    assertTrue(actual.isEmpty(), message ?: "Expected collection $actual to be empty")
}

fun assertIsEmpty(actual: Map<*, *>?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected map not to be empty, but it's null.") }
    assertTrue(actual.isEmpty(), message ?: "Expected map $actual to be empty")
}

fun assertNotEmpty(actual: String?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected string not to be empty, but it's null.") }
    assertTrue(actual.isNotEmpty(), message ?: "Expected string $actual not to be empty")
}

fun assertNotEmpty(actual: Collection<*>?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected collection not to be empty, but it's null.") }
    assertTrue(actual.isNotEmpty(), message ?: "Expected collection $actual not to be empty")
}

fun assertNotEmpty(actual: Map<*, *>?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected map not to be empty, but it's null.") }
    assertTrue(actual.isNotEmpty(), message ?: "Expected map $actual not to be empty")
}

fun <T> assertDoesNotContain(iterable: Iterable<T>?, element: T, message: String? = null) {
    if(iterable == null) { fail(message ?: "Expected collection not to contain $element, but it's null.") }
    iterable.firstOrNull { it == element }?.let {
        fail(message ?: "Expected $iterable not to contain $element")
    }
}

fun <T> assertDoesNotContainAllOf(iterable: Iterable<T>?, other: Iterable<T>, message: String? = null) {
    if(iterable == null) { fail(message ?: "Expected collection to contain $other, but it's null.") }
    other.forEach {
        assertDoesNotContain(iterable, it, message ?: "Expected $iterable not to contain $it")
    }
}

fun <T> assertContains(iterable: Iterable<T>?, element: T, message: String? = null) {
    if(iterable == null) { fail(message ?: "Expected collection to contain $element, but it's null.") }
    kotlin.test.assertContains(iterable, element, message ?: "Expected $iterable not to contain $element")
}

fun <T> assertContainsAllOf(iterable: Iterable<T>?, other: Iterable<T>, message: String? = null) {
    if(iterable == null) { fail(message ?: "Expected collection to contain $other, but it's null.") }
    other.forEach {
        kotlin.test.assertContains(iterable, it, message ?: "Expected $iterable not to contain $it")
    }
}

fun assertTrue(actual: Boolean?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected value to be true, but it's null") }
    assertTrue(actual, message)
}

fun assertFalse(actual: Boolean?, message: String? = null) {
    if(actual == null) { fail(message ?: "Expected value to be false, but it's null") }
    assertFalse(actual, message)
}
