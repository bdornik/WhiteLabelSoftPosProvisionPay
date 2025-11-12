package com.payten.nkbm.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.nkbm.R
import com.payten.nkbm.ui.theme.MyriadPro

/**
 * Numeric keypad with numbers 0-9 and backspace button.
 *
 * @param onNumberClick Callback when number button is clicked.
 * @param onBackspaceClick Callback when backspace button is clicked.
 */
@Composable
fun NumericKeypad(
    onNumberClick: (Int) -> Unit,
    onBackspaceClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        listOf(
            listOf(1, 2, 3),
            listOf(4, 5, 6),
            listOf(7, 8, 9)
        ).forEach { row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { number ->
                    NumericKey(
                        text = number.toString(),
                        onClick = { onNumberClick(number) }
                    )
                }
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Spacer(modifier = Modifier.size(80.dp))

            NumericKey(
                text = "0",
                onClick = { onNumberClick(0) }
            )

            BackspaceKey(onClick = onBackspaceClick)
        }
    }
}

/**
 * Individual numeric key button.
 *
 * @param text Text to display on the button.
 * @param onClick Callback when button is clicked.
 */
@Composable
private fun NumericKey(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 28.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

/**
 * Backspace key button with icon.
 *
 * @param onClick Callback when button is clicked.
 */
@Composable
private fun BackspaceKey(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(80.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiary)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.backspace),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}