package com.example.subrek.features.subscription.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subrek.features.subscription.data.local.LocalAppEntity
import com.example.subrek.features.subscription.domain.model.CatalogItem
import com.example.subrek.features.subscription.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CatalogUiState(
    val catalogItems: List<CatalogItem> = emptyList(),
    val searchQuery: String = "",
    val isSaveSuccess: Boolean = false
)

@HiltViewModel
class TambahLanggananViewModel @Inject constructor(
    private val repository: SubscriptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        observeCatalogApps()
    }

    private fun observeCatalogApps() {
        viewModelScope.launch {
            repository.getCustomApps()
                .map { localApps ->
                    localApps.map { app ->
                        val isUserCustom = !app.id.startsWith("app_")
                        CatalogItem(app.id, app.name, app.iconUrl, isCustom = isUserCustom)
                    }
                }
                .catch { e -> e.printStackTrace() }
                .collect { items ->
                    _uiState.update { it.copy(catalogItems = items) }
                }
        }
    }

    fun updateSearchQuery(query: String) { _uiState.update { it.copy(searchQuery = query) } }

    fun addCustomApp(name: String, iconUrl: String?) {
        viewModelScope.launch {
            repository.insertCustomApp(LocalAppEntity(UUID.randomUUID().toString(), name, iconUrl))
        }
    }

    fun saveNewSubscription(
        name: String,
        iconUrl: String?,
        price: Double,
        currency: String,
        cycle: String,
        paymentMethod: String,
        date: String,
        isTrial: Boolean
    ) {
        viewModelScope.launch {
            repository.saveSubscriptionExtended(
                id = UUID.randomUUID().toString(),
                name = name,
                price = price,
                currency = currency.ifBlank { "IDR" },
                billingCycle = cycle,
                paymentMethod = paymentMethod.ifBlank { "E-Wallet" },
                nextPaymentDate = date,
                status = if (isTrial) "TRIAL" else "ACTIVE",
                iconUrl = iconUrl
            )
            _uiState.update { it.copy(isSaveSuccess = true) }
        }
    }

    fun addCustomAppWithDetails(
        name: String,
        price: Double,
        currency: String,
        billingCycle: String,
        paymentMethod: String,
        nextPaymentDate: String
    ) {
        addCustomAppWithImage(name, price, currency, billingCycle, paymentMethod, nextPaymentDate, null)
    }

    fun deleteApp(item: CatalogItem) {
        viewModelScope.launch {
            repository.deleteCustomApp(item.id) // Kirim ID saja
        }
    }
    fun addCustomAppWithImage(
        name: String,
        price: Double,
        currency: String,
        billingCycle: String,
        paymentMethod: String,
        nextPaymentDate: String,
        imageUri: android.net.Uri?
    ) {
        viewModelScope.launch {
            var remoteIconUrl: String? = null

            if (imageUri != null) {
                try {
                    remoteIconUrl = repository.uploadAppIconStorage(imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            val generatedId = UUID.randomUUID().toString()

            repository.insertCustomApp(
                LocalAppEntity(
                    id = generatedId,
                    name = name,
                    iconUrl = remoteIconUrl
                )
            )

            repository.saveSubscriptionExtended(
                id = generatedId,
                name = name,
                price = price,
                currency = currency,
                billingCycle = billingCycle,
                paymentMethod = paymentMethod,
                nextPaymentDate = nextPaymentDate,
                status = "ACTIVE",
                iconUrl = remoteIconUrl
            )

            _uiState.update { it.copy(isSaveSuccess = true) }
        }
    }

    fun addOnlyCustomApp(name: String, imageUri: android.net.Uri?) {
        viewModelScope.launch {
            var remoteIconUrl: String? = null
            if (imageUri != null) {
                try {
                    remoteIconUrl = repository.uploadAppIconStorage(imageUri)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            val generatedId = UUID.randomUUID().toString()
            repository.insertCustomApp(
                LocalAppEntity(
                    id = generatedId,
                    name = name,
                    iconUrl = remoteIconUrl
                )
            )
        }
    }
    fun resetSaveSuccess() { _uiState.update { it.copy(isSaveSuccess = false) } }
}
