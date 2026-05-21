package com.example.subrek.features.subscription.presentation.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.subrek.features.subscription.presentation.viewmodel.SubscriptionDetailViewModel
import java.time.format.DateTimeFormatter
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import java.time.Instant
import java.time.ZoneId

import androidx.compose.foundation.clickable

import androidx.compose.material.icons.filled.DateRange
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionDetailScreen(
    viewModel: SubscriptionDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    val context = LocalContext.current

    // State form internal
    var priceInput by remember { mutableStateOf("") }
    var selectedCycle by remember { mutableStateOf("MONTHLY") }
    var startDateInput by remember { mutableStateOf("") }
    var paymentMethodInput by remember { mutableStateOf("") }
    var isFreeTrial by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Sinkronisasi data awal saat entitas berhasil dimuat dari DB
    LaunchedEffect(uiState.subscription) {
        uiState.subscription?.let {
            priceInput = it.price.toInt().toString()
            selectedCycle = it.billingCycle.name
            startDateInput = it.startDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            paymentMethodInput = it.paymentMethod
            isFreeTrial = it.isTrial
        }
    }

    // Aksi otomatis jika operasi mutasi berhasil dieksekusi
    if (uiState.isUpdateSuccess || uiState.isTerminationSuccess) {
        LaunchedEffect(Unit) { onNavigateBack() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Detail Langganan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else uiState.subscription?.let { sub ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(scrollState)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. AREA METADATA UTAMA (ICON & NAMA)
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = sub.iconUrl ?: "https://placeholder.co/100",
                            contentDescription = null,
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = sub.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.Gray)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(text = "Metadata sistem bawaan (Kunci)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant)

                // 2. LOGIKA PENGEDITAN FORM
                // Field 1: Nama Aplikasi (Locked - Read Only)
                OutlinedTextField(
                    value = sub.name,
                    onValueChange = {},
                    label = { Text("Nama Aplikasi") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    leadingIcon = { Icon(imageVector = Icons.Default.Lock, contentDescription = "Terkunci") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color.LightGray, unfocusedBorderColor = Color.LightGray)
                )

                // Field 2: Biaya / Harga (Editable)
                OutlinedTextField(
                    value = priceInput,
                    onValueChange = { priceInput = it },
                    label = { Text("Total Biaya") },
                    placeholder = { Text("0", color = Color.Gray.copy(alpha = 0.5f)) },
                    leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Field 3: Metode Pembayaran (Editable)
                OutlinedTextField(
                    value = paymentMethodInput,
                    onValueChange = { paymentMethodInput = it },
                    label = { Text("Metode Pembayaran") },
                    placeholder = { Text("Contoh: Gopay, Bank Transfer...", color = Color.Gray.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Field 4: Periode Subscriptions / Billing Cycle (Editable Dropdown)
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = viewModel.selectedCycle,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Periode Penagihan") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            listOf("MONTHLY", "YEARLY").forEach { cycle ->
                                DropdownMenuItem(
                                    text = { Text(cycle) },
                                    onClick = {
                                        viewModel.selectedCycle = cycle  // ← update ViewModel
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Field 5: Tanggal Mulai Penagihan (Editable with DatePicker)
                val calendar = java.util.Calendar.getInstance()
                val datePickerDialog = android.app.DatePickerDialog(
                    context,
                    { _, year, month, dayOfMonth ->
                        val selectedDate = java.time.LocalDate.of(year, month + 1, dayOfMonth)
                        startDateInput = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    },
                    calendar.get(java.util.Calendar.YEAR),
                    calendar.get(java.util.Calendar.MONTH),
                    calendar.get(java.util.Calendar.DAY_OF_MONTH)
                )

                val interactionSource = remember { MutableInteractionSource() }
                OutlinedTextField(
                    value = startDateInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Tanggal Mulai Penagihan") },
                    placeholder = { Text("Pilih Tanggal", color = Color.Gray.copy(alpha = 0.5f)) },
                    trailingIcon = {
                        IconButton(onClick = { datePickerDialog.show() }) {
                            Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                        }
                    },
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = { datePickerDialog.show() }
                        )
                )

                // Field 6: Layanan Free Trial (Switch Toggle)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Layanan Free Trial", fontWeight = FontWeight.Medium)
                    Switch(
                        checked = isFreeTrial,
                        onCheckedChange = { isFreeTrial = it }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. SELEKSI AKSI FINSIAL BUTTONS
                // Button Aksi Simpan Perubahan Detail
                Button(
                    onClick = {
                        val finalPrice = priceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                        viewModel.updateBillingDetails(
                            price = finalPrice,
                            billingCycle = selectedCycle,
                            startDate = startDateInput,
                            paymentMethod = paymentMethodInput,
                            isTrial = isFreeTrial
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Simpan Perubahan")
                }
            }

                // Button "Akhiri Langganan" di bagian paling bawah halaman
                OutlinedButton(
                    onClick = { viewModel.terminateSubscriptionService() },
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = ButtonDefaults.outlinedButtonBorder(enabled = true).copy(brush = androidx.compose.ui.graphics.SolidColor(Color.Red))
                ) {
                    Text("Akhiri Langganan", fontWeight = FontWeight.Bold)
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
