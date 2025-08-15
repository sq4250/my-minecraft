package com.tsian;

import com.tsian.world.World;
import com.tsian.world.Block;

/**
 * 玩家类 - 管理玩家位置、碰撞箱和物理
 */
public class Player {
    
    // 玩家碰撞箱尺寸（以方块为单位，1方块 = 16像素）
    private static final float PLAYER_WIDTH = 10.0f / 16.0f;   // 0.625方块宽
    private static final float PLAYER_DEPTH = 10.0f / 16.0f;   // 0.625方块深
    private static final float PLAYER_HEIGHT = 29.0f / 16.0f;  // 1.8125方块高
    
    // 摄像机相对于玩家脚部的高度
    private static final float CAMERA_HEIGHT = 26.0f / 16.0f;  // 1.625方块高
    
    // 物理常量
    private static final float GRAVITY = -20.0f;        // 重力加速度
    private static final float TERMINAL_VELOCITY = -50.0f; // 最大下落速度
    private static final float JUMP_VELOCITY = 8.0f;    // 跳跃初速度
    
    // 玩家位置（脚部中心点）
    private float x, y, z;
    
    // 玩家速度
    private float velocityX, velocityY, velocityZ;
    
    // 物理状态
    private boolean onGround;
    private boolean inWater;
    private boolean isFlying;           // 飞行状态
    
    // 移动参数
    private float walkSpeed = 4.3f;     // 行走速度
    private float sprintSpeed = 5.6f;   // 冲刺速度
    private float flySpeed = 10.8f;     // 飞行速度
    
    // 双击空格检测
    private boolean spacePressed = false;
    private float lastSpacePressTime = 0;
    private static final float DOUBLE_CLICK_TIME = 0.2f; // 快速双击时间间隔
    
    // 世界引用
    private World world;
    
    public Player(float startX, float startY, float startZ, World world) {
        this.x = startX;
        this.y = startY;
        this.z = startZ;
        this.world = world;
        this.velocityX = 0;
        this.velocityY = 0;
        this.velocityZ = 0;
        this.onGround = false;
        this.inWater = false;
        this.isFlying = false;
        this.spacePressed = false;
        this.lastSpacePressTime = 0;
    }
    
    /**
     * 更新玩家物理和位置
     */
    public void update(float deltaTime) {
        // 飞行模式下的物理处理
        if (isFlying) {
            // 飞行模式：无重力，自由移动
            float newX = x + velocityX * deltaTime;
            float newY = y + velocityY * deltaTime;
            float newZ = z + velocityZ * deltaTime;
            
            // 飞行模式下无碰撞检测，可以穿越方块
            x = newX;
            y = newY;
            z = newZ;
            
            // 飞行模式下的阻力
            velocityX *= 0.8f;
            velocityY *= 0.8f;
            velocityZ *= 0.8f;
            
            onGround = false;
        } else {
            // 正常物理模式
            // 首先检测当前是否在地面上
            checkGroundCollision();
            
            // 应用重力
            if (!onGround && !inWater) {
                velocityY += GRAVITY * deltaTime;
                if (velocityY < TERMINAL_VELOCITY) {
                    velocityY = TERMINAL_VELOCITY;
                }
            } else if (onGround && velocityY < 0) {
                // 在地面上时，清除向下的速度
                velocityY = 0;
            }
            
            // 在水中时减缓下落速度
            if (inWater) {
                velocityY *= 0.8f; // 水中阻力
            }
            
            // 计算新位置
            float newX = x + velocityX * deltaTime;
            float newY = y + velocityY * deltaTime;
            float newZ = z + velocityZ * deltaTime;
            
            // 执行碰撞检测和位置更新
            handleMovement(newX, newY, newZ);
            
            // 再次检测地面（移动后）
            checkGroundCollision();
            
            // 减缓水平速度（摩擦力）
            if (onGround) {
                velocityX *= 0.5f;
                velocityZ *= 0.5f;
            } else {
                velocityX *= 0.91f; // 空气阻力
                velocityZ *= 0.91f;
            }
            
            // 检测是否在水中
            checkWaterCollision();
        }
    }
    
