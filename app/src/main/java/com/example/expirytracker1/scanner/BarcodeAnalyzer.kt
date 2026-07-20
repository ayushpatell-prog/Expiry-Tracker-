package com.example.expirytracker1.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.*
import java.util.regex.Pattern

class BarcodeAnalyzer(
    private val onDetected: (String?, String?) -> Unit
) : ImageAnalysis.Analyzer {

    var isBarcodeScanningEnabled = true
    
    // For single frame capture
    var isCaptureRequested = false
    var onImageCaptured: ((Bitmap) -> Unit)? = null
    var onOcrResult: ((String?) -> Unit)? = null

    private var lastBarcode: String? = null
    private var lastBarcodeTime = 0L

    private val barcodeScanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13, Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A, Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_QR_CODE, Barcode.FORMAT_CODE_128
            )
            .build()
    )
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Improved date pattern to be more flexible
    private val datePattern = Pattern.compile(
        "(?i)\\b(?:exp|expiry|best before|best by|use by|bb)?[:\\s]*" +
        "(\\d{1,4})[./-](\\d{1,2})[./-](\\d{1,4})\\b|" +
        "\\b(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*[\\s,]*(\\d{2,4})\\b|" +
        "\\b(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val expiryKeywords = listOf("EXP", "EXPIRY", "BEST BEFORE", "BEST BY", "USE BY", "CONSUME BEFORE", "EXPIRES", "BB")
    private val mfgKeywords = listOf("PKD", "PACKED", "PACK DATE", "MFD", "MFG", "MANUFACTURED", "PRODUCTION", "MANUFACTURING", "BATCH", "MRP")

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        if (isBarcodeScanningEnabled) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            barcode.rawValue?.let { value ->
                                val currentTime = System.currentTimeMillis()
                                if (value != lastBarcode || currentTime - lastBarcodeTime > 2000) {
                                    lastBarcode = value
                                    lastBarcodeTime = currentTime
                                    Log.d("BarcodeAnalyzer", "Barcode detected: $value")
                                    onDetected(value, null)
                                }
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        } else if (isCaptureRequested) {
            isCaptureRequested = false
            Log.d("BarcodeAnalyzer", "Capture requested. Rotation: $rotationDegrees")
            
            // Get bitmap from ImageProxy
            val bitmap = imageProxy.toBitmap()
            
            // Rotate bitmap based on CameraX rotation
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            
            // ROI Crop - Exact match for the 220x70dp window
            val frameWidth = rotatedBitmap.width
            val frameHeight = rotatedBitmap.height
            
            val cropWidth = (frameWidth * 0.7).toInt().coerceAtMost(frameWidth)
            val cropHeight = (frameHeight * 0.2).toInt().coerceAtMost(frameHeight)
            val left = (frameWidth - cropWidth) / 2
            val top = (frameHeight - cropHeight) / 2
            
            try {
                val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, left, top, cropWidth, cropHeight)
                Log.d("BarcodeAnalyzer", "Image cropped successfully. Size: ${croppedBitmap.width}x${croppedBitmap.height}")
                onImageCaptured?.invoke(croppedBitmap)
            } catch (e: Exception) {
                Log.e("BarcodeAnalyzer", "Crop failed: ${e.message}")
                onImageCaptured?.invoke(rotatedBitmap)
            } finally {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    fun performOcrOnBitmap(bitmap: Bitmap, onComplete: (String?) -> Unit) {
        Log.d("BarcodeAnalyzer", "Performing OCR on bitmap...")
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                Log.d("BarcodeAnalyzer", "OCR Result text: ${visionText.text}")
                val detectedDate = processOcrText(visionText.text)
                Log.d("BarcodeAnalyzer", "Detected Date: $detectedDate")
                onComplete(detectedDate)
            }
            .addOnFailureListener { e ->
                Log.e("BarcodeAnalyzer", "OCR failed: ${e.message}")
                onComplete(null)
            }
    }

    private fun processOcrText(fullText: String): String? {
        val lines = fullText.split("\n")
        val candidates = mutableListOf<Pair<String, Int>>() // Date, Score

        lines.forEach { line ->
            val cleanLine = line.uppercase().trim()
            val matcher = datePattern.matcher(cleanLine)
            while (matcher.find()) {
                val dateStr = matcher.group().trim()
                var score = 10 // Base score

                // Check for keywords in the same line
                if (expiryKeywords.any { cleanLine.contains(it) }) {
                    score += 100
                }

                if (mfgKeywords.any { cleanLine.contains(it) }) {
                    score -= 500
                }

                val normalized = normalizeAndFormatDate(dateStr)
                if (normalized != dateStr || dateStr.length >= 6) {
                    candidates.add(normalized to score)
                }
            }
        }

        // Also search in the full text if no candidates found in lines
        if (candidates.isEmpty()) {
            val matcher = datePattern.matcher(fullText.uppercase())
            while (matcher.find()) {
                candidates.add(normalizeAndFormatDate(matcher.group().trim()) to 5)
            }
        }

        return candidates.filter { it.second > 0 }.maxByOrNull { it.second }?.first
    }

    private fun normalizeAndFormatDate(dateStr: String): String {
        // Strip out keywords from the string itself
        var cleanDate = dateStr.uppercase()
        expiryKeywords.forEach { cleanDate = cleanDate.replace(it, "") }
        cleanDate = cleanDate.replace(":", "").replace(" ", "").trim()

        // Handle DD MMM YYYY
        if (dateStr.any { it.isLetter() }) {
            try {
                val sdfInput = java.text.SimpleDateFormat("dd MMM yyyy", Locale.US)
                val date = sdfInput.parse(dateStr)
                if (date != null) {
                    return java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {}
        }

        // Try to find digits and separators
        val match = Regex("(\\d{1,4})[./-](\\d{1,2})[./-](\\d{1,4})").find(cleanDate)
        if (match != null) {
            val groups = match.groupValues
            var day = ""
            var month = ""
            var year = ""

            if (groups[1].length == 4) {
                // YYYY-MM-DD
                year = groups[1]
                month = groups[2]
                day = groups[3]
            } else {
                // DD/MM/YYYY or DD/MM/YY
                day = groups[1]
                month = groups[2]
                year = if (groups[3].length == 2) "20${groups[3]}" else groups[3]
            }

            try {
                val numericSdf = java.text.SimpleDateFormat("d/M/yyyy", Locale.US)
                val date = numericSdf.parse("$day/$month/$year")
                if (date != null) {
                    return java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {}
        }
        
        return dateStr
    }

    fun analyzeImage(image: InputImage, onComplete: (String?) -> Unit) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedDate = processOcrText(visionText.text)
                onComplete(detectedDate)
            }
            .addOnFailureListener { onComplete(null) }
    }
}
