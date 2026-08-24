package com.thiagoventura.horacerta.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val BlueGradient = Brush.linearGradient(
    colors = listOf(Color(0xFF0870E5), Color(0xFF064FCB), Color(0xFF003BB1)),
    start = Offset(0f, 0f),
    end = Offset(1000f, 1000f),
)

@Composable
fun HoraCertaBrand(
    title: String = "Hora Certa",
    modifier: Modifier = Modifier,
    titleSize: Int = 27,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        PillMark(Modifier.size(width = 38.dp, height = 50.dp))
        Spacer(Modifier.size(13.dp))
        Text(
            title,
            color = Ink,
            fontSize = titleSize.sp,
            lineHeight = (titleSize + 2).sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
fun PillMark(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.rotate(38f)) {
        val stroke = size.minDimension * .075f
        val corner = size.width / 2f
        drawRoundRect(
            color = Color.White,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = androidx.compose.ui.graphics.drawscope.Fill,
        )
        clipRect(left = 0f, top = 0f, right = size.width, bottom = size.height * .54f) {
            drawRoundRect(
                brush = BlueGradient,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            )
        }
        drawRoundRect(
            color = Cobalt,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(corner, corner),
            style = Stroke(stroke),
        )
        drawLine(
            color = Color(0xFFBBD8FF),
            start = Offset(stroke, size.height * .54f),
            end = Offset(size.width - stroke, size.height * .54f),
            strokeWidth = stroke * .55f,
        )
    }
}

@Composable
fun CroppedMockupAsset(
    @DrawableRes resource: Int,
    sourceX: Int,
    sourceY: Int,
    sourceWidth: Int,
    sourceHeight: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val bitmap = androidx.compose.ui.graphics.ImageBitmap.imageResource(resource)
    Image(
        painter = BitmapPainter(
            image = bitmap,
            srcOffset = IntOffset(sourceX, sourceY),
            srcSize = IntSize(sourceWidth, sourceHeight),
        ),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}

@Composable
fun GradientPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (enabled) BlueGradient else Brush.linearGradient(listOf(Color(0xFFE4E2DE), Color(0xFFE4E2DE))))
            .then(if (enabled) Modifier else Modifier)
            .noRippleClickable(enabled, onClick),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading?.invoke()
            if (leading != null) Spacer(Modifier.size(10.dp))
            Text(
                text,
                color = if (enabled) Color.White else Color(0xFFA9A6A2),
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private fun Modifier.noRippleClickable(enabled: Boolean, onClick: () -> Unit): Modifier =
    this.clickable(
        enabled = enabled,
        indication = null,
        interactionSource = null,
        onClick = onClick,
    )

@Composable
fun DotPager(current: Int, total: Int, labelFirst: Boolean = false) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (labelFirst) {
            Text("${current + 1} de $total", color = Ink, fontSize = 17.sp)
            Spacer(Modifier.height(14.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            repeat(total) { index ->
                Box(
                    Modifier
                        .padding(horizontal = 8.dp)
                        .size(if (index == current) 18.dp else 14.dp)
                        .background(
                            if (index == current) Cobalt else if (index < current) Color(0xFF79B5FF) else Color(0xFFD5E5FA),
                            CircleShape,
                        )
                )
            }
        }
        if (!labelFirst) {
            Spacer(Modifier.height(14.dp))
            Text("${current + 1} de $total", color = Ink, fontSize = 17.sp)
        }
    }
}
