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
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class MonthlySpend(val monthName: String, val amount: Double)

data class DashboardUiState(
    val rawSubscriptions: List<Subscription> = emptyList(),
    val subscriptionsList: List<Subscription> = emptyList(),
    val subscriptionHistory: List<Subscription> = emptyList(),
    val statsState: UiState<DashboardStats> = UiState.Loading,
    val userName: String = "User",
    val userAvatarUrl: String? = null,
    val totalConsumptionThisMonth: Double = 0.0,
    val lifetimeSpending: Double = 0.0,
    val monthlyHistorySpending: List<MonthlySpend> = emptyList(),
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

    private fun getPaymentDatesInMonth(sub: Subscription, targetMonthDate: LocalDate): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        val start = sub.startDate
        val targetMonth = targetMonthDate.month
        val targetYear = targetMonthDate.year

        if (targetMonthDate.withDayOfMonth(targetMonthDate.lengthOfMonth()).isBefore(start)) {
            return emptyList()
        }

        when (sub.billingCycle.name) {
            "WEEKLY" -> {
                var current = start
                while (current.year <= targetYear) {
                    if (current.year == targetYear && current.month == targetMonth) {
                        dates.add(current)
                    }
                    current = current.plusWeeks(1)
                }
            }
            "MONTHLY" -> {
                var current = start
                while (current.year <= targetYear) {
                    if (current.year == targetYear && current.month == targetMonth) {
                        dates.add(current)
                    }
                    current = current.plusMonths(1)
                }
            }
            "YEARLY" -> {
                var current = start
                while (current.year <= targetYear) {
                    if (current.year == targetYear && current.month == targetMonth) {
                        dates.add(current)
                    }
                    current = current.plusYears(1)
                }
            }
        }
        return dates
    }

    private fun calculateTotalSpendForMonth(
        activeSubs: List<Subscription>,
        endedSubs: List<Subscription>,
        targetMonthDate: LocalDate
    ): Double {
        var total = 0.0

        fun getSpendingForSub(sub: Subscription): Double {
            val targetMonthStart = targetMonthDate.withDayOfMonth(1)
            val createdMonthStart = sub.createdAt.withDayOfMonth(1)
            if (targetMonthStart.isBefore(createdMonthStart)) {
                return 0.0
            }

            val paymentDates = getPaymentDatesInMonth(sub, targetMonthDate)
            var subTotal = 0.0
            for (date in paymentDates) {
                val isWithinActivePeriod = if (sub.status.name == "ENDED") {
                    !date.isAfter(sub.nextPaymentDate)
                } else {
                    true
                }

                if (isWithinActivePeriod) {
                    val dateStr = date.toString()
                    val isConfirmed = sub.confirmedPaymentDates.split(",").contains(dateStr)

                    if (isConfirmed) {
                        subTotal += when (sub.billingCycle.name) {
                            "YEARLY" -> sub.price / 12.0
                            else -> sub.price
                        }
                    }
                }
            }
            return subTotal
        }

        for (sub in activeSubs) {
            total += getSpendingForSub(sub)
        }
        for (sub in endedSubs) {
            total += getSpendingForSub(sub)
        }

        return total
    }

    private fun calculatePastMonthsSpending(
        activeSubs: List<Subscription>,
        endedSubs: List<Subscription>
    ): List<MonthlySpend> {
        val allSubs = activeSubs + endedSubs
        if (allSubs.isEmpty()) return emptyList()

        val inputMonths = allSubs.map { it.createdAt.withDayOfMonth(1) }.toSet()
        val sortedMonths = inputMonths.sorted()

        val monthNameFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))
        val result = mutableListOf<MonthlySpend>()

        for (monthDate in sortedMonths) {
            val totalForMonth = calculateTotalSpendForMonth(activeSubs, endedSubs, monthDate)
            if (totalForMonth > 0.0) {
                result.add(MonthlySpend(monthDate.format(monthNameFormatter), totalForMonth))
            }
        }

        return result
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
                    
                    val endedSubs = try {
                        repository.getSubscriptionHistory().first()
                    } catch (e: Exception) {
                        emptyList()
                    }

                    val activeSubs = subs.filter { it.status.name == "ACTIVE" || it.status.name == "TRIAL" }
                    val totalThisMonth = calculateTotalSpendForMonth(activeSubs, endedSubs, LocalDate.now())

                    val monthlyHistory = calculatePastMonthsSpending(activeSubs, endedSubs)
                    val lifetimeSpend = monthlyHistory.sumOf { it.amount }

                    _uiState.update { it.copy(
                        rawSubscriptions = subs,
                        statsState = UiState.Success(stats),
                        totalConsumptionThisMonth = totalThisMonth,
                        lifetimeSpending = lifetimeSpend,
                        monthlyHistorySpending = monthlyHistory,
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
                    
                    val currentSubs = _uiState.value.rawSubscriptions
                    val activeSubs = currentSubs.filter { it.status.name == "ACTIVE" || it.status.name == "TRIAL" }
                    val monthlyHistory = calculatePastMonthsSpending(activeSubs, history)
                    val lifetimeSpend = monthlyHistory.sumOf { it.amount }
                    
                    _uiState.update { it.copy(
                        lifetimeSpending = lifetimeSpend,
                        monthlyHistorySpending = monthlyHistory
                    ) }
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

    fun markAsPaid(subscription: Subscription) {
        viewModelScope.launch {
            val unconfirmed = subscription.getUnconfirmedPaymentDates()
            val dateToConfirm = unconfirmed.firstOrNull() ?: subscription.nextPaymentDate
            val updatedSub = subscription.confirmPaymentDate(dateToConfirm)
            repository.insertSubscription(updatedSub)
        }
    }

    fun skipAndConfirmCurrent(subscription: Subscription) {
        viewModelScope.launch {
            val updatedSub = subscription.skipOverdueAndConfirmCurrent()
            repository.insertSubscription(updatedSub)
        }
    }
}
