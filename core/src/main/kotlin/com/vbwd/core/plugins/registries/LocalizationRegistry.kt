package com.vbwd.core.plugins.registries

/**
 * Merged plugin translations. Port of the iOS `LocalizationRegistry`: repeated
 * calls for the same locale merge (last write wins per key); [t] returns the key
 * itself when missing (i18n fallback).
 */
class LocalizationRegistry {
    private val byLocale = mutableMapOf<String, MutableMap<String, String>>()

    fun add(locale: String, messages: Map<String, String>) {
        byLocale.getOrPut(locale) { mutableMapOf() }.putAll(messages)
    }

    fun all(): Map<String, Map<String, String>> = byLocale.mapValues { it.value.toMap() }

    fun t(key: String, locale: String): String = byLocale[locale]?.get(key) ?: key
}
