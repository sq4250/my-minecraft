package com.tsian;

import com.tsian.world.World;
import com.tsian.world.Block;

/**
 * 方块交互管理器 - 处理射线投射和方块交互
 */
public class BlockInteractionManager {
    
    private World world;
    private Block targetBlock;
    private int targetFace;
    private float breakProgress;
    private boolean isBreaking;
    private float breakStartTime;
    private boolean needsMeshRebuild; // 是否需要重新构建渲染缓冲区
    
    private static final float BREAK_TIME = 1.0f; // 1秒破坏时间
    private static final float MAX_REACH_DISTANCE = 5.0f; // 最大交互距离
    
    public BlockInteractionManager(World world) {
        this.world = world;
        this.targetBlock = null;
        this.targetFace = -1;
        this.breakProgress = 0.0f;
        this.isBreaking = false;
        this.breakStartTime = 0.0f;
        this.needsMeshRebuild = false;
    }
    
    /**
     * 射线投射检测与方块相交
     */
    public RaycastResult raycast(float startX, float startY, float startZ, 
                                float dirX, float dirY, float dirZ) {
        
        // 归一化方向向量
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        dirX /= length;
        dirY /= length;
        dirZ /= length;
        
        // DDA算法进行体素遍历
        float currentX = startX;
        float currentY = startY;
        float currentZ = startZ;
        
        float deltaDistX = Math.abs(1.0f / dirX);
        float deltaDistY = Math.abs(1.0f / dirY);
        float deltaDistZ = Math.abs(1.0f / dirZ);
        
        int mapX = (int) Math.floor(currentX);
        int mapY = (int) Math.floor(currentY);
        int mapZ = (int) Math.floor(currentZ);
        
        float sideDistX, sideDistY, sideDistZ;
        int stepX, stepY, stepZ;
        
        if (dirX < 0) {
            stepX = -1;
            sideDistX = (currentX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0f - currentX) * deltaDistX;
        }
        
        if (dirY < 0) {
            stepY = -1;
            sideDistY = (currentY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0f - currentY) * deltaDistY;
        }
        
        if (dirZ < 0) {
            stepZ = -1;
            sideDistZ = (currentZ - mapZ) * deltaDistZ;
        } else {
            stepZ = 1;
            sideDistZ = (mapZ + 1.0f - currentZ) * deltaDistZ;
        }
        
        // 执行DDA
        boolean hit = false;
        int side = 0; // 哪个面被击中：0=X面，1=Y面，2=Z面
        float distance = 0;
        
        while (distance < MAX_REACH_DISTANCE) {
            // 跳到下一个方块边界
            if (sideDistX < sideDistY && sideDistX < sideDistZ) {
                sideDistX += deltaDistX;
                mapX += stepX;
                side = 0;
                distance = sideDistX - deltaDistX;
            } else if (sideDistY < sideDistZ) {
                sideDistY += deltaDistY;
                mapY += stepY;
                side = 1;
                distance = sideDistY - deltaDistY;
            } else {
                sideDistZ += deltaDistZ;
                mapZ += stepZ;
                side = 2;
                distance = sideDistZ - deltaDistZ;
            }
            
            // 检查当前位置是否有方块
            Block block = getBlockAt(mapX, mapY, mapZ);
            if (block != null && block.getType() != Block.BlockType.AIR) {
                hit = true;
                break;
            }
        }
        
        if (hit) {
            // 计算被击中的面
            int face = calculateHitFace(side, stepX, stepY, stepZ);
            Block hitBlock = getBlockAt(mapX, mapY, mapZ);
            
            // 计算击中点
            float hitX = startX + dirX * distance;
            float hitY = startY + dirY * distance;
            float hitZ = startZ + dirZ * distance;
            
            return new RaycastResult(true, hitBlock, face, hitX, hitY, hitZ, distance);
        }
        
        return new RaycastResult(false, null, -1, 0, 0, 0, MAX_REACH_DISTANCE);
    }
    
    /**
     * 计算被击中的面
     */
    private int calculateHitFace(int side, int stepX, int stepY, int stepZ) {
        switch (side) {
            case 0: // X面
                return stepX > 0 ? 2 : 3; // 左面或右面
            case 1: // Y面
                return stepY > 0 ? 5 : 4; // 下面或上面
            case 2: // Z面
                return stepZ > 0 ? 1 : 0; // 后面或前面
            default:
                return 0;
        }
    }
    
    /**
     * 获取指定位置的方块
     */
    private Block getBlockAt(int x, int y, int z) {
        for (Block block : world.getBlocks()) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return block;
            }
        }
        return null;
    }
    
    /**
     * 开始破坏方块
     */
    public void startBreaking(float currentTime) {
        if (targetBlock != null) {
            isBreaking = true;
            breakStartTime = currentTime;
            breakProgress = 0.0f;
        }
    }
    
    /**
     * 停止破坏方块
     */
    public void stopBreaking() {
        isBreaking = false;
        breakProgress = 0.0f;
    }
    
    /**
     * 更新破坏进度
     */
    public void updateBreaking(float currentTime) {
        if (isBreaking && targetBlock != null) {
            float elapsed = currentTime - breakStartTime;
            breakProgress = Math.min(elapsed / BREAK_TIME, 1.0f);
            
            // 如果破坏完成，移除方块
            if (breakProgress >= 1.0f) {
                removeBlock(targetBlock);
                stopBreaking();
                setTargetBlock(null, -1);
            }
        }
    }
    
    /**
     * 移除方块（实际上设置为空气）
     */
    private void removeBlock(Block block) {
        block.setType(Block.BlockType.AIR);
        // 需要重新计算可见面和更新渲染
        world.recalculateVisibleFaces();
        // 标记需要重新构建渲染缓冲区
        needsMeshRebuild = true;
    }
    
    /**
     * 设置目标方块
     */
    public void setTargetBlock(Block block, int face) {
        // 如果目标方块改变，重置破坏进度
        if (this.targetBlock != block) {
            stopBreaking();
        }
        this.targetBlock = block;
        this.targetFace = face;
    }
    
    // Getter方法
    public Block getTargetBlock() { return targetBlock; }
    public int getTargetFace() { return targetFace; }
    public float getBreakProgress() { return breakProgress; }
    public boolean isBreaking() { return isBreaking; }
    public boolean needsMeshRebuild() { return needsMeshRebuild; }
    
    /**
     * 标记mesh重建已完成
     */
    public void markMeshRebuilt() {
        needsMeshRebuild = false;
    }
    
    /**
     * 射线投射结果
     */
    public static class RaycastResult {
        public final boolean hit;
        public final Block block;
        public final int face;
        public final float hitX, hitY, hitZ;
        public final float distance;
        
        public RaycastResult(boolean hit, Block block, int face, 
                           float hitX, float hitY, float hitZ, float distance) {
            this.hit = hit;
            this.block = block;
            this.face = face;
            this.hitX = hitX;
            this.hitY = hitY;
            this.hitZ = hitZ;
            this.distance = distance;
        }
    }
}