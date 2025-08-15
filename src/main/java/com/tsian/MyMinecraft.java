package com.tsian;

import com.tsian.world.World;
import com.tsian.render.SimpleRenderer;

import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 简易版Minecraft - 使用LWJGL和现代OpenGL可编程管线开发
 */
public class MyMinecraft {
    
    // 窗口句柄
    private long window;
    
    // 窗口尺寸
    private static final int DEFAULT_WINDOW_WIDTH = 1024;
    private static final int DEFAULT_WINDOW_HEIGHT = 768;
    private static final String WINDOW_TITLE = "My Minecraft";
    
    // 当前窗口尺寸
    private int currentWidth = DEFAULT_WINDOW_WIDTH;
    private int currentHeight = DEFAULT_WINDOW_HEIGHT;
    
    // 全屏状态
    private boolean isFullscreen = false;
    private int windowedWidth = DEFAULT_WINDOW_WIDTH;
    private int windowedHeight = DEFAULT_WINDOW_HEIGHT;
    private int windowedPosX, windowedPosY;
    
    // 渲染相关变量
    private int textureId;
    private int shaderProgram;
    
    // 世界和渲染
    private World world;
    
    // 摄像头和时间
    private Camera camera;
    private float lastFrameTime;
    
    // 简化渲染器
    private SimpleRenderer simpleRenderer;
    
    // 着色器管理器
    private ShaderManager shaderManager;
    
