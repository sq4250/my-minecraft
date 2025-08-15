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
     * 射线投射检测与方块相交 - 改进的实现
     */
    public RaycastResult raycast(float startX, float startY, float startZ,
                                float dirX, float dirY, float dirZ) {
        
        // 归一化方向向量
        float length = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
        if (length == 0) return new RaycastResult(false, null, -1, 0, 0, 0, MAX_REACH_DISTANCE);
        
        dirX /= length;
        dirY /= length;
        dirZ /= length;
        
        // 使用改进的DDA算法
        int mapX = (int) Math.floor(startX);
        int mapY = (int) Math.floor(startY);
        int mapZ = (int) Math.floor(startZ);
        
        // 防止除零错误
        float deltaDistX = (Math.abs(dirX) < 1e-6f) ? 1e30f : Math.abs(1.0f / dirX);
        float deltaDistY = (Math.abs(dirY) < 1e-6f) ? 1e30f : Math.abs(1.0f / dirY);
        float deltaDistZ = (Math.abs(dirZ) < 1e-6f) ? 1e30f : Math.abs(1.0f / dirZ);
        
        float sideDistX, sideDistY, sideDistZ;
        int stepX, stepY, stepZ;
        
        // X轴设置
        if (dirX < 0) {
            stepX = -1;
            sideDistX = (startX - mapX) * deltaDistX;
        } else {
            stepX = 1;
            sideDistX = (mapX + 1.0f - startX) * deltaDistX;
        }
        
        // Y轴设置
        if (dirY < 0) {
            stepY = -1;
            sideDistY = (startY - mapY) * deltaDistY;
        } else {
            stepY = 1;
            sideDistY = (mapY + 1.0f - startY) * deltaDistY;
        }
        
        // Z轴设置
        if (dirZ < 0) {
            stepZ = -1;
            sideDistZ = (startZ - mapZ) * deltaDistZ;
        } else {
            stepZ = 1;
            sideDistZ = (mapZ + 1.0f - startZ) * deltaDistZ;
        }
        
        // 执行DDA遍历
        boolean hit = false;
        int side = 0; // 哪个面被击中：0=X面，1=Y面，2=Z面
        float perpWallDist = 0; // 垂直墙面距离
        
        while (perpWallDist < MAX_REACH_DISTANCE) {
            // 跳到下一个方块边界
            if (sideDistX < sideDistY && sideDistX < sideDistZ) {
                perpWallDist = sideDistX;
                sideDistX += deltaDistX;
                mapX += stepX;
                side = 0;
            } else if (sideDistY < sideDistZ) {
                perpWallDist = sideDistY;
                sideDistY += deltaDistY;
                mapY += stepY;
                side = 1;
            } else {
                perpWallDist = sideDistZ;
                sideDistZ += deltaDistZ;
                mapZ += stepZ;
                side = 2;
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
            
            // 计算真实的击中距离和位置
            float realDistance = perpWallDist;
            float hitX = startX + dirX * realDistance;
            float hitY = startY + dirY * realDistance;
            float hitZ = startZ + dirZ * realDistance;
            
            return new RaycastResult(true, hitBlock, face, hitX, hitY, hitZ, realDistance);
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
        return world.getBlockAt(x, y, z);
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
     * 放置方块
     */
    public boolean placeBlock(float cameraX, float cameraY, float cameraZ,
                             float dirX, float dirY, float dirZ, Block.BlockType blockType) {
        // 执行射线投射找到放置位置
        RaycastResult result = raycast(cameraX, cameraY, cameraZ, dirX, dirY, dirZ);
        
        if (!result.hit) {
            return false; // 没有击中任何方块，无法放置
        }
        
        // 计算放置位置（在击中面的相邻位置）
        int placeX = result.block.getX();
        int placeY = result.block.getY();
        int placeZ = result.block.getZ();
        
        // 根据击中的面计算放置位置
        switch (result.face) {
            case 0: // 前面 (+Z)
                placeZ++;
                break;
            case 1: // 后面 (-Z)
                placeZ--;
                break;
            case 2: // 左面 (-X)
                placeX--;
                break;
            case 3: // 右面 (+X)
                placeX++;
                break;
            case 4: // 上面 (+Y)
                placeY++;
                break;
            case 5: // 下面 (-Y)
                placeY--;
                break;
            default:
                return false;
        }
        
        // 检查放置位置是否已经有方块
        Block existingBlock = getBlockAt(placeX, placeY, placeZ);
        if (existingBlock != null && existingBlock.getType() != Block.BlockType.AIR) {
            return false; // 位置已被占用
        }
        
        // 计算玩家的脚部位置（从摄像头位置推算）
        float playerFootX = cameraX;
        float playerFootY = cameraY - (26.0f / 16.0f); // 摄像头高度26像素 = 1.625方块
        float playerFootZ = cameraZ;
        
        // 检查玩家是否与放置位置重叠（使用正确的玩家脚部位置）
        if (isPlayerColliding(playerFootX, playerFootY, playerFootZ, placeX, placeY, placeZ)) {
            return false; // 会与玩家碰撞
        }
        
        // 在世界中添加新方块
        boolean success = world.addBlock(placeX, placeY, placeZ, blockType);
        if (success) {
            // 标记需要重新构建渲染缓冲区
            needsMeshRebuild = true;
        }
        
        return success;
    }
    
    /**
     * 简单的玩家碰撞检测
     */
    private boolean isPlayerColliding(float playerX, float playerY, float playerZ,
                                     int blockX, int blockY, int blockZ) {
        // 玩家高度约为1.8格，宽度约为0.6格
        float playerWidth = 0.6f;
        float playerHeight = 1.8f;
        
        // 检查X和Z平面的碰撞
        boolean xCollision = Math.abs(playerX - blockX) < (0.5f + playerWidth / 2);
        boolean zCollision = Math.abs(playerZ - blockZ) < (0.5f + playerWidth / 2);
        
        // 检查Y轴碰撞（玩家脚部到头部）
        boolean yCollision = (playerY - playerHeight / 2 < blockY + 0.5f) &&
                            (playerY + playerHeight / 2 > blockY - 0.5f);
        
        return xCollision && zCollision && yCollision;
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