package com.payten.whitelabel.ui.screens

import android.content.Intent
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.config.SupercaseConfig
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.components.BackButton
import com.payten.whitelabel.ui.theme.MyriadPro
import androidx.core.net.toUri

/**
 * Settings screen with profile, PIN, language, terms, and contact options.
 *
 * Features:
 * - Profile navigation
 * - Change PIN
 * - Language selection (Slovenian, English, Serbian)
 * - Terms and conditions
 * - Contact details with dialer
 * - App version display
 *
 * @param sharedPreferences SharedPreferences instance for language persistence
 * @param onNavigateBack Callback when back button is clicked
 * @param onProfileClick Navigate to Profile screen
 * @param onChangePinClick Navigate to Change PIN screen
 * @param onTermsClick Navigate to Terms screen
 */
@Composable
fun SettingsScreen(
    sharedPreferences: KsPrefs,
    onNavigateBack: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onChangePinClick: () -> Unit = {},
    onTermsClick: () -> Unit = {}
) {
    val context = LocalContext.current

    var currentLanguageIndex by remember {
        mutableIntStateOf(sharedPreferences.pull(SharedPreferencesKeys.LANGUAGE, 2))
    }

    var showLanguageDialog by remember { mutableStateOf(false) }

    val packageInfo = remember {
        context.packageManager.getPackageInfo(context.packageName, 0)
    }
    val versionCode: Long = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
        packageInfo.longVersionCode
    } else {
        @Suppress("DEPRECATION")
        packageInfo.versionCode.toLong()
    }
    val versionName: String = packageInfo.versionName ?: "unknown"
    val appVersion = "$versionName ($versionCode)"

    val currentLanguageText = when (currentLanguageIndex) {
        0 -> stringResource(R.string.language_slo)
        1 -> stringResource(R.string.language_en)
        2 -> stringResource(R.string.language_sr)
        else -> stringResource(R.string.language_slo)
    }

    val currentLanguageFlag = when (currentLanguageIndex) {
        0 -> "🇸🇮"
        1 -> "🇬🇧"
        2 -> "🇷🇸"
        else -> "🇸🇮"
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            SettingsHeader(
                title = stringResource(R.string.settings_title),
                onNavigateBack = onNavigateBack
            )

            Spacer(modifier = Modifier.height(32.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                color = Color(0xFFFFFFFF),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    SettingsItem(
                        title = stringResource(R.string.settings_profile),
                        showArrow = true,
                        onClick = onProfileClick
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_change_pin),
                        showArrow = true,
                        onClick = onChangePinClick
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_language),
                        subtitle = currentLanguageText,
                        flag = currentLanguageFlag,
                        showDropdown = true,
                        onClick = { showLanguageDialog = true }
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_terms),
                        showArrow = true,
                        onClick = onTermsClick
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = Color.LightGray.copy(alpha = 0.3f)
                    )

                    SettingsItem(
                        title = stringResource(R.string.settings_contact_info),
                        subtitle = SupercaseConfig.CONTACT_NUMBER,
                        showPhoneIcon = true,
                        onClick = {
                            val phoneNumber = SupercaseConfig.CONTACT_NUMBER
                                .replace(",", "")
                                .replace(" ", "")
                            val intent = Intent(Intent.ACTION_DIAL).apply {
                                data = "tel:$phoneNumber".toUri()
                            }
                            // This is a system activity, so we are not compromising our architecture
                            context.startActivity(intent)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = appVersion,
                fontSize = 12.sp,
                fontFamily = MyriadPro,
                color = Color.Gray,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
        }
    }

    if (showLanguageDialog) {
        LanguagePickerDialog(
            currentLanguageIndex = currentLanguageIndex,
            onDismiss = { showLanguageDialog = false },
            onLanguageSelected = { index ->
                Log.d("SettingsScreen", "=== LANGUAGE CHANGE STARTED ===")
                Log.d("SettingsScreen", "Selected index: $index")
                sharedPreferences.push(SharedPreferencesKeys.LANGUAGE, index)
                currentLanguageIndex = index

                val localeTag = when (index) {
                    0 -> "sl"
                    1 -> "en"
                    2 -> "sr"
                    else -> "sr"
                }
                Log.d("SettingsScreen", "Locale tag: $localeTag")
                Log.d("SettingsScreen", "Before setApplicationLocales")
                AppCompatDelegate.setApplicationLocales(
                    LocaleListCompat.forLanguageTags(localeTag)
                )
                Log.d("SettingsScreen", "After setApplicationLocales")

                showLanguageDialog = false
            }
        )
    }
}

/**
 * Settings screen header with back button and title.
 */
@Composable
private fun SettingsHeader(
    title: String,
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
            text = title,
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
 * Individual settings item row.
 */
@Composable
private fun SettingsItem(
    title: String,
    subtitle: String? = null,
    flag: String? = null,
    showArrow: Boolean = false,
    showDropdown: Boolean = false,
    showPhoneIcon: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {

            flag?.let {
                Text(
                    text = it,
                    fontSize = 24.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )
            }

            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontFamily = MyriadPro,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                subtitle?.let {
                    Text(
                        text = it,
                        fontSize = 14.sp,
                        fontFamily = MyriadPro,
                        fontWeight = FontWeight.Normal,
                        color = Color.Gray
                    )
                }
            }
        }

        when {
            showArrow -> Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            showDropdown -> Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
            showPhoneIcon -> Icon(
                painter = painterResource(id = R.drawable.phone),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Language picker dialog.
 */
@Composable
private fun LanguagePickerDialog(
    currentLanguageIndex: Int,
    onDismiss: () -> Unit,
    onLanguageSelected: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_language_dialog_title),
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                LanguageOption(
                    flag = "🇸🇮",
                    name = stringResource(R.string.language_slo),
                    isSelected = currentLanguageIndex == 0,
                    onClick = { onLanguageSelected(0) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LanguageOption(
                    flag = "🇬🇧",
                    name = stringResource(R.string.language_en),
                    isSelected = currentLanguageIndex == 1,
                    onClick = { onLanguageSelected(1) }
                )

                Spacer(modifier = Modifier.height(8.dp))

                LanguageOption(
                    flag = "🇷🇸",
                    name = stringResource(R.string.language_sr),
                    isSelected = currentLanguageIndex == 2,
                    onClick = { onLanguageSelected(2) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    stringResource(R.string.settings_dialog_cancel),
                    fontFamily = MyriadPro
                )
            }
        }
    )
}

/**
 * Language option in dialog.
 */
@Composable
private fun LanguageOption(
    flag: String,
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = flag,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 16.dp)
        )

        Text(
            text = name,
            fontSize = 16.sp,
            fontFamily = MyriadPro,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
        )
    }
}