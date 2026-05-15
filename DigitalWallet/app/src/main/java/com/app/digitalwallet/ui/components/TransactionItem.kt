package com.app.digitalwallet.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.app.digitalwallet.data.Transaction
import com.app.digitalwallet.data.TransactionStatus
import java.util.Locale

@Composable
fun TransactionItem(
    transaction: Transaction,
    subtitle: String = "${transaction.category} • ${transaction.time}",
    onClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        transaction.icon, 
                        contentDescription = null, 
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                val title = if (!transaction.receiverName.isNullOrBlank()) {
                    transaction.receiverName
                } else if (transaction.merchantName.isNotBlank()) {
                    transaction.merchantName
                } else {
                    transaction.category
                }

                Text(
                    text = title, 
                    fontWeight = FontWeight.Bold, 
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (!transaction.receiverPhone.isNullOrBlank()) {
                        "${transaction.receiverPhone} • ${transaction.time}"
                    } else {
                        subtitle
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                    fontSize = 11.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (transaction.isPositive) "+" else "-") + "$${String.format(Locale.US, "%,.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (transaction.isPositive) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
                
                val statusColor = when(transaction.status) {
                    TransactionStatus.SUCCESS -> Color(0xFF4CAF50)
                    TransactionStatus.PENDING -> Color(0xFFFFA000)
                    TransactionStatus.FAILED -> Color(0xFFE57373)
                }
                val statusBg = statusColor.copy(alpha = 0.1f)

                Surface(
                    color = statusBg,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "• " + transaction.status.name,
                        color = statusColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
