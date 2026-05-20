package com.example.subrek.features.subscription.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // State form internal
    var priceInput by remember { mutableStateOf("") }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    // Sinkronisasi data awal saat entitas berhasil dimuat dari DB
    LaunchedEffect(uiState.subscription) {
        uiState.subscription?.let {
            priceInput = it.price.toInt().toString()
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
                            model = "https://placeholder.co/100", // Icon URL not in domain model yet, using placeholder
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
                    value = viewModel.priceInput,
                    onValueChange = { viewModel.priceInput = it },
                    label = { Text("Total Biaya (Rp)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // Field 3: Periode Subscriptions / Billing Cycle (Editable Dropdown)
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

                // Field 4: Tanggal Mulai Penagihan (Editable)
                OutlinedTextField(
                    value = viewModel.startDateInput,
                    onValueChange = {}, // Read only
                    label = { Text("Tanggal Mulai Penagihan") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }, // Klik untuk buka kalender
                    readOnly = true,
                    enabled = false, // Agar field tidak bisa diketik manual
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                        }
                    }
                )

                // DIALOG DATE PICKER (Ditaruh di bawah field atau di dalam Box)
                if (showDatePicker) {
                    // Parse tanggal saat ini untuk default value picker
                    val initialDate = try {
                        LocalDate.parse(viewModel.startDateInput, DateTimeFormatter.ISO_LOCAL_DATE)
                            .atStartOfDay(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
                    } catch (e: Exception) {
                        System.currentTimeMillis()
                    }

                    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialDate)

                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                datePickerState.selectedDateMillis?.let { millis ->
                                    val date = Instant.ofEpochMilli(millis)
                                        .atZone(ZoneId.systemDefault())
                                        .toLocalDate()
                                    // UPDATE KE VIEWMODEL
                                    viewModel.startDateInput = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                }
                                showDatePicker = false
                            }) { Text("OK") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text("Batal") }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                // 3. SELEKSI AKSI FINSIAL BUTTONS
                // Button Aksi Simpan Perubahan Detail
                Button(
                    onClick = { viewModel.updateSubscriptionBilling() },
                    modifier = Modifier.fillMaxWidth()
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
