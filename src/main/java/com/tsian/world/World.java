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
        
        // 生成一棵树
        generateTree(2, 1, 2);
        
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
     * 重新计算可见面（当方块被破坏后调用）
     */
    public void recalculateVisibleFaces() {
        calculateVisibleFaces();
        System.out.println("Recalculated visible faces: " + cachedVisibleFaces.size() + " faces");
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
    
    /**
     * 在指定位置生成一棵美观的树
     */
    private void generateTree(int baseX, int baseY, int baseZ) {
        // 生成树干 (6格高，更高一些)
        for (int y = 0; y < 6; y++) {
            blocks.add(new Block(baseX, baseY + y, baseZ, Block.BlockType.WOOD_LOG));
        }
        
        // 生成更自然的树冠形状
        generateTreeCanopy(baseX, baseY, baseZ);
    }
    
    /**
     * 生成宽大的自然树冠（向上平移，只留1格树干侵入）
     */
    private void generateTreeCanopy(int centerX, int centerY, int centerZ) {
        int canopyStartY = centerY + 5; // 从第6层开始，树干最顶部y+5侵入树叶
        
        // 底层 (y+5): 超大圆形，半径4，跳过树干（树干侵入这一层）
        for (int x = centerX - 4; x <= centerX + 4; x++) {
            for (int z = centerZ - 4; z <= centerZ + 4; z++) {
                // 跳过树干位置
                if (x == centerX && z == centerZ) {
                    continue;
                }
                float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (dist <= 3.8f) {
                    // 边缘稀疏一些，形成更自然的形状
                    if (dist < 3.0f || (dist < 3.5f && (Math.abs(x - centerX) <= 2 || Math.abs(z - centerZ) <= 2))) {
                        blocks.add(new Block(x, canopyStartY, z, Block.BlockType.LEAVES));
                    }
                }
            }
        }
        
        // 第二层 (y+6): 大圆形，半径3.5
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (dist <= 3.3f) {
                    blocks.add(new Block(x, canopyStartY + 1, z, Block.BlockType.LEAVES));
                }
            }
        }
        
        // 第三层 (y+7): 圆形，半径3
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int z = centerZ - 3; z <= centerZ + 3; z++) {
                float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (dist <= 2.8f) {
                    blocks.add(new Block(x, canopyStartY + 2, z, Block.BlockType.LEAVES));
                }
            }
        }
        
        // 第四层 (y+8): 圆形，半径2.5
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (dist <= 2.3f) {
                    blocks.add(new Block(x, canopyStartY + 3, z, Block.BlockType.LEAVES));
                }
            }
        }
        
        // 第五层 (y+9): 圆形，半径2
        for (int x = centerX - 2; x <= centerX + 2; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                float dist = (float) Math.sqrt((x - centerX) * (x - centerX) + (z - centerZ) * (z - centerZ));
                if (dist <= 1.8f) {
                    blocks.add(new Block(x, canopyStartY + 4, z, Block.BlockType.LEAVES));
                }
            }
        }
        
        // 顶层 (y+10): 十字形 + 中心
        blocks.add(new Block(centerX, canopyStartY + 5, centerZ, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX - 1, canopyStartY + 5, centerZ, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX + 1, canopyStartY + 5, centerZ, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX, canopyStartY + 5, centerZ - 1, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX, canopyStartY + 5, centerZ + 1, Block.BlockType.LEAVES));
        
        // 增加更多不规则的树叶让大树冠更自然
        // 在外围添加一些突出的树叶
        blocks.add(new Block(centerX - 4, canopyStartY + 1, centerZ, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX + 4, canopyStartY + 1, centerZ, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX, canopyStartY + 1, centerZ - 4, Block.BlockType.LEAVES));
        blocks.add(new Block(centerX, canopyStartY + 1, centerZ + 4, Block.BlockType.LEAVES));
    }
}