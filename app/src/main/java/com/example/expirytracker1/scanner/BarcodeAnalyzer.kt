package com.example.expirytracker1.scanner

import android.graphics.Bitmap
import android.graphics.Matrix
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

    private val datePattern = Pattern.compile(
        "\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})\\b|" +
        "\\b(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})\\b|" +
        "\\b(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*\\s+(\\d{2,4})\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val expiryKeywords = listOf("EXP", "EXPIRY", "BEST BEFORE", "BEST BY", "USE BY", "CONSUME BEFORE", "EXPIRES")
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
                        for (barcode in barcodes) {
                            barcode.rawValue?.let { value ->
                                onDetected(value, null)
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
                imageProxy.close()
            }
        } else if (isCaptureRequested) {
            isCaptureRequested = false
            
            // Get bitmap from ImageProxy
            val bitmap = imageProxy.toBitmap()
            
            // Rotate bitmap based on CameraX rotation
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            
            // User wants to freeze the frame and then crop. 
            // Step 4: Crop ONLY the transparent rectangle automatically.
            // ROI Crop - Centered slot (matches the 220x70dp UI box approximately)
            val cropWidth = (rotatedBitmap.width * 0.6).toInt()
            val cropHeight = (rotatedBitmap.height * 0.15).toInt()
            val left = (rotatedBitmap.width - cropWidth) / 2
            val top = (rotatedBitmap.height - cropHeight) / 2
            
            try {
                val croppedBitmap = Bitmap.createBitmap(rotatedBitmap, left, top, cropWidth, cropHeight)
                onImageCaptured?.invoke(croppedBitmap)
            } catch (e: Exception) {
                onImageCaptured?.invoke(rotatedBitmap)
            } finally {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    fun performOcrOnBitmap(bitmap: Bitmap, onComplete: (String?) -> Unit) {
        val image = InputImage.fromBitmap(bitmap, 0)
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                onComplete(processOcrText(visionText.text))
            }
            .addOnFailureListener {
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
                val dateStr = matcher.group()
                var score = 10 // Base score

                if (expiryKeywords.any { cleanLine.contains(it) }) {
                    score += 100
                }

                if (mfgKeywords.any { cleanLine.contains(it) }) {
                    score -= 500
                }

                candidates.add(normalizeAndFormatDate(dateStr) to score)
            }
        }

        return candidates.filter { it.second > 0 }.maxByOrNull { it.second }?.first
    }

    private fun normalizeAndFormatDate(dateStr: String): String {
        if (dateStr.any { it.isLetter() }) {
            try {
                val sdfInput = java.text.SimpleDateFormat("dd MMM yyyy", Locale.US)
                val date = sdfInput.parse(dateStr)
                if (date != null) {
                    return java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {}
        }

        val match = Regex("(\\d{1,2}|\\d{4})([./-])(\\d{1,2})([./-])(\\d{2,4})").find(dateStr)
        if (match != null) {
            val groups = match.groupValues
            var day = groups[1]
            var month = groups[3]
            var year = groups[5]

            if (groups[1].length == 4) {
                year = groups[1]
                month = groups[3]
                day = groups[5]
            } else {
                year = if (groups[5].length == 2) "20${groups[5]}" else groups[5]
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

    fun analyzeImage(image: InputImage, onComplete: () -> Unit = {}) {
        textRecognizer.process(image)
            .addOnSuccessListener { visionText ->
                val detectedDate = processOcrText(visionText.text)
                onOcrResult?.invoke(detectedDate)
            }
            .addOnCompleteListener { onComplete() }
    }
}
