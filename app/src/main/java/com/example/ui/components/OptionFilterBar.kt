package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OptionType
import com.example.ui.theme.CallGreen
import com.example.ui.theme.CallGreenContainer
import com.example.ui.theme.PutRed
import com.example.ui.theme.PutRedContainer
import com.example.ui.theme.SurfaceBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevatedDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun OptionFilterBar(
    selectedFilter: OptionType?,
    onFilterSelect: (OptionType?) -> Unit,
    isScanning: Boolean,
    onTriggerScan: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Filter Chips: ALL | CALLS (CE) | PUTS (PE)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterPill(
                label = "ALL",
                isSelected = selectedFilter == null,
                onClick = { onFilterSelect(null) },
                activeColor = TextPrimary,
                activeBg = SurfaceElevatedDark
            )
            FilterPill(
                label = "CE (Calls)",
                isSelected = selectedFilter == OptionType.CE,
                onClick = { onFilterSelect(OptionType.CE) },
                activeColor = CallGreen,
                activeBg = CallGreenContainer
            )
            FilterPill(
                label = "PE (Puts)",
                isSelected = selectedFilter == OptionType.PE,
                onClick = { onFilterSelect(OptionType.PE) },
                activeColor = PutRed,
                activeBg = PutRedContainer
            )
        }

        // Quick Scan Button
        val infiniteTransition = rememberInfiniteTransition(label = "spin")
        val rotation by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "spinRotation"
        )

        ElevatedButton(
            onClick = { onTriggerScan() },
            enabled = !isScanning,
            modifier = Modifier
                .height(36.dp)
                .testTag("quick_scan_button"),
            colors = ButtonDefaults.elevatedButtonColors(
                containerColor = CallGreen,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = if (isScanning) Icons.Default.Sync else Icons.Default.FlashOn,
                contentDescription = "Scan",
                modifier = Modifier
                    .size(16.dp)
                    .then(if (isScanning) Modifier.rotate(rotation) else Modifier)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isScanning) "Scanning..." else "Scan Now",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    activeColor: Color,
    activeBg: Color
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) activeBg else SurfaceDark)
            .border(
                1.dp,
                if (isSelected) activeColor.copy(alpha = 0.5f) else SurfaceBorderDark,
                RoundedCornerShape(8.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) activeColor else TextSecondary
        )
    }
}
