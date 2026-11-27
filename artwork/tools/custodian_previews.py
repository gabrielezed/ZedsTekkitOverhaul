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
from enderic_art import NEAREST, THIEF_HEAD, SABOTEUR_HEAD, crop, donor, faces, swatches

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
        for (int i = 20; i <= 28; i++) watcher.addObject(i, Integer.valueOf(0));
        watcher.updateObject(27, Integer.valueOf(Float.floatToIntBits(1)));
        frame(actor, "neutral", 1, 0, 0, 0, 0, 0);
        actor.maxHurtTime = 10;
        actor.hurtTime = 8;
        actor.attackedAtYaw = 55;
        frame(actor, "hurt impact", 1, 0, 0, 0, 0, 0);
        actor.hurtTime = 5;
        frame(actor, "hurt follow through", 1, 0, 0, 0, 0, 0);
        actor.hurtTime = 0;
        frame(actor, "contracted", 4 + Attack.PRESSURE.ordinal(), Attack.PRESSURE.windup - 1, 0, 0, 0, 0);
        frame(actor, "extended", 3, 12, 0, 0, 0, 0);
        frame(actor, "direct attack", 4, Attack.STRIKE.windup - 2, -1, 0, -4, 0);
        frame(actor, "fully extended arm", 4, Attack.STRIKE.windup + Attack.STRIKE.active - 1, -1, 4.85F, -4, 0);
        frame(actor, "radial pressure charge", 5, 21, 0, 0, 0, 0);
        frame(actor, "pressure release", 5, Attack.PRESSURE.windup + 3, 0, 0, 0, 0);
        frame(actor, "teleport preparation", 6, Attack.BLINK.windup - 2, 0, 0, 0, 0);
        frame(actor, "teleport arrival", 6, Attack.BLINK.windup + 13, 0, 0, 0, 0);
        frame(actor, "resonance", 7, Attack.RESONANCE.windup - 1, 0, 0, 0, 0);
        frame(actor, "recovery", 4, Attack.STRIKE.windup + Attack.STRIKE.active + 6, 1, 2.42F, 2, 0);
        frame(actor, "defeated", 0, 0, 0, 0, 0, 15);
        float[] sizes = {1, 0.70F, 0.38F, 0.80F};
        String[] names = {"compression normal", "compression intermediate", "fully compressed", "re-expansion"};
        for (int i = 0; i < sizes.length; i++) {
            watcher.updateObject(27, Integer.valueOf(Float.floatToIntBits(sizes[i])));
            frame(actor, names[i], 4 + Attack.COMPRESSION.ordinal(), 23 + i*15, 0, 0, 0, 0);
        }
        zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin caster =
                (zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin)
                allocator.allocateInstance(zed.tekkitoverhaul.exploration.abyssal.guardian.EntityAbyssalEnderkin.class);
        DataWatcher casting = new DataWatcher();
        casting.addObject(20, Byte.valueOf((byte) 0));
        field.set(caster, casting);
        java.lang.reflect.Constructor<?> aquaticModel =
                Class.forName(RenderAbyssalEnderkin.class.getName() + "$CasterModel").getDeclaredConstructor();
        aquaticModel.setAccessible(true);
        ModelBase model = (ModelBase) aquaticModel.newInstance();
        System.out.println("@abyssal neutral");
        model.render(caster, 0, 0, 24, 0, 0, 0.0625F);
        caster.hurtTime = 8;
        caster.maxHurtTime = 10;
        caster.attackedAtYaw = 55;
        System.out.println("@abyssal hurt");
        model.render(caster, 0, 0, 24, 0, 0, 0.0625F);
        caster.hurtTime = 5;
        System.out.println("@abyssal hurt settle");
        model.render(caster, 0, 0, 24, 0, 0, 0.0625F);
        caster.hurtTime = 0;
        casting.updateObject(20, Byte.valueOf((byte) 12));
        System.out.println("@abyssal casting");
        model.render(caster, 0, 0, 24, 0, 0, 0.0625F);
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
             ("Neutral side", "neutral", 90), ("Three-quarter", "neutral", 35),
             ("From above", "neutral", 26), ("From below", "neutral", 24)]
    views.extend((name.capitalize(), name, yaw) for name, yaw in (
        ("contracted", 28), ("extended", 25), ("direct attack", 35), ("fully extended arm", 70),
        ("radial pressure charge", 30), ("pressure release", 34), ("teleport preparation", 37), ("teleport arrival", 30),
        ("resonance", 23), ("recovery", 52), ("defeated", 16),
        ("compression normal", 27), ("compression intermediate", 27), ("fully compressed", 27), ("re-expansion", 27)))
    sheet = Image.new("RGB", (1120, ((len(views) + 3) // 4) * 252), "#111e28")
    font = ImageFont.load_default()
    for index, (label, name, yaw) in enumerate(views):
        card = Image.new("RGB", (278, 248), "#172a37")
        depth = [float("inf")] * (card.width * card.height)
        anchor = (221 if name == "fully extended arm" else 139, 157)
        pitch = -45 if label == "From below" else 38 if label == "From above" else -10
        draw(card, depth, frames[name], skin, yaw, pitch, 34, anchor)
        draw(card, depth, frames[name+" eyes"], eyes, yaw, pitch, 34, anchor, True)
        ImageDraw.Draw(card).text((12, 223), label, font=font, fill="#d3e2eb")
        sheet.paste(card, (280*(index % 4), 252*(index // 4)))
    sheet.save(OUT / "poses.png")

    scale = Image.new("RGB", (720, 435), "#172a37")
    depth = [float("inf")] * (scale.width * scale.height)
    with ZipFile(ROOT / "jars/bin/minecraft.jar") as archive:
        for name, texture, x in (("player", "mob/char.png", 124), ("enderman", "mob/enderman.png", 357)):
            tex = Image.open(BytesIO(archive.read(texture))).convert("RGBA")
            draw(scale, depth, frames[name], tex, 22, 0, 75, (x, 260))
    draw(scale, depth, frames["neutral"], skin, 22, 0, 75, (594, 260))
    draw(scale, depth, frames["neutral eyes"], eyes, 22, 0, 75, (594, 260), True)
    ink = ImageDraw.Draw(scale)
    ink.line((25, 373, 695, 373), fill="#455762")
    for x, label in ((124, "Native player"), (357, "Native Enderman"), (594, "Abyssal Custodian")):
        ink.text((x, 400), label, anchor="mm", fill="#d3e2eb", font=font)
    scale.save(OUT / "scale.png")
    lineup(frames, skin)
    hurt_sheet(frames, skin, eyes)


def hurt_sheet(frames, custodian, eyes):
    abyssal = Image.open(SOURCES / "enderkin.png").convert("RGBA")
    sheet = Image.new("RGB", (840, 570), "#141f2b")
    for row, (label, keys, skin, scale) in enumerate((
        ("Abyssal Enderkin", ("abyssal neutral", "abyssal hurt", "abyssal hurt settle"), abyssal, 110),
        ("Abyssal Custodian", ("neutral", "hurt impact", "hurt follow through"), custodian, 40),
    )):
        for col, (key, phase) in enumerate(zip(keys, ("Swim", "Impact", "Follow-through"))):
            card = Image.new("RGB", (278, 268), "#263448")
            depth = [float("inf")] * (card.width * card.height)
            anchor = (139, 170 if row else 66)
            draw(card, depth, frames[key], skin, 22, -10, scale, anchor)
            if row:
                draw(card, depth, frames[key + " eyes"], eyes, 22, -10, scale, anchor, True)
            ImageDraw.Draw(card).text((12, 247), label + " / " + phase, fill="#dce7f0")
            sheet.paste(card, (col * 280, row * 284))
    assert frames["abyssal neutral"] != frames["abyssal hurt"] != frames["abyssal hurt settle"]
    assert frames["neutral"] != frames["hurt impact"] != frames["hurt follow through"]
    sheet.save(OUT / "hurt-reactions.png")


def lineup(frames, custodian):
    sheet = Image.new("RGB", (930, 530), "#232938")
    draw_ink = ImageDraw.Draw(sheet)
    abyss = Image.open(SOURCES / "enderkin.png").convert("RGBA")
    items = [("Thief", donor("thief"), THIEF_HEAD), ("Saboteur", donor("saboteur"), SABOTEUR_HEAD),
             ("Revised Enderkin", abyss, (0,0,7,6,6)), ("Revised Custodian", custodian, (0,0,16,13,14))]
    previous = os.environ.get("ZTO_REFINEMENT_BASELINE")
    if previous:
        with ZipFile(previous) as archive:
            old = Image.open(BytesIO(archive.read("zed/textures/abyssal/enderkin.png"))).convert("RGBA")
        items.insert(2, ("Previous Enderkin", old, (0,0,7,5,6)))
    for i, (label, texture, head) in enumerate(items):
        x = 12+184*i
        tile = texture.resize((158,158), NEAREST)
        sheet.paste(tile,(x,28),tile)
        face = crop(texture, faces(*head)[1]).resize((140,92), NEAREST)
        sheet.paste(face, (x,206), face)
        draw_ink.text((x, 7), label, fill="#d9dfec")
        for index,color in enumerate(swatches(texture)):
            draw_ink.rectangle((x+index*23,308,x+index*23+21,329), fill=color[:3])
    for i,(key,yaw) in enumerate((("abyssal neutral", 0), ("abyssal neutral", 90), ("abyssal casting", 35))):
        card = Image.new("RGB",(180,160),"#2d3a48")
        draw(card,[float("inf")]*(180*160),frames[key],abyss,yaw,-10,90,(90,40))
        sheet.paste(card,(12+i*192,350))
        draw_ink.text((12+i*192,510),key, fill="#d9dfec")
    sheet.save(OUT / "enderic-family.png")


if __name__ == "__main__":
    main()
