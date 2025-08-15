package com.tsian;

import com.tsian.world.World;
import com.tsian.world.BlockInteractionManager;

import org.lwjgl.opengl.GL;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * 简易版Minecraft - 使用LWJGL和现代OpenGL可编程管线开发
 */
public class MyMinecraft {
    
    // 窗口管理器
    private WindowManager windowManager;
    
    // 输入处理器
    private InputHandler inputHandler;
    
    // 渲染管理器
    private RenderManager renderManager;
    
    // 世界和渲染
    private World world;
    
    // 摄像头、玩家和时间
    private Camera camera;
    private Player player;
    private float lastFrameTime;
    
    // 方块交互管理器
    private BlockInteractionManager interactionManager;
    
    // 标记是否需要重新构建网格
    private boolean meshRebuildNeeded = false;
    
    public void run() {
        System.out.println("Starting My Minecraft with Modern OpenGL...");
        
        init();
        loop();
        
        // 清理资源
        cleanup();
        
        // 清理窗口管理器资源
        if (windowManager != null) {
            windowManager.cleanup();
        }
    }
    
    private void init() {
        // 初始化窗口管理器
        windowManager = new WindowManager();
        windowManager.initGLFW();
        windowManager.createWindow();
        windowManager.centerWindow();
        windowManager.showWindow();
        
        // 初始化摄像头、玩家和世界
        camera = new Camera();
        world = new World();
        
        // 创建玩家并设置初始位置（在空岛中心）
        player = new Player(32.0f, 8.0f, 32.0f, world);
        
        // 初始化基于区块的世界
        world.initializeWorld(player.getX(), player.getZ());
        
        interactionManager = new BlockInteractionManager(world);
        lastFrameTime = (float) glfwGetTime();
        
        // 初始化输入处理器
        inputHandler = new InputHandler(this, windowManager, camera, player, interactionManager);
        inputHandler.setupCallbacks();
    }
    
    private void loop() {
        setupOpenGLContext();
        initializeRenderResources();
        setupOpenGLState();
        
        // 主渲染循环
        while (!glfwWindowShouldClose(windowManager.getWindow())) {
            processFrame();
        }
    }
    
    private void setupOpenGLContext() {
        // 创建OpenGL上下文能力
        GL.createCapabilities();
        
        // 打印OpenGL版本信息
        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("OpenGL Renderer: " + glGetString(GL_RENDERER));
    }
    
    private void initializeRenderResources() {
        // 初始化渲染资源
        try {
            renderManager = new RenderManager();
            renderManager.initRender(world, interactionManager);
            System.out.println("Render initialization successful!");
        } catch (Exception e) {
            System.err.println("Failed to initialize render: " + e.getMessage());
            e.printStackTrace();
            return;
        }
    }
    
    private void setupOpenGLState() {
        // 设置清除颜色为天蓝色
        glClearColor(0.53f, 0.81f, 0.98f, 1.0f);
        
        // 启用深度测试
        glEnable(GL_DEPTH_TEST);
        
        // 启用面剔除 - 剔除背面，从方块内部看不到面
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glFrontFace(GL_CCW); // 逆时针为正面
        
        // 启用混合以支持透明度
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }
    
    private void processFrame() {
        // 先处理事件，确保输入及时响应
        glfwPollEvents();
        
        // 计算帧时间
        float currentFrameTime = (float) glfwGetTime();
        float deltaTime = currentFrameTime - lastFrameTime;
        lastFrameTime = currentFrameTime;
        
        // 处理输入和更新玩家
        if (inputHandler != null) {
            inputHandler.processInput(deltaTime);
        }
        
        // 更新玩家物理和位置
        player.update(deltaTime);
        
        // 更新摄像头位置（跟随玩家）
        float[] cameraPos = player.getCameraPosition();
        camera.setPosition(cameraPos[0], cameraPos[1], cameraPos[2]);
        
        // 更新世界状态（区块加载/卸载）
        world.updateWorld(player.getX(), player.getZ());
        
        // 更新方块交互
        if (inputHandler != null) {
            inputHandler.updateBlockInteraction(currentFrameTime, deltaTime);
        }
        
        // 检查是否需要重新构建网格
        if (meshRebuildNeeded) {
            if (renderManager != null) {
                renderManager.rebuildMesh(world);
            }
            meshRebuildNeeded = false;
        }
        
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        render();
        
        glfwSwapBuffers(windowManager.getWindow());
    }
    
    
    private void render() {
        // 使用渲染管理器进行渲染
        renderManager.render(camera, windowManager.getCurrentWidth(), windowManager.getCurrentHeight());
    }
    
    private void cleanup() {
        if (renderManager != null) {
            renderManager.cleanup();
        }
    }
    
    /**
     * 通知需要重新构建网格
     */
    public void onMeshRebuildNeeded() {
        meshRebuildNeeded = true;
    }
    
    /**
     * 获取世界对象
     */
    public World getWorld() {
        return world;
    }
    
    /**
     * 重新构建网格
     */
    public void rebuildMesh() {
        if (renderManager != null && world != null) {
            renderManager.rebuildMesh(world);
        }
    }
    
    public static void main(String[] args) {
        new MyMinecraft().run();
    }
}
