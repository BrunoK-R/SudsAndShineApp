package com.sudsmobile.shared.ui

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.onFocusedBoundsChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.unit.dp

class KeyboardDismissState internal constructor() {
    internal var focusedCoordinates by mutableStateOf<LayoutCoordinates?>(null)
    internal var focusManager: FocusManager? = null
    internal var keyboardController: SoftwareKeyboardController? = null
}

@Composable
fun rememberKeyboardDismissState(): KeyboardDismissState = remember { KeyboardDismissState() }

@Composable
fun Modifier.observeFocusedInput(state: KeyboardDismissState): Modifier {
    state.focusManager = LocalFocusManager.current
    state.keyboardController = LocalSoftwareKeyboardController.current
    return onFocusedBoundsChanged { state.focusedCoordinates = it }
}

/** Dismisses the keyboard after a tap outside the currently focused input, without consuming it. */
@Composable
fun Modifier.dismissKeyboardOnOutsideTap(): Modifier {
    val state = rememberKeyboardDismissState()
    return observeFocusedInput(state).dismissKeyboardOnOutsideTap(state)
}

/** Use one state across a dialog surface and its content when they have separate focus hierarchies. */
@Composable
fun Modifier.dismissKeyboardOnOutsideTap(state: KeyboardDismissState): Modifier {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val containerCoordinates = remember { mutableStateOf<LayoutCoordinates?>(null) }

    return this
        .onGloballyPositioned { containerCoordinates.value = it }
        .pointerInput(focusManager, keyboardController, state) {
            awaitEachGesture {
                val down = awaitFirstDown(
                    requireUnconsumed = false,
                    pass = PointerEventPass.Initial,
                )
                val container = containerCoordinates.value
                val focused = state.focusedCoordinates
                val focusedBounds = if (container?.isAttached == true && focused?.isAttached == true) {
                    container.localBoundingBoxOf(focused, clipBounds = false)
                } else {
                    null
                }
                val padding = 12.dp.toPx()
                val outsideFocusedInput = focusedBounds != null && !Rect(
                    focusedBounds.left - padding,
                    focusedBounds.top - padding,
                    focusedBounds.right + padding,
                    focusedBounds.bottom + padding,
                ).contains(down.position)
                if (waitForUpOrCancellation(pass = PointerEventPass.Initial) != null &&
                    outsideFocusedInput
                ) {
                    (state.focusManager ?: focusManager).clearFocus()
                    (state.keyboardController ?: keyboardController)?.hide()
                }
            }
        }
}
