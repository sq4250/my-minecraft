package com.tsian;

import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * 批量渲染系统 - 将相同纹理的方块合并渲染
 */
public class RenderBatch {
    
    private static final int MAX_BATCH_SIZE = 1000; // 每批最大方块数
    private static final int VERTICES_PER_FACE = 4;
    private static final int FACES_PER_BLOCK = 6;
    private static final int FLOATS_PER_VERTEX = 5; // x,y,z,u,v
    
    // 按纹理分组的批次
    private Map<String, Batch> batches;
    
    // 全局VAO和缓冲区
    private int vaoId;
    private int vboId;
    private int eboId;
    
    public RenderBatch() {
        this.batches = new HashMap<>();
        
        // 创建VAO和缓冲区
        vaoId = glGenVertexArrays();
        vboId = glGenBuffers();
        eboId = glGenBuffers();
        
        setupBuffers();
    }
    
    /**
     * 添加方块面到批次
     */
    public void addFace(Block block, int face, float[] vertices, int[] indices) {
        String materialKey = getMaterialKey(block);
        Batch batch = batches.computeIfAbsent(materialKey, k -> new Batch());
        batch.addFace(vertices, indices);
    }
    
    /**
     * 清空所有批次
     */
    public void clear() {
        for (Batch batch : batches.values()) {
            batch.clear();
        }
    }
    
    /**
     * 渲染所有批次
     */
    public void render(int shaderProgram, int textureId) {
        if (batches.isEmpty()) return;
        
        glBindVertexArray(vaoId);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // 合并所有批次到单一缓冲区
        List<Float> allVertices = new ArrayList<>();
        List<Integer> allIndices = new ArrayList<>();
        int globalVertexOffset = 0;
        
        for (Batch batch : batches.values()) {
            if (batch.hasData()) {
                // 添加顶点数据
                allVertices.addAll(batch.vertices);
                
                // 添加索引数据（批次内部已经处理了局部偏移，这里只需要全局偏移）
                for (Integer index : batch.indices) {
                    allIndices.add(index + globalVertexOffset);
                }
                
                globalVertexOffset += batch.vertices.size() / FLOATS_PER_VERTEX;
            }
        }
        
        if (!allVertices.isEmpty() && !allIndices.isEmpty()) {
            uploadCombinedData(allVertices, allIndices);
            
            int indexCount = allIndices.size();
            glDrawElements(GL_TRIANGLES, indexCount, GL_UNSIGNED_INT, 0);
            
            // 静默渲染，避免每帧输出日志
        }
        
        glBindVertexArray(0);
    }
    
    /**
     * 上传合并的数据到GPU
     */
    private void uploadCombinedData(List<Float> vertices, List<Integer> indices) {
        // 创建顶点缓冲区
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(vertices.size());
        for (Float v : vertices) {
            vertexBuffer.put(v);
        }
        vertexBuffer.flip();
        
        // 创建索引缓冲区
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(indices.size());
        for (Integer i : indices) {
            indexBuffer.put(i);
        }
        indexBuffer.flip();
        
        // 上传到GPU
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_DYNAMIC_DRAW);
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_DYNAMIC_DRAW);
        
        // 释放内存
        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }
    
    /**
     * 获取材质键（用于分组）
     */
    private String getMaterialKey(Block block) {
        return block.getType().name();
    }
    
    /**
     * 设置VAO和属性
     */
    private void setupBuffers() {
        glBindVertexArray(vaoId);
        
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        
        // 位置属性 (location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // 纹理坐标属性 (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
    }
    
    /**
     * 清理资源
     */
    public void cleanup() {
        if (vaoId != 0) glDeleteVertexArrays(vaoId);
        if (vboId != 0) glDeleteBuffers(vboId);
        if (eboId != 0) glDeleteBuffers(eboId);
        
        for (Batch batch : batches.values()) {
            batch.cleanup();
        }
    }
    
    /**
     * 单个批次类
     */
    private class Batch {
        private List<Float> vertices;
        private List<Integer> indices;
        private int currentVertexOffset;
        private boolean needsUpdate;
        
        public Batch() {
            this.vertices = new ArrayList<>();
            this.indices = new ArrayList<>();
            this.currentVertexOffset = 0;
            this.needsUpdate = false;
        }
        
        public void addFace(float[] faceVertices, int[] faceIndices) {
            // 添加顶点数据
            for (float vertex : faceVertices) {
                vertices.add(vertex);
            }
            
            // 添加索引数据（在批次内部调整偏移）
            for (int index : faceIndices) {
                indices.add(index + currentVertexOffset);
            }
            
            currentVertexOffset += faceVertices.length / FLOATS_PER_VERTEX;
            needsUpdate = true;
        }
        
        public void clear() {
            vertices.clear();
            indices.clear();
            currentVertexOffset = 0;
            needsUpdate = true;
        }
        
        public boolean hasData() {
            return !vertices.isEmpty() && !indices.isEmpty();
        }
        
        public void cleanup() {
            vertices.clear();
            indices.clear();
        }
    }
    
    /**
     * 获取批次统计信息
     */
    public BatchStats getStats() {
        int totalBatches = 0;
        int totalVertices = 0;
        int totalIndices = 0;
        
        for (Batch batch : batches.values()) {
            if (batch.hasData()) {
                totalBatches++;
                totalVertices += batch.vertices.size() / FLOATS_PER_VERTEX;
                totalIndices += batch.indices.size();
            }
        }
        
        return new BatchStats(totalBatches, totalVertices, totalIndices / 3);
    }
    
    /**
     * 批次统计信息
     */
    public static class BatchStats {
        public final int batchCount;
        public final int vertexCount;
        public final int triangleCount;
        
        public BatchStats(int batchCount, int vertexCount, int triangleCount) {
            this.batchCount = batchCount;
            this.vertexCount = vertexCount;
            this.triangleCount = triangleCount;
        }
        
        @Override
        public String toString() {
            return String.format("Batches: %d, Vertices: %d, Triangles: %d", 
                               batchCount, vertexCount, triangleCount);
        }
    }
}