    /**
     * 处理玩家移动和碰撞
     */
    private void handleMovement(float newX, float newY, float newZ) {
        // 分别检测XYZ轴的碰撞
        
        // X轴移动
        if (velocityX != 0) {
            if (!checkCollision(newX, y, z)) {
                x = newX;
            } else {
                velocityX = 0; // 撞墙停止
            }
        }
        
        // Y轴移动
        if (velocityY != 0) {
            if (!checkCollision(x, newY, z)) {
                y = newY;
            } else {
                velocityY = 0; // 撞到障碍物停止
            }
        }
        
        // Z轴移动
        if (velocityZ != 0) {
            if (!checkCollision(x, y, newZ)) {
                z = newZ;
            } else {
                velocityZ = 0; // 撞墙停止
            }
        }
    }
    
    /**
     * 独立的地面检测方法
     */
    private void checkGroundCollision() {
        // 检测玩家脚部稍下方是否有固体方块
        float testY = y - 0.01f; // 脚部稍下方
        onGround = checkCollision(x, testY, z);
    }
    
    /**
     * 检测碰撞箱是否与方块碰撞
     */
    private boolean checkCollision(float playerX, float playerY, float playerZ) {
        // 计算碰撞箱边界
        float minX = playerX - PLAYER_WIDTH / 2;
        float maxX = playerX + PLAYER_WIDTH / 2;
        float minY = playerY;
        float maxY = playerY + PLAYER_HEIGHT;
        float minZ = playerZ - PLAYER_DEPTH / 2;
        float maxZ = playerZ + PLAYER_DEPTH / 2;
        
        // 检查碰撞箱范围内的所有方块
        int startX = (int) Math.floor(minX);
        int endX = (int) Math.floor(maxX);
        int startY = (int) Math.floor(minY);
        int endY = (int) Math.floor(maxY);
        int startZ = (int) Math.floor(minZ);
        int endZ = (int) Math.floor(maxZ);
        
        for (int bx = startX; bx <= endX; bx++) {
            for (int by = startY; by <= endY; by++) {
                for (int bz = startZ; bz <= endZ; bz++) {
                    Block block = world.getBlockAt(bx, by, bz);
                    if (block != null && block.isSolid()) {
                        // 检查与方块的精确碰撞
                        if (isAABBColliding(minX, minY, minZ, maxX, maxY, maxZ,
                                          bx, by, bz, bx + 1, by + 1, bz + 1)) {
                            return true; // 发生碰撞
                        }
                    }
                }
            }
        }
        
        return false; // 无碰撞
    }
    
    /**
     * 检测两个AABB包围盒是否碰撞
     */
    private boolean isAABBColliding(float minX1, float minY1, float minZ1, 
                                   float maxX1, float maxY1, float maxZ1,
                                   float minX2, float minY2, float minZ2,
                                   float maxX2, float maxY2, float maxZ2) {
        return maxX1 > minX2 && minX1 < maxX2 &&
               maxY1 > minY2 && minY1 < maxY2 &&
               maxZ1 > minZ2 && minZ1 < maxZ2;
    }
    
    /**
     * 检测是否在水中
     */
    private void checkWaterCollision() {
        // 检查玩家头部是否在水中
        float headY = y + PLAYER_HEIGHT * 0.8f;
        Block headBlock = world.getBlockAt((int) Math.floor(x), (int) Math.floor(headY), (int) Math.floor(z));
        inWater = (headBlock != null && headBlock.getType() == Block.BlockType.WATER);
    }
    
    /**
     * 处理玩家输入移动（基于摄像头方向）
     */
    public void handleInput(float[] moveDirection, boolean jump, boolean sprint, float deltaTime) {
        
        // 双击空格检测（需要传入当前时间）
        handleSpaceDoubleClick(jump, (float) System.currentTimeMillis() / 1000.0f);
        
        if (isFlying) {
            // 飞行模式输入处理
            handleFlyingInput(moveDirection, jump, sprint, deltaTime);
        } else {
            // 正常模式输入处理
            handleNormalInput(moveDirection, jump, sprint, deltaTime);
        }
    }
    
    /**
     * 处理双击空格检测
     */
    private void handleSpaceDoubleClick(boolean currentSpacePressed, float currentTime) {
        if (currentSpacePressed && !spacePressed) {
            // 空格刚按下
            if (currentTime - lastSpacePressTime < DOUBLE_CLICK_TIME) {
                // 双击检测成功，切换飞行模式
                toggleFlying();
                lastSpacePressTime = 0; // 重置时间避免连续触发
            } else {
                lastSpacePressTime = currentTime;
            }
        }
        spacePressed = currentSpacePressed;
    }
    
