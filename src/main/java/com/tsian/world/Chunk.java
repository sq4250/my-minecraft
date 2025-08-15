package com.tsian.world;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块类 - 代表一个16x16的方块区域
 */
public class Chunk {
    
    public static final int CHUNK_SIZE = 16; // 区块大小16x16
    public static final int WORLD_HEIGHT = 256; // 世界高度
    
    private final int chunkX, chunkZ; // 区块坐标
    private final Map<String, Block> blocks; // 区块内的方块
    private boolean isGenerated; // 是否已生成地形
    private boolean needsRebuild; // 是否需要重新构建渲染数据
    
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new HashMap<>();
        this.isGenerated = false;
        this.needsRebuild = true;
    }
    
    /**
     * 生成超平坦地形
     * 从上到下：草方块(y=3) 泥块(y=2) 泥块(y=1) 圆石(y=0)
     */
    public void generateFlatTerrain() {
        if (isGenerated) return;
        
        // 生成16x16区域的超平坦地形
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                // 计算世界坐标
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;
                
                // 生成4层方块
                setBlock(worldX, 0, worldZ, Block.BlockType.STONE);    // 底层：圆石
                setBlock(worldX, 1, worldZ, Block.BlockType.DIRT);     // 第二层：泥块
                setBlock(worldX, 2, worldZ, Block.BlockType.DIRT);     // 第三层：泥块
                setBlock(worldX, 3, worldZ, Block.BlockType.GRASS);    // 顶层：草方块
            }
        }
        
        isGenerated = true;
        needsRebuild = true;
        System.out.println("Generated flat terrain for chunk (" + chunkX + ", " + chunkZ + ")");
    }
    
    /**
     * 在区块中设置方块
     */
    public void setBlock(int worldX, int worldY, int worldZ, Block.BlockType type) {
        String key = getBlockKey(worldX, worldY, worldZ);
        Block existingBlock = blocks.get(key);
        
        if (existingBlock != null) {
            existingBlock.setType(type);
        } else {
            blocks.put(key, new Block(worldX, worldY, worldZ, type));
        }
        needsRebuild = true;
    }
    
    /**
     * 获取区块中的方块
     */
    public Block getBlock(int worldX, int worldY, int worldZ) {
        return blocks.get(getBlockKey(worldX, worldY, worldZ));
    }
    
    /**
     * 检查世界坐标是否在此区块内
     */
    public boolean containsBlock(int worldX, int worldZ) {
        int minX = chunkX * CHUNK_SIZE;
        int maxX = minX + CHUNK_SIZE - 1;
        int minZ = chunkZ * CHUNK_SIZE;
        int maxZ = minZ + CHUNK_SIZE - 1;
        
        return worldX >= minX && worldX <= maxX && worldZ >= minZ && worldZ <= maxZ;
    }
    
    /**
     * 获取方块键值
     */
    private String getBlockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
    
    /**
     * 获取区块内所有方块
     */
    public Map<String, Block> getBlocks() {
        return blocks;
    }
    
    /**
     * 移除方块（设置为空气）
     */
    public boolean removeBlock(int worldX, int worldY, int worldZ) {
        Block block = getBlock(worldX, worldY, worldZ);
        if (block != null && block.getType() != Block.BlockType.AIR) {
            block.setType(Block.BlockType.AIR);
            needsRebuild = true;
            return true;
        }
        return false;
    }
    
    /**
     * 添加方块到区块
     */
    public boolean addBlock(int worldX, int worldY, int worldZ, Block.BlockType type) {
        if (!containsBlock(worldX, worldZ)) {
            return false; // 不在此区块范围内
        }
        
        Block existingBlock = getBlock(worldX, worldY, worldZ);
        if (existingBlock != null && existingBlock.getType() != Block.BlockType.AIR) {
            return false; // 位置已被占用
        }
        
        setBlock(worldX, worldY, worldZ, type);
        return true;
    }
    
    // Getter方法
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean isGenerated() { return isGenerated; }
    public boolean needsRebuild() { return needsRebuild; }
    
    public void markRebuilt() {
        needsRebuild = false;
    }
    
    /**
     * 获取区块的字符串表示
     */
    @Override
    public String toString() {
        return "Chunk(" + chunkX + "," + chunkZ + ")";
    }
}