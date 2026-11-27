"""Capture real MCP ModelBox vertices, articulation and UVs with a headless display-list recorder."""

from io import BytesIO
from math import ceil, floor, radians, cos, sin, sqrt
import os
from pathlib import Path
import subprocess
import tempfile
from zipfile import ZipFile

from PIL import Image, ImageDraw, ImageFont

from custodian_assets import ART, SOURCES

ROOT = ART.parent
OUT = ART / "previews" / "custodian"

GL = r"""
package org.lwjgl.opengl;
import java.util.*;
public final class GL11 {
    public static final int GL_COMPILE = 4864;
    private static final Map<Integer, List<double[]>> lists = new HashMap<Integer, List<double[]>>();
    private static final Deque<double[][]> stack = new ArrayDeque<double[][]>();
    private static double[][] matrix = identity();
    private static int next = 1, compiling;
    public static int glGenLists(int count) { int start = next; next += count; return start; }
    public static void glNewList(int id, int mode) { compiling = id; lists.put(id, new ArrayList<double[]>()); }
    public static void glEndList() { compiling = 0; }
    public static void record(double[] vertex) { lists.get(compiling).add(vertex); }
    public static void glPushMatrix() { stack.push(matrix); }
    public static void glPopMatrix() { matrix = stack.pop(); }
    public static void glTranslatef(float x, float y, float z) {
        double[][] transform = identity();
        transform[0][3] = x; transform[1][3] = y; transform[2][3] = z;
        multiply(transform);
    }
    public static void glScalef(float x, float y, float z) {
        double[][] transform = identity();
        transform[0][0] = x; transform[1][1] = y; transform[2][2] = z;
        multiply(transform);
    }
    public static void glRotatef(float degrees, float x, float y, float z) {
        int axis = x != 0 ? 0 : y != 0 ? 1 : 2;
        int a = (axis + 1) % 3, b = (axis + 2) % 3;
        double angle = Math.toRadians(degrees), c = Math.cos(angle), s = Math.sin(angle);
        double[][] transform = identity();
        transform[a][a] = transform[b][b] = c;
        transform[a][b] = -s; transform[b][a] = s;
        multiply(transform);
    }
    private static double[][] identity() {
        double[][] result = new double[4][4];
        for (int i = 0; i < 4; i++) result[i][i] = 1;
        return result;
    }
    private static void multiply(double[][] transform) {
        double[][] result = new double[4][4];
        for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++)
            for (int j = 0; j < 4; j++) result[row][col] += matrix[row][j] * transform[j][col];
        matrix = result;
    }
    public static void glCallList(int id) {
        for (double[] vertex : lists.get(id)) {
            double[] point = {vertex[0], vertex[1], vertex[2], 1};
            for (int row = 0; row < 3; row++) {
                double coordinate = 0;
                for (int j = 0; j < 4; j++) coordinate += matrix[row][j] * point[j];
                System.out.print(coordinate + " ");
            }
            System.out.println(vertex[3] + " " + vertex[4]);
        }
    }
}
"""

TESSELLATOR = r"""
package net.minecraft.src;
import org.lwjgl.opengl.GL11;
public final class Tessellator {
    public static final Tessellator instance = new Tessellator();
    public void startDrawingQuads() {}
    public void setNormal(float x, float y, float z) {}
    public int draw() { return 0; }
    public void addVertexWithUV(double x, double y, double z, double u, double v) {
        GL11.record(new double[] {x, y, z, u, v});
    }
}
"""

