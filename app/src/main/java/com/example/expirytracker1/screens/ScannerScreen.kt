package com.example.expirytracker1.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

enum class ScanStep {
    WAITING_FOR_BARCODE,
    WAITING_FOR_EXPIRY,
    CROP_PREVIEW,
    EXPIRY_FAILED,
    CONFIRMATION
}

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
    
    var scanStep by remember { mutableStateOf(ScanStep.WAITING_FOR_BARCODE) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var detectedExpiryDate by remember { mutableStateOf<String?>(null) }
    var isProcessingOcr by remember { mutableStateOf(false) }
    
    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    
    val analyzer = remember { 
        BarcodeAnalyzer { barcode, _ ->
            if (scanStep == ScanStep.WAITING_FOR_BARCODE && barcode != null) {
                viewModel.scanBarcode(barcode)
            }
        }
    }

    LaunchedEffect(scanStep) {
        analyzer.isBarcodeScanningEnabled = (scanStep == ScanStep.WAITING_FOR_BARCODE)
    }

    LaunchedEffect(scannedProduct) {
        if (scannedProduct != null && scanStep == ScanStep.WAITING_FOR_BARCODE) {
            vibrate(context)
            scanStep = ScanStep.WAITING_FOR_EXPIRY
        }
    }

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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission && scanStep != ScanStep.CROP_PREVIEW) {
            CameraPreview(
                modifier = Modifier.fillMaxSize(),
                analyzer = analyzer,
                onCameraControlReady = { cameraControl = it }
            )
        }

        if (scanStep != ScanStep.CROP_PREVIEW) {
            ScannerOverlay(
                scanStep = scanStep,
                productName = scannedProduct?.product_name,
                onNavigateBack = onNavigateBack,
                onCaptureExpiry = {
                    analyzer.onImageCaptured = { bitmap ->
                        capturedBitmap = bitmap
                        scanStep = ScanStep.CROP_PREVIEW
                    }
                    analyzer.isCaptureRequested = true
                },
                onFlashClick = {
                    isFlashOn = !isFlashOn
                    cameraControl?.enableTorch(isFlashOn)
                },
                isFlashOn = isFlashOn
            )
        }

        if (scanStep == ScanStep.CROP_PREVIEW && capturedBitmap != null) {
            CropPreviewScreen(
                bitmap = capturedBitmap!!,
                onRetake = { 
                    scanStep = ScanStep.WAITING_FOR_EXPIRY 
                },
                onContinue = { adjustedBitmap ->
                    isProcessingOcr = true
                    analyzer.performOcrOnBitmap(adjustedBitmap) { result ->
                        isProcessingOcr = false
                        if (result != null) {
                            detectedExpiryDate = result
                            scanStep = ScanStep.CONFIRMATION
                            vibrate(context)
                        } else {
                            scanStep = ScanStep.EXPIRY_FAILED
                        }
                    }
                }
            )
        }

        if (isScanning || isProcessingOcr) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }

        if (scanStep == ScanStep.CONFIRMATION) {
            ModalBottomSheet(
                onDismissRequest = { 
                    scanStep = ScanStep.WAITING_FOR_EXPIRY
                    detectedExpiryDate = null
                },
                modifier = Modifier.fillMaxHeight(0.9f)
            ) {
                ProductEntryContent(
                    productName = scannedProduct?.product_name ?: "",
                    brand = scannedProduct?.brands ?: "",
                    imageUrl = scannedProduct?.image_url ?: "",
                    barcode = scannedProduct?.code ?: "",
                    category = scannedProduct?.categories?.split(",")?.firstOrNull() ?: "Others",
                    preFillExpiry = detectedExpiryDate,
                    onSave = { newItem ->
                        viewModel.addProduct(newItem)
                        viewModel.clearScannedProduct()
                        scope.launch {
                            Toast.makeText(context, "✓ Product Added Successfully", Toast.LENGTH_SHORT).show()
                            delay(500)
                            onNavigateBack()
                        }
                    },
                    onCancel = { 
                        scanStep = ScanStep.WAITING_FOR_EXPIRY
                        detectedExpiryDate = null
                    },
                    onRetake = {
                        scanStep = ScanStep.WAITING_FOR_EXPIRY
                        detectedExpiryDate = null
                    }
                )
            }
        }

        if (scanStep == ScanStep.EXPIRY_FAILED) {
            ModalBottomSheet(
                onDismissRequest = { scanStep = ScanStep.WAITING_FOR_EXPIRY }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(48.dp))
                    Text("Couldn't detect expiry date", style = MaterialTheme.typography.titleLarge)
                    Text("Please try again or enter the date manually.", textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Button(
                        onClick = { scanStep = ScanStep.WAITING_FOR_EXPIRY },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Retake")
                    }
                    
                    OutlinedButton(
                        onClick = { scanStep = ScanStep.CONFIRMATION },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.EditCalendar, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Enter Manually")
                    }
                }
            }
        }
    }
}

