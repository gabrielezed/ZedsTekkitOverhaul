package zed.tekkitoverhaul.crafting;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.src.CompressedStreamTools;
import net.minecraft.src.Item;
import net.minecraft.src.ItemStack;
import net.minecraft.src.NBTBase;
import net.minecraft.src.NBTTagCompound;
import net.minecraft.src.NBTTagList;

final class RecipeBookmarks {
    private static final int LIMIT = 12;
    private final File file;
    private final List<Entry> entries = new ArrayList<Entry>();
    private int next;

    RecipeBookmarks(File gameDirectory) throws IOException {
        file = new File(gameDirectory, "config/ZTORecipeBookmarks.dat");
        NBTTagCompound saved = CompressedStreamTools.read(file);
        if (saved == null) return;
        NBTBase recipes = saved.getTag("recipes");
        if (!(recipes instanceof NBTTagList)) return;
        NBTTagList list = (NBTTagList) recipes;
        for (int index = 0; index < list.tagCount() && entries.size() < LIMIT; index++) {
            if (!(list.tagAt(index) instanceof NBTTagCompound)) continue;
            NBTTagCompound tag = (NBTTagCompound) list.tagAt(index);
            try {
                NBTTagCompound itemTag = tag.getCompoundTag("item");
                int id = itemTag.getShort("id");
                if (!itemTag.hasKey("id") || id < 0 || id >= Item.itemsList.length || Item.itemsList[id] == null) continue;
                ItemStack item = ItemStack.loadItemStackFromNBT(itemTag);
                if (item != null && item.stackSize > 0) {
                    entries.add(new Entry(item, tag.getBoolean("usage"), tag.getString("category"), tag.getInteger("page")));
                }
            } catch (ClassCastException malformed) {
                // Keep the other bookmarks when a single entry has invalid NBT field types.
            }
        }
    }

    String toggle(Entry entry) throws IOException {
        List<Entry> updated = new ArrayList<Entry>(entries);
        boolean removed = false;
        for (int index = 0; index < updated.size(); index++) {
            if (updated.get(index).same(entry)) {
                updated.remove(index);
                removed = true;
                break;
            }
        }
        if (!removed) {
            if (updated.size() == LIMIT) return "Bookmark list is full";
            updated.add(0, entry);
        }
        save(updated);
        entries.clear();
        entries.addAll(updated);
        next = 0;
        return removed ? "Bookmark removed" : "Recipe bookmarked";
    }

    Entry advance() {
        if (entries.isEmpty()) return null;
        Entry entry = entries.get(next);
        next = (next + 1) % entries.size();
        return entry;
    }

    private void save(List<Entry> updated) throws IOException {
        NBTTagCompound root = new NBTTagCompound();
        NBTTagList list = new NBTTagList();
        for (Entry entry : updated) {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setCompoundTag("item", entry.item.writeToNBT(new NBTTagCompound()));
            tag.setBoolean("usage", entry.usage);
            tag.setString("category", entry.category);
            tag.setInteger("page", entry.page);
            list.appendTag(tag);
        }
        root.setTag("recipes", list);
        File temporary = File.createTempFile("ZTORecipeBookmarks", ".tmp", file.getParentFile());
        try {
            CompressedStreamTools.write(root, temporary);
            try {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException unavailable) {
                Files.move(temporary.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary.toPath());
        }
    }

    static final class Entry {
        final ItemStack item;
        final boolean usage;
        final String category;
        final int page;

        Entry(ItemStack item, boolean usage, String category, int page) {
            this.item = item.copy();
            this.usage = usage;
            this.category = category;
            this.page = Math.max(0, page);
        }

        boolean same(Entry other) {
            return usage == other.usage && page == other.page && category.equals(other.category)
                    && InventoryPlan.sameType(item, other.item);
        }
    }
}
