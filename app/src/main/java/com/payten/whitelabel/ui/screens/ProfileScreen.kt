package com.payten.whitelabel.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.R

/**
 * Profile details screen showing user information.
 *
 * Displays:
 * - User ID / Name
 * - Address
 * - City
 * - TID (Terminal ID)
 *
 * @param sharedPreferences SharedPreferences for reading user data
 * @param onNavigateBack Callback when back button is clicked
 */
@Composable
fun ProfileScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {}
) {
    val userName = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_NAME, "N/A")
    val address = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_ADDRESS, "N/A")
    val city = sharedPreferences.pull(SharedPreferencesKeys.MERCHANT_PLACE_NAME, "N/A")
    val tid = sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "N/A")

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            ProfileHeader(onNavigateBack = onNavigateBack)

            Spacer(modifier = Modifier.height(40.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color.White,
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    ProfileInfoItem(
                        label = stringResource(R.string.profile_user_id_label),
                        value = userName,
                        icon = R.drawable.user
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileInfoItem(
                        label = stringResource(R.string.profile_address_label),
                        value = address,
                        icon = R.drawable.location_pin
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileInfoItem(
                        label = stringResource(R.string.profile_city_label),
                        value = city,
                        icon = R.drawable.globe
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    ProfileInfoItem(
                        label = stringResource(R.string.profile_tid_label),
                        value = tid,
                        icon = R.drawable.id
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Profile screen header with back button and title.
 */
@Composable
private fun ProfileHeader(
    onNavigateBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BackButton(onClick = onNavigateBack)

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = stringResource(R.string.profile_title),
            fontSize = 20.sp,
            fontFamily = MyriadPro,
            fontWeight = FontWeight.Bold,
            letterSpacing = 4.sp,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.width(40.dp))
    }
}

/**
 * Profile information item with label, value, and icon.
 */
@Composable
private fun ProfileInfoItem(
    label: String,
    value: String,
    icon: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                fontSize = 12.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Normal,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Image(
            painter = painterResource(id = icon),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }
}