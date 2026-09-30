package com.tohn95.internetradio.ui.components

import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tohn95.internetradio.ui.theme.LocalPalette

/**
 * Высота плавающей нижней зоны (мини-плеер + навбар + системная навигация).
 * Контент экранов уходит ПОД неё (панель полупрозрачная), а списки добавляют этот отступ снизу,
 * чтобы последний элемент можно было докрутить над панелью.
 */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

data class NavItem<T : Any>(val route: T, val icon: ImageVector, @StringRes val label: Int)

/** Нижняя навигация — плавающая скруглённая «пилюля», слегка прозрачная. */
@Composable
fun <T : Any> FloatingNavBar(
    items: List<NavItem<T>>,
    isSelected: (T) -> Boolean,
    onClick: (T) -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 64.dp,
) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier.fillMaxWidth().height(height)
            .shadow(8.dp, shape, spotColor = Color.Black)
            .clip(shape)
            .background(palette.cardBg.copy(alpha = 0.92f))
            .border(1.dp, palette.cardBorder.copy(alpha = 0.55f), shape)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items.forEach { item ->
            val selected = isSelected(item.route)
            val color by animateColorAsState(if (selected) palette.primary else palette.textMuted, label = "nav")
            val pill by animateColorAsState(
                if (selected) palette.primary.copy(alpha = 0.18f) else Color.Transparent, label = "navPill",
            )
            Column(
                Modifier.weight(1f).fillMaxHeight().padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(22.dp)).clickable { onClick(item.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier.clip(RoundedCornerShape(50)).background(pill).padding(horizontal = 14.dp, vertical = 3.dp),
                ) {
                    Icon(item.icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
                Text(
                    stringResource(item.label), color = color, fontSize = 11.sp, maxLines = 1,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
