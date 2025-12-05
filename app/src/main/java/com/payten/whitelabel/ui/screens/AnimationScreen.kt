package com.payten.whitelabel.ui.screens

import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.mastercard.sonic.controller.SonicController
import com.mastercard.sonic.controller.SonicType
import com.mastercard.sonic.listeners.OnCompleteListener
import com.mastercard.sonic.listeners.OnPrepareListener
import com.mastercard.sonic.widget.SonicView
import com.visa.SensoryBrandingView
import androidx.core.graphics.toColorInt

/**
 * Animation screen that shows Visa or Mastercard payment success animation.
 *
 * @param cardType "visa" or "mastercard"
 * @param onAnimationComplete Callback when animation finishes
 */
@Composable
fun AnimationScreen(
    cardType: String,
    onAnimationComplete: () -> Unit
) {
    val context = LocalContext.current

    if (cardType.contains("visa", ignoreCase = true)) {
        // Visa Animation
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
        ) {
            AndroidView(
                factory = { ctx ->
                    SensoryBrandingView(ctx, null).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        isSoundEffectsEnabled = true
                        setBackgroundColor("#ffffff".toColorInt())
                        backdropColor = "#ffffff".toColorInt()

                        // Start animation
                        animate {
                            onAnimationComplete()
                        }
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    } else if (cardType.contains("card", ignoreCase = true)) {
        // Mastercard Animation
        val sonicController = SonicController()

        AndroidView(
            factory = { ctx ->
                SonicView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )

                    sonicController.prepare(
                        context = context,
                        sonicType = SonicType.SOUND_AND_ANIMATION,
                        onPrepareListener = object : OnPrepareListener {
                            override fun onPrepared(statusCode: Int) {
                                if (!sonicController.isPlaying) {
                                    sonicController.play(
                                        onCompleteListener = object : OnCompleteListener {
                                            override fun onComplete(statusCode: Int) {
                                                onAnimationComplete()
                                            }
                                        },
                                        sonicView = this@apply
                                    )
                                }
                            }
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        DisposableEffect(Unit) {
            onDispose {
                // Cleanup if needed
            }
        }
    }
}
