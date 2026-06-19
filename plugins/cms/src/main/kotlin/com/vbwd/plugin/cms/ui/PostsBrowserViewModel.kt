package com.vbwd.plugin.cms.ui

import com.vbwd.core.networking.ApiError
import com.vbwd.plugin.cms.domain.CmsService
import com.vbwd.plugin.cms.domain.CmsServiceError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Owns the manifest fetch + the URL the WebView loads. One [LoadState] drives
 * the screen. Port of the iOS `PostsBrowserViewModel`.
 */
class PostsBrowserViewModel(
    private val type: String?,
    private val category: String?,
    private val archiveUrlFallback: String?,
    private val webOrigin: String?,
    private val service: CmsService,
) {
    sealed interface LoadState {
        data object Idle : LoadState
        data object Validating : LoadState
        data class Ready(val url: String) : LoadState
        data object NotConfigured : LoadState
        data class Error(val message: String) : LoadState
    }

    private val _state = MutableStateFlow<LoadState>(LoadState.Idle)
    val state: StateFlow<LoadState> = _state.asStateFlow()

    suspend fun load() {
        if (type.isNullOrEmpty() || category.isNullOrEmpty()) {
            _state.value = LoadState.NotConfigured
            return
        }
        _state.value = LoadState.Validating
        try {
            val manifest = service.fetchManifest(type, category)
            if (!manifest.ok) {
                _state.value = LoadState.Error("Posts not available for this configuration.")
                return
            }
            val resolved = resolve(manifest.archiveUrl, webOrigin)
            _state.value = when {
                resolved != null -> LoadState.Ready(resolved)
                archiveUrlFallback != null -> LoadState.Ready(archiveUrlFallback)
                else -> LoadState.Error("Server returned no archive URL.")
            }
        } catch (notFound: CmsServiceError.NotFound) {
            // 404 ⇒ the dedicated "not configured" screen (reason is informational).
            _state.value = LoadState.NotConfigured
        } catch (error: ApiError) {
            _state.value = LoadState.Error(error.message)
        }
    }

    fun retry() {
        _state.value = LoadState.Idle
    }

    companion object {
        /** Resolve `archive_url`: absolute → verbatim; relative → against [base]. */
        fun resolve(raw: String?, base: String?): String? {
            if (raw.isNullOrEmpty()) return null
            if (raw.startsWith("http://") || raw.startsWith("https://")) return raw
            val origin = base?.takeIf { it.isNotEmpty() } ?: return null
            return origin.trimEnd('/') + "/" + raw.trimStart('/')
        }
    }
}
