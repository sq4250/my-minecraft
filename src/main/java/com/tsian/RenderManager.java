package com.tsian;

import com.tsian.render.ShaderManager;
import com.tsian.render.SimpleRenderer;
import com.tsian.render.TextureLoader;
import com.tsian.render.UIRenderer;
import com.tsian.world.World;
import com.tsian.world.BlockInteractionManager;

import static org.lwjgl.opengl.GL20.*;

/**
 * 渲染管理器 - 负责管理游戏的渲染资源和渲染过程
 */
public class RenderManager {
    
    // 渲染相关变量
    private int textureId;
    private int shaderProgram;
    
    // 渲染器组件
    private SimpleRenderer simpleRenderer;
    private ShaderManager shaderManager;
    private UIRenderer uiRenderer;
    
    // 方块交互管理器引用
    private BlockInteractionManager interactionManager;
    
    /**
     * 初始化渲染资源
     */
    public void initRender(World world, BlockInteractionManager interactionManager) {
        this.interactionManager = interactionManager;
        initTexture();
        initShaders();
        initRenderer(world);
        initUIRenderer();
    }
    
    /**
     * 初始化纹理
     */
    private void initTexture() {
        // 加载纹理
        textureId = TextureLoader.loadTexture("texture/my-mc-texture.png");
        System.out.println("Texture loaded with ID: " + textureId);
    }
    
    /**
     * 初始化着色器
     */
    private void initShaders() {
        // 初始化着色器管理器
        shaderManager = new ShaderManager();
        shaderProgram = shaderManager.createBlockShaderProgram();
        System.out.println("Shader program created with ID: " + shaderProgram);
    }
    
    /**
     * 初始化渲染器
     */
    private void initRenderer(World world) {
        // 初始化简化渲染器并构建几何数据
        simpleRenderer = new SimpleRenderer();
        simpleRenderer.buildMeshFromWorld(world);
        System.out.println("Simple renderer initialized and mesh built");
    }
    
    /**
     * 初始化UI渲染器
     */
    private void initUIRenderer() {
        // 初始化UI渲染器
        uiRenderer = new UIRenderer();
        System.out.println("UI renderer initialized");
    }
    
    /**
     * 渲染一帧
     */
    public void render(Camera camera, int windowWidth, int windowHeight) {
        // 使用着色器程序
        glUseProgram(shaderProgram);
        
        // 设置纹理uniform
        int textureLocation = glGetUniformLocation(shaderProgram, "ourTexture");
        glUniform1i(textureLocation, 0);
        
        // 设置破坏效果参数
        setupBreakingEffect(camera);
        
        // 传递矩阵uniform
        uploadMatrices(camera, windowWidth, windowHeight);
        
        // 设置光照方向uniform (固定方向光)
        setupLighting();
        
        // 简化渲染 - 传递摄像头位置用于透明方块排序
        simpleRenderer.render(textureId, camera.getX(), camera.getY(), camera.getZ());
        
        // 渲染UI元素（十字标记）
        uiRenderer.renderCrosshair(windowWidth, windowHeight);
    }
    
    /**
     * 传递矩阵uniform
     */
    private void uploadMatrices(Camera camera, int windowWidth, int windowHeight) {
        // 创建变换矩阵
        float[] modelMatrix = createModelMatrix();
        float[] viewMatrix = camera.getViewMatrix();
        float[] projectionMatrix = Camera.perspective(45.0f, (float)windowWidth / (float)windowHeight, 0.1f, 1000.0f);
        
        // 传递矩阵uniform
        shaderManager.uploadMatrix4f(shaderProgram, "model", modelMatrix);
        shaderManager.uploadMatrix4f(shaderProgram, "view", viewMatrix);
        shaderManager.uploadMatrix4f(shaderProgram, "projection", projectionMatrix);
    }
    
    /**
     * 创建模型矩阵
     */
    private float[] createModelMatrix() {
        // 创建单位矩阵
        float[] model = new float[16];
        model[0] = 1.0f; model[5] = 1.0f; model[10] = 1.0f; model[15] = 1.0f;
        return model;
    }
    
    /**
     * 设置破坏效果参数
     */
    private void setupBreakingEffect(Camera camera) {
        if (interactionManager != null) {
            // 破坏进度
            int breakProgressLocation = glGetUniformLocation(shaderProgram, "breakProgress");
            glUniform1f(breakProgressLocation, interactionManager.getBreakProgress());
            
            // 目标方块位置
            int targetBlockPosLocation = glGetUniformLocation(shaderProgram, "targetBlockPos");
            if (interactionManager.getTargetBlock() != null) {
                glUniform3f(targetBlockPosLocation,
                           interactionManager.getTargetBlock().getX(),
                           interactionManager.getTargetBlock().getY(),
                           interactionManager.getTargetBlock().getZ());
            } else {
                glUniform3f(targetBlockPosLocation, -999, -999, -999); // 无效位置
            }
            
            // 目标面
            int targetFaceLocation = glGetUniformLocation(shaderProgram, "targetFace");
            glUniform1i(targetFaceLocation, interactionManager.getTargetFace());
        }
    }
    
    /**
     * 设置光照参数
     */
    private void setupLighting() {
        // 设置固定方向光 (从上方 slightly 偏斜的光照)
        int lightDirectionLocation = glGetUniformLocation(shaderProgram, "lightDirection");
        glUniform3f(lightDirectionLocation, -0.5f, -1.0f, -0.5f);  // 从上方偏斜的光照方向
        
        // 设置环境光强度
        int ambientLightLocation = glGetUniformLocation(shaderProgram, "ambientLight");
        glUniform1f(ambientLightLocation, 0.3f);  // 30% 环境光
        
        // 设置环境光遮蔽强度
        int aoStrengthLocation = glGetUniformLocation(shaderProgram, "aoStrength");
        glUniform1f(aoStrengthLocation, 0.5f);  // 50% AO 强度 - 减少对比度，创建更平滑的圆形阴影
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        cleanupRenderer();
        cleanupUIRenderer();
        cleanupShaderManager();
        cleanupTextures();
    }
    
    /**
     * 清理渲染器
     */
    private void cleanupRenderer() {
        // 清理简化渲染器
        if (simpleRenderer != null) simpleRenderer.cleanup();
    }
    
    /**
     * 清理UI渲染器
     */
    private void cleanupUIRenderer() {
        // 清理UI渲染器
        if (uiRenderer != null) uiRenderer.cleanup();
    }
    
    /**
     * 清理着色器管理器
     */
    private void cleanupShaderManager() {
        // 清理着色器管理器
        if (shaderManager != null) shaderManager.cleanup();
    }
    
    /**
     * 清理纹理
     */
    private void cleanupTextures() {
        // 删除渲染资源
        if (textureId != 0) {
            org.lwjgl.opengl.GL11.glDeleteTextures(textureId);
        }
    }
    
    /**
     * 重新构建网格
     */
    public void rebuildMesh(World world) {
        simpleRenderer.buildMeshFromWorld(world);
    }
    
    // Getter方法
    public int getTextureId() { return textureId; }
    public int getShaderProgram() { return shaderProgram; }
    public SimpleRenderer getSimpleRenderer() { return simpleRenderer; }
    public ShaderManager getShaderManager() { return shaderManager; }
    public UIRenderer getUiRenderer() { return uiRenderer; }
}