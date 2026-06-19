package com.vbwd.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Host application entry point.
 *
 * `@HiltAndroidApp` bootstraps the Hilt object graph — the composition root
 * (decision D3, the Android port of the iOS `SDKContainer`). Core singletons
 * (ApiClient, TokenStore, EventBus, PlatformSdk, PluginRegistry, …) are provided
 * by Hilt modules from A01.1 onward; dynamically-loaded plugins are bootstrapped
 * by `PluginHost` (A02), not by Hilt.
 */
@HiltAndroidApp
class VbwdApplication : Application()
