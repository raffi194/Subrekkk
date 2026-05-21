package com.example.subrek.features.subscription.presentation.screens

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import com.example.subrek.features.subscription.domain.model.CatalogItem
import com.example.subrek.features.subscription.presentation.viewmodel.TambahLanggananViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.TextButton
import java.time.Instant
import java.time.ZoneId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TambahLanggananScreen(
    viewModel: TambahLanggananViewModel,
    onSuccessSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedAppForForm by remember { mutableStateOf<CatalogItem?>(null) }

    var showAppDialog by remember { mutableStateOf(false) }
    var appToDelete by remember { mutableStateOf<CatalogItem?>(null) }

    val datePickerState = rememberDatePickerState()
    var showDatePicker by remember { mutableStateOf(false) }

    // State Input Form Detail Berlangganan
    var priceInput by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("") }
    var selectedCycle by remember { mutableStateOf("Monthly") }
    var startDateInput by remember { mutableStateOf("") }
    var isFreeTrial by remember { mutableStateOf(false) }
    var isCycleDropdownExpanded by remember { mutableStateOf(false) }

    // Reset form states when user changes selected app
    LaunchedEffect(selectedAppForForm) {
        priceInput = ""
        paymentMethod = ""
        selectedCycle = "Monthly"
        startDateInput = ""
        isFreeTrial = false
        isCycleDropdownExpanded = false
    }

    val calendar = Calendar.getInstance()
    val mainDatePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
            startDateInput = selectedDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    if (uiState.isSaveSuccess) {
        LaunchedEffect(Unit) {
            viewModel.resetSaveSuccess()
            selectedAppForForm = null
            onSuccessSave()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tambah Langganan", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { showAppDialog = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Tambah App Baru")
                    }
                }
            )
        },
        contentWindowInsets = WindowInsets.statusBars
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
                    .navigationBarsPadding()
            ) {
                if (selectedAppForForm == null) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.updateSearchQuery(it) },
                        placeholder = { Text("Cari spesifik app subscriptions...", color = Color.Gray.copy(alpha = 0.5f)) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    val filteredItems = uiState.catalogItems.filter { item ->
                        item.name.contains(uiState.searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                    ) {
                        items(filteredItems, key = { it.id }) { app ->
                            // --- Implementasi Swipe to Delete ---
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = {
                                    if (it == SwipeToDismissBoxValue.EndToStart) {
                                        appToDelete = app
                                    }
                                    false
                                }
                            )

                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    val color = if (dismissState.dismissDirection == SwipeToDismissBoxValue.EndToStart)
                                        MaterialTheme.colorScheme.error else Color.Transparent
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(vertical = 4.dp)
                                            .background(color, RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.White, modifier = Modifier.padding(end = 16.dp))
                                    }
                                },
                                content = {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().clickable { selectedAppForForm = app },
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .padding(16.dp)
                                                .fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                AsyncImage(
                                                    model = app.iconUrl ?: "https://placeholder.co/100",
                                                    contentDescription = null,
                                                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                                )
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(text = app.name, fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                            )
                                        }
                                    }
                                }
                            )
                        }
                    }
                } else {
                    // FORM DETAIL BERLANGGANAN (Sama seperti kode asli Anda)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                            AsyncImage(
                                model = selectedAppForForm?.iconUrl ?: "https://placeholder.co/100",
                                contentDescription = null,
                                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(text = selectedAppForForm?.name ?: "", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }

                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            enabled = true,
                            label = { Text("Biaya Berlangganan") },
                            placeholder = { Text("0", color = Color.Gray.copy(alpha = 0.5f)) },
                            leadingIcon = { Text("Rp ", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = paymentMethod,
                            onValueChange = { paymentMethod = it },
                            enabled = true,
                            label = { Text("Metode Pembayaran") },
                            placeholder = { Text("Contoh: Gopay, Bank Transfer...", color = Color.Gray.copy(alpha = 0.5f)) },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Box(modifier = Modifier.fillMaxWidth()) {
                            ExposedDropdownMenuBox(
                                expanded = isCycleDropdownExpanded,
                                onExpandedChange = { isCycleDropdownExpanded = !isCycleDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedCycle,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Siklus Penagihan") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCycleDropdownExpanded) },
                                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                )
                                ExposedDropdownMenu(
                                    expanded = isCycleDropdownExpanded,
                                    onDismissRequest = { isCycleDropdownExpanded = false }
                                ) {
                                    listOf("Weekly", "Monthly", "Yearly").forEach { opt ->
                                        DropdownMenuItem(
                                            text = { Text(opt) },
                                            onClick = { selectedCycle = opt; isCycleDropdownExpanded = false }
                                        )
                                    }
                                }
                            }
                        }

                        val interactionSource = remember { MutableInteractionSource() }
                        OutlinedTextField(
                            value = startDateInput,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal Mulai Penagihan") },
                            placeholder = { Text("Pilih Tanggal", color = Color.Gray.copy(alpha = 0.5f)) },
                            trailingIcon = {
                                IconButton(onClick = { mainDatePickerDialog.show() }) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Pilih Tanggal")
                                }
                            },
                            interactionSource = interactionSource,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    interactionSource = interactionSource,
                                    indication = null,
                                    onClick = { mainDatePickerDialog.show() }
                                )
                        )

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Layanan Free Trial", fontWeight = FontWeight.Medium)
                            Switch(checked = isFreeTrial, onCheckedChange = { isFreeTrial = it })
                        }

                        Button(
                            onClick = {
                                val cleanPrice = priceInput.replace(",", ".").toDoubleOrNull() ?: 0.0
                                val finalPaymentMethod = paymentMethod.ifBlank { "Lainnya" }
                                viewModel.saveNewSubscription(
                                    name = selectedAppForForm!!.name,
                                    iconUrl = selectedAppForForm!!.iconUrl,
                                    price = cleanPrice,
                                    currency = "IDR",
                                    cycle = selectedCycle.uppercase(),
                                    paymentMethod = finalPaymentMethod,
                                    date = startDateInput.ifBlank { LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) },
                                    isTrial = isFreeTrial
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp).padding(top = 8.dp)
                        ) {
                            Text("Simpan Langganan", fontWeight = FontWeight.Bold)
                        }

                        TextButton(onClick = { selectedAppForForm = null }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Kembali ke Katalog") }
                    }
                }
            }

            // =========================================================================
            // DIALOG TAMBAHAN UNTUK ENTRI KUSTOM
            // =========================================================================
            if (showAppDialog) {
                var appName by remember { mutableStateOf("") }
                var selectedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }

                val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                    contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
                ) { uri: android.net.Uri? ->
                    selectedImageUri = uri
                }

                AlertDialog(
                    onDismissRequest = { showAppDialog = false },
                    title = { Text("Tambah Aplikasi Baru", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { imagePickerLauncher.launch("image/*") }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (selectedImageUri != null) {
                                        AsyncImage(
                                            model = selectedImageUri,
                                            contentDescription = "Preview Icon",
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(12.dp))
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Pilih Gambar",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column {
                                    Text("Ikon Aplikasi", fontWeight = FontWeight.SemiBold)
                                    Text(
                                        text = if (selectedImageUri != null) "Gambar terpilih" else "Tidak Wajib Diisi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (selectedImageUri != null) MaterialTheme.colorScheme.primary else Color.Gray
                                    )
                                }
                            }

                            OutlinedTextField(
                                value = appName,
                                onValueChange = { appName = it },
                                label = { Text("Nama Aplikasi *") },
                                placeholder = { Text("Contoh: Mola TV", color = Color.Gray.copy(alpha = 0.5f)) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (appName.isNotBlank()) {
                                    viewModel.addOnlyCustomApp(
                                        name = appName,
                                        imageUri = selectedImageUri
                                    )
                                    showAppDialog = false
                                }
                            }
                        ) {
                            Text("Simpan")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAppDialog = false }) { Text("Batal") }
                    }
                )
            }

            // =========================================================================
            // DIALOG KONFIRMASI HAPUS APLIKASI KATALOG
            // =========================================================================
            if (appToDelete != null) {
                AlertDialog(
                    onDismissRequest = { appToDelete = null },
                    title = { Text("Hapus Aplikasi", fontWeight = FontWeight.Bold) },
                    text = { Text("Apakah Anda yakin ingin menghapus '${appToDelete?.name}' dari katalog?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                appToDelete?.let { app ->
                                    viewModel.deleteApp(app)
                                }
                                appToDelete = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Hapus")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { appToDelete = null }) {
                            Text("Batal")
                        }
                    }
                )
            }
        }
    }
}