package com.payten.whitelabel.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.R

/**
 * T&C acceptance box with checkbox and clickable link to view full terms.
 *
 * @param isChecked Whether the checkbox is checked.
 * @param onCheckedChange Callback when checkbox state changes.
 * @param onViewTermsClick Callback when the link is clicked.
 */
@Composable
fun TermsAndConditionsBox(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onViewTermsClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.tertiary,
                RoundedCornerShape(16.dp)
            )
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier
                    .size(24.dp)
                    .padding(top = 2.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            ClickableTermsText(
                onClick = onViewTermsClick
            )
        }
    }
}

/**
 * Clickable text component for T&C with underlined link.
 */
@Composable
private fun ClickableTermsText(
    onClick: () -> Unit
) {
    // Build annotated string with clickable portion
    val annotatedString = buildAnnotatedString {
        withStyle(
            style = SpanStyle(
                fontFamily = MyriadPro,
                color = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            append(stringResource(R.string.terms_text_part1))
        }

        // The clickable portion itself
        pushStringAnnotation(
            tag = "terms_link",
            annotation = "terms_and_conditions"
        )
        withStyle(
            style = SpanStyle(
                fontFamily = MyriadPro,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            )
        ) {
            append(stringResource(R.string.terms_text_link))
        }
        pop()

        withStyle(
            style = SpanStyle(
                fontFamily = MyriadPro,
                color = MaterialTheme.colorScheme.onTertiary
            )
        ) {
            append(stringResource(R.string.terms_text_part2))
        }
    }

    ClickableText(
        text = annotatedString,
        style = MaterialTheme.typography.bodyMedium.copy(
            lineHeight = 20.sp
        ),
        onClick = { offset ->
            annotatedString.getStringAnnotations(
                tag = "terms_link",
                start = offset,
                end = offset
            ).firstOrNull()?.let {
                onClick()
            }
        }
    )
}