    // 鼠标状态
    private boolean firstMouse = true;
    private boolean mouseCaptured = false;
    private float lastX = DEFAULT_WINDOW_WIDTH / 2.0f;
    private float lastY = DEFAULT_WINDOW_HEIGHT / 2.0f;
    
    
    public void run() {
        System.out.println("Starting My Minecraft with Modern OpenGL...");
        
        init();
        loop();
        
        // 清理资源
        cleanup();
        
        // 释放窗口和窗口回调
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // 终止GLFW并释放错误回调
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    private void init() {
        // 设置错误回调，将GLFW错误消息打印到System.err
        GLFWErrorCallback.createPrint(System.err).set();
        
        // 初始化GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }
        
        // 配置GLFW - 使用OpenGL 3.3 Core Profile
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        
        // 创建窗口
        window = glfwCreateWindow(currentWidth, currentHeight, WINDOW_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        
        // 设置键盘回调
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true);
            }
            // 按F1切换鼠标捕获模式
            if (key == GLFW_KEY_F1 && action == GLFW_PRESS) {
                toggleMouseCapture();
            }
            // 按F11切换全屏模式
            if (key == GLFW_KEY_F11 && action == GLFW_PRESS) {
                toggleFullscreen();
            }
        });
        
        // 设置窗口大小变化回调
        glfwSetWindowSizeCallback(window, (window, width, height) -> {
            currentWidth = width;
            currentHeight = height;
            glViewport(0, 0, width, height);
            System.out.println("Window resized to: " + width + "x" + height);
        });
        
        // 设置鼠标回调
        glfwSetCursorPosCallback(window, (window, xpos, ypos) -> {
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
        glfwSetMouseButtonCallback(window, (window, button, action, mods) -> {
            if (button == GLFW_MOUSE_BUTTON_LEFT && action == GLFW_PRESS) {
                if (!mouseCaptured) {
                    captureMouse();
                }
            }
        });
        
        // 窗口居中
        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);
            
            glfwGetWindowSize(window, pWidth, pHeight);
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
            
            glfwSetWindowPos(
                window,
                (vidmode.width() - pWidth.get(0)) / 2,
                (vidmode.height() - pHeight.get(0)) / 2
            );
        }
        
        // 使当前线程的OpenGL上下文为当前上下文
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // 启用v-sync
        glfwShowWindow(window);
        
        // 初始化摄像头和世界
        camera = new Camera(0.0f, 2.0f, 5.0f); // 调整摄像头位置
        world = new World();
        lastFrameTime = (float) glfwGetTime();
    }
    
    private void loop() {
        // 创建OpenGL上下文能力
        GL.createCapabilities();
        
        // 打印OpenGL版本信息
        System.out.println("OpenGL Version: " + glGetString(GL_VERSION));
        System.out.println("OpenGL Renderer: " + glGetString(GL_RENDERER));
        
        // 初始化渲染资源
        try {
            initRender();
            System.out.println("Render initialization successful!");
        } catch (Exception e) {
            System.err.println("Failed to initialize render: " + e.getMessage());
            e.printStackTrace();
            return;
        }
        
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
        
        // 主渲染循环
        while (!glfwWindowShouldClose(window)) {
            // 先处理事件，确保输入及时响应
            glfwPollEvents();
            
            // 计算帧时间
            float currentFrameTime = (float) glfwGetTime();
            float deltaTime = currentFrameTime - lastFrameTime;
            lastFrameTime = currentFrameTime;
            
            // 处理输入
            processInput(deltaTime);
            
            // 简单世界不需要更新
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
            render();
            
            glfwSwapBuffers(window);
        }
    }
    
    private void processInput(float deltaTime) {
        // 直接查询键盘状态，避免回调冲突
        boolean forward = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS;
        boolean backward = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS;
        boolean left = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
        boolean right = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS;
        boolean up = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
        boolean down = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
        
        camera.processKeyboardInput(forward, backward, left, right, up, down, deltaTime);
    }
    
    private void captureMouse() {
        mouseCaptured = true;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        firstMouse = true; // 重置首次移动标志
        System.out.println("Mouse captured. Press F1 to release.");
    }
    
    private void releaseMouse() {
        mouseCaptured = false;
        glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        System.out.println("Mouse released. Click to capture again.");
    }
    
    private void toggleMouseCapture() {
        if (mouseCaptured) {
            releaseMouse();
        } else {
            captureMouse();
        }
    }
    
    /**
     * 切换全屏模式
     */
    private void toggleFullscreen() {
        if (isFullscreen) {
            // 切换到窗口模式
            glfwSetWindowMonitor(window, NULL, windowedPosX, windowedPosY,
                               windowedWidth, windowedHeight, GLFW_DONT_CARE);
            currentWidth = windowedWidth;
            currentHeight = windowedHeight;
            isFullscreen = false;
            System.out.println("Switched to windowed mode: " + currentWidth + "x" + currentHeight);
        } else {
            // 保存当前窗口位置和大小
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer pWidth = stack.mallocInt(1);
                IntBuffer pHeight = stack.mallocInt(1);
                IntBuffer pXPos = stack.mallocInt(1);
                IntBuffer pYPos = stack.mallocInt(1);
                
                glfwGetWindowSize(window, pWidth, pHeight);
                glfwGetWindowPos(window, pXPos, pYPos);
                
                windowedWidth = pWidth.get(0);
                windowedHeight = pHeight.get(0);
                windowedPosX = pXPos.get(0);
                windowedPosY = pYPos.get(0);
            }
            
            // 切换到全屏模式
            long monitor = glfwGetPrimaryMonitor();
            GLFWVidMode vidMode = glfwGetVideoMode(monitor);
            
            glfwSetWindowMonitor(window, monitor, 0, 0,
                               vidMode.width(), vidMode.height(), vidMode.refreshRate());
            currentWidth = vidMode.width();
            currentHeight = vidMode.height();
            isFullscreen = true;
            System.out.println("Switched to fullscreen mode: " + currentWidth + "x" + currentHeight);
        }
        
        // 更新视口
        glViewport(0, 0, currentWidth, currentHeight);
        
        // 重置鼠标位置
        lastX = currentWidth / 2.0f;
        lastY = currentHeight / 2.0f;
        firstMouse = true;
    }
    
    private void initRender() {
        // 加载纹理
        textureId = TextureLoader.loadTexture("texture/my-mc-texture.png");
        System.out.println("Texture loaded with ID: " + textureId);
        
        // 初始化着色器管理器
        shaderManager = new ShaderManager();
        shaderProgram = shaderManager.createBlockShaderProgram();
        System.out.println("Shader program created with ID: " + shaderProgram);
        // 初始化简化渲染器并构建几何数据
        simpleRenderer = new SimpleRenderer();
        simpleRenderer.buildMeshFromWorld(world);
        System.out.println("Simple renderer initialized and mesh built");
        
    }
    
    
    
    private void render() {
        // 使用着色器程序
        glUseProgram(shaderProgram);
        
        // 设置纹理uniform
        int textureLocation = glGetUniformLocation(shaderProgram, "ourTexture");
        glUniform1i(textureLocation, 0);
        
        // 设置光照参数
        setupLighting();
        
        // 创建变换矩阵
        float[] modelMatrix = createModelMatrix();
        float[] viewMatrix = camera.getViewMatrix();
        float[] projectionMatrix = Camera.perspective(45.0f, (float)currentWidth / (float)currentHeight, 0.1f, 1000.0f);
        
        // 传递矩阵uniform
        shaderManager.uploadMatrix4f(shaderProgram, "model", modelMatrix);
        shaderManager.uploadMatrix4f(shaderProgram, "view", viewMatrix);
        shaderManager.uploadMatrix4f(shaderProgram, "projection", projectionMatrix);
        
        // 简化渲染 - 传递摄像头位置用于透明方块排序
        simpleRenderer.render(textureId, camera.getX(), camera.getY(), camera.getZ());
    }
    
    
    private float[] createModelMatrix() {
        // 创建单位矩阵
        float[] model = new float[16];
        model[0] = 1.0f; model[5] = 1.0f; model[10] = 1.0f; model[15] = 1.0f;
        return model;
    }
    
    /**
     * 设置光照参数
     */
    private void setupLighting() {
        // 固定的全局光照方向（从右上前方照射，角度更温和）
        int lightDirLocation = glGetUniformLocation(shaderProgram, "lightDirection");
        glUniform3f(lightDirLocation, 0.6f, -0.8f, 0.4f);
        
        // 光照颜色（温暖的白色，稍微降低强度）
        int lightColorLocation = glGetUniformLocation(shaderProgram, "lightColor");
        glUniform3f(lightColorLocation, 0.9f, 0.85f, 0.7f);
        
        // 环境光颜色（更温暖的蓝白色，增加整体亮度）
        int ambientColorLocation = glGetUniformLocation(shaderProgram, "ambientColor");
        glUniform3f(ambientColorLocation, 0.6f, 0.7f, 0.9f);
        
        // 环境光强度（提高基础亮度）
        int ambientStrengthLocation = glGetUniformLocation(shaderProgram, "ambientStrength");
        glUniform1f(ambientStrengthLocation, 0.5f);
    }
    
    private void cleanup() {
        // 清理简化渲染器
        if (simpleRenderer != null) simpleRenderer.cleanup();
        
        // 清理着色器管理器
        if (shaderManager != null) shaderManager.cleanup();
        
        // 删除渲染资源
        if (textureId != 0) glDeleteTextures(textureId);
    }
    
    public static void main(String[] args) {
        new MyMinecraft().run();
    }
}
