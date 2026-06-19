package com.vbwd.plugin.example.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Plugin-owned reactive state (the web Pinia store analogue / port of the iOS
 * `ExampleStore`). Single responsibility: holds the plugin's state only. It is
 * registered with the SDK via `createStore` and exposed as [StateFlow]s.
 */
class ExampleStore {
    private val _active = MutableStateFlow(false)
    val active: StateFlow<Boolean> = _active.asStateFlow()

    private val _sawLogin = MutableStateFlow(false)
    val sawLogin: StateFlow<Boolean> = _sawLogin.asStateFlow()

    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()

    fun incrementCounter() {
        _count.value += 1
    }

    fun markLoginSeen() {
        _sawLogin.value = true
    }

    fun setActive(value: Boolean) {
        _active.value = value
    }
}
