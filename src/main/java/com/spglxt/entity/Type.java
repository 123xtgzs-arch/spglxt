package com.spglxt.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 分类实体类 - 对应苹果CMS mac_type表
 * 
 * @author spglxt
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "mac_type")
public class Type implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分类ID (主键)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Integer typeId;

    /**
     * 分类名称
     */
    @Column(name = "type_name", length = 60)
    private String typeName;

    /**
     * 英文名称
     */
    @Column(name = "type_en", length = 60)
    private String typeEn;

    /**
     * 排序号
     */
    @Column(name = "type_sort")
    private Integer typeSort;

    /**
     * 模型ID (1=视频)
     */
    @Column(name = "type_mid")
    private Integer typeMid;

    /**
     * 上级分类ID (0为顶级分类)
     */
    @Column(name = "type_pid")
    private Integer typePid;

    /**
     * 状态 (0隐藏 1显示)
     */
    @Column(name = "type_status")
    private Integer typeStatus;

    /**
     * 子分类列表 (非数据库字段,用于树形结构)
     */
    @Transient
    private List<Type> children = new ArrayList<>();
}
