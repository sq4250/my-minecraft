package com.tsian.world;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块类 - 代表一个16x16的方块区域
 *
 * 负责：
 * - 存储和管理区块内的方块数据
 * - 提供区块级别的方块操作方法
 * - 验证坐标是否在区块范围内
 */
public class Chunk {
    
    public static final int CHUNK_SIZE = 16; // 区块大小16x16
    
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