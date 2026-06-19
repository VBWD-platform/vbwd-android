package com.vbwd.core.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.networking.ApiError
import com.vbwd.core.session.AuthSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A01.4 boot behaviour at the composable level: [RootView] restores the session
 * on first composition and routes to the right slot. Instrumented (real
 * composition host) — runs under `connectedCheck`.
 */
@RunWith(AndroidJUnit4::class)
class RootViewTest {
    @get:Rule
    val composeRule = createComposeRule()

    private class FakeAuthService(private val restored: AuthUser?) : AuthService {
        override suspend fun login(credentials: Credentials): AuthUser = restored!!
        override suspend fun logout() = Unit
        override fun restore(): AuthUser? = restored
        override suspend fun fetchProfile(): AuthUser = restored!!
        override suspend fun refreshAccessToken(): String = throw ApiError.NotImplemented("nope")
        override fun currentToken(): String? = null
    }

    private fun content(restored: AuthUser?) {
        composeRule.setContent {
            RootView(
                session = AuthSession(FakeAuthService(restored)),
                loginContent = { Text("login", modifier = Modifier.testTag("login_slot")) },
                authenticatedContent = { user ->
                    Text(user.email, modifier = Modifier.testTag("home_slot"))
                },
            )
        }
    }

    @Test
    fun coldStartWithNoSessionShowsLogin() {
        content(restored = null)
        composeRule.onNodeWithTag("login_slot").assertIsDisplayed()
    }

    @Test
    fun coldStartWithPersistedSessionShowsAuthenticatedRoot() {
        content(restored = AuthUser(id = "u1", email = "test@example.com"))
        composeRule.onNodeWithTag("home_slot").assertIsDisplayed()
    }
}
