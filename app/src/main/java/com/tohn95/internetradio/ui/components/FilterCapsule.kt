package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.ui.theme.LocalPalette

/**
 * Капсула фильтра/сортировки (Поиск, списки станций): значок + подпись + ▾.
 * Выбранная — в цвет темы; с [onClear] вместо ▾ крестик сброса — отдельная зона нажатия,
 * чтобы сброс не открывал выбор.
 */
@Composable
fun FilterCapsule(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onClear: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(50)
    val showClear = selected && onClear != null
    Row(
        modifier.height(36.dp).clip(shape)
            .background(if (selected) palette.primary.copy(alpha = 0.18f) else palette.cardBg)
            .border(1.dp, if (selected) palette.primary.copy(alpha = 0.55f) else palette.cardBorder, shape)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = if (showClear) 2.dp else 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) palette.primary else palette.textMuted, modifier = Modifier.size(16.dp))
        Text(
            label, color = if (selected) palette.primary else palette.text, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp).widthIn(max = 150.dp),
        )
        if (showClear) {
            Box(
                Modifier.padding(start = 2.dp).size(30.dp).clip(CircleShape).clickable(onClick = onClear!!),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Close, contentDescription = null, tint = palette.primary, modifier = Modifier.size(16.dp))
            }
        } else {
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = palette.textMuted, modifier = Modifier.size(18.dp))
        }
    }
}
