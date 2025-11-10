package com.payten.nkbm.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.nkbm.ui.theme.MyriadPro
/**
 * Custom text field for the RegistrationPage.
 *
 * Might get reused later.
 * */
@Composable
fun CustomTextField(
    label: String,
    placeholder: String,
    value: String = "",
    onValueChange: (String) -> Unit = {}
) {
    var text by remember { mutableStateOf(value) }

    Column(
        modifier = Modifier
            .height(84.dp)
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            fontFamily = MyriadPro,
            color = Color.Gray,
            fontWeight = FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        BasicTextField(
            value = text,
            onValueChange = {
                text = it
                onValueChange(it)
            },
            textStyle = TextStyle(
                fontSize = 18.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            ),
            decorationBox = { innerTextField ->
                Box {
                    if (text.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}