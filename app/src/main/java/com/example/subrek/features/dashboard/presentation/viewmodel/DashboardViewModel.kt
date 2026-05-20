package com.example.subrek.features.dashboard.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.subrek.core.utils.UiState
import com.example.subrek.features.dashboard.domain.model.DashboardStats
import com.example.subrek.features.dashboard.domain.usecase.GetDashboardStatsUseCase
import com.example.subrek.features.subscription.domain.model.Subscription
import com.example.subrek.features.subscription.domain.repository.SubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DashboardUiState(
    val rawSubscriptions: List<Subscription> = emptyList(),
    val subscriptionsList: List<Subscription> = emptyList(),
    val subscriptionHistory: List<Subscription> = emptyList(),
    val statsState: UiState<DashboardStats> = UiState.Loading,
    val userName: String = "User",
    val userAvatarUrl: String? = null,
    val totalConsumptionThisMonth: Double = 0.0,
    val activeAppsCount: Int = 0,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SubscriptionRepository,
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
        observeStats()
    }

    private fun observeStats() {
        repository.getActiveSubscriptionsCount()
            .onEach { count -> _uiState.update { it.copy(activeAppsCount = count) } }
            .launchIn(viewModelScope)
    }

    fun loadDashboardData() {
        viewModelScope.launch {
            repository.getAllSubscriptions()
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.localizedMessage) }
                }
                .collect { subs ->
                    val stats = getDashboardStatsUseCase(subs)

                    // Menghitung pengeluaran khusus bulan ini
                    val today = LocalDate.now()
                    val currentMonth = today.month
                    val currentYear = today.year
                    val nextMonth = today.plusMonths(1)
                    val activeSubs = subs.filter { it.status.name == "ACTIVE" || it.status.name == "TRIAL" }

                    val totalThisMonth = activeSubs.sumOf { sub ->
                        val nextDate = sub.nextPaymentDate
                        when (sub.billingCycle.name) {
                            "MONTHLY" -> {
                                val isCurrentMonth = nextDate.month == currentMonth && nextDate.year == currentYear
                                val isNextMonth = nextDate.month == nextMonth.month && nextDate.year == nextMonth.year
                                if (isCurrentMonth || isNextMonth) sub.price else 0.0
                            }
                            "WEEKLY" -> sub.price * 4.0
                            "YEARLY" -> {
                                val isCurrentMonth = nextDate.month == currentMonth && nextDate.year == currentYear
                                if (isCurrentMonth) sub.price else 0.0
                            }
                            else -> 0.0
                        }
                    }
                    subs.forEach { sub ->
                        android.util.Log.d("DASH_SUBS", "name=${sub.name} price=${sub.price} cycle=${sub.billingCycle} nextPayment=${sub.nextPaymentDate}")
                    }

                    _uiState.update { it.copy(
                        rawSubscriptions = subs,
                        statsState = UiState.Success(stats),
                        totalConsumptionThisMonth = totalThisMonth,
                        isLoading = false
                    ) }
                    applyFilter()
                }
        }
        viewModelScope.launch {
            repository.getSubscriptionHistory()
                .catch { /* silent fail */ }
                .collect { history ->
                    _uiState.update { it.copy(subscriptionHistory = history) }
                }
        }
    }

    private fun applyFilter() {
        val current = _uiState.value
        _uiState.update { it.copy(subscriptionsList = current.rawSubscriptions) }
    }

    fun deleteSubscription(subscriptionId: String) {
        viewModelScope.launch {
            repository.deleteSubscription(subscriptionId)
        }
    }



    // 👈 FITUR BARU: Menandai subscription telah dibayar dan memajukan tanggal siklus berikutnya
    fun markAsPaid(subscription: Subscription) {
        viewModelScope.launch {
            val updatedSub = subscription.copy(
                nextPaymentDate = when (subscription.billingCycle.name) {
                    "WEEKLY" -> subscription.nextPaymentDate.plusWeeks(1)
                    "MONTHLY" -> subscription.nextPaymentDate.plusMonths(1)
                    "YEARLY" -> subscription.nextPaymentDate.plusYears(1)
                    else -> subscription.nextPaymentDate.plusMonths(1)
                },
                updatedAt = LocalDate.now()
            )
            repository.insertSubscription(updatedSub)
        }
    }
}