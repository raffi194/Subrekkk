package com.example.subrek.features.subscription.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Query("SELECT * FROM subscriptions ORDER BY next_payment_date ASC")
    fun getAllSubscriptions(): Flow<List<SubscriptionEntity>>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    suspend fun getSubscriptionById(id: String): SubscriptionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: SubscriptionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscriptions(subscriptions: List<SubscriptionEntity>)

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscriptionById(id: String)

    // FUNGSI getDirtySubscriptions() TELAH DIHAPUS

    @Query("""
        SELECT COALESCE(AVG(
            CASE 
                WHEN billing_cycle = 'YEARLY' THEN price / 12.0
                ELSE price
            END
        ), 0.0) FROM subscriptions WHERE status = 'ACTIVE'
    """)
    fun getAverageMonthlyConsumption(): Flow<Double?>

    @Query("SELECT * FROM subscriptions WHERE status = 'ENDED'")
    fun getSubscriptionHistory(): Flow<List<SubscriptionEntity>>

    @Query("""
        SELECT * FROM subscriptions 
        WHERE status IN ('ACTIVE', 'TRIAL') 
        AND date(next_payment_date) = date(:targetDate)
    """)
    suspend fun getSubscriptionsExpiringOn(targetDate: String): List<SubscriptionEntity>

    @Query("UPDATE subscriptions SET status = 'NEEDS_REVIEW' WHERE id = :id")
    suspend fun markAsNeedsReview(id: String)

    // Ghost Detector: Ambil langganan yang unconfirmedCyclesCount >= 2
    @Query("SELECT * FROM subscriptions WHERE unconfirmed_cycles_count >= 2 AND status != 'NEEDS_REVIEW'")
    suspend fun getGhostSubscriptions(): List<SubscriptionEntity>

    // Statistik: Total pengeluaran bulanan (estimasi)
    @Query("SELECT SUM(price) FROM subscriptions WHERE status IN ('ACTIVE', 'TRIAL')")
    fun getTotalMonthlyExpense(): Flow<Double?>

    @Query("SELECT * FROM subscriptions WHERE id = :id LIMIT 1")
    fun getSubscriptionByIdFlow(id: String): Flow<SubscriptionEntity?>

    @Query("""
    UPDATE subscriptions 
    SET price = :price, 
        billing_cycle = :billingCycle, 
        start_date = :startDate,
        next_payment_date = :nextPaymentDate
    WHERE id = :id
""")
    suspend fun updateSubscriptionBilling(
        id: String,
        price: Double,
        billingCycle: String,
        startDate: String,
        nextPaymentDate: String  // ← parameter terpisah
    )
    @Query("UPDATE subscriptions SET status = 'ENDED' WHERE id = :id")
    suspend fun terminateSubscription(id: String)

    // --- Tambahan Step 5.4 ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: LocalCategoryEntity)

    @Query("SELECT * FROM local_categories")
    fun getCustomCategories(): Flow<List<LocalCategoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomApp(app: LocalAppEntity)

    @Query("SELECT * FROM local_apps")
    fun getCustomApps(): Flow<List<LocalAppEntity>>

    @Query("DELETE FROM local_apps WHERE id = :id")
    suspend fun deleteCustomApp(id: String)
}