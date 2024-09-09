package com.craftworks.music.ui.elements

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

// Taken from this Medium article:
// https://blog.canopas.com/jetpack-compose-cool-button-click-effects-c6bbecec7bcb

enum class ButtonState { Pressed, Idle }
fun Modifier.bounceClick() = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val scale by animateFloatAsState(if (buttonState == ButtonState.Pressed) 0.9f else 1f,
        label = "Animated Button Scale",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )

    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
//        .clickable(
//            interactionSource = remember { MutableInteractionSource() },
//            indication = null,
//            onClick = {  }
//        )
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState == ButtonState.Pressed) {
                    waitForUpOrCancellation()
                    ButtonState.Idle
                } else {
                    awaitFirstDown(false)
                    ButtonState.Pressed
                }
            }
        }
}

fun Modifier.moveClick(right: Boolean) = composed {
    var buttonState by remember { mutableStateOf(ButtonState.Idle) }
    val position by animateDpAsState(if (buttonState == ButtonState.Pressed) 12.dp else 0.dp,
        label = "Animated Button Position",
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        )

    )

    this
        .offset{
            IntOffset(x = if (right) position.toPx().toInt() else -position.toPx().toInt(), y= 0)
        }
//        .clickable(
//            interactionSource = remember { MutableInteractionSource() },
//            indication = null,
//            onClick = {  }
//        )
        .pointerInput(buttonState) {
            awaitPointerEventScope {
                buttonState = if (buttonState == ButtonState.Pressed) {
                    waitForUpOrCancellation()
                    ButtonState.Idle
                } else {
                    awaitFirstDown(false)
                    ButtonState.Pressed
                }
            }
        }
}