package com.example.subrek.features.subscription.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subrek.core.theme.Blue600
import com.example.subrek.core.theme.Slate950
import com.example.subrek.core.utils.UiState
import com.example.subrek.features.subscription.domain.model.Subscription
import com.example.subrek.features.subscription.presentation.viewmodel.SubscriptionViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionListScreen(
    viewModel: SubscriptionViewModel,
    onNavigateToAdd: () -> Unit
) {
    val state by viewModel.subscriptionsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subrek", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Slate950)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onNavigateToAdd,
                containerColor = Blue600,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Langganan")
            }
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
                .padding(paddingValues)
                .navigationBarsPadding()
        ) {
            when (state) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is UiState.Success -> {
                    val subscriptions = (state as UiState.Success<List<Subscription>>).data
                    if (subscriptions.isEmpty()) {
                        Text(
                            text = "Belum ada langganan",
                            modifier = Modifier.align(Alignment.Center),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(subscriptions) { subscription ->
                                SubscriptionItem(subscription)
                            }
                        }
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = (state as UiState.Error).message,
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.error
                    )
                }
                else -> {}
            }
        }
    }
}

@Composable
fun SubscriptionItem(subscription: Subscription) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = subscription.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                // 🛍️ SESUDAH: Row tag/SuggestionChip telah dihapus sepenuhnya agar layout lebih minimalis
            }
            val formattedPrice = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                maximumFractionDigits = 0
            }.format(subscription.price)
            Text(
                text = formattedPrice,
                fontWeight = FontWeight.Bold,
                color = Blue600
            )
        }
    }
}
