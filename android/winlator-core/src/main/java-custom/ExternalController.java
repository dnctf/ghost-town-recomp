package com.winlator.cmod.inputcontrols;

import android.view.InputDevice;

/** Input classification shim; Ghost Town forwards Android controls directly to X11. */
public final class ExternalController {
    private ExternalController() {}
    public static boolean isGameController(InputDevice device) {
        return device != null && (device.getSources() & InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD;
    }
}
