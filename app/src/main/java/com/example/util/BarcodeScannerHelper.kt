package com.example.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

object BarcodeScannerHelper {

    fun scanBarcode(
        context: Context,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                .enableAutoZoom()
                .build()

            val scanner = GmsBarcodeScanning.getClient(context, options)

            scanner.startScan()
                .addOnSuccessListener { barcode ->
                    val rawValue = barcode.rawValue
                    if (!rawValue.isNullOrBlank()) {
                        onSuccess(rawValue)
                    } else {
                        onError("Code-barre vide ou illisible.")
                    }
                }
                .addOnCanceledListener {
                    // Annulation par l'utilisateur
                }
                .addOnFailureListener { e ->
                    val errorMsg = e.localizedMessage ?: "Erreur lors du scan du code-barre."
                    onError(errorMsg)
                }
        } catch (e: Exception) {
            onError(e.localizedMessage ?: "Impossible de démarrer le lecteur de code-barre.")
        }
    }
}