CAPTURE = r"""
package zed.tekkitoverhaul.exploration.abyssal.guardian.client;
import java.lang.reflect.Field;
import net.minecraft.src.*;
import org.lwjgl.opengl.GL11;
import zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalCustodian;
import zed.tekkitoverhaul.exploration.abyssal.guardian.CustodianBalance.Attack;

public final class Capture {
    public static void main(String[] args) throws Exception {
        Field access = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        access.setAccessible(true);
        sun.misc.Unsafe allocator = (sun.misc.Unsafe) access.get(null);
        EntityAbyssalCustodian actor = (EntityAbyssalCustodian) allocator.allocateInstance(EntityAbyssalCustodian.class);
        Field field = Entity.class.getDeclaredField("dataWatcher");
        field.setAccessible(true);
        DataWatcher watcher = new DataWatcher();
        field.set(actor, watcher);
        for (int i = 20; i <= 26; i++) watcher.addObject(i, Integer.valueOf(0));
        frame(actor, "neutral", 1, 0, 0, 0, 0, 0);
        frame(actor, "contracted", 4 + Attack.PRESSURE.ordinal(), Attack.PRESSURE.windup - 1, 0, 0, 0, 0);
        frame(actor, "extended", 3, 12, 0, 0, 0, 0);
        frame(actor, "direct attack", 4, Attack.STRIKE.windup - 2, -1, 0, -4, 0);
        frame(actor, "fully extended arm", 4, Attack.STRIKE.windup + Attack.STRIKE.active - 1, -1, 4.85F, -4, 0);
        frame(actor, "radial pressure charge", 5, 21, 0, 0, 0, 0);
        frame(actor, "teleport preparation", 6, Attack.BLINK.windup - 2, 0, 0, 0, 0);
        frame(actor, "teleport arrival", 6, Attack.BLINK.windup + 13, 0, 0, 0, 0);
        frame(actor, "resonance", 7, Attack.RESONANCE.windup - 1, 0, 0, 0, 0);
        frame(actor, "recovery", 4, Attack.STRIKE.windup + Attack.STRIKE.active + 6, 1, 2.42F, 2, 0);
        frame(actor, "defeated", 0, 0, 0, 0, 0, 15);
        System.out.println("@enderman");
        new ModelEnderman().render(null, 0, 0, 0, 0, 0, 0.0625F);
        System.out.println("@player");
        new ModelBiped().render(null, 0, 0, 0, 0, 0, 0.0625F);
    }

    private static void frame(EntityAbyssalCustodian actor, String name, int pose, int tick, int arm,
                              float reach, float pitch, int death) {
        DataWatcher watcher = actor.getDataWatcher();
        watcher.updateObject(20, Integer.valueOf(pose));
        watcher.updateObject(21, Integer.valueOf(tick));
        watcher.updateObject(22, Integer.valueOf(arm));
        watcher.updateObject(23, Integer.valueOf(Float.floatToIntBits(reach)));
        watcher.updateObject(24, Integer.valueOf(Float.floatToIntBits(pitch)));
        watcher.updateObject(26, Integer.valueOf(1));
        actor.deathTime = death;
        GL11.glPushMatrix();
        if (death > 0) GL11.glRotatef(-78, 0, 0, 1);
        System.out.println("@" + name);
        new CustodianModel(false).render(actor, 0, 0, 38, 0, 0, 0.0625F);
        System.out.println("@" + name + " eyes");
        new CustodianModel(true).render(actor, 0, 0, 38, 0, 0, 0.0625F);
        GL11.glPopMatrix();
    }
}
"""


def capture():
    with tempfile.TemporaryDirectory(prefix="zto-custodian-preview-") as folder:
        classes = Path(folder)
        paths = []
        for filename, code in (("GL11.java", GL), ("Tessellator.java", TESSELLATOR), ("Capture.java", CAPTURE)):
            path = classes / filename
            path.write_text(code)
            paths.append(path)
        game = ROOT / "bin" / "minecraft"
        cp = os.pathsep.join(str(p) for p in (game, ROOT / "jars/bin/lwjgl.jar"))
        subprocess.run(["javac", "-cp", cp, "-d", classes, *paths], check=True)
        result = subprocess.check_output([
            "java", "-cp", os.pathsep.join((folder, cp)),
            "zed.tekkitoverhaul.exploration.abyssal.guardian.client.Capture",
        ], text=True)
    frames = {}
    for line in result.splitlines():
        if line.startswith("@"):
            frame = frames.setdefault(line[1:], [])
        else:
            frame.append(tuple(map(float, line.split())))
    return frames


def project(vertex, yaw, pitch, scale, anchor, bias):
    x, y, z, u, v = vertex
    x, z = x*cos(yaw) + z*sin(yaw), -x*sin(yaw) + z*cos(yaw)
    y, z = y*cos(pitch) - z*sin(pitch), y*sin(pitch) + z*cos(pitch)
    return anchor[0] + scale*x, anchor[1] + scale*y, z-bias, u, v


