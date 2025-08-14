package com.tsian;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块类 - 表示16x16x256的方块区域
 */
public class Chunk {
    
    public static final int CHUNK_SIZE = 16;
    public static final int CHUNK_HEIGHT = 256;
    public static final int FLAT_WORLD_HEIGHT = 4;
    
    private final int chunkX, chunkZ;
    private final Map<String, Block> blocks;
    private boolean isGenerated = false;
    private boolean needsRebuild = true;
    
    public Chunk(int chunkX, int chunkZ) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.blocks = new HashMap<>();
    }
    
    /**
     * 生成平坦世界区块
     */
    public void generate() {
        if (isGenerated) return;
        
        for (int x = 0; x < CHUNK_SIZE; x++) {
            for (int z = 0; z < CHUNK_SIZE; z++) {
                // 计算世界坐标
                int worldX = chunkX * CHUNK_SIZE + x;
                int worldZ = chunkZ * CHUNK_SIZE + z;
                
                // 生成4层高的平坦世界
                for (int y = 0; y < FLAT_WORLD_HEIGHT; y++) {
                    Block.BlockType type;
                    
                    if (y == 0) {
                        type = Block.BlockType.STONE; // 基岩层
                    } else if (y < FLAT_WORLD_HEIGHT - 1) {
                        type = Block.BlockType.DIRT; // 泥土层
                    } else {
                        type = Block.BlockType.GRASS; // 草地层
                    }
                    
                    Block block = new Block(worldX, y, worldZ, type);
                    blocks.put(getBlockKey(x, y, z), block);
                }
            }
        }
        
        isGenerated = true;
        needsRebuild = true;
        System.out.println("Generated chunk at (" + chunkX + ", " + chunkZ + ") with " + blocks.size() + " blocks");
    }
    
    /**
     * 获取区块内指定位置的方块
     */
    public Block getBlock(int localX, int localY, int localZ) {
        if (localX < 0 || localX >= CHUNK_SIZE || 
            localY < 0 || localY >= CHUNK_HEIGHT || 
            localZ < 0 || localZ >= CHUNK_SIZE) {
            return null;
        }
        return blocks.get(getBlockKey(localX, localY, localZ));
    }
    
    /**
     * 设置区块内指定位置的方块
     */
    public void setBlock(int localX, int localY, int localZ, Block.BlockType type) {
        if (localX < 0 || localX >= CHUNK_SIZE || 
            localY < 0 || localY >= CHUNK_HEIGHT || 
            localZ < 0 || localZ >= CHUNK_SIZE) {
            return;
        }
        
        int worldX = chunkX * CHUNK_SIZE + localX;
        int worldZ = chunkZ * CHUNK_SIZE + localZ;
        Block block = new Block(worldX, localY, worldZ, type);
        blocks.put(getBlockKey(localX, localY, localZ), block);
        needsRebuild = true;
    }
    
    /**
     * 获取所有方块
     */
    public Map<String, Block> getBlocks() {
        return blocks;
    }
    
    /**
     * 检查指定位置是否有实心方块
     */
    public boolean isSolidBlock(int localX, int localY, int localZ) {
        Block block = getBlock(localX, localY, localZ);
        return block != null && block.isSolid();
    }
    
    /**
     * 获取方块键值
     */
    private String getBlockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
    
    /**
     * 将世界坐标转换为区块内坐标
     */
    public static int worldToLocal(int worldCoord) {
        return ((worldCoord % CHUNK_SIZE) + CHUNK_SIZE) % CHUNK_SIZE;
    }
    
    /**
     * 将世界坐标转换为区块坐标
     */
    public static int worldToChunk(int worldCoord) {
        return worldCoord >> 4; // 除以16
    }
    
    // Getter方法
    public int getChunkX() { return chunkX; }
    public int getChunkZ() { return chunkZ; }
    public boolean isGenerated() { return isGenerated; }
    public boolean needsRebuild() { return needsRebuild; }
    public void setNeedsRebuild(boolean needsRebuild) { this.needsRebuild = needsRebuild; }
    
    /**
     * 获取区块的距离摄像头的距离
     */
    public double getDistanceFromCamera(float cameraX, float cameraZ) {
        // 计算区块中心点
        float chunkCenterX = (chunkX * CHUNK_SIZE) + (CHUNK_SIZE / 2.0f);
        float chunkCenterZ = (chunkZ * CHUNK_SIZE) + (CHUNK_SIZE / 2.0f);
        
        // 计算距离
        float dx = cameraX - chunkCenterX;
        float dz = cameraZ - chunkCenterZ;
        return Math.sqrt(dx * dx + dz * dz);
    }
}