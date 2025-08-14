package com.tsian;

import java.util.List;

/**
 * 区块网格 - 存储预生成的顶点数据，类似MC的区块渲染系统
 */
public class ChunkMesh {
    
    private final int chunkX;
    private final int chunkZ;
    private final List<Face> faces;
    private final int faceCount;
    
    public ChunkMesh(int chunkX, int chunkZ, List<Face> faces) {
        this.chunkX = chunkX;
        this.chunkZ = chunkZ;
        this.faces = faces;
        this.faceCount = faces.size();
    }
    
    public int getChunkX() {
        return chunkX;
    }
    
    public int getChunkZ() {
        return chunkZ;
    }
    
    public List<Face> getFaces() {
        return faces;
    }
    
    public int getFaceCount() {
        return faceCount;
    }
    
    /**
     * 面数据结构 - 包含方块和面信息
     */
    public static class Face {
        public final Block block;
        public final int faceIndex;
        
        public Face(Block block, int faceIndex) {
            this.block = block;
            this.faceIndex = faceIndex;
        }
    }
}