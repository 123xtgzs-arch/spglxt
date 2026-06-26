package com.spglxt.repository;

import com.spglxt.entity.Type;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 分类数据访问层
 * 
 * @author spglxt
 */
@Repository
public interface TypeRepository extends JpaRepository<Type, Integer> {

    /**
     * 根据模型ID查询分类列表 (按排序号排序)
     *
     * @param typeMid 模型ID (1=视频)
     * @return 分类列表
     */
    List<Type> findByTypeMidOrderByTypeSortAscTypeIdAsc(Integer typeMid);

    /**
     * 根据父级ID查询子分类
     *
     * @param typePid 父级ID
     * @return 子分类列表
     */
    List<Type> findByTypePidOrderByTypeSortAscTypeIdAsc(Integer typePid);

    /**
     * 根据分类名称查询
     *
     * @param typeName 分类名称
     * @return 分类对象
     */
    Type findByTypeName(String typeName);

    /**
     * 统计某分类下的视频数量
     *
     * @param typeId 分类ID
     * @return 视频数量
     */
    @Query("SELECT COUNT(v) FROM Video v WHERE v.typeId = :typeId")
    Long countVideosByTypeId(@Param("typeId") Integer typeId);
}
