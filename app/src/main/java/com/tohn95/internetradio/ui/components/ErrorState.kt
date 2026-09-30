package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.R
import com.tohn95.internetradio.ui.theme.LocalPalette

/** Есть ли интернет (из NetworkMonitor, раздаёт AppScaffold). */
val LocalOnline = compositionLocalOf { true }

/** Плашка «Нет интернета» поверх экранов — пилюля в стиле приложения. */
@Composable
fun OfflinePill(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(50)
    Row(
        modifier.shadow(8.dp, shape, spotColor = Color.Black).clip(shape).background(palette.red)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.WifiOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
        Text(
            stringResource(R.string.err_offline_banner), color = Color.White, fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp),
        )
    }
}

/** Карточка ошибки: значок в цветном круге, заголовок, пояснение, 1–2 кнопки. */
@Composable
fun ErrorCard(
    icon: ImageVector,
    title: String,
    message: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(palette.cardBg).padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(palette.primary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = palette.primary, modifier = Modifier.size(28.dp)) }
        Text(
            title, color = palette.text, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            message, color = palette.textMuted, fontSize = 14.sp, lineHeight = 19.sp, textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Row(Modifier.padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (secondaryLabel != null && onSecondary != null) {
                OutlinedButton(onClick = onSecondary) { Text(secondaryLabel) }
            }
            Button(
                onClick = onPrimary,
                colors = ButtonDefaults.buttonColors(containerColor = palette.primary, contentColor = palette.onPrimary()),
            ) { Text(primaryLabel) }
        }
    }
}

/**
 * Ошибка загрузки каталога: без интернета — «Нет подключения», с интернетом — «Каталог не отвечает»
 * (серверы radio-browser недоступны) и подсказка, что Избранное всё равно играет.
 */
@Composable
fun CatalogError(onRetry: () -> Unit, modifier: Modifier = Modifier, onOpenFavorites: (() -> Unit)? = null) {
    if (!LocalOnline.current) {
        ErrorCard(
            Icons.Filled.WifiOff, stringResource(R.string.err_offline_title), stringResource(R.string.err_offline_msg),
            primaryLabel = stringResource(R.string.retry), onPrimary = onRetry, modifier = modifier,
        )
    } else {
        ErrorCard(
            Icons.Filled.CloudOff, stringResource(R.string.err_servers_title), stringResource(R.string.err_servers_msg),
            primaryLabel = stringResource(R.string.retry), onPrimary = onRetry, modifier = modifier,
            secondaryLabel = onOpenFavorites?.let { stringResource(R.string.err_to_favorites) }, onSecondary = onOpenFavorites,
        )
    }
}
