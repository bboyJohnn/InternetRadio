package com.tohn95.internetradio.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Текст в узкой плитке, который переносится только между словами. Если самое длинное слово
 * не помещается в ширину (или весь текст — в [maxLines] строк), шрифт плавно уменьшается до
 * [minSize], а не режет слово посередине («Инструмента-л», «Великобрита-ния»).
 */
@Composable
fun WordFitText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = 2,
    minLines: Int = 1,
    minSize: TextUnit = 10.sp,
) {
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(modifier) {
        val maxWidth = constraints.maxWidth
        val fitted = remember(text, style, maxWidth, maxLines) {
            if (maxWidth == Constraints.Infinity) return@remember style
            val words = text.split(' ').filter { it.isNotEmpty() }
            val base = style.fontSize.value
            var size = base
            fun styled(s: Float) = style.copy(
                fontSize = s.sp,
                lineHeight = if (style.lineHeight.isSp) (style.lineHeight.value * s / base).sp else style.lineHeight,
            )
            while (size > minSize.value) {
                val st = styled(size)
                val wordsFit = words.all { measurer.measure(it, st, softWrap = false, maxLines = 1).size.width <= maxWidth }
                val all = measurer.measure(text, st, maxLines = maxLines, constraints = Constraints(maxWidth = maxWidth))
                if (wordsFit && !all.hasVisualOverflow) break
                size -= 0.5f
            }
            styled(size)
        }
        Text(text, style = fitted, maxLines = maxLines, minLines = minLines, overflow = TextOverflow.Ellipsis)
    }
}