def triangle(pixels, dimensions, depth, tex, vertices, bright):
    a, b, c = vertices
    x0, y0, z0, u0, v0 = a
    dx1, dy1, dz1, du1, dv1 = (b[i]-a[i] for i in range(5))
    dx2, dy2, dz2, du2, dv2 = (c[i]-a[i] for i in range(5))
    area = dx1*dy2 - dy1*dx2
    if area >= -0.001:
        return
    nx, ny, nz = dy1*dz2-dz1*dy2, dz1*dx2-dx1*dz2, area
    normal = sqrt(nx*nx+ny*ny+nz*nz)
    light = 1 if bright else 0.66 + max(0, (-nx-ny-nz)/normal) * 0.41
    width, height = dimensions
    texture = tex.load()
    for y in range(max(0, floor(min(y0, b[1], c[1]))), min(height, ceil(max(y0, b[1], c[1])))):
        for x in range(max(0, floor(min(x0, b[0], c[0]))), min(width, ceil(max(x0, b[0], c[0])))):
            px, py = x + 0.5 - x0, y + 0.5 - y0
            s, t = (px*dy2-py*dx2)/area, (py*dx1-px*dy1)/area
            if s < 0 or t < 0 or s+t > 1:
                continue
            z = z0 + s*dz1 + t*dz2
            index = x+y*width
            if z >= depth[index]:
                continue
            u = max(0, min(tex.width-1, int((u0+s*du1+t*du2)*tex.width)))
            v = max(0, min(tex.height-1, int((v0+s*dv1+t*dv2)*tex.height)))
            color = texture[u, v]
            if color[3]:
                pixels[x, y] = tuple(min(255, round(channel*light)) for channel in color[:3])
                depth[index] = z


def draw(image, depth, vertices, tex, yaw, pitch, scale, anchor, glow=False):
    points = [project(v, radians(yaw), radians(pitch), scale, anchor, 0.001 if glow else 0) for v in vertices]
    for i in range(0, len(points), 4):
        a, b, c, d = points[i:i+4]
        triangle(image.load(), image.size, depth, tex, (a, b, c), glow)
        triangle(image.load(), image.size, depth, tex, (a, c, d), glow)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    frames = capture()
    skin = Image.open(SOURCES / "custodian.png").convert("RGBA")
    eyes = Image.open(SOURCES / "custodian_eyes.png").convert("RGBA")
    views = [("Neutral front", "neutral", 0), ("Neutral rear", "neutral", 180),
             ("Neutral side", "neutral", 90), ("Three-quarter", "neutral", 35)]
    views.extend((name.capitalize(), name, yaw) for name, yaw in (
        ("contracted", 28), ("extended", 25), ("direct attack", 35), ("fully extended arm", 70),
        ("radial pressure charge", 30), ("teleport preparation", 37), ("teleport arrival", 30),
        ("resonance", 23), ("recovery", 52), ("defeated", 16)))
    sheet = Image.new("RGB", (1120, 4*252), "#111e28")
    font = ImageFont.load_default()
    for index, (label, name, yaw) in enumerate(views):
        card = Image.new("RGB", (278, 248), "#172a37")
        depth = [float("inf")] * (card.width * card.height)
        anchor = (239 if name == "fully extended arm" else 139, 142)
        draw(card, depth, frames[name], skin, yaw, -10, 49, anchor)
        draw(card, depth, frames[name+" eyes"], eyes, yaw, -10, 49, anchor, True)
        ImageDraw.Draw(card).text((12, 223), label, font=font, fill="#d3e2eb")
        sheet.paste(card, (280*(index % 4), 252*(index // 4)))
    sheet.save(OUT / "poses.png")

    scale = Image.new("RGB", (720, 345), "#172a37")
    depth = [float("inf")] * (scale.width * scale.height)
    with ZipFile(ROOT / "jars/bin/minecraft.jar") as archive:
        for name, texture, x in (("player", "mob/char.png", 124), ("enderman", "mob/enderman.png", 357)):
            tex = Image.open(BytesIO(archive.read(texture))).convert("RGBA")
            draw(scale, depth, frames[name], tex, 22, 0, 75, (x, 179))
    draw(scale, depth, frames["neutral"], skin, 22, 0, 75, (594, 179))
    draw(scale, depth, frames["neutral eyes"], eyes, 22, 0, 75, (594, 179), True)
    ink = ImageDraw.Draw(scale)
    ink.line((25, 292, 695, 292), fill="#455762")
    for x, label in ((124, "Native player"), (357, "Native Enderman"), (594, "Abyssal Custodian")):
        ink.text((x, 316), label, anchor="mm", fill="#d3e2eb", font=font)
    scale.save(OUT / "scale.png")


if __name__ == "__main__":
    main()
