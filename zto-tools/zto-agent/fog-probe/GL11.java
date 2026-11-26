package org.lwjgl.opengl;

import java.nio.FloatBuffer;

/** Headless fog sink, compiled only for the underwater probe. */
public final class GL11 {
    public static float fogDensity;
    public static int fogMode;

    public static void glFogf(int parameter, float value) {
        if (parameter == 2914) fogDensity = value;
    }

    public static void glFog(int parameter, FloatBuffer values) {}
    public static void glFogi(int parameter, int value) {
        if (parameter == 2917) fogMode = value;
    }
    public static void glNormal3f(float x, float y, float z) {}
    public static void glColor4f(float r, float g, float b, float a) {}
    public static void glEnable(int capability) {}
    public static void glColorMaterial(int face, int mode) {}
}
