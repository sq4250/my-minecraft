package com.tsian;

import java.util.ArrayList;
import java.util.List;

/**
 * 世界类 - 简单的3x3x3方块世界
 */
public class World {
    
    private List<Block> blocks;
    
    public World() {
        this.blocks = new ArrayList<>();
        generateSimpleWorld();
        System.out.println("Initialized simple 3x3x3 world with " + blocks.size() + " blocks");
    }
    
    /**
     * 生成简单的3x3x3世界
     */
    private void generateSimpleWorld() {
        blocks.clear();
        
        // 生成3x3x3的方块世界 (27个方块)
        for (int x = -1; x <= 1; x++) {
            for (int y = 0; y <= 2; y++) {
                for (int z = -1; z <= 1; z++) {
                    Block.BlockType type;
                    
                    // 简单的方块类型分配
                    if (y == 0) {
                        type = Block.BlockType.STONE;
                    } else if (y == 1) {
                        type = Block.BlockType.DIRT;
                    } else {
                        type = Block.BlockType.GRASS;
                    }
                    
                    blocks.add(new Block(x, y, z, type));
                }
            }
        }
    }
    
    /**
     * 获取所有可见面
     */
    public List<VisibleFace> getVisibleFaces() {
        List<VisibleFace> visibleFaces = new ArrayList<>();
        
        for (Block block : blocks) {
            if (!block.isSolid()) continue;
            
            // 检查每个面是否可见
            for (int face = 0; face < 6; face++) {
                if (isFaceVisible(block, face)) {
                    visibleFaces.add(new VisibleFace(block, face));
                }
            }
        }
        
        return visibleFaces;
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
        
        // 如果相邻位置没有实心方块，则该面可见
        return !hasSolidBlockAt(adjX, adjY, adjZ);
    }
    
    /**
     * 检查指定位置是否有实心方块
     */
    private boolean hasSolidBlockAt(int x, int y, int z) {
        for (Block block : blocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return block.isSolid();
            }
        }
        return false;
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