package com.tsian.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界类 - 优化的方块世界
 */
public class World {
    
    private List<Block> blocks;
    private Map<String, Block> blockMap; // 快速查找映射
    private List<VisibleFace> cachedVisibleFaces; // 缓存的可见面
    
    public World() {
        this.blocks = new ArrayList<>();
        this.blockMap = new HashMap<>();
        generateSimpleWorld();
        calculateVisibleFaces(); // 预计算可见面
        System.out.println("Initialized optimized world with " + blocks.size() + " blocks, " +
                          cachedVisibleFaces.size() + " visible faces");
    }
    
    /**
     * 生成多层世界 - 泥土层为7x7
     */
    private void generateSimpleWorld() {
        blocks.clear();
        
        // 石头层 (y=0): 3x3
        for (int x = -5; x <= 5; x++) {
            for (int z = -5; z <= 5; z++) {
                blocks.add(new Block(x, 0, z, Block.BlockType.STONE));
            }
        }
        
        // 泥土层 (y=1): 7x7
        for (int x = -3; x <= 3; x++) {
            for (int z = -3; z <= 3; z++) {
                blocks.add(new Block(x, 1, z, Block.BlockType.DIRT));
            }
        }
        
        // 水层 (y=2): 3x3 (只在泥土中央区域)
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                blocks.add(new Block(x, 2, z, Block.BlockType.WATER));
            }
        }
        // 将方块添加到快速查找映射
        for (Block block : blocks) {
            blockMap.put(getBlockKey(block.getX(), block.getY(), block.getZ()), block);
        }
    }
    
    /**
     * 获取方块键值（用于HashMap查找）
     */
    private String getBlockKey(int x, int y, int z) {
        return x + "," + y + "," + z;
    }
    
    /**
     * 预计算所有可见面
     */
    private void calculateVisibleFaces() {
        cachedVisibleFaces = new ArrayList<>();
        
        for (Block block : blocks) {
            if (block.getType() == Block.BlockType.AIR) continue;
            
            // 检查每个面是否可见
            for (int face = 0; face < 6; face++) {
                if (isFaceVisible(block, face)) {
                    cachedVisibleFaces.add(new VisibleFace(block, face));
                }
            }
        }
        
        System.out.println("Pre-calculated " + cachedVisibleFaces.size() + " visible faces");
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
        
        // 如果是相同类型的方块，面不可见（剔除）
        if (block.getType() == adjacentBlock.getType()) {
            return false;
        }
        
        // 水的特殊处理：
        // 当前方块是水，相邻是任何非空气方块 -> 水面可见
        if (block.getType() == Block.BlockType.WATER) {
            return true;
        }
        
        // 当前方块是固体，相邻是水 -> 固体面可见（透过水看到）
        if (block.isSolid() && adjacentBlock.getType() == Block.BlockType.WATER) {
            return true;
        }
        
        // 默认：相邻是固体则面不可见
        return !adjacentBlock.isSolid();
    }
    
    /**
     * 获取指定位置的方块（O(1)查找）
     */
    private Block getBlockAt(int x, int y, int z) {
        return blockMap.get(getBlockKey(x, y, z));
    }
    
    /**
     * 获取所有方块
     */
    public List<Block> getBlocks() {
        return blocks;
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