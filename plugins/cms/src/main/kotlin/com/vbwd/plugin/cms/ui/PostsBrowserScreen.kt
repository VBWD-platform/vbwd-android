package com.vbwd.plugin.cms.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

private val PADDING = 16.dp

/**
 * Posts browser — a config-driven WebView over the host CMS embed archive
 * (S91: one web renderer ⇒ every post type works). Port of the iOS
 * `PostsBrowserView`. Fail-loud: a missing manifest shows a state, never a blank.
 */
@Composable
fun PostsBrowserScreen(viewModel: PostsBrowserViewModel, tokenProvider: () -> String?) {
    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) { viewModel.load() }

    when (val current = state) {
        is PostsBrowserViewModel.LoadState.Ready -> CmsWebView(current.url, tokenProvider)
        PostsBrowserViewModel.LoadState.NotConfigured -> Centered("Posts are not configured.")
        is PostsBrowserViewModel.LoadState.Error -> Centered(current.message)
        else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            CircularProgressIndicator()
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun CmsWebView(url: String, tokenProvider: () -> String?) {
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag("posts_browser_webview"),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, pageUrl: String?, favicon: android.graphics.Bitmap?) {
                        val script = PostsBrowserAuthBootstrap.javaScriptSource(tokenProvider(), userJson = null)
                        if (script.isNotEmpty()) view?.evaluateJavascript(script, null)
                    }
                }
                loadUrl(url)
            }
        },
    )
}

@Composable
private fun Centered(message: String) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(PADDING)) {
        Text(message, modifier = Modifier.testTag("posts_browser_message"))
    }
}
