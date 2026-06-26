package com.spglxt.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 视频实体类 - 对应苹果CMS mac_vod表
 * 
 * @author spglxt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mac_vod")
public class Video implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 视频ID (主键)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vod_id")
    private Integer vodId;

    /**
     * 分类ID
     */
    @Column(name = "type_id")
    private Integer typeId;

    /**
     * 一级分类ID
     */
    @Column(name = "type_id_1")
    private Integer typeId1;

    /**
     * 视频名称
     */
    @Column(name = "vod_name", length = 255)
    private String vodName;

    /**
     * 副标题
     */
    @Column(name = "vod_sub", length = 255)
    private String vodSub;

    /**
     * 英文名
     */
    @Column(name = "vod_en", length = 255)
    private String vodEn;

    /**
     * 状态 (0未审核 1已审核)
     */
    @Column(name = "vod_status")
    private Integer vodStatus;

    /**
     * 封面图片
     */
    @Column(name = "vod_pic", length = 1024)
    private String vodPic;

    /**
     * 演员
     */
    @Column(name = "vod_actor", length = 255)
    private String vodActor;

    /**
     * 导演
     */
    @Column(name = "vod_director", length = 255)
    private String vodDirector;

    /**
     * 编剧
     */
    @Column(name = "vod_writer", length = 255)
    private String vodWriter;

    /**
     * 简介
     */
    @Column(name = "vod_blurb", length = 255)
    private String vodBlurb;

    /**
     * 备注 (如:更新至第X集)
     */
    @Column(name = "vod_remarks", length = 100)
    private String vodRemarks;

    /**
     * 地区
     */
    @Column(name = "vod_area", length = 20)
    private String vodArea;

    /**
     * 语言
     */
    @Column(name = "vod_lang", length = 20)
    private String vodLang;

    /**
     * 年份
     */
    @Column(name = "vod_year", length = 10)
    private String vodYear;

    /**
     * 评分
     */
    @Column(name = "vod_score", length = 5)
    private String vodScore;

    /**
     * 总点击量
     */
    @Column(name = "vod_hits")
    private Integer vodHits;

    /**
     * 日点击量
     */
    @Column(name = "vod_hits_day")
    private Integer vodHitsDay;

    /**
     * 周点击量
     */
    @Column(name = "vod_hits_week")
    private Integer vodHitsWeek;

    /**
     * 月点击量
     */
    @Column(name = "vod_hits_month")
    private Integer vodHitsMonth;

    /**
     * 更新时间 (Unix时间戳)
     */
    @Column(name = "vod_time")
    private Integer vodTime;

    /**
     * 添加时间 (Unix时间戳)
     */
    @Column(name = "vod_time_add")
    private Integer vodTimeAdd;

    /**
     * 播放来源 (逗号分隔)
     */
    @Column(name = "vod_play_from", columnDefinition = "TEXT")
    private String vodPlayFrom;

    /**
     * 播放地址
     */
    @Column(name = "vod_play_url", columnDefinition = "TEXT")
    private String vodPlayUrl;

    /**
     * 详细内容
     */
    @Column(name = "vod_content", columnDefinition = "TEXT")
    private String vodContent;

    /**
     * 下载来源 (逗号分隔)
     */
    @Column(name = "vod_down_from", columnDefinition = "TEXT")
    private String vodDownFrom;

    /**
     * 下载地址
     */
    @Column(name = "vod_down_url", columnDefinition = "TEXT")
    private String vodDownUrl;

    /**
     * 标签 (逗号分隔)
     */
    @Column(name = "vod_tag", length = 255)
    private String vodTag;

    /**
     * 扩展分类 (逗号分隔)
     */
    @Column(name = "vod_class", length = 255)
    private String vodClass;

    /**
     * 总集数
     */
    @Column(name = "vod_total")
    private Integer vodTotal;

    /**
     * 连载数
     */
    @Column(name = "vod_serial", length = 20)
    private String vodSerial;

    /**
     * 完结状态 (0未完结 1已完结)
     */
    @Column(name = "vod_isend")
    private Integer vodIsend;

    /**
     * 剧情名称/选集列表
     */
    @Column(name = "vod_plot_name", columnDefinition = "TEXT")
    private String vodPlotName;

    /**
     * 剧情详情
     */
    @Column(name = "vod_plot_detail", columnDefinition = "TEXT")
    private String vodPlotDetail;

    /**
     * 分类名称 (非数据库字段,用于关联查询)
     */
    @Transient
    private String typeName;
}
