package com.tsian;

/**
 * LOD (Level of Detail) 系统
 * 根据距离摄像机的远近决定渲染的细节级别
 */
public class LODSystem {
    
    // LOD级别定义
    public enum LODLevel {
        HIGH(0, 0.0f, 20.0f),      // 高细节：距离 0-20
        MEDIUM(1, 20.0f, 50.0f),   // 中细节：距离 20-50
        LOW(2, 50.0f, 100.0f),     // 低细节：距离 50-100
        CULLED(3, 100.0f, Float.MAX_VALUE); // 剔除：距离 100+
        
        public final int level;
        public final float minDistance;
        public final float maxDistance;
        
        LODLevel(int level, float minDistance, float maxDistance) {
            this.level = level;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }
    }
    
    // 摄像机位置
    private float cameraX, cameraY, cameraZ;
    
    /**
     * 更新摄像机位置
     */
    public void updateCameraPosition(float x, float y, float z) {
        this.cameraX = x;
        this.cameraY = y;
        this.cameraZ = z;
    }
    
    /**
     * 计算方块到摄像机的距离
     */
    public float calculateDistance(Block block) {
        float dx = block.getX() - cameraX;
        float dy = block.getY() - cameraY;
        float dz = block.getZ() - cameraZ;
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
    
    /**
     * 获取方块的LOD级别
     */
    public LODLevel getLODLevel(Block block) {
        float distance = calculateDistance(block);
        return getLODLevelByDistance(distance);
    }
    
    /**
     * 根据距离获取LOD级别
     */
    public LODLevel getLODLevelByDistance(float distance) {
        for (LODLevel level : LODLevel.values()) {
            if (distance >= level.minDistance && distance < level.maxDistance) {
                return level;
            }
        }
        return LODLevel.CULLED;
    }
    
    /**
     * 判断方块是否应该被渲染
     */
    public boolean shouldRender(Block block) {
        return getLODLevel(block) != LODLevel.CULLED;
    }
    
    /**
     * 判断方块的某个面是否应该被渲染
     * 不同LOD级别可能会跳过某些面来提升性能
     */
    public boolean shouldRenderFace(Block block, int face, LODLevel lodLevel) {
        switch (lodLevel) {
            case HIGH:
                // 高细节：渲染所有面
                return true;
                
            case MEDIUM:
                // 中细节：跳过下面的面（通常看不到）
                return face != 5; // 5 = 下面
                
            case LOW:
                // 低细节：只渲染可见的主要面
                float distance = calculateDistance(block);
                
                // 根据视角和距离决定哪些面重要
                float dx = block.getX() - cameraX;
                float dz = block.getZ() - cameraZ;
                
                switch (face) {
                    case 0: // 前面 (+Z)
                        return dz > 0;
                    case 1: // 后面 (-Z)
                        return dz < 0;
                    case 2: // 左面 (-X)
                        return dx < 0;
                    case 3: // 右面 (+X)
                        return dx > 0;
                    case 4: // 上面 (+Y)
                        return true; // 上面通常是重要的
                    case 5: // 下面 (-Y)
                        return false; // 低细节时不渲染下面
                    default:
                        return true;
                }
                
            case CULLED:
            default:
                return false;
        }
    }
    
    /**
     * 获取LOD渲染配置
     */
    public LODRenderConfig getLODConfig(Block block) {
        LODLevel level = getLODLevel(block);
        float distance = calculateDistance(block);
        
        return new LODRenderConfig(level, distance, shouldRender(block));
    }
    
    /**
     * LOD渲染配置
     */
    public static class LODRenderConfig {
        public final LODLevel lodLevel;
        public final float distance;
        public final boolean shouldRender;
        
        // 渲染质量参数
        public final float textureQuality;  // 纹理质量 (0.0-1.0)
        public final boolean enableShadows; // 是否启用阴影
        public final int detailLevel;       // 细节级别
        
        public LODRenderConfig(LODLevel lodLevel, float distance, boolean shouldRender) {
            this.lodLevel = lodLevel;
            this.distance = distance;
            this.shouldRender = shouldRender;
            
            // 根据LOD级别设置渲染参数
            switch (lodLevel) {
                case HIGH:
                    this.textureQuality = 1.0f;
                    this.enableShadows = true;
                    this.detailLevel = 3;
                    break;
                case MEDIUM:
                    this.textureQuality = 0.75f;
                    this.enableShadows = true;
                    this.detailLevel = 2;
                    break;
                case LOW:
                    this.textureQuality = 0.5f;
                    this.enableShadows = false;
                    this.detailLevel = 1;
                    break;
                case CULLED:
                default:
                    this.textureQuality = 0.0f;
                    this.enableShadows = false;
                    this.detailLevel = 0;
                    break;
            }
        }
        
        @Override
        public String toString() {
            return String.format("LOD[%s, dist=%.1f, render=%s, quality=%.2f]", 
                               lodLevel, distance, shouldRender, textureQuality);
        }
    }
    
    /**
     * 获取LOD系统统计信息
     */
    public LODStats calculateStats(Iterable<Block> blocks) {
        int[] levelCounts = new int[LODLevel.values().length];
        int totalBlocks = 0;
        int renderedBlocks = 0;
        
        for (Block block : blocks) {
            totalBlocks++;
            LODLevel level = getLODLevel(block);
            levelCounts[level.level]++;
            
            if (level != LODLevel.CULLED) {
                renderedBlocks++;
            }
        }
        
        return new LODStats(levelCounts, totalBlocks, renderedBlocks);
    }
    
    /**
     * LOD统计信息
     */
    public static class LODStats {
        public final int[] levelCounts;
        public final int totalBlocks;
        public final int renderedBlocks;
        public final int culledBlocks;
        public final float cullRatio;
        
        public LODStats(int[] levelCounts, int totalBlocks, int renderedBlocks) {
            this.levelCounts = levelCounts.clone();
            this.totalBlocks = totalBlocks;
            this.renderedBlocks = renderedBlocks;
            this.culledBlocks = totalBlocks - renderedBlocks;
            this.cullRatio = totalBlocks > 0 ? (float) culledBlocks / totalBlocks : 0.0f;
        }
        
        @Override
        public String toString() {
            return String.format("LOD Stats - Total: %d, Rendered: %d, Culled: %d (%.1f%%), " +
                               "High: %d, Medium: %d, Low: %d", 
                               totalBlocks, renderedBlocks, culledBlocks, cullRatio * 100,
                               levelCounts[0], levelCounts[1], levelCounts[2]);
        }
    }
}