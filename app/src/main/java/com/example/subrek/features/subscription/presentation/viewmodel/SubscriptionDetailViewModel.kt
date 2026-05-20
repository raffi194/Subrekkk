package com.example.subrek.features.subscription.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subrek.features.subscription.domain.model.Subscription
import com.example.subrek.features.subscription.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.time.format.DateTimeFormatter


data class DetailUiState(
    val subscription: Subscription? = null,
    val isLoading: Boolean = false,
    val isUpdateSuccess: Boolean = false,
    val isTerminationSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class SubscriptionDetailViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Mengambil ID parameter dari argumen navigasi Compose
    private val subscriptionId: String = checkNotNull(savedStateHandle["subscriptionId"])

    var priceInput by mutableStateOf("")
    var selectedCycle by mutableStateOf("MONTHLY")
    var startDateInput by mutableStateOf("")
    private val _uiState = MutableStateFlow(DetailUiState())
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    init {
        loadSubscriptionDetail()
    }

    private fun loadSubscriptionDetail() {
        viewModelScope.launch {
            repository.getSubscriptionByIdFlow(subscriptionId)
                .collect { sub ->
                    sub?.let {  // ← tambahkan null check
                        priceInput = it.price.toInt().toString()
                        selectedCycle = it.billingCycle.name
                        startDateInput = it.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

                        _uiState.update { state -> state.copy(subscription = it, isLoading = false) }
                    }
                }
        }
    }

    fun updateSubscriptionBilling() {
        viewModelScope.launch {
            try {
                // Parsing harga
                val price = priceInput.replace(".", "").replace(",", "").toDoubleOrNull() ?: 0.0

                // Panggil repository
                repository.updateSubscriptionBilling(subscriptionId, price, selectedCycle, startDateInput)

                _uiState.update { it.copy(isUpdateSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun terminateSubscriptionService() {
        viewModelScope.launch {
            try {
                repository.terminateSubscription(subscriptionId)
                _uiState.update { it.copy(isTerminationSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }
}
