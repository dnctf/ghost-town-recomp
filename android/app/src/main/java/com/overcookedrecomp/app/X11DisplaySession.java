package com.overcookedrecomp.app;

import android.content.Context;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.winlator.cmod.xconnector.UnixSocketConfig;
import com.winlator.cmod.xenvironment.components.SysVSharedMemoryComponent;
import com.winlator.cmod.xenvironment.components.VirGLRendererComponent;
import com.winlator.cmod.xenvironment.components.XServerComponent;
import com.winlator.cmod.widget.XServerView;
import com.winlator.cmod.xserver.Pointer;
import com.winlator.cmod.xserver.ScreenInfo;
import com.winlator.cmod.xserver.XServer;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the Android-side X11 + VirGL services for one guest display session.
 * The guest rootfs is also the socket root, so PRoot sees the same Unix sockets
 * without a separate Android-side mount or root permission.
 */
public final class X11DisplaySession implements AutoCloseable {
    public interface Listener {
        void onStatus(String message);
        void onError(String message, Throwable error);
    }

    private final FrameLayout host;
    private final XServer xServer;
    private final XServerView view;
    private final XServerComponent xServerComponent;
    private final SysVSharedMemoryComponent sharedMemoryComponent;
    private final VirGLRendererComponent virglRendererComponent;
    private final Listener listener;
    private final AtomicBoolean componentsStarted = new AtomicBoolean(false);
    private volatile boolean closed;

    public X11DisplaySession(
            Context context,
            FrameLayout host,
            File guestRootfs,
            int width,
            int height,
            Listener listener) {
        if (guestRootfs == null || !guestRootfs.isDirectory()) {
            throw new IllegalArgumentException("Guest rootfs is not available.");
        }
        this.host = host;
        this.listener = listener;
        xServer = new XServer(new ScreenInfo(width, height));
        view = new XServerView(context, xServer, this::startComponentsAfterEgl);
        xServer.setRenderer(view.getRenderer());

        String socketRoot = guestRootfs.getAbsolutePath();
        xServerComponent = new XServerComponent(
                xServer,
                UnixSocketConfig.createSocket(socketRoot, UnixSocketConfig.XSERVER_PATH));
        sharedMemoryComponent = new SysVSharedMemoryComponent(
                xServer,
                UnixSocketConfig.createSocket(socketRoot, UnixSocketConfig.SYSVSHM_SERVER_PATH));
        virglRendererComponent = new VirGLRendererComponent(
                xServer,
                UnixSocketConfig.createSocket(socketRoot, UnixSocketConfig.VIRGL_SERVER_PATH));

        view.setOnTouchListener(this::handleTouch);
        host.addView(view, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private void startComponentsAfterEgl() {
        if (closed || !componentsStarted.compareAndSet(false, true)) return;
        try {
            xServerComponent.start();
            sharedMemoryComponent.start();
            virglRendererComponent.start();
            if (listener != null) listener.onStatus("X11 server and VirGL renderer started.");
        } catch (Throwable error) {
            try { virglRendererComponent.stop(); } catch (Throwable ignored) {}
            try { sharedMemoryComponent.stop(); } catch (Throwable ignored) {}
            try { xServerComponent.stop(); } catch (Throwable ignored) {}
            componentsStarted.set(false);
            if (listener != null) listener.onError("Unable to start X11/VirGL services.", error);
        }
    }

    private boolean handleTouch(android.view.View target, MotionEvent event) {
        if (closed) return false;
        int viewWidth = Math.max(1, target.getWidth());
        int viewHeight = Math.max(1, target.getHeight());
        int x = Math.max(0, Math.min(viewWidth - 1, (int) event.getX()));
        int y = Math.max(0, Math.min(viewHeight - 1, (int) event.getY()));
        xServer.injectPointerMove(x * xServer.screenInfo.width / viewWidth,
                y * xServer.screenInfo.height / viewHeight);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                xServer.injectPointerButtonPress(Pointer.Button.BUTTON_LEFT);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                xServer.injectPointerButtonRelease(Pointer.Button.BUTTON_LEFT);
                return true;
            case MotionEvent.ACTION_MOVE:
                return true;
            default:
                return true;
        }
    }

    public XServerView getView() {
        return view;
    }

    public boolean isReady() {
        return componentsStarted.get() && !closed;
    }

    public void onResume() {
        if (!closed) view.onResume();
    }

    public void onPause() {
        if (!closed) view.onPause();
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        if (componentsStarted.getAndSet(false)) {
            try { virglRendererComponent.stop(); } catch (Throwable ignored) {}
            try { sharedMemoryComponent.stop(); } catch (Throwable ignored) {}
            try { xServerComponent.stop(); } catch (Throwable ignored) {}
        }
        view.onPause();
        xServer.cursorLocker.cancel();
        host.removeView(view);
    }
}
