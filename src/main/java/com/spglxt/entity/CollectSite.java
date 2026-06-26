package com.spglxt.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 采集站点实体类 (JSON文件存储)
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
     * 站点类型 (1=JSON格式 2=XML格式)
     */
    private Integer type;

    /**
     * 附加参数
     */
    private String param;

    /**
     * 采集方式 (0=新增+更新 1=仅新增 2=仅更新)
     */
    private String collectOpt;

    /**
     * 过滤方式 (0=不过滤 1=新增+更新 2=仅新增 3=仅更新)
     */
    private String collectFilter;

    /**
     * 过滤的播放源 (逗号分隔)
     */
    private String filterFrom;

    /**
     * 跳过相同集数 (0=否 1=是)
     */
    private Integer skipSameTotal;

    /**
     * 更新规则 (a=播放地址,d=详情,逗号分隔)
     */
    private String uprule;

    /**
     * 状态 (0=禁用 1=启用)
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Integer createdAt;

    /**
     * 更新时间
     */
    private Integer updatedAt;
}
