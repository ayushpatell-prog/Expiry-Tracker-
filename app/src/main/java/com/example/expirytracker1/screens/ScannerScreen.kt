package com.example.expirytracker1.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.expirytracker1.data.PantryItem
import com.example.expirytracker1.scanner.BarcodeAnalyzer
import com.example.expirytracker1.ui.theme.ExpiryTracker1Theme
import com.example.expirytracker1.viewmodel.ProductViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ProductViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scannedProduct by viewModel.scannedProduct.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    
    // Store temporarily parsed GS1 expiry
    var gs1ExpiryDate by remember { mutableStateOf<String?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let {
                val inputImage = com.google.mlkit.vision.common.InputImage.fromFilePath(context, it)
                val analyzer = BarcodeAnalyzer { barcode, expiry ->
                    if (expiry != null) gs1ExpiryDate = expiry
                    if (barcode != null) viewModel.scanBarcode(barcode)
                }
                analyzer.analyzeImage(inputImage)
            }
        }
    )

    var showBottomSheet by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
            if (!granted) {
                Toast.makeText(context, "Camera permission is required.", Toast.LENGTH_LONG).show()
            }
        }
    )

    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    LaunchedEffect(scannedProduct) {
        if (scannedProduct != null) {
            showBottomSheet = true
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                onBarcodeDetected = { barcode, expiry ->
                    if (!isScanning && !showBottomSheet) {
                        if (expiry != null && gs1ExpiryDate != expiry) {
                            gs1ExpiryDate = expiry
                        }
                        if (barcode != null) {
                            viewModel.scanBarcode(barcode)
                        }
                    }
                },
                onCameraControlReady = { cameraControl = it }
            )
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Camera Permission Required",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Button(onClick = { launcher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.padding(top = 16.dp)) {
                        Text("Grant Permission")
                    }
                }
            }
        }

        ScannerOverlay(
            onNavigateBack = onNavigateBack,
            onManualClick = { showBottomSheet = true },
            onGalleryClick = { galleryLauncher.launch("image/*") },
            onFlashClick = {
                isFlashOn = !isFlashOn
                cameraControl?.enableTorch(isFlashOn)
            },
            isFlashOn = isFlashOn
        )

        if (isScanning) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { 
                    showBottomSheet = false
                    viewModel.clearScannedProduct()
                },
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                ProductEntryContent(
                    productName = scannedProduct?.product_name ?: "",
                    brand = scannedProduct?.brands ?: "",
                    imageUrl = scannedProduct?.image_url ?: "",
                    barcode = scannedProduct?.code ?: "",
                    category = scannedProduct?.categories?.split(",")?.firstOrNull() ?: "Others",
                    preFillExpiry = gs1ExpiryDate,
                    onSave = { newItem ->
                        viewModel.addProduct(newItem)
                        showBottomSheet = false
                        viewModel.clearScannedProduct()
                        gs1ExpiryDate = null
                        scope.launch {
                            Toast.makeText(context, "✓ Product Added Successfully", Toast.LENGTH_SHORT).show()
                            delay(500)
                            onNavigateBack()
                        }
                    },
                    onCancel = { 
                        showBottomSheet = false
                        viewModel.clearScannedProduct()
                        gs1ExpiryDate = null
                    }
                )
            }
        }
    }
}

@ComposePreview(showBackground = true)
@Composable
fun ScannerScreenPreview() {
    ExpiryTracker1Theme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) {
            ScannerOverlay(
                onNavigateBack = {},
                onManualClick = {},
                onGalleryClick = {},
                onFlashClick = {},
                isFlashOn = false
            )
        }
    }
}

