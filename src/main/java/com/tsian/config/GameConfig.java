package com.tsian.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * 游戏配置类 - 管理所有游戏常量参数
 */
public class GameConfig {
    // 玩家相关配置
    @SerializedName("player")
    public PlayerConfig player = new PlayerConfig();
    
    // 摄像头相关配置
    @SerializedName("camera")
    public CameraConfig camera = new CameraConfig();
    
    // 世界相关配置
    @SerializedName("world")
    public WorldConfig world = new WorldConfig();
    
    // 输入相关配置
    @SerializedName("input")
    public InputConfig input = new InputConfig();
    
    // 渲染相关配置
    @SerializedName("render")
    public RenderConfig render = new RenderConfig();
    
    // 窗口相关配置
    @SerializedName("window")
    public WindowConfig window = new WindowConfig();
    
    // 方块交互相关配置
    @SerializedName("block_interaction")
    public BlockInteractionConfig blockInteraction = new BlockInteractionConfig();
    
    /**
     * 玩家配置
     */
    public static class PlayerConfig {
        @SerializedName("width")
        public float width = 10.0f / 16.0f;  // 0.625方块宽
        
        @SerializedName("depth")
        public float depth = 10.0f / 16.0f;  // 0.625方块深
        
        @SerializedName("height")
        public float height = 29.0f / 16.0f; // 1.8125方块高
        
        @SerializedName("camera_height")
        public float cameraHeight = 26.0f / 16.0f; // 1.625方块高
        
        @SerializedName("gravity")
        public float gravity = -20.0f; // 重力加速度
        
        @SerializedName("terminal_velocity")
        public float terminalVelocity = -50.0f; // 最大下落速度
        
        @SerializedName("jump_velocity")
        public float jumpVelocity = 8.0f; // 跳跃初速度
        
        @SerializedName("double_click_time")
        public float doubleClickTime = 0.3f; // 快速双击时间间隔（秒）
        
        @SerializedName("walk_speed")
        public float walkSpeed = 4.3f; // 行走速度
        
        @SerializedName("sprint_speed")
        public float sprintSpeed = 10.8f; // 冲刺速度
        
        @SerializedName("fly_speed")
        public float flySpeed = 10.8f; // 飞行速度
    }
    
    /**
     * 摄像头配置
     */
    public static class CameraConfig {
        @SerializedName("initial_yaw")
        public float initialYaw = -90.0f; // 初始偏航角
        
        @SerializedName("initial_pitch")
        public float initialPitch = 0.0f; // 初始俯仰角
        
        @SerializedName("mouse_sensitivity")
        public float mouseSensitivity = 0.1f; // 鼠标灵敏度
        
        @SerializedName("max_pitch")
        public float maxPitch = 89.0f; // 最大俯仰角
    }
    
    /**
     * 世界配置
     */
    public static class WorldConfig {
        @SerializedName("island_size")
        public int islandSize = 4; // 空岛大小（区块）
        
        @SerializedName("island_min_chunk")
        public int islandMinChunk = 0; // 空岛最小区块坐标
        
        @SerializedName("island_max_chunk")
        public int islandMaxChunk = 3; // 空岛最大区块坐标
        
        @SerializedName("chunk_size")
        public int chunkSize = 16; // 区块大小
    }
    
    /**
     * 输入配置
     */
    public static class InputConfig {
        @SerializedName("place_delay")
        public float placeDelay = 0.2f; // 放置方块延迟（秒）
    }
    
    /**
     * 渲染配置
     */
    public static class RenderConfig {
        @SerializedName("floats_per_vertex")
        public int floatsPerVertex = 12; // 每个顶点的浮点数数量
        
        @SerializedName("fov")
        public float fov = 45.0f; // 视野角度
        
        @SerializedName("near_plane")
        public float nearPlane = 0.1f; // 近裁剪面
        
        @SerializedName("far_plane")
        public float farPlane = 1000.0f; // 远裁剪面
        
        @SerializedName("ambient_light")
        public float ambientLight = 0.3f; // 环境光强度
        
        @SerializedName("ao_strength")
        public float aoStrength = 0.5f; // AO强度
        
        @SerializedName("selected_block_brightness")
        public float selectedBlockBrightness = 1.3f; // 选中方块亮度倍数
    }
    
    /**
     * 窗口配置
     */
    public static class WindowConfig {
        @SerializedName("default_width")
        public int defaultWidth = 1024; // 默认窗口宽度
        
        @SerializedName("default_height")
        public int defaultHeight = 768; // 默认窗口高度
        
        @SerializedName("title")
        public String title = "My Minecraft"; // 窗口标题
    }
    
    /**
     * 方块交互配置
     */
    public static class BlockInteractionConfig {
        @SerializedName("break_time")
        public float breakTime = 1.0f; // 破坏方块时间（秒）
        
        @SerializedName("max_reach_distance")
        public float maxReachDistance = 5.0f; // 最大交互距离
    }
    
    /**
     * 从JSON文件加载配置
     */
    public static GameConfig loadFromFile(String filePath) throws IOException {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(filePath)) {
            return gson.fromJson(reader, GameConfig.class);
        }
    }
    
    /**
     * 从资源文件加载默认配置
     */
    public static GameConfig loadDefault() throws IOException {
        Gson gson = new Gson();
        try (InputStreamReader reader = new InputStreamReader(
                GameConfig.class.getClassLoader().getResourceAsStream("config.json"))) {
            return gson.fromJson(reader, GameConfig.class);
        }
    }
    
    /**
     * 保存配置到JSON文件
     */
    public void saveToFile(String filePath) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try (FileWriter writer = new FileWriter(filePath)) {
            gson.toJson(this, writer);
        }
    }
}