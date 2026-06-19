package com.vbwd.core.ui.login

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.vbwd.core.domain.AuthService
import com.vbwd.core.domain.AuthUser
import com.vbwd.core.domain.Credentials
import com.vbwd.core.networking.ApiError
import com.vbwd.core.session.AuthSession
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI test for [LoginScreen]. Instrumented (not Robolectric) for a real
 * composition host — runs under `connectedCheck`, mirroring the iOS
 * `LoginView` UI assertions against the same test-tag anchors.
 */
@RunWith(AndroidJUnit4::class)
class LoginScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val user = AuthUser(id = "u1", email = "test@example.com")

    private class FakeAuthService(private val error: ApiError? = null) : AuthService {
        override suspend fun login(credentials: Credentials): AuthUser {
            error?.let { throw it }
            return AuthUser(id = "u1", email = credentials.email)
        }

        override suspend fun logout() = Unit
        override fun restore(): AuthUser? = null
        override suspend fun fetchProfile(): AuthUser = AuthUser(id = "u1", email = "x")
        override suspend fun refreshAccessToken(): String = throw ApiError.NotImplemented("nope")
        override fun currentToken(): String? = null
    }

    private fun setContent(service: AuthService = FakeAuthService()) {
        composeRule.setContent {
            MaterialTheme {
                LoginScreen(viewModel = LoginViewModel(AuthSession(service)))
            }
        }
    }

    @Test
    fun fieldsRenderAndSubmitIsDisabledWhenEmpty() {
        setContent()
        composeRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).assertIsDisplayed()
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).assertIsNotEnabled()
    }

    @Test
    fun submitEnablesOnceBothFieldsAreFilled() {
        setContent()
        composeRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("test@example.com")
        composeRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performTextInput("pw")
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).assertIsEnabled()
    }

    @Test
    fun errorLabelShowsWhenLoginFails() {
        setContent(FakeAuthService(error = ApiError.Http(401, "Invalid credentials")))
        composeRule.onNodeWithTag(LoginTestTags.EMAIL_FIELD).performTextInput("test@example.com")
        composeRule.onNodeWithTag(LoginTestTags.PASSWORD_FIELD).performTextInput("bad")
        composeRule.onNodeWithTag(LoginTestTags.SUBMIT_BUTTON).performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithTag(LoginTestTags.ERROR_LABEL).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag(LoginTestTags.ERROR_LABEL).assertIsDisplayed()
    }
}
