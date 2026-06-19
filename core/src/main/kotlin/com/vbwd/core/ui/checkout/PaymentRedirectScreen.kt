package com.vbwd.core.ui.checkout

import android.annotation.SuppressLint
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.viewinterop.AndroidView

private const val CALLBACK_SCHEME = "vbwd"

/**
 * Hosts the external payment page in a [WebView]. Port of the iOS
 * `PaymentRedirectView` (`ASWebAuthenticationSession`): when the provider
 * redirects to the app callback scheme (`vbwd://…`), [onResult] is invoked with
 * the callback URL so the checkout view model can complete the flow.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun PaymentRedirectScreen(url: String, onResult: (String?) -> Unit) {
    AndroidView(
        modifier = Modifier.fillMaxSize().testTag("payment_redirect"),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(
                        view: WebView?,
                        request: WebResourceRequest?,
                    ): Boolean {
                        val target = request?.url ?: return false
                        if (target.scheme == CALLBACK_SCHEME) {
                            onResult(target.toString())
                            return true
                        }
                        return false
                    }
                }
                loadUrl(url)
            }
        },
    )
}
