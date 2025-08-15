package com.tsian;

import com.tsian.world.BlockInteractionManager;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * 输入处理器 - 负责处理键盘和鼠标输入
 */
public class InputHandler {
    
    private MyMinecraft game;
    private WindowManager windowManager;
    private Camera camera;
    private Player player;
    private BlockInteractionManager interactionManager;
    
    // 鼠标状态
    private boolean firstMouse = true;
    private boolean mouseCaptured = false;
    private float lastX = 0;
    private float lastY = 0;
    
    // 鼠标按键状态
    private boolean leftMousePressed = false;
    private boolean rightMousePressed = false;
    
    // 连续放置方块的定时器
    private float lastPlaceTime = 0;
    private static final float PLACE_DELAY = 0.2f; // 200ms放置间隔
    
    public InputHandler(MyMinecraft game, WindowManager windowManager, Camera camera, 
                       Player player, BlockInteractionManager interactionManager) {
        this.game = game;
        this.windowManager = windowManager;
        this.camera = camera;
        this.player = player;
        this.interactionManager = interactionManager;
        
        // 初始化鼠标位置
        this.lastX = windowManager.getCurrentWidth() / 2.0f;
        this.lastY = windowManager.getCurrentHeight() / 2.0f;
    }
    
    /**
     * 设置GLFW回调
     */
    public void setupCallbacks() {
        long window = windowManager.getWindow();
        
        // 设置键盘回调
        glfwSetKeyCallback(window, (windowHandle, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(windowHandle, true);
            }
            // 按F1切换鼠标捕获模式
            if (key == GLFW_KEY_F1 && action == GLFW_PRESS) {
                toggleMouseCapture();
            }
            // 按F11切换全屏模式
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                windowManager.toggleFullscreen();
                // 更新视口
                glViewport(0, 0, windowManager.getCurrentWidth(), windowManager.getCurrentHeight());
                // 重置鼠标位置
                lastX = windowManager.getCurrentWidth() / 2.0f;
                lastY = windowManager.getCurrentHeight() / 2.0f;
                firstMouse = true;
            }
        });
        
        // 设置窗口大小变化回调
        glfwSetWindowSizeCallback(window, (windowHandle, width, height) -> {
            windowManager.setCurrentWidth(width);
            windowManager.setCurrentHeight(height);
            glViewport(0, 0, width, height);
            System.out.println("Window resized to: " + width + "x" + height);
        });
        
        // 设置鼠标回调
        glfwSetCursorPosCallback(window, (windowHandle, xpos, ypos) -> {
            if (!mouseCaptured) return; // 只有在捕获模式下才处理鼠标移动
            
            if (firstMouse) {
                lastX = (float) xpos;
                lastY = (float) ypos;
                firstMouse = false;
                return; // 第一次移动时不处理偏移
            }
            
            float xoffset = (float) xpos - lastX;
            float yoffset = lastY - (float) ypos; // 反转Y轴，因为Y坐标从底部到顶部
            
            lastX = (float) xpos;
            lastY = (float) ypos;
            
            // 立即处理鼠标移动，不等待下一帧
            camera.processMouseMovement(xoffset, yoffset);
        });
        
        // 鼠标点击回调，点击时捕获鼠标
        glfwSetMouseButtonCallback(window, (windowHandle, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT) {
                if (action == GLFW_PRESS) {
                    if (!mouseCaptured) {
                        captureMouse();
                    } else {
                        // 开始破坏方块
                        leftMousePressed = true;
                        handleMouseClick();
                    }
                } else if (action == GLFW_RELEASE) {
                    // 停止破坏方块
                    leftMousePressed = false;
                    if (interactionManager != null) {
                        interactionManager.stopBreaking();
                    }
                }
            } else if (button == GLFW_MOUSE_BUTTON_RIGHT) {
                if (action == GLFW_PRESS) {
                    if (!mouseCaptured) {
                        captureMouse();
                    } else {
                        // 开始连续放置方块
                        rightMousePressed = true;
                        handleRightClick();
                    }
                } else if (action == GLFW_RELEASE) {
                    // 停止连续放置方块
                    rightMousePressed = false;
                }
            }
        });
    }
    
    /**
     * 处理鼠标点击
     */
    private void handleMouseClick() {
        if (!mouseCaptured || interactionManager == null) return;
        
        // 使用精确的屏幕中心射线方向
        float[] cameraDirection = camera.getCenterRayDirection();
        
        // 执行射线投射（从摄像头位置稍微向前偏移一点，确保射线从摄像头中心发出）
        float rayStartX = camera.getX() + cameraDirection[0] * 0.1f;
        float rayStartY = camera.getY() + cameraDirection[1] * 0.1f;
        float rayStartZ = camera.getZ() + cameraDirection[2] * 0.1f;
        
        BlockInteractionManager.RaycastResult result = interactionManager.raycast(
            rayStartX, rayStartY, rayStartZ,
            cameraDirection[0], cameraDirection[1], cameraDirection[2]
        );
        
        if (result.hit) {
            interactionManager.setTargetBlock(result.block, result.face);
            interactionManager.startBreaking((float) glfwGetTime());
        }
    }
    
    /**
     * 处理右键点击（放置木板）
     */
    private void handleRightClick() {
        if (!mouseCaptured || interactionManager == null) return;
        
        // 使用精确的屏幕中心射线方向
        float[] cameraDirection = camera.getCenterRayDirection();
        
        // 射线起点稍微向前偏移一点，确保射线从摄像头中心发出
        float rayStartX = camera.getX() + cameraDirection[0] * 0.1f;
        float rayStartY = camera.getY() + cameraDirection[1] * 0.1f;
        float rayStartZ = camera.getZ() + cameraDirection[2] * 0.1f;
        
        // 尝试放置木板方块
        boolean success = interactionManager.placeBlock(
            rayStartX, rayStartY, rayStartZ,
            cameraDirection[0], cameraDirection[1], cameraDirection[2],
            com.tsian.world.Block.BlockType.WOOD_PLANK
        );
        
        if (success) {
            // 如果放置成功，通知游戏需要重新构建网格
            game.onMeshRebuildNeeded();
            System.out.println("木板放置成功!");
        } else {
            System.out.println("无法在此位置放置木板");
        }
    }
    
    /**
     * 处理玩家输入
     */
    public void processInput(float deltaTime) {
        long window = windowManager.getWindow();
        
        // 直接查询键盘状态，避免回调冲突
        boolean forward = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS;
        boolean backward = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS;
        boolean left = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
        boolean right = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS;
        boolean jump = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
        boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS; // 改为shift键
        
        // 获取摄像头方向并传递给玩家
        float[] moveDir = camera.getMovementDirection(forward, backward, left, right);
        
        // 使用基于摄像头方向的移动（shift用于飞行下降或地面冲刺）
        player.handleInput(moveDir, jump, shift, deltaTime);
    }
    
    /**
     * 更新方块交互
     */
    public void updateBlockInteraction(float currentTime, float deltaTime) {
        if (interactionManager == null) return;
        
        // 使用精确的屏幕中心射线方向
        float[] cameraDirection = camera.getCenterRayDirection();
        
        // 射线起点稍微向前偏移一点，确保射线从摄像头中心发出
        float rayStartX = camera.getX() + cameraDirection[0] * 0.1f;
        float rayStartY = camera.getY() + cameraDirection[1] * 0.1f;
        float rayStartZ = camera.getZ() + cameraDirection[2] * 0.1f;
        
        BlockInteractionManager.RaycastResult result = interactionManager.raycast(
            rayStartX, rayStartY, rayStartZ,
            cameraDirection[0], cameraDirection[1], cameraDirection[2]
        );
        
        // 处理连续破坏方块
        if (leftMousePressed) {
            if (result.hit) {
                // 如果当前没有在破坏方块，开始破坏
                if (!interactionManager.isBreaking()) {
                    interactionManager.setTargetBlock(result.block, result.face);
                    interactionManager.startBreaking(currentTime);
                }
                // 如果已经在破坏同一个方块，继续破坏
                else if (result.block == interactionManager.getTargetBlock()) {
                    interactionManager.updateBreaking(currentTime);
                    
                    // 检查是否需要重新构建渲染缓冲区（方块被破坏后）
                    if (interactionManager.needsMeshRebuild()) {
                        // 通知游戏需要重新构建网格
                        game.onMeshRebuildNeeded();
                        interactionManager.markMeshRebuilt();
                    }
                }
                // 如果目标方块改变，重新开始破坏
                else {
                    interactionManager.stopBreaking();
                    interactionManager.setTargetBlock(result.block, result.face);
                    interactionManager.startBreaking(currentTime);
                }
            } else {
                // 没有击中方块，停止破坏
                interactionManager.stopBreaking();
                interactionManager.setTargetBlock(null, -1);
            }
        }
        // 处理连续放置方块
        else if (rightMousePressed) {
            // 更新放置定时器
            lastPlaceTime += deltaTime;
            
            // 如果达到放置间隔，尝试放置方块
            if (lastPlaceTime >= PLACE_DELAY) {
                if (result.hit) {
                    // 尝试放置木板方块
                    boolean success = interactionManager.placeBlock(
                        rayStartX, rayStartY, rayStartZ,
                        cameraDirection[0], cameraDirection[1], cameraDirection[2],
                        com.tsian.world.Block.BlockType.WOOD_PLANK
                    );
                    
                    if (success) {
                        // 如果放置成功，通知游戏需要重新构建网格
                        game.onMeshRebuildNeeded();
                        System.out.println("木板放置成功!");
                    } else {
                        System.out.println("无法在此位置放置木板");
                    }
                }
                
                // 重置放置定时器
                lastPlaceTime = 0;
            }
            
            // 更新目标方块显示
            if (result.hit) {
                interactionManager.setTargetBlock(result.block, result.face);
            } else {
                interactionManager.setTargetBlock(null, -1);
            }
        }
        // 如果鼠标没有按下，只是更新目标方块（用于显示十字标记）
        else {
            if (result.hit) {
                interactionManager.setTargetBlock(result.block, result.face);
            } else {
                interactionManager.setTargetBlock(null, -1);
            }
        }
    }
    
    private void captureMouse() {
        mouseCaptured = true;
        glfwSetInputMode(windowManager.getWindow(), GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        firstMouse = true; // 重置首次移动标志
        System.out.println("Mouse captured. Press F1 to release.");
    }
    
    private void releaseMouse() {
        mouseCaptured = false;
        glfwSetInputMode(windowManager.getWindow(), GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        System.out.println("Mouse released. Click to capture again.");
    }
    
    private void toggleMouseCapture() {
        if (mouseCaptured) {
            releaseMouse();
        } else {
            captureMouse();
        }
    }
    
    // Getter方法
    public boolean isMouseCaptured() { return mouseCaptured; }
    public boolean isFirstMouse() { return firstMouse; }
    public float getLastX() { return lastX; }
    public float getLastY() { return lastY; }
    public boolean isLeftMousePressed() { return leftMousePressed; }
    
    // Setter方法
    public void setFirstMouse(boolean firstMouse) { this.firstMouse = firstMouse; }
    public void setLastX(float lastX) { this.lastX = lastX; }
    public void setLastY(float lastY) { this.lastY = lastY; }
}