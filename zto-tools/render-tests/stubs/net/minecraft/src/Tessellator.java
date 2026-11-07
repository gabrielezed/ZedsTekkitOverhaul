package net.minecraft.src;

/** Test-only stub: not included in the Minecraft runtime or release JAR. */
public final class Tessellator {
    public static final Tessellator instance = new Tessellator();
    public int mode, brightness, vertexCount, drawings;
    private boolean drawing;

    public void startDrawing(int mode) {
        this.mode = mode;
        drawing = true;
        brightness = -1;
        vertexCount = 0;
    }

    public void setBrightness(int brightness) {
        if (!drawing) throw new IllegalStateException("Not drawing");
        this.brightness = brightness;
    }

    public void setNormal(float x, float y, float z) {
        if (brightness == -1) throw new IllegalStateException("Missing lightmap brightness");
    }

    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        if (brightness == -1) throw new IllegalStateException("Missing lightmap brightness");
        vertexCount++;
    }

    public void draw() {
        if (!drawing) throw new IllegalStateException("Not drawing");
        drawing = false;
        drawings++;
    }
}
