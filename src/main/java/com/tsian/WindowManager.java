package com.tsian;

import com.tsian.config.GameConfig;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 窗口管理器 - 负责创建和管理游戏窗口
 */
public class WindowManager {
    
    // 窗口句柄
    private long window;
    
    // 窗口尺寸
    private int DEFAULT_WINDOW_WIDTH;
    private int DEFAULT_WINDOW_HEIGHT;
    private String WINDOW_TITLE;
    
    // 当前窗口尺寸
    private int currentWidth = DEFAULT_WINDOW_WIDTH;
    private int currentHeight = DEFAULT_WINDOW_HEIGHT;
    
    // 全屏状态
    private boolean isFullscreen = false;
    private int windowedWidth = DEFAULT_WINDOW_WIDTH;
    private int windowedHeight = DEFAULT_WINDOW_HEIGHT;
    private int windowedPosX, windowedPosY;
    
    public WindowManager() {
        this(new GameConfig());
    }
    
    public WindowManager(GameConfig config) {
        // 使用配置中的参数
        this.DEFAULT_WINDOW_WIDTH = config.window.defaultWidth;
        this.DEFAULT_WINDOW_HEIGHT = config.window.defaultHeight;
        this.WINDOW_TITLE = config.window.title;
        
        // 初始化当前窗口尺寸
        this.currentWidth = DEFAULT_WINDOW_WIDTH;
        this.currentHeight = DEFAULT_WINDOW_HEIGHT;
        this.windowedWidth = DEFAULT_WINDOW_WIDTH;
        this.windowedHeight = DEFAULT_WINDOW_HEIGHT;
    }
    
    /**
     * 初始化GLFW
     */
    public void initGLFW() {
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
    }
    
    /**
     * 创建窗口
     */
    public void createWindow() {
        // 创建窗口
        window = glfwCreateWindow(currentWidth, currentHeight, WINDOW_TITLE, NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
    }
    
    /**
     * 居中窗口
     */
    public void centerWindow() {
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
    }
    
    /**
     * 显示窗口
     */
    public void showWindow() {
        // 使当前线程的OpenGL上下文为当前上下文
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // 启用v-sync
        glfwShowWindow(window);
    }
    
    /**
     * 切换全屏模式
     */
    public void toggleFullscreen() {
        if (isFullscreen) {
            switchToWindowedMode();
        } else {
            switchToFullscreenMode();
        }
    }
    
    private void switchToWindowedMode() {
        // 切换到窗口模式
        glfwSetWindowMonitor(window, NULL, windowedPosX, windowedPosY,
                           windowedWidth, windowedHeight, GLFW_DONT_CARE);
        currentWidth = windowedWidth;
        currentHeight = windowedHeight;
        isFullscreen = false;
        System.out.println("Switched to windowed mode: " + currentWidth + "x" + currentHeight);
    }
    
    private void switchToFullscreenMode() {
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
    
    /**
     * 清理资源
     */
    public void cleanup() {
        // 释放窗口和窗口回调
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        
        // 终止GLFW并释放错误回调
        glfwTerminate();
        glfwSetErrorCallback(null).free();
    }
    
    // Getter方法
    public long getWindow() { return window; }
    public int getCurrentWidth() { return currentWidth; }
    public int getCurrentHeight() { return currentHeight; }
    public boolean isFullscreen() { return isFullscreen; }
    
    public void setCurrentWidth(int currentWidth) { this.currentWidth = currentWidth; }
    public void setCurrentHeight(int currentHeight) { this.currentHeight = currentHeight; }
}