package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.BillingCalculator
import com.example.data.BillingInput
import com.example.data.BillingRecord
import com.example.data.BillingResult
import com.example.data.RoomBillingResult
import com.example.ui.BillingViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PdfGenerator

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.Image
import java.io.File
import android.graphics.BitmapFactory


private fun openPdf(context: Context, uri: Uri) {
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "No PDF viewer found. PDF saved to Downloads folder.", android.widget.Toast.LENGTH_LONG).show()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentWindowInsets = WindowInsets(0, 0, 0, 0)
                ) { innerPadding ->
                    BillingAppScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
        handlePdfDeepLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handlePdfDeepLink(intent)
    }

    private fun handlePdfDeepLink(intent: android.content.Intent?) {
        val data: Uri? = intent?.data
        if (data != null && (data.scheme == "http" || data.scheme == "https") && data.host == "download.auditor") {
            val roomNum = data.getQueryParameter("n")?.toIntOrNull() ?: 1
            val monthParam = data.getQueryParameter("m")?.replace("_", " ") ?: ""
            
            this.lifecycleScope.launch {
                try {
                    val db = com.example.data.BillingDatabase.getDatabase(this@MainActivity)
                    val dao = db.billingDao
                    
                    val record = if (monthParam.isNotBlank()) {
                        val latest = dao.getLatestRecord()
                        if (latest != null && latest.billingMonth.equals(monthParam, ignoreCase = true)) {
                            latest
                        } else {
                            null
                        }
                    } else {
                        dao.getLatestRecord()
                    } ?: dao.getLatestRecord()

                    if (record != null) {
                        val input = com.example.data.BillingInput(
                            prevMmr = record.prevMmr,
                            currMmr = record.currMmr,
                            prevR1 = record.prevR1,
                            currR1 = record.currR1,
                            prevR2 = record.prevR2,
                            currR2 = record.currR2,
                            prevR3 = record.prevR3,
                            currR3 = record.currR3,
                            prevR4 = record.prevR4,
                            currR4 = record.currR4,
                            prevR5 = record.prevR5,
                            currR5 = record.currR5,
                            prevR6 = record.prevR6,
                            currR6 = record.currR6,
                            prevMotor = record.prevMotor,
                            currMotor = record.currMotor,
                            rate = record.rate,
                            rent1 = record.rent1,
                            rent2 = record.rent2,
                            rent3 = record.rent3,
                            rent4 = record.rent4,
                            rent5 = record.rent5,
                            rent6 = record.rent6,
                            sweeper1 = record.sweeper1,
                            sweeper2 = record.sweeper2,
                            sweeper3 = record.sweeper3,
                            sweeper4 = record.sweeper4,
                            sweeper5 = record.sweeper5,
                            sweeper6 = record.sweeper6,
                            custom1 = record.custom1,
                            custom2 = record.custom2,
                            custom3 = record.custom3,
                            custom4 = record.custom4,
                            custom5 = record.custom5,
                            custom6 = record.custom6,
                            tenant1 = record.tenant1,
                            tenant2 = record.tenant2,
                            tenant3 = record.tenant3,
                            tenant4 = record.tenant4,
                            tenant5 = record.tenant5,
                            tenant6 = record.tenant6
                        )
                        val calcResult = com.example.data.BillingCalculator.calculate(input)
                        val roomRes = when (roomNum) {
                            1 -> calcResult.room1
                            2 -> calcResult.room2
                            3 -> calcResult.room3
                            4 -> calcResult.room4
                            5 -> calcResult.room5
                            else -> calcResult.room6
                        }
                        val prevR = when (roomNum) {
                            1 -> record.prevR1; 2 -> record.prevR2; 3 -> record.prevR3; 4 -> record.prevR4; 5 -> record.prevR5; else -> record.prevR6
                        }
                        val currR = when (roomNum) {
                            1 -> record.currR1; 2 -> record.currR2; 3 -> record.currR3; 4 -> record.currR4; 5 -> record.currR5; else -> record.currR6
                        }
                        val billNum = "BILL-${record.billingMonth.replace(" ", "")}-R$roomNum"
                        val uri = PdfGenerator.generateAndSaveRoomPdf(
                            context = this@MainActivity,
                            result = roomRes,
                            roomNum = roomNum,
                            prevReading = prevR,
                            currReading = currR,
                            month = record.billingMonth,
                            date = record.billingDate,
                            rate = record.rate,
                            billNum = billNum
                        )
                        if (uri != null) {
                            Toast.makeText(this@MainActivity, "Room $roomNum bill downloaded successfully!", Toast.LENGTH_LONG).show()
                            openPdf(this@MainActivity, uri)
                        } else {
                            Toast.makeText(this@MainActivity, "Failed to generate Room $roomNum PDF.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "No record found.", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@MainActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}

@Composable
fun MeterPhotoThumbnail(photoPath: String, onRemove: () -> Unit) {
    val bitmap = remember(photoPath) {
        try {
            if (photoPath.isNotEmpty()) {
                val file = File(photoPath)
                if (file.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 4 }
                    BitmapFactory.decodeFile(file.absolutePath, opts)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }
    
    if (bitmap != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "Meter Thumbnail",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Photo saved in app", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Delete, contentDescription = "Remove photo", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun BentoSummaryPanel(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val discrepancy = viewModel.totalElectricityUsageDiscrepancy
    val totalRent = viewModel.totalRentCollected
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Discrepancy Card
        val discColor = when {
            discrepancy > 0 -> MaterialTheme.colorScheme.error
            discrepancy < 0 -> MaterialTheme.colorScheme.tertiary
            else -> MaterialTheme.colorScheme.primary
        }
        val discBg = when {
            discrepancy > 0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            discrepancy < 0 -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.35f)
            else -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        }
        val discIcon = when {
            discrepancy == 0 -> Icons.Default.CheckCircle
            discrepancy > 0 -> Icons.Default.Warning
            else -> Icons.Default.Info
        }
        val discLabel = when {
            discrepancy > 0 -> "Main Meter Overrun"
            discrepancy < 0 -> "Main Meter Underrun"
            else -> "Perfect Balance"
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(88.dp),
            color = discBg,
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                discColor.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(discColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = discIcon,
                        contentDescription = "Discrepancy Status",
                        tint = discColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Discrepancy",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "${if (discrepancy > 0) "+" else ""}$discrepancy Units",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = discColor
                    )
                    Text(
                        text = discLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Rent Collected Card
        Surface(
            modifier = Modifier
                .weight(1f)
                .height(88.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            shape = RoundedCornerShape(16.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Rent Icon",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                Column(
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Rent Collected",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "₹${String.format(java.util.Locale.US, "%,.2f", totalRent)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Active Occupants",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

@Composable
fun BillingAppScreen(
    modifier: Modifier = Modifier,
    viewModel: BillingViewModel = viewModel()
) {
    val context = LocalContext.current
    val savedRecords by viewModel.savedRecords.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    var onPhotoCapturedCallback by remember { mutableStateOf<((Uri) -> Unit)?>(null) }
    var photoUriToCaptureInto by remember { mutableStateOf<Uri?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            photoUriToCaptureInto?.let { uri ->
                onPhotoCapturedCallback?.invoke(uri)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            photoUriToCaptureInto?.let { uri ->
                takePictureLauncher.launch(uri)
            }
        } else {
            Toast.makeText(context, "Camera permission is required to click a meter photo.", Toast.LENGTH_SHORT).show()
        }
    }

    val triggerPhotoCapture: (String, (String) -> Unit, (String) -> Unit) -> Unit = { prefix, onPathSaved, onValueRead ->
        val dir = File(context.filesDir, "meter_photos")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        val file = File(dir, "${prefix}_${System.currentTimeMillis()}.jpg")
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context,
            "com.example.fileprovider",
            file
        )
        photoUriToCaptureInto = uri
        onPhotoCapturedCallback = {
            onPathSaved(file.absolutePath)
            Toast.makeText(context, "Photo saved in app. Checking reading with Gemini AI...", Toast.LENGTH_LONG).show()
            viewModel.analyzeMeterPhotoAndFill(file.absolutePath) { parsed ->
                onValueRead(parsed)
                Toast.makeText(context, "Gemini read value: $parsed", Toast.LENGTH_SHORT).show()
            }
        }

        if (androidx.core.content.ContextCompat.checkSelfPermission(context, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            takePictureLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }

    val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    )
                )
            )
            .padding(top = statusBarPadding, bottom = navBarPadding)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Auditor Logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "Billing Auditor",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${if (viewModel.billingMonth.isNotBlank()) viewModel.billingMonth else "Oct 2024"} • Station 4",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable {
                        Toast.makeText(context, "System active on Bento Grid Engine.", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "System Specs",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Real-Time summary stats panel
        BentoSummaryPanel(viewModel = viewModel)

        // Bento segmented tab selector
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            shape = RoundedCornerShape(24.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {},
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color.Transparent
                    )
                }
            ) {
                val tabHeaders = listOf(
                    Triple("Inputs", Icons.Default.Create, 0),
                    Triple("Report", Icons.Default.Info, 1),
                    Triple("History", Icons.Default.List, 2),
                    Triple("Tenants", Icons.Default.Home, 3)
                )
                
                tabHeaders.forEach { (text, icon, tabIdx) ->
                    Tab(
                        selected = selectedTab == tabIdx,
                        onClick = {
                            if (tabIdx == 1) {
                                if (viewModel.performCalculation()) {
                                    selectedTab = tabIdx
                                } else {
                                    Toast.makeText(context, "Validation check failed: ${viewModel.errorMessage ?: "Please fix errors."}", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                selectedTab = tabIdx
                            }
                        },
                        modifier = Modifier
                            .padding(4.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (selectedTab == tabIdx) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                            ),
                        text = {
                            Text(
                                text = text,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (selectedTab == tabIdx) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        icon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = "Tab $text",
                                modifier = Modifier.size(16.dp),
                                tint = if (selectedTab == tabIdx) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    )
                }
            }
        }

        // Tab Content Router
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> MeterInputsTab(
                    viewModel = viewModel,
                    onCalculateSuccess = {
                        selectedTab = 1
                    },
                    triggerPhotoCapture = triggerPhotoCapture
                )
                1 -> AuditReportTab(
                    viewModel = viewModel,
                    onSaveRecord = {
                        viewModel.saveRecord()
                        Toast.makeText(context, "Billing record committed verified to database!", Toast.LENGTH_SHORT).show()
                        selectedTab = 2
                    }
                )
                2 -> SavedHistoryTab(
                    savedRecords = savedRecords,
                    onRestore = { record ->
                        viewModel.restoreRecord(record)
                        Toast.makeText(context, "${record.billingMonth} loaded into dashboard!", Toast.LENGTH_SHORT).show()
                        selectedTab = 1
                    },
                    onDelete = { record ->
                        viewModel.deleteRecord(record)
                        Toast.makeText(context, "Record deleted from system storage.", Toast.LENGTH_SHORT).show()
                    }
                )
                3 -> TenantsTab(
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun MeterInputsTab(
    viewModel: BillingViewModel,
    onCalculateSuccess: () -> Unit,
    triggerPhotoCapture: (String, (String) -> Unit, (String) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // App Briefing card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Landlord Dashboard Input Station",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Fill main meters, active room occupant names, sweeper rates, rents, and custom charges per room to calculate dynamic allocations instantly.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Global Parameters Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Global Period & Monetary Parameters",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.billingMonth,
                            onValueChange = { 
                                viewModel.billingMonth = it 
                                viewModel.checkAndAutofillNextMonth(it)
                            },
                            label = { Text("Billing Month") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.billingDate,
                            onValueChange = { viewModel.billingDate = it },
                            label = { Text("Billing Date") },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    viewModel.latestSavedRecord?.let { latest ->
                        val nextMonth = viewModel.getNextMonthName(latest.billingMonth)
                        OutlinedButton(
                            onClick = {
                                viewModel.billingMonth = nextMonth
                                viewModel.prevMmr = latest.currMmr.toString()
                                viewModel.prevR1 = latest.currR1.toString()
                                viewModel.prevR2 = latest.currR2.toString()
                                viewModel.prevR3 = latest.currR3.toString()
                                viewModel.prevR4 = latest.currR4.toString()
                                viewModel.prevR5 = latest.currR5.toString()
                                viewModel.prevR6 = latest.currR6.toString()
                                viewModel.prevMotor = latest.currMotor.toString()
                                
                                viewModel.currMmr = ""
                                viewModel.currR1 = ""
                                viewModel.currR2 = ""
                                viewModel.currR3 = ""
                                viewModel.currR4 = ""
                                viewModel.currR5 = ""
                                viewModel.currR6 = ""
                                viewModel.currMotor = ""

                                viewModel.rate = latest.rate.toString()

                                viewModel.tenant1 = latest.tenant1
                                viewModel.tenant2 = latest.tenant2
                                viewModel.tenant3 = latest.tenant3
                                viewModel.tenant4 = latest.tenant4
                                viewModel.tenant5 = latest.tenant5
                                viewModel.tenant6 = latest.tenant6

                                viewModel.rent1 = latest.rent1.toString()
                                viewModel.rent2 = latest.rent2.toString()
                                viewModel.rent3 = latest.rent3.toString()
                                viewModel.rent4 = latest.rent4.toString()
                                viewModel.rent5 = latest.rent5.toString()
                                viewModel.rent6 = latest.rent6.toString()

                                viewModel.sweeper1 = latest.sweeper1.toString()
                                viewModel.sweeper2 = latest.sweeper2.toString()
                                viewModel.sweeper3 = latest.sweeper3.toString()
                                viewModel.sweeper4 = latest.sweeper4.toString()
                                viewModel.sweeper5 = latest.sweeper5.toString()
                                viewModel.sweeper6 = latest.sweeper6.toString()

                                viewModel.custom1 = latest.custom1.toString()
                                viewModel.custom2 = latest.custom2.toString()
                                viewModel.custom3 = latest.custom3.toString()
                                viewModel.custom4 = latest.custom4.toString()
                                viewModel.custom5 = latest.custom5.toString()
                                viewModel.custom6 = latest.custom6.toString()
                                
                                viewModel.performCalculation()
                                Toast.makeText(context, "Welcome to $nextMonth! Previous readings and metadata loaded from ${latest.billingMonth} currents.", Toast.LENGTH_LONG).show()
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "start next month",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start $nextMonth from ${latest.billingMonth} readings", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.rate,
                            onValueChange = { viewModel.rate = it },
                            label = { Text("Elec Rate (Per Unit)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Main & Motor meter readings Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Auditor Master Readings Checkpoint",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // MMR
                    Text("Main Meter Readings (MMR)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = viewModel.prevMmr,
                            onValueChange = { viewModel.prevMmr = it },
                            label = { Text("Prev MMR Unit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.currMmr,
                            onValueChange = { viewModel.currMmr = it },
                            label = { Text("Curr MMR Unit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        triggerPhotoCapture(
                                            "mmr",
                                            { path: String -> viewModel.photoMmr = path },
                                            { value: String -> viewModel.currMmr = value }
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = "Take Photo",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }

                    if (viewModel.photoMmr.isNotEmpty()) {
                        MeterPhotoThumbnail(
                            photoPath = viewModel.photoMmr,
                            onRemove = { viewModel.photoMmr = "" }
                        )
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Motor
                    Text("Shared Water Motor Readings (Motor)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedTextField(
                            value = viewModel.prevMotor,
                            onValueChange = { viewModel.prevMotor = it },
                            label = { Text("Prev Motor Unit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = viewModel.currMotor,
                            onValueChange = { viewModel.currMotor = it },
                            label = { Text("Curr Motor Unit") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Room-Specific Configurations Header
        item {
            Text(
                text = "Individual Room Portfolios (R1-R6)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // 6 Room Cards with Room-specific Tenant, Sweeper, Customs, Readings!
        items(6) { index ->
            val roomNum = index + 1
            
            // Map mutable values dynamically
            val tenantVal = when(roomNum) {
                1 -> viewModel.tenant1; 2 -> viewModel.tenant2; 3 -> viewModel.tenant3; 4 -> viewModel.tenant4; 5 -> viewModel.tenant5; else -> viewModel.tenant6
            }
            val prevVal = when(roomNum) {
                1 -> viewModel.prevR1; 2 -> viewModel.prevR2; 3 -> viewModel.prevR3; 4 -> viewModel.prevR4; 5 -> viewModel.prevR5; else -> viewModel.prevR6
            }
            val currVal = when(roomNum) {
                1 -> viewModel.currR1; 2 -> viewModel.currR2; 3 -> viewModel.currR3; 4 -> viewModel.currR4; 5 -> viewModel.currR5; else -> viewModel.currR6
            }
            val rentVal = when(roomNum) {
                1 -> viewModel.rent1; 2 -> viewModel.rent2; 3 -> viewModel.rent3; 4 -> viewModel.rent4; 5 -> viewModel.rent5; else -> viewModel.rent6
            }
            val sweeperVal = when(roomNum) {
                1 -> viewModel.sweeper1; 2 -> viewModel.sweeper2; 3 -> viewModel.sweeper3; 4 -> viewModel.sweeper4; 5 -> viewModel.sweeper5; else -> viewModel.sweeper6
            }
            val customVal = when(roomNum) {
                1 -> viewModel.custom1; 2 -> viewModel.custom2; 3 -> viewModel.custom3; 4 -> viewModel.custom4; 5 -> viewModel.custom5; else -> viewModel.custom6
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val headerTenantSuffix = if (tenantVal.isNotBlank() && !tenantVal.equals("vacant", ignoreCase = true)) " ($tenantVal)" else " (VACANT)"
                        Text(
                            text = "ROOM $roomNum$headerTenantSuffix PARAMETERS",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val allocationLabel = if (roomNum <= 3) "Discrepancy + Motor" else "Motor Only"
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = allocationLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                    // Tenant Name Option
                    OutlinedTextField(
                        value = tenantVal,
                        onValueChange = { name ->
                            when(roomNum) {
                                1 -> viewModel.tenant1 = name; 2 -> viewModel.tenant2 = name; 3 -> viewModel.tenant3 = name
                                4 -> viewModel.tenant4 = name; 5 -> viewModel.tenant5 = name; else -> viewModel.tenant6 = name
                            }
                        },
                        label = { Text("Tenant Name (type 'VACANT' if empty)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )                     // Previous / Current Readings
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = prevVal,
                            onValueChange = {
                                when(roomNum) {
                                    1 -> viewModel.prevR1 = it; 2 -> viewModel.prevR2 = it; 3 -> viewModel.prevR3 = it
                                    4 -> viewModel.prevR4 = it; 5 -> viewModel.prevR5 = it; else -> viewModel.prevR6 = it
                                }
                            },
                            label = { Text("Prev Rd") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = currVal,
                            onValueChange = {
                                when(roomNum) {
                                    1 -> viewModel.currR1 = it; 2 -> viewModel.currR2 = it; 3 -> viewModel.currR3 = it
                                    4 -> viewModel.currR4 = it; 5 -> viewModel.currR5 = it; else -> viewModel.currR6 = it
                                }
                            },
                            label = { Text("Curr Rd") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            trailingIcon = {
                                IconButton(
                                    onClick = {
                                        triggerPhotoCapture(
                                            "room_$roomNum",
                                            { path: String ->
                                                when(roomNum) {
                                                    1 -> viewModel.photoR1 = path; 2 -> viewModel.photoR2 = path; 3 -> viewModel.photoR3 = path
                                                    4 -> viewModel.photoR4 = path; 5 -> viewModel.photoR5 = path; else -> viewModel.photoR6 = path
                                                }
                                            },
                                            { value: String ->
                                                when(roomNum) {
                                                    1 -> viewModel.currR1 = value; 2 -> viewModel.currR2 = value; 3 -> viewModel.currR3 = value
                                                    4 -> viewModel.currR4 = value; 5 -> viewModel.currR5 = value; else -> viewModel.currR6 = value
                                                }
                                            }
                                        )
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Create,
                                        contentDescription = "Take Photo",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }

                    val roomPhotoPath = when(roomNum) {
                        1 -> viewModel.photoR1; 2 -> viewModel.photoR2; 3 -> viewModel.photoR3
                        4 -> viewModel.photoR4; 5 -> viewModel.photoR5; else -> viewModel.photoR6
                    }
                    if (roomPhotoPath.isNotEmpty()) {
                        MeterPhotoThumbnail(
                            photoPath = roomPhotoPath,
                            onRemove = {
                                when(roomNum) {
                                    1 -> viewModel.photoR1 = ""; 2 -> viewModel.photoR2 = ""; 3 -> viewModel.photoR3 = ""
                                    4 -> viewModel.photoR4 = ""; 5 -> viewModel.photoR5 = ""; else -> viewModel.photoR6 = ""
                                }
                            }
                        )
                    }

                    // Charges: Rent (₹), Sweeper Charge (₹), Custom Charge (₹)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rentVal,
                            onValueChange = {
                                when(roomNum) {
                                    1 -> viewModel.rent1 = it; 2 -> viewModel.rent2 = it; 3 -> viewModel.rent3 = it
                                    4 -> viewModel.rent4 = it; 5 -> viewModel.rent5 = it; else -> viewModel.rent6 = it
                                }
                            },
                            label = { Text("Rent (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = sweeperVal,
                            onValueChange = {
                                when(roomNum) {
                                    1 -> viewModel.sweeper1 = it; 2 -> viewModel.sweeper2 = it; 3 -> viewModel.sweeper3 = it
                                    4 -> viewModel.sweeper4 = it; 5 -> viewModel.sweeper5 = it; else -> viewModel.sweeper6 = it
                                }
                            },
                            label = { Text("Sweeper (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = customVal,
                            onValueChange = {
                                when(roomNum) {
                                    1 -> viewModel.custom1 = it; 2 -> viewModel.custom2 = it; 3 -> viewModel.custom3 = it
                                    4 -> viewModel.custom4 = it; 5 -> viewModel.custom5 = it; else -> viewModel.custom6 = it
                                }
                            },
                            label = { Text("Custom (₹)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true
                        )
                    }
                }
            }
        }

        // Bottom CTA controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            viewModel.resetToExampleData()
                            Toast.makeText(context, "Inputs reset to standard mock ledger data.", Toast.LENGTH_SHORT).show()
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.Refresh, contentDescription = "Reset inputs", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reset Demo", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            if (viewModel.performCalculation()) {
                                Toast.makeText(context, "Billing verified. Directing to Audit report!", Toast.LENGTH_SHORT).show()
                                onCalculateSuccess()
                            } else {
                                Toast.makeText(context, "Calculations rejected: ${viewModel.errorMessage ?: "Please fix errors"}", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Submit audit", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify & Audit", style = MaterialTheme.typography.bodyMedium, maxLines = 1)
                    }
                }
            }
        }

        // Spacer list footer
        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun AuditReportTab(
    viewModel: BillingViewModel,
    onSaveRecord: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val result = viewModel.calculationResult ?: return

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // SUMMARY SECTION AT THE TOP OF THE REPORT
        item {
            Spacer(modifier = Modifier.height(8.dp))
            ReportSummarySection(result = result)
        }

        // DOWNLOADS AND EXPORTS OFFICE
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.25f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "EXPORTS & DOWNLOAD OFFICE",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    
                    Text(
                        text = "Quick export system for professional audited invoice PDFs. Generate the aggregated master sheet or individual room slips directly to your device.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                val uri = PdfGenerator.generateAndSaveMasterPdf(
                                    context = context,
                                    result = result,
                                    month = viewModel.billingMonth,
                                    date = viewModel.billingDate
                                )
                                if (uri != null) {
                                    Toast.makeText(context, "Master report PDF downloaded successfully!", Toast.LENGTH_SHORT).show()
                                    openPdf(context, uri)
                                } else {
                                    Toast.makeText(context, "Error saving Master PDF.", Toast.LENGTH_SHORT).show()
                                }
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Download Master",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download Master PDF", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            var count = 0
                            val roomsList = listOf(
                                Triple(result.room1, 1, viewModel.tenant1),
                                Triple(result.room2, 2, viewModel.tenant2),
                                Triple(result.room3, 3, viewModel.tenant3),
                                Triple(result.room4, 4, viewModel.tenant4),
                                Triple(result.room5, 5, viewModel.tenant5),
                                Triple(result.room6, 6, viewModel.tenant6)
                            )
                            roomsList.forEach { (roomRes, num, tenant) ->
                                val billNum = "BILL-${viewModel.billingMonth.replace(" ", "")}-R$num"
                                val prevR = when (num) {
                                    1 -> viewModel.prevR1; 2 -> viewModel.prevR2; 3 -> viewModel.prevR3; 4 -> viewModel.prevR4; 5 -> viewModel.prevR5; else -> viewModel.prevR6
                                }.toIntOrNull() ?: 0
                                val currR = when (num) {
                                    1 -> viewModel.currR1; 2 -> viewModel.currR2; 3 -> viewModel.currR3; 4 -> viewModel.currR4; 5 -> viewModel.currR5; else -> viewModel.currR6
                                }.toIntOrNull() ?: 0
                                val uri = PdfGenerator.generateAndSaveRoomPdf(
                                    context, roomRes, num, prevR, currR, viewModel.billingMonth, viewModel.billingDate, result.input.rate, billNum
                                )
                                if (uri != null) count++
                            }
                            Toast.makeText(context, "Downloaded $count individual room PDFs in downloads archive!", Toast.LENGTH_LONG).show()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Download Room Bills",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download All 6 Room PDFs", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedButton(
                        onClick = {
                            val record = com.example.data.BillingRecord(
                                billingMonth = viewModel.billingMonth,
                                billingDate = viewModel.billingDate,
                                prevMmr = viewModel.prevMmr.toIntOrNull() ?: 0,
                                currMmr = viewModel.currMmr.toIntOrNull() ?: 0,
                                prevR1 = viewModel.prevR1.toIntOrNull() ?: 0,
                                currR1 = viewModel.currR1.toIntOrNull() ?: 0,
                                prevR2 = viewModel.prevR2.toIntOrNull() ?: 0,
                                currR2 = viewModel.currR2.toIntOrNull() ?: 0,
                                prevR3 = viewModel.prevR3.toIntOrNull() ?: 0,
                                currR3 = viewModel.currR3.toIntOrNull() ?: 0,
                                prevR4 = viewModel.prevR4.toIntOrNull() ?: 0,
                                currR4 = viewModel.currR4.toIntOrNull() ?: 0,
                                prevR5 = viewModel.prevR5.toIntOrNull() ?: 0,
                                currR5 = viewModel.currR5.toIntOrNull() ?: 0,
                                prevR6 = viewModel.prevR6.toIntOrNull() ?: 0,
                                currR6 = viewModel.currR6.toIntOrNull() ?: 0,
                                prevMotor = viewModel.prevMotor.toIntOrNull() ?: 0,
                                currMotor = viewModel.currMotor.toIntOrNull() ?: 0,
                                rate = viewModel.rate.toDoubleOrNull() ?: 0.0,
                                rent1 = viewModel.rent1.toDoubleOrNull() ?: 0.0,
                                rent2 = viewModel.rent2.toDoubleOrNull() ?: 0.0,
                                rent3 = viewModel.rent3.toDoubleOrNull() ?: 0.0,
                                rent4 = viewModel.rent4.toDoubleOrNull() ?: 0.0,
                                rent5 = viewModel.rent5.toDoubleOrNull() ?: 0.0,
                                rent6 = viewModel.rent6.toDoubleOrNull() ?: 0.0,
                                sweeper1 = viewModel.sweeper1.toDoubleOrNull() ?: 0.0,
                                sweeper2 = viewModel.sweeper2.toDoubleOrNull() ?: 0.0,
                                sweeper3 = viewModel.sweeper3.toDoubleOrNull() ?: 0.0,
                                sweeper4 = viewModel.sweeper4.toDoubleOrNull() ?: 0.0,
                                sweeper5 = viewModel.sweeper5.toDoubleOrNull() ?: 0.0,
                                sweeper6 = viewModel.sweeper6.toDoubleOrNull() ?: 0.0,
                                custom1 = viewModel.custom1.toDoubleOrNull() ?: 0.0,
                                custom2 = viewModel.custom2.toDoubleOrNull() ?: 0.0,
                                custom3 = viewModel.custom3.toDoubleOrNull() ?: 0.0,
                                custom4 = viewModel.custom4.toDoubleOrNull() ?: 0.0,
                                custom5 = viewModel.custom5.toDoubleOrNull() ?: 0.0,
                                custom6 = viewModel.custom6.toDoubleOrNull() ?: 0.0,
                                tenant1 = viewModel.tenant1,
                                tenant2 = viewModel.tenant2,
                                tenant3 = viewModel.tenant3,
                                tenant4 = viewModel.tenant4,
                                tenant5 = viewModel.tenant5,
                                tenant6 = viewModel.tenant6,
                                photoMmr = viewModel.photoMmr,
                                photoR1 = viewModel.photoR1,
                                photoR2 = viewModel.photoR2,
                                photoR3 = viewModel.photoR3,
                                photoR4 = viewModel.photoR4,
                                photoR5 = viewModel.photoR5,
                                photoR6 = viewModel.photoR6
                            )
                            val uri = PdfGenerator.generateAndSaveMeterRecordsPdf(
                                context = context,
                                record = record,
                                month = viewModel.billingMonth,
                                date = viewModel.billingDate
                            )
                            if (uri != null) {
                                Toast.makeText(context, "Meter record PDF downloaded successfully!", Toast.LENGTH_SHORT).show()
                                openPdf(context, uri)
                            } else {
                                Toast.makeText(context, "Error saving Meter Record PDF.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Create,
                            contentDescription = "Download Meter Record",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Download Meter Record PDF", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Section A: Calculations Step
        item {
            Text(
                text = "A. Mathematical Allocation Audit Trail",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AuditStepCard(
                        stepNum = 1,
                        title = "Unadjusted Room Deductions",
                        formula = "Formula: (Current - Previous) Readings",
                        calculation = """
                            R1 = ${result.input.currR1} - ${result.input.prevR1} = ${result.diffR1} units (Tenant: ${result.room1.tenantName})
                            R2 = ${result.input.currR2} - ${result.input.prevR2} = ${result.diffR2} units (Tenant: ${result.room2.tenantName})
                            R3 = ${result.input.currR3} - ${result.input.prevR3} = ${result.diffR3} units (Tenant: ${result.room3.tenantName})
                            R4 = ${result.input.currR4} - ${result.input.prevR4} = ${result.diffR4} units (Tenant: ${result.room4.tenantName})
                            R5 = ${result.input.currR5} - ${result.input.prevR5} = ${result.diffR5} units (Tenant: ${result.room5.tenantName})
                            R6 = ${result.input.currR6} - ${result.input.prevR6} = ${result.diffR6} units (Tenant: ${result.room6.tenantName})
                            Water Motor = ${result.input.currMotor} - ${result.input.prevMotor} = ${result.diffMotor} units
                        """.trimIndent()
                    )

                    AuditStepCard(
                        stepNum = 2,
                        title = "Collective Individual Unadjusted Sum (K)",
                        formula = "Formula: K = R1 + R2 + R3 + R4 + R5 + R6 + Motor",
                        calculation = """
                            K = ${result.diffR1} + ${result.diffR2} + ${result.diffR3} + ${result.diffR4} + ${result.diffR5} + ${result.diffR6} + ${result.diffMotor} = ${result.totalIndividualK} units
                        """.trimIndent()
                    )

                    AuditStepCard(
                        stepNum = 3,
                        title = "Main Line Meter Readings (MMR Difference)",
                        formula = "Formula: MMR = Current MMR - Previous MMR",
                        calculation = """
                            MMR = ${result.input.currMmr} - ${result.input.prevMmr} = ${result.mainMeterMMR} units
                        """.trimIndent()
                    )

                    AuditStepCard(
                        stepNum = 4,
                        title = "Discrepancy Consumption Unit (L)",
                        formula = "Formula: L = MMR - K",
                        calculation = """
                            L = ${result.mainMeterMMR} - ${result.totalIndividualK} = ${result.discrepancyL} units
                        """.trimIndent()
                    )

                    AuditStepCard(
                        stepNum = 5,
                        title = "Equal Discrepancy Allocation (Rooms 1, 2, 3)",
                        formula = "Formula: Divided equally with remainders to lowest original consumption.",
                        calculation = """
                            Sorted Rooms: R1=${result.diffR1} un, R2=${result.diffR2} un, R3=${result.diffR3} un
                            Equal distribution allocated:
                            Room 1 += ${result.discDistR1} units
                            Room 2 += ${result.discDistR2} units
                            Room 3 += ${result.discDistR3} units
                            Distributed total discrepancy = ${result.discDistR1 + result.discDistR2 + result.discDistR3} of ${result.discrepancyL} units.
                        """.trimIndent()
                    )

                    AuditStepCard(
                        stepNum = 6,
                        title = "Water Motor Distribution (All 6 Rooms)",
                        formula = "Formula: Shared evenly among 6 rooms with remainders to lowest original consumption.",
                        calculation = """
                            Motor difference sum: ${result.diffMotor} units
                            Equal allocation with lowest-volume-first remainder handling:
                            Room 1 += ${result.motorDistR1} units
                            Room 2 += ${result.motorDistR2} units
                            Room 3 += ${result.motorDistR3} units
                            Room 4 += ${result.motorDistR4} units
                            Room 5 += ${result.motorDistR5} units
                            Room 6 += ${result.motorDistR6} units
                            Shared sum distributed = ${result.motorDistR1 + result.motorDistR2 + result.motorDistR3 + result.motorDistR4 + result.motorDistR5 + result.motorDistR6} of ${result.diffMotor} units.
                        """.trimIndent()
                    )
                }
            }
        }

        // ROOM BILLING TABLE SECTION (Section B)
        item {
            Text(
                text = "B. Room Billing Assessments & Invoices",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        item {
            RoomBillingTable(
                result = result,
                viewModel = viewModel,
                onDownloadPdf = { roomNum, roomRes ->
                    val billNum = "BILL-${viewModel.billingMonth.replace(" ", "")}-R$roomNum"
                    val prevR = when (roomNum) {
                        1 -> viewModel.prevR1; 2 -> viewModel.prevR2; 3 -> viewModel.prevR3; 4 -> viewModel.prevR4; 5 -> viewModel.prevR5; else -> viewModel.prevR6
                    }.toIntOrNull() ?: 0
                    val currR = when (roomNum) {
                        1 -> viewModel.currR1; 2 -> viewModel.currR2; 3 -> viewModel.currR3; 4 -> viewModel.currR4; 5 -> viewModel.currR5; else -> viewModel.currR6
                    }.toIntOrNull() ?: 0
                    val uri = PdfGenerator.generateAndSaveRoomPdf(
                        context, roomRes, roomNum, prevR, currR, viewModel.billingMonth, viewModel.billingDate, result.input.rate, billNum
                    )
                    if (uri != null) {
                        Toast.makeText(context, "Room $roomNum bill downloaded successfully to Downloads folder!", Toast.LENGTH_LONG).show()
                        openPdf(context, uri)
                    } else {
                        Toast.makeText(context, "Error saving Room PDF.", Toast.LENGTH_LONG).show()
                    }
                }
            )
        }

        // Commit CTA Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Commit Account Settlement?",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "This will freeze all tenant names, individual rents, sweeper rates, custom charges, and verify totals into the secure historical database permanently.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = onSaveRecord,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = "Commit safe", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Verify, Freeze, & Commit Bill")
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun ReportSummarySection(result: com.example.data.BillingResult, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "BILLING CYCLE SUMMARY",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            
            // MAIN METER READING
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MAIN METER READING", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Main Mainline Meter", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Prev", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${result.input.prevMmr}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Curr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${result.input.currMmr}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Diff", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${result.mainMeterMMR}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))

            // METER READING (Motor Meter Details)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("MOTOR METER", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Water Motor Pump", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Prev", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${result.input.prevMotor}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Curr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("${result.input.currMotor}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Diff (Meter Reading)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        Text("${result.diffMotor}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            
            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))

            // SUBMISSION UNIT: all six room meter units combined
            val submissionUnitBytes = result.diffR1 + result.diffR2 + result.diffR3 + result.diffR4 + result.diffR5 + result.diffR6
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "SUBMISSION UNIT (Combined Rooms)",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$submissionUnitBytes units",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))

            // UTILITY READING: discrepancy allocation in readable format
            // e.g. "4 + 5 + 5" instead of "14" indicating distribution among R1, R2, R3
            val readableUtility = "${result.discDistR1} + ${result.discDistR2} + ${result.discDistR3}"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "UTILITY READING (Discrepancy)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Allocated among R1, R2, R3",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                Text(
                    text = "$readableUtility = ${result.discrepancyL} units",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (result.discrepancyL == 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))

            // TOTAL UNIT: Meter Reading + Submission Unit + Utility Reading for the final total
            val finalTotalUnitCalculated = result.diffMotor + submissionUnitBytes + result.discrepancyL
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL UNIT (Verified Sum)",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${result.diffMotor} + $submissionUnitBytes + ${result.discrepancyL} = $finalTotalUnitCalculated units",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun RoomBillingTable(
    result: com.example.data.BillingResult,
    viewModel: BillingViewModel,
    onDownloadPdf: (Int, com.example.data.RoomBillingResult) -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ROOM BILLING ASSESSMENT TABLE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Text(
                text = "← Swipe horizontally to view full table columns →",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(modifier = Modifier.horizontalScroll(scrollState)) {
                Column {
                    // Two-level Table Header
                    Row(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(vertical = 8.dp)
                    ) {
                        TableCell("Room", width = 85.dp, isHeader = true)
                        TableCell("Tenant Name", width = 125.dp, isHeader = true)
                        
                        // Electric Unit Header Span (comprising 4 sub-sections)
                        Column(modifier = Modifier.width(260.dp)) {
                            Text(
                                "Electric Unit (Original + Motor + Utility)",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(modifier = Modifier.fillMaxWidth()) {
                                TableCell("Meter Unit", width = 65.dp, isHeader = true, isSubHeader = true, textAlign = TextAlign.Center)
                                TableCell("Meter Div.", width = 65.dp, isHeader = true, isSubHeader = true, textAlign = TextAlign.Center)
                                TableCell("Utility", width = 65.dp, isHeader = true, isSubHeader = true, textAlign = TextAlign.Center)
                                TableCell("Total", width = 65.dp, isHeader = true, isSubHeader = true, textAlign = TextAlign.Center)
                            }
                        }
                        
                        TableCell("Elec. Charge", width = 100.dp, isHeader = true)
                        TableCell("Rent", width = 90.dp, isHeader = true)
                        TableCell("Sweeper", width = 80.dp, isHeader = true)
                        TableCell("Total Amount", width = 105.dp, isHeader = true)
                        TableCell("PDF", width = 135.dp, isHeader = true)
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f))

                    val roomsList = listOf(
                        Triple(1, result.room1, 1),
                        Triple(2, result.room2, 2),
                        Triple(3, result.room3, 3),
                        Triple(4, result.room4, 4),
                        Triple(5, result.room5, 5),
                        Triple(6, result.room6, 6)
                    )

                    roomsList.forEach { (roomNum, roomRes, _) ->
                        Row(
                            modifier = Modifier
                                .padding(vertical = 4.dp)
                                .background(
                                    if (roomNum % 2 == 1) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                                    else Color.Transparent
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TableCell("Room $roomNum", width = 85.dp)
                            val tenantNameStr = if (roomRes.tenantName.isBlank() || roomRes.tenantName.equals("vacant", ignoreCase = true)) "VACANT" else roomRes.tenantName
                            TableCell(tenantNameStr, width = 125.dp, color = if (tenantNameStr == "VACANT") MaterialTheme.colorScheme.error else Color.Unspecified)
                            
                            // Subsections for Electric Unit
                            TableCell("${roomRes.originalDiff}", width = 65.dp, textAlign = TextAlign.Center)
                            TableCell("${roomRes.sharedMotor}", width = 65.dp, textAlign = TextAlign.Center)
                            TableCell("${roomRes.sharedDiscrepancy}", width = 65.dp, textAlign = TextAlign.Center)
                            TableCell("${roomRes.updatedUnits}", width = 65.dp, isBold = true, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
                            
                            TableCell("₹${String.format("%.1f", roomRes.electricityCharge)}", width = 100.dp)
                            TableCell("₹${String.format("%.1f", roomRes.rent)}", width = 90.dp)
                            TableCell("₹${String.format("%.1f", roomRes.sweeper)}", width = 80.dp)
                            TableCell("₹${String.format("%.1f", roomRes.totalBill)}", width = 105.dp, isBold = true, color = MaterialTheme.colorScheme.secondary)
                            
                            // Download button cell
                            Box(
                                modifier = Modifier
                                    .width(135.dp)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Button(
                                    onClick = { onDownloadPdf(roomNum, roomRes) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.fillMaxWidth().height(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Download Bill",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Download now", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                    }
                }
            }
        }
    }
}

@Composable
fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isSubHeader: Boolean = false,
    isBold: Boolean = false,
    color: Color = Color.Unspecified,
    textAlign: TextAlign = TextAlign.Start
) {
    Box(
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = when(textAlign) {
            TextAlign.Center -> Alignment.Center
            TextAlign.End -> Alignment.CenterEnd
            else -> Alignment.CenterStart
        }
    ) {
        Text(
            text = text,
            style = if (isHeader) {
                if (isSubHeader) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.bodySmall
            },
            fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isHeader) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else if (color != Color.Unspecified) {
                color
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = textAlign
        )
    }
}

@Composable
fun SavedHistoryTab(
    savedRecords: List<BillingRecord>,
    onRestore: (BillingRecord) -> Unit,
    onDelete: (BillingRecord) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    if (savedRecords.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Default.List, contentDescription = "History empty icon", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(28.dp))
                }
                Text(
                    text = "No Archives Found",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.outline,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "No previous billing audits have been committed. Enter data in inputs and tap 'Verify & Audit' then 'Commit' to save.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Historical Audits Archive",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "These historical billing statements are frozen immutable copies. Restore them to active inputs or download reports.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            items(savedRecords) { record ->
                // Reverse engineering BillingResult from history to make sure reproducibility holds 100%!
                val historicalInput = BillingInput(
                    prevMmr = record.prevMmr, currMmr = record.currMmr,
                    prevR1 = record.prevR1, currR1 = record.currR1,
                    prevR2 = record.prevR2, currR2 = record.currR2,
                    prevR3 = record.prevR3, currR3 = record.currR3,
                    prevR4 = record.prevR4, currR4 = record.currR4,
                    prevR5 = record.prevR5, currR5 = record.currR5,
                    prevR6 = record.prevR6, currR6 = record.currR6,
                    prevMotor = record.prevMotor, currMotor = record.currMotor,
                    rate = record.rate,
                    sweeper1 = record.sweeper1, sweeper2 = record.sweeper2, sweeper3 = record.sweeper3, sweeper4 = record.sweeper4, sweeper5 = record.sweeper5, sweeper6 = record.sweeper6,
                    rent1 = record.rent1, rent2 = record.rent2, rent3 = record.rent3, rent4 = record.rent4, rent5 = record.rent5, rent6 = record.rent6,
                    custom1 = record.custom1, custom2 = record.custom2, custom3 = record.custom3, custom4 = record.custom4, custom5 = record.custom5, custom6 = record.custom6,
                    tenant1 = record.tenant1, tenant2 = record.tenant2, tenant3 = record.tenant3, tenant4 = record.tenant4, tenant5 = record.tenant5, tenant6 = record.tenant6
                )
                val historicalResult = BillingCalculator.calculate(historicalInput)

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = record.billingMonth,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Audited Date: ${record.billingDate}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }

                            IconButton(
                                onClick = { onDelete(record) },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove record",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Brief summary metrics of that frozen month
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(text = "Unadjusted MMR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(text = "${historicalResult.mainMeterMMR} units", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Column {
                                Text(text = "R1-R6 Allocated", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(text = "${historicalResult.totalUpdatedUnits} units", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "Verif.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(
                                    text = "PASSED ✓",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        // Room occupant list for this billing history period
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text("AUDITED ROOM OCCUPANTS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                            
                            val tenantsList = listOf(
                                "R1: ${if (record.tenant1.isBlank() || record.tenant1.equals("vacant", ignoreCase = true)) "VACANT" else "${record.tenant1} (₹${record.rent1.toInt()})"}",
                                "R2: ${if (record.tenant2.isBlank() || record.tenant2.equals("vacant", ignoreCase = true)) "VACANT" else "${record.tenant2} (₹${record.rent2.toInt()})"}",
                                "R3: ${if (record.tenant3.isBlank() || record.tenant3.equals("vacant", ignoreCase = true)) "VACANT" else "${record.tenant3} (₹${record.rent3.toInt()})"}",
                                "R4: ${if (record.tenant4.isBlank() || record.tenant4.equals("vacant", ignoreCase = true)) "VACANT" else "${record.tenant4} (₹${record.rent4.toInt()})"}",
                                "R5: ${if (record.tenant5.isBlank() || record.tenant5.equals("vacant", ignoreCase = true)) "VACANT" else "${record.tenant5} (₹${record.rent5.toInt()})"}",
                                "R6: ${if (record.tenant6.isBlank() || record.tenant6.equals("vacant", ignoreCase = true)) "VACANT" else "${record.tenant6} (₹${record.rent6.toInt()})"}"
                            )
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (0..2).forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = tenantsList[col], style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                (3..5).forEach { col ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(text = tenantsList[col], style = MaterialTheme.typography.labelSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        // Trigger buttons inside archive: RESTORE, DOWNLOAD MASTER, DOWNLOAD ALL INDIVIDUALS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onRestore(record) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Restore inputs", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Restore Active", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }

                            Button(
                                onClick = {
                                    val uri = PdfGenerator.generateAndSaveMasterPdf(context, historicalResult, record.billingMonth, record.billingDate)
                                    if (uri != null) {
                                        Toast.makeText(context, "Historical frozen Master PDF generated successfully!", Toast.LENGTH_SHORT).show()
                                        openPdf(context, uri)
                                    } else {
                                        Toast.makeText(context, "Error reproducing PDF.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(imageVector = Icons.Default.Star, contentDescription = "Download custom master", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Master PDF", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }

                            OutlinedButton(
                                onClick = {
                                    var count = 0
                                    listOf(
                                        Triple(historicalResult.room1, 1, record.tenant1),
                                        Triple(historicalResult.room2, 2, record.tenant2),
                                        Triple(historicalResult.room3, 3, record.tenant3),
                                        Triple(historicalResult.room4, 4, record.tenant4),
                                        Triple(historicalResult.room5, 5, record.tenant5),
                                        Triple(historicalResult.room6, 6, record.tenant6)
                                    ).forEach { (roomRes, num, tenant) ->
                                        val billNum = "BILL-${record.billingMonth.replace(" ", "")}-R$num"
                                        val prevR = when (num) {
                                            1 -> record.prevR1; 2 -> record.prevR2; 3 -> record.prevR3; 4 -> record.prevR4; 5 -> record.prevR5; else -> record.prevR6
                                        }
                                        val currR = when (num) {
                                            1 -> record.currR1; 2 -> record.currR2; 3 -> record.currR3; 4 -> record.currR4; 5 -> record.currR5; else -> record.currR6
                                        }
                                        val uri = PdfGenerator.generateAndSaveRoomPdf(
                                            context, roomRes, num, prevR, currR, record.billingMonth, record.billingDate, historicalResult.input.rate, billNum
                                        )
                                        if (uri != null) count++
                                    }
                                    Toast.makeText(context, "Reproduced $count individual room PDFs in downloads archive!", Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Rooms Bills", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    val uri = PdfGenerator.generateAndSaveMeterRecordsPdf(
                                        context = context,
                                        record = record,
                                        month = record.billingMonth,
                                        date = record.billingDate
                                    )
                                    if (uri != null) {
                                        Toast.makeText(context, "Historical Meter Records PDF generated successfully!", Toast.LENGTH_SHORT).show()
                                        openPdf(context, uri)
                                    } else {
                                        Toast.makeText(context, "Error saving Record PDF.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                            ) {
                                Icon(imageVector = Icons.Default.Create, contentDescription = "Verification Photos", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Download Photo Records PDF", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                            }
                        }
                    }
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun TenantsTab(
    viewModel: BillingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val savedRecords by viewModel.savedRecords.collectAsState()
    var selectedDetailRoom by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tenants & Room Registry",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Manage room occupant names, base rents, sweeper schedules, and inspect historic lease records.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }

        // Room lists cards
        item {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (roomNum in 1..6) {
                    val tenantName = when (roomNum) {
                        1 -> viewModel.tenant1
                        2 -> viewModel.tenant2
                        3 -> viewModel.tenant3
                        4 -> viewModel.tenant4
                        5 -> viewModel.tenant5
                        else -> viewModel.tenant6
                    }
                    val isVacant = tenantName.isBlank() || tenantName.equals("vacant", ignoreCase = true)
                    val statusText = if (isVacant) "VACANT" else "OCCUPIED"
                    
                    val rentAmount = when (roomNum) {
                        1 -> viewModel.rent1; 2 -> viewModel.rent2; 3 -> viewModel.rent3; 4 -> viewModel.rent4; 5 -> viewModel.rent5; else -> viewModel.rent6
                    }
                    val sweeperAmount = when (roomNum) {
                        1 -> viewModel.sweeper1; 2 -> viewModel.sweeper2; 3 -> viewModel.sweeper3; 4 -> viewModel.sweeper4; 5 -> viewModel.sweeper5; else -> viewModel.sweeper6
                    }
                    val customAmount = when (roomNum) {
                        1 -> viewModel.custom1; 2 -> viewModel.custom2; 3 -> viewModel.custom3; 4 -> viewModel.custom4; 5 -> viewModel.custom5; else -> viewModel.custom6
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedDetailRoom = roomNum },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Room $roomNum",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Surface(
                                        color = if (isVacant) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primaryContainer,
                                        shape = RoundedCornerShape(100.dp)
                                    ) {
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            color = if (isVacant) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                                
                                Text(
                                    text = if (isVacant) "No occupant registered (Vacant)" else "Rented to: $tenantName",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isVacant) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(top = 4.dp)
                                ) {
                                    Text(
                                        text = "Rent: ₹${rentAmount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = "Sweeper: ₹${sweeperAmount}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    if ((customAmount.toDoubleOrNull() ?: 0.0) > 0.0) {
                                        Text(
                                            text = "Custom: ₹${customAmount}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "Inspect details",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // Real-time transfer events audit trail
        if (viewModel.transferEvents.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "ACTIVE ROOM REASSIGNMENT AUDIT TRAIL",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            viewModel.transferEvents.forEach { logMessage ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "info",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = logMessage,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Details alert dialog with Occupancy log dynamic audit check
    selectedDetailRoom?.let { roomNum ->
        val tenantName = when (roomNum) {
            1 -> viewModel.tenant1; 2 -> viewModel.tenant2; 3 -> viewModel.tenant3; 4 -> viewModel.tenant4; 5 -> viewModel.tenant5; else -> viewModel.tenant6
        }
        val isVacant = tenantName.isBlank() || tenantName.equals("vacant", ignoreCase = true)
        val rentAmount = when (roomNum) {
            1 -> viewModel.rent1; 2 -> viewModel.rent2; 3 -> viewModel.rent3; 4 -> viewModel.rent4; 5 -> viewModel.rent5; else -> viewModel.rent6
        }
        val sweeperAmount = when (roomNum) {
            1 -> viewModel.sweeper1; 2 -> viewModel.sweeper2; 3 -> viewModel.sweeper3; 4 -> viewModel.sweeper4; 5 -> viewModel.sweeper5; else -> viewModel.sweeper6
        }
        val customAmount = when (roomNum) {
            1 -> viewModel.custom1; 2 -> viewModel.custom2; 3 -> viewModel.custom3; 4 -> viewModel.custom4; 5 -> viewModel.custom5; else -> viewModel.custom6
        }

        // True Occupancy History Audit Log mapped dynamically from saved records!
        val historicLeases = savedRecords.mapNotNull { record ->
            val occupant = when (roomNum) {
                1 -> record.tenant1; 2 -> record.tenant2; 3 -> record.tenant3; 4 -> record.tenant4; 5 -> record.tenant5; else -> record.tenant6
            }
            if (occupant.isNotBlank()) {
                Triple(record.billingMonth, occupant, record.billingDate)
            } else null
        }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { selectedDetailRoom = null },
            confirmButton = {
                Button(onClick = { selectedDetailRoom = null }) {
                    Text("Close")
                }
            },
            title = {
                Text(
                    text = "Room $roomNum • Lease Dossier",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("ROOM STANDARD METADATA (EDITABLE)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            
                            OutlinedTextField(
                                value = tenantName,
                                onValueChange = { newValue ->
                                    when (roomNum) {
                                        1 -> viewModel.tenant1 = newValue
                                        2 -> viewModel.tenant2 = newValue
                                        3 -> viewModel.tenant3 = newValue
                                        4 -> viewModel.tenant4 = newValue
                                        5 -> viewModel.tenant5 = newValue
                                        else -> viewModel.tenant6 = newValue
                                    }
                                },
                                label = { Text("Active Tenant Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = rentAmount,
                                    onValueChange = { newValue ->
                                        when (roomNum) {
                                            1 -> viewModel.rent1 = newValue
                                            2 -> viewModel.rent2 = newValue
                                            3 -> viewModel.rent3 = newValue
                                            4 -> viewModel.rent4 = newValue
                                            5 -> viewModel.rent5 = newValue
                                            else -> viewModel.rent6 = newValue
                                        }
                                    },
                                    label = { Text("Rent (₹)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = sweeperAmount,
                                    onValueChange = { newValue ->
                                        when (roomNum) {
                                            1 -> viewModel.sweeper1 = newValue
                                            2 -> viewModel.sweeper2 = newValue
                                            3 -> viewModel.sweeper3 = newValue
                                            4 -> viewModel.sweeper4 = newValue
                                            5 -> viewModel.sweeper5 = newValue
                                            else -> viewModel.sweeper6 = newValue
                                        }
                                    },
                                    label = { Text("Sweeper (₹)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            
                            OutlinedTextField(
                                value = customAmount,
                                onValueChange = { newValue ->
                                    when (roomNum) {
                                        1 -> viewModel.custom1 = newValue
                                        2 -> viewModel.custom2 = newValue
                                        3 -> viewModel.custom3 = newValue
                                        4 -> viewModel.custom4 = newValue
                                        5 -> viewModel.custom5 = newValue
                                        else -> viewModel.custom6 = newValue
                                    }
                                },
                                label = { Text("Custom Fee (₹)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    if (!isVacant) {
                        var targetSelectedRoom by remember { mutableStateOf<Int?>(null) }
                        var showDropdown by remember { mutableStateOf(false) }

                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "TENANT TRANSFER SERVICE",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Move tenant '$tenantName' to another room. Current room configuration rents are preserved.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        OutlinedButton(
                                            onClick = { showDropdown = true },
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (targetSelectedRoom == null) "Select target room" else "To Room $targetSelectedRoom",
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        androidx.compose.material3.DropdownMenu(
                                            expanded = showDropdown,
                                            onDismissRequest = { showDropdown = false }
                                        ) {
                                            (1..6).filter { it != roomNum }.forEach { targetNum ->
                                                val targetTenant = when (targetNum) {
                                                    1 -> viewModel.tenant1; 2 -> viewModel.tenant2; 3 -> viewModel.tenant3
                                                    4 -> viewModel.tenant4; 5 -> viewModel.tenant5; else -> viewModel.tenant6
                                                }
                                                val targetLabel = if (targetTenant.isBlank() || targetTenant.equals("vacant", ignoreCase = true)) "VACANT" else targetTenant
                                                androidx.compose.material3.DropdownMenuItem(
                                                    text = { Text("Room $targetNum ($targetLabel)") },
                                                    onClick = {
                                                        targetSelectedRoom = targetNum
                                                        showDropdown = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            val tRoom = targetSelectedRoom
                                            if (tRoom != null) {
                                                val success = viewModel.transferTenant(roomNum, tRoom)
                                                if (success) {
                                                    Toast.makeText(context, "Completed transfer of '$tenantName' to Room $tRoom!", Toast.LENGTH_LONG).show()
                                                    selectedDetailRoom = null
                                                }
                                            } else {
                                                Toast.makeText(context, "Please choose a target room", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Text("Transfer", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "OCCUPANCY HISTORY LOG",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                        if (historicLeases.isEmpty()) {
                            Text(
                                text = "No historical billing records captured yet. Save a verified cycle to build history logs automatically.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(historicLeases) { lease ->
                                    val leaseTenant = if (lease.second.isBlank() || lease.second.equals("vacant", ignoreCase = true)) "VACANT" else lease.second
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = lease.first,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = "Audited: ${lease.third}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline
                                            )
                                        }
                                        Text(
                                            text = leaseTenant,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = if (leaseTenant == "VACANT") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun AuditStepCard(
    stepNum: Int,
    title: String,
    formula: String,
    calculation: String,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(MaterialTheme.colorScheme.primary, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stepNum.toString(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formula,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Info,
                contentDescription = if (expanded) "Collapse detail" else "Expand detail",
                tint = MaterialTheme.colorScheme.outline
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(modifier = Modifier.padding(top = 12.dp, start = 38.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = calculation,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun UnitTableHeader() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Unit Meter",
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = "Original Diff",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Final Updated Diff",
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun UnitTableRow(label: String, orig: Int, final: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$orig units",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        Text(
            text = "$final units",
            modifier = Modifier.weight(1.4f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun RoomCompactInvoice(
    roomNum: Int,
    res: RoomBillingResult,
    rate: Double,
    date: String,
    onCopyTemplate: () -> Unit,
    onDownloadPdf: () -> Unit,
    onSharePdf: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    val tenantLabel = if (res.tenantName.isBlank() || res.tenantName.equals("vacant", ignoreCase = true)) "VACANT" else res.tenantName
                    Text(
                        text = "ROOM $roomNum INVOICE (Tenant: $tenantLabel)",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Due Statement ($date)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCopyTemplate,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Copy text summary",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    IconButton(
                        onClick = onSharePdf,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share invoice PDF",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Gross Room Rent", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "₹${res.rent.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(text = "Electricity Charge", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = "(${res.updatedUnits} units × ₹${rate.toInt()}/unit)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Text(text = "₹${res.electricityCharge.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Sweeper Charge", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "₹${res.sweeper.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
            }

            if (res.custom > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Custom Charge", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(text = "₹${res.custom.toInt()}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Units breakdown:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (roomNum <= 3) {
                            "Base ${res.originalDiff} + Disc ${res.sharedDiscrepancy} + Mot ${res.sharedMotor} = ${res.updatedUnits} un"
                        } else {
                            "Base ${res.originalDiff} + Mot ${res.sharedMotor} = ${res.updatedUnits} un"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "TOTAL DUE", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Text(text = "Status: UNPAID / DUE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onDownloadPdf,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.padding(vertical = 2.dp)
                    ) {
                        Text("Download PDF", style = MaterialTheme.typography.bodySmall)
                    }

                    Text(text = "₹${res.totalBill.toInt()}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
