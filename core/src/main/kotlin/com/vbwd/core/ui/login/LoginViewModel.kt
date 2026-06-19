package com.vbwd.core.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbwd.core.domain.Credentials
import com.vbwd.core.session.AuthSession
import com.vbwd.core.session.AuthState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login screen logic. Port of the iOS `LoginViewModel` / `Login.vue`. Holds all
 * decisions so the composable is thin and this is unit-testable without
 * rendering (SRP). Navigation is NOT here — the root view owns it (web router
 * parity): on success [AuthSession.state] flips to `Authenticated` and the
 * router routes away.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val session: AuthSession,
) : ViewModel() {

    data class UiState(
        val email: String = "",
        val password: String = "",
        val isLoading: Boolean = false,
        val errorMessage: String? = null,
    ) {
        val canSubmit: Boolean
            get() = email.isNotEmpty() && password.isNotEmpty() && !isLoading
    }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun onEmailChange(value: String) = _uiState.update { it.copy(email = value) }

    fun onPasswordChange(value: String) = _uiState.update { it.copy(password = value) }

    fun submit() {
        if (!_uiState.value.canSubmit) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            session.signIn(Credentials(_uiState.value.email, _uiState.value.password))
            val message = (session.state.value as? AuthState.Error)?.message
            _uiState.update { it.copy(isLoading = false, errorMessage = message) }
        }
    }
}
