package com.payten.nkbm.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.payten.nkbm.R
import com.payten.nkbm.ui.theme.AppTheme
import com.payten.nkbm.ui.theme.MyriadPro

/**
 * Menu screen showing merchant info and navigation options.
 *
 * @param merchantName Merchant's name from SharedPreferencesKeys.MERCHANT_NAME
 * @param merchantAddress Full address from MERCHANT_ADDRESS + MERCHANT_PLACE_NAME
 * @param onClose Callback when close button is clicked
 * @param onTrafficClick Navigate to Traffic screen
 * @param onSettingsClick Navigate to Settings screen
 * @param onEndOfDayClick Navigate to End of Day screen
 * @param onSignOutClick Sign out action
 */
@Composable
fun MenuScreen(
    merchantName: String,
    merchantAddress: String,
    onClose: () -> Unit = {},
    onTrafficClick: () -> Unit = {},
    onSettingsClick: () -> Unit = {},
    onEndOfDayClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            MenuHeader(onClose = onClose)

            Spacer(modifier = Modifier.height(24.dp))

            MerchantInfoCard(
                name = merchantName,
                address = merchantAddress
            )

            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                MenuItemWithArrow(
                    iconRes = R.drawable.traffic,
                    title = "Promet",
                    onClick = onTrafficClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                MenuItemWithArrow(
                    iconRes = R.drawable.settings,
                    title = "Podešavanja",
                    onClick = onSettingsClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                MenuItemWithArrow(
                    iconRes = R.drawable.overview,
                    title = "Presek dana",
                    onClick = onEndOfDayClick
                )

                Spacer(modifier = Modifier.height(16.dp))

                DashedDivider()

                Spacer(modifier = Modifier.height(16.dp))

                MenuItemWithClickableIcon(
                    iconRes = R.drawable.sign_out,
                    title = "Odjava",
                    onClick = onSignOutClick
                )
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Header with centered Payten logo and exit button.
 */
@Composable
private fun MenuHeader(
    onClose: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 16.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.payten_dark),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .size(40.dp)
                .align(Alignment.CenterEnd)
                .clip(CircleShape)
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.exit),
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

/**
 * Merchant information card
 */
@Composable
private fun MerchantInfoCard(
    name: String,
    address: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = Color.LightGray.copy(alpha = 0.3f),
                shape = RoundedCornerShape(12.dp)
            )
            .background(
                color = Color.White,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(16.dp)
    ) {
        Column {
            Text(
                text = name,
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            if (address.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(12.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        text = address,
                        fontSize = 13.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

/**
 * Menu item
 */
@Composable
private fun MenuItemWithArrow(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        IconButton(
            onClick = onClick,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

/**
 * Menu item (Sign out)
 */
@Composable
private fun MenuItemWithClickableIcon(
    iconRes: Int,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier
                .size(28.dp)
                .clickable(onClick = onClick)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun DashedDivider(
    color: Color = Color.Gray.copy(alpha = 0.5f),
    thickness: Float = 2f,
    dashWidth: Float = 10f,
    dashGap: Float = 10f
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .drawBehind {
                val pathEffect = PathEffect.dashPathEffect(
                    intervals = floatArrayOf(dashWidth, dashGap),
                    phase = 0f
                )
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = thickness,
                    pathEffect = pathEffect
                )
            }
    )
}

@Preview
@Composable
fun MenuScreenPreview() {
    AppTheme {
        MenuScreen(
            merchantName = "Petar Petrović",
            merchantAddress = "Bul. Mihajla Pupina 10b, Beograd"
        )
    }
}