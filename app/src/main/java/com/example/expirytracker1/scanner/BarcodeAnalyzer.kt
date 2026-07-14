package com.example.expirytracker1.scanner

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
import java.util.regex.Pattern

class BarcodeAnalyzer(
    private val onDetected: (String?, String?) -> Unit
) : ImageAnalysis.Analyzer {

    private val barcodeOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
            Barcode.FORMAT_UPC_A,
            Barcode.FORMAT_UPC_E,
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_CODE_128
        )
        .build()

    private val barcodeScanner = BarcodeScanning.getClient(barcodeOptions)
    private val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // Regex for common date formats: 
    // Supports: DD/MM/YYYY, DD-MM-YYYY, DD.MM.YYYY, DD/MM/YY, etc.
    // Also captures MM/YYYY
    private val datePattern = Pattern.compile(
        "\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{2,4})\\b|" +
        "\\b(\\d{4})[./-](\\d{1,2})[./-](\\d{1,2})\\b|" +
        "\\b(\\d{1,2})[./-](\\d{4})\\b"
    )

    fun analyzeImage(image: InputImage, onComplete: () -> Unit = {}) {
        barcodeScanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { value ->
                        var gs1Expiry: String? = null
                        val gs1Regex = Regex("(?:^|\\D)17(\\d{6})(?:\\D|$)")
                        val match = gs1Regex.find(value)
                        if (match != null) {
                            val d = match.groupValues[1]
                            gs1Expiry = "${d.substring(4, 6)}/${d.substring(2, 4)}/20${d.substring(0, 2)}"
                        }
                        onDetected(value, gs1Expiry)
                    }
                }
            }
            .addOnCompleteListener {
                textRecognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val lines = visionText.textBlocks.flatMap { it.lines }
                        for (line in lines) {
                            val text = line.text.uppercase()
                            if (text.contains("EXP") || text.contains("BEST") || text.contains("BB") || text.contains("USE BY")) {
                                val matcher = datePattern.matcher(text)
                                if (matcher.find()) {
                                    val dateStr = matcher.group()
                                    onDetected(null, dateStr)
                                    break
                                }
                            } else {
                                val matcher = datePattern.matcher(text)
                                if (matcher.find()) {
                                    val dateStr = matcher.group()
                                    onDetected(null, dateStr)
                                }
                            }
                        }
                    }
                    .addOnCompleteListener {
                        onComplete()
                    }
            }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        analyzeImage(image) {
            imageProxy.close()
        }
    }
}
