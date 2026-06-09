package com.app.digitalwallet.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.digitalwallet.domain.model.Transaction
import java.util.Locale

@Composable
fun TransactionDetailContent(
    transaction: Transaction,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            modifier = Modifier.size(72.dp),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = transaction.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val title = if (!transaction.senderName.isNullOrBlank()) {
            transaction.senderName
        } else transaction.merchantName.ifBlank {
            transaction.category
        }

        Text(
            title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (!transaction.senderPhone.isNullOrBlank()) {
            Text(
                transaction.senderPhone,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )
        }
        Text(
            transaction.category,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = (if (transaction.isPositive) "+" else "-") + "$${String.format(Locale.US, "%,.2f", transaction.amount)}",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = if (transaction.isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(32.dp))

        DetailRow("Status", transaction.status.name, isStatus = true, status = transaction.status)
        DetailRow("Transaction Date", "${transaction.date} • ${transaction.time}")
        if (transaction.reference != null) {
            DetailRow("Reference", transaction.reference)
        }
        DetailRow("Payment Method", transaction.paymentMethod, icon = Icons.Default.CreditCard)

        Spacer(modifier = Modifier.height(32.dp))

        PrimaryButton(
            text = "Close",
            onClick = onClose,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
