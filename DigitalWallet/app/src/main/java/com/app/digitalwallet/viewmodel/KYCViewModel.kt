package com.app.digitalwallet.viewmodel

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.digitalwallet.api.dto.KYCResponse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import com.app.digitalwallet.data.KycRepository
import dagger.hilt.android.qualifiers.ApplicationContext

// A sealed class to represent all possible KYC states
sealed class KycUiState {
    object Idle : KycUiState()
    object Loading : KycUiState()
    data class Loaded(val data: KYCResponse) : KycUiState()
    data class Error(val message: String) : KycUiState()
}
@HiltViewModel
class KYCViewModel @Inject constructor(
    private val repository: KycRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // ── Form input state (for submission) ──
    var fullName by mutableStateOf("")
    var dob by mutableStateOf("")
    var address by mutableStateOf("")
    var idCardUri by mutableStateOf<Uri?>(null)
    var selfieUri by mutableStateOf<Uri?>(null)

    // ── One-time navigation event ──
    private val _verificationSuccess = MutableSharedFlow<Unit>()
    val verificationSuccess = _verificationSuccess.asSharedFlow()

    fun submitKYC() {
        // validation stays exactly the same
        if (fullName.isBlank()) { kycUiState = KycUiState.Error("Full name is required"); return }
        if (dob.isBlank()) { kycUiState = KycUiState.Error("Date of birth is required"); return }
        if (address.length < 10) { kycUiState = KycUiState.Error("Address must be at least 10 characters"); return }
        if (idCardUri == null) { kycUiState = KycUiState.Error("ID Card image is required"); return }
        if (selfieUri == null) { kycUiState = KycUiState.Error("Selfie image is required"); return }

        viewModelScope.launch {
            kycUiState = KycUiState.Loading  // ← replaces isLoading = true
            try {
                val idCardFile = uriToFile(context, idCardUri!!, "id_card.jpg")
                val selfieFile = uriToFile(context, selfieUri!!, "selfie.jpg")

                val idPart = MultipartBody.Part.createFormData(
                    "id_card_image", idCardFile.name,
                    idCardFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )
                val selfiePart = MultipartBody.Part.createFormData(
                    "selfie_image", selfieFile.name,
                    selfieFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                )

                val response = repository.submitKYC(fullName, dob, address, idPart, selfiePart)

                if (response.success) {
                    _verificationSuccess.emit(Unit)  // ← keep this for navigation
                    kycUiState = KycUiState.Idle     // ← reset after success
                } else {
                    kycUiState = KycUiState.Error(response.message ?: "Submission failed")
                }
            } catch (e: Exception) {
                kycUiState = KycUiState.Error(e.localizedMessage ?: "An error occurred")
            }
            // no finally needed — each branch sets its own state
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

    var kycUiState by mutableStateOf<KycUiState>(KycUiState.Idle)
        private set

    fun resetKycState() {
        kycUiState = KycUiState.Idle
    }

    fun getKYC() {
        viewModelScope.launch {
            kycUiState = KycUiState.Loading
            try {
                val response = repository.getKYC()
                if (response.success && response.data != null) {
                    kycUiState = KycUiState.Loaded(response.data) // ✅ DTO used directly
                } else {
                    kycUiState = KycUiState.Error(response.message ?: "Unknown error")
                }
            } catch (e: Exception) {
                kycUiState = KycUiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}
