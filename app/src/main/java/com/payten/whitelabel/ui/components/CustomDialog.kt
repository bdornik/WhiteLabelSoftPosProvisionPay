package com.payten.whitelabel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.payten.whitelabel.ui.theme.AppTheme
import com.payten.whitelabel.ui.theme.MyriadPro

/**
 * Custom dialog for success and error messages.
 *
 * Displays an icon, title, and button in a styled dialog.
 *
 * @param isSuccess True for success dialog, false for error.
 * @param title Title text to display below the icon.
 * @param buttonText Text for the button.
 * @param onDismiss Callback when dialog is dismissed (button click or outside tap).
 */
@Composable
fun CustomDialog(
    isSuccess: Boolean,
    title: String,
    buttonText: String? = null,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            color = if (isSuccess) {
                                Color(0xFF4CAF50)  // Green
                            } else {
                                Color(0xFFE53935)  // Red
                            },
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // If the need arises, this can be an Image instead of an Icon
                    Icon(
                        imageVector = if (isSuccess) {
                            Icons.Default.Check
                        } else {
                            Icons.Default.Close
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    lineHeight = 24.sp
                )

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Text(
                        text = buttonText ?: if (isSuccess) "U REDU" else "NAZAD",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CustomDialogSuccessPreview() {
    AppTheme {
        CustomDialog(
            isSuccess = true,
            title = "Uspešna registracija",
            onDismiss = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun CustomDialogErrorPreview() {
    AppTheme {
        CustomDialog(
            isSuccess = false,
            title = "Uneli ste neispravan PIN",
            onDismiss = {}
        )
    }
}