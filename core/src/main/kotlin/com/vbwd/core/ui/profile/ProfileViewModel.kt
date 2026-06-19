package com.vbwd.core.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vbwd.core.domain.ProfileService
import com.vbwd.core.domain.UserProfile
import com.vbwd.core.networking.ApiError
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Editable personal-info + address fields. Port of the iOS `ProfileFormData`. */
data class ProfileFormData(
    val firstName: String = "",
    val lastName: String = "",
    val company: String = "",
    val taxNumber: String = "",
    val phone: String = "",
    val addressLine1: String = "",
    val addressLine2: String = "",
    val city: String = "",
    val postalCode: String = "",
    val country: String = "",
) {
    fun toUserProfile() = UserProfile(
        firstName = firstName,
        lastName = lastName,
        company = company,
        taxNumber = taxNumber,
        phone = phone,
        addressLine1 = addressLine1,
        addressLine2 = addressLine2,
        city = city,
        postalCode = postalCode,
        country = country,
    )

    companion object {
        fun from(profile: UserProfile) = ProfileFormData(
            firstName = profile.firstName,
            lastName = profile.lastName,
            company = profile.company,
            taxNumber = profile.taxNumber,
            phone = profile.phone,
            addressLine1 = profile.addressLine1,
            addressLine2 = profile.addressLine2,
            city = profile.city,
            postalCode = profile.postalCode,
            country = profile.country,
        )
    }
}

/** Password-change form. Port of the iOS `PasswordFormData`. */
data class PasswordFormData(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
)

/**
 * Drives the profile edit form. Port of the iOS `ProfileViewModel`: owns
 * load/save/change-password + validation; the screen is thin (SRP). Save is
 * gated on [UiState.isDirty]; errors surface from [ApiError].
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val service: ProfileService,
) : ViewModel() {

    data class UiState(
        val isLoading: Boolean = false,
        val isSaving: Boolean = false,
        val isDirty: Boolean = false,
        val errorMessage: String? = null,
        val successMessage: String? = null,
        val passwordError: String? = null,
        val passwordSuccess: String? = null,
    )

    private var loadedForm = ProfileFormData()

    private val _form = MutableStateFlow(ProfileFormData())
    val form: StateFlow<ProfileFormData> = _form.asStateFlow()

    private val _password = MutableStateFlow(PasswordFormData())
    val password: StateFlow<PasswordFormData> = _password.asStateFlow()

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    fun updateForm(transform: (ProfileFormData) -> ProfileFormData) {
        _form.value = transform(_form.value)
        _uiState.value = _uiState.value.copy(isDirty = _form.value != loadedForm)
    }

    fun updatePassword(transform: (PasswordFormData) -> PasswordFormData) {
        _password.value = transform(_password.value)
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            try {
                val profile = service.fetchProfile()
                loadedForm = ProfileFormData.from(profile)
                _form.value = loadedForm
                _uiState.value = _uiState.value.copy(isLoading = false, isDirty = false)
            } catch (error: ApiError) {
                _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = error.message)
            }
        }
    }

    fun save() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, errorMessage = null, successMessage = null)
            try {
                val updated = service.updateDetails(_form.value.toUserProfile())
                loadedForm = ProfileFormData.from(updated)
                _form.value = loadedForm
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    isDirty = false,
                    successMessage = "Profile updated successfully",
                )
            } catch (error: ApiError) {
                _uiState.value = _uiState.value.copy(isSaving = false, errorMessage = error.message)
            }
        }
    }

    fun changePassword() {
        val data = _password.value
        val validationError = validatePassword(data)
        if (validationError != null) {
            _uiState.value = _uiState.value.copy(passwordError = validationError, passwordSuccess = null)
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(passwordError = null, passwordSuccess = null)
            try {
                service.changePassword(current = data.currentPassword, new = data.newPassword)
                _password.value = PasswordFormData()
                _uiState.value = _uiState.value.copy(passwordSuccess = "Password changed successfully")
            } catch (error: ApiError) {
                _uiState.value = _uiState.value.copy(passwordError = error.message)
            }
        }
    }

    private fun validatePassword(data: PasswordFormData): String? = when {
        data.currentPassword.isEmpty() || data.newPassword.isEmpty() || data.confirmPassword.isEmpty() ->
            "All password fields are required"
        data.newPassword != data.confirmPassword -> "New passwords do not match"
        data.newPassword.length < MIN_PASSWORD_LENGTH -> "New password must be at least 8 characters"
        else -> null
    }

    private companion object {
        const val MIN_PASSWORD_LENGTH = 8
    }
}
