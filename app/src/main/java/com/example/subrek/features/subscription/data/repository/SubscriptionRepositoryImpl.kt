package com.example.subrek.features.subscription.data.repository

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.subrek.features.subscription.data.local.LocalAppEntity
import com.example.subrek.features.subscription.data.local.SubscriptionDao
import com.example.subrek.features.subscription.data.mapper.toDomain
import com.example.subrek.features.subscription.data.mapper.toEntity
import com.example.subrek.features.subscription.domain.model.Subscription
import com.example.subrek.features.subscription.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscriptionRepositoryImpl @Inject constructor(
    private val subscriptionDao: SubscriptionDao,
    @ApplicationContext private val context: Context
) : SubscriptionRepository {

    override fun getAllSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getAllSubscriptions().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getSubscriptionById(id: String): Subscription? {
        return subscriptionDao.getSubscriptionById(id)?.toDomain()
    }

    override suspend fun insertSubscription(subscription: Subscription) {
        // Karena sudah offline-only, hapus argumen (isDirty = true) pada fungsi toEntity().
        // Pastikan Anda juga sudah menghapus parameter isDirty di dalam berkas SubscriptionMapper.kt
        subscriptionDao.insertSubscription(subscription.toEntity())
    }

    override suspend fun deleteSubscription(id: String) {
        subscriptionDao.deleteSubscriptionById(id)
    }

    // CATATAN: Jika interface SubscriptionRepository.kt masih mewajibkan fungsi 'syncWithRemote()',
    // hapus fungsi tersebut dari interface. Atau Anda bisa membiarkan implementasi kosong ini:
    override suspend fun syncWithRemote(): Result<Unit> {
        return Result.success(Unit) // Tidak melakukan apapun di mode offline
    }

    override suspend fun getSubscriptionsExpiringInDays(days: Int): List<Subscription> {
        val targetLocalDate = LocalDate.now().plusDays(days.toLong())
        val formattedTarget = targetLocalDate.format(DateTimeFormatter.ISO_LOCAL_DATE)

        return subscriptionDao.getSubscriptionsExpiringOn(formattedTarget).map {
            it.toDomain()
        }
    }

    override suspend fun getGhostSubscriptions(): List<Subscription> {
        return subscriptionDao.getGhostSubscriptions().map { it.toDomain() }
    }

    override fun getTotalMonthlyExpense(): Flow<Double> {
        return subscriptionDao.getTotalMonthlyExpense().map { it ?: 0.0 }
    }

    override fun getActiveSubscriptions(): Flow<List<Subscription>> {
        return subscriptionDao.getAllSubscriptions().map { entities ->
            entities.filter { it.status == "ACTIVE" || it.status == "TRIAL" }.map { it.toDomain() }
        }
    }

    override fun getSubscriptionHistory(): Flow<List<Subscription>> {
        return subscriptionDao.getSubscriptionHistory().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAverageConsumption(): Flow<Double> {
        return subscriptionDao.getAverageMonthlyConsumption()
            .map { it ?: 0.0 }
            .catch { emit(0.0) }
    }

    override fun getActiveSubscriptionsCount(): Flow<Int> {
        return subscriptionDao.getAllSubscriptions().map { entities ->
            entities.count { it.status == "ACTIVE" || it.status == "TRIAL" }
        }.catch { emit(0) }
    }

    override suspend fun deleteSubscriptionFromLocalAndRemote(id: String) {
        // Meskipun nama fungsinya masih mengandung "AndRemote" (untuk menyesuaikan Interface),
        // fungsinya kini hanya menghapus dari Room lokal.
        subscriptionDao.deleteSubscriptionById(id)
    }

    override fun getSubscriptionByIdFlow(id: String): Flow<Subscription?> {
        return subscriptionDao.getSubscriptionByIdFlow(id).map { it?.toDomain() }
    }

    override suspend fun updateSubscriptionBilling(
        id: String,
        price: Double,
        billingCycle: String,
        startDate: String,
        paymentMethod: String,
        isTrial: Boolean,
        status: String
    ) {
<<<<<<< HEAD
        val existing = subscriptionDao.getSubscriptionById(id)
        val calculatedNextPaymentDate = if (existing != null && (existing.price == 0.0 || existing.paymentMethod == "Belum Diatur")) {
            startDate
        } else {
            existing?.nextPaymentDate ?: startDate
        }
        
=======
        val parsedStartDate = try {
            LocalDate.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }

        // Hitung nextPaymentDate yang benar dari startDate + siklus
        val today = LocalDate.now()
        val nextPaymentDate = calculateNextPaymentDate(parsedStartDate, billingCycle, today)

        android.util.Log.d("UPDATE_BILLING", "id=$id price=$price cycle=$billingCycle nextPayment=$nextPaymentDate")

>>>>>>> 312a66543b8ece88c234f0cd48beabdb1c08e53c
        subscriptionDao.updateSubscriptionBilling(
            id = id,
            price = price,
            billingCycle = billingCycle,
            startDate = startDate,
<<<<<<< HEAD
            nextPaymentDate = calculatedNextPaymentDate,
            paymentMethod = paymentMethod,
            isTrial = isTrial,
            status = status
        )
=======
            nextPaymentDate = nextPaymentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
    }

    private fun calculateNextPaymentDate(
        startDate: LocalDate,
        billingCycle: String,
        today: LocalDate
    ): LocalDate {
        var next = startDate
        while (next.isBefore(today)) {
            next = when (billingCycle.uppercase()) {
                "WEEKLY"  -> next.plusWeeks(1)
                "MONTHLY" -> next.plusMonths(1)
                "YEARLY"  -> next.plusYears(1)
                else      -> next.plusMonths(1)
            }
        }
        return next
>>>>>>> 312a66543b8ece88c234f0cd48beabdb1c08e53c
    }

    override suspend fun terminateSubscription(id: String) {
        // Logika update status ENDED ke Supabase dihapus total
        subscriptionDao.terminateSubscription(id)
    }

    override suspend fun insertCustomApp(app: LocalAppEntity) {
        // Logika upsert ke Supabase dihapus total
        subscriptionDao.insertCustomApp(app)
    }

    override fun getCustomApps(): Flow<List<LocalAppEntity>> {
        return subscriptionDao.getCustomApps()
    }

    override suspend fun deleteCustomApp(id: String) {
        subscriptionDao.deleteCustomApp(id)
    }

    override suspend fun saveSubscription(
        name: String,
        iconUrl: String?,
        price: Double,
        cycle: String,
        date: String,
        isTrial: Boolean
    ) {
        val id = java.util.UUID.randomUUID().toString()
        saveSubscriptionExtended(
            id = id,
            name = name,
            price = price,
            currency = "IDR",
            billingCycle = cycle,
            paymentMethod = "Manual",
            nextPaymentDate = date,
            status = if (isTrial) "TRIAL" else "ACTIVE",
            iconUrl = iconUrl
        )
    }

    override suspend fun saveSubscriptionExtended(
        id: String,
        name: String,
        price: Double,
        currency: String,
        billingCycle: String,
        paymentMethod: String,
        nextPaymentDate: String,
        status: String,
        iconUrl: String?
    ) {
        // Variabel autentikasi session Supabase dihapus total

        // 🛠️ SAFE PARSING TANGGAL
        val parsedDate = try {
            LocalDate.parse(nextPaymentDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            LocalDate.now()
        }

        // 🛠️ SAFE ENUM CONVERSION
        val domainCycle = try {
            com.example.subrek.features.subscription.domain.model.BillingCycle.valueOf(billingCycle.uppercase())
        } catch (e: Exception) {
            com.example.subrek.features.subscription.domain.model.BillingCycle.MONTHLY
        }

        val domainStatus = try {
            com.example.subrek.features.subscription.domain.model.SubscriptionStatus.valueOf(status.uppercase())
        } catch (e: Exception) {
            com.example.subrek.features.subscription.domain.model.SubscriptionStatus.ACTIVE
        }

        val subscription = com.example.subrek.features.subscription.domain.model.Subscription(
            id = id,
            name = name,
            price = price,
            currency = currency.ifBlank { "IDR" },
            billingCycle = domainCycle,
            startDate = parsedDate,
            nextPaymentDate = parsedDate,
            paymentMethod = paymentMethod.ifBlank { "E-Wallet" },
            isTrial = status.uppercase() == "TRIAL",
            status = domainStatus,
            createdAt = LocalDate.now(),
            updatedAt = LocalDate.now(),
            iconUrl = iconUrl
        )

        // Menyimpan data murni ke Database Lokal
        insertSubscription(subscription)
    }

    override suspend fun uploadAppIconStorage(uri: android.net.Uri): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = "app_icon_${System.currentTimeMillis()}.png"
            val file = java.io.File(context.filesDir, fileName)
            val outputStream = java.io.FileOutputStream(file)
            inputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}