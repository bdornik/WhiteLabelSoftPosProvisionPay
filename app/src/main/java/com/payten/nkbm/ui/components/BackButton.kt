package com.payten.nkbm.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.ui.tooling.preview.Preview
/**
 * Unnecessarily complicated back button.
 * */
@Preview
@Composable
fun BackButton(
    modifier: Modifier = Modifier,
    circleColor: Color = MaterialTheme.colorScheme.secondary,
    triangleColor: Color = MaterialTheme.colorScheme.secondary,
    onClick: () -> Unit = {}
) {
    Canvas(
        modifier = modifier
            .size(40.dp)
            .clickable(
                onClick = onClick,
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
    ) {
        val centerX = size.width / 2
        val centerY = size.height / 2
        val radius = 20.dp.toPx()
        val strokeWidth = 2.dp.toPx()

        drawArc(
            color = circleColor,
            startAngle = 30f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = Offset(centerX - radius, centerY - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth)
        )

        val triangleHeight = 12.dp.toPx()
        val triangleWidth = triangleHeight * 0.8f

        val trianglePath = Path().apply {
            moveTo(centerX - triangleWidth / 2, centerY)
            lineTo(centerX + triangleWidth / 2, centerY - triangleHeight / 2)
            lineTo(centerX + triangleWidth / 2, centerY + triangleHeight / 2)
            close()
        }

        drawPath(
            path = trianglePath,
            color = triangleColor
        )
    }
}