package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.OperationType
import com.example.ui.theme.ClientBg
import com.example.ui.theme.ClientColor
import com.example.ui.theme.FournisseurBg
import com.example.ui.theme.FournisseurColor
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ReceiptLong
import com.example.ui.theme.ReglementBg
import com.example.ui.theme.ReglementColor

@Composable
fun OperationBadge(
    type: OperationType,
    modifier: Modifier = Modifier
) {
    val (bgColor, fgColor, icon) = when (type) {
        OperationType.BL_CLIENT -> Triple(ClientBg, ClientColor, Icons.Default.ArrowUpward)
        OperationType.BL_FOURNISSEUR -> Triple(FournisseurBg, FournisseurColor, Icons.Default.ArrowDownward)
        OperationType.FACTURE -> Triple(Color(0xFFFEF3C7), Color(0xFFD97706), Icons.Default.Description)
        OperationType.FACTURE_FOURNISSEUR -> Triple(Color(0xFFE0E7FF), Color(0xFF4338CA), Icons.Default.ReceiptLong)
        OperationType.BON_COMMANDE -> Triple(Color(0xFFEDE9FE), Color(0xFF7C3AED), Icons.Default.Assignment)
        OperationType.DEVIS_CLIENT -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), Icons.Default.Description)
        OperationType.AVOIR_CLIENT -> Triple(Color(0xFFFFEDD5), Color(0xFFC2410C), Icons.Default.ReceiptLong)
        OperationType.DEVIS_FOURNISSEUR -> Triple(Color(0xFFE0F7FA), Color(0xFF00838F), Icons.Default.Assignment)
        OperationType.AVOIR_FOURNISSEUR -> Triple(Color(0xFFFEE2E2), Color(0xFFB91C1C), Icons.Default.ReceiptLong)
        OperationType.REGLEMENT -> Triple(ReglementBg, ReglementColor, Icons.Default.Payments)
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fgColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = type.label,
                color = fgColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}
