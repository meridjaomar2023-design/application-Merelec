package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CallMerge
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CompareArrows
import androidx.compose.material.icons.filled.Contrast
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RotateRight
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.OperationType
import com.example.ui.viewmodel.OperationViewModel
import com.example.util.CurrencyUtils
import com.example.util.DateUtils
import com.example.util.IntelligentOcrEngine
import com.example.util.InvoiceOcrHelper
import com.example.util.InvoiceOcrResult
import com.example.util.OcrArchiveRecord
import com.example.util.OcrColumnConfig
import com.example.util.OcrColumnField
import com.example.util.OcrColumnPreset
import com.example.util.OcrConfidenceLevel
import com.example.util.OcrExtractedLine
import com.example.util.OcrPartnerProfile
import com.example.util.OcrTransformationAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.math.abs
import kotlin.math.round

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IntelligentOcrScreen(
    viewModel: OperationViewModel,
    initialTargetType: OperationType = OperationType.BL_CLIENT
) {
    BackHandler {
        viewModel.navigateBack()
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()
    val catalogProducts by viewModel.catalogProductsList.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()

    val knownPartners = remember(clients, suppliers) {
        (clients + suppliers).distinctBy { it.name.trim().lowercase(Locale.FRENCH) }
    }

    // Pages scannées : originaux immuables vs pages affichées (avec recadrage/contraste/rotation)
    var rawOriginalPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var workingPages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var savedImmutablePaths by remember { mutableStateOf<List<String>>(emptyList()) }

    // État Zoom & Pan sur le document original
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    var isEnhancedContrast by remember { mutableStateOf(false) }

    // État d'analyse IA / OCR
    var isAnalyzing by remember { mutableStateOf(false) }
    var isRefiningWithAi by remember { mutableStateOf(false) }
    var analysisProgress by remember { mutableFloatStateOf(0f) }
    var analysisProgressText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var ocrResult by remember { mutableStateOf<InvoiceOcrResult?>(null) }
    var matchedPartnerProfile by remember { mutableStateOf<OcrPartnerProfile?>(null) }
    var showGeminiAssistantDialog by remember { mutableStateOf(false) }

    // Lignes extraites et sélection de ligne pour surbrillance visuelle sur le document
    var extractedLines by remember { mutableStateOf<List<OcrExtractedLine>>(emptyList()) }
    var selectedLineIdForHighlight by remember { mutableStateOf<String?>(null) }
    var manualCorrectionsCount by remember { mutableIntStateOf(0) }
    var columnConfig by remember { mutableStateOf(OcrColumnConfig()) }
    var showCustomColumnOrderEditor by remember { mutableStateOf(false) }

    fun updateColumnConfigAndReparse(newConfig: OcrColumnConfig) {
        columnConfig = newConfig
        val currentResult = ocrResult
        if (currentResult != null) {
            val reparsed = InvoiceOcrHelper.reparseResultWithColumnConfig(
                existingResult = currentResult,
                columnConfig = newConfig,
                catalogProducts = catalogProducts
            )
            ocrResult = reparsed
            extractedLines = reparsed.extractedLines
        }
    }

    // Métadonnées générales éditables
    var supplierName by remember { mutableStateOf("") }
    var clientName by remember { mutableStateOf("") }
    var partnerAddress by remember { mutableStateOf("") }
    var partnerPhone by remember { mutableStateOf("") }
    var partnerRc by remember { mutableStateOf("") }
    var partnerNif by remember { mutableStateOf("") }
    var partnerNis by remember { mutableStateOf("") }
    var partnerAi by remember { mutableStateOf("") }
    var invoiceNumber by remember { mutableStateOf("") }
    var invoiceDate by remember { mutableStateOf("") }
    var bonNumber by remember { mutableStateOf("") }
    var bonDate by remember { mutableStateOf("") }
    var orderRef by remember { mutableStateOf("") }
    var paymentConditions by remember { mutableStateOf("Espèces") }
    var showMetadataDetails by remember { mutableStateOf(false) }

    // Transformation Fournisseur -> Client (Action A, B ou C)
    var transformationAction by remember(initialTargetType) {
        mutableStateOf(
            if (initialTargetType.isPurchaseDocument) OcrTransformationAction.KEEP_ORIGINAL
            else OcrTransformationAction.SAVE_AND_TRANSFORM
        )
    }
    var supplierDocType by remember(initialTargetType) {
        mutableStateOf(
            if (initialTargetType.isPurchaseDocument) initialTargetType else OperationType.FACTURE_FOURNISSEUR
        )
    }
    var clientDocType by remember(initialTargetType) {
        mutableStateOf(
            if (initialTargetType.isSalesDocument) initialTargetType else OperationType.BL_CLIENT
        )
    }

    // Paramètres commerciaux : Majoration, Remises, TVA multi-taux & Frais
    val defaultMarkupPct = remember(companySettings.defaultMarginCoefficient) {
        val coeff = companySettings.defaultMarginCoefficient
        if (coeff > 1.0) round((coeff - 1.0) * 10000.0) / 100.0 else 20.0
    }
    var globalMajorationText by remember(defaultMarkupPct) {
        mutableStateOf(CurrencyUtils.formatQty(defaultMarkupPct))
    }
    var isMajorationApplied by remember { mutableStateOf(false) }
    var globalRemiseText by remember { mutableStateOf("0") }
    var appliquerTva by remember { mutableStateOf(false) }
    var globalTvaRate by remember { mutableDoubleStateOf(companySettings.defaultTvaRate) }
    var useMultiRateTva by remember { mutableStateOf(false) }
    var fraisTransportText by remember { mutableStateOf("0") }
    var fraisSupplementairesText by remember { mutableStateOf("0") }
    var autresFraisText by remember { mutableStateOf("0") }
    var showCommercialParams by remember { mutableStateOf(true) }

    // Mode d'affichage sur écran compact (Split Haut/Bas vs Onglets Document/Tableau)
    var compactViewMode by remember { mutableIntStateOf(0) } // 0: Split 2 parties, 1: Document Plein, 2: Tableau Plein

    // Dialogues Historique & Mémoire Fournisseurs
    var showArchiveAndMemoryDialog by remember { mutableStateOf(false) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    suspend fun applyOcrResultToUi(result: InvoiceOcrResult, overwriteLines: Boolean = true) {
        ocrResult = result
        if (overwriteLines || extractedLines.isEmpty()) {
            extractedLines = result.extractedLines
            selectedLineIdForHighlight = result.extractedLines.firstOrNull()?.id
        }

        if (result.detectedPartnerName.isNotBlank() && supplierName.isBlank()) {
            supplierName = result.detectedPartnerName
            matchedPartnerProfile = withContext(Dispatchers.IO) {
                IntelligentOcrEngine.findPartnerProfile(context, result.detectedPartnerName)
            }
            matchedPartnerProfile?.let { profile ->
                if (profile.defaultMajorationPct > 0.0) {
                    globalMajorationText = CurrencyUtils.formatQty(profile.defaultMajorationPct)
                }
            }
        }
        if (result.detectedPartnerAddress.isNotBlank() && partnerAddress.isBlank()) partnerAddress = result.detectedPartnerAddress
        if (result.detectedPartnerPhone.isNotBlank() && partnerPhone.isBlank()) partnerPhone = result.detectedPartnerPhone
        if (result.detectedPartnerRc.isNotBlank() && partnerRc.isBlank()) partnerRc = result.detectedPartnerRc
        if (result.detectedPartnerNif.isNotBlank() && partnerNif.isBlank()) partnerNif = result.detectedPartnerNif
        if (result.detectedPartnerNis.isNotBlank() && partnerNis.isBlank()) partnerNis = result.detectedPartnerNis
        if (result.detectedPartnerAi.isNotBlank() && partnerAi.isBlank()) partnerAi = result.detectedPartnerAi
        if (result.detectedInvoiceNumber.isNotBlank() && invoiceNumber.isBlank()) invoiceNumber = result.detectedInvoiceNumber
        if (result.detectedDate.isNotBlank() && invoiceDate.isBlank()) invoiceDate = result.detectedDate
        if (result.detectedBonNumber.isNotBlank() && bonNumber.isBlank()) bonNumber = result.detectedBonNumber
        if (result.detectedBonDate.isNotBlank() && bonDate.isBlank()) bonDate = result.detectedBonDate
        if (result.detectedOrderRef.isNotBlank() && orderRef.isBlank()) orderRef = result.detectedOrderRef
        if (result.detectedPaymentConditions.isNotBlank()) paymentConditions = result.detectedPaymentConditions

        appliquerTva = result.appliquerTva
        if (result.tauxTva > 0.0) globalTvaRate = result.tauxTva

        when (result.detectedDocumentType.uppercase(Locale.ROOT)) {
            "BL_FOURNISSEUR" -> {
                supplierDocType = OperationType.BL_FOURNISSEUR
                clientDocType = OperationType.BL_CLIENT
            }
            "DEVIS_FOURNISSEUR" -> {
                supplierDocType = OperationType.DEVIS_FOURNISSEUR
                clientDocType = OperationType.DEVIS_CLIENT
            }
            "BON_COMMANDE" -> {
                supplierDocType = OperationType.BL_FOURNISSEUR
                clientDocType = OperationType.BON_COMMANDE
            }
            "FACTURE_CLIENT" -> {
                supplierDocType = OperationType.FACTURE_FOURNISSEUR
                clientDocType = OperationType.FACTURE
            }
            "BL_CLIENT" -> {
                supplierDocType = OperationType.BL_FOURNISSEUR
                clientDocType = OperationType.BL_CLIENT
            }
            else -> {
                supplierDocType = OperationType.FACTURE_FOURNISSEUR
            }
        }

        val distinctVatRates = result.extractedLines.map { it.tauxTva }.distinct()
        if (distinctVatRates.size > 1) {
            useMultiRateTva = true
        }
    }

    fun triggerDocumentAnalysis(pagesToAnalyze: List<Bitmap> = workingPages) {
        if (pagesToAnalyze.isEmpty()) {
            errorMessage = "Veuillez d'abord scanner ou importer au moins une page (Photo, Image ou PDF)."
            return
        }
        isAnalyzing = true
        isRefiningWithAi = false
        errorMessage = null
        analysisProgress = 0.20f
        analysisProgressText = "Étape 1/3 : Préparation de l'image (${pagesToAnalyze.size} page(s))..."

        scope.launch {
            try {
                // Sauvegarde d'une copie originale inaltérable en tâche de fond
                if (savedImmutablePaths.isEmpty() && rawOriginalPages.isNotEmpty()) {
                    savedImmutablePaths = withContext(Dispatchers.IO) {
                        IntelligentOcrEngine.saveImmutableOriginalBitmaps(
                            context = context,
                            pages = rawOriginalPages,
                            archiveId = "scan_${System.currentTimeMillis()}"
                        )
                    }
                }

                // ÉTAPE 1 : Reconnaissance structurelle locale rapide ML Kit
                analysisProgress = 0.50f
                analysisProgressText = "Étape 2/3 : Extraction optique des lignes et colonnes (ML Kit)..."
                val localOutcome = IntelligentOcrEngine.analyzeDocumentPagesLocalFast(
                    context = context,
                    pages = pagesToAnalyze,
                    knownPartners = knownPartners,
                    catalogProducts = catalogProducts,
                    ownCompanyName = companySettings.companyName,
                    columnConfig = columnConfig
                )
                val localResult = localOutcome.getOrThrow()

                val hasLocalLines = localResult.extractedLines.isNotEmpty()
                if (hasLocalLines) {
                    // Affichage immédiat des lignes locales tout en lançant l'affinage Gemini IA en tâche de fond
                    applyOcrResultToUi(localResult, overwriteLines = true)
                    analysisProgress = 0.85f
                    isAnalyzing = false
                    isRefiningWithAi = true
                } else {
                    // Si ML Kit n'a pas encore extrait de lignes, on garde la barre de progression active pendant l'analyse Gemini 3.8 Flash Vision
                    applyOcrResultToUi(localResult, overwriteLines = false)
                    analysisProgress = 0.80f
                    analysisProgressText = "Étape 3/3 : Reconnaissance intelligente par Gemini 3.8 Flash (IA Vision)..."
                }

                val editsBeforeAi = manualCorrectionsCount
                val aiRefined = IntelligentOcrEngine.refineWithAi(
                    context = context,
                    pages = pagesToAnalyze,
                    baseMlResult = localResult,
                    catalogProducts = catalogProducts,
                    ownCompanyName = companySettings.companyName,
                    columnConfig = columnConfig
                )
                isRefiningWithAi = false

                if (aiRefined != null && (aiRefined.extractedLines.isNotEmpty() || !hasLocalLines)) {
                    val userHasNotEditedYet = manualCorrectionsCount == editsBeforeAi
                    val localVerifiedCount = localResult.extractedLines.count { it.confidence == OcrConfidenceLevel.HIGH && !it.hasCalculationMismatch }
                    val aiVerifiedCount = aiRefined.extractedLines.count { it.confidence == OcrConfidenceLevel.HIGH && !it.hasCalculationMismatch }
                    // Ne remplacer les lignes locales vérifiées mathématiquement (Qts × PU = Total) que si l'IA a au moins autant de lignes vérifiées
                    val shouldOverwriteLines = (userHasNotEditedYet && (aiVerifiedCount >= localVerifiedCount || !hasLocalLines)) || extractedLines.isEmpty()
                    applyOcrResultToUi(aiRefined, overwriteLines = shouldOverwriteLines)
                } else if (!hasLocalLines && localResult.rawText.isBlank()) {
                    errorMessage = "Aucun texte lisible détecté sur cette image. Essayez le bouton « Contraste + » ou vérifiez la netteté de la photo."
                }
                analysisProgress = 1.0f
            } catch (e: Exception) {
                errorMessage = "Erreur lors de l'analyse : ${e.localizedMessage ?: "Échec"}"
            } finally {
                isAnalyzing = false
                isRefiningWithAi = false
            }
        }
    }

    fun addImportedBitmaps(newBitmaps: List<Bitmap>, autoAnalyze: Boolean = true) {
        if (newBitmaps.isEmpty()) return
        val updatedRaw = rawOriginalPages + newBitmaps
        val updatedWorking = workingPages + newBitmaps
        rawOriginalPages = updatedRaw
        workingPages = updatedWorking
        currentPageIndex = updatedWorking.lastIndex.coerceAtLeast(0)
        zoomScale = 1f
        panOffset = Offset.Zero
        if (autoAnalyze) {
            triggerDocumentAnalysis(updatedWorking)
        }
    }

    // Sélecteur multi-images (JPG, JPEG, PNG) avec décodage normalisé rapide
    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(10)
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isAnalyzing = true
                errorMessage = null
                analysisProgress = 0.12f
                analysisProgressText = "Chargement et décodage de ${uris.size} image(s)..."
                val loaded = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        InvoiceOcrHelper.loadNormalizedBitmapFromUri(context, uri) ?: try {
                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                BitmapFactory.decodeStream(stream)
                            }
                        } catch (_: Exception) {
                            null
                        }
                    }
                }
                if (loaded.isNotEmpty()) {
                    addImportedBitmaps(loaded, autoAnalyze = true)
                } else {
                    isAnalyzing = false
                    errorMessage = "Impossible de décoder l'image sélectionnée. Veuillez choisir une image JPG ou PNG valide."
                }
            }
        }
    }

    // Sélecteur de fichier PDF multipages
    val pdfDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                isAnalyzing = true
                errorMessage = null
                analysisProgress = 0.15f
                analysisProgressText = "Conversion haute définition des pages du fichier PDF..."
                val renderedPages = IntelligentOcrEngine.renderPdfPagesToBitmaps(context, uri)
                if (renderedPages.isNotEmpty()) {
                    addImportedBitmaps(renderedPages, autoAnalyze = true)
                } else {
                    isAnalyzing = false
                    errorMessage = "Impossible de lire ce fichier PDF ou document vide."
                }
            }
        }
    }

    // Capture Appareil Photo (supporte l'ajout successif de plusieurs pages)
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        val uri = pendingCameraUri
        if (success && uri != null) {
            scope.launch {
                isAnalyzing = true
                errorMessage = null
                analysisProgress = 0.12f
                analysisProgressText = "Chargement de la photo capturée..."
                val bmp = withContext(Dispatchers.IO) {
                    InvoiceOcrHelper.loadNormalizedBitmapFromUri(context, uri) ?: try {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    } catch (_: Exception) {
                        null
                    }
                }
                if (bmp != null) {
                    addImportedBitmaps(listOf(bmp), autoAnalyze = true)
                } else {
                    isAnalyzing = false
                    errorMessage = "Impossible de lire la photo capturée."
                }
            }
        }
    }

    fun launchCameraCapture() {
        try {
            val docsDir = File(context.cacheDir, "documents")
            if (!docsDir.exists()) docsDir.mkdirs()
            val photoFile = File(docsDir, "ocr_intelligent_${System.currentTimeMillis()}.jpg")
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
            errorMessage = "Permission Caméra refusée. Vous pouvez importer une image ou un PDF."
        }
    }

    // Calculs financiers temps réel
    val globalMajorationPct = globalMajorationText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val globalRemisePct = globalRemiseText.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
    val fraisTransport = fraisTransportText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val fraisSupplementaires = fraisSupplementairesText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val autresFrais = autresFraisText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val totalFrais = fraisTransport + fraisSupplementaires + autresFrais

    val activeSelectedLines = extractedLines.filter { it.selected && it.designation.isNotBlank() }
    val globalRemiseFactor = 1.0 - (globalRemisePct / 100.0)

    val calculatedLinesTotalHt = activeSelectedLines.sumOf { line ->
        val lineHt = if (transformationAction == OcrTransformationAction.KEEP_ORIGINAL && !isMajorationApplied) {
            val lineRemiseFactor = 1.0 - (line.remisePct.coerceIn(0.0, 100.0) / 100.0)
            round(line.quantite * line.prixUnitaire * lineRemiseFactor * 100.0) / 100.0
        } else {
            line.totalHt
        }
        round(lineHt * globalRemiseFactor * 100.0) / 100.0
    }

    val calculatedTotalHtWithFees = round((calculatedLinesTotalHt + totalFrais) * 100.0) / 100.0

    val calculatedTotalTva = if (!appliquerTva) {
        0.0
    } else if (useMultiRateTva) {
        val linesTva = activeSelectedLines.sumOf { line ->
            val baseHt = round(line.totalHt * globalRemiseFactor * 100.0) / 100.0
            round(baseHt * (line.tauxTva / 100.0) * 100.0) / 100.0
        }
        val feesTva = round(totalFrais * (globalTvaRate / 100.0) * 100.0) / 100.0
        round((linesTva + feesTva) * 100.0) / 100.0
    } else {
        round(calculatedTotalHtWithFees * (globalTvaRate / 100.0) * 100.0) / 100.0
    }

    val calculatedTotalTtc = round((calculatedTotalHtWithFees + calculatedTotalTva) * 100.0) / 100.0

    // Vérification des lignes ambiguës (Rouge / LOW non vérifiées par l'utilisateur)
    val unverifiedLowConfidenceLines = activeSelectedLines.filter {
        it.confidence == OcrConfidenceLevel.LOW && !it.userVerified
    }

    // Détection d'écart entre le Total HT extrait du document original et la somme HT brute des lignes
    val rawExtractedSumHt = activeSelectedLines.sumOf { round(it.quantite * it.prixUnitaire * 100.0) / 100.0 }
    val docExtractedTotalHt = ocrResult?.extractedTotalHt ?: 0.0
    val hasDocumentTotalDiscrepancy = docExtractedTotalHt > 0.0 && abs(docExtractedTotalHt - rawExtractedSumHt) > 2.0

    MaterialTheme(colorScheme = OcrHighContrastLightColorScheme) {
    CompositionLocalProvider(LocalContentColor provides Color(0xFF0F172A)) {
    if (showArchiveAndMemoryDialog) {
        OcrArchiveAndMemoryModal(
            onDismiss = { showArchiveAndMemoryDialog = false }
        )
    }

    if (showGeminiAssistantDialog) {
        val contextSummary = remember(
            supplierName,
            clientName,
            invoiceNumber,
            extractedLines,
            calculatedTotalHtWithFees,
            calculatedTotalTtc
        ) {
            buildString {
                appendLine("Fournisseur/Partenaire : ${supplierName.ifBlank { "Non renseigné" }}")
                appendLine("Client : ${clientName.ifBlank { "Non renseigné" }}")
                appendLine("N° Facture/Bon : ${invoiceNumber.ifBlank { bonNumber.ifBlank { "Non détecté" } }}")
                appendLine("Nombre d'articles extraits : ${extractedLines.size}")
                extractedLines.take(25).forEachIndexed { idx, line ->
                    appendLine("${idx + 1}. [Réf: ${line.reference}] ${line.designation} | Qts=${line.quantite} | PU HT=${line.prixUnitaire} DA | Total HT=${line.totalHt} DA")
                }
                appendLine("Total HT calculé : ${CurrencyUtils.formatDa(calculatedTotalHtWithFees)}")
                appendLine("Total TTC calculé : ${CurrencyUtils.formatDa(calculatedTotalTtc)}")
            }
        }
        GeminiCommercialAssistantModal(
            pages = workingPages,
            ocrContextSummary = contextSummary,
            onDismiss = { showGeminiAssistantDialog = false }
        )
    }

    val canSaveOcrDocument = extractedLines.isNotEmpty() || ocrResult != null || workingPages.isNotEmpty()

    fun triggerSaveAndExit(openInEditor: Boolean = false) {
        val activeResult = ocrResult ?: InvoiceOcrResult(
            rawText = "",
            reconstructedRows = emptyList(),
            detectedPartnerName = supplierName,
            detectedInvoiceNumber = invoiceNumber,
            detectedDateText = invoiceDate,
            appliquerTva = appliquerTva,
            tauxTva = globalTvaRate,
            extractedLines = extractedLines,
            detectedAddress = partnerAddress,
            detectedPhone = partnerPhone,
            detectedRc = partnerRc,
            detectedNif = partnerNif,
            detectedNis = partnerNis,
            detectedArticleImposition = partnerAi,
            detectedBonNumber = bonNumber,
            detectedBonDate = bonDate,
            detectedOrderReference = orderRef,
            detectedPaymentConditions = paymentConditions
        )
        viewModel.executeIntelligentOcrAction(
            context = context,
            action = transformationAction,
            ocrResult = activeResult.copy(
                detectedAddress = partnerAddress,
                detectedPhone = partnerPhone,
                detectedRc = partnerRc,
                detectedNif = partnerNif,
                detectedNis = partnerNis,
                detectedArticleImposition = partnerAi,
                detectedDateText = invoiceDate,
                detectedBonDate = bonDate,
                detectedOrderReference = orderRef,
                detectedPaymentConditions = paymentConditions
            ),
            editedLines = extractedLines,
            supplierDocType = supplierDocType,
            clientDocType = clientDocType,
            supplierNameInput = supplierName,
            clientNameInput = clientName,
            invoiceNumberInput = invoiceNumber,
            bonNumberInput = bonNumber,
            globalMajorationPct = globalMajorationText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            remiseGlobalePct = globalRemiseText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            appliquerTva = appliquerTva,
            tauxTvaGlobal = globalTvaRate,
            fraisTransport = fraisTransportText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            fraisSupplementaires = fraisSupplementairesText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            autresFrais = autresFraisText.replace(',', '.').toDoubleOrNull() ?: 0.0,
            savedPagePaths = savedImmutablePaths,
            manualCorrectionsSummary = "$manualCorrectionsCount modification(s) manuelle(s)",
            openInEditorInsteadOfSave = openInEditor
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateBack() },
                        modifier = Modifier.testTag("intelligent_ocr_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour",
                            tint = Color.White
                        )
                    }
                },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF93C5FD),
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "OCR Intelligent – Gestion Commerciale",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Gemini 3.8 Flash Vision • Vérification visuelle • Transformation Client",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFCBD5E1),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                },
                actions = {
                    if (canSaveOcrDocument) {
                        Button(
                            onClick = { triggerSaveAndExit(openInEditor = false) },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .testTag("intelligent_ocr_top_save_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Enregistrer le bon et quitter",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Enregistrer",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White
                            )
                        }
                    }
                    FilledTonalButton(
                        onClick = { showGeminiAssistantDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFFEEF2FF),
                            contentColor = Color(0xFF312E81)
                        ),
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .testTag("intelligent_ocr_gemini_assistant_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Assistant Gemini IA",
                            tint = Color(0xFF4338CA),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Gemini IA",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF312E81)
                        )
                    }
                    FilledTonalButton(
                        onClick = { showArchiveAndMemoryDialog = true },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E3A8A),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .testTag("intelligent_ocr_history_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = "Historique & Mémoire IA",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Historique",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        bottomBar = {
            if (canSaveOcrDocument) {
                Surface(
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFCBD5E1))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(0.36f)) {
                            Text(
                                text = "${activeSelectedLines.size.coerceAtLeast(extractedLines.size)} article(s)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF334155),
                                maxLines = 1
                            )
                            Text(
                                text = CurrencyUtils.formatDa(calculatedTotalTtc),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF065F46),
                                maxLines = 1
                            )
                        }

                        OutlinedButton(
                            onClick = { triggerSaveAndExit(openInEditor = true) },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("intelligent_ocr_sticky_editor_btn")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Éditeur",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0F172A)
                            )
                        }

                        Button(
                            onClick = { triggerSaveAndExit(openInEditor = false) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF16A34A),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(0.64f)
                                .testTag("intelligent_ocr_sticky_save_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Enregistrer le Bon & Quitter",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color.White,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC))
        ) {
            // BARRE D'IMPORTATION ET D'ACTION IA (Section 1 & 2)
            Surface(
                color = Color.White,
                tonalElevation = 2.dp,
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Appareil Photo
                        Button(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) launchCameraCapture()
                                else cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E40AF)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("ocr_import_camera_btn")
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (workingPages.isEmpty()) "Scanner Photo" else "+ Page Photo",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // 2. Images JPG / JPEG / PNG (Multipages)
                        FilledTonalButton(
                            onClick = {
                                multiplePhotoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("ocr_import_gallery_btn")
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Images (JPG/PNG)",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        // 3. Import PDF multipages
                        OutlinedButton(
                            onClick = {
                                pdfDocumentLauncher.launch(arrayOf("application/pdf"))
                            },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            modifier = Modifier.testTag("ocr_import_pdf_btn")
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Importer PDF",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFFDC2626)
                            )
                        }

                        // 4. Bouton Analyser le document (IA)
                        if (workingPages.isNotEmpty()) {
                            Button(
                                onClick = { triggerDocumentAnalysis(workingPages) },
                                enabled = !isAnalyzing,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier.testTag("ocr_analyze_ai_btn")
                            ) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Analyser le document (${workingPages.size} p.)",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }

                    // Sélecteur du Choix et de l'Ordre des Colonnes lors du scan (ex: Quantité / Réf / Désignation / P.U.HT)
                    OcrColumnOrderSelectorSection(
                        columnConfig = columnConfig,
                        showCustomEditor = showCustomColumnOrderEditor,
                        onToggleCustomEditor = { showCustomColumnOrderEditor = !showCustomColumnOrderEditor },
                        onColumnConfigChange = { updateColumnConfigAndReparse(it) }
                    )

                    // Barre de progression visuelle OCR + Gemini 3.8 Flash
                    if (isAnalyzing) {
                        val pct = (analysisProgress.coerceIn(0.05f, 1f) * 100).toInt()
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp))
                                .testTag("ocr_visual_progress_banner")
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(18.dp),
                                            strokeWidth = 2.5.dp,
                                            color = Color(0xFF4338CA)
                                        )
                                        Text(
                                            text = analysisProgressText.ifBlank { "Traitement OCR & Gemini IA en cours..." },
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF312E81)
                                        )
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
                                    progress = { analysisProgress.coerceIn(0.05f, 1f) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = Color(0xFF4338CA),
                                    trackColor = Color(0xFFC7D2FE)
                                )
                            }
                        }
                    } else if (isRefiningWithAi) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF0FDF4),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(8.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(15.dp),
                                            strokeWidth = 2.dp,
                                            color = Color(0xFF16A34A)
                                        )
                                        Text(
                                            text = "Extraction rapide (${extractedLines.size} ligne(s)) • Affinage intelligent Gemini 3.8 Flash (85%)...",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF166534)
                                        )
                                    }
                                    TextButton(onClick = { isRefiningWithAi = false }) {
                                        Text("Masquer", style = MaterialTheme.typography.labelSmall)
                                    }
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
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = err,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color(0xFF991B1B),
                                    modifier = Modifier.weight(1f)
                                )
                                TextButton(onClick = { errorMessage = null }) {
                                    Text("Fermer", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }

            // INTERFACE EN DEUX PARTIES (Section 4 : À gauche Document original, À droite Tableau extrait)
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                val isWideScreen = maxWidth >= 700.dp

                Column(modifier = Modifier.fillMaxSize()) {
                    // Sélecteur de disposition sur écran compact (avec défilement horizontal pour éviter tout retour à la ligne vertical)
                    if (!isWideScreen && workingPages.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE2E8F0))
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 10.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Affichage :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF0F172A),
                                maxLines = 1
                            )
                            listOf(
                                0 to "2 Parties (Doc + Tableau)",
                                2 to "Tableau Plein (${extractedLines.size})",
                                1 to "Document Zoom"
                            ).forEach { (mode, label) ->
                                val isSel = compactViewMode == mode
                                FilterChip(
                                    selected = isSel,
                                    onClick = { compactViewMode = mode },
                                    colors = ocrFilterChipColors(),
                                    label = {
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (isSel) Color.White else Color(0xFF0F172A),
                                            maxLines = 1
                                        )
                                    }
                                )
                            }
                        }
                    }

                    if (isWideScreen) {
                        // Écran large / Tablette / Paysage : Côtes à côtes (Gauche : Document | Droite : Tableau)
                        Row(modifier = Modifier.fillMaxSize()) {
                            Box(
                                modifier = Modifier
                                    .weight(0.44f)
                                    .fillMaxHeight()
                            ) {
                                ScannedDocumentViewerPane(
                                    workingPages = workingPages,
                                    rawOriginalPages = rawOriginalPages,
                                    currentPageIndex = currentPageIndex,
                                    onSelectPage = { idx ->
                                        currentPageIndex = idx
                                        zoomScale = 1f
                                        panOffset = Offset.Zero
                                    },
                                    zoomScale = zoomScale,
                                    panOffset = panOffset,
                                    onUpdateZoomPan = { s, o ->
                                        zoomScale = s
                                        panOffset = o
                                    },
                                    isEnhancedContrast = isEnhancedContrast,
                                    onRotateCurrentPage = {
                                        if (currentPageIndex in workingPages.indices) {
                                            val rotated = IntelligentOcrEngine.rotateBitmap(workingPages[currentPageIndex], 90f)
                                            workingPages = workingPages.toMutableList().also { it[currentPageIndex] = rotated }
                                        }
                                    },
                                    onAutoCropCurrentPage = {
                                        if (currentPageIndex in workingPages.indices) {
                                            val cropped = IntelligentOcrEngine.autoCropDocumentMargins(workingPages[currentPageIndex])
                                            workingPages = workingPages.toMutableList().also { it[currentPageIndex] = cropped }
                                        }
                                    },
                                    onToggleEnhanceReadability = {
                                        if (currentPageIndex in workingPages.indices) {
                                            isEnhancedContrast = !isEnhancedContrast
                                            val targetBmp = if (isEnhancedContrast) {
                                                IntelligentOcrEngine.enhanceDocumentReadability(workingPages[currentPageIndex])
                                            } else {
                                                rawOriginalPages.getOrNull(currentPageIndex) ?: workingPages[currentPageIndex]
                                            }
                                            workingPages = workingPages.toMutableList().also { it[currentPageIndex] = targetBmp }
                                        }
                                    },
                                    onRemoveCurrentPage = {
                                        if (workingPages.isNotEmpty() && currentPageIndex in workingPages.indices) {
                                            val newWorking = workingPages.toMutableList().also { it.removeAt(currentPageIndex) }
                                            val newRaw = rawOriginalPages.toMutableList().also {
                                                if (currentPageIndex in it.indices) it.removeAt(currentPageIndex)
                                            }
                                            workingPages = newWorking
                                            rawOriginalPages = newRaw
                                            currentPageIndex = currentPageIndex.coerceAtMost((newWorking.size - 1).coerceAtLeast(0))
                                        }
                                    },
                                    highlightedLine = extractedLines.find { it.id == selectedLineIdForHighlight }
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(Color(0xFFCBD5E1))
                            )

                            Box(
                                modifier = Modifier
                                    .weight(0.56f)
                                    .fillMaxHeight()
                            ) {
                                ExtractedDataAndTransformationPane(
                                    viewModel = viewModel,
                                    ocrResult = ocrResult,
                                    matchedPartnerProfile = matchedPartnerProfile,
                                    extractedLines = extractedLines,
                                    selectedLineIdForHighlight = selectedLineIdForHighlight,
                                    onSelectLineForHighlight = { line ->
                                        selectedLineIdForHighlight = line.id
                                        if (line.pageIndex in workingPages.indices) {
                                            currentPageIndex = line.pageIndex
                                        }
                                    },
                                    onUpdateLines = { updated, isManualEdit ->
                                        extractedLines = updated
                                        if (isManualEdit) manualCorrectionsCount++
                                    },
                                    supplierName = supplierName,
                                    onSupplierNameChange = { supplierName = it },
                                    clientName = clientName,
                                    onClientNameChange = { clientName = it },
                                    partnerAddress = partnerAddress,
                                    onPartnerAddressChange = { partnerAddress = it },
                                    partnerPhone = partnerPhone,
                                    onPartnerPhoneChange = { partnerPhone = it },
                                    partnerRc = partnerRc,
                                    onPartnerRcChange = { partnerRc = it },
                                    partnerNif = partnerNif,
                                    onPartnerNifChange = { partnerNif = it },
                                    partnerNis = partnerNis,
                                    onPartnerNisChange = { partnerNis = it },
                                    partnerAi = partnerAi,
                                    onPartnerAiChange = { partnerAi = it },
                                    invoiceNumber = invoiceNumber,
                                    onInvoiceNumberChange = { invoiceNumber = it },
                                    invoiceDate = invoiceDate,
                                    onInvoiceDateChange = { invoiceDate = it },
                                    bonNumber = bonNumber,
                                    onBonNumberChange = { bonNumber = it },
                                    bonDate = bonDate,
                                    onBonDateChange = { bonDate = it },
                                    orderRef = orderRef,
                                    onOrderRefChange = { orderRef = it },
                                    paymentConditions = paymentConditions,
                                    onPaymentConditionsChange = { paymentConditions = it },
                                    showMetadataDetails = showMetadataDetails,
                                    onToggleMetadataDetails = { showMetadataDetails = !showMetadataDetails },
                                    transformationAction = transformationAction,
                                    onTransformationActionChange = { transformationAction = it },
                                    supplierDocType = supplierDocType,
                                    onSupplierDocTypeChange = { supplierDocType = it },
                                    clientDocType = clientDocType,
                                    onClientDocTypeChange = { clientDocType = it },
                                    globalMajorationText = globalMajorationText,
                                    onGlobalMajorationTextChange = { globalMajorationText = it },
                                    isMajorationApplied = isMajorationApplied,
                                    onApplyMajoration = {
                                        val pct = globalMajorationText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                        extractedLines = extractedLines.map { line ->
                                            line.copy(
                                                majorationPct = if (line.applyMajoration) pct else 0.0
                                            )
                                        }
                                        isMajorationApplied = pct > 0.0
                                    },
                                    onRemoveMajoration = {
                                        extractedLines = extractedLines.map { line ->
                                            line.copy(majorationPct = 0.0)
                                        }
                                        isMajorationApplied = false
                                    },
                                    globalRemiseText = globalRemiseText,
                                    onGlobalRemiseTextChange = { globalRemiseText = it },
                                    appliquerTva = appliquerTva,
                                    onAppliquerTvaChange = { appliquerTva = it },
                                    globalTvaRate = globalTvaRate,
                                    onGlobalTvaRateChange = { rate ->
                                        globalTvaRate = rate
                                        if (!useMultiRateTva) {
                                            extractedLines = extractedLines.map { it.copy(tauxTva = rate) }
                                        }
                                    },
                                    useMultiRateTva = useMultiRateTva,
                                    onUseMultiRateTvaChange = { useMultiRateTva = it },
                                    fraisTransportText = fraisTransportText,
                                    onFraisTransportTextChange = { fraisTransportText = it },
                                    fraisSupplementairesText = fraisSupplementairesText,
                                    onFraisSupplementairesTextChange = { fraisSupplementairesText = it },
                                    autresFraisText = autresFraisText,
                                    onAutresFraisTextChange = { autresFraisText = it },
                                    showCommercialParams = showCommercialParams,
                                    onToggleCommercialParams = { showCommercialParams = !showCommercialParams },
                                    calculatedLinesTotalHt = calculatedLinesTotalHt,
                                    totalFrais = totalFrais,
                                    calculatedTotalHtWithFees = calculatedTotalHtWithFees,
                                    calculatedTotalTva = calculatedTotalTva,
                                    calculatedTotalTtc = calculatedTotalTtc,
                                    hasDocumentTotalDiscrepancy = hasDocumentTotalDiscrepancy,
                                    rawExtractedSumHt = rawExtractedSumHt,
                                    unverifiedLowConfidenceLines = unverifiedLowConfidenceLines,
                                    savedImmutablePaths = savedImmutablePaths,
                                    manualCorrectionsCount = manualCorrectionsCount,
                                    columnConfig = columnConfig,
                                    onColumnConfigChange = { updateColumnConfigAndReparse(it) }
                                )
                            }
                        }
                    } else {
                        // Écran compact : Vue 2 parties (Aperçu compact en haut + Tableau complet en bas sans espace vide)
                        if (compactViewMode == 0) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(if (extractedLines.isNotEmpty()) 136.dp else 185.dp)
                                ) {
                                    ScannedDocumentViewerPane(
                                        workingPages = workingPages,
                                        rawOriginalPages = rawOriginalPages,
                                        currentPageIndex = currentPageIndex,
                                        onSelectPage = { idx ->
                                            currentPageIndex = idx
                                            zoomScale = 1f
                                            panOffset = Offset.Zero
                                        },
                                        zoomScale = zoomScale,
                                        panOffset = panOffset,
                                        onUpdateZoomPan = { s, o ->
                                            zoomScale = s
                                            panOffset = o
                                        },
                                        isEnhancedContrast = isEnhancedContrast,
                                        onRotateCurrentPage = {
                                            if (currentPageIndex in workingPages.indices) {
                                                val rotated = IntelligentOcrEngine.rotateBitmap(workingPages[currentPageIndex], 90f)
                                                workingPages = workingPages.toMutableList().also { it[currentPageIndex] = rotated }
                                            }
                                        },
                                        onAutoCropCurrentPage = {
                                            if (currentPageIndex in workingPages.indices) {
                                                val cropped = IntelligentOcrEngine.autoCropDocumentMargins(workingPages[currentPageIndex])
                                                workingPages = workingPages.toMutableList().also { it[currentPageIndex] = cropped }
                                            }
                                        },
                                        onToggleEnhanceReadability = {
                                            if (currentPageIndex in workingPages.indices) {
                                                isEnhancedContrast = !isEnhancedContrast
                                                val targetBmp = if (isEnhancedContrast) {
                                                    IntelligentOcrEngine.enhanceDocumentReadability(workingPages[currentPageIndex])
                                                } else {
                                                    rawOriginalPages.getOrNull(currentPageIndex) ?: workingPages[currentPageIndex]
                                                }
                                                workingPages = workingPages.toMutableList().also { it[currentPageIndex] = targetBmp }
                                            }
                                        },
                                        onRemoveCurrentPage = {
                                            if (workingPages.isNotEmpty() && currentPageIndex in workingPages.indices) {
                                                val newWorking = workingPages.toMutableList().also { it.removeAt(currentPageIndex) }
                                                val newRaw = rawOriginalPages.toMutableList().also {
                                                    if (currentPageIndex in it.indices) it.removeAt(currentPageIndex)
                                                }
                                                workingPages = newWorking
                                                rawOriginalPages = newRaw
                                                currentPageIndex = currentPageIndex.coerceAtMost((newWorking.size - 1).coerceAtLeast(0))
                                            }
                                        },
                                        highlightedLine = extractedLines.find { it.id == selectedLineIdForHighlight }
                                    )
                                }

                                HorizontalDivider(thickness = 1.dp, color = Color(0xFFCBD5E1))

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                ) {
                                    ExtractedDataAndTransformationPane(
                                        viewModel = viewModel,
                                        ocrResult = ocrResult,
                                        matchedPartnerProfile = matchedPartnerProfile,
                                        extractedLines = extractedLines,
                                        selectedLineIdForHighlight = selectedLineIdForHighlight,
                                        onSelectLineForHighlight = { line ->
                                            selectedLineIdForHighlight = line.id
                                            if (line.pageIndex in workingPages.indices) {
                                                currentPageIndex = line.pageIndex
                                            }
                                        },
                                        onUpdateLines = { updated, isManualEdit ->
                                            extractedLines = updated
                                            if (isManualEdit) manualCorrectionsCount++
                                        },
                                        supplierName = supplierName,
                                        onSupplierNameChange = { supplierName = it },
                                        clientName = clientName,
                                        onClientNameChange = { clientName = it },
                                        partnerAddress = partnerAddress,
                                        onPartnerAddressChange = { partnerAddress = it },
                                        partnerPhone = partnerPhone,
                                        onPartnerPhoneChange = { partnerPhone = it },
                                        partnerRc = partnerRc,
                                        onPartnerRcChange = { partnerRc = it },
                                        partnerNif = partnerNif,
                                        onPartnerNifChange = { partnerNif = it },
                                        partnerNis = partnerNis,
                                        onPartnerNisChange = { partnerNis = it },
                                        partnerAi = partnerAi,
                                        onPartnerAiChange = { partnerAi = it },
                                        invoiceNumber = invoiceNumber,
                                        onInvoiceNumberChange = { invoiceNumber = it },
                                        invoiceDate = invoiceDate,
                                        onInvoiceDateChange = { invoiceDate = it },
                                        bonNumber = bonNumber,
                                        onBonNumberChange = { bonNumber = it },
                                        bonDate = bonDate,
                                        onBonDateChange = { bonDate = it },
                                        orderRef = orderRef,
                                        onOrderRefChange = { orderRef = it },
                                        paymentConditions = paymentConditions,
                                        onPaymentConditionsChange = { paymentConditions = it },
                                        showMetadataDetails = showMetadataDetails,
                                        onToggleMetadataDetails = { showMetadataDetails = !showMetadataDetails },
                                        transformationAction = transformationAction,
                                        onTransformationActionChange = { transformationAction = it },
                                        supplierDocType = supplierDocType,
                                        onSupplierDocTypeChange = { supplierDocType = it },
                                        clientDocType = clientDocType,
                                        onClientDocTypeChange = { clientDocType = it },
                                        globalMajorationText = globalMajorationText,
                                        onGlobalMajorationTextChange = { globalMajorationText = it },
                                        isMajorationApplied = isMajorationApplied,
                                        onApplyMajoration = {
                                            val pct = globalMajorationText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                            extractedLines = extractedLines.map { line ->
                                                line.copy(
                                                    majorationPct = if (line.applyMajoration) pct else 0.0
                                                )
                                            }
                                            isMajorationApplied = pct > 0.0
                                        },
                                        onRemoveMajoration = {
                                            extractedLines = extractedLines.map { line ->
                                                line.copy(majorationPct = 0.0)
                                            }
                                            isMajorationApplied = false
                                        },
                                        globalRemiseText = globalRemiseText,
                                        onGlobalRemiseTextChange = { globalRemiseText = it },
                                        appliquerTva = appliquerTva,
                                        onAppliquerTvaChange = { appliquerTva = it },
                                        globalTvaRate = globalTvaRate,
                                        onGlobalTvaRateChange = { rate ->
                                            globalTvaRate = rate
                                            if (!useMultiRateTva) {
                                                extractedLines = extractedLines.map { it.copy(tauxTva = rate) }
                                            }
                                        },
                                        useMultiRateTva = useMultiRateTva,
                                        onUseMultiRateTvaChange = { useMultiRateTva = it },
                                        fraisTransportText = fraisTransportText,
                                        onFraisTransportTextChange = { fraisTransportText = it },
                                        fraisSupplementairesText = fraisSupplementairesText,
                                        onFraisSupplementairesTextChange = { fraisSupplementairesText = it },
                                        autresFraisText = autresFraisText,
                                        onAutresFraisTextChange = { autresFraisText = it },
                                        showCommercialParams = showCommercialParams,
                                        onToggleCommercialParams = { showCommercialParams = !showCommercialParams },
                                        calculatedLinesTotalHt = calculatedLinesTotalHt,
                                        totalFrais = totalFrais,
                                        calculatedTotalHtWithFees = calculatedTotalHtWithFees,
                                        calculatedTotalTva = calculatedTotalTva,
                                        calculatedTotalTtc = calculatedTotalTtc,
                                        hasDocumentTotalDiscrepancy = hasDocumentTotalDiscrepancy,
                                        rawExtractedSumHt = rawExtractedSumHt,
                                        unverifiedLowConfidenceLines = unverifiedLowConfidenceLines,
                                        savedImmutablePaths = savedImmutablePaths,
                                        manualCorrectionsCount = manualCorrectionsCount,
                                        columnConfig = columnConfig,
                                        onColumnConfigChange = { updateColumnConfigAndReparse(it) }
                                    )
                                }
                            }
                        } else if (compactViewMode == 1) {
                            ScannedDocumentViewerPane(
                                workingPages = workingPages,
                                rawOriginalPages = rawOriginalPages,
                                currentPageIndex = currentPageIndex,
                                onSelectPage = { idx ->
                                    currentPageIndex = idx
                                    zoomScale = 1f
                                    panOffset = Offset.Zero
                                },
                                zoomScale = zoomScale,
                                panOffset = panOffset,
                                onUpdateZoomPan = { s, o ->
                                    zoomScale = s
                                    panOffset = o
                                },
                                isEnhancedContrast = isEnhancedContrast,
                                onRotateCurrentPage = {
                                    if (currentPageIndex in workingPages.indices) {
                                        val rotated = IntelligentOcrEngine.rotateBitmap(workingPages[currentPageIndex], 90f)
                                        workingPages = workingPages.toMutableList().also { it[currentPageIndex] = rotated }
                                    }
                                },
                                onAutoCropCurrentPage = {
                                    if (currentPageIndex in workingPages.indices) {
                                        val cropped = IntelligentOcrEngine.autoCropDocumentMargins(workingPages[currentPageIndex])
                                        workingPages = workingPages.toMutableList().also { it[currentPageIndex] = cropped }
                                    }
                                },
                                onToggleEnhanceReadability = {
                                    if (currentPageIndex in workingPages.indices) {
                                        isEnhancedContrast = !isEnhancedContrast
                                        val targetBmp = if (isEnhancedContrast) {
                                            IntelligentOcrEngine.enhanceDocumentReadability(workingPages[currentPageIndex])
                                        } else {
                                            rawOriginalPages.getOrNull(currentPageIndex) ?: workingPages[currentPageIndex]
                                        }
                                        workingPages = workingPages.toMutableList().also { it[currentPageIndex] = targetBmp }
                                    }
                                },
                                onRemoveCurrentPage = {
                                    if (workingPages.isNotEmpty() && currentPageIndex in workingPages.indices) {
                                        val newWorking = workingPages.toMutableList().also { it.removeAt(currentPageIndex) }
                                        val newRaw = rawOriginalPages.toMutableList().also {
                                            if (currentPageIndex in it.indices) it.removeAt(currentPageIndex)
                                        }
                                        workingPages = newWorking
                                        rawOriginalPages = newRaw
                                        currentPageIndex = currentPageIndex.coerceAtMost((newWorking.size - 1).coerceAtLeast(0))
                                    }
                                },
                                highlightedLine = extractedLines.find { it.id == selectedLineIdForHighlight }
                            )
                        } else {
                            ExtractedDataAndTransformationPane(
                                viewModel = viewModel,
                                ocrResult = ocrResult,
                                matchedPartnerProfile = matchedPartnerProfile,
                                extractedLines = extractedLines,
                                selectedLineIdForHighlight = selectedLineIdForHighlight,
                                onSelectLineForHighlight = { line ->
                                    selectedLineIdForHighlight = line.id
                                    if (line.pageIndex in workingPages.indices) {
                                        currentPageIndex = line.pageIndex
                                    }
                                },
                                onUpdateLines = { updated, isManualEdit ->
                                    extractedLines = updated
                                    if (isManualEdit) manualCorrectionsCount++
                                },
                                supplierName = supplierName,
                                onSupplierNameChange = { supplierName = it },
                                clientName = clientName,
                                onClientNameChange = { clientName = it },
                                partnerAddress = partnerAddress,
                                onPartnerAddressChange = { partnerAddress = it },
                                partnerPhone = partnerPhone,
                                onPartnerPhoneChange = { partnerPhone = it },
                                partnerRc = partnerRc,
                                onPartnerRcChange = { partnerRc = it },
                                partnerNif = partnerNif,
                                onPartnerNifChange = { partnerNif = it },
                                partnerNis = partnerNis,
                                onPartnerNisChange = { partnerNis = it },
                                partnerAi = partnerAi,
                                onPartnerAiChange = { partnerAi = it },
                                invoiceNumber = invoiceNumber,
                                onInvoiceNumberChange = { invoiceNumber = it },
                                invoiceDate = invoiceDate,
                                onInvoiceDateChange = { invoiceDate = it },
                                bonNumber = bonNumber,
                                onBonNumberChange = { bonNumber = it },
                                bonDate = bonDate,
                                onBonDateChange = { bonDate = it },
                                orderRef = orderRef,
                                onOrderRefChange = { orderRef = it },
                                paymentConditions = paymentConditions,
                                onPaymentConditionsChange = { paymentConditions = it },
                                showMetadataDetails = showMetadataDetails,
                                onToggleMetadataDetails = { showMetadataDetails = !showMetadataDetails },
                                transformationAction = transformationAction,
                                onTransformationActionChange = { transformationAction = it },
                                supplierDocType = supplierDocType,
                                onSupplierDocTypeChange = { supplierDocType = it },
                                clientDocType = clientDocType,
                                onClientDocTypeChange = { clientDocType = it },
                                globalMajorationText = globalMajorationText,
                                onGlobalMajorationTextChange = { globalMajorationText = it },
                                isMajorationApplied = isMajorationApplied,
                                onApplyMajoration = {
                                    val pct = globalMajorationText.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                    extractedLines = extractedLines.map { line ->
                                        line.copy(
                                            majorationPct = if (line.applyMajoration) pct else 0.0
                                        )
                                    }
                                    isMajorationApplied = pct > 0.0
                                },
                                onRemoveMajoration = {
                                    extractedLines = extractedLines.map { line ->
                                        line.copy(majorationPct = 0.0)
                                    }
                                    isMajorationApplied = false
                                },
                                globalRemiseText = globalRemiseText,
                                onGlobalRemiseTextChange = { globalRemiseText = it },
                                appliquerTva = appliquerTva,
                                onAppliquerTvaChange = { appliquerTva = it },
                                globalTvaRate = globalTvaRate,
                                onGlobalTvaRateChange = { rate ->
                                    globalTvaRate = rate
                                    if (!useMultiRateTva) {
                                        extractedLines = extractedLines.map { it.copy(tauxTva = rate) }
                                    }
                                },
                                useMultiRateTva = useMultiRateTva,
                                onUseMultiRateTvaChange = { useMultiRateTva = it },
                                fraisTransportText = fraisTransportText,
                                onFraisTransportTextChange = { fraisTransportText = it },
                                fraisSupplementairesText = fraisSupplementairesText,
                                onFraisSupplementairesTextChange = { fraisSupplementairesText = it },
                                autresFraisText = autresFraisText,
                                onAutresFraisTextChange = { autresFraisText = it },
                                showCommercialParams = showCommercialParams,
                                onToggleCommercialParams = { showCommercialParams = !showCommercialParams },
                                calculatedLinesTotalHt = calculatedLinesTotalHt,
                                totalFrais = totalFrais,
                                calculatedTotalHtWithFees = calculatedTotalHtWithFees,
                                calculatedTotalTva = calculatedTotalTva,
                                calculatedTotalTtc = calculatedTotalTtc,
                                hasDocumentTotalDiscrepancy = hasDocumentTotalDiscrepancy,
                                rawExtractedSumHt = rawExtractedSumHt,
                                unverifiedLowConfidenceLines = unverifiedLowConfidenceLines,
                                savedImmutablePaths = savedImmutablePaths,
                                manualCorrectionsCount = manualCorrectionsCount,
                                columnConfig = columnConfig,
                                onColumnConfigChange = { updateColumnConfigAndReparse(it) }
                            )
                        }
                    }
                }
            }
        }
    }
    }
    }
}

