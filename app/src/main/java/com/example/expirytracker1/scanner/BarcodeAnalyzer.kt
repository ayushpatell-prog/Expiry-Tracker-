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

    // Regex for various date formats - Optimized for common labels
    private val datePattern = Pattern.compile(
        "\\b(\\d{1,2})[./: -]+(\\d{1,2})[./: -]+(\\d{2,4})\\b|" +
        "\\b(\\d{4})[./: -]+(\\d{1,2})[./: -]+(\\d{1,2})\\b|" +
        "\\b(\\d{1,2})[./: -]+(JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC)[A-Z]*[./: -]+(\\d{2,4})\\b",
        Pattern.CASE_INSENSITIVE
    )

    private val expiryKeywords = listOf("EXP", "EXPIRY", "BEST BEFORE", "BEST BY", "USE BY", "CONSUME BEFORE", "EXPIRES", "BB", "E:", "ED:")
    private val mfgKeywords = listOf("PKD", "PACKED", "PACK DATE", "MFD", "MFG", "MANUFACTURED", "PRODUCTION", "MANUFACTURING", "BATCH", "MRP", "M:", "MD:")

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
                if (dateStr.length < 5) continue

                var score = 10 
                if (expiryKeywords.any { cleanLine.contains(it) }) score += 100
                if (mfgKeywords.any { cleanLine.contains(it) }) score -= 500

                candidates.add(normalizeAndFormatDate(dateStr) to score)
            }
        }

        Log.d("OCR_DEBUG", "Found candidates: $candidates")

        return when {
            // Case 1: We found a date with a very high score (explicitly linked to "EXP")
            candidates.any { it.second > 100 } -> {
                candidates.filter { it.second > 100 }.maxByOrNull { it.second }?.first
            }
            // Case 2: No explicit keywords, but multiple dates exist
            // Per user request, if multiple dates are found, take the second one (usually EXP after MFD)
            candidates.size >= 2 -> {
                candidates[1].first
            }
            // Case 3: Only one date found
            candidates.size == 1 -> {
                candidates[0].first
            }
            else -> null
        }
    }

    private fun normalizeAndFormatDate(dateStr: String): String {
        // Handle formats like 21/MAY/27 or 20 JUN 2026
        if (dateStr.any { it.isLetter() }) {
            try {
                // Remove special separators to make parsing easier
                val cleanDate = dateStr.replace("/", " ").replace("-", " ").replace(".", " ").replace(":", " ")
                val parts = cleanDate.split("\\s+".toRegex())
                
                if (parts.size == 3) {
                    val day = parts[0].padStart(2, '0')
                    val monthStr = parts[1].uppercase()
                    var year = parts[2]
                    if (year.length == 2) year = "20$year"

                    // Try to parse month manually to be safe
                    val monthInt = when {
                        monthStr.startsWith("JAN") -> 0
                        monthStr.startsWith("FEB") -> 1
                        monthStr.startsWith("MAR") -> 2
                        monthStr.startsWith("APR") -> 3
                        monthStr.startsWith("MAY") -> 4
                        monthStr.startsWith("JUN") -> 5
                        monthStr.startsWith("JUL") -> 6
                        monthStr.startsWith("AUG") -> 7
                        monthStr.startsWith("SEP") -> 8
                        monthStr.startsWith("OCT") -> 9
                        monthStr.startsWith("NOV") -> 10
                        monthStr.startsWith("DEC") -> 11
                        else -> -1
                    }

                    if (monthInt != -1) {
                        val cal = Calendar.getInstance()
                        cal.set(year.toInt(), monthInt, day.toInt())
                        return java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
                    }
                }
            } catch (e: Exception) {
                Log.e("OCR_ERROR", "Error parsing alpha date: $dateStr", e)
            }
        }

        // Numeric parsing
        val match = Regex("(\\d{1,4})[./: -]+(\\d{1,2})[./: -]+(\\d{1,4})").find(dateStr)
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
                val cal = Calendar.getInstance()
                cal.set(year.toInt(), month.toInt() - 1, day.toInt())
                return java.text.SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(cal.time)
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

    fun close() {
        barcodeScanner.close()
        textRecognizer.close()
    }
}
