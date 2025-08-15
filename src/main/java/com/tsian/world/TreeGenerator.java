package com.tsian.world;

import java.util.Random;

/**
 * 树木生成器 - 负责生成各种类型的树木结构
 */
public class TreeGenerator {
    
    private static final Random random = new Random();
    
    /**
     * 生成一棵标准橡树
     * @param chunk 目标区块
     * @param localX 区块内的x坐标 (0-15)
     * @param localZ 区块内的z坐标 (0-15)
     * @param groundY 地面高度
     */
    public static void generateOakTree(Chunk chunk, int localX, int localZ, int groundY) {
        // 计算世界坐标
        int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + localX;
        int worldZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE + localZ;
        
        // 随机树高 (4-6块)
        int treeHeight = 4 + random.nextInt(3);
        
        // 生成树干
        for (int y = 1; y <= treeHeight; y++) {
            chunk.setBlock(worldX, groundY + y, worldZ, Block.BlockType.WOOD_LOG);
        }
        
        // 生成树叶 - 分层生成
        int leavesStartY = groundY + treeHeight - 1; // 树叶从树干顶部往下2层开始
        
        // 顶层树叶 (十字形)
        generateLeavesLayer(chunk, worldX, worldZ, leavesStartY + 2, 1);
        
        // 中层树叶 (3x3)
        generateLeavesLayer(chunk, worldX, worldZ, leavesStartY + 1, 2);
        
        // 下层树叶 (3x3)
        generateLeavesLayer(chunk, worldX, worldZ, leavesStartY, 2);
        
        // 可选：最下层稀疏树叶 (3x3但有缺失)
        if (random.nextBoolean()) {
            generateSparseLeavesLayer(chunk, worldX, worldZ, leavesStartY - 1, 2);
        }
    }
    
    /**
     * 生成树叶层
     * @param chunk 目标区块
     * @param centerX 中心世界x坐标
     * @param centerZ 中心世界z坐标
     * @param y 生成高度
     * @param radius 半径 (1=十字形, 2=3x3)
     */
    private static void generateLeavesLayer(Chunk chunk, int centerX, int centerZ, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                
                // 十字形模式 (radius=1)
                if (radius == 1) {
                    if (dx == 0 || dz == 0) {
                        tryPlaceLeaves(chunk, x, y, z);
                    }
                } 
                // 3x3模式 (radius=2)
                else if (radius == 2) {
                    // 跳过四个角落，形成更自然的圆形
                    if (Math.abs(dx) == 2 && Math.abs(dz) == 2) {
                        if (random.nextFloat() < 0.3f) { // 30%概率在角落放置树叶
                            tryPlaceLeaves(chunk, x, y, z);
                        }
                    } else {
                        tryPlaceLeaves(chunk, x, y, z);
                    }
                }
            }
        }
    }
    
    /**
     * 生成稀疏树叶层（有随机缺失）
     */
    private static void generateSparseLeavesLayer(Chunk chunk, int centerX, int centerZ, int y, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int x = centerX + dx;
                int z = centerZ + dz;
                
                // 60%概率生成树叶
                if (random.nextFloat() < 0.6f) {
                    // 跳过四个角落
                    if (!(Math.abs(dx) == 2 && Math.abs(dz) == 2)) {
                        tryPlaceLeaves(chunk, x, y, z);
                    }
                }
            }
        }
    }
    
    /**
     * 尝试放置树叶方块
     */
    private static void tryPlaceLeaves(Chunk chunk, int worldX, int worldY, int worldZ) {
        // 检查是否在区块范围内
        if (chunk.containsBlock(worldX, worldZ)) {
            // 检查位置是否为空气
            Block existingBlock = chunk.getBlock(worldX, worldY, worldZ);
            if (existingBlock == null || existingBlock.getType() == Block.BlockType.AIR) {
                chunk.setBlock(worldX, worldY, worldZ, Block.BlockType.LEAVES);
            }
        }
    }
    
    /**
     * 检查位置是否适合生成树木
     * @param chunk 目标区块
     * @param localX 区块内x坐标
     * @param localZ 区块内z坐标
     * @param groundY 地面高度
     * @return 是否适合生成树
     */
    public static boolean canPlaceTree(Chunk chunk, int localX, int localZ, int groundY) {
        int worldX = chunk.getChunkX() * Chunk.CHUNK_SIZE + localX;
        int worldZ = chunk.getChunkZ() * Chunk.CHUNK_SIZE + localZ;
        
        // 检查地面是否是草方块
        Block groundBlock = chunk.getBlock(worldX, groundY, worldZ);
        if (groundBlock == null || groundBlock.getType() != Block.BlockType.GRASS) {
            return false;
        }
        
        // 检查树干位置是否有足够空间 (高度6格)
        for (int y = 1; y <= 6; y++) {
            Block checkBlock = chunk.getBlock(worldX, groundY + y, worldZ);
            if (checkBlock != null && checkBlock.getType() != Block.BlockType.AIR) {
                return false;
            }
        }
        
        // 检查周围2格范围内是否有足够空间 (避免树重叠)
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue; // 跳过中心位置
                
                int checkX = worldX + dx;
                int checkZ = worldZ + dz;
                
                // 检查是否有其他树干
                Block checkBlock = chunk.getBlock(checkX, groundY + 1, checkZ);
                if (checkBlock != null && checkBlock.getType() == Block.BlockType.WOOD_LOG) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 在区块中随机生成树木
     * @param chunk 目标区块
     * @param density 树木密度 (0.0-1.0)
     */
    public static void generateRandomTrees(Chunk chunk, float density) {
        int treesToGenerate = (int) (Chunk.CHUNK_SIZE * Chunk.CHUNK_SIZE * density / 100.0f);
        treesToGenerate = Math.max(1, treesToGenerate); // 至少生成1棵树
        
        int attempts = 0;
        int generated = 0;
        int maxAttempts = treesToGenerate * 10; // 限制尝试次数避免无限循环
        
        while (generated < treesToGenerate && attempts < maxAttempts) {
            attempts++;
            
            // 随机选择区块内位置
            int localX = random.nextInt(Chunk.CHUNK_SIZE);
            int localZ = random.nextInt(Chunk.CHUNK_SIZE);
            
            // 地面高度 (平坦地形是y=3)
            int groundY = 3;
            
            if (canPlaceTree(chunk, localX, localZ, groundY)) {
                generateOakTree(chunk, localX, localZ, groundY);
                generated++;
            }
        }
        
        if (generated > 0) {
            System.out.println("Generated " + generated + " trees in chunk (" + 
                             chunk.getChunkX() + ", " + chunk.getChunkZ() + ")");
        }
    }
}