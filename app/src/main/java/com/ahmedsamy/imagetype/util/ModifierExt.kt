package com.ahmedsamy.imagetype.util

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
fun Modifier.checkerboardBackground(
    tileSize: Dp = 12.dp,
    colorLight: Color = Color(0xFFEAEAEA),
    colorDark: Color = Color(0xFFD4D4D4)
) = drawBehind {
    val sizePx = tileSize.toPx()
    val columns = (size.width / sizePx).toInt() + 1
    val rows = (size.height / sizePx).toInt() + 1
    for (i in 0 until columns) {
        for (j in 0 until rows) {
            val color = if ((i + j) % 2 == 0) colorLight else colorDark
            drawRect(
                color = color,
                topLeft = Offset(i * sizePx, j * sizePx),
                size = androidx.compose.ui.geometry.Size(sizePx, sizePx)
            )
        }
    }
}
