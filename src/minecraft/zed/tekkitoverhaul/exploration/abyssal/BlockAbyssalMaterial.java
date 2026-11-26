package zed.tekkitoverhaul.exploration.abyssal;

import net.minecraft.src.Block;
import net.minecraft.src.Material;

final class BlockAbyssalMaterial extends Block {
    static final String TEXTURE = "/zed/textures/abyssal/materials.png";

    BlockAbyssalMaterial(int id, int texture, String key, float hardness, boolean sediment, int light) {
        super(id, texture, sediment ? Material.ground : Material.rock);
        setBlockName("zto." + key);
        setHardness(hardness);
        setResistance(sediment ? 0.5F : 10.0F);
        setStepSound(sediment ? soundGravelFootstep : soundStoneFootstep);
        setLightValue(light / 15.0F);
    }

    @Override
    public String getTextureFile() {
        return TEXTURE;
    }
}