@Composable
fun CropPreviewScreen(
    bitmap: Bitmap,
    onRetake: () -> Unit,
    onContinue: (Bitmap) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
        scale *= zoomChange
        offset += offsetChange
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clipToBounds(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .transformable(state = state)
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                contentScale = ContentScale.Fit
            )
            
            // Fixed Crop Frame for reference
            Box(
                modifier = Modifier
                    .size(width = 220.dp, height = 70.dp)
                    .border(2.dp, Color.White, RoundedCornerShape(12.dp))
            )
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f)) {
                    Text("Retake")
                }
                Spacer(Modifier.width(16.dp))
                Button(
                    onClick = { 
                        // Simplified continue: just use current bitmap. 
                        // In a full implementation, we'd apply transform to crop.
                        onContinue(bitmap) 
                    }, 
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Continue")
                }
            }
        }
    }
}

fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(100)
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    analyzer: BarcodeAnalyzer,
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
                        it.setAnalyzer(executor, analyzer)
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
    scanStep: ScanStep,
    productName: String?,
    onNavigateBack: () -> Unit,
    onCaptureExpiry: () -> Unit,
    onFlashClick: () -> Unit,
    isFlashOn: Boolean
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            
            drawRect(color = Color.Black.copy(alpha = if (scanStep == ScanStep.WAITING_FOR_BARCODE) 0.4f else 0.75f))
            
            val boxWidth = 220.dp.toPx()
            val boxHeight = if (scanStep == ScanStep.WAITING_FOR_BARCODE) 220.dp.toPx() else 70.dp.toPx()
            val left = (canvasWidth - boxWidth) / 2
            val top = (canvasHeight - boxHeight) / 2
            
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(boxWidth, boxHeight),
                cornerRadius = CornerRadius(16.dp.toPx()),
                blendMode = BlendMode.Clear
            )
        }

        val infiniteTransition = rememberInfiniteTransition(label = "border")
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            val boxWidth = 220.dp
            val boxHeight = if (scanStep == ScanStep.WAITING_FOR_BARCODE) 220.dp else 70.dp
            
            Box(
                modifier = Modifier
                    .size(width = boxWidth, height = boxHeight)
                    .border(
                        width = 2.dp,
                        color = if (scanStep == ScanStep.CONFIRMATION) Color(0xFF4CAF50) else primaryColor.copy(alpha = alpha),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }

        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 48.dp, start = 20.dp)
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.3f))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
        }

        AnimatedVisibility(
            visible = scanStep != ScanStep.WAITING_FOR_BARCODE,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 60.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50), modifier = Modifier.size(32.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("✓ Barcode Detected", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text("Product: ${productName ?: "Loading..."}", style = MaterialTheme.typography.bodySmall, maxLines = 1)
                    }
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))
            
            if (scanStep == ScanStep.WAITING_FOR_EXPIRY) {
                Text(
                    text = "Move the expiry date inside the box.",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            } else if (scanStep == ScanStep.WAITING_FOR_BARCODE) {
                Text(
                    text = "Align barcode within the frame",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Spacer(modifier = Modifier.height(if (scanStep == ScanStep.WAITING_FOR_BARCODE) 260.dp else 120.dp))
            
            if (scanStep == ScanStep.WAITING_FOR_EXPIRY) {
                Text(
                    text = "Examples:\n• EXP  • Expiry Date\n• Best Before  • Use By",
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                LargeFloatingActionButton(
                    onClick = onCaptureExpiry,
                    modifier = Modifier.padding(bottom = 32.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text("Capture Expiry", fontWeight = FontWeight.Bold)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(60.dp))
        }

        IconButton(
            onClick = onFlashClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 60.dp, end = 40.dp)
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
    onCancel: () -> Unit,
    onRetake: () -> Unit
) {
    val context = LocalContext.current
    var name by remember { mutableStateOf(productName) }
    var itemBrand by remember { mutableStateOf(brand) }
    var itemCategory by remember { mutableStateOf(category) }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("Pcs") }
    var purchaseDate by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    
    val incomingDateFormats = listOf(
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()),
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
                } catch (e: Exception) { }
            }
        }
        mutableLongStateOf(parsedTime ?: (System.currentTimeMillis() + 86400000 * 7))
    }
    
    var reminder by remember { mutableStateOf("1 Day Before") }
    var notes by remember { mutableStateOf("") }
    var itemImageUrl by remember { mutableStateOf(imageUrl) }
    var capturedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPhotoOptions by remember { mutableStateOf(false) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            uri?.let { itemImageUrl = it.toString(); capturedBitmap = null }
        }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            bitmap?.let { capturedBitmap = it; itemImageUrl = "" }
        }
    )

    val datePickerState = rememberDatePickerState()
    var showDatePickerFor by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(Modifier.width(8.dp))
            Text("Barcode Scanned", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
            Spacer(Modifier.width(16.dp))
            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF4CAF50))
            Spacer(Modifier.width(8.dp))
            Text("Expiry Detected", style = MaterialTheme.typography.labelMedium, color = Color(0xFF4CAF50))
        }

        Text("Confirm Product Details", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showPhotoOptions = true },
                contentAlignment = Alignment.Center
            ) {
                if (capturedBitmap != null) {
                    Image(
                        bitmap = capturedBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (itemImageUrl.isNotBlank()) {
                    AsyncImage(
                        model = itemImageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Photo", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                    }
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

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = itemBrand,
                onValueChange = { itemBrand = it },
                label = { Text("Brand") },
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = barcode,
                onValueChange = {},
                label = { Text("Barcode") },
                readOnly = true,
                modifier = Modifier.weight(1f)
            )
        }

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
                value = dateFormat.format(Date(expiryDate)),
                onValueChange = {},
                label = { Text("Detected Expiry*") },
                readOnly = true,
                modifier = Modifier.weight(1f),
                trailingIcon = { IconButton(onClick = { showDatePickerFor = "expiry" }) { Icon(Icons.Default.EditCalendar, null) } }
            )
            var showRemDropdown by remember { mutableStateOf(false) }
            Box(modifier = Modifier.weight(1f)) {
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
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f)
            ) {
                Text("Retake Photo")
            }
            Button(
                onClick = {
                    if (name.isNotBlank() && quantity.isNotBlank()) {
                        val diff = expiryDate - System.currentTimeMillis()
                        val daysLeft = (diff / (1000 * 60 * 60 * 24)).toInt().coerceAtLeast(0)
                        
                        var finalImageUrl = itemImageUrl
                        if (capturedBitmap != null) {
                            finalImageUrl = saveBitmapToLocalFile(context, capturedBitmap!!) ?: ""
                        }

                        onSave(PantryItem(
                            name = name,
                            brand = itemBrand,
                            category = itemCategory,
                            quantity = "$quantity $unit",
                            barcode = barcode,
                            imageUrl = finalImageUrl,
                            purchaseDate = dateFormat.format(Date(purchaseDate)),
                            expiryDate = dateFormat.format(Date(expiryDate)),
                            expiryTimestamp = expiryDate,
                            reminder = reminder,
                            notes = notes
                        ))
                    }
                },
                enabled = name.isNotBlank() && quantity.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Confirm & Save")
            }
        }
        
        TextButton(
            onClick = onCancel,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Cancel")
        }
    }

    if (showPhotoOptions) {
        ModalBottomSheet(onDismissRequest = { showPhotoOptions = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                Text("Select Photo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                ListItem(
                    headlineContent = { Text("Click Photo") },
                    leadingContent = { Icon(Icons.Default.CameraAlt, null) },
                    modifier = Modifier.clickable { cameraLauncher.launch(null); showPhotoOptions = false }
                )
                ListItem(
                    headlineContent = { Text("Choose from Gallery") },
                    leadingContent = { Icon(Icons.Default.PhotoLibrary, null) },
                    modifier = Modifier.clickable { galleryLauncher.launch("image/*"); showPhotoOptions = false }
                )
            }
        }
    }

    if (showDatePickerFor != null) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerFor = null },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        expiryDate = it
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

private fun saveBitmapToLocalFile(context: Context, bitmap: Bitmap): String? {
    return try {
        val filename = "product_${System.currentTimeMillis()}.jpg"
        val file = File(context.filesDir, filename)
        val out = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        out.flush()
        out.close()
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@ComposePreview(showBackground = true)
@Composable
fun ScannerScreenPreview() {
    ExpiryTracker1Theme(darkTheme = false) {
        Box(modifier = Modifier.fillMaxSize().background(Color.Gray)) {
            ScannerOverlay(
                scanStep = ScanStep.WAITING_FOR_BARCODE,
                productName = null,
                onNavigateBack = {},
                onCaptureExpiry = {},
                onFlashClick = {},
                isFlashOn = false
            )
        }
    }
}
