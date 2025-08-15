package com.tsian.world;

import java.util.HashMap;
import java.util.Map;

/**
 * 区块管理器 - 管理固定4x4区块空岛
 *
 * 负责：
 * - 管理所有已加载的区块
 * - 提供世界级别的方块操作接口（坐标验证、区块查找等）
 * - 协调区块的加载和初始化
 * - 作为World类和Chunk类之间的中介
 */
public class ChunkManager {
    
    // 固定的空岛范围：4x4区块 (64x64方块)
    private static final int ISLAND_SIZE = 4; // 4x4区块
    private static final int ISLAND_MIN_CHUNK = 0; // 最小区块坐标
    private static final int ISLAND_MAX_CHUNK = 3; // 最大区块坐标
    
    private final Map<String, Chunk> loadedChunks; // 已加载的区块
    private boolean isInitialized = false; // 是否已初始化空岛
    
    public ChunkManager() {
        this.loadedChunks = new HashMap<>();
    }
    
    /**
     * 更新区块加载状态（对于固定空岛，这个方法不需要做任何事）
     */
    public void updateChunks(float playerX, float playerZ) {
        // 固定空岛不需要动态加载，所以这个方法为空
    }
    
    /**
     * 检查区块坐标是否在空岛范围内
     */
    private boolean isValidChunk(int chunkX, int chunkZ) {
        return chunkX >= ISLAND_MIN_CHUNK && chunkX <= ISLAND_MAX_CHUNK && 
               chunkZ >= ISLAND_MIN_CHUNK && chunkZ <= ISLAND_MAX_CHUNK;
    }
    
    /**
     * 加载或创建区块（仅限空岛范围内）
     */
    public Chunk loadChunk(int chunkX, int chunkZ) {
        // 检查是否在空岛范围内
        if (!isValidChunk(chunkX, chunkZ)) {
            return null; // 空岛范围外不生成区块
        }
        
        String chunkKey = getChunkKey(chunkX, chunkZ);
        
        if (loadedChunks.containsKey(chunkKey)) {
            return loadedChunks.get(chunkKey);
        }
        
        // 创建新区块
        Chunk chunk = new Chunk(chunkX, chunkZ);
        
        // 生成空岛地形
        generateIslandTerrain(chunk);
        
        loadedChunks.put(chunkKey, chunk);
        System.out.println("Loaded island chunk: " + chunk + " (Total loaded: " + loadedChunks.size() + ")");
        
        return chunk;
    }
    
