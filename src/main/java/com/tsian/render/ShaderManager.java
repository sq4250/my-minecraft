package com.tsian.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.BufferUtils.createFloatBuffer;

/**
 * 着色器管理器 - 负责着色器的创建、编译、链接和管理
 */
public class ShaderManager {
    
    private int shaderProgram;
    
    /**
     * 创建默认的方块渲染着色器程序
     */
    public int createBlockShaderProgram() {
        String vertexShaderSource = loadShaderSource("shaders/block.vert");
        String fragmentShaderSource = loadShaderSource("shaders/block.frag");
        
        shaderProgram = createShaderProgram(vertexShaderSource, fragmentShaderSource);
        return shaderProgram;
    }
    
    /**
     * 从资源文件加载着色器源码
     */
    private String loadShaderSource(String resourcePath) {
        try {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
            if (inputStream == null) {
                throw new RuntimeException("Shader file not found: " + resourcePath);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            StringBuilder source = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                source.append(line).append('\n');
            }
            
            reader.close();
            return source.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + resourcePath, e);
        }
    }
    
    /**
     * 创建着色器程序
     */
    public int createShaderProgram(String vertexShaderSource, String fragmentShaderSource) {
        // 编译顶点着色器
        int vertexShader = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexShader, vertexShaderSource);
        glCompileShader(vertexShader);
        checkShaderCompilation(vertexShader, "VERTEX");
        
        // 编译片段着色器
        int fragmentShader = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentShader, fragmentShaderSource);
        glCompileShader(fragmentShader);
        checkShaderCompilation(fragmentShader, "FRAGMENT");
        
        // 创建着色器程序
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        checkProgramLinking(program);
        
        // 删除着色器对象
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        
        return program;
    }
    
    /**
     * 检查着色器编译状态
     */
    private void checkShaderCompilation(int shader, String type) {
        int success = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (success == 0) {
            String infoLog = glGetShaderInfoLog(shader);
            throw new RuntimeException("Shader compilation failed (" + type + "): " + infoLog);
        }
    }
    
    /**
     * 检查程序链接状态
     */
    private void checkProgramLinking(int program) {
        int success = glGetProgrami(program, GL_LINK_STATUS);
        if (success == 0) {
            String infoLog = glGetProgramInfoLog(program);
            throw new RuntimeException("Program linking failed: " + infoLog);
        }
    }
    
    
    /**
     * 上传4x4矩阵uniform
     */
    public void uploadMatrix4f(int program, String varName, float[] matrix) {
        int varLocation = glGetUniformLocation(program, varName);
        FloatBuffer matrixBuffer = createFloatBuffer(16);
        matrixBuffer.put(matrix).flip();
        glUniformMatrix4fv(varLocation, false, matrixBuffer);
    }
    
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (shaderProgram != 0) {
            glDeleteProgram(shaderProgram);
            shaderProgram = 0;
        }
    }
}