package com.tsian.world;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界类 - 基于区块的无限世界
 */
public class World {
    
    private ChunkManager chunkManager; // 区块管理器
    private List<VisibleFace> cachedVisibleFaces; // 缓存的可见面
    private boolean needsVisibleFaceUpdate = true; // 是否需要更新可见面
    
    // 玩家位置跟踪
    private float lastPlayerX = Float.MAX_VALUE;
    private float lastPlayerZ = Float.MAX_VALUE;
    
    public World() {
        this.chunkManager = new ChunkManager();
        this.cachedVisibleFaces = new ArrayList<>();
        System.out.println("Initialized chunk-based infinite world");
    }
    
    /**
     * 初始化世界（加载玩家周围的初始区块）
     */
    public void initializeWorld(float playerX, float playerZ) {
        chunkManager.loadInitialChunks(playerX, playerZ);
        lastPlayerX = playerX;
        lastPlayerZ = playerZ;
        calculateVisibleFaces();
        System.out.println("World initialized around player position (" + playerX + ", " + playerZ + ")");
        System.out.println(chunkManager.getStats());
    }
    
    /**
     * 更新世界状态（基于玩家位置）
     */
    public void updateWorld(float playerX, float playerZ) {
        // 检查玩家是否移动了足够远的距离
        float deltaX = Math.abs(playerX - lastPlayerX);
        float deltaZ = Math.abs(playerZ - lastPlayerZ);
        
        if (deltaX > 8.0f || deltaZ > 8.0f) { // 移动超过半个区块
            chunkManager.updateChunks(playerX, playerZ);
            lastPlayerX = playerX;
            lastPlayerZ = playerZ;
            needsVisibleFaceUpdate = true;
        }
        
        // 检查是否有区块需要重建
        for (Chunk chunk : chunkManager.getLoadedChunks().values()) {
            if (chunk.needsRebuild()) {
                needsVisibleFaceUpdate = true;
                break;
            }
        }
        
        // 更新可见面（如果需要）
        if (needsVisibleFaceUpdate) {
            calculateVisibleFaces();
            needsVisibleFaceUpdate = false;
        }
    }
    
    /**
     * 预计算所有可见面（基于区块）
     */
    private void calculateVisibleFaces() {
        cachedVisibleFaces.clear();
        
        // 遍历所有已加载区块中的方块
        for (Chunk chunk : chunkManager.getLoadedChunks().values()) {
            for (Block block : chunk.getBlocks().values()) {
                if (block.getType() == Block.BlockType.AIR) continue;
                
                // 检查每个面是否可见
                for (int face = 0; face < 6; face++) {
                    if (isFaceVisible(block, face)) {
                        cachedVisibleFaces.add(new VisibleFace(block, face));
                    }
                }
            }
            
            // 标记区块已重建
            chunk.markRebuilt();
        }
        
        System.out.println("Calculated " + cachedVisibleFaces.size() + " visible faces from " + 
                          chunkManager.getLoadedChunks().size() + " chunks");
    }
    
    /**
     * 获取所有可见面（使用缓存）
     */
    public List<VisibleFace> getVisibleFaces() {
        return cachedVisibleFaces;
    }
    
    /**
     * 检查方块的某个面是否可见
     */
    private boolean isFaceVisible(Block block, int face) {
        int x = block.getX();
        int y = block.getY();
        int z = block.getZ();
        
        // 计算相邻方块的位置
        int adjX = x, adjY = y, adjZ = z;
        
        switch (face) {
            case 0: adjZ++; break; // 前面
            case 1: adjZ--; break; // 后面
            case 2: adjX--; break; // 左面
            case 3: adjX++; break; // 右面
            case 4: adjY++; break; // 上面
            case 5: adjY--; break; // 下面
        }
        
        // 获取相邻方块
        Block adjacentBlock = getBlockAt(adjX, adjY, adjZ);
        
        // 如果没有相邻方块，面可见
        if (adjacentBlock == null || adjacentBlock.getType() == Block.BlockType.AIR) {
            return true;
        }
        
        // 如果是相同类型的方块，面不可见（剔除）- 包括同种透明方块
        if (block.getType() == adjacentBlock.getType()) {
            return false;
        }
        
        // 透明方块的特殊处理
        boolean currentIsTransparent = (block.getType() == Block.BlockType.WATER || block.getType() == Block.BlockType.LEAVES);
        boolean adjacentIsTransparent = (adjacentBlock.getType() == Block.BlockType.WATER || adjacentBlock.getType() == Block.BlockType.LEAVES);
        
        // 如果当前方块是透明的，且相邻方块是不透明的，当前面不可见（剔除）
        if (currentIsTransparent && !adjacentIsTransparent) {
            return false;
        }
        
        // 如果当前方块是不透明的，且相邻方块是透明的，当前面可见
        if (!currentIsTransparent && adjacentIsTransparent) {
            return true;
        }
        
        // 如果都是透明但不同类型，面可见
        if (currentIsTransparent && adjacentIsTransparent) {
            return true;
        }
        
        // 默认：不透明方块之间的相邻面不可见
        return false;
    }
    
    /**
     * 获取指定位置的方块
     */
    public Block getBlockAt(int x, int y, int z) {
        return chunkManager.getBlockAt(x, y, z);
    }
    
    /**
     * 获取所有方块（从所有已加载区块）
     */
    public List<Block> getBlocks() {
        List<Block> allBlocks = new ArrayList<>();
        for (Chunk chunk : chunkManager.getLoadedChunks().values()) {
            allBlocks.addAll(chunk.getBlocks().values());
        }
        return allBlocks;
    }
    
    /**
     * 重新计算可见面（当方块被破坏后调用）
     */
    public void recalculateVisibleFaces() {
        calculateVisibleFaces();
    }
    
    /**
     * 添加新方块到世界中
     */
    public boolean addBlock(int x, int y, int z, Block.BlockType blockType) {
        boolean success = chunkManager.addBlock(x, y, z, blockType);
        if (success) {
            needsVisibleFaceUpdate = true;
        }
        return success;
    }
    
    /**
     * 移除指定位置的方块（设置为空气）
     */
    public boolean removeBlockAt(int x, int y, int z) {
        boolean success = chunkManager.removeBlock(x, y, z);
        if (success) {
            needsVisibleFaceUpdate = true;
        }
        return success;
    }
    
    /**
     * 获取区块管理器统计信息
     */
    public String getChunkStats() {
        return chunkManager.getStats();
    }
    
    /**
     * 获取区块管理器（用于调试）
     */
    public ChunkManager getChunkManager() {
        return chunkManager;
    }
    
    /**
     * 可见面数据结构
     */
    public static class VisibleFace {
        public final Block block;
        public final int face;
        
        public VisibleFace(Block block, int face) {
            this.block = block;
            this.face = face;
        }
    }
}