    /**
     * 生成空岛地形
     */
    private void generateIslandTerrain(Chunk chunk) {
        int chunkX = chunk.getChunkX();
        int chunkZ = chunk.getChunkZ();
        
        // 计算空岛中心坐标
        float centerX = (ISLAND_MAX_CHUNK + ISLAND_MIN_CHUNK) * 0.5f * Chunk.CHUNK_SIZE + Chunk.CHUNK_SIZE / 2.0f;
        float centerZ = (ISLAND_MAX_CHUNK + ISLAND_MIN_CHUNK) * 0.5f * Chunk.CHUNK_SIZE + Chunk.CHUNK_SIZE / 2.0f;
        
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                // 计算到空岛中心的距离
                float distanceToCenter = (float) Math.sqrt(
                    Math.pow(worldX - centerX, 2) + Math.pow(worldZ - centerZ, 2)
                );
                
                // 检查是否在水池区域内（中心附近5格半径）
                if (distanceToCenter <= 5.0f) {
                    // 创建水池：在地面高度放置水方块，替换草方块
                    chunk.setBlock(worldX, 0, worldZ, Block.BlockType.STONE);    // 底层：圆石
                    chunk.setBlock(worldX, 1, worldZ, Block.BlockType.DIRT);     // 第二层：泥块
                    chunk.setBlock(worldX, 2, worldZ, Block.BlockType.DIRT);     // 第三层：泥块
                    chunk.setBlock(worldX, 3, worldZ, Block.BlockType.WATER);    // 顶层：水方块（替换草方块）
                } else if (distanceToCenter <= 24) {
                    // 核心区域：完整地形
                    generateFullTerrain(chunk, worldX, worldZ);
                } else if (distanceToCenter <= 28) {
                    // 边缘区域：随机生成，形成自然边缘
                    float edgeChance = (28 - distanceToCenter) / 4.0f; // 0到1的渐变
                    if (Math.random() < edgeChance) {
                        generateFullTerrain(chunk, worldX, worldZ);
                    }
                }
                // 超出28格的区域保持空气
            }
        }
        
        // 在空岛上生成少量树木
        TreeGenerator.generateRandomTrees(chunk, 1.0f); // 降低树木密度
        
        System.out.println("Generated island terrain for chunk (" + chunkX + ", " + chunkZ + ")");
    }
    
    /**
     * 生成完整地形层
     */
    private void generateFullTerrain(Chunk chunk, int worldX, int worldZ) {
        chunk.setBlock(worldX, 0, worldZ, Block.BlockType.STONE);    // 底层：圆石
        chunk.setBlock(worldX, 1, worldZ, Block.BlockType.DIRT);     // 第二层：泥块
        chunk.setBlock(worldX, 2, worldZ, Block.BlockType.DIRT);     // 第三层：泥块  
        chunk.setBlock(worldX, 3, worldZ, Block.BlockType.GRASS);    // 顶层：草方块
    }
    
    /**
     * 获取指定位置的区块
     */
    public Chunk getChunk(int chunkX, int chunkZ) {
        if (!isValidChunk(chunkX, chunkZ)) {
            return null;
        }
        String chunkKey = getChunkKey(chunkX, chunkZ);
        return loadedChunks.get(chunkKey);
    }
    
    /**
     * 根据世界坐标获取区块
     */
    public Chunk getChunkByWorldPos(int worldX, int worldZ) {
        int chunkX = worldToChunkCoord(worldX);
        int chunkZ = worldToChunkCoord(worldZ);
        return getChunk(chunkX, chunkZ);
    }
    
    /**
     * 获取指定世界坐标的方块
     */
    public Block getBlockAt(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunkByWorldPos(worldX, worldZ);
        if (chunk != null) {
            return chunk.getBlock(worldX, worldY, worldZ);
        }
        return null;
    }
    
    /**
     * 在指定世界坐标设置方块
     */
    public boolean setBlockAt(int worldX, int worldY, int worldZ, Block.BlockType type) {
        Chunk chunk = getChunkByWorldPos(worldX, worldZ);
        if (chunk != null) {
            chunk.setBlock(worldX, worldY, worldZ, type);
            return true;
        }
        return false;
    }
    
    /**
     * 添加方块到世界
     */
    public boolean addBlock(int worldX, int worldY, int worldZ, Block.BlockType type) {
        Chunk chunk = getChunkByWorldPos(worldX, worldZ);
        if (chunk != null) {
            return chunk.addBlock(worldX, worldY, worldZ, type);
        }
        return false; // 空岛范围外不允许添加方块
    }
    
    /**
     * 移除指定世界坐标的方块
     */
    public boolean removeBlock(int worldX, int worldY, int worldZ) {
        Chunk chunk = getChunkByWorldPos(worldX, worldZ);
        if (chunk != null) {
            return chunk.removeBlock(worldX, worldY, worldZ);
        }
        return false;
    }
    
    /**
     * 世界坐标转换为区块坐标
     */
    public static int worldToChunkCoord(int worldCoord) {
        return worldCoord >= 0 ? worldCoord / Chunk.CHUNK_SIZE : (worldCoord + 1) / Chunk.CHUNK_SIZE - 1;
    }
    
    /**
     * 获取区块键值
     */
    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }
    
    /**
     * 获取所有已加载的区块
     */
    public Map<String, Chunk> getLoadedChunks() {
        return loadedChunks;
    }
    
    /**
     * 初始化空岛（一次性加载所有4x4区块）
     */
    public void loadInitialChunks(float playerX, float playerZ) {
        if (isInitialized) {
            return; // 已经初始化过了
        }
        
        // 加载整个4x4空岛
        for (int x = ISLAND_MIN_CHUNK; x <= ISLAND_MAX_CHUNK; x++) {
            for (int z = ISLAND_MIN_CHUNK; z <= ISLAND_MAX_CHUNK; z++) {
                loadChunk(x, z);
            }
        }
        
        isInitialized = true;
        System.out.println("Loaded complete 4x4 island (" + ISLAND_SIZE + "x" + ISLAND_SIZE + " chunks)");
    }
    
    /**
     * 获取统计信息
     */
    public String getStats() {
        return "Island chunks: " + loadedChunks.size() + "/" + (ISLAND_SIZE * ISLAND_SIZE);
    }
}