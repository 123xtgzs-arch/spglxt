package com.spglxt.dto;

import lombok.Data;

/**
 * 定时采集配置
 */
@Data
public class CronConfig {
    /**
     * 是否启用（0=禁用, 1=启用）
     */
    private Integer enabled = 0;
    
    /**
     * 调度模式（interval=间隔模式, cron=定时模式）
     */
    private String mode = "interval";
    
    /**
     * 执行间隔（小时）
     */
    private Integer interval = 6;
    
    /**
     * 定时执行时间点（格式：HH:MM,HH:MM 或简写 H,H）
     */
    private String cronHours = "0,12";
    
    /**
     * 采集范围：N小时内更新的视频（空=不限）
     */
    private String hours = "24";
    
    /**
     * 指定分类ID（空=全部）
     */
    private String typeId = "";
    
    /**
     * 上次执行时间戳
     */
    private Long lastRun = 0L;
}
