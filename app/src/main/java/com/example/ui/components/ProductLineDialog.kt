package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.example.data.model.ProductCatalogEntity
import com.example.ui.viewmodel.LineEditorState
import com.example.util.BarcodeScannerHelper
import com.example.util.CurrencyUtils

@Composable
fun ProductLineDialog(
    state: LineEditorState,
    recentProducts: List<String>,
    catalogProducts: List<ProductCatalogEntity> = emptyList(),
    isPurchaseDocument: Boolean = false,
    onProduitChanged: (String) -> Unit,
    onQuantiteChanged: (String) -> Unit,
    onPrixUnitaireChanged: (String) -> Unit,
    onAutoReopenChanged: (Boolean) -> Unit,
    onSaveAndNext: () -> Unit,
    onSaveAndClose: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!state.isVisible) return

    val context = LocalContext.current
    val currentQty = CurrencyUtils.parseAmount(state.quantiteText).coerceAtLeast(0.0)
    val currentPu = if (state.prixUnitaireText.isNotBlank()) CurrencyUtils.parseAmount(state.prixUnitaireText) else 0.0
    val lineSubtotal = currentQty * currentPu

    val typedQuery = state.produit.trim()

    // Filtrage en temps réel des articles du catalogue en rapport avec ce que l'utilisateur tape (ex: "disj" -> tous les disjoncteurs)
    val matchingCatalogProducts = remember(typedQuery, catalogProducts) {
        if (typedQuery.isBlank()) {
            emptyList()
        } else {
            val tokens = typedQuery.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            catalogProducts.filter { prod ->
                val searchable = "${prod.name} ${prod.reference} ${prod.category} ${prod.marque} ${prod.barcode}".lowercase()
                tokens.all { token -> searchable.contains(token) }
            }.take(25)
        }
    }

    // Suggestions hors catalogue (historique récent ou défaut) correspondant à la saisie
    val matchingFallbackNames = remember(typedQuery, recentProducts, catalogProducts) {
        val catalogNamesLower = catalogProducts.map { it.name.lowercase() }.toSet()
        val baseList = if (recentProducts.isNotEmpty()) {
            recentProducts
        } else {
            listOf(
                "Câble 2.5mm",
                "Disjoncteur 16A",
                "Disjoncteur Différentiel 32A",
                "Peinture Satinée",
                "Enduit de Lissage",
                "Prise Murale",
                "Tube PVC 40mm",
                "Ampoule LED 12W"
            )
        }
        if (typedQuery.isBlank()) {
            baseList.take(12)
        } else {
            baseList.filter { name ->
                name.contains(typedQuery, ignoreCase = true) && !catalogNamesLower.contains(name.lowercase())
            }.take(10)
        }
    }

    val isExactSingleMatch = matchingCatalogProducts.size == 1 &&
        matchingCatalogProducts.first().name.equals(typedQuery, ignoreCase = true)

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("product_line_dialog"),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Inventory,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Ligne Produit",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Fermer")
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Produit input avec recherche prédictive en direct
                OutlinedTextField(
                    value = state.produit,
                    onValueChange = onProduitChanged,
                    label = { Text("Article / Produit *") },
                    placeholder = { Text("Ex: tapez « disj » pour voir tous les disjoncteurs...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_name_input"),
                    singleLine = true,
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (state.produit.isNotEmpty()) {
                                IconButton(
                                    onClick = { onProduitChanged("") },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Effacer",
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    BarcodeScannerHelper.scanBarcode(
                                        context = context,
                                        onSuccess = { code ->
                                            onProduitChanged(code)
                                        }
                                    )
                                },
                                modifier = Modifier.testTag("scan_line_input_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scanner code-barres",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    },
                    isError = state.produitError != null,
                    supportingText = state.produitError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Liste déroulante des suggestions en rapport avec ce que l'utilisateur tape (ex: "disj")
                if (typedQuery.isNotBlank() && (!isExactSingleMatch || matchingFallbackNames.isNotEmpty()) &&
                    (matchingCatalogProducts.isNotEmpty() || matchingFallbackNames.isNotEmpty())
                ) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                RoundedCornerShape(10.dp)
                            )
                            .testTag("product_live_suggestions_list")
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 185.dp)
                                .verticalScroll(rememberScrollState())
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "Suggestions pour « $typedQuery » (${matchingCatalogProducts.size + matchingFallbackNames.size}) :",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                            matchingCatalogProducts.forEachIndexed { idx, prod ->
                                val suggestedPrice: Double = if (isPurchaseDocument && (prod.prixAchat ?: 0.0) > 0.0) {
                                    prod.prixAchat ?: 0.0
                                } else {
                                    prod.defaultUnitPrice ?: prod.prixVente ?: 0.0
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onProduitChanged(prod.name)
                                            if (suggestedPrice > 0.0 && !prod.isArticleEclate) {
                                                onPrixUnitaireChanged(CurrencyUtils.formatQty(suggestedPrice))
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = prod.name,
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        val metaParts = buildList {
                                            if (prod.reference.isNotBlank()) add("Réf: ${prod.reference}")
                                            if (prod.category.isNotBlank()) add("Famille: ${prod.category}")
                                            if (prod.marque.isNotBlank()) add("Marque: ${prod.marque}")
                                            add("Stock: ${CurrencyUtils.formatQty(prod.stockActuel)} ${prod.unit}")
                                        }
                                        Text(
                                            text = metaParts.joinToString(" • "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = CurrencyUtils.formatDa(suggestedPrice),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                                if (idx < matchingCatalogProducts.lastIndex || matchingFallbackNames.isNotEmpty()) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }

                            matchingFallbackNames.forEachIndexed { idx, fallbackName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onProduitChanged(fallbackName) }
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Search,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = fallbackName,
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                }
                                if (idx < matchingFallbackNames.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }

                // Quick suggestions chips lorsque le champ est vide
                if (typedQuery.isBlank()) {
                    val quickChips = remember(catalogProducts, recentProducts) {
                        val list = mutableListOf<String>()
                        catalogProducts.take(15).forEach { if (!list.contains(it.name)) list.add(it.name) }
                        recentProducts.take(10).forEach { if (!list.contains(it)) list.add(it) }
                        if (list.isEmpty()) {
                            list.addAll(
                                listOf(
                                    "Câble 2.5mm",
                                    "Disjoncteur 16A",
                                    "Peinture Satinée",
                                    "Prise Murale",
                                    "Ampoule LED 12W"
                                )
                            )
                        }
                        list
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickChips.forEach { suggestion ->
                            val catProd = catalogProducts.find { it.name.equals(suggestion, ignoreCase = true) }
                            val chipBg = when {
                                catProd?.isArticleKit == true -> Color(0xFF0D9488).copy(alpha = 0.14f)
                                catProd?.isArticleEclate == true -> Color(0xFF7C3AED).copy(alpha = 0.14f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            val chipTextColor = when {
                                catProd?.isArticleKit == true -> Color(0xFF0D9488)
                                catProd?.isArticleEclate == true -> Color(0xFF7C3AED)
                                else -> MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            val prefix = when {
                                catProd?.isArticleKit == true -> "🎁 "
                                catProd?.isArticleEclate == true -> "⚡ "
                                else -> ""
                            }
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = chipBg,
                                modifier = Modifier.clickable {
                                    onProduitChanged(suggestion)
                                    val priceToUse = if (isPurchaseDocument && (catProd?.prixAchat ?: 0.0) > 0.0) {
                                        catProd?.prixAchat ?: 0.0
                                    } else {
                                        catProd?.defaultUnitPrice ?: 0.0
                                    }
                                    if (priceToUse > 0 && catProd?.isArticleEclate != true) {
                                        onPrixUnitaireChanged(CurrencyUtils.formatQty(priceToUse))
                                    }
                                }
                            ) {
                                Text(
                                    text = "$prefix$suggestion",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (catProd?.isComposite == true) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    color = chipTextColor
                                )
                            }
                        }
                    }
                }

                // Détection si l'article saisi est un Article Kit ou un Article Éclaté
                val matchedCatalogProd = catalogProducts.find {
                    it.name.trim().equals(state.produit.trim(), ignoreCase = true)
                }
                if (matchedCatalogProd?.isArticleKit == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF0D9488).copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "🎁 Article Kit (Nomenclature commerciale)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0D9488)
                            )
                            Text(
                                text = "• 1 seule ligne par défaut (l'article parent) au prix fixe du pack.\n• Rigide : impossible de retirer un composant sans éclater le kit.\n• Déstocke automatiquement les composants en tâche de fond à la validation.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else if (matchedCatalogProd?.isArticleEclate == true) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF7C3AED).copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "⚡ Article Éclaté (ou Kit éclaté)",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF7C3AED)
                            )
                            Text(
                                text = "• Plusieurs lignes : l'article parent disparaîtra et sera remplacé par tous ses composants dès la validation.\n• Totalement flexible : chaque composant deviendra une ligne indépendante modifiable ou supprimable.\n• Prix calculé par l'addition du prix de chaque composant individuel.",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Quantité with stepper controls
                Text(
                    text = "Quantité :",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = {
                            val newQty = (currentQty - 1.0).coerceAtLeast(1.0)
                            onQuantiteChanged(CurrencyUtils.formatQty(newQty))
                        },
                        modifier = Modifier.testTag("qty_minus_button")
                    ) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "-1")
                    }

                    OutlinedTextField(
                        value = state.quantiteText,
                        onValueChange = onQuantiteChanged,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("product_qty_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )

                    IconButton(
                        onClick = {
                            val newQty = currentQty + 1.0
                            onQuantiteChanged(CurrencyUtils.formatQty(newQty))
                        },
                        modifier = Modifier.testTag("qty_plus_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "+1")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Prix Unitaire (Price / Decimal, facultatif)
                OutlinedTextField(
                    value = state.prixUnitaireText,
                    onValueChange = onPrixUnitaireChanged,
                    label = { Text("Prix Unitaire en DA (Facultatif)") },
                    placeholder = { Text("0.00") },
                    trailingIcon = { Text("DA", modifier = Modifier.padding(end = 12.dp)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("product_price_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                // Subtotal Preview
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Sous-total ligne :",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Text(
                            text = CurrencyUtils.formatDa(lineSubtotal),
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Auto re-open switch (Enchaînement fluide)
                Spacer(modifier = Modifier.height(14.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAutoReopenChanged(!state.autoReopen) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Repeat,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Auto re-open (Enchaîner)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Saisir l'article suivant immédiatement",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = state.autoReopen,
                            onCheckedChange = onAutoReopenChanged,
                            modifier = Modifier.testTag("auto_reopen_switch")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.autoReopen) {
                    Button(
                        onClick = onSaveAndNext,
                        modifier = Modifier.testTag("save_and_next_button")
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ajouter & Suivant")
                    }
                }
                FilledTonalButton(
                    onClick = onSaveAndClose,
                    modifier = Modifier.testTag("save_line_button")
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Valider")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Annuler")
            }
        }
    )
}
