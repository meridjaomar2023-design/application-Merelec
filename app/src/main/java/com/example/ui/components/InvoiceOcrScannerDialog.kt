package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.model.OperationType
import com.example.data.model.PartnerEntity
import com.example.data.model.ProductCatalogEntity
import com.example.util.CurrencyUtils
import com.example.util.IntelligentOcrEngine
import com.example.util.InvoiceOcrHelper
import com.example.util.InvoiceOcrResult
import com.example.util.OcrExtractedLine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import com.example.ui.screens.OcrColumnOrderSelectorSection
import com.example.util.OcrColumnConfig
import com.example.util.OcrConfidenceLevel

private val OcrDialogHighContrastColorScheme = lightColorScheme(
    primary = Color(0xFF1E3A8A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF0D9488),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF115E59),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFE2E8F0),
    onSurfaceVariant = Color(0xFF334155),
    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

@Composable
fun InvoiceOcrScannerDialog(
    initialTargetType: OperationType = OperationType.FACTURE_FOURNISSEUR,
    knownPartners: List<PartnerEntity> = emptyList(),
    catalogProducts: List<ProductCatalogEntity> = emptyList(),
    ownCompanyName: String = "",
    defaultMarginCoefficient: Double = 1.25,
    allowAppendToCurrentDraft: Boolean = false,
    onImportResult: (
        targetType: OperationType,
        partnerName: String,
        detectedInvoiceNumber: String,
        lines: List<OcrExtractedLine>,
        appliquerTva: Boolean,
        tauxTva: Double,
        marginCoefficient: Double,
        appendToExistingDraft: Boolean,
        saveImmediately: Boolean
    ) -> Unit,
    onOpenIntelligentOcr: (() -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isScanning by remember { mutableStateOf(false) }
    var isRefiningWithGemini by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableFloatStateOf(0f) }
    var scanProgressStep by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var scannedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var hasScannedOnce by remember { mutableStateOf(false) }

    var columnConfig by remember { mutableStateOf(OcrColumnConfig()) }
    var showCustomColumnOrderEditor by remember { mutableStateOf(false) }
    var lastScanResult by remember { mutableStateOf<InvoiceOcrResult?>(null) }

    var targetType by remember(initialTargetType) { mutableStateOf(initialTargetType) }
    var partnerName by remember { mutableStateOf("") }
    var detectedRawPartnerName by remember { mutableStateOf("") }
    var detectedInvoiceNumber by remember { mutableStateOf("") }
    var appliquerTva by remember { mutableStateOf(false) }
    var tauxTva by remember { mutableDoubleStateOf(19.0) }
    var marginInput by remember(defaultMarginCoefficient, initialTargetType) {
        if (!initialTargetType.isPurchaseDocument && defaultMarginCoefficient > 0.0) {
            val c = defaultMarginCoefficient
            mutableStateOf(
                if (c % 1.0 == 0.0) c.toInt().toString()
                else String.format(Locale.US, "%.2f", c).trimEnd('0').trimEnd('.')
            )
        } else {
            mutableStateOf("1")
        }
    }
    var extractedLines by remember { mutableStateOf<List<OcrExtractedLine>>(emptyList()) }
    var reconstructedRows by remember { mutableStateOf<List<String>>(emptyList()) }
    var showRawTextRows by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    fun applyScanResult(result: InvoiceOcrResult) {
        lastScanResult = result
        if (result.detectedPartnerName.isNotBlank()) {
            detectedRawPartnerName = result.detectedPartnerName
            val isKnownSupplier = knownPartners.any {
                it.type.name == "FOURNISSEUR" && it.name.trim().equals(result.detectedPartnerName.trim(), ignoreCase = true)
            }
            if (!targetType.isPurchaseDocument && isKnownSupplier) {
                partnerName = ""
            } else {
                partnerName = result.detectedPartnerName
            }
        }
        if (result.detectedInvoiceNumber.isNotBlank()) {
            detectedInvoiceNumber = result.detectedInvoiceNumber
        }
        appliquerTva = result.appliquerTva
        tauxTva = result.tauxTva
        reconstructedRows = result.reconstructedRows
        extractedLines = result.extractedLines
        showRawTextRows = extractedLines.isEmpty() && result.reconstructedRows.isNotEmpty()
    }

    fun updateColumnConfigAndReparse(newConfig: OcrColumnConfig) {
        columnConfig = newConfig
        val existing = lastScanResult
        if (existing != null && existing.reconstructedRows.isNotEmpty()) {
            val reparsed = InvoiceOcrHelper.reparseResultWithColumnConfig(
                existingResult = existing,
                columnConfig = newConfig,
                catalogProducts = catalogProducts
            )
            lastScanResult = reparsed
            extractedLines = reparsed.extractedLines
        }
    }

    fun analyzeLoadedBitmap(bitmap: Bitmap, forceGeminiVision: Boolean = false) {
        isScanning = true
        isRefiningWithGemini = false
        errorMessage = null
        scanProgress = 0.25f
        scanProgressStep = "Étape 1/3 : Préparation de l'image (${bitmap.width}×${bitmap.height} px)..."

        scope.launch {
            try {
                scanProgress = 0.50f
                scanProgressStep = "Étape 2/3 : Extraction optique locale des lignes (ML Kit)..."
                val localResult = IntelligentOcrEngine.analyzeDocumentPagesLocalFast(
                    context = context,
                    pages = listOf(bitmap),
                    knownPartners = knownPartners,
                    catalogProducts = catalogProducts,
                    ownCompanyName = ownCompanyName,
                    columnConfig = columnConfig
                ).getOrThrow()

                val hasLocalLines = localResult.extractedLines.isNotEmpty() && !forceGeminiVision
                if (hasLocalLines) {
                    applyScanResult(localResult)
                    hasScannedOnce = true
                    scanProgress = 0.85f
                    isScanning = false
                    isRefiningWithGemini = true
                } else {
                    scanProgress = 0.80f
                    scanProgressStep = "Étape 3/3 : Analyse intelligente par Gemini 3.8 Flash (IA Vision)..."
                }

                val aiRefined = IntelligentOcrEngine.refineWithAi(
                    context = context,
                    pages = listOf(bitmap),
                    baseMlResult = localResult,
                    catalogProducts = catalogProducts,
                    ownCompanyName = ownCompanyName,
                    columnConfig = columnConfig
                )
                isRefiningWithGemini = false

                if (aiRefined != null && (aiRefined.extractedLines.isNotEmpty() || !hasLocalLines)) {
                    val localVerifiedCount = localResult.extractedLines.count {
                        it.confidence == OcrConfidenceLevel.HIGH && !it.hasCalculationMismatch && it.prixUnitaire > 0.0
                    }
                    val aiVerifiedCount = aiRefined.extractedLines.count {
                        it.confidence == OcrConfidenceLevel.HIGH && !it.hasCalculationMismatch && it.prixUnitaire > 0.0
                    }
                    if (!forceGeminiVision && hasLocalLines && localVerifiedCount > aiVerifiedCount) {
                        val merged = aiRefined.copy(extractedLines = localResult.extractedLines)
                        applyScanResult(merged)
                    } else {
                        applyScanResult(aiRefined)
                    }
                } else if (!hasLocalLines) {
                    applyScanResult(localResult)
                }
                scanProgress = 1.0f
                hasScannedOnce = true
            } catch (e: Exception) {
                errorMessage = "Erreur lors de la reconnaissance OCR : ${e.localizedMessage ?: "échec de lecture"}"
            } finally {
                isScanning = false
                isRefiningWithGemini = false
            }
        }
    }

    fun processImageUri(uri: Uri) {
        isScanning = true
        isRefiningWithGemini = false
        errorMessage = null
        scanProgress = 0.15f
        scanProgressStep = "Étape 1/3 : Chargement et décodage de l'image..."
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    InvoiceOcrHelper.loadNormalizedBitmapFromUri(context, uri)
                }
                if (bitmap == null) {
                    isScanning = false
                    errorMessage = "Impossible de décoder l'image sélectionnée. Veuillez choisir un fichier JPG ou PNG valide."
                    return@launch
                }
                scannedBitmap = bitmap
                analyzeLoadedBitmap(bitmap, forceGeminiVision = false)
            } catch (e: Exception) {
                isScanning = false
                errorMessage = "Erreur lors du chargement de l'image : ${e.localizedMessage ?: "échec"}"
            }
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            processImageUri(uri)
        }
    }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            processImageUri(uri)
        }
    }

    fun launchCameraCapture() {
        try {
            val docsDir = File(context.cacheDir, "documents")
            if (!docsDir.exists()) docsDir.mkdirs()
            val photoFile = File(docsDir, "ocr_facture_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            pendingCameraUri = uri
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            errorMessage = "Impossible d'ouvrir l'appareil photo : ${e.localizedMessage ?: "erreur"}"
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchCameraCapture()
        } else {
            errorMessage = "Permission Caméra refusée. Vous pouvez choisir une image depuis la galerie."
        }
    }

    val activeMargin = marginInput.replace(',', '.').toDoubleOrNull()?.takeIf { it > 0.0 } ?: 1.0
    val selectedLines = extractedLines.filter { it.selected && it.designation.isNotBlank() }
    val previewTotalHt = selectedLines.sumOf { line ->
        val pu = kotlin.math.round(line.prixUnitaire * activeMargin * 100.0) / 100.0
        line.quantite * pu
    }
    val previewTotalTtc = if (appliquerTva) previewTotalHt * (1.0 + tauxTva / 100.0) else previewTotalHt

    MaterialTheme(colorScheme = OcrDialogHighContrastColorScheme) {
        CompositionLocalProvider(LocalContentColor provides Color(0xFF0F172A)) {
            AlertDialog(
                onDismissRequest = onDismiss,
                containerColor = Color.White,
                titleContentColor = Color(0xFF0F172A),
                textContentColor = Color(0xFF0F172A),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.DocumentScanner,
                            contentDescription = null,
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Scanner une Facture en Image (OCR)",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Reconnaissance optique des articles, quantités, prix et fournisseur",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF334155)
                            )
                        }
                    }
                },
                text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onOpenIntelligentOcr != null) {
                    FilledTonalButton(
                        onClick = onOpenIntelligentOcr,
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ocr_open_intelligent_studio_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ouvrir « OCR Intelligent IA » (PDF Multipages & 2 Volets)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF312E81)
                        )
                    }
                }

                // 1. Boutons de sélection d'image (Galerie ou Appareil photo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_pick_gallery_image_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddPhotoAlternate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Image / Galerie",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                launchCameraCapture()
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ocr_capture_camera_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF4338CA)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Prendre Photo",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF4338CA)
                        )
                    }
                }

                OcrColumnOrderSelectorSection(
                    columnConfig = columnConfig,
                    showCustomEditor = showCustomColumnOrderEditor,
                    onToggleCustomEditor = { showCustomColumnOrderEditor = !showCustomColumnOrderEditor },
                    onColumnConfigChange = { newConfig -> updateColumnConfigAndReparse(newConfig) }
                )

                if (isScanning) {
                    val pct = (scanProgress.coerceIn(0.05f, 1f) * 100).toInt()
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEEF2FF),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(12.dp))
                            .testTag("ocr_dialog_progress_banner")
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = Color(0xFF4338CA)
                                    )
                                    Column {
                                        Text(
                                            text = scanProgressStep.ifBlank { "Reconnaissance OCR & Gemini IA en cours..." },
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF312E81)
                                        )
                                        Text(
                                            text = "Extraction des articles, quantités (Qts), PU HT et coordonnées",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF4338CA)
                                        )
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFF4338CA)
                                ) {
                                    Text(
                                        text = "$pct%",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            LinearProgressIndicator(
                                progress = { scanProgress.coerceIn(0.05f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF4338CA),
                                trackColor = Color(0xFFC7D2FE)
                            )
                        }
                    }
                } else if (isRefiningWithGemini) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF16A34A)
                                )
                                Text(
                                    text = "Affinage intelligent par Gemini 3.8 Flash en cours (85%)...",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF166534)
                                )
                            }
                            LinearProgressIndicator(
                                progress = { 0.85f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = Color(0xFF16A34A),
                                trackColor = Color(0xFFDCFCE7)
                            )
                        }
                    }
                }

                errorMessage?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                // Aperçu miniature de la facture scannée + bouton de ré-analyse Gemini 3.8 Flash
                scannedBitmap?.let { bmp ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(105.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF1F5F9))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = bmp.asImageBitmap(),
                                contentDescription = "Facture scannée",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        FilledTonalButton(
                            onClick = { analyzeLoadedBitmap(bmp, forceGeminiVision = true) },
                            enabled = !isScanning,
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ocr_reanalyze_gemini_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF4338CA),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Relancer l'analyse Vision avec Gemini 3.8 Flash IA",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF312E81)
                            )
                        }
                    }
                }

                if (!hasScannedOnce && !isScanning) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Comment fonctionne le scanner OCR ?",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "1. Choisissez une photo ou capture d'écran de facture (fournisseur ou client).\n" +
                                    "2. L'OCR détecte automatiquement le fournisseur/client, le N° de facture et toutes les lignes d'articles (Désignation, Qté, Prix Unitaire).\n" +
                                    "3. Vérifiez ou ajustez les lignes détectées, puis cliquez sur « Ouvrir dans l'éditeur ».",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (hasScannedOnce) {
                    HorizontalDivider()

                    // 2. Choix du type de document cible
                    Text(
                        text = "1. Type de document à générer :",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF312E81)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            OperationType.BL_CLIENT to "Bon de Livraison (BL Client)",
                            OperationType.FACTURE to "Facture Client",
                            OperationType.FACTURE_FOURNISSEUR to "Facture Fournisseur",
                            OperationType.BL_FOURNISSEUR to "Bon Réception Fourn.",
                            OperationType.DEVIS_CLIENT to "Devis Client"
                        ).forEach { (opType, label) ->
                            FilterChip(
                                selected = targetType == opType,
                                onClick = {
                                    val wasPurchase = targetType.isPurchaseDocument
                                    val nowPurchase = opType.isPurchaseDocument
                                    targetType = opType
                                    if (wasPurchase && !nowPurchase) {
                                        // Passage vers un document Client (BL Client / Facture Client)
                                        if (partnerName.trim().equals(detectedRawPartnerName.trim(), ignoreCase = true)) {
                                            val isClient = knownPartners.any {
                                                it.type.name == "CLIENT" && it.name.trim().equals(partnerName.trim(), ignoreCase = true)
                                            }
                                            if (!isClient) {
                                                partnerName = ""
                                            }
                                        }
                                        if (marginInput == "1") {
                                            val c = defaultMarginCoefficient
                                            marginInput = if (c % 1.0 == 0.0) c.toInt().toString()
                                            else String.format(Locale.US, "%.2f", c).trimEnd('0').trimEnd('.')
                                        }
                                    } else if (!wasPurchase && nowPurchase) {
                                        if (partnerName.isBlank() && detectedRawPartnerName.isNotBlank()) {
                                            partnerName = detectedRawPartnerName
                                        }
                                        marginInput = "1"
                                    }
                                },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    // Indicateur clair de l'onglet de destination
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (targetType.isSalesDocument) Color(0xFFEFF6FF) else Color(0xFFFEF3C7),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (targetType.isSalesDocument) Icons.Default.TrendingUp else Icons.Default.ShoppingBag,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (targetType.isSalesDocument) Color(0xFF2563EB) else Color(0xFFD97706)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (targetType.isSalesDocument)
                                    "Ce document sera enregistré dans l'onglet « Vente » (Bons de Livraison & Factures Clients)"
                                else
                                    "Ce document sera enregistré dans l'onglet « Achat » (Factures & Bons Fournisseurs)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = if (targetType.isSalesDocument) Color(0xFF1D4ED8) else Color(0xFFB45309)
                            )
                        }
                    }

                    // 3. En-tête détecté : Fournisseur / Client + N° Facture d'origine
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it },
                            label = {
                                Text(
                                    if (targetType.isPurchaseDocument) "Fournisseur" else "Client (Vente)",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            placeholder = {
                                Text(
                                    if (targetType.isPurchaseDocument) "Fournisseur Divers"
                                    else "Client Comptoir",
                                    color = Color(0xFF94A3B8)
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = Color(0xFF1E293B),
                                unfocusedLabelColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(1.2f)
                                .testTag("ocr_partner_name_input")
                        )
                        OutlinedTextField(
                            value = detectedInvoiceNumber,
                            onValueChange = { detectedInvoiceNumber = it },
                            label = {
                                Text(
                                    "N° Facture origine",
                                    color = Color(0xFF1E293B),
                                    fontWeight = FontWeight.Medium
                                )
                            },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.Black,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.Black,
                                unfocusedTextColor = Color.Black,
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White,
                                focusedBorderColor = Color(0xFF2563EB),
                                unfocusedBorderColor = Color(0xFFCBD5E1),
                                focusedLabelColor = Color(0xFF1E293B),
                                unfocusedLabelColor = Color(0xFF475569)
                            ),
                            modifier = Modifier
                                .weight(0.8f)
                                .testTag("ocr_invoice_number_input")
                        )
                    }

                    // Suggestions rapides de partenaires existants
                    val relevantPartners = remember(targetType, knownPartners) {
                        knownPartners.filter {
                            if (targetType.isPurchaseDocument) it.type.name == "FOURNISSEUR"
                            else it.type.name == "CLIENT"
                        }.take(10)
                    }
                    if (relevantPartners.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            relevantPartners.forEach { p ->
                                FilterChip(
                                    selected = partnerName.trim().equals(p.name.trim(), ignoreCase = true),
                                    onClick = { partnerName = p.name },
                                    label = { Text(p.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Coefficient de marge optionnel si on importe directement en Facture Client ou BL Client
                    if (!targetType.isPurchaseDocument) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Coeff. de marge sur les prix lus :",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF312E81)
                                    )
                                    OutlinedTextField(
                                        value = marginInput,
                                        onValueChange = { marginInput = it },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            color = Color.Black,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.Black,
                                            unfocusedTextColor = Color.Black,
                                            focusedContainerColor = Color.White,
                                            unfocusedContainerColor = Color.White,
                                            focusedBorderColor = Color(0xFF4338CA),
                                            unfocusedBorderColor = Color(0xFFCBD5E1)
                                        ),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.width(95.dp)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "1" to "Prix lu (x1)",
                                        "1.15" to "+15%",
                                        "1.20" to "+20%",
                                        "1.25" to "+25%",
                                        "1.30" to "+30%"
                                    ).forEach { (valStr, lbl) ->
                                        FilterChip(
                                            selected = marginInput.replace(',', '.') == valStr,
                                            onClick = { marginInput = valStr },
                                            label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Option TVA détectée
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Appliquer TVA (${tauxTva.toInt()}%)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                            if (appliquerTva) {
                                Spacer(modifier = Modifier.width(8.dp))
                                FilterChip(
                                    selected = tauxTva == 19.0,
                                    onClick = { tauxTva = 19.0 },
                                    label = { Text("19%", style = MaterialTheme.typography.labelSmall) }
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                FilterChip(
                                    selected = tauxTva == 9.0,
                                    onClick = { tauxTva = 9.0 },
                                    label = { Text("9%", style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Switch(
                            checked = appliquerTva,
                            onCheckedChange = { appliquerTva = it }
                        )
                    }

                    HorizontalDivider()

                    // 4. Liste des articles détectés par OCR
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "2. Articles détectés (${selectedLines.size}/${extractedLines.size}) :",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF312E81)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            TextButton(
                                onClick = {
                                    extractedLines = extractedLines + OcrExtractedLine(
                                        designation = "Nouvel article",
                                        quantite = 1.0,
                                        prixUnitaire = 0.0,
                                        selected = true
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Ajouter", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }

                    // Barre d'actions rapides sur les articles détectés
                    if (extractedLines.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    extractedLines = extractedLines.map { line ->
                                        line.copy(
                                            quantite = if (line.prixUnitaire > 0.0) line.prixUnitaire else 1.0,
                                            prixUnitaire = line.quantite
                                        )
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp).testTag("ocr_swap_all_qty_price_btn")
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF4338CA))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Inverser Qté ⇄ Prix", style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = Color(0xFF4338CA))
                            }

                            val allSelected = extractedLines.all { it.selected }
                            OutlinedButton(
                                onClick = {
                                    val newSelect = !allSelected
                                    extractedLines = extractedLines.map { it.copy(selected = newSelect) }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp)
                            ) {
                                Text(if (allSelected) "Tout décocher" else "Tout cocher", style = MaterialTheme.typography.labelSmall)
                            }

                            if (extractedLines.any { !it.selected }) {
                                OutlinedButton(
                                    onClick = {
                                        extractedLines = extractedLines.filter { it.selected }
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(30.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text("Suppr. non cochés", style = MaterialTheme.typography.labelSmall)
                                }
                            }

                            OutlinedButton(
                                onClick = {
                                    extractedLines = emptyList()
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(30.dp).testTag("ocr_clear_all_lines_btn"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626))
                            ) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Tout effacer", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                            }
                        }
                    }

                    if (extractedLines.isEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEF3C7),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Aucune ligne tabulaire détectée ou tous les articles ont été effacés. Utilisez les lignes de texte OCR ci-dessous ou cliquez sur « Ajouter » pour saisir un article.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF92400E),
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            extractedLines.forEachIndexed { idx, line ->
                                var qtyText by remember(line.id, line.quantite) {
                                    mutableStateOf(CurrencyUtils.formatQty(line.quantite))
                                }
                                var puText by remember(line.id, line.prixUnitaire) {
                                    mutableStateOf(CurrencyUtils.formatQty(line.prixUnitaire))
                                }

                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (line.selected) Color(0xFFF8FAFC) else Color(0xFFF1F5F9),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(
                                            width = 1.dp,
                                            color = if (line.selected) Color(0xFF94A3B8) else Color(0xFFCBD5E1),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // 1. En-tête de la carte : Checkbox, Numéro d'article et Bouton Supprimer
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Checkbox(
                                                    checked = line.selected,
                                                    onCheckedChange = { checked ->
                                                        extractedLines = extractedLines.toMutableList().also {
                                                            it[idx] = line.copy(selected = checked)
                                                        }
                                                    }
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = if (line.selected) Color(0xFFDBEAFE) else Color(0xFFE2E8F0)
                                                ) {
                                                    Text(
                                                        text = "Article #${idx + 1}" + if (line.reference.isNotBlank()) " (${line.reference})" else "",
                                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                        color = if (line.selected) Color(0xFF1E40AF) else Color(0xFF475569),
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }

                                            // Bouton Supprimer explicite et bien visible
                                            FilledTonalButton(
                                                onClick = {
                                                    extractedLines = extractedLines.filterIndexed { i, _ -> i != idx }
                                                },
                                                colors = ButtonDefaults.filledTonalButtonColors(
                                                    containerColor = Color(0xFFFEE2E2),
                                                    contentColor = Color(0xFFDC2626)
                                                ),
                                                shape = RoundedCornerShape(6.dp),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier
                                                    .height(32.dp)
                                                    .testTag("ocr_delete_line_${idx}_btn")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.DeleteOutline,
                                                    contentDescription = "Supprimer cet article",
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Supprimer",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                                )
                                            }
                                        }

                                        // 2. Champs de l'article affichés dans l'ordre exact choisi (ex: 1. Qts, 2. Désignation, 3. P.U.HT, 4. Réf)
                                        val orderedDisplayFields = columnConfig.displayFields
                                        val fieldRows = if (orderedDisplayFields.size <= 3) {
                                            listOf(orderedDisplayFields)
                                        } else {
                                            orderedDisplayFields.chunked(2)
                                        }

                                        fieldRows.forEach { rowFields ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                rowFields.forEach { field ->
                                                    val posNumber = orderedDisplayFields.indexOf(field) + 1
                                                    val fieldWeight = when (field) {
                                                        com.example.util.OcrColumnField.DESIGNATION -> if (rowFields.size == 1) 1f else if (rowFields.size == 2) 0.64f else 0.48f
                                                        com.example.util.OcrColumnField.PU_HT -> if (rowFields.size == 2) 0.36f else 0.28f
                                                        com.example.util.OcrColumnField.QUANTITE -> if (rowFields.size == 2) 0.36f else 0.24f
                                                        com.example.util.OcrColumnField.REFERENCE -> if (rowFields.size == 2) 0.36f else 0.25f
                                                    }

                                                    when (field) {
                                                        com.example.util.OcrColumnField.QUANTITE -> {
                                                            OutlinedTextField(
                                                                value = qtyText,
                                                                onValueChange = { v ->
                                                                    qtyText = v
                                                                    val parsed = InvoiceOcrHelper.parseFrenchDouble(v)
                                                                    if (parsed != null && parsed > 0.0) {
                                                                        extractedLines = extractedLines.toMutableList().also {
                                                                            it[idx] = line.copy(quantite = parsed)
                                                                        }
                                                                    }
                                                                },
                                                                label = { Text("$posNumber. Qts", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold) },
                                                                singleLine = true,
                                                                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                                                colors = OutlinedTextFieldDefaults.colors(
                                                                    focusedTextColor = Color.Black,
                                                                    unfocusedTextColor = Color.Black,
                                                                    focusedContainerColor = Color.White,
                                                                    unfocusedContainerColor = Color.White,
                                                                    focusedBorderColor = Color(0xFF2563EB),
                                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                                    focusedLabelColor = Color(0xFF1E293B),
                                                                    unfocusedLabelColor = Color(0xFF475569)
                                                                ),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                modifier = Modifier.weight(fieldWeight)
                                                            )
                                                        }
                                                        com.example.util.OcrColumnField.DESIGNATION -> {
                                                            OutlinedTextField(
                                                                value = line.designation,
                                                                onValueChange = { newDes ->
                                                                    extractedLines = extractedLines.toMutableList().also {
                                                                        it[idx] = line.copy(designation = newDes)
                                                                    }
                                                                },
                                                                label = { Text("$posNumber. Désignation", color = Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                                                singleLine = true,
                                                                textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold),
                                                                colors = OutlinedTextFieldDefaults.colors(
                                                                    focusedTextColor = Color.Black,
                                                                    unfocusedTextColor = Color.Black,
                                                                    focusedContainerColor = Color.White,
                                                                    unfocusedContainerColor = Color.White,
                                                                    focusedBorderColor = Color(0xFF2563EB),
                                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                                    focusedLabelColor = Color(0xFF1E293B),
                                                                    unfocusedLabelColor = Color(0xFF475569)
                                                                ),
                                                                modifier = Modifier.weight(fieldWeight)
                                                            )
                                                        }
                                                        com.example.util.OcrColumnField.PU_HT -> {
                                                            OutlinedTextField(
                                                                value = puText,
                                                                onValueChange = { v ->
                                                                    puText = v
                                                                    val parsed = InvoiceOcrHelper.parseFrenchDouble(v)
                                                                    if (parsed != null && parsed >= 0.0) {
                                                                        extractedLines = extractedLines.toMutableList().also {
                                                                            it[idx] = line.copy(prixUnitaire = parsed)
                                                                        }
                                                                    }
                                                                },
                                                                label = { Text("$posNumber. P.U.HT", color = Color(0xFF1E293B), fontWeight = FontWeight.Bold) },
                                                                singleLine = true,
                                                                textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                                                colors = OutlinedTextFieldDefaults.colors(
                                                                    focusedTextColor = Color.Black,
                                                                    unfocusedTextColor = Color.Black,
                                                                    focusedContainerColor = Color.White,
                                                                    unfocusedContainerColor = Color.White,
                                                                    focusedBorderColor = Color(0xFF2563EB),
                                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                                    focusedLabelColor = Color(0xFF1E293B),
                                                                    unfocusedLabelColor = Color(0xFF475569)
                                                                ),
                                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                                modifier = Modifier.weight(fieldWeight)
                                                            )
                                                        }
                                                        com.example.util.OcrColumnField.REFERENCE -> {
                                                            OutlinedTextField(
                                                                value = line.reference,
                                                                onValueChange = { newRef ->
                                                                    extractedLines = extractedLines.toMutableList().also {
                                                                        it[idx] = line.copy(reference = newRef)
                                                                    }
                                                                },
                                                                label = { Text("$posNumber. Réf", color = Color(0xFF1E293B), fontWeight = FontWeight.Medium) },
                                                                singleLine = true,
                                                                textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.SemiBold),
                                                                colors = OutlinedTextFieldDefaults.colors(
                                                                    focusedTextColor = Color.Black,
                                                                    unfocusedTextColor = Color.Black,
                                                                    focusedContainerColor = Color.White,
                                                                    unfocusedContainerColor = Color.White,
                                                                    focusedBorderColor = Color(0xFF2563EB),
                                                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                                                    focusedLabelColor = Color(0xFF1E293B),
                                                                    unfocusedLabelColor = Color(0xFF475569)
                                                                ),
                                                                modifier = Modifier.weight(fieldWeight)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // 4. Sous-total de la ligne bien visible
                                        val effectivePu = kotlin.math.round(line.prixUnitaire * activeMargin * 100.0) / 100.0
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFFEEF2FF),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "Sous-total (${CurrencyUtils.formatQty(line.quantite)} × ${CurrencyUtils.formatDa(effectivePu)}) :",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = Color(0xFF4338CA)
                                                )
                                                Text(
                                                    text = CurrencyUtils.formatDa(line.quantite * effectivePu),
                                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                                    color = Color(0xFF312E81)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 5. Accordéon pour voir toutes les lignes de texte OCR brutes et ajouter en 1 clic
                    if (reconstructedRows.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showRawTextRows = !showRawTextRows },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showRawTextRows)
                                    "Masquer le texte OCR brut (${reconstructedRows.size} lignes)"
                                else
                                    "Voir le texte OCR reconnu (${reconstructedRows.size} lignes)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = if (showRawTextRows) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        AnimatedVisibility(visible = showRawTextRows) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Astuce : Cliquez sur « + Article » pour ajouter une ligne non détectée ou « Tiers » pour définir le nom du client/fournisseur :",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    reconstructedRows.forEach { rawLine ->
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surface,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Text(
                                                    text = rawLine,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    TextButton(
                                                        onClick = {
                                                            partnerName = rawLine.split("   ").firstOrNull()?.trim() ?: rawLine.take(40)
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("Tiers", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp))
                                                    }
                                                    TextButton(
                                                        onClick = {
                                                            val parsed = InvoiceOcrHelper.tryParseProductRow(
                                                                rawLine,
                                                                catalogProducts,
                                                                insideTable = true
                                                            ) ?: OcrExtractedLine(
                                                                designation = rawLine.replace("   ", " ").trim(),
                                                                quantite = 1.0,
                                                                prixUnitaire = 0.0,
                                                                selected = true
                                                            )
                                                            extractedLines = extractedLines + parsed
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp)
                                                    ) {
                                                        Text("+ Article", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Synthèse financière avant import
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFECFDF5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "${selectedLines.size} article(s) sélectionné(s)",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF065F46)
                                )
                                Text(
                                    text = "Destination : ${targetType.label} (Onglet ${if (targetType.isSalesDocument) "Vente" else "Achat"})",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF047857)
                                )
                            }
                            Text(
                                text = CurrencyUtils.formatDa(previewTotalTtc),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (hasScannedOnce || scannedBitmap != null || extractedLines.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (allowAppendToCurrentDraft) {
                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onImportResult(
                                    targetType,
                                    partnerName,
                                    detectedInvoiceNumber,
                                    extractedLines,
                                    appliquerTva,
                                    tauxTva,
                                    activeMargin,
                                    true,
                                    false
                                )
                            },
                            enabled = extractedLines.isNotEmpty()
                        ) {
                            Text("Ajouter au bon", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onImportResult(
                                targetType,
                                partnerName,
                                detectedInvoiceNumber,
                                extractedLines,
                                appliquerTva,
                                tauxTva,
                                activeMargin,
                                false,
                                false
                            )
                        },
                        enabled = true,
                        modifier = Modifier.testTag("ocr_open_editor_btn")
                    ) {
                        Text("Éditer", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                    Button(
                        onClick = {
                            onDismiss()
                            onImportResult(
                                targetType,
                                partnerName,
                                detectedInvoiceNumber,
                                extractedLines,
                                appliquerTva,
                                tauxTva,
                                activeMargin,
                                allowAppendToCurrentDraft,
                                true
                            )
                        },
                        enabled = true,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.testTag("ocr_confirm_import_btn")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (targetType == OperationType.BL_CLIENT) "Enregistrer le BL"
                                else if (targetType == OperationType.FACTURE) "Enregistrer Facture"
                                else if (targetType == OperationType.FACTURE_FOURNISSEUR) "Enregistrer Facture Fourn."
                                else if (targetType == OperationType.BL_FOURNISSEUR) "Enregistrer Bon Fourn."
                                else "Enregistrer (${targetType.shortCode})",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer", color = Color(0xFF1E3A8A), fontWeight = FontWeight.Bold)
            }
        }
            )
        }
    }
}