/**
 * VOLET GAUCHE / HAUT : Visualiseur du document original scanné avec Zoom, Navigation Multipages,
 * Prétraitement (Pivoter, Recadrer, Améliorer lisibilité) et Surbrillance de la ligne sélectionnée.
 */
@Composable
private fun ScannedDocumentViewerPane(
    workingPages: List<Bitmap>,
    rawOriginalPages: List<Bitmap>,
    currentPageIndex: Int,
    onSelectPage: (Int) -> Unit,
    zoomScale: Float,
    panOffset: Offset,
    onUpdateZoomPan: (Float, Offset) -> Unit,
    isEnhancedContrast: Boolean,
    onRotateCurrentPage: () -> Unit,
    onAutoCropCurrentPage: () -> Unit,
    onToggleEnhanceReadability: () -> Unit,
    onRemoveCurrentPage: () -> Unit,
    highlightedLine: OcrExtractedLine?
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F172A))
    ) {
        // Barre d'outils d'image & multipages
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E293B))
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (workingPages.isEmpty()) "Aucun document"
                else "Page ${currentPageIndex + 1}/${workingPages.size}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            if (workingPages.size > 1) {
                workingPages.indices.forEach { idx ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (idx == currentPageIndex) Color(0xFF3B82F6) else Color(0xFF334155),
                        modifier = Modifier.clickable { onSelectPage(idx) }
                    ) {
                        Text(
                            text = "P.${idx + 1}",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (workingPages.isNotEmpty()) {
                IconButton(
                    onClick = { onUpdateZoomPan((zoomScale + 0.4f).coerceAtMost(4f), panOffset) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ZoomIn, contentDescription = "Zoom +", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = {
                        val next = (zoomScale - 0.4f).coerceAtLeast(1f)
                        onUpdateZoomPan(next, if (next == 1f) Offset.Zero else panOffset)
                    },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.ZoomOut, contentDescription = "Zoom -", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onRotateCurrentPage,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.RotateRight, contentDescription = "Pivoter 90°", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onAutoCropCurrentPage,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.Crop, contentDescription = "Recadrer marges", tint = Color.White, modifier = Modifier.size(18.dp))
                }
                IconButton(
                    onClick = onToggleEnhanceReadability,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        Icons.Default.Contrast,
                        contentDescription = "Améliorer lisibilité",
                        tint = if (isEnhancedContrast) Color(0xFF38BDF8) else Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(
                    onClick = onRemoveCurrentPage,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(Icons.Default.DeleteOutline, contentDescription = "Retirer page", tint = Color(0xFFF87171), modifier = Modifier.size(18.dp))
                }
            }
        }

        // Zone d'affichage du document avec gestes de zoom et surbrillance de zone
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(workingPages.size, currentPageIndex) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (zoomScale * zoom).coerceIn(1f, 4.5f)
                        val newOffset = if (newScale > 1f) panOffset + pan else Offset.Zero
                        onUpdateZoomPan(newScale, newOffset)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val activeBitmap = workingPages.getOrNull(currentPageIndex)
            if (activeBitmap == null) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DocumentScanner,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Document Original Scanné",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFE2E8F0)
                    )
                    Text(
                        text = "Scannez une photo, importez une image (JPG/PNG) ou un fichier PDF multipages pour visualiser et vérifier chaque ligne extraite.",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                        .graphicsLayer(
                            scaleX = zoomScale,
                            scaleY = zoomScale,
                            translationX = panOffset.x,
                            translationY = panOffset.y
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = activeBitmap.asImageBitmap(),
                        contentDescription = "Page ${currentPageIndex + 1}",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )

                    // Surbrillance de la zone correspondante lorsque l'utilisateur sélectionne une ligne
                    val bbox = highlightedLine?.takeIf { it.pageIndex == currentPageIndex }?.boundingBox
                    if (bbox != null) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val bmpRatio = activeBitmap.width.toFloat() / activeBitmap.height.toFloat().coerceAtLeast(1f)
                            val canvasRatio = size.width / size.height.coerceAtLeast(1f)
                            val drawnWidth: Float
                            val drawnHeight: Float
                            val offsetX: Float
                            val offsetY: Float

                            if (bmpRatio > canvasRatio) {
                                drawnWidth = size.width
                                drawnHeight = size.width / bmpRatio
                                offsetX = 0f
                                offsetY = (size.height - drawnHeight) / 2f
                            } else {
                                drawnHeight = size.height
                                drawnWidth = size.height * bmpRatio
                                offsetX = (size.width - drawnWidth) / 2f
                                offsetY = 0f
                            }

                            val rectLeft = offsetX + bbox.left * drawnWidth
                            val rectTop = offsetY + bbox.top * drawnHeight
                            val rectW = (bbox.right - bbox.left).coerceAtLeast(0.05f) * drawnWidth
                            val rectH = (bbox.bottom - bbox.top).coerceAtLeast(0.025f) * drawnHeight

                            drawRect(
                                color = Color(0x55FACC15),
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectW, rectH)
                            )
                            drawRect(
                                color = Color(0xFF2563EB),
                                topLeft = Offset(rectLeft, rectTop),
                                size = Size(rectW, rectH),
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * VOLET DROIT / BAS : Tableau normalisé des données extraites, indicateurs de confiance,
 * outils de correction (fusionner, séparer, permuter colonnes), majoration automatique,
 * TVA multi-taux, frais et transformation Fournisseur -> Client.
 */
@Composable
private fun ExtractedDataAndTransformationPane(
    viewModel: OperationViewModel,
    ocrResult: InvoiceOcrResult?,
    matchedPartnerProfile: OcrPartnerProfile?,
    extractedLines: List<OcrExtractedLine>,
    selectedLineIdForHighlight: String?,
    onSelectLineForHighlight: (OcrExtractedLine) -> Unit,
    onUpdateLines: (List<OcrExtractedLine>, Boolean) -> Unit,
    supplierName: String,
    onSupplierNameChange: (String) -> Unit,
    clientName: String,
    onClientNameChange: (String) -> Unit,
    partnerAddress: String,
    onPartnerAddressChange: (String) -> Unit,
    partnerPhone: String,
    onPartnerPhoneChange: (String) -> Unit,
    partnerRc: String,
    onPartnerRcChange: (String) -> Unit,
    partnerNif: String,
    onPartnerNifChange: (String) -> Unit,
    partnerNis: String,
    onPartnerNisChange: (String) -> Unit,
    partnerAi: String,
    onPartnerAiChange: (String) -> Unit,
    invoiceNumber: String,
    onInvoiceNumberChange: (String) -> Unit,
    invoiceDate: String,
    onInvoiceDateChange: (String) -> Unit,
    bonNumber: String,
    onBonNumberChange: (String) -> Unit,
    bonDate: String,
    onBonDateChange: (String) -> Unit,
    orderRef: String,
    onOrderRefChange: (String) -> Unit,
    paymentConditions: String,
    onPaymentConditionsChange: (String) -> Unit,
    showMetadataDetails: Boolean,
    onToggleMetadataDetails: () -> Unit,
    transformationAction: OcrTransformationAction,
    onTransformationActionChange: (OcrTransformationAction) -> Unit,
    supplierDocType: OperationType,
    onSupplierDocTypeChange: (OperationType) -> Unit,
    clientDocType: OperationType,
    onClientDocTypeChange: (OperationType) -> Unit,
    globalMajorationText: String,
    onGlobalMajorationTextChange: (String) -> Unit,
    isMajorationApplied: Boolean,
    onApplyMajoration: () -> Unit,
    onRemoveMajoration: () -> Unit,
    globalRemiseText: String,
    onGlobalRemiseTextChange: (String) -> Unit,
    appliquerTva: Boolean,
    onAppliquerTvaChange: (Boolean) -> Unit,
    globalTvaRate: Double,
    onGlobalTvaRateChange: (Double) -> Unit,
    useMultiRateTva: Boolean,
    onUseMultiRateTvaChange: (Boolean) -> Unit,
    fraisTransportText: String,
    onFraisTransportTextChange: (String) -> Unit,
    fraisSupplementairesText: String,
    onFraisSupplementairesTextChange: (String) -> Unit,
    autresFraisText: String,
    onAutresFraisTextChange: (String) -> Unit,
    showCommercialParams: Boolean,
    onToggleCommercialParams: () -> Unit,
    calculatedLinesTotalHt: Double,
    totalFrais: Double,
    calculatedTotalHtWithFees: Double,
    calculatedTotalTva: Double,
    calculatedTotalTtc: Double,
    hasDocumentTotalDiscrepancy: Boolean,
    rawExtractedSumHt: Double,
    unverifiedLowConfidenceLines: List<OcrExtractedLine>,
    savedImmutablePaths: List<String>,
    manualCorrectionsCount: Int,
    columnConfig: OcrColumnConfig = OcrColumnConfig(),
    onColumnConfigChange: (OcrColumnConfig) -> Unit = {}
) {
    val context = LocalContext.current
    val clients by viewModel.clientsList.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliersList.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. BANDEAU PROFIL MÉMOIRE DOCUMENTAIRE & COLONNES RECONNUES / CHOISIES (Section 2 & 8)
        if (ocrResult != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEEF2FF),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp))
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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Memory,
                                    contentDescription = null,
                                    tint = Color(0xFF4338CA),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (columnConfig.isAuto) {
                                        "Structure reconnue : ${ocrResult.detectedColumnOrder}"
                                    } else {
                                        "Ordre des colonnes choisi : ${columnConfig.summaryLabel()}"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF312E81)
                                )
                            }
                            if (matchedPartnerProfile != null) {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDCFCE7)
                                ) {
                                    Text(
                                        text = "Mémoire Fournisseur (${matchedPartnerProfile.scansCount} scans)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF166534),
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Boutons directs pour changer l'ordre des colonnes (1er, 2e, 3e, 4e) directement depuis le bandeau
                        OcrDirectColumnTapBar(
                            columnConfig = columnConfig,
                            onColumnConfigChange = onColumnConfigChange
                        )
                    }
                }
            }
        }

        // 2. INFORMATIONS GÉNÉRALES EXTRAITES (Section 3)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleMetadataDetails() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1. Informations Générales & Tiers",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF1E293B)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (showMetadataDetails) "Masquer détails fiscaux" else "NIF, RC, Bon, Adresse...",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF2563EB)
                            )
                            Icon(
                                imageVector = if (showMetadataDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF2563EB)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = supplierName,
                            onValueChange = onSupplierNameChange,
                            label = { Text("Fournisseur émetteur", color = Color(0xFF1E293B)) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            colors = ocrBlackFieldColors(),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("intelligent_ocr_supplier_input")
                        )
                        OutlinedTextField(
                            value = invoiceNumber,
                            onValueChange = onInvoiceNumberChange,
                            label = { Text("N° Facture", color = Color(0xFF1E293B)) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                            colors = ocrBlackFieldColors(),
                            modifier = Modifier
                                .weight(0.75f)
                                .testTag("intelligent_ocr_inv_number_input")
                        )
                    }

                    // Suggestions rapides de fournisseurs connus
                    if (suppliers.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            suppliers.take(8).forEach { sup ->
                                FilterChip(
                                    selected = supplierName.trim().equals(sup.name.trim(), ignoreCase = true),
                                    onClick = { onSupplierNameChange(sup.name) },
                                    label = { Text(sup.name, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = showMetadataDetails) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = invoiceDate,
                                    onValueChange = onInvoiceDateChange,
                                    label = { Text("Date Facture") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = bonNumber,
                                    onValueChange = onBonNumberChange,
                                    label = { Text("N° Bon (BL)") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = orderRef,
                                    onValueChange = onOrderRefChange,
                                    label = { Text("Réf. Commande") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = partnerAddress,
                                    onValueChange = onPartnerAddressChange,
                                    label = { Text("Adresse") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1.3f)
                                )
                                OutlinedTextField(
                                    value = partnerPhone,
                                    onValueChange = onPartnerPhoneChange,
                                    label = { Text("Téléphone") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(0.9f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = partnerNif,
                                    onValueChange = onPartnerNifChange,
                                    label = { Text("NIF") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 11.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = partnerRc,
                                    onValueChange = onPartnerRcChange,
                                    label = { Text("RC") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 11.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = partnerNis,
                                    onValueChange = onPartnerNisChange,
                                    label = { Text("NIS") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 11.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = partnerAi,
                                    onValueChange = onPartnerAiChange,
                                    label = { Text("Art. Imp.") },
                                    singleLine = true,
                                    textStyle = TextStyle(color = Color.Black, fontSize = 11.sp),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. EN-TÊTE DU TABLEAU NORMALISÉ & OUTILS DE CORRECTION EN MASSE (Section 3 & 4)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
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
                        Column {
                            Text(
                                text = "2. Tableau Normalisé des Articles Extraits (${extractedLines.size})",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF1E293B)
                            )
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val highCount = extractedLines.count { it.confidence == OcrConfidenceLevel.HIGH || it.userVerified }
                                val medCount = extractedLines.count { it.confidence == OcrConfidenceLevel.MEDIUM && !it.userVerified }
                                val lowCount = extractedLines.count { it.confidence == OcrConfidenceLevel.LOW && !it.userVerified }
                                Text("🟢 $highCount fiable(s)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534))
                                Text("🟠 $medCount à vérifier", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309))
                                Text("🔴 $lowCount incertain(s)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDC2626))
                            }
                        }

                        Button(
                            onClick = {
                                val newLine = OcrExtractedLine(
                                    reference = "",
                                    designation = "Nouvel article",
                                    quantite = 1.0,
                                    prixUnitaire = 0.0,
                                    tauxTva = globalTvaRate,
                                    majorationPct = if (isMajorationApplied) {
                                        globalMajorationText.replace(',', '.').toDoubleOrNull() ?: 0.0
                                    } else 0.0,
                                    confidence = OcrConfidenceLevel.HIGH,
                                    userVerified = true
                                )
                                onUpdateLines(extractedLines + newLine, true)
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp).testTag("intelligent_ocr_add_line_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Ajouter ligne", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }

                    // En-tête visuel de l'ordre des colonnes actif dans le tableau
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF1F5F9),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Ordre tableau :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF1E3A8A)
                            )
                            columnConfig.displayFields.forEachIndexed { colIndex, colField ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFDBEAFE)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        if (colIndex > 0) {
                                            Text(
                                                text = "◀",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF1E40AF),
                                                modifier = Modifier.clickable {
                                                    onColumnConfigChange(columnConfig.moveField(colField, -1))
                                                }
                                            )
                                        }
                                        Text(
                                            text = "${colIndex + 1}. ${colField.shortLabel}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                            color = Color(0xFF1E3A8A)
                                        )
                                        if (colIndex < columnConfig.displayFields.lastIndex) {
                                            Text(
                                                text = "▶",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF1E40AF),
                                                modifier = Modifier.clickable {
                                                    onColumnConfigChange(columnConfig.moveField(colField, 1))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Actions globales sur les colonnes
                    if (extractedLines.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    onUpdateLines(
                                        extractedLines.map { line ->
                                            line.copy(
                                                quantite = if (line.prixUnitaire > 0.0) line.prixUnitaire else 1.0,
                                                prixUnitaire = line.quantite,
                                                userVerified = true
                                            )
                                        },
                                        true
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Permuter Qts ⇄ PU HT (Tous)", style = MaterialTheme.typography.labelSmall)
                            }

                            OutlinedButton(
                                onClick = {
                                    onUpdateLines(
                                        extractedLines.map { line ->
                                            line.copy(
                                                reference = line.designation.take(20),
                                                designation = line.reference.ifBlank { line.designation },
                                                userVerified = true
                                            )
                                        },
                                        true
                                    )
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(28.dp)
                            ) {
                                Icon(Icons.Default.CompareArrows, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Permuter Réf ⇄ Désignation", style = MaterialTheme.typography.labelSmall)
                            }

                            if (unverifiedLowConfidenceLines.isNotEmpty()) {
                                FilledTonalButton(
                                    onClick = {
                                        onUpdateLines(
                                            extractedLines.map { it.copy(userVerified = true) },
                                            true
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Confirmer toutes les lignes (${unverifiedLowConfidenceLines.size})", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. LISTE DES LIGNES EXTRAITES AVEC INDICATEUR DE CONFIANCE ET OUTILS DE FUSION/SÉPARATION
        if (extractedLines.isEmpty()) {
            item {
                val rawRows = ocrResult?.reconstructedRows.orEmpty()
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF3C7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (rawRows.isNotEmpty()) {
                                "Aucune ligne de tableau standard détectée automatiquement, mais ${rawRows.size} ligne(s) de texte ont été lues sur l'image. Cliquez sur « + Article » ci-dessous pour les ajouter au tableau :"
                            } else {
                                "Aucun article extrait pour le moment. Importez un document (Photo, Image ou PDF) puis cliquez sur « Analyser le document »."
                            },
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF92400E)
                        )

                        if (rawRows.isNotEmpty()) {
                            rawRows.take(25).forEach { rawRow ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = rawRow,
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = Color.Black,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        FilledTonalButton(
                                            onClick = {
                                                val parsed = InvoiceOcrHelper.parseManualRowToLine(rawRow)
                                                onUpdateLines(extractedLines + parsed, true)
                                            },
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Text("+ Article", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            itemsIndexed(
                items = extractedLines,
                key = { _, item -> item.id }
            ) { idx, line ->
                val isHighlighted = line.id == selectedLineIdForHighlight
                val badgeColor = when {
                    line.userVerified || line.confidence == OcrConfidenceLevel.HIGH -> Color(0xFF16A34A)
                    line.confidence == OcrConfidenceLevel.MEDIUM -> Color(0xFFD97706)
                    else -> Color(0xFFDC2626)
                }
                val badgeBgColor = when {
                    line.userVerified || line.confidence == OcrConfidenceLevel.HIGH -> Color(0xFFDCFCE7)
                    line.confidence == OcrConfidenceLevel.MEDIUM -> Color(0xFFFEF3C7)
                    else -> Color(0xFFFEE2E2)
                }
                val badgeLabel = when {
                    line.userVerified -> "Vérifié"
                    line.confidence == OcrConfidenceLevel.HIGH -> "Fiable"
                    line.confidence == OcrConfidenceLevel.MEDIUM -> "À vérifier"
                    else -> "Incertain"
                }

                var qtyText by remember(line.id, line.quantite) {
                    mutableStateOf(CurrencyUtils.formatQty(line.quantite))
                }
                var puText by remember(line.id, line.prixUnitaire) {
                    mutableStateOf(CurrencyUtils.formatQty(line.prixUnitaire))
                }
                var remiseLineText by remember(line.id, line.remisePct) {
                    mutableStateOf(CurrencyUtils.formatQty(line.remisePct))
                }
                var majoLineText by remember(line.id, line.majorationPct) {
                    mutableStateOf(CurrencyUtils.formatQty(line.majorationPct))
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isHighlighted) Color(0xFFEFF6FF) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isHighlighted) 3.dp else 1.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isHighlighted) 2.dp else 1.dp,
                            color = if (isHighlighted) Color(0xFF2563EB) else badgeColor.copy(alpha = 0.45f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        .clickable { onSelectLineForHighlight(line) }
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Ligne supérieure : Checkbox + N° + Badge de confiance + Actions (Fusionner, Séparer, Permuter, Supprimer)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Checkbox(
                                    checked = line.selected,
                                    onCheckedChange = { checked ->
                                        onUpdateLines(
                                            extractedLines.mapIndexed { i, item ->
                                                if (i == idx) item.copy(selected = checked) else item
                                            },
                                            false
                                        )
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                                Text(
                                    text = "#${idx + 1}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF0F172A)
                                )
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = badgeBgColor,
                                    modifier = Modifier.clickable {
                                        onUpdateLines(
                                            extractedLines.mapIndexed { i, item ->
                                                if (i == idx) item.copy(userVerified = true, confidence = OcrConfidenceLevel.HIGH)
                                                else item
                                            },
                                            true
                                        )
                                    }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(badgeColor)
                                        )
                                        Text(
                                            text = badgeLabel,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = badgeColor
                                        )
                                    }
                                }

                                if (line.hasCalculationMismatch) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = Color(0xFFFEE2E2)
                                    ) {
                                        Text(
                                            text = "Écart total ligne",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF991B1B),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }

                            // Boutons d'actions de ligne : Permuter Qts/PU, Fusionner avec suivant, Séparer, Supprimer
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                // Permuter Qts ⇄ PU HT sur cette ligne
                                IconButton(
                                    onClick = {
                                        val newQ = if (line.prixUnitaire > 0.0) line.prixUnitaire else 1.0
                                        val newP = line.quantite
                                        qtyText = CurrencyUtils.formatQty(newQ)
                                        puText = CurrencyUtils.formatQty(newP)
                                        onUpdateLines(
                                            extractedLines.mapIndexed { i, item ->
                                                if (i == idx) item.copy(quantite = newQ, prixUnitaire = newP, userVerified = true)
                                                else item
                                            },
                                            true
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.SwapHoriz,
                                        contentDescription = "Permuter Qts et PU HT",
                                        tint = Color(0xFF4338CA),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Fusionner avec la ligne suivante
                                if (idx < extractedLines.lastIndex) {
                                    IconButton(
                                        onClick = {
                                            val nextLine = extractedLines[idx + 1]
                                            val merged = line.copy(
                                                reference = line.reference.ifBlank { nextLine.reference },
                                                designation = "${line.designation.trim()} ${nextLine.designation.trim()}".trim(),
                                                quantite = if (line.quantite > 0.0 && line.prixUnitaire > 0.0) line.quantite
                                                else if (nextLine.quantite > 0.0) nextLine.quantite else line.quantite,
                                                prixUnitaire = if (line.prixUnitaire > 0.0) line.prixUnitaire else nextLine.prixUnitaire,
                                                userVerified = true
                                            )
                                            val newList = extractedLines.toMutableList()
                                            newList[idx] = merged
                                            newList.removeAt(idx + 1)
                                            onUpdateLines(newList, true)
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.CallMerge,
                                            contentDescription = "Fusionner avec la ligne suivante",
                                            tint = Color(0xFF0284C7),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Séparer la ligne en deux
                                IconButton(
                                    onClick = {
                                        val words = line.designation.trim().split(Regex("\\s+"))
                                        val firstHalf = if (words.size > 1) words.take(words.size / 2).joinToString(" ") else line.designation
                                        val secondHalf = if (words.size > 1) words.drop(words.size / 2).joinToString(" ") else "${line.designation} (Suite)"
                                        val firstItem = line.copy(designation = firstHalf, userVerified = true)
                                        val secondItem = OcrExtractedLine(
                                            pageIndex = line.pageIndex,
                                            reference = "",
                                            designation = secondHalf,
                                            quantite = 1.0,
                                            prixUnitaire = 0.0,
                                            tauxTva = line.tauxTva,
                                            confidence = OcrConfidenceLevel.MEDIUM,
                                            userVerified = true
                                        )
                                        val newList = extractedLines.toMutableList()
                                        newList[idx] = firstItem
                                        newList.add(idx + 1, secondItem)
                                        onUpdateLines(newList, true)
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.CallSplit,
                                        contentDescription = "Séparer la ligne",
                                        tint = Color(0xFFD97706),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                // Supprimer la ligne
                                IconButton(
                                    onClick = {
                                        onUpdateLines(
                                            extractedLines.filterIndexed { i, _ -> i != idx },
                                            true
                                        )
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Supprimer la ligne",
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // Champs de la ligne affichés dans l'ORDRE EXACT choisi par l'utilisateur (ex: 1. Qts | 2. Désignation | 3. P.U.HT)
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
                                        OcrColumnField.DESIGNATION -> if (rowFields.size == 1) 1f else if (rowFields.size == 2) 0.64f else 0.48f
                                        OcrColumnField.PU_HT -> if (rowFields.size == 2) 0.36f else 0.28f
                                        OcrColumnField.QUANTITE -> if (rowFields.size == 2) 0.36f else 0.24f
                                        OcrColumnField.REFERENCE -> if (rowFields.size == 2) 0.36f else 0.25f
                                    }

                                    when (field) {
                                        OcrColumnField.QUANTITE -> {
                                            OutlinedTextField(
                                                value = qtyText,
                                                onValueChange = { txt ->
                                                    qtyText = txt
                                                    val parsed = txt.replace(',', '.').toDoubleOrNull()
                                                    if (parsed != null && parsed >= 0.0) {
                                                        onUpdateLines(
                                                            extractedLines.mapIndexed { i, item ->
                                                                if (i == idx) item.copy(quantite = parsed, userVerified = true) else item
                                                            },
                                                            true
                                                        )
                                                    }
                                                },
                                                label = { Text("$posNumber. Qts", color = Color(0xFF1E293B)) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                                                colors = ocrBlackFieldColors(),
                                                modifier = Modifier.weight(fieldWeight)
                                            )
                                        }
                                        OcrColumnField.DESIGNATION -> {
                                            OutlinedTextField(
                                                value = line.designation,
                                                onValueChange = { newDes ->
                                                    onUpdateLines(
                                                        extractedLines.mapIndexed { i, item ->
                                                            if (i == idx) item.copy(designation = newDes, userVerified = true) else item
                                                        },
                                                        true
                                                    )
                                                },
                                                label = { Text("$posNumber. Désignation", color = Color(0xFF1E293B)) },
                                                maxLines = 2,
                                                textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                                                colors = ocrBlackFieldColors(),
                                                modifier = Modifier.weight(fieldWeight)
                                            )
                                        }
                                        OcrColumnField.PU_HT -> {
                                            OutlinedTextField(
                                                value = puText,
                                                onValueChange = { txt ->
                                                    puText = txt
                                                    val parsed = txt.replace(',', '.').toDoubleOrNull()
                                                    if (parsed != null && parsed >= 0.0) {
                                                        onUpdateLines(
                                                            extractedLines.mapIndexed { i, item ->
                                                                if (i == idx) item.copy(prixUnitaire = parsed, userVerified = true) else item
                                                            },
                                                            true
                                                        )
                                                    }
                                                },
                                                label = { Text("$posNumber. P.U.HT", color = Color(0xFF1E293B)) },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                                textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                                                colors = ocrBlackFieldColors(),
                                                modifier = Modifier.weight(fieldWeight)
                                            )
                                        }
                                        OcrColumnField.REFERENCE -> {
                                            OutlinedTextField(
                                                value = line.reference,
                                                onValueChange = { newRef ->
                                                    onUpdateLines(
                                                        extractedLines.mapIndexed { i, item ->
                                                            if (i == idx) item.copy(reference = newRef, userVerified = true) else item
                                                        },
                                                        true
                                                    )
                                                },
                                                label = { Text("$posNumber. Réf", color = Color(0xFF1E293B)) },
                                                singleLine = true,
                                                textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                                colors = ocrBlackFieldColors(),
                                                modifier = Modifier.weight(fieldWeight)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Champs optionnels Remise % et Majoration %
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = remiseLineText,
                                onValueChange = { txt ->
                                    remiseLineText = txt
                                    val parsed = txt.replace(',', '.').toDoubleOrNull()?.coerceIn(0.0, 100.0) ?: 0.0
                                    onUpdateLines(
                                        extractedLines.mapIndexed { i, item ->
                                            if (i == idx) item.copy(remisePct = parsed, userVerified = true) else item
                                        },
                                        true
                                    )
                                },
                                label = { Text("Remise %", color = Color(0xFF1E293B)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                colors = ocrBlackFieldColors(),
                                modifier = Modifier.weight(1f)
                            )

                            if (transformationAction != OcrTransformationAction.KEEP_ORIGINAL) {
                                OutlinedTextField(
                                    value = majoLineText,
                                    onValueChange = { txt ->
                                        majoLineText = txt
                                        val parsed = txt.replace(',', '.').toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
                                        onUpdateLines(
                                            extractedLines.mapIndexed { i, item ->
                                                if (i == idx) item.copy(majorationPct = parsed, applyMajoration = parsed > 0.0, userVerified = true) else item
                                            },
                                            true
                                        )
                                    },
                                    label = { Text("+Majoration %", color = Color(0xFF4338CA)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(color = Color(0xFF312E81), fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        // Pied de carte de ligne : Prix Vente HT après majoration/remise + Total HT ligne + Sélecteur TVA ligne si multi-taux
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (transformationAction != OcrTransformationAction.KEEP_ORIGINAL && line.majorationPct > 0.0) {
                                        Text(
                                            text = "PU Client HT : ${CurrencyUtils.formatDa(line.puHtNet)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color(0xFF4338CA)
                                        )
                                    }
                                    if (useMultiRateTva && appliquerTva) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            listOf(19.0, 9.0, 0.0).forEach { vRate ->
                                                Surface(
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = if (line.tauxTva == vRate) Color(0xFF2563EB) else Color(0xFFCBD5E1),
                                                    modifier = Modifier.clickable {
                                                        onUpdateLines(
                                                            extractedLines.mapIndexed { i, item ->
                                                                if (i == idx) item.copy(tauxTva = vRate) else item
                                                            },
                                                            true
                                                        )
                                                    }
                                                ) {
                                                    Text(
                                                        text = "TVA ${vRate.toInt()}%",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (line.tauxTva == vRate) Color.White else Color.Black,
                                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Text(
                                    text = "Total HT : ${CurrencyUtils.formatDa(line.totalHt)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. MAJORATION AUTOMATIQUE DES PRIX, REMISES, TVA MULTI-TAUX & FRAIS (Section 6 & 7)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleCommercialParams() },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "3. Majoration Prix, Remises, TVA & Frais",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF312E81)
                        )
                        Icon(
                            imageVector = if (showCommercialParams) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF312E81)
                        )
                    }

                    AnimatedVisibility(visible = showCommercialParams) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Majoration globale en pourcentage
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEEF2FF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Majoration automatique : Prix vente HT = Prix achat HT × (1 + % / 100)",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF312E81)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = globalMajorationText,
                                            onValueChange = onGlobalMajorationTextChange,
                                            label = { Text("Majoration %", color = Color(0xFF1E293B)) },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                            textStyle = TextStyle(color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold),
                                            colors = ocrBlackFieldColors(),
                                            modifier = Modifier
                                                .width(120.dp)
                                                .testTag("intelligent_ocr_majoration_pct_input")
                                        )

                                        Button(
                                            onClick = onApplyMajoration,
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("intelligent_ocr_apply_majoration_btn")
                                        ) {
                                            Text(
                                                text = "Appliquer majoration",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }

                                        OutlinedButton(
                                            onClick = onRemoveMajoration,
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                                            modifier = Modifier.testTag("intelligent_ocr_remove_majoration_btn")
                                        ) {
                                            Text(
                                                text = "Retirer",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("10", "15", "20", "25", "30", "40").forEach { preset ->
                                            FilterChip(
                                                selected = globalMajorationText == preset,
                                                onClick = {
                                                    onGlobalMajorationTextChange(preset)
                                                },
                                                label = { Text("+$preset%", style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }

                            // Remise globale & Frais commerciaux (Transport, Supplémentaires, Autres)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = globalRemiseText,
                                    onValueChange = onGlobalRemiseTextChange,
                                    label = { Text("Remise Glob. %") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = fraisTransportText,
                                    onValueChange = onFraisTransportTextChange,
                                    label = { Text("Frais Transport") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedTextField(
                                    value = fraisSupplementairesText,
                                    onValueChange = onFraisSupplementairesTextChange,
                                    label = { Text("Frais Suppl.") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = autresFraisText,
                                    onValueChange = onAutresFraisTextChange,
                                    label = { Text("Autres Frais") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    textStyle = TextStyle(color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold),
                                    colors = ocrBlackFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Gestion TVA globale ou multi-taux
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "TVA :",
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Switch(
                                        checked = appliquerTva,
                                        onCheckedChange = onAppliquerTvaChange
                                    )
                                    if (appliquerTva) {
                                        FilterChip(
                                            selected = globalTvaRate == 19.0 && !useMultiRateTva,
                                            onClick = {
                                                onUseMultiRateTvaChange(false)
                                                onGlobalTvaRateChange(19.0)
                                            },
                                            label = { Text("19%", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = globalTvaRate == 9.0 && !useMultiRateTva,
                                            onClick = {
                                                onUseMultiRateTvaChange(false)
                                                onGlobalTvaRateChange(9.0)
                                            },
                                            label = { Text("9%", style = MaterialTheme.typography.labelSmall) }
                                        )
                                        FilterChip(
                                            selected = useMultiRateTva,
                                            onClick = { onUseMultiRateTvaChange(!useMultiRateTva) },
                                            label = { Text("Multi-taux", style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 6. COMPARAISON DES TOTAUX EXTRAITS VS TOTAUX CALCULÉS (Section 7 & 10)
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (hasDocumentTotalDiscrepancy) Color(0xFFFEF3C7) else Color(0xFFECFDF5),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (hasDocumentTotalDiscrepancy) Color(0xFFF59E0B) else Color(0xFF10B981),
                        RoundedCornerShape(12.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (hasDocumentTotalDiscrepancy) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color(0xFFB45309),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Écart détecté : Total HT lu sur document = ${CurrencyUtils.formatDa(ocrResult?.extractedTotalHt ?: 0.0)} vs Somme brute lignes = ${CurrencyUtils.formatDa(rawExtractedSumHt)}",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF92400E)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Lignes HT :", style = MaterialTheme.typography.labelMedium, color = Color(0xFF1E293B))
                        Text(CurrencyUtils.formatDa(calculatedLinesTotalHt), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                    }
                    if (totalFrais > 0.0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Frais (Transport / Suppl.) :", style = MaterialTheme.typography.labelSmall, color = Color(0xFF475569))
                            Text(CurrencyUtils.formatDa(totalFrais), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        }
                    }
                    if (appliquerTva) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (useMultiRateTva) "Montant TVA (Multi-taux) :" else "Montant TVA (${globalTvaRate.toInt()}%) :",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF475569)
                            )
                            Text(CurrencyUtils.formatDa(calculatedTotalTva), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = Color.Black)
                        }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TOTAL TTC CALCULÉ :",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = CurrencyUtils.formatDa(calculatedTotalTtc),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                            color = Color(0xFF065F46)
                        )
                    }
                }
            }
        }

        // 7. TRANSFORMATION FOURNISSEUR VERS CLIENT & VALIDATION FINALE (Section 5 & 12)
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "4. Action Finale : Conserver ou Transformer en Document Client",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF1E293B)
                    )

                    // Les 3 modes A, B, C
                    OcrTransformationAction.entries.forEach { mode ->
                        val isSelected = transformationAction == mode
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) Color(0xFFEEF2FF) else Color(0xFFF8FAFC),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) Color(0xFF4338CA) else Color(0xFFCBD5E1),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable { onTransformationActionChange(mode) }
                                .testTag("ocr_action_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Default.DocumentScanner,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF4338CA) else Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                                Column {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold),
                                        color = if (isSelected) Color(0xFF312E81) else Color(0xFF1E293B)
                                    )
                                    Text(
                                        text = mode.description,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    // Sélection du type de document fournisseur (si A ou C)
                    if (transformationAction == OcrTransformationAction.KEEP_ORIGINAL ||
                        transformationAction == OcrTransformationAction.SAVE_AND_TRANSFORM
                    ) {
                        Text(
                            text = "Type du document Fournisseur (Onglet Achat) :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFB45309)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                OperationType.FACTURE_FOURNISSEUR to "Facture Fournisseur",
                                OperationType.BL_FOURNISSEUR to "Bon de Livraison Fourn.",
                                OperationType.DEVIS_FOURNISSEUR to "Devis Fournisseur"
                            ).forEach { (opType, lbl) ->
                                FilterChip(
                                    selected = supplierDocType == opType,
                                    onClick = { onSupplierDocTypeChange(opType) },
                                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }

                    // Sélection du type de document client et du Client destinataire (si B ou C)
                    if (transformationAction == OcrTransformationAction.TRANSFORM_TO_CLIENT ||
                        transformationAction == OcrTransformationAction.SAVE_AND_TRANSFORM
                    ) {
                        Text(
                            text = "Type du document Client à générer (Onglet Vente) :",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF1D4ED8)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                OperationType.BL_CLIENT to "Bon de Livraison Client",
                                OperationType.FACTURE to "Facture Client",
                                OperationType.DEVIS_CLIENT to "Devis Client",
                                OperationType.BON_COMMANDE to "Bon de Commande Client"
                            ).forEach { (opType, lbl) ->
                                FilterChip(
                                    selected = clientDocType == opType,
                                    onClick = { onClientDocTypeChange(opType) },
                                    label = { Text(lbl, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = clientName,
                            onValueChange = onClientNameChange,
                            label = { Text("Client destinataire (Vente)", color = Color(0xFF1E293B)) },
                            placeholder = { Text("Client Comptoir", color = Color(0xFF94A3B8)) },
                            singleLine = true,
                            textStyle = TextStyle(color = Color.Black, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                            colors = ocrBlackFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("intelligent_ocr_client_input")
                        )

                        if (clients.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                clients.take(10).forEach { cli ->
                                    FilterChip(
                                        selected = clientName.trim().equals(cli.name.trim(), ignoreCase = true),
                                        onClick = { onClientNameChange(cli.name) },
                                        label = { Text(cli.name, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    // Avertissement si des lignes Rouge (LOW) n'ont pas encore été vérifiées
                    if (unverifiedLowConfidenceLines.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = Color(0xFFDC2626),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${unverifiedLowConfidenceLines.size} ligne(s) incertaine(s) (Rouge) doivent être vérifiées avant validation.",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color(0xFF991B1B)
                                    )
                                }
                                TextButton(
                                    onClick = {
                                        onUpdateLines(
                                            extractedLines.map { it.copy(userVerified = true) },
                                            true
                                        )
                                    }
                                ) {
                                    Text("Tout valider", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold))
                                }
                            }
                        }
                    }

                    // Boutons finaux : Ouvrir dans l'éditeur ou Enregistrer directement
                    val activeResult = ocrResult ?: InvoiceOcrResult(
                        rawText = "",
                        reconstructedRows = emptyList(),
                        detectedPartnerName = supplierName,
                        detectedInvoiceNumber = invoiceNumber,
                        detectedDateText = invoiceDate,
                        appliquerTva = appliquerTva,
                        tauxTva = globalTvaRate,
                        extractedLines = extractedLines,
                        detectedAddress = partnerAddress,
                        detectedPhone = partnerPhone,
                        detectedRc = partnerRc,
                        detectedNif = partnerNif,
                        detectedNis = partnerNis,
                        detectedArticleImposition = partnerAi,
                        detectedBonNumber = bonNumber,
                        detectedBonDate = bonDate,
                        detectedOrderReference = orderRef,
                        detectedPaymentConditions = paymentConditions
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.executeIntelligentOcrAction(
                                    context = context,
                                    action = transformationAction,
                                    ocrResult = activeResult.copy(
                                        detectedAddress = partnerAddress,
                                        detectedPhone = partnerPhone,
                                        detectedRc = partnerRc,
                                        detectedNif = partnerNif,
                                        detectedNis = partnerNis,
                                        detectedArticleImposition = partnerAi,
                                        detectedDateText = invoiceDate,
                                        detectedBonDate = bonDate,
                                        detectedOrderReference = orderRef,
                                        detectedPaymentConditions = paymentConditions
                                    ),
                                    editedLines = extractedLines,
                                    supplierDocType = supplierDocType,
                                    clientDocType = clientDocType,
                                    supplierNameInput = supplierName,
                                    clientNameInput = clientName,
                                    invoiceNumberInput = invoiceNumber,
                                    bonNumberInput = bonNumber,
                                    globalMajorationPct = globalMajorationText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    remiseGlobalePct = globalRemiseText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    appliquerTva = appliquerTva,
                                    tauxTvaGlobal = globalTvaRate,
                                    fraisTransport = fraisTransportText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    fraisSupplementaires = fraisSupplementairesText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    autresFrais = autresFraisText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    savedPagePaths = savedImmutablePaths,
                                    manualCorrectionsSummary = "$manualCorrectionsCount modification(s) manuelle(s)",
                                    openInEditorInsteadOfSave = true
                                )
                            },
                            enabled = extractedLines.isNotEmpty() || ocrResult != null,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.42f)
                                .testTag("intelligent_ocr_open_editor_btn")
                        ) {
                            Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Éditeur",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.executeIntelligentOcrAction(
                                    context = context,
                                    action = transformationAction,
                                    ocrResult = activeResult.copy(
                                        detectedAddress = partnerAddress,
                                        detectedPhone = partnerPhone,
                                        detectedRc = partnerRc,
                                        detectedNif = partnerNif,
                                        detectedNis = partnerNis,
                                        detectedArticleImposition = partnerAi,
                                        detectedDateText = invoiceDate,
                                        detectedBonDate = bonDate,
                                        detectedOrderReference = orderRef,
                                        detectedPaymentConditions = paymentConditions
                                    ),
                                    editedLines = extractedLines,
                                    supplierDocType = supplierDocType,
                                    clientDocType = clientDocType,
                                    supplierNameInput = supplierName,
                                    clientNameInput = clientName,
                                    invoiceNumberInput = invoiceNumber,
                                    bonNumberInput = bonNumber,
                                    globalMajorationPct = globalMajorationText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    remiseGlobalePct = globalRemiseText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    appliquerTva = appliquerTva,
                                    tauxTvaGlobal = globalTvaRate,
                                    fraisTransport = fraisTransportText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    fraisSupplementaires = fraisSupplementairesText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    autresFrais = autresFraisText.replace(',', '.').toDoubleOrNull() ?: 0.0,
                                    savedPagePaths = savedImmutablePaths,
                                    manualCorrectionsSummary = "$manualCorrectionsCount modification(s) manuelle(s)",
                                    openInEditorInsteadOfSave = false
                                )
                            },
                            enabled = extractedLines.isNotEmpty() || ocrResult != null,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(0.58f)
                                .testTag("intelligent_ocr_save_final_btn")
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (transformationAction) {
                                    OcrTransformationAction.KEEP_ORIGINAL -> "Enregistrer Document"
                                    OcrTransformationAction.TRANSFORM_TO_CLIENT -> "Générer Doc Client"
                                    OcrTransformationAction.SAVE_AND_TRANSFORM -> "Enregistrer & Transformer"
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.ExtraBold)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialogue d'Historique & Archivage OCR (Section 9) et de Mémoire des Fournisseurs/Clients (Section 8).
 */
@Composable
private fun OcrArchiveAndMemoryModal(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Historique Archives, 1: Mémoire Fournisseurs
    var searchQuery by remember { mutableStateOf("") }
    var archives by remember { mutableStateOf<List<OcrArchiveRecord>>(emptyList()) }
    var profiles by remember { mutableStateOf<List<OcrPartnerProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            archives = IntelligentOcrEngine.loadArchiveHistory(context)
            profiles = IntelligentOcrEngine.loadAllPartnerProfiles(context)
        }
    }

    val filteredArchives = remember(archives, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.FRENCH)
        if (q.isEmpty()) archives
        else archives.filter {
            it.partnerName.lowercase(Locale.FRENCH).contains(q) ||
                it.targetClientName.lowercase(Locale.FRENCH).contains(q) ||
                it.invoiceNumber.lowercase(Locale.FRENCH).contains(q) ||
                it.bonNumber.lowercase(Locale.FRENCH).contains(q) ||
                it.documentType.lowercase(Locale.FRENCH).contains(q) ||
                it.documentDate.lowercase(Locale.FRENCH).contains(q)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = "Historique OCR & Mémoire Documentaire IA",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                )
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("Archives (${archives.size})") }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("Profils Fournisseurs (${profiles.size})") }
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Rechercher par date, N°, fournisseur, client, type...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (filteredArchives.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aucun document archivé correspondant.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(filteredArchives) { _, rec ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = rec.partnerName,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = DateUtils.formatDateTime(rec.timestamp),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                        Text(
                                            text = "Type: ${rec.documentType} • Facture: ${rec.invoiceNumber.ifBlank { "-" }} • ${rec.linesCount} art. • TTC: ${CurrencyUtils.formatDa(rec.totalTtc)}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                            color = Color(0xFF334155)
                                        )
                                        if (rec.originalDocId.isNotBlank() || rec.transformedClientDocId.isNotBlank()) {
                                            Text(
                                                text = buildString {
                                                    if (rec.originalDocId.isNotBlank()) append("Doc Achat: ${rec.originalDocId} ")
                                                    if (rec.transformedClientDocId.isNotBlank()) append("→ Doc Client: ${rec.transformedClientDocId} (${rec.targetClientName})")
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = Color(0xFF2563EB)
                                            )
                                        }
                                        Text(
                                            text = "${rec.manualCorrectionsSummary} • ${rec.pageImagePaths.size} page(s) originale(s) archivée(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF059669)
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (profiles.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Aucun profil fournisseur mémorisé pour le moment.\nLes profils se créent automatiquement après chaque validation.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(profiles) { _, prof ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEEF2FF),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, Color(0xFFC7D2FE), RoundedCornerShape(10.dp))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = prof.partnerName,
                                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                                                color = Color(0xFF312E81)
                                            )
                                            Text(
                                                text = "${prof.scansCount} scan(s)",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFF4338CA)
                                            )
                                        }
                                        Text(
                                            text = "Colonnes habituelles : ${prof.preferredColumnOrder}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF1E293B)
                                        )
                                        Text(
                                            text = "Majoration habituelle : +${CurrencyUtils.formatQty(prof.defaultMajorationPct)}% • TVA : ${prof.defaultTvaRate.toInt()}% • ${prof.learnedAliases.size} article(s) mémorisé(s)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

@Composable
private fun GeminiCommercialAssistantModal(
    pages: List<Bitmap>,
    ocrContextSummary: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var questionInput by remember { mutableStateOf("") }
    var aiAnswer by remember { mutableStateOf("") }
    var isStreaming by remember { mutableStateOf(false) }
    var streamError by remember { mutableStateOf<String?>(null) }

    fun sendQuestion(prompt: String) {
        if (prompt.isBlank() || isStreaming) return
        isStreaming = true
        streamError = null
        aiAnswer = ""
        scope.launch {
            try {
                IntelligentOcrEngine.askGeminiCommercialAssistantStream(
                    userQuestion = prompt,
                    pages = pages,
                    ocrContextSummary = ocrContextSummary
                ).collect { chunk ->
                    aiAnswer += chunk
                }
            } catch (e: Exception) {
                streamError = "Erreur Gemini IA : ${e.localizedMessage ?: "vérifiez votre connexion internet"}"
            } finally {
                isStreaming = false
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF4338CA)
                )
                Column {
                    Text(
                        text = "Assistant IA Gemini 3.8 Flash",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold)
                    )
                    Text(
                        text = "Analyse multimodale des factures, calculs, marges et conseils commerciaux",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
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
                Text(
                    text = "Questions rapides Gemini :",
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
                        "Vérifie tous les calculs HT/TVA/TTC de ce document",
                        "Résume les articles et propose une majoration client optimale",
                        "Identifie les informations fiscales du fournisseur (RC, NIF, NIS)"
                    ).forEach { preset ->
                        FilledTonalButton(
                            onClick = {
                                questionInput = preset
                                sendQuestion(preset)
                            },
                            enabled = !isStreaming,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(preset, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = questionInput,
                    onValueChange = { questionInput = it },
                    label = { Text("Posez votre question à Gemini 3.8 Flash") },
                    placeholder = { Text("Ex: Quels sont les articles les plus coûteux sur cette facture ?") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                    colors = ocrBlackFieldColors()
                )

                Button(
                    onClick = { sendQuestion(questionInput) },
                    enabled = !isStreaming && questionInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isStreaming) "Réflexion Gemini en cours..." else "Interroger Gemini 3.8 Flash",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                if (isStreaming) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFF4338CA),
                        trackColor = Color(0xFFC7D2FE)
                    )
                }

                streamError?.let { err ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFFEE2E2),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = err,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF991B1B),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                if (aiAnswer.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF8FAFC),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Réponse de Gemini 3.8 Flash :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                color = Color(0xFF4338CA)
                            )
                            Text(
                                text = aiAnswer,
                                style = MaterialTheme.typography.bodySmall.copy(color = Color(0xFF0F172A))
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Fermer")
            }
        }
    )
}

private val OcrHighContrastLightColorScheme = lightColorScheme(
    primary = Color(0xFF1E40AF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDBEAFE),
    onPrimaryContainer = Color(0xFF1E3A8A),
    secondary = Color(0xFF4338CA),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEEF2FF),
    onSecondaryContainer = Color(0xFF312E81),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF1E293B),
    outline = Color(0xFF64748B)
)

@Composable
private fun ocrFilterChipColors() = FilterChipDefaults.filterChipColors(
    containerColor = Color.White,
    labelColor = Color(0xFF0F172A),
    iconColor = Color(0xFF1E40AF),
    selectedContainerColor = Color(0xFF1E40AF),
    selectedLabelColor = Color.White,
    selectedLeadingIconColor = Color.White
)

@Composable
private fun ocrBlackFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = Color.Black,
    unfocusedTextColor = Color.Black,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedBorderColor = Color(0xFF2563EB),
    unfocusedBorderColor = Color(0xFFCBD5E1),
    focusedLabelColor = Color(0xFF1E293B),
    unfocusedLabelColor = Color(0xFF475569),
    cursorColor = Color.Black
)

private fun formatOrdinalPosition(pos: Int): String {
    return if (pos == 1) "1er" else "${pos}e"
}

@Composable
fun OcrDirectColumnTapBar(
    columnConfig: OcrColumnConfig,
    onColumnConfigChange: (OcrColumnConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        // Ligne principale : les 4 colonnes (Qts, Désignation, P.U.HT, Réf) à toucher dans l'ordre voulu (1er, 2e, 3e, 4e)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choisir l'ordre :",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1E3A8A),
                maxLines = 1
            )

            OcrColumnField.entries.forEach { field ->
                val selectedPos = columnConfig.selectedPositionOf(field)
                val isSelected = selectedPos != null
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) Color(0xFF1E40AF) else Color.White,
                    modifier = Modifier
                        .border(
                            width = if (isSelected) 1.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF94A3B8),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            onColumnConfigChange(columnConfig.toggleFieldInOrder(field))
                        }
                        .testTag("ocr_tap_col_${field.name.lowercase(Locale.ROOT)}_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        if (selectedPos != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color.White
                            ) {
                                Text(
                                    text = formatOrdinalPosition(selectedPos),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp
                                    ),
                                    color = Color(0xFF1E40AF),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                )
                            }
                        }
                        Text(
                            text = field.shortLabel,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = if (isSelected) Color.White else Color(0xFF0F172A),
                            maxLines = 1
                        )
                    }
                }
            }

            if (!columnConfig.isAuto) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFFEE2E2),
                    modifier = Modifier
                        .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(8.dp))
                        .clickable {
                            onColumnConfigChange(OcrColumnConfig(isAuto = true))
                        }
                        .testTag("ocr_reset_column_order_btn")
                ) {
                    Text(
                        text = "↺ Réinitialiser",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF991B1B),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFDBEAFE),
                    modifier = Modifier.border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(8.dp))
                ) {
                    Text(
                        text = "Auto IA actif (touchez 1er, 2e...)",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E3A8A),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun OcrColumnOrderSelectorSection(
    columnConfig: OcrColumnConfig,
    showCustomEditor: Boolean,
    onToggleCustomEditor: () -> Unit,
    onColumnConfigChange: (OcrColumnConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFF1F5F9),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(10.dp))
            .testTag("ocr_column_order_selector_section")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // 1. Sélection directe par clic (1er : Qts, 2e : Désignation, 3e : P.U.HT, 4e : Réf) toujours visible
            OcrDirectColumnTapBar(
                columnConfig = columnConfig,
                onColumnConfigChange = onColumnConfigChange
            )

            // 2. Résumé de l'ordre choisi avec flèches ◀ ▶ + bouton Modèles prédéfinis
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!columnConfig.isAuto) {
                    val fieldsToShow = columnConfig.activeFields
                    fieldsToShow.forEachIndexed { idx, field ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFEEF2FF),
                            modifier = Modifier.border(1.dp, Color(0xFFA5B4FC), RoundedCornerShape(6.dp))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (idx > 0) {
                                    Text(
                                        text = "◀",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier
                                            .clickable {
                                                onColumnConfigChange(columnConfig.moveField(field, -1))
                                            }
                                            .testTag("ocr_col_move_left_${field.name.lowercase(Locale.ROOT)}")
                                    )
                                }
                                Text(
                                    text = "${formatOrdinalPosition(idx + 1)} : ${field.shortLabel}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color(0xFF1E1B4B),
                                    maxLines = 1
                                )
                                if (idx < fieldsToShow.lastIndex) {
                                    Text(
                                        text = "▶",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.ExtraBold),
                                        color = Color(0xFF1E40AF),
                                        modifier = Modifier
                                            .clickable {
                                                onColumnConfigChange(columnConfig.moveField(field, 1))
                                            }
                                            .testTag("ocr_col_move_right_${field.name.lowercase(Locale.ROOT)}")
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "Astuce : touchez ex. « Qts » (1er), puis « Désignation » (2e), puis « P.U.HT » (3e)",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF334155),
                        maxLines = 1
                    )
                }

                OutlinedButton(
                    onClick = onToggleCustomEditor,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(26.dp)
                        .testTag("ocr_toggle_custom_column_order_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapHoriz,
                        contentDescription = null,
                        tint = Color(0xFF1E40AF),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (showCustomEditor) "Masquer modèles" else "Modèles d'ordre",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1E40AF),
                        maxLines = 1
                    )
                }
            }

            // 3. Modèles prédéfinis rapides (dépliable)
            AnimatedVisibility(visible = showCustomEditor) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OcrColumnPreset.entries.forEach { preset ->
                        val isSelected = columnConfig.matchesPreset(preset)
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                onColumnConfigChange(OcrColumnConfig.fromPreset(preset))
                            },
                            colors = ocrFilterChipColors(),
                            label = {
                                Text(
                                    text = preset.shortTitle,
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) Color.White else Color(0xFF0F172A),
                                    maxLines = 1
                                )
                            },
                            modifier = Modifier.testTag("ocr_preset_${preset.name.lowercase(Locale.ROOT)}_chip")
                        )
                    }
                }
            }
        }
    }
}

