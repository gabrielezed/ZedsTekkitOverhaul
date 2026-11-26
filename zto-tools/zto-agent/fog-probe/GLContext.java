package org.lwjgl.opengl;

/** The fixture supplies an empty capability set for the native air branch. */
public final class GLContext {
    public static ContextCapabilities capabilities;

    public static ContextCapabilities getCapabilities() {
        return capabilities;
    }
}
