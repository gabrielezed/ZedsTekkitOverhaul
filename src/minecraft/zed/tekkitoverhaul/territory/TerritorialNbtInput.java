package zed.tekkitoverhaul.territory;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;

/** Reads the small subset of legacy NBT this dataset owns, checking sizes before allocation. */
final class TerritorialNbtInput {
    private static final int END = 0, INT = 3, LONG = 4, LIST = 9, COMPOUND = 10;

    private TerritorialNbtInput() { }

    static NBTTagCompound read(InputStream source) throws IOException {
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(new GZIPInputStream(source)))) {
            if (input.readUnsignedByte() != COMPOUND || !readName(input).isEmpty()) throw invalid();
            NBTTagCompound root = readCompound(input, 0);
            if (input.read() != -1) throw invalid();
            return root;
        }
    }

    private static NBTTagCompound readCompound(DataInputStream input, int depth) throws IOException {
        NBTTagCompound tag = new NBTTagCompound();
        for (int fields = 0; fields <= 8; fields++) {
            int type = input.readUnsignedByte();
            if (type == END) return tag;
            if (fields == 8) throw invalid();
            String name = readName(input);
            if (name.isEmpty() || tag.hasKey(name)) throw invalid();
            if (type == INT) tag.setInteger(name, input.readInt());
            else if (type == LONG) tag.setLong(name, input.readLong());
            else if (type == COMPOUND && depth == 0 && "data".equals(name)) tag.setTag(name, readCompound(input, 1));
            else if (type == LIST && depth == 1 && "Regions".equals(name)) tag.setTag(name, readRegions(input));
            else throw invalid();
        }
        throw invalid();
    }

    private static NBTTagList readRegions(DataInputStream input) throws IOException {
        int element = input.readUnsignedByte();
        int count = input.readInt();
        if (count < 0 || count > TerritorialActivity.CAPACITY || (count > 0 && element != COMPOUND)) throw invalid();
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < count; i++) list.appendTag(readCompound(input, 2));
        return list;
    }

    private static String readName(DataInputStream input) throws IOException {
        int length = input.readUnsignedShort();
        if (length > 24) throw invalid();
        char[] name = new char[length];
        for (int i = 0; i < length; i++) {
            int value = input.readUnsignedByte();
            if (value < 32 || value > 126) throw invalid();
            name[i] = (char) value;
        }
        return new String(name);
    }

    private static IOException invalid() { return new IOException("Invalid territorial NBT framing"); }
}
