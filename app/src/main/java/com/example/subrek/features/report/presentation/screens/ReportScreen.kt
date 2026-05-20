package com.example.subrek.features.report.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.subrek.core.theme.*
import com.example.subrek.core.utils.UiState
import com.example.subrek.features.report.presentation.viewmodel.ReportViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: ReportViewModel) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analitik Laporan", fontWeight = FontWeight.Black) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Slate950,
                    titleContentColor = Slate50
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Slate950)
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // 1. INFO BOX ALERTER APABILA ADA GHOST SUBSCRIPTIONS YANG TERDETEKSI
            if (state.detectedGhostCount > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Rose500.copy(alpha = 0.15f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Ghost Detector Alert! 👻", color = Rose500, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Ditemukan ${state.detectedGhostCount} layanan aktif yang tidak dikonfirmasi selama 2 siklus penagihan berturut-turut. Status otomatis diubah menjadi 'Perlu Ditinjau'.",
                            color = Slate400,
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Selector Tipe Tren (Bulanan vs Tahunan)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Slate900, shape = RoundedCornerShape(8.dp))
                    .padding(4.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleTrendType(false) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!state.isYearlyTrend) Blue600 else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = Slate50
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) { Text("Bulanan", fontSize = 13.sp) }
                Button(
                    onClick = { viewModel.toggleTrendType(true) },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isYearlyTrend) Blue600 else androidx.compose.ui.graphics.Color.Transparent,
                        contentColor = Slate50
                    ),
                    shape = RoundedCornerShape(6.dp)
                ) { Text("Tahunan", fontSize = 13.sp) }
            }

            // 2. KANVAS REKAPITULASI GRAFIK GARIS KUSTOM (LINE CHART TREND POINTS)
            when (val reportResult = state.reportState) {
                is UiState.Success -> {
                    val report = reportResult.data
                    val points = report.trendPoints.values.toList()
                    val labels = report.trendPoints.keys.toList()

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Slate900)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Tren Biaya Pengeluaran", color = Slate400, fontSize = 13.sp)
                            val formattedPrice = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                                maximumFractionDigits = 0
                            }.format(report.totalSpend)
                            Text(formattedPrice, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Slate50)
                            
                            Spacer(modifier = Modifier.height(24.dp))

                            // Canvas Perender Grafik Garis Murni
                            Canvas(modifier = Modifier.fillMaxWidth().height(160.dp)) {
                                if (points.isNotEmpty()) {
                                    val maxVal = points.maxOrNull()?.takeIf { it > 0 } ?: 1.0
                                    val widthSpacing = size.width / (points.size - 1).coerceAtLeast(1)
                                    
                                    val path = Path()
                                    points.forEachIndexed { idx, valPoint ->
                                        val x = idx * widthSpacing
                                        val y = size.height - ((valPoint / maxVal).toFloat() * size.height)
                                        
                                        if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                        
                                        // Gambar titik koordinat lingkaran kecil disetiap sumbu simpul
                                        drawCircle(color = Blue500, radius = 8f, center = Offset(x, y))
                                    }
                                    
                                    // Gambar Garis Aliran Utama Tren Grafik
                                    drawPath(path = path, color = Blue600, style = Stroke(width = 6f))
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Baris Label Sumbu X Grafik
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                labels.forEach { text ->
                                    Text(text = text, fontSize = 10.sp, color = Slate400, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 3. BREAKDOWN PER KATEGORI
                    Text("Breakdown Per Kategori", color = Slate50, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    report.categoryBreakdown.forEach { (category, amount) ->
                        val formattedAmount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("id-ID")).apply {
                            maximumFractionDigits = 0
                        }.format(amount)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Slate900, RoundedCornerShape(12.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category, color = Slate50, fontSize = 14.sp)
                            Text(formattedAmount, color = Blue500, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
                is UiState.Loading -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) { 
                    CircularProgressIndicator(color = Blue500) 
                }
                is UiState.Error -> Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    Text("Gagal memuat data: ${reportResult.message}", color = Rose500)
                }
                else -> Unit
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
