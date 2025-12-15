package com.payten.whitelabel.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import at.favre.lib.crypto.bcrypt.BCrypt
import com.cioccarellia.ksprefs.KsPrefs
import com.payten.whitelabel.R
import com.payten.whitelabel.dto.keys.GetKeysRequestDto
import com.payten.whitelabel.persistance.SharedPreferencesKeys
import com.payten.whitelabel.ui.theme.MyriadPro
import com.payten.whitelabel.viewmodel.RegistrationViewModel
import com.simant.MainApplication

private const val TAG = "ReactivationScreen"

/**
 * Reactivation screen that handles terminal reactivation flow.
 *
 * Implements the same 5-step flow as the old ReactivationActivity:
 * 1. Call reactivation API
 * 2. Call activate API
 * 3. Generate token
 * 4. Get host keys
 * 5. Initialize MTA (SDK)
 *
 * After completion, navigates to PIN login.
 */
@Composable
fun ReactivationScreen(
    sharedPreferences: KsPrefs,
    onReactivationComplete: () -> Unit = {},
    viewModel: RegistrationViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    var currentStep by remember { mutableIntStateOf(1) }

    val reactivationSuccess by viewModel.reactivationSuccess.observeAsState()
    val activationSuccess by viewModel.paytenActivationSuccessfull.observeAsState()
    val tokenSuccess by viewModel.paytenGenerateTokenSuccessfull.observeAsState()
    val keysSuccess by viewModel.paytenGetHostKeys.observeAsState()
    val sdkSuccess by viewModel.sdkRegisterSuccess.observeAsState()

    Log.d(TAG, "ReactivationScreen - currentStep=$currentStep")

    LaunchedEffect(Unit) {
        Log.d(TAG, "Starting reactivation flow")
        val tid = sharedPreferences.pull(SharedPreferencesKeys.POS_SERVICE_TERMINAL_ID, "")
        Log.d(TAG, "Calling reactivation with tid=$tid")
        viewModel.reactivation(tid)
    }

    LaunchedEffect(reactivationSuccess) {
        if (reactivationSuccess == true) {
            Log.d(TAG, "Reactivation success - moving to step 2")
            currentStep = 2

            val userId = sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, "")
            val activationCode = sharedPreferences.pull(SharedPreferencesKeys.USER_ACTIVATION_CODE, "")
            val sharedPrefsSOFT = context.getSharedPreferences("SOFTPOS_PARAMETERS_MDI", Context.MODE_PRIVATE)

            Log.d(TAG, "Calling activate with userId=$userId")
            viewModel.activate(userId, activationCode, "Intesa.Android", context, sharedPrefsSOFT)
        }
    }

    LaunchedEffect(activationSuccess) {
        if (activationSuccess == true) {
            Log.d(TAG, "Activation success - moving to step 3")
            currentStep = 3

            val act = sharedPreferences.pull(SharedPreferencesKeys.USER_ACTIVATION_CODE, "")
            val encryptedActId = BCrypt.withDefaults().hashToString(12, act.toCharArray())
            sharedPreferences.push(SharedPreferencesKeys.HASHED_ACT_ID, encryptedActId)
            sharedPreferences.push(SharedPreferencesKeys.IS_REGISTERED, true)

            val userId = sharedPreferences.pull(SharedPreferencesKeys.USER_ID, "")
            val tid = sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "")

            Log.d(TAG, "Calling generateToken with userId=$userId, tid=$tid")
            viewModel.generateToken(userId, tid)
        }
    }

    LaunchedEffect(tokenSuccess) {
        if (tokenSuccess == true) {
            Log.d(TAG, "Token generation success - moving to step 4")
            currentStep = 4

            val tid = sharedPreferences.pull(SharedPreferencesKeys.USER_TID, "")
            Log.d(TAG, "Calling getHostKeys with tid=$tid")
            viewModel.getHostKeys(GetKeysRequestDto(tid))
        }
    }

    LaunchedEffect(keysSuccess) {
        if (keysSuccess == true) {
            Log.d(TAG, "GetHostKeys success - moving to step 5")
            currentStep = 5

            // Update parameters (matches ReactivationActivity line 91)
            val application = context.applicationContext as? MainApplication
            application?.updateParameters()

            val userId = sharedPreferences.pull(SharedPreferencesKeys.REGISTRATION_USER_ID, "")
            val activationCode = sharedPreferences.pull(SharedPreferencesKeys.USER_ACTIVATION_CODE, "")

            Log.d(TAG, "Calling initializeMta")
            application?.let {
                viewModel.initializeMta(it, userId, activationCode, context)
            }
        }
    }

    LaunchedEffect(sdkSuccess) {
        if (sdkSuccess == true) {
            Log.d(TAG, "SDK registration success - reactivation complete")

            // Set IS_LOGGED_IN and navigate to PIN login (matches ReactivationActivity lines 126-131)
            sharedPreferences.push(SharedPreferencesKeys.IS_LOGGED_IN, true)
            sharedPreferences.push(SharedPreferencesKeys.PIN_COUNT, 3)

            Log.d(TAG, "Calling onReactivationComplete")
            onReactivationComplete()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
    ) {
        Image(
            painter = painterResource(id = R.drawable.gradient_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.reactivation_in_progress),
                fontSize = 24.sp,
                fontFamily = MyriadPro,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                for (ledNumber in 1..5) {
                    val isActive = ledNumber <= currentStep
                    Surface(
                        modifier = Modifier.size(48.dp),
                        shape = CircleShape,
                        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.3f)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = ledNumber.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isActive) Color.White else Color.Gray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 6.dp
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.please_wait),
                fontSize = 16.sp,
                fontFamily = MyriadPro,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
            )
        }
    }
}
