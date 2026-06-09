package com.app.digitalwallet.ui.screens.kyc

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.domain.model.KycData
import com.app.digitalwallet.domain.usecase.kyc.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

sealed class KycUiState {
    object Idle : KycUiState()
    object Loading : KycUiState()
    data class Loaded(val data: KycData) : KycUiState()
    data class Error(val message: String) : KycUiState()
}

sealed class KycUiEvent {
    object VerificationSuccess : KycUiEvent()
    data class ShowError(val message: String) : KycUiEvent()
}

data class KycFormState(
    val fullName: String = "",
    val dob: String = "",
    val address: String = "",
    val idCardUri: Uri? = null,
    val selfieUri: Uri? = null
)

@HiltViewModel
class KYCViewModel @Inject constructor(
    private val submitKycUseCase: SubmitKycUseCase,
    private val getKycUseCase: GetKycUseCase,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _formState = MutableStateFlow(KycFormState())
    val formState: StateFlow<KycFormState> = _formState.asStateFlow()

    private val _kycUiState = MutableStateFlow<KycUiState>(KycUiState.Idle)
    val kycUiState: StateFlow<KycUiState> = _kycUiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<KycUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onFullNameChange(name: String) { _formState.update { it.copy(fullName = name) } }
    fun onDobChange(dob: String) { _formState.update { it.copy(dob = dob) } }
    fun onAddressChange(address: String) { _formState.update { it.copy(address = address) } }
    fun onIdCardUriChange(uri: Uri?) { _formState.update { it.copy(idCardUri = uri) } }
    fun onSelfieUriChange(uri: Uri?) { _formState.update { it.copy(selfieUri = uri) } }

    fun submitKYC() {
        val currentForm = _formState.value
        if (currentForm.fullName.isBlank()) { _kycUiState.value = KycUiState.Error("Full name is required"); return }
        if (currentForm.dob.isBlank()) { _kycUiState.value = KycUiState.Error("Date of birth is required"); return }
        if (currentForm.address.length < 10) { _kycUiState.value = KycUiState.Error("Address must be at least 10 characters"); return }
        if (currentForm.idCardUri == null) { _kycUiState.value = KycUiState.Error("ID Card image is required"); return }
        if (currentForm.selfieUri == null) { _kycUiState.value = KycUiState.Error("Selfie image is required"); return }

        viewModelScope.launch {
            _kycUiState.value = KycUiState.Loading
            try {
                val idCardFile = uriToFile(context, currentForm.idCardUri, "id_card.jpg")
                val selfieFile = uriToFile(context, currentForm.selfieUri, "selfie.jpg")

                val idPart = MultipartBody.Part.createFormData(
                    "id_card_image", idCardFile.name,
                    idCardFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val selfiePart = MultipartBody.Part.createFormData(
                    "selfie_image", selfieFile.name,
                    selfieFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )

                val result = submitKycUseCase(
                    currentForm.fullName, 
                    currentForm.dob, 
                    currentForm.address, 
                    idPart, 
                    selfiePart
                )

                result.onSuccess {
                    _uiEvent.emit(KycUiEvent.VerificationSuccess)
                    _kycUiState.value = KycUiState.Idle
                }.onFailure { error ->
                    _kycUiState.value = KycUiState.Error(error.message ?: "Submission failed")
                }
            } catch (e: Exception) {
                _kycUiState.value = KycUiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }

    private fun uriToFile(context: Context, uri: Uri, fileName: String): File {
        val file = File(context.cacheDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(file).use { output ->
                input.copyTo(output)
            }
        }
        return file
    }

    fun resetKycState() {
        _kycUiState.value = KycUiState.Idle
    }

    fun getKYC() {
        viewModelScope.launch {
            _kycUiState.value = KycUiState.Loading
            getKycUseCase()
                .onSuccess { data ->
                    _kycUiState.value = KycUiState.Loaded(data)
                }
                .onFailure { error ->
                    _kycUiState.value = KycUiState.Error(error.message ?: "Unknown error")
                }
        }
    }
}
