package com.example.expirytracker1.scanner

import android.graphics.Bitmap
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
    
    // Callbacks for UI
    var onImageCaptured: ((Bitmap) -> Unit)? = null

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

    // Regex for various date formats
    private val datePattern = Pattern.compile(
        "\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})\\b|" +
        "\\b(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})\\b|" +
        "\\b(\\d{1,2})\\s+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*[\\s,]*(\\d{2,4})\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val expiryKeywords = listOf("EXP", "EXPIRY", "BEST BEFORE", "BEST BY", "USE BY", "CONSUME BEFORE", "EXPIRES", "BB")
    private val mfgKeywords = listOf("PKD", "PACKED", "PACK DATE", "MFD", "MFG", "MANUFACTURED", "PRODUCTION", "MANUFACTURING", "BATCH", "MRP")

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isBarcodeScanningEnabled) {
            val mediaImage = imageProxy.image
            if (mediaImage != null) {
                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                barcodeScanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        if (barcodes.isNotEmpty()) {
                            val barcode = barcodes[0]
                            barcode.rawValue?.let { value ->
                                val currentTime = System.currentTimeMillis()
                                if (value != lastBarcode || currentTime - lastBarcodeTime > 2000) {
                                    lastBarcode = value
                                    lastBarcodeTime = currentTime
                                    onDetected(value, null)
                                }
                            }
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            } else {
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
                val detectedDate = processOcrText(visionText.text)
                onComplete(detectedDate)
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
                val dateStr = matcher.group().trim()
                var score = 10 

                if (expiryKeywords.any { cleanLine.contains(it) }) score += 100
                if (mfgKeywords.any { cleanLine.contains(it) }) score -= 500

                candidates.add(normalizeAndFormatDate(dateStr) to score)
            }
        }

        return candidates.filter { it.second > 0 }.maxByOrNull { it.second }?.first
    }

    private fun normalizeAndFormatDate(dateStr: String): String {
        // Handle names like 20 JUN 2026
        if (dateStr.any { it.isLetter() }) {
            try {
                val sdfInput = java.text.SimpleDateFormat("dd MMM yyyy", Locale.US)
                val date = sdfInput.parse(dateStr)
                if (date != null) {
                    return java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
                }
            } catch (e: Exception) {}
        }

        val match = Regex("(\\d{1,4})[./-](\\d{1,2})[./-](\\d{1,4})").find(dateStr)
        if (match != null) {
            val (v1, v2, v3) = match.destructured
            var day = ""
            var month = ""
            var year = ""

            if (v1.length == 4) { // YYYY-MM-DD
                year = v1
                month = v2
                day = v3
            } else { // DD/MM/YYYY or DD/MM/YY
                day = v1
                month = v2
                year = if (v3.length == 2) "20$v3" else v3
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
                onComplete(processOcrText(visionText.text))
            }
            .addOnFailureListener { onComplete(null) }
    }
}
