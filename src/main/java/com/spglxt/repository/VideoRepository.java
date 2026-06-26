package com.spglxt.repository;

import com.spglxt.entity.Video;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 视频数据访问层
 * 
 * @author spglxt
 */
@Repository
public interface VideoRepository extends JpaRepository<Video, Integer>, JpaSpecificationExecutor<Video> {

    /**
     * 根据分类ID查询视频列表
     *
     * @param typeId   分类ID
     * @param pageable 分页参数
     * @return 视频分页列表
     */
    Page<Video> findByTypeId(Integer typeId, Pageable pageable);

    /**
     * 根据状态查询视频列表
     *
     * @param vodStatus 状态 (0未审核 1已审核)
     * @param pageable  分页参数
     * @return 视频分页列表
     */
    Page<Video> findByVodStatus(Integer vodStatus, Pageable pageable);

    /**
     * 根据视频名称模糊查询
     *
     * @param vodName  视频名称
     * @param pageable 分页参数
     * @return 视频分页列表
     */
    Page<Video> findByVodNameContaining(String vodName, Pageable pageable);

    /**
     * 根据视频名称精确查询
     *
     * @param vodName 视频名称
     * @return 视频列表
     */
    List<Video> findByVodName(String vodName);

    /**
     * 查找去除空格后名称相同的视频
     *
     * @param vodName 视频名称(已去除空格)
     * @return 视频列表
     */
    @Query("SELECT v FROM Video v WHERE REPLACE(v.vodName, ' ', '') = :vodName")
    List<Video> findByVodNameWithoutSpaces(@Param("vodName") String vodName);

    /**
     * 统计今日新增视频数量
     *
     * @param todayStart 今日开始时间戳
     * @return 数量
     */
    @Query("SELECT COUNT(v) FROM Video v WHERE v.vodTimeAdd >= :todayStart")
    Long countTodayVideos(@Param("todayStart") Integer todayStart);

    /**
     * 统计待审核视频数量
     *
     * @return 数量
     */
    Long countByVodStatus(Integer vodStatus);

    /**
     * 批量更新视频状态
     *
     * @param ids       视频ID列表
     * @param vodStatus 状态
     * @return 影响行数
     */
    @Modifying
    @Query("UPDATE Video v SET v.vodStatus = :vodStatus WHERE v.vodId IN :ids")
    int batchUpdateStatus(@Param("ids") List<Integer> ids, @Param("vodStatus") Integer vodStatus);

    /**
     * 批量删除视频
     *
     * @param ids 视频ID列表
     * @return 影响行数
     */
    @Modifying
    @Query("DELETE FROM Video v WHERE v.vodId IN :ids")
    int batchDeleteByIds(@Param("ids") List<Integer> ids);

    /**
     * 根据年份查询视频列表
     *
     * @param vodYear  年份
     * @param pageable 分页参数
     * @return 视频分页列表
     */
    Page<Video> findByVodYear(String vodYear, Pageable pageable);

    /**
     * 根据地区查询视频列表
     *
     * @param vodArea  地区
     * @param pageable 分页参数
     * @return 视频分页列表
     */
    Page<Video> findByVodArea(String vodArea, Pageable pageable);

    /**
     * 查找重复视频(名称相同的视频至少2条)
     *
     * @return 重复视频分组列表
     */
    @Query(value = "SELECT v.vod_name as vodName, COUNT(*) as count, " +
            "GROUP_CONCAT(v.vod_id ORDER BY v.vod_id) as vodIds, " +
            "GROUP_CONCAT(v.vod_remarks ORDER BY v.vod_id SEPARATOR '|||') as vodRemarks, " +
            "GROUP_CONCAT(v.vod_play_from ORDER BY v.vod_id SEPARATOR '|||') as vodPlayFroms " +
            "FROM mac_vod v " +
            "GROUP BY v.vod_name " +
            "HAVING COUNT(*) > 1 " +
            "ORDER BY COUNT(*) DESC " +
            "LIMIT 50", nativeQuery = true)
    List<Map<String, Object>> findDuplicateVideos();
}
