package com.tsian;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 世界类 - 基于原版MC的区块管理和渲染系统
 */
public class World {
    
    // 距离配置
    private static final int SIMULATION_DISTANCE = 12;  // 区块模拟距离
    private static final int RENDER_DISTANCE = 8;       // 渲染距离
    
    private Map<String, Chunk> loadedChunks;
    private Map<String, ChunkMesh> chunkMeshes;
    private float lastCameraX = Float.MAX_VALUE;
    private float lastCameraZ = Float.MAX_VALUE;
    
    public World() {
        this.loadedChunks = new HashMap<>();
        this.chunkMeshes = new HashMap<>();
        System.out.println("Initialized world with MC-style rendering");
        System.out.println("Simulation distance: " + SIMULATION_DISTANCE + " chunks");
        System.out.println("Render distance: " + RENDER_DISTANCE + " chunks");
    }
    
    /**
     * 更新世界 - 管理区块加载和网格生成
     */
    public void update(float cameraX, float cameraZ) {
        // 检查是否需要更新
        float dx = cameraX - lastCameraX;
        float dz = cameraZ - lastCameraZ;
        float distanceSquared = dx * dx + dz * dz;
        
        if (distanceSquared < 16.0f) { // 4格距离的平方
            return;
        }
        
        lastCameraX = cameraX;
        lastCameraZ = cameraZ;
        
        int cameraChunkX = Chunk.worldToChunk((int) Math.floor(cameraX));
        int cameraChunkZ = Chunk.worldToChunk((int) Math.floor(cameraZ));
        
        // 加载模拟距离内的区块
        loadChunksInSimulationDistance(cameraChunkX, cameraChunkZ);
        
        // 卸载远距离区块
        unloadDistantChunks(cameraChunkX, cameraChunkZ);
        
        // 生成缺失的区块网格
        generateMissingChunkMeshes();
    }
    
    /**
     * 加载模拟距离内的区块
     */
    private void loadChunksInSimulationDistance(int cameraChunkX, int cameraChunkZ) {
        int loaded = 0;
        
        for (int dx = -SIMULATION_DISTANCE; dx <= SIMULATION_DISTANCE; dx++) {
            for (int dz = -SIMULATION_DISTANCE; dz <= SIMULATION_DISTANCE; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > SIMULATION_DISTANCE) continue;
                
                int chunkX = cameraChunkX + dx;
                int chunkZ = cameraChunkZ + dz;
                String key = getChunkKey(chunkX, chunkZ);
                
                if (!loadedChunks.containsKey(key)) {
                    Chunk chunk = new Chunk(chunkX, chunkZ);
                    chunk.generate();
                    loadedChunks.put(key, chunk);
                    loaded++;
                }
            }
        }
        
