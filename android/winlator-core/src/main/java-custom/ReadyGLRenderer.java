package com.winlator.cmod.renderer;

import com.winlator.cmod.widget.XServerView;
import com.winlator.cmod.xserver.XServer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/** GLRenderer variant that signals the session after its EGL context is initialized. */
public final class ReadyGLRenderer extends GLRenderer {
    private final Runnable onReady;
    private boolean notified;

    public ReadyGLRenderer(XServerView view, XServer xServer, Runnable onReady) {
        super(view, xServer);
        this.onReady = onReady;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        super.onSurfaceCreated(gl, config);
        if (!notified) {
            notified = true;
            if (onReady != null) onReady.run();
        }
    }
}