    /**
     * 切换飞行模式
     */
    private void toggleFlying() {
        isFlying = !isFlying;
        if (isFlying) {
            // 开始飞行，清除Y轴速度避免突然下坠或上升
            velocityY = 0;
            System.out.println("飞行模式：开启");
        } else {
            // 停止飞行，让玩家自然下落
            velocityY = 0;
            System.out.println("飞行模式：关闭");
        }
    }
    
    /**
     * 处理飞行模式输入
     */
    private void handleFlyingInput(float[] moveDirection, boolean jump, boolean sprint, float deltaTime) {
        float speed = flySpeed;
        
        // 水平移动
        if (moveDirection != null && moveDirection.length >= 2) {
            if (moveDirection[0] != 0 || moveDirection[1] != 0) {
                velocityX = moveDirection[0] * speed;
                velocityZ = moveDirection[1] * speed;
            } else {
                velocityX = 0;
                velocityZ = 0;
            }
        }
        
        // 垂直移动：空格上升，shift下降
        if (jump) {
            // 飞行模式下空格上升
            velocityY = flySpeed;
        } else if (sprint) {
            // shift下降
            velocityY = -flySpeed;
        } else {
            velocityY = 0;
        }
    }
    
    /**
     * 处理正常模式输入
     */
    private void handleNormalInput(float[] moveDirection, boolean jump, boolean sprint, float deltaTime) {
        float speed = sprint ? sprintSpeed : walkSpeed;
        
        // 使用从摄像头获取的移动方向
        if (moveDirection != null && moveDirection.length >= 2) {
            if (moveDirection[0] != 0 || moveDirection[1] != 0) {
                velocityX = moveDirection[0] * speed;
                velocityZ = moveDirection[1] * speed;
            } else {
                // 没有输入时，在地面上快速减速
                if (onGround) {
                    velocityX = 0;
                    velocityZ = 0;
                }
            }
        }
        
        // 跳跃
        if (jump && (onGround || inWater)) {
            if (inWater) {
                velocityY = JUMP_VELOCITY * 0.5f; // 水中跳跃力度减半
            } else {
                velocityY = JUMP_VELOCITY;
            }
            onGround = false;
        }
    }
    
    /**
     * 兼容性方法 - 保持原有接口
     */
    public void handleInput(boolean forward, boolean backward, boolean left, boolean right,
                           boolean jump, boolean sprint, float deltaTime) {
        // 简单的直接方向移动（不基于摄像头）
        float speed = sprint ? sprintSpeed : walkSpeed;
        float moveX = 0, moveZ = 0;
        
        if (forward) moveZ -= 1;
        if (backward) moveZ += 1;
        if (left) moveX -= 1;
        if (right) moveX += 1;
        
        // 归一化移动向量
        if (moveX != 0 || moveZ != 0) {
            float length = (float) Math.sqrt(moveX * moveX + moveZ * moveZ);
            moveX /= length;
            moveZ /= length;
            
            velocityX = moveX * speed;
            velocityZ = moveZ * speed;
        } else {
            if (onGround) {
                velocityX = 0;
                velocityZ = 0;
            }
        }
        
        // 跳跃
        if (jump && (onGround || inWater)) {
            if (inWater) {
                velocityY = JUMP_VELOCITY * 0.5f;
            } else {
                velocityY = JUMP_VELOCITY;
            }
            onGround = false;
        }
    }
    
    /**
     * 获取摄像机位置（玩家眼部位置）
     */
    public float[] getCameraPosition() {
        return new float[]{x, y + CAMERA_HEIGHT, z};
    }
    
    /**
     * 设置玩家位置
     */
    public void setPosition(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }
    
    /**
     * 传送玩家到指定位置（无碰撞检测）
     */
    public void teleport(float x, float y, float z) {
        setPosition(x, y, z);
        velocityX = velocityY = velocityZ = 0;
        onGround = false;
    }
    
    // Getter方法
    public float getX() { return x; }
    public float getY() { return y; }
    public float getZ() { return z; }
    
    public float getVelocityX() { return velocityX; }
    public float getVelocityY() { return velocityY; }
    public float getVelocityZ() { return velocityZ; }
    
    public boolean isOnGround() { return onGround; }
    public boolean isInWater() { return inWater; }
    public boolean isFlying() { return isFlying; }
    
    // 碰撞箱尺寸Getter
    public static float getPlayerWidth() { return PLAYER_WIDTH; }
    public static float getPlayerHeight() { return PLAYER_HEIGHT; }
    public static float getPlayerDepth() { return PLAYER_DEPTH; }
    public static float getCameraHeight() { return CAMERA_HEIGHT; }
}