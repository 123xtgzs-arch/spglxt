package com.spglxt.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 采集任务DTO
 * 
 * @author spglxt
 */
@Data
public class CollectTask {
    /**
     * 任务ID
     */
    private String id;
    
    /**
     * 站点ID
     */
    @JsonProperty("site_id")
    private Integer siteId;
    
    /**
     * 站点名称
     */
    @JsonProperty("site_name")
    private String siteName;
    
    /**
     * 任务状态: pending(等待), running(运行中), completed(已完成), stopped(已停止), failed(失败)
     */
    private String status;
    
    /**
     * 当前页
     */
    @JsonProperty("current_page")
    private Integer currentPage;
    
    /**
     * 总页数
     */
    @JsonProperty("total_pages")
    private Integer totalPages;
    
    /**
     * 进度文本
     */
    private String progress;
    
    /**
     * 新增数量
     */
    @JsonProperty("add_count")
    private Integer addCount;
    
    /**
     * 更新数量
     */
    @JsonProperty("update_count")
    private Integer updateCount;
    
    /**
     * 开始时间
     */
    @JsonProperty("start_time")
    private String startTime;
    
    /**
     * 结束时间
     */
    @JsonProperty("end_time")
    private String endTime;
    
    /**
     * 执行日志
     */
    private List<String> logs;
    
    /**
     * 是否自动执行(用于标识是否需要前端自动执行)
     */
    @JsonProperty("auto_execute")
    private Boolean autoExecute;

    /**
     * 采集范围：N小时内更新的视频
     */
    private String hours;

    /**
     * 指定资源站分类ID
     */
    @JsonProperty("type_id")
    private String typeId;

    /**
     * 最大采集页数（0=不限）
     */
    @JsonProperty("max_pages")
    private Integer maxPages;

    /**
     * 任务来源：manual / cron / scheduler
     */
    private String source;
    
    /**
     * 创建时间戳
     */
    @JsonProperty("created_at")
    private Long createdAt;
    
    public CollectTask() {
        this.logs = new ArrayList<>();
        this.addCount = 0;
        this.updateCount = 0;
        this.currentPage = 0;
        this.totalPages = 0;
        this.autoExecute = false;
    }
    
    /**
     * 添加日志
     */
    public void addLog(String log) {
        if (this.logs == null) {
            this.logs = new ArrayList<>();
        }
        this.logs.add(log);
    }
}
