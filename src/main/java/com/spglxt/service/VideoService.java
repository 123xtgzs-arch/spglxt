package com.spglxt.service;

import com.spglxt.entity.Video;
import com.spglxt.repository.VideoRepository;
import com.spglxt.util.VideoNameUtil;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;

/**
 * 视频服务层
 * 
 * @author spglxt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepository videoRepository;

    /**
     * 分页查询视频列表 (支持多条件筛选)
     *
     * @param page      页码
     * @param pageSize  每页数量
     * @param filters   筛选条件
     * @return 视频分页结果
     */
    public Page<Video> getVideoList(int page, int pageSize, Map<String, Object> filters) {
        // 构建动态查询条件
        Specification<Video> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 关键词搜索 (名称、演员、导演、播放地址)
            if (filters.containsKey("keyword") && StringUtils.hasText((String) filters.get("keyword"))) {
                String keyword = "%" + filters.get("keyword") + "%";
                Predicate p1 = cb.like(root.get("vodName"), keyword);
                Predicate p2 = cb.like(root.get("vodActor"), keyword);
                Predicate p3 = cb.like(root.get("vodDirector"), keyword);
                Predicate p4 = cb.like(root.get("vodPlayUrl"), keyword);
                predicates.add(cb.or(p1, p2, p3, p4));
            }

            // 按分类筛选
            if (filters.containsKey("typeId") && filters.get("typeId") != null) {
                predicates.add(cb.equal(root.get("typeId"), filters.get("typeId")));
            }

            // 按状态筛选
            if (filters.containsKey("status") && filters.get("status") != null) {
                predicates.add(cb.equal(root.get("vodStatus"), filters.get("status")));
            }

            // 按年份筛选
            if (filters.containsKey("year") && StringUtils.hasText((String) filters.get("year"))) {
                predicates.add(cb.equal(root.get("vodYear"), filters.get("year")));
            }

            // 按地区筛选
            if (filters.containsKey("area") && StringUtils.hasText((String) filters.get("area"))) {
                predicates.add(cb.equal(root.get("vodArea"), filters.get("area")));
            }

            // 按播放地址筛选
            if (filters.containsKey("playUrl") && StringUtils.hasText((String) filters.get("playUrl"))) {
                String playUrl = "%" + filters.get("playUrl") + "%";
                predicates.add(cb.like(root.get("vodPlayUrl"), playUrl));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        // 排序规则
        Sort sort = getSort(filters);
        Pageable pageable = PageRequest.of(page - 1, pageSize, sort);

        return videoRepository.findAll(spec, pageable);
    }

    /**
     * 根据筛选条件获取排序规则
     */
    private Sort getSort(Map<String, Object> filters) {
        String order = (String) filters.getOrDefault("order", "time");
        return switch (order) {
            case "hits" -> Sort.by(Sort.Direction.DESC, "vodHits");
            case "score" -> Sort.by(Sort.Direction.DESC, "vodScore");
            case "add_time" -> Sort.by(Sort.Direction.DESC, "vodTimeAdd");
            default -> Sort.by(Sort.Direction.DESC, "vodTime");
        };
    }

    /**
     * 根据ID查询视频详情
     *
     * @param id 视频ID
     * @return 视频详情
     */
    public Optional<Video> getVideoById(Integer id) {
        return videoRepository.findById(id);
    }

    /**
     * 根据名称查询视频
     *
     * @param name 视频名称
     * @return 视频详情
     */
    public Optional<Video> findByName(String name) {
        List<Video> videos = videoRepository.findByVodName(name);
        return videos.isEmpty() ? Optional.empty() : Optional.of(videos.get(0));
    }

    /**
     * 根据名称查询视频(去除空格匹配)
     *
     * @param name 视频名称
     * @return 视频详情
     */
    public Optional<Video> findByNameWithoutSpaces(String name) {
        String cleanedName = name.replaceAll("\\s+", "");
        List<Video> videos = videoRepository.findByVodNameWithoutSpaces(cleanedName);
        return videos.isEmpty() ? Optional.empty() : Optional.of(videos.get(0));
    }

    /**
     * 根据标准化名称查询视频(季数/罗马数字统一格式后匹配)
     *
     * @param normalizedName 标准化后的名称
     * @return 视频详情
     */
    public Optional<Video> findByNormalizedName(String normalizedName) {
        // 提取基础关键词用于初步筛选
        String baseName = normalizedName.replaceAll("[0-9]+", ""); // 去掉数字
        if (baseName.length() < 2) {
            return Optional.empty();
        }
        
        // 取前几个字符作为搜索关键词
        String keyword = baseName.substring(0, Math.min(4, baseName.length()));
        
        // 查找包含关键词的所有视频(限制50条避免性能问题)
        List<Video> candidates = videoRepository.findByVodNameContaining(keyword, 
            org.springframework.data.domain.PageRequest.of(0, 50)).getContent();
        
        // 遍历候选视频,对比标准化名称
        for (Video video : candidates) {
            String candidateName = VideoNameUtil.normalizeVideoName(video.getVodName());
            if (candidateName.equals(normalizedName)) {
                return Optional.of(video);
            }
        }
        
        return Optional.empty();
    }

    /**
     * 添加视频
     *
     * @param video 视频对象
     * @return 保存后的视频对象
     */
    @Transactional
    public Video addVideo(Video video) {
        int currentTime = (int) (System.currentTimeMillis() / 1000);
        video.setVodTime(currentTime);
        video.setVodTimeAdd(currentTime);
        
        // 如果没有设置type_id_1,使用typeId
        if (video.getTypeId1() == null && video.getTypeId() != null) {
            video.setTypeId1(video.getTypeId());
        }
        
        // 为苹果CMS数据库NOT NULL字段设置默认值
        setDefaultValues(video);
        
        return videoRepository.save(video);
    }

    /**
     * 更新视频
     *
     * @param id    视频ID
     * @param video 更新的视频信息
     * @return 更新后的视频对象
     */
    @Transactional
    public Video updateVideo(Integer id, Video video) {
        return videoRepository.findById(id).map(existingVideo -> {
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            video.setVodId(id);
            video.setVodTime(currentTime);
            video.setVodTimeAdd(existingVideo.getVodTimeAdd()); // 保留原添加时间
            
            // 如果没有设置type_id_1,使用typeId或保留原值
            if (video.getTypeId1() == null) {
                if (video.getTypeId() != null) {
                    video.setTypeId1(video.getTypeId());
                } else {
                    video.setTypeId1(existingVideo.getTypeId1());
                }
            }
            
            // 为苹果CMS数据库NOT NULL字段设置默认值
            setDefaultValues(video);
            
            return videoRepository.save(video);
        }).orElseThrow(() -> new RuntimeException("视频不存在: " + id));
    }

    /**
     * 删除视频
     *
     * @param id 视频ID
     */
    @Transactional
    public void deleteVideo(Integer id) {
        videoRepository.deleteById(id);
    }

    /**
     * 批量删除视频
     *
     * @param ids 视频ID列表
     * @return 删除的数量
     */
    @Transactional
    public int batchDeleteVideos(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return videoRepository.batchDeleteByIds(ids);
    }

    /**
     * 批量审核视频
     *
     * @param ids    视频ID列表
     * @param status 状态 (0未审核 1已审核)
     * @return 更新的数量
     */
    @Transactional
    public int batchAuditVideos(List<Integer> ids, Integer status) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return videoRepository.batchUpdateStatus(ids, status);
    }

    /**
     * 获取统计数据
     *
     * @return 统计数据Map
     */
    public Map<String, Long> getStats() {
        Map<String, Long> stats = new HashMap<>();

        // 总数
        long total = videoRepository.count();
        stats.put("total", total);

        // 今日新增 (计算今天0点的时间戳)
        LocalDate today = LocalDate.now();
        long todayStart = today.atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        long todayCount = videoRepository.countTodayVideos((int) todayStart);
        stats.put("today", todayCount);

        // 待审核
        long pending = videoRepository.countByVodStatus(0);
        stats.put("pending", pending);

        return stats;
    }

    /**
     * 查找重复视频(名称相同的视频)
     *
     * @return 重复视频列表
     */
    public List<Map<String, Object>> findDuplicateVideos() {
        List<Map<String, Object>> rawData = videoRepository.findDuplicateVideos();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> row : rawData) {
            String vodName = (String) row.get("vodName");
            Long count = ((Number) row.get("count")).longValue();
            String vodIdsStr = (String) row.get("vodIds");
            String vodRemarksStr = (String) row.get("vodRemarks");
            String vodPlayFromsStr = (String) row.get("vodPlayFroms");

            // 解析ID列表
            String[] idArr = vodIdsStr.split(",");
            String[] remarksArr = vodRemarksStr != null ? vodRemarksStr.split("\\|\\|\\|") : new String[idArr.length];
            String[] playFromsArr = vodPlayFromsStr != null ? vodPlayFromsStr.split("\\|\\|\\|") : new String[idArr.length];

            // 构建videos列表
            List<Map<String, Object>> videos = new ArrayList<>();
            for (int i = 0; i < idArr.length; i++) {
                Map<String, Object> video = new HashMap<>();
                video.put("vod_id", Integer.parseInt(idArr[i].trim()));
                video.put("vod_name", vodName);
                video.put("vod_remarks", i < remarksArr.length ? remarksArr[i] : "");
                video.put("vod_play_from", i < playFromsArr.length ? playFromsArr[i] : "");
                videos.add(video);
            }

            Map<String, Object> group = new HashMap<>();
            group.put("vod_name", vodName);
            group.put("count", count);
            group.put("videos", videos);
            result.add(group);
        }

        return result;
    }

    /**
     * 合并视频(将源视频的播放地址合并到目标视频,然后删除源视频)
     *
     * @param targetId 目标视频ID(保留)
     * @param sourceId 源视频ID(删除)
     */
    @Transactional
    public void mergeVideos(Integer targetId, Integer sourceId) {
        // 获取目标视频和源视频
        Video target = videoRepository.findById(targetId)
                .orElseThrow(() -> new RuntimeException("目标视频不存在"));
        Video source = videoRepository.findById(sourceId)
                .orElseThrow(() -> new RuntimeException("源视频不存在"));

        // 合并播放地址
        String targetFrom = target.getVodPlayFrom() != null ? target.getVodPlayFrom() : "";
        String targetUrl = target.getVodPlayUrl() != null ? target.getVodPlayUrl() : "";
        String sourceFrom = source.getVodPlayFrom() != null ? source.getVodPlayFrom() : "";
        String sourceUrl = source.getVodPlayUrl() != null ? source.getVodPlayUrl() : "";

        List<String> targetFromArr = new ArrayList<>(Arrays.asList(targetFrom.split("\\$\\$\\$")));
        List<String> targetUrlArr = new ArrayList<>(Arrays.asList(targetUrl.split("\\$\\$\\$")));
        List<String> sourceFromArr = Arrays.asList(sourceFrom.split("\\$\\$\\$"));
        List<String> sourceUrlArr = Arrays.asList(sourceUrl.split("\\$\\$\\$"));

        // 将源视频的播放源添加到目标视频(去重)
        for (int i = 0; i < sourceFromArr.size(); i++) {
            String fromName = sourceFromArr.get(i);
            if (fromName != null && !fromName.isEmpty() && !targetFromArr.contains(fromName)) {
                targetFromArr.add(fromName);
                targetUrlArr.add(i < sourceUrlArr.size() ? sourceUrlArr.get(i) : "");
            }
        }

        // 更新目标视频
        String newFrom = String.join("$$$", targetFromArr);
        String newUrl = String.join("$$$", targetUrlArr);
        target.setVodPlayFrom(newFrom);
        target.setVodPlayUrl(newUrl);
        target.setVodTime((int) (System.currentTimeMillis() / 1000));
        videoRepository.save(target);

        // 删除源视频
        videoRepository.deleteById(sourceId);
        
        log.info("视频合并成功: 保留ID={}, 删除ID={}", targetId, sourceId);
    }
    
    /**
     * 为视频对象的必填字段设置默认值(苹果CMS V10标准)
     */
    private void setDefaultValues(Video video) {
        if (video.getVodSub() == null) video.setVodSub("");
        if (video.getVodEn() == null) video.setVodEn("");
        if (video.getVodStatus() == null) video.setVodStatus(0);
        if (video.getVodPic() == null) video.setVodPic("");
        if (video.getVodActor() == null) video.setVodActor("");
        if (video.getVodDirector() == null) video.setVodDirector("");
        if (video.getVodWriter() == null) video.setVodWriter("");
        if (video.getVodBlurb() == null) video.setVodBlurb("");
        if (video.getVodRemarks() == null) video.setVodRemarks("");
        if (video.getVodArea() == null) video.setVodArea("");
        if (video.getVodLang() == null) video.setVodLang("");
        if (video.getVodYear() == null) video.setVodYear("");
        if (video.getVodScore() == null) video.setVodScore("0.0");
        if (video.getVodHits() == null) video.setVodHits(0);
        if (video.getVodHitsDay() == null) video.setVodHitsDay(0);
        if (video.getVodHitsWeek() == null) video.setVodHitsWeek(0);
        if (video.getVodHitsMonth() == null) video.setVodHitsMonth(0);
        if (video.getVodPlayFrom() == null) video.setVodPlayFrom("");
        if (video.getVodPlayUrl() == null) video.setVodPlayUrl("");
        if (video.getVodContent() == null) video.setVodContent("");
        if (video.getVodDownFrom() == null) video.setVodDownFrom("");
        if (video.getVodDownUrl() == null) video.setVodDownUrl("");
        if (video.getVodTag() == null) video.setVodTag("");
        if (video.getVodClass() == null) video.setVodClass("");
        if (video.getVodTotal() == null) video.setVodTotal(0);
        if (video.getVodSerial() == null) video.setVodSerial("");
        if (video.getVodIsend() == null) video.setVodIsend(0);
        if (video.getVodPlotName() == null) video.setVodPlotName("");
        if (video.getVodPlotDetail() == null) video.setVodPlotDetail("");
    }
}
