package com.winlator.xr;

/** XR hooks are disabled in the embedded GLES display backend. */
public final class XrActivity {
    private XrActivity() {}
    public static boolean isEnabled(Object context) { return false; }
    public static XrActivity getInstance() { return new XrActivity(); }
    public void bindFramebuffer() {}
}
