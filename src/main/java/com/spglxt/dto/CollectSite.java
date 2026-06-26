package com.spglxt.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 采集站点DTO (使用JSON文件存储,不映射数据库)
 * 
 * @author spglxt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectSite implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 站点ID
     */
    private Integer id;

    /**
     * 站点名称
     */
    private String name;

    /**
     * API地址
     */
    private String url;

    /**
     * 类型 (1=JSON 2=XML)
     */
    private Integer type;

    /**
     * 附加参数
     */
    private String param;

    /**
     * 采集模式 (0=全部 1=仅新增 2=仅更新)
     */
    private String collectOpt;

    /**
     * 过滤模式 (0=不过滤 1=新增+更新都过滤 2=仅新增时过滤 3=仅更新时过滤)
     */
    private String collectFilter;

    /**
     * 要过滤的播放来源 (逗号分隔)
     */
    private String filterFrom;

    /**
     * 跳过相同集数 (1=是 0=否)
     */
    private Integer skipSameTotal;

    /**
     * 更新规则 (a=播放地址,d=下载地址,逗号分隔)
     */
    private String uprule;

    /**
     * 状态 (0=禁用 1=启用)
     */
    private Integer status;

    /**
     * 分类绑定配置 (资源站分类ID -> 本地分类ID的映射)
     * 例如: {"24": 21, "25": 24}
     * 如果值为0,表示不采集该分类
     * 如果资源站分类ID不在此映射中,则跳过该分类的视频
     */
    private Map<String, Integer> bindConfig;

    /**
     * 定时采集配置
     */
    private CronConfig cronConfig;

    /**
     * 创建时间 (Unix时间戳)
     */
    private Long createdAt;

    /**
     * 更新时间 (Unix时间戳)
     */
    private Long updatedAt;
    
    /**
     * 获取分类绑定配置,如果为null则返回空Map
     */
    public Map<String, Integer> getBindConfig() {
        if (bindConfig == null) {
            bindConfig = new HashMap<>();
        }
        return bindConfig;
    }
    
    /**
     * 获取定时采集配置,如果为null则返回默认配置
     */
    public CronConfig getCronConfig() {
        if (cronConfig == null) {
            cronConfig = new CronConfig();
        }
        return cronConfig;
    }
}
