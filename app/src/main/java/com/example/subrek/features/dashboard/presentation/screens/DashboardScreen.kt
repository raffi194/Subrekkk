package com.example.subrek.features.dashboard.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.subrek.core.theme.*
import com.example.subrek.features.dashboard.presentation.viewmodel.DashboardViewModel
import com.example.subrek.features.profile.presentation.viewmodel.ProfileViewModel
import com.example.subrek.features.subscription.domain.model.Subscription
import com.example.subrek.features.subscription.domain.model.SubscriptionStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.roundToInt
import com.example.subrek.core.background.NotificationWorker // Pastikan import class ini
import androidx.compose.ui.platform.LocalContext
import androidx.work.WorkManager
import androidx.work.OneTimeWorkRequestBuilder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    profileViewModel: ProfileViewModel,
    onNavigateToAddSubscription: () -> Unit,
    onNavigateToDetail: (String) -> Unit = {}
) {
    val state by viewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current // Tambahkan ini

    LaunchedEffect(Unit) {
        viewModel.loadDashboardData() // Panggil fungsi untuk mengambil data terbaru
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    // Diberi batasan ukuran (Box & padding) agar tidak menutupi tombol Plus
                    Box(modifier = Modifier.padding(end = 16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (!profileState.avatarUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = profileState.avatarUrl,
                                    contentDescription = "Foto Profil",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFF1F5F9))
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .background(Blue600, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = (profileState.fullName?.takeIf { it.isNotEmpty() } ?: "U").take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Selamat datang,",
                                    fontSize = 11.sp,
                                    color = Slate500,
                                    fontWeight = FontWeight.Normal
                                )
                                Text(
                                    text = when {
                                        profileState.isLoading -> "Memuat..."
                                        !profileState.fullName.isNullOrBlank() -> profileState.fullName!!
                                        else -> "Pengguna"
                                    },
                                    fontSize = 14.sp,
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Tombol Notifikasi
                    IconButton(
                        onClick = { /* TODO: navigate ke log notifikasi */ },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Log Notifikasi",
                            tint = Color.Black
                        )
                    }
                    Button(
                        onClick = {
                            val workRequest = OneTimeWorkRequestBuilder<NotificationWorker>().build()
                            WorkManager.getInstance(context).enqueue(workRequest)
                        },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text("Test Notifikasi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {

            // 1. KARTU ESTIMASI RINGKASAN BULANAN
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(
                            "Total Konsumsi Bulan Ini",
                            color = Slate500,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val formattedPrice = if (state.totalConsumptionThisMonth > 0) {
                                NumberFormat
                                    .getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                                        maximumFractionDigits = 0
                                    }
                                    .format(state.totalConsumptionThisMonth)
                            } else {
                                "Rp 0"
                            }
                            Text(
                                formattedPrice,
                                color = Color.Black,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Aplikasi Aktif: ${state.activeAppsCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (state.activeAppsCount > 0) Blue600 else Slate500,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 2. DAFTAR LANGGANAN AKTIF DAN FREE TRIAL
            val activeSubscriptions = state.subscriptionsList.filter { it.status == SubscriptionStatus.ACTIVE }
            val freeTrialSubscriptions = state.subscriptionsList.filter { it.status == SubscriptionStatus.TRIAL }

            item {
                Text(
                    text = "Active Subscriptions",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (activeSubscriptions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada langganan aktif ditemukan",
                            color = Slate500,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(activeSubscriptions, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        SwipeToDeleteSubscriptionItem(
                            subscription = item,
                            onDelete = { viewModel.deleteSubscription(item.id) },
                            onClick = { onNavigateToDetail(item.id) },
                            onMarkPaid = { viewModel.markAsPaid(item) },
                            showTrialTag = true
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Free Trial",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
            }

            if (freeTrialSubscriptions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Tidak ada uji coba gratis ditemukan",
                            color = Slate500,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                items(freeTrialSubscriptions, key = { it.id }) { item ->
                    Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)) {
                        SwipeToDeleteSubscriptionItem(
                            subscription = item,
                            onDelete = { viewModel.deleteSubscription(item.id) },
                            onClick = { onNavigateToDetail(item.id) },
                            onMarkPaid = { viewModel.markAsPaid(item) },
                            showTrialTag = false
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Composable SwipeToDelete
// =============================================================================
@Composable
fun SwipeToDeleteSubscriptionItem(
    subscription: Subscription,
    onDelete: () -> Unit,
    onClick: () -> Unit,
    onMarkPaid: () -> Unit,
    showTrialTag: Boolean = true
) {
    var offsetX by remember { mutableStateOf(0f) }
    val revealThreshold = -120.dp.value
    val dismissThreshold = -300.dp.value
    var showConfirmDialog by remember { mutableStateOf(false) }

    val animatedOffset by animateFloatAsState(
        targetValue = offsetX,
        label = "swipe_offset"
    )

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = {
                showConfirmDialog = false
                offsetX = 0f
            },
            title = { Text("Hapus Langganan?") },
            text = {
                Text("Langganan \"${subscription.name}\" akan dihapus secara permanen dari lokal.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirmDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Rose500)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showConfirmDialog = false
                    offsetX = 0f
                }) {
                    Text("Batal")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Rose500, shape = RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Hapus",
                tint = Color.White,
                modifier = Modifier.padding(end = 24.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        val newOffset = (offsetX + delta).coerceIn(-320.dp.value, 0f)
                        offsetX = newOffset
                    },
                    onDragStopped = {
                        when {
                            offsetX < dismissThreshold -> {
                                showConfirmDialog = true
                            }
                            offsetX < revealThreshold -> {
                                offsetX = revealThreshold
                            }
                            else -> {
                                offsetX = 0f
                            }
                        }
                    }
                )
        ) {
            SubscriptionItemRow(
                sub = subscription,
                onClick = onClick,
                onMarkPaid = onMarkPaid,
                showTrialTag = showTrialTag
            )
        }
    }
}

// =============================================================================
// SubscriptionItemRow
// =============================================================================
@Composable
fun SubscriptionItemRow(
    sub: Subscription,
    onClick: () -> Unit = {},
    onMarkPaid: () -> Unit = {},
    showTrialTag: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF8FAFC), shape = RoundedCornerShape(16.dp))
            // Clickable dihapus dari Row agar geser (swipe) tidak macet
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        // horizontalArrangement = Arrangement.SpaceBetween // Dihapus karena kita menggunakan weight dan letak elemen berurutan
    ) {

        // --- TAMBAHAN KODE UNTUK GAMBAR / PLACEHOLDER MULAI DI SINI ---
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE2E8F0)), // Warna latar belakang placeholder
            contentAlignment = Alignment.Center
        ) {
            // Jika model 'Subscription' Anda memiliki URL gambar (misal: sub.logoUrl),
            // Anda bisa menggunakan AsyncImage dari Coil. Silakan uncomment dan sesuaikan kodenya:

            /*
            if (!sub.logoUrl.isNullOrEmpty()) {
                AsyncImage(
                    model = sub.logoUrl,
                    contentDescription = "Logo ${sub.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
            */

            // Placeholder: Menampilkan huruf pertama dari nama langganan jika gambar tidak ada
            Text(
                text = sub.name.take(1).uppercase(),
                color = Slate500,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            /* } */ // Tutup kurawal else jika Anda mengaktifkan AsyncImage di atas
        }

        Spacer(modifier = Modifier.width(12.dp))
        // --- TAMBAHAN KODE SELESAI ---

        Column(modifier = Modifier.weight(0.6f)) {
            Text(
                sub.name,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color.Black,
                // Clickable dipindah ke teks nama agar tetap bisa diklik untuk detail
                modifier = Modifier.clickable { onClick() }
            )
            Text(
                sub.paymentMethod,
                fontSize = 12.sp,
                color = Slate500
            )
            Spacer(modifier = Modifier.height(8.dp))

            val today = LocalDate.now()
            val daysDiff = ChronoUnit.DAYS.between(today, sub.nextPaymentDate)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (sub.status == SubscriptionStatus.TRIAL && showTrialTag) {
                    BadgeCard("Free Trial", Amber500.copy(alpha = 0.15f), Amber500)
                }

                when {
                    daysDiff < 0L ->
                        BadgeCard("Terlewat ${kotlin.math.abs(daysDiff)} Hari", Rose500.copy(alpha = 0.15f), Rose500)
                    daysDiff == 0L ->
                        BadgeCard("Hari Ini", Rose500.copy(alpha = 0.15f), Rose500)
                    daysDiff in 1..3 ->
                        BadgeCard("$daysDiff Hari Lagi", Rose500.copy(alpha = 0.15f), Rose500)
                    daysDiff in 4..7 ->
                        BadgeCard("Segera Jatuh Tempo", Amber500.copy(alpha = 0.15f), Amber500)
                    else -> {
                        val formattedDate = sub.nextPaymentDate
                            .format(DateTimeFormatter.ofPattern("dd MMM yy", Locale.ENGLISH))
                        BadgeCard("$formattedDate", Emerald500.copy(alpha = 0.15f), Emerald500)
                    }
                }
            }

            if (daysDiff < 0L && (sub.status == SubscriptionStatus.ACTIVE || sub.status == SubscriptionStatus.TRIAL)) {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onMarkPaid,
                    colors = ButtonDefaults.buttonColors(containerColor = Blue600),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Tandai Dibayar", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Column(modifier = Modifier.weight(0.4f), horizontalAlignment = Alignment.End) {
            val formattedPrice = NumberFormat
                .getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                    maximumFractionDigits = 0
                }
                .format(sub.price)
            Text(
                formattedPrice,
                fontWeight = FontWeight.Black,
                fontSize = 14.sp,
                color = Color.Black
            )
            Text(
                "/${sub.billingCycle.name.lowercase()}",
                fontSize = 11.sp,
                color = Slate500
            )
        }
    }
}

@Composable
fun BadgeCard(
    text: String,
    containerColor: Color,
    textColor: Color
) {
    Box(
        modifier = Modifier
            .background(containerColor, shape = RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 0.dp)
    ) {
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = textColor)
    }
}