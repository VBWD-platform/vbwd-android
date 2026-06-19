package com.vbwd.core.ui.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vbwd.core.plugins.ComponentFactory

private val SCREEN_PADDING = 16.dp
private val FIELD_SPACING = 12.dp

/**
 * Profile edit form. Port of the iOS `ProfileEditView`: renders the core detail
 * fields, then the plugin-contributed `Profile*` sections at the bottom (the
 * extension seam — passed in as [profileSections] so `:core` stays decoupled
 * from the host). Save is disabled while pristine.
 */
@Composable
fun ProfileEditScreen(
    viewModel: ProfileViewModel,
    profileSections: List<Pair<String, ComponentFactory>> = emptyList(),
) {
    val form by viewModel.form.collectAsState()
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(SCREEN_PADDING)
            .testTag("profile_edit_screen"),
        verticalArrangement = Arrangement.spacedBy(FIELD_SPACING),
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall)

        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.successMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        Field("First name", form.firstName) { value -> viewModel.updateForm { it.copy(firstName = value) } }
        Field("Last name", form.lastName) { value -> viewModel.updateForm { it.copy(lastName = value) } }
        Field("Company", form.company) { value -> viewModel.updateForm { it.copy(company = value) } }
        Field("Tax number", form.taxNumber) { value -> viewModel.updateForm { it.copy(taxNumber = value) } }
        Field("Phone", form.phone) { value -> viewModel.updateForm { it.copy(phone = value) } }
        Field("Address line 1", form.addressLine1) { value -> viewModel.updateForm { it.copy(addressLine1 = value) } }
        Field("Address line 2", form.addressLine2) { value -> viewModel.updateForm { it.copy(addressLine2 = value) } }
        Field("City", form.city) { value -> viewModel.updateForm { it.copy(city = value) } }
        Field("Postal code", form.postalCode) { value -> viewModel.updateForm { it.copy(postalCode = value) } }
        Field("Country", form.country) { value -> viewModel.updateForm { it.copy(country = value) } }

        Button(
            onClick = viewModel::save,
            enabled = state.isDirty && !state.isSaving,
            modifier = Modifier.fillMaxWidth().testTag("profile_save_button"),
        ) {
            Text(if (state.isSaving) "Saving…" else "Save")
        }

        profileSections.forEach { (_, section) -> section() }
    }
}

@Composable
private fun Field(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}
