package com.oorbitt.launcher.ui.util

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.ui.graphics.Shape

fun getIconShape(
    topStartPercent: Int,
    topEndPercent: Int,
    bottomStartPercent: Int,
    bottomEndPercent: Int,
    isCut: Boolean
): Shape {
    return if (isCut) {
        CutCornerShape(
            topStart = CornerSize(topStartPercent),
            topEnd = CornerSize(topEndPercent),
            bottomStart = CornerSize(bottomStartPercent),
            bottomEnd = CornerSize(bottomEndPercent)
        )
    } else {
        RoundedCornerShape(
            topStart = CornerSize(topStartPercent),
            topEnd = CornerSize(topEndPercent),
            bottomStart = CornerSize(bottomStartPercent),
            bottomEnd = CornerSize(bottomEndPercent)
        )
    }
}
