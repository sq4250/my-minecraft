package com.tsian.render;
import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * UI渲染器 - 用于渲染2D UI元素如十字标记
 */
public class UIRenderer {
    
    private int vaoId;
    private int vboId;
    private int eboId;
    private int shaderProgram;
    
    private FloatBuffer vertexBuffer;
    private IntBuffer indexBuffer;
    
    private ShaderManager shaderManager;
    
    public UIRenderer() {
        shaderManager = new ShaderManager();
        initializeBuffers();
        createShaderProgram();
    }
    
    /**
     * 初始化VAO/VBO/EBO
     */
    private void initializeBuffers() {
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        eboId = glGenBuffers();
        
        glBindVertexArray(vaoId);
        
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        
        // 位置属性 (location = 0) - 2D坐标
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 2 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        glBindVertexArray(0);
    }
    
    /**
     * 创建UI着色器程序
     */
    private void createShaderProgram() {
        String vertexShaderSource = 
            "#version 330 core\n" +
            "layout (location = 0) in vec2 aPos;\n" +
            "void main() {\n" +
            "    gl_Position = vec4(aPos, 0.0, 1.0);\n" +
            "}\n";
            
        String fragmentShaderSource = 
            "#version 330 core\n" +
            "out vec4 FragColor;\n" +
            "uniform vec3 color;\n" +
            "void main() {\n" +
            "    FragColor = vec4(color, 1.0);\n" +
            "}\n";
            
        shaderProgram = shaderManager.createShaderProgram(vertexShaderSource, fragmentShaderSource);
    }
    
    /**
     * 渲染十字标记
     */
    public void renderCrosshair(int screenWidth, int screenHeight) {
        // 保存当前OpenGL状态
        boolean depthTestEnabled = glIsEnabled(GL_DEPTH_TEST);
        boolean cullFaceEnabled = glIsEnabled(GL_CULL_FACE);
        
        // 禁用深度测试和面剔除，确保UI在最前面
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        
        glUseProgram(shaderProgram);
        
        // 设置十字标记颜色（白色）
        int colorLocation = glGetUniformLocation(shaderProgram, "color");
        glUniform3f(colorLocation, 1.0f, 1.0f, 1.0f);
        
        // 固定像素大小的十字准心 - 不随窗口尺寸变化
        float crosshairLengthPixels = 25.0f; // 十字长度：15像素
        float crosshairWidthPixels = 2.0f;   // 十字宽度：2像素
         
        // 将像素转换为NDC坐标
        float crosshairLength = crosshairLengthPixels / (float) screenWidth * 2.0f; // 水平方向
        float crosshairHeight = crosshairLengthPixels / (float) screenHeight * 2.0f; // 垂直方向
        float lineWidth = crosshairWidthPixels / (float) screenWidth * 2.0f; // 水平线宽度
        float lineHeight = crosshairWidthPixels / (float) screenHeight * 2.0f; // 垂直线高度
        
        // 创建十字标记顶点数据（两条线：水平和垂直）
        float[] vertices = {
            // 水平线
            -crosshairLength * 0.5f, -lineHeight * 0.5f,  // 左下
             crosshairLength * 0.5f, -lineHeight * 0.5f,  // 右下
             crosshairLength * 0.5f,  lineHeight * 0.5f,  // 右上
            -crosshairLength * 0.5f,  lineHeight * 0.5f,  // 左上
            
            // 垂直线
            -lineWidth * 0.5f, -crosshairHeight * 0.5f,  // 左下
             lineWidth * 0.5f, -crosshairHeight * 0.5f,  // 右下
             lineWidth * 0.5f,  crosshairHeight * 0.5f,  // 右上
            -lineWidth * 0.5f,  crosshairHeight * 0.5f   // 左上
        };
        
        int[] indices = {
            // 水平线（两个三角形）
            0, 1, 2,
            2, 3, 0,
            
            // 垂直线（两个三角形）
            4, 5, 6,
            6, 7, 4
        };
        
        // 更新缓冲区数据
        vertexBuffer = MemoryUtil.memAllocFloat(vertices.length);
        vertexBuffer.put(vertices).flip();
        
        indexBuffer = MemoryUtil.memAllocInt(indices.length);
        indexBuffer.put(indices).flip();
        
        glBindVertexArray(vaoId);
        
        // 上传顶点数据
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
        
        // 上传索引数据
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);
        
        // 渲染
        glDrawElements(GL_TRIANGLES, indices.length, GL_UNSIGNED_INT, 0);
        
        glBindVertexArray(0);
        
        // 释放临时缓冲区
        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
        
        // 恢复OpenGL状态
        if (depthTestEnabled) glEnable(GL_DEPTH_TEST);
        if (cullFaceEnabled) glEnable(GL_CULL_FACE);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
        if (vboId != 0) glDeleteBuffers(vboId);
        if (eboId != 0) glDeleteBuffers(eboId);
        
        if (shaderManager != null) shaderManager.cleanup();
        if (shaderProgram != 0) glDeleteProgram(shaderProgram);
    }
}