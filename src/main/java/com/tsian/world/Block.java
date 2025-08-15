package com.tsian.world;

/**
 * 方块类 - 表示世界中的一个方块
 */
public class Block {
    
    public enum BlockType {
        AIR(0),
        GRASS(1),
        DIRT(2),
        STONE(3),
        LEAVES(4),
        WOOD_LOG(5),
        WOOD_PLANK(6),
        WATER(7);
        
        private final int id;
        
        BlockType(int id) {
            this.id = id;
        }
        
        public int getId() {
            return id;
        }
        
        public boolean isSolid() {
            return this != AIR && this != WATER;
        }
    }
    
    private final int x, y, z;
    private BlockType type;
    
    public Block(int x, int y, int z, BlockType type) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.type = type;
    }
    
    // Getter方法
    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public BlockType getType() { return type; }
    
    public void setType(BlockType type) {
        this.type = type;
    }
    
    public boolean isSolid() {
        return type.isSolid();
    }
    
    /**
     * 获取方块的纹理坐标 (u1, v1, u2, v2)
     * 4x3布局: 1草侧面 2泥土 3草上面 4圆石 5树叶 6橡木截面 7橡木侧面 8木板 9水 10-12破坏纹理
     */
    public float[] getTextureCoords(int face) {
        // face: 0=前, 1=后, 2=左, 3=右, 4=上, 5=下
        switch (type) {
            case GRASS:
                if (face == 4) { // 上面 - 草上表面 (位置3)
                    return new float[]{32.0f/64.0f, 0.0f, 48.0f/64.0f, 16.0f/48.0f};
                } else if (face == 5) { // 下面 - 泥土 (位置2)
                    return new float[]{16.0f/64.0f, 0.0f, 32.0f/64.0f, 16.0f/48.0f};
                } else { // 侧面 - 草方块侧面 (位置1)
                    return new float[]{0.0f, 0.0f, 16.0f/64.0f, 16.0f/48.0f};
                }
            case DIRT: // 泥方块表面 (位置2)
                return new float[]{16.0f/64.0f, 0.0f, 32.0f/64.0f, 16.0f/48.0f};
            case STONE: // 圆石表面 (位置4)
                return new float[]{48.0f/64.0f, 0.0f, 64.0f/64.0f, 16.0f/48.0f};
            case LEAVES: // 树叶表面 (位置5)
                return new float[]{0.0f/64.0f, 16.0f/48.0f, 16.0f/64.0f, 32.0f/48.0f};
            case WOOD_LOG:
                if (face == 4 || face == 5) { // 上下面 - 橡木截面 (位置6)
                    return new float[]{16.0f/64.0f, 16.0f/48.0f, 32.0f/64.0f, 32.0f/48.0f};
                } else { // 侧面 - 橡木侧面 (位置7)
                    return new float[]{32.0f/64.0f, 16.0f/48.0f, 48.0f/64.0f, 32.0f/48.0f};
                }
            case WOOD_PLANK: // 木板 (位置8)
                return new float[]{48.0f/64.0f, 16.0f/48.0f, 64.0f/64.0f, 32.0f/48.0f};
            case WATER: // 水静态纹理 (位置9)
                return new float[]{0.0f/64.0f, 32.0f/48.0f, 16.0f/64.0f, 48.0f/48.0f};
            default:
                return new float[]{0.0f, 0.0f, 16.0f/64.0f, 16.0f/48.0f};
        }
    }
}