        if (loaded > 0) {
            System.out.println("Loaded " + loaded + " chunks");
        }
    }
    
    /**
     * 卸载远距离区块
     */
    private void unloadDistantChunks(int cameraChunkX, int cameraChunkZ) {
        List<String> toRemove = new ArrayList<>();
        
        for (Map.Entry<String, Chunk> entry : loadedChunks.entrySet()) {
            Chunk chunk = entry.getValue();
            int dx = chunk.getChunkX() - cameraChunkX;
            int dz = chunk.getChunkZ() - cameraChunkZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance > SIMULATION_DISTANCE + 2) {
                toRemove.add(entry.getKey());
            }
        }
        
        for (String key : toRemove) {
            loadedChunks.remove(key);
            chunkMeshes.remove(key);
        }
        
        if (!toRemove.isEmpty()) {
            System.out.println("Unloaded " + toRemove.size() + " chunks");
        }
    }
    
    /**
     * 生成缺失的区块网格
     */
    private void generateMissingChunkMeshes() {
        int generated = 0;
        
        for (Map.Entry<String, Chunk> entry : loadedChunks.entrySet()) {
            String key = entry.getKey();
            Chunk chunk = entry.getValue();
            
            if (!chunkMeshes.containsKey(key)) {
                ChunkMesh mesh = generateChunkMesh(chunk);
                chunkMeshes.put(key, mesh);
                generated++;
            }
        }
        
        if (generated > 0) {
            System.out.println("Generated " + generated + " chunk meshes");
        }
    }
    
    /**
     * 生成区块网格 - MC风格的面剔除
     */
    private ChunkMesh generateChunkMesh(Chunk chunk) {
        List<ChunkMesh.Face> faces = new ArrayList<>();
        
        // 遍历区块中的所有方块
        for (Block block : chunk.getBlocks().values()) {
            if (!block.isSolid()) continue;
            
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();
            
            // 检查每个面是否需要渲染
            for (int face = 0; face < 6; face++) {
                if (shouldRenderFace(x, y, z, face)) {
                    faces.add(new ChunkMesh.Face(block, face));
                }
            }
        }
        
        return new ChunkMesh(chunk.getChunkX(), chunk.getChunkZ(), faces);
    }
    
    /**
     * 检查面是否需要渲染 - 相邻实心方块会遮挡面
     */
    private boolean shouldRenderFace(int x, int y, int z, int face) {
        int adjX = x, adjY = y, adjZ = z;
        
        switch (face) {
            case 0: adjZ++; break; // 前面
            case 1: adjZ--; break; // 后面
            case 2: adjX--; break; // 左面
            case 3: adjX++; break; // 右面
            case 4: adjY++; break; // 上面
            case 5: adjY--; break; // 下面
        }
        
        return !isSolidBlock(adjX, adjY, adjZ);
    }
    
    /**
     * 获取可渲染的区块网格 - MC风格的渲染剔除
     */
    public List<ChunkMesh> getRenderableChunkMeshes(float cameraX, float cameraZ) {
        List<ChunkMesh> renderableMeshes = new ArrayList<>();
        
        int cameraChunkX = Chunk.worldToChunk((int) Math.floor(cameraX));
        int cameraChunkZ = Chunk.worldToChunk((int) Math.floor(cameraZ));
        
        for (ChunkMesh mesh : chunkMeshes.values()) {
            // 距离剔除
            int dx = mesh.getChunkX() - cameraChunkX;
            int dz = mesh.getChunkZ() - cameraChunkZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            
            if (distance <= RENDER_DISTANCE) {
                renderableMeshes.add(mesh);
            }
        }
        
        System.out.println("Rendering " + renderableMeshes.size() + " chunk meshes");
        return renderableMeshes;
    }
    
    /**
     * 检查指定位置是否有实心方块
     */
    public boolean isSolidBlock(int worldX, int worldY, int worldZ) {
        int chunkX = Chunk.worldToChunk(worldX);
        int chunkZ = Chunk.worldToChunk(worldZ);
        
        Chunk chunk = loadedChunks.get(getChunkKey(chunkX, chunkZ));
        if (chunk == null) return false;
        
        int localX = Chunk.worldToLocal(worldX);
        int localZ = Chunk.worldToLocal(worldZ);
        
        Block block = chunk.getBlock(localX, worldY, localZ);
        return block != null && block.isSolid();
    }
    
    /**
     * 获取区块键值
     */
    private String getChunkKey(int chunkX, int chunkZ) {
        return chunkX + "," + chunkZ;
    }
    
    /**
     * 强制重新生成所有区块网格
     */
    public void regenerateAllMeshes() {
        chunkMeshes.clear();
        System.out.println("Cleared all chunk meshes for regeneration");
    }
    
    /**
     * 获取加载的区块数量
     */
    public int getLoadedChunkCount() {
        return loadedChunks.size();
    }
    
    /**
     * 获取生成的网格数量
     */
    public int getChunkMeshCount() {
        return chunkMeshes.size();
    }
}