@ComposePreview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun ScannerScreenDarkPreview() {
    ExpiryTracker1Theme(darkTheme = true) {
        Box(modifier = Modifier.fillMaxSize().background(Color.DarkGray)) {
            ScannerOverlay(
                onNavigateBack = {},
                onManualClick = {},
                onGalleryClick = {},
                onFlashClick = {},
                isFlashOn = true
            )
        }
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String?, String?) -> Unit,
    onCameraControlReady: (CameraControl) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(executor, BarcodeAnalyzer { barcode, expiry ->
                            onBarcodeDetected(barcode, expiry)
                        })
                    }

                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                    .build()

                try {
                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    onCameraControlReady(camera.cameraControl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

@Composable
fun ScannerOverlay(
    onNavigateBack: () -> Unit,
    onManualClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFlashClick: () -> Unit,
    isFlashOn: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 48.dp, start = 20.dp, end = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(width = 280.dp, height = 280.dp)
                    .border(width = 1.dp, color = Color.White.copy(alpha = 0.3f), shape = RoundedCornerShape(24.dp))
            ) {
                val cornerSize = 40.dp
                val thickness = 4.dp
                Box(modifier = Modifier.size(cornerSize).align(Alignment.TopStart).border(thickness, primaryColor, RoundedCornerShape(topStart = 24.dp)))
                Box(modifier = Modifier.size(cornerSize).align(Alignment.TopEnd).border(thickness, primaryColor, RoundedCornerShape(topEnd = 24.dp)))
                Box(modifier = Modifier.size(cornerSize).align(Alignment.BottomStart).border(thickness, primaryColor, RoundedCornerShape(bottomStart = 24.dp)))
                Box(modifier = Modifier.size(cornerSize).align(Alignment.BottomEnd).border(thickness, primaryColor, RoundedCornerShape(bottomEnd = 24.dp)))
            }
            
            Surface(
                modifier = Modifier.padding(top = 350.dp).clip(RoundedCornerShape(24.dp)),
                color = Color.Black.copy(alpha = 0.5f)
            ) {
                Text(
                    text = "Align barcode within the frame",
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Bottom Controls Container
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Gallery and Flash Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Gallery Button
                IconButton(
                    onClick = onGalleryClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color.White)
                }

                // Flash Button
                IconButton(
                    onClick = onFlashClick,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(if (isFlashOn) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f))
                ) {
                    Icon(
                        if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "Flash",
                        tint = if (isFlashOn) Color.Black else Color.White
                    )
                }
            }

            // Manual Entry Button
            Surface(
                onClick = { onManualClick() },
                color = Color.Black.copy(alpha = 0.5f),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = Color.White)
                    Text("Manual Entry", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEntryContent(
    productName: String,
    brand: String,
    imageUrl: String,
    barcode: String,
    category: String,
    preFillExpiry: String? = null,
    onSave: (PantryItem) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(productName) }
    var itemBrand by remember { mutableStateOf(brand) }
    var itemCategory by remember { mutableStateOf(category) }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Pcs") }
    var purchaseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    // Support multiple incoming formats from OCR/GS1
    val incomingDateFormats = listOf(
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()),
        SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()),
        SimpleDateFormat("MM/yyyy", Locale.getDefault()),
        SimpleDateFormat("dd/MM/yy", Locale.getDefault())
    )

    var expiryDate by remember { 
        var parsedTime: Long? = null
        if (!preFillExpiry.isNullOrBlank()) {
            for (format in incomingDateFormats) {
                try {
                    parsedTime = format.parse(preFillExpiry)?.time
                    if (parsedTime != null) break
                } catch (e: Exception) {
                    // Continue to next format
                }
            }
        }
        mutableLongStateOf(parsedTime ?: (System.currentTimeMillis() + 86400000 * 7))
    }
    
    var reminder by remember { mutableStateOf("1 Day Before") }
    var notes by remember { mutableStateOf("") }

    val datePickerState = rememberDatePickerState()
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Product Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (imageUrl.isNotBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name*") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        OutlinedTextField(
            value = itemBrand,
            onValueChange = { itemBrand = it },
            label = { Text("Brand") },
            modifier = Modifier.fillMaxWidth()
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = quantity,
                onValueChange = { quantity = it },
                label = { Text("Quantity*") },
                modifier = Modifier.weight(1f)
            )
            var showUnitDropdown by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = unit,
                    onValueChange = {},
                    label = { Text("Unit") },
                    readOnly = true,
                    trailingIcon = { IconButton(onClick = { showUnitDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                    modifier = Modifier.fillMaxWidth()
                )
                DropdownMenu(expanded = showUnitDropdown, onDismissRequest = { showUnitDropdown = false }) {
                    listOf("Pcs", "Kg", "Gm", "Ltr", "Ml", "Pack").forEach { u ->
                        DropdownMenuItem(text = { Text(u) }, onClick = { unit = u; showUnitDropdown = false })
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = dateFormat.format(Date(purchaseDate)),
                onValueChange = {},
                label = { Text("Purchase Date") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                trailingIcon = { IconButton(onClick = { showDatePickerFor = "purchase" }) { Icon(Icons.Default.CalendarToday, null) } }
            )
            OutlinedTextField(
                value = dateFormat.format(Date(expiryDate)),
                onValueChange = {},
                label = { Text("Expiry Date*") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                trailingIcon = { IconButton(onClick = { showDatePickerFor = "expiry" }) { Icon(Icons.Default.CalendarToday, null) } }
            )
        }

        var showRemDropdown by remember { mutableStateOf(false) }
        Box {
            OutlinedTextField(
                value = reminder,
                onValueChange = {},
                label = { Text("Reminder") },
                readOnly = true,
                trailingIcon = { IconButton(onClick = { showRemDropdown = true }) { Icon(Icons.Default.ArrowDropDown, null) } },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = showRemDropdown, onDismissRequest = { showRemDropdown = false }) {
                listOf("On Expiry Day", "1 Day Before", "3 Days Before", "7 Days Before").forEach { r ->
                    DropdownMenuItem(text = { Text(r) }, onClick = { reminder = r; showRemDropdown = false })
                }
            }
        }

        OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            label = { Text("Notes") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 2
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onCancel) { Text("Cancel") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && quantity.isNotBlank()) {
                        onSave(PantryItem(
                            name = name,
                            brand = itemBrand,
                            category = itemCategory,
                            quantity = "$quantity $unit",
                            barcode = barcode,
                            imageUrl = imageUrl,
                            purchaseDate = dateFormat.format(Date(purchaseDate)),
                            expiryDate = dateFormat.format(Date(expiryDate)),
                            expiryTimestamp = expiryDate,
                            reminder = reminder,
                            notes = notes
                        ))
                    }
                },
                enabled = name.isNotBlank() && quantity.isNotBlank()
            ) {
                Text("Save Product")
            }
        }
    }

    if (showDatePickerFor != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        if (showDatePickerFor == "purchase") purchaseDate = it
                        else expiryDate = it
                    }
                    showDatePickerFor = null
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePickerFor = null }) { Text("Cancel") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
