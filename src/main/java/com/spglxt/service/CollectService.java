package com.spglxt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spglxt.dto.CollectSite;
import com.spglxt.dto.CollectTask;
import com.spglxt.dto.CronConfig;
import com.spglxt.entity.Video;
import com.spglxt.util.VideoNameUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * 采集服务层
 * 
 * @author spglxt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectService {

    private final ObjectMapper objectMapper;
    private final CollectTaskService taskService;
    private final VideoService videoService;
    private final RestTemplate restTemplate = new RestTemplate();

    private static final String SITES_FILE = "config/collect_sites.json";
    private static final String NAMEWORDS_FILE = "config/collect_namewords.txt";
    
    // 系统内置播放源白名单(只有白名单中的播放源才会被添加)
    private static final List<String> SYSTEM_PLAYERS = Arrays.asList(
        "cyfunA", "cyfunB", "bilibili", "cyfunC", "cyfunW",
        "qiyi", "qq", "youku", "YYNB", "1080zyk",
        "cyfun4K", "jsm3u8", "tym3u8", "lzm3u8", "modum3u8"
    );

    /**
     * 获取所有采集站点
     */
    public List<CollectSite> getAllSites() {
        return loadSitesFromFile();
    }

    /**
     * 根据ID查找站点
     */
    public CollectSite findSiteById(Integer id) {
        List<CollectSite> sites = loadSitesFromFile();
        return sites.stream()
                .filter(site -> site.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * 添加站点
     */
    public CollectSite addSite(CollectSite site) {
        List<CollectSite> sites = loadSitesFromFile();
        
        // 生成新ID
        int maxId = sites.stream()
                .mapToInt(CollectSite::getId)
                .max()
                .orElse(0);
        
        site.setId(maxId + 1);
        site.setCreatedAt(System.currentTimeMillis() / 1000);
        site.setUpdatedAt(System.currentTimeMillis() / 1000);
        
        sites.add(site);
        saveSitesToFile(sites);
        
        return site;
    }

    /**
     * 更新站点
     */
    public boolean updateSite(Integer id, CollectSite site) {
        List<CollectSite> sites = loadSitesFromFile();
        
        for (int i = 0; i < sites.size(); i++) {
            if (sites.get(i).getId().equals(id)) {
                site.setId(id);
                site.setCreatedAt(sites.get(i).getCreatedAt());
                site.setUpdatedAt(System.currentTimeMillis() / 1000);
                sites.set(i, site);
                saveSitesToFile(sites);
                return true;
            }
        }
        
        return false;
    }

    /**
     * 删除站点
     */
    public boolean deleteSite(Integer id) {
        List<CollectSite> sites = loadSitesFromFile();
        boolean removed = sites.removeIf(site -> site.getId().equals(id));
        
        if (removed) {
            saveSitesToFile(sites);
        }
        
        return removed;
    }

    /**
     * 从资源站API获取并解析数据
     */
    public Map<String, Object> fetchAndParseList(CollectSite site, Map<String, String> params) {
        try {
            StringBuilder url = new StringBuilder(site.getUrl());
            if (!url.toString().contains("?")) {
                url.append("?");
            } else if (!url.toString().endsWith("&") && !url.toString().endsWith("?")) {
                url.append("&");
            }
            
            // 添加参数
            if (params != null) {
                params.forEach((key, value) -> 
                    url.append(key).append("=").append(value).append("&")
                );
            }
            
            // 添加额外参数
            if (site.getParam() != null && !site.getParam().isEmpty()) {
                url.append(site.getParam());
            }
            
            String finalUrl = url.toString();
            if (finalUrl.endsWith("&")) {
                finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
            }
            
            log.info("========== 采集API请求 ==========");
            log.info("站点: {}", site.getName());
            log.info("请求URL: {}", finalUrl);
            log.info("请求类型: {}", site.getType() == 1 ? "JSON" : "XML");
            
            long startTime = System.currentTimeMillis();
            String response = restTemplate.getForObject(finalUrl, String.class);
            long endTime = System.currentTimeMillis();
            
            log.info("请求耗时: {}ms", endTime - startTime);
            
            if (response == null || response.isEmpty()) {
                log.error("响应内容为空");
                throw new RuntimeException("资源站返回空响应");
            }
            
            log.info("响应长度: {} 字符", response.length());
            log.debug("响应内容(前500字符): {}", response.substring(0, Math.min(500, response.length())));
            
            // 解析响应
            Map<String, Object> result;
            if (site.getType() == 1) {
                result = parseJsonResponse(response);
            } else {
                result = parseXmlResponse(response);
            }
            
            log.info("解析结果 - 分类数: {}, 视频数: {}, 总页数: {}, 当前页: {}", 
                result.containsKey("class") ? ((List<?>) result.get("class")).size() : 0,
                result.containsKey("list") ? ((List<?>) result.get("list")).size() : 0,
                result.get("pagecount"),
                result.get("page"));
            log.info("========== 请求完成 ==========");
            
            return result;
        } catch (Exception e) {
            log.error("========== 请求失败 ==========");
            log.error("错误信息: {}", e.getMessage());
            log.error("异常堆栈:", e);
            throw new RuntimeException("请求资源站失败: " + e.getMessage());
        }
    }

    /**
     * 解析JSON响应
     */
    private Map<String, Object> parseJsonResponse(String response) {
        try {
            log.info("---------- 开始解析JSON响应 ----------");
            JsonNode root = objectMapper.readTree(response);
            Map<String, Object> result = new HashMap<>();
            
            // 解析分类列表
            JsonNode classNode = root.path("class");
            if (classNode.isArray()) {
                List<Map<String, Object>> classList = new ArrayList<>();
                classNode.forEach(node -> {
                    Map<String, Object> classItem = new HashMap<>();
                    classItem.put("type_id", node.path("type_id").asText());
                    classItem.put("type_name", node.path("type_name").asText());
                    classList.add(classItem);
                });
                result.put("class", classList);
                log.info("解析到 {} 个分类", classList.size());
            } else {
                log.warn("响应中没有class字段或不是数组");
            }
            
            // 解析视频列表
            JsonNode listNode = root.path("list");
            if (listNode.isArray()) {
                List<Map<String, Object>> videoList = new ArrayList<>();
                listNode.forEach(node -> {
                    Map<String, Object> video = new HashMap<>();
                    node.fields().forEachRemaining(entry -> {
                        if (entry.getValue().isNumber()) {
                            video.put(entry.getKey(), entry.getValue().asInt());
                        } else {
                            video.put(entry.getKey(), entry.getValue().asText());
                        }
                    });
                    videoList.add(video);
                });
                result.put("list", videoList);
                log.info("解析到 {} 个视频", videoList.size());
                if (!videoList.isEmpty()) {
                    log.debug("第一个视频: {}", videoList.get(0).get("vod_name"));
                }
            } else {
                log.warn("响应中没有list字段或不是数组");
            }
            
            // 分页信息
            int page = root.path("page").asInt(1);
            int pagecount = root.path("pagecount").asInt(1);
            int limit = root.path("limit").asInt(20);
            int total = root.path("total").asInt(0);
            
            result.put("page", page);
            result.put("pagecount", pagecount);
            result.put("limit", limit);
            result.put("total", total);
            
            log.info("分页信息 - 当前页:{}, 总页数:{}, 每页数量:{}, 总数:{}", page, pagecount, limit, total);
            log.info("---------- JSON解析完成 ----------");
            
            return result;
        } catch (Exception e) {
            log.error("---------- JSON解析失败 ----------");
            log.error("错误信息: {}", e.getMessage());
            log.error("响应内容(前1000字符): {}", response.substring(0, Math.min(1000, response.length())));
            throw new RuntimeException("解析JSON响应失败: " + e.getMessage());
        }
    }

    /**
     * 解析XML响应(简化版,主要用于MacCMS XML格式)
     */
    private Map<String, Object> parseXmlResponse(String response) {
        // TODO: 实现XML解析
        throw new RuntimeException("XML格式暂未支持,请使用JSON格式");
    }

    /**
     * 批量导入视频
     * @return Map{add: 新增数, update: 更新数, skip: 跳过数, skipReasons: 跳过原因列表}
     */
    public Map<String, Object> importVideos(List<Map<String, Object>> videos, Integer siteId, String mode) {
        log.info("========== 开始导入视频 ==========");
        log.info("站点ID: {}, 模式: {}, 视频数量: {}", siteId, mode, videos.size());
        
        Map<String, Object> result = new HashMap<>();
        result.put("add", 0);
        result.put("update", 0);
        result.put("skip", 0);
        List<String> skipReasons = new ArrayList<>();
        result.put("skipReasons", skipReasons);
        
        // 加载站点配置(包含分类绑定)
        CollectSite site = findSiteById(siteId);
        String collectOpt = "0";
        String[] uprule = {"a", "d"};
        String filterFrom = "";
        String collectFilter = "0";
        Integer skipSameTotal = 1;
        Map<String, Integer> bindConfig = new HashMap<>();
        
        if (site != null) {
            collectOpt = site.getCollectOpt() != null ? site.getCollectOpt() : "0";
            if ("1".equals(collectOpt)) {
                mode = "insert";
                log.info("站点配置: 仅新增模式");
            } else if ("2".equals(collectOpt)) {
                mode = "update";
                log.info("站点配置: 仅更新模式");
            }
            
            if (site.getUprule() != null && !site.getUprule().isEmpty()) {
                uprule = site.getUprule().split(",");
            }
            filterFrom = site.getFilterFrom() != null ? site.getFilterFrom() : "";
            collectFilter = site.getCollectFilter() != null ? site.getCollectFilter() : "0";
            skipSameTotal = site.getSkipSameTotal() != null ? site.getSkipSameTotal() : 1;
            bindConfig = site.getBindConfig(); // 从站点配置中获取分类绑定
            
            log.info("更新规则: {}", String.join(",", uprule));
            log.info("过滤来源: {}", filterFrom.isEmpty() ? "无" : filterFrom);
            log.info("过滤模式: {}", collectFilter);
            log.info("智能跳过: {}", skipSameTotal == 1 ? "启用" : "禁用");
        }
        
        log.info("分类绑定配置: {} 个绑定", bindConfig.size());
        
        // 加载名称同义库
        Map<String, String> namewordsMap = getNamewordsMap();
        log.info("名称同义库: {} 个映射", namewordsMap.size());
        
        // 构建允许采集的影片名称集合(包含key和value)
        Set<String> allowedVideoNames = new HashSet<>();
        if (!namewordsMap.isEmpty()) {
            allowedVideoNames.addAll(namewordsMap.keySet());  // 资源站名称
            allowedVideoNames.addAll(namewordsMap.values());  // 本地数据库名称
            log.info("允许采集的影片数量: {}", allowedVideoNames.size());
        }
        
        // 遍历导入
        int processedCount = 0;
        for (Map<String, Object> videoData : videos) {
            processedCount++;
            try {
                String vodName = String.valueOf(videoData.getOrDefault("vod_name", "")).trim();
                if (vodName.isEmpty()) {
                    String reason = "视频名称为空";
                    log.warn("[{}/{}] 跳过 - {}", processedCount, videos.size(), reason);
                    skipReasons.add(reason);
                    int skipCount = (Integer) result.get("skip");
                    result.put("skip", skipCount + 1);
                    continue;
                }
                
                // 名称绑定过滤: 如果配置了namewords,则只采集配置中的影片
                if (!allowedVideoNames.isEmpty() && !allowedVideoNames.contains(vodName)) {
                    String reason = vodName + " - 未在绑定影片配置中";
                    log.debug("[{}/{}] 跳过 - {}", processedCount, videos.size(), reason);
                    skipReasons.add(reason);
                    int skipCount = (Integer) result.get("skip");
                    result.put("skip", skipCount + 1);
                    continue;
                }
                
                // 名称同义库转换
                String originalName = vodName;
                if (namewordsMap.containsKey(vodName)) {
                    vodName = namewordsMap.get(vodName);
                    log.info("[{}/{}] 名称转换: {} -> {}", processedCount, videos.size(), originalName, vodName);
                }
                
                // 分类绑定检查（对齐 PHP）
                String sourceTypeId = String.valueOf(videoData.getOrDefault("type_id", "0"));
                Integer localTypeId;

                if (bindConfig != null && !bindConfig.isEmpty()) {
                    if (!bindConfig.containsKey(sourceTypeId)) {
                        String reason = vodName + " - 分类" + sourceTypeId + "未绑定(不采集)";
                        log.info("[{}/{}] 跳过 - {}", processedCount, videos.size(), reason);
                        skipReasons.add(reason);
                        int skipCount = (Integer) result.get("skip");
                        result.put("skip", skipCount + 1);
                        continue;
                    }
                    localTypeId = bindConfig.get(sourceTypeId);
                    if (localTypeId == null || localTypeId == 0) {
                        String reason = vodName + " - 分类" + sourceTypeId + "绑定为0(不采集)";
                        log.info("[{}/{}] 跳过 - {}", processedCount, videos.size(), reason);
                        skipReasons.add(reason);
                        int skipCount = (Integer) result.get("skip");
                        result.put("skip", skipCount + 1);
                        continue;
                    }
                    log.debug("[{}/{}] {} - 分类映射: {} -> {}", processedCount, videos.size(), vodName, sourceTypeId, localTypeId);
                } else {
                    try {
                        localTypeId = Integer.parseInt(sourceTypeId);
                    } catch (NumberFormatException e) {
                        localTypeId = 0;
                    }
                    if (localTypeId == 0) {
                        String reason = vodName + " - 无效分类" + sourceTypeId;
                        skipReasons.add(reason);
                        int skipCount = (Integer) result.get("skip");
                        result.put("skip", skipCount + 1);
                        continue;
                    }
                }
                
                // 检查是否存在 - 多级匹配
                Optional<Video> existingVideo = videoService.findByName(vodName);
                
                // 1. 精确匹配失败，尝试去除空格匹配
                if (existingVideo.isEmpty()) {
                    existingVideo = videoService.findByNameWithoutSpaces(vodName);
                    if (existingVideo.isPresent()) {
                        log.debug("[{}/{}] {} - 去空格匹配成功 ID:{}, 数据库名称: {}", 
                            processedCount, videos.size(), vodName, 
                            existingVideo.get().getVodId(), existingVideo.get().getVodName());
                    }
                }
                
                // 2. 去空格匹配失败，尝试标准化名称匹配(季数/罗马数字)
                if (existingVideo.isEmpty()) {
                    String normalizedName = VideoNameUtil.normalizeVideoName(vodName);
                    log.debug("[{}/{}] {} - 标准化名称: {}", processedCount, videos.size(), vodName, normalizedName);
                    // 标准化名称匹配需要更复杂的查询,这里先跳过
                }
                
                if (existingVideo.isPresent()) {
                    log.debug("[{}/{}] {} - 视频已存在 ID:{}", processedCount, videos.size(), vodName, existingVideo.get().getVodId());
                } else {
                    log.debug("[{}/{}] {} - 新视频", processedCount, videos.size(), vodName);
                }
                
                if (existingVideo.isPresent() && "insert".equals(mode)) {
                    String reason = vodName + " - 仅新增模式,视频已存在";
                    log.debug("[{}/{}] 跳过 - {}", processedCount, videos.size(), reason);
                    skipReasons.add(reason);
                    int skipCount = (Integer) result.get("skip");
                    result.put("skip", skipCount + 1);
                    continue;
                }
                
                if (existingVideo.isEmpty() && "update".equals(mode)) {
                    String reason = vodName + " - 仅更新模式,视频不存在";
                    log.debug("[{}/{}] 跳过 - {}", processedCount, videos.size(), reason);
                    skipReasons.add(reason);
                    int skipCount = (Integer) result.get("skip");
                    result.put("skip", skipCount + 1);
                    continue;
                }
                
                // 构建视频对象
                Video video = existingVideo.orElse(new Video());
                
                if (existingVideo.isEmpty()) {
                    // ============ 新增视频 ============
                    video.setTypeId(localTypeId);
                    video.setTypeId1(localTypeId);
                    video.setVodName(vodName);
                    
                    // 处理所有字符串字段
                    String vodSub = String.valueOf(videoData.getOrDefault("vod_sub", ""));
                    String vodEn = String.valueOf(videoData.getOrDefault("vod_en", ""));
                    String vodPic = String.valueOf(videoData.getOrDefault("vod_pic", ""));
                    String vodBlurb = String.valueOf(videoData.getOrDefault("vod_blurb", ""));
                    String vodRemarks = String.valueOf(videoData.getOrDefault("vod_remarks", ""));
                    String vodActor = String.valueOf(videoData.getOrDefault("vod_actor", ""));
                    String vodDirector = String.valueOf(videoData.getOrDefault("vod_director", ""));
                    String vodWriter = String.valueOf(videoData.getOrDefault("vod_writer", ""));
                    String vodArea = String.valueOf(videoData.getOrDefault("vod_area", ""));
                    String vodLang = String.valueOf(videoData.getOrDefault("vod_lang", ""));
                    String vodYear = String.valueOf(videoData.getOrDefault("vod_year", ""));
                    String vodContent = String.valueOf(videoData.getOrDefault("vod_content", ""));
                    String vodPlayFrom = String.valueOf(videoData.getOrDefault("vod_play_from", ""));
                    String vodPlayUrl = String.valueOf(videoData.getOrDefault("vod_play_url", ""));
                    String vodScore = String.valueOf(videoData.getOrDefault("vod_score", "0.0"));
                    String vodDownFrom = String.valueOf(videoData.getOrDefault("vod_down_from", ""));
                    String vodDownUrl = String.valueOf(videoData.getOrDefault("vod_down_url", ""));
                    String vodTag = String.valueOf(videoData.getOrDefault("vod_tag", ""));
                    String vodClass = String.valueOf(videoData.getOrDefault("vod_class", ""));
                    String vodSerial = String.valueOf(videoData.getOrDefault("vod_serial", ""));
                    
                    // 处理 "null" 字符串
                    vodSub = "null".equals(vodSub) ? "" : vodSub;
                    vodEn = "null".equals(vodEn) ? "" : vodEn;
                    vodPic = "null".equals(vodPic) ? "" : vodPic;
                    vodBlurb = "null".equals(vodBlurb) ? "" : vodBlurb;
                    vodRemarks = "null".equals(vodRemarks) ? "" : vodRemarks;
                    vodRemarks = formatRemarks(vodRemarks);
                    vodActor = "null".equals(vodActor) ? "" : vodActor;
                    vodDirector = "null".equals(vodDirector) ? "" : vodDirector;
                    vodWriter = "null".equals(vodWriter) ? "" : vodWriter;
                    vodArea = "null".equals(vodArea) ? "" : vodArea;
                    vodLang = "null".equals(vodLang) ? "" : vodLang;
                    vodYear = "null".equals(vodYear) ? "" : vodYear;
                    vodContent = "null".equals(vodContent) ? "" : vodContent;
                    vodPlayFrom = "null".equals(vodPlayFrom) ? "" : vodPlayFrom;
                    vodPlayUrl = "null".equals(vodPlayUrl) ? "" : vodPlayUrl;
                    vodDownFrom = "null".equals(vodDownFrom) ? "" : vodDownFrom;
                    vodDownUrl = "null".equals(vodDownUrl) ? "" : vodDownUrl;
                    vodTag = "null".equals(vodTag) ? "" : vodTag;
                    vodClass = "null".equals(vodClass) ? "" : vodClass;
                    vodSerial = "null".equals(vodSerial) ? "" : vodSerial;
                    
                    // ============ 播放来源过滤 ============
                    // 第一步: 系统白名单过滤(只保留白名单中的播放源)
                    if (!vodPlayFrom.isEmpty()) {
                        String[] fromArr = vodPlayFrom.split("\\$\\$\\$");
                        String[] urlArr = vodPlayUrl.split("\\$\\$\\$");
                        
                        List<String> whitelistFrom = new ArrayList<>();
                        List<String> whitelistUrl = new ArrayList<>();
                        
                        for (int i = 0; i < fromArr.length; i++) {
                            String fromName = fromArr[i].trim();
                            if (SYSTEM_PLAYERS.contains(fromName)) {
                                whitelistFrom.add(fromArr[i]);
                                whitelistUrl.add(i < urlArr.length ? urlArr[i] : "");
                            } else {
                                log.debug("[{}/{}] {} - 跳过非白名单播放源: {}", processedCount, videos.size(), vodName, fromName);
                            }
                        }
                        
                        vodPlayFrom = String.join("$$$", whitelistFrom);
                        vodPlayUrl = String.join("$$$", whitelistUrl);
                        
                        log.debug("[{}/{}] {} - 白名单过滤后播放源: {}", processedCount, videos.size(), vodName, vodPlayFrom);
                    }
                    
                    // 第二步: 用户黑名单过滤(从白名单结果中移除不想要的播放源)
                    if (!filterFrom.isEmpty() && !vodPlayFrom.isEmpty()) {
                        // 检查是否需要黑名单过滤
                        boolean shouldFilter = false;
                        if ("1".equals(collectFilter)) {
                            // 新增+更新都过滤
                            shouldFilter = true;
                        } else if ("2".equals(collectFilter)) {
                            // 仅新增时过滤
                            shouldFilter = true;
                        }
                        
                        if (shouldFilter) {
                            String[] filterArr = filterFrom.split(",");
                            List<String> filterList = new ArrayList<>();
                            for (String f : filterArr) {
                                filterList.add(f.trim());
                            }
                            
                            String[] fromArr = vodPlayFrom.split("\\$\\$\\$");
                            String[] urlArr = vodPlayUrl.split("\\$\\$\\$");
                            
                            List<String> filteredFrom = new ArrayList<>();
                            List<String> filteredUrl = new ArrayList<>();
                            
                            for (int i = 0; i < fromArr.length; i++) {
                                String fromName = fromArr[i].trim();
                                if (!filterList.contains(fromName)) {
                                    filteredFrom.add(fromArr[i]);
                                    filteredUrl.add(i < urlArr.length ? urlArr[i] : "");
                                } else {
                                    log.debug("[{}/{}] {} - 黑名单过滤播放源: {}", processedCount, videos.size(), vodName, fromName);
                                }
                            }
                            
                            vodPlayFrom = String.join("$$$", filteredFrom);
                            vodPlayUrl = String.join("$$$", filteredUrl);
                            
                            log.debug("[{}/{}] {} - 黑名单过滤后播放源: {}", processedCount, videos.size(), vodName, vodPlayFrom);
                        }
                    }
                    
                    // 第三步: 检查是否还有有效播放源
                    if (vodPlayFrom.isEmpty() || vodPlayUrl.isEmpty()) {
                        String reason = vodName + " - 没有有效的播放源(过滤后)";
                        log.info("[{}/{}] ⏭️ 跳过 - {}", processedCount, videos.size(), reason);
                        skipReasons.add(reason);
                        int skipCount = (Integer) result.get("skip");
                        result.put("skip", skipCount + 1);
                        continue;
                    }
                    
                    video.setVodSub(vodSub);
                    video.setVodEn(vodEn);
                    video.setVodPic(vodPic);
                    video.setVodBlurb(vodBlurb);
                    video.setVodRemarks(vodRemarks);
                    video.setVodActor(vodActor);
                    video.setVodDirector(vodDirector);
                    video.setVodWriter(vodWriter);
                    video.setVodArea(vodArea);
                    video.setVodLang(vodLang);
                    video.setVodYear(vodYear);
                    video.setVodContent(vodContent);
                    video.setVodPlayFrom(vodPlayFrom);
                    video.setVodPlayUrl(vodPlayUrl);
                    video.setVodScore(vodScore);
                    video.setVodDownFrom(vodDownFrom);
                    video.setVodDownUrl(vodDownUrl);
                    video.setVodTag(vodTag);
                    video.setVodClass(vodClass);
                    video.setVodSerial(vodSerial);
                    video.setVodStatus(1);
                    
                    // 处理数值型字段
                    Integer vodTotal = 0;
                    Integer vodIsend = 0;
                    try {
                        Object totalObj = videoData.get("vod_total");
                        if (totalObj != null && !"null".equals(String.valueOf(totalObj))) {
                            vodTotal = Integer.parseInt(String.valueOf(totalObj));
                        }
                    } catch (Exception e) {
                        vodTotal = 0;
                    }
                    try {
                        Object isendObj = videoData.get("vod_isend");
                        if (isendObj != null && !"null".equals(String.valueOf(isendObj))) {
                            vodIsend = Integer.parseInt(String.valueOf(isendObj));
                        }
                    } catch (Exception e) {
                        vodIsend = 0;
                    }
                    
                    video.setVodTotal(vodTotal);
                    video.setVodIsend(vodIsend);
                    video.setVodHits(0);
                    video.setVodHitsDay(0);
                    video.setVodHitsWeek(0);
                    video.setVodHitsMonth(0);
                    
                    // 剧情相关字段
                    String vodPlotName = String.valueOf(videoData.getOrDefault("vod_plot_name", ""));
                    String vodPlotDetail = String.valueOf(videoData.getOrDefault("vod_plot_detail", ""));
                    vodPlotName = "null".equals(vodPlotName) ? "" : vodPlotName;
                    vodPlotDetail = "null".equals(vodPlotDetail) ? "" : vodPlotDetail;
                    video.setVodPlotName(vodPlotName);
                    video.setVodPlotDetail(vodPlotDetail);
                    
                    int currentTime = (int) (System.currentTimeMillis() / 1000);
                    video.setVodTime(currentTime);
                    video.setVodTimeAdd(currentTime);
                    
                    videoService.addVideo(video);
                    log.info("[{}/{}] ✅ 新增 - {}", processedCount, videos.size(), vodName);
                    int addCount = (Integer) result.get("add");
                    result.put("add", addCount + 1);
                } else {
                    // ============ 更新视频 - 智能跳过判断 ============
                    log.debug("[{}/{}] {} - 进入更新分支, 智能跳过开关: {}", processedCount, videos.size(), vodName, skipSameTotal);
                    
                    // 智能跳过：按 uprule 比对备注(d)与播放地址(a)
                    if (skipSameTotal == 1) {
                        Video existingVideoData = existingVideo.get();
                        String newPlayFrom = String.valueOf(videoData.getOrDefault("vod_play_from", ""));
                        String newPlayUrl = String.valueOf(videoData.getOrDefault("vod_play_url", ""));
                        String[] filteredPlay = filterPlaySources(newPlayFrom, newPlayUrl, filterFrom, collectFilter, true);

                        if (shouldSkipByUprule(existingVideoData, videoData, filteredPlay, parseUpruleList(site))) {
                            String reason = vodName + ": 更新规则内字段(地址/备注)均未变化";
                            skipReasons.add(reason);
                            log.info("[{}/{}] ⏭️ 跳过 - {}", processedCount, videos.size(), reason);
                            int skipCount = (Integer) result.get("skip");
                            result.put("skip", skipCount + 1);
                            continue;
                        }
                    } else {
                        log.debug("[{}/{}] {} - 智能跳过功能已禁用", processedCount, videos.size(), vodName);
                    }
                    
                    // ============ 执行更新 - 根据uprule决定更新哪些字段 ============
                    log.debug("[{}/{}] {} - 更新规则: {}", processedCount, videos.size(), vodName, String.join(",", uprule));
                    
                    List<String> upruleList = parseUpruleList(site);
                    
                    // a=播放地址(合并模式:保留已有线路,仅更新/追加同名播放源)
                    if (upruleList.contains("a")) {
                        String newPlayFrom = String.valueOf(videoData.getOrDefault("vod_play_from", ""));
                        String newPlayUrl = String.valueOf(videoData.getOrDefault("vod_play_url", ""));
                        String[] filtered = filterPlaySources(newPlayFrom, newPlayUrl, filterFrom, collectFilter, true);
                        newPlayFrom = filtered[0];
                        newPlayUrl = filtered[1];
                        
                        if (newPlayFrom.isEmpty() || newPlayUrl.isEmpty()) {
                            log.info("[{}/{}] {} - 更新时没有有效的播放源(过滤后),跳过更新播放地址", processedCount, videos.size(), vodName);
                        } else {
                            Video existingVideoData = existingVideo.get();
                            String existFrom = existingVideoData.getVodPlayFrom() != null ? existingVideoData.getVodPlayFrom() : "";
                            String existUrl = existingVideoData.getVodPlayUrl() != null ? existingVideoData.getVodPlayUrl() : "";
                            String[] merged = mergePlaySources(existFrom, existUrl, newPlayFrom, newPlayUrl);
                            video.setVodPlayFrom(merged[0]);
                            video.setVodPlayUrl(merged[1]);
                            log.debug("[{}/{}] {} - 合并后播放源: {}", processedCount, videos.size(), vodName, merged[0]);
                        }
                    }
                    
                    // b=下载地址
                    if (upruleList.contains("b")) {
                        String newDownFrom = String.valueOf(videoData.getOrDefault("vod_down_from", ""));
                        String newDownUrl = String.valueOf(videoData.getOrDefault("vod_down_url", ""));
                        if (!"null".equals(newDownFrom)) video.setVodDownFrom(newDownFrom);
                        if (!"null".equals(newDownUrl)) video.setVodDownUrl(newDownUrl);
                    }
                    
                    // c=连载数
                    if (upruleList.contains("c")) {
                        String newSerial = String.valueOf(videoData.getOrDefault("vod_serial", ""));
                        if (!"null".equals(newSerial)) video.setVodSerial(newSerial);
                    }
                    
                    // d=备注（为空不更新）
                    if (upruleList.contains("d")) {
                        applyRemarksUpdateIfPresent(video, videoData);
                    }
                    
                    // e=导演
                    if (upruleList.contains("e")) {
                        String newDirector = String.valueOf(videoData.getOrDefault("vod_director", ""));
                        if (!"null".equals(newDirector)) video.setVodDirector(newDirector);
                    }
                    
                    // f=演员
                    if (upruleList.contains("f")) {
                        String newActor = String.valueOf(videoData.getOrDefault("vod_actor", ""));
                        if (!"null".equals(newActor)) video.setVodActor(newActor);
                    }
                    
                    // g=年份
                    if (upruleList.contains("g")) {
                        String newYear = String.valueOf(videoData.getOrDefault("vod_year", ""));
                        if (!"null".equals(newYear)) video.setVodYear(newYear);
                    }
                    
                    // h=地区
                    if (upruleList.contains("h")) {
                        String newArea = String.valueOf(videoData.getOrDefault("vod_area", ""));
                        if (!"null".equals(newArea)) video.setVodArea(newArea);
                    }
                    
                    // i=语言
                    if (upruleList.contains("i")) {
                        String newLang = String.valueOf(videoData.getOrDefault("vod_lang", ""));
                        if (!"null".equals(newLang)) video.setVodLang(newLang);
                    }
                    
                    // j=图片
                    if (upruleList.contains("j")) {
                        String newPic = String.valueOf(videoData.getOrDefault("vod_pic", ""));
                        if (!"null".equals(newPic)) video.setVodPic(newPic);
                    }
                    
                    // k=详情
                    if (upruleList.contains("k")) {
                        String newContent = String.valueOf(videoData.getOrDefault("vod_content", ""));
                        if (!"null".equals(newContent)) video.setVodContent(newContent);
                    }
                    
                    // l=TAG
                    if (upruleList.contains("l")) {
                        String newTag = String.valueOf(videoData.getOrDefault("vod_tag", ""));
                        if (!"null".equals(newTag)) video.setVodTag(newTag);
                    }
                    
                    // m=副标题
                    if (upruleList.contains("m")) {
                        String newSub = String.valueOf(videoData.getOrDefault("vod_sub", ""));
                        if (!"null".equals(newSub)) video.setVodSub(newSub);
                    }
                    
                    // n=扩展分类
                    if (upruleList.contains("n")) {
                        String newClass = String.valueOf(videoData.getOrDefault("vod_class", ""));
                        if (!"null".equals(newClass)) video.setVodClass(newClass);
                    }
                    
                    // o=编剧
                    if (upruleList.contains("o")) {
                        String newWriter = String.valueOf(videoData.getOrDefault("vod_writer", ""));
                        if (!"null".equals(newWriter)) video.setVodWriter(newWriter);
                    }
                    
                    // r=简介
                    if (upruleList.contains("r")) {
                        String newBlurb = String.valueOf(videoData.getOrDefault("vod_blurb", ""));
                        if (!"null".equals(newBlurb)) video.setVodBlurb(newBlurb);
                    }
                    
                    // u=总集数
                    if (upruleList.contains("u")) {
                        try {
                            Object totalObj = videoData.get("vod_total");
                            if (totalObj != null && !"null".equals(String.valueOf(totalObj))) {
                                video.setVodTotal(Integer.parseInt(String.valueOf(totalObj)));
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    
                    // v=完结状态
                    if (upruleList.contains("v")) {
                        try {
                            Object isendObj = videoData.get("vod_isend");
                            if (isendObj != null && !"null".equals(String.valueOf(isendObj))) {
                                video.setVodIsend(Integer.parseInt(String.valueOf(isendObj)));
                            }
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    
                    // 更新时间
                    int currentTime = (int) (System.currentTimeMillis() / 1000);
                    video.setVodTime(currentTime);
                    
                    videoService.updateVideo(existingVideo.get().getVodId(), video);
                    log.info("[{}/{}] ✅ 更新 - {}", processedCount, videos.size(), vodName);
                    int updateCount = (Integer) result.get("update");
                    result.put("update", updateCount + 1);
                }
            } catch (Exception e) {
                log.error("[{}/{}] ❌ 导入失败: {}", processedCount, videos.size(), e.getMessage());
                int skipCount = (Integer) result.get("skip");
                result.put("skip", skipCount + 1);
            }
        }
        
        log.info("========== 导入完成 ==========");
        log.info("总计: {} 个, 新增: {}, 更新: {}, 跳过: {}", 
            videos.size(), result.get("add"), result.get("update"), result.get("skip"));
        
        return result;
    }

    /**
     * 从文件加载站点数据
     */
    private List<CollectSite> loadSitesFromFile() {
        File file = new File(SITES_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try {
            return objectMapper.readValue(file, new TypeReference<List<CollectSite>>() {});
        } catch (IOException e) {
            log.error("读取采集站点配置失败", e);
            return new ArrayList<>();
        }
    }

    /**
     * 保存站点数据到文件
     */
    private void saveSitesToFile(List<CollectSite> sites) {
        File file = new File(SITES_FILE);
        File dir = file.getParentFile();
        
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, sites);
        } catch (IOException e) {
            log.error("保存采集站点配置失败", e);
            throw new RuntimeException("保存采集站点配置失败");
        }
    }

    /**
     * 加载分类绑定配置
     * @deprecated 已废弃,分类绑定已整合到站点配置中,请使用 site.getBindConfig()
     */
    @Deprecated
    public Map<String, Integer> loadBindConfig(Integer siteId) {
        // 向后兼容:先尝试从站点配置中读取
        CollectSite site = findSiteById(siteId);
        if (site != null && site.getBindConfig() != null && !site.getBindConfig().isEmpty()) {
            return site.getBindConfig();
        }
        
        // 如果站点配置中没有,则尝试从旧的单独配置文件中读取
        File file = new File("config/collect_bind_" + siteId + ".json");
        if (!file.exists()) {
            return new HashMap<>();
        }
        
        try {
            return objectMapper.readValue(file, new TypeReference<Map<String, Integer>>() {});
        } catch (IOException e) {
            log.error("读取分类绑定配置失败", e);
            return new HashMap<>();
        }
    }

    /**
     * 保存分类绑定配置
     * @deprecated 已废弃,分类绑定已整合到站点配置中,请使用 updateSite 方法
     */
    @Deprecated
    public void saveBindConfig(Integer siteId, Map<String, Integer> binds) {
        // 向后兼容:保存到站点配置中
        CollectSite site = findSiteById(siteId);
        if (site != null) {
            site.setBindConfig(binds);
            updateSite(siteId, site);
        }
    }

    /**
     * 加载名称同义库
     */
    public String loadNamewords() {
        File file = new File(NAMEWORDS_FILE);
        if (!file.exists()) {
            return "";
        }
        
        try {
            return java.nio.file.Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("读取名称同义库失败", e);
            return "";
        }
    }

    /**
     * 保存名称同义库
     */
    public void saveNamewords(String content) {
        File file = new File(NAMEWORDS_FILE);
        File dir = file.getParentFile();
        
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try {
            java.nio.file.Files.writeString(file.toPath(), content, java.nio.charset.StandardCharsets.UTF_8);
            log.info("名称同义库保存成功");
        } catch (IOException e) {
            log.error("保存名称同义库失败", e);
            throw new RuntimeException("保存名称同义库失败");
        }
    }

    /**
     * 解析名称同义库，返回映射Map {资源站名称 -> 本地名称}
     */
    public Map<String, String> getNamewordsMap() {
        String content = loadNamewords();
        if (content == null || content.isEmpty()) {
            return new HashMap<>();
        }
        
        Map<String, String> nameMap = new HashMap<>();
        String[] lines = content.split("\n");
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || !line.contains("=")) {
                continue;
            }
            
            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String fromName = parts[0].trim();
                String toName = parts[1].trim();
                if (!fromName.isEmpty() && !toName.isEmpty()) {
                    nameMap.put(fromName, toName);
                }
            }
        }
        
        return nameMap;
    }

    /**
     * 获取所有任务
     */
    public List<CollectTask> getAllTasks() {
        return taskService.getAllTasks();
    }

    /**
     * 创建采集任务并在服务端后台执行
     */
    public String createTask(Integer siteId) {
        CollectSite site = findSiteById(siteId);
        if (site == null) {
            throw new RuntimeException("站点不存在");
        }

        CollectTask task = taskService.createTask(siteId, site.getName());
        taskService.markBackgroundExecution(task.getId());
        return task.getId();
    }

    /**
     * 恢复/继续在服务端后台执行任务
     */
    public void resumeTaskInBackground(String taskId) {
        CollectTask task = taskService.getTask(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        if ("completed".equals(task.getStatus()) || "stopped".equals(task.getStatus())) {
            throw new RuntimeException("任务已结束，无法继续");
        }
        taskService.markBackgroundExecution(taskId);
    }

    /**
     * 创建定时采集任务并更新 lastRun
     */
    public String createCronTask(Integer siteId, int maxPages, String source) {
        CollectSite site = findSiteById(siteId);
        if (site == null) {
            throw new RuntimeException("站点不存在");
        }

        CronConfig cron = site.getCronConfig();
        CollectTask task = taskService.createCronTask(
                siteId,
                site.getName(),
                cron.getHours(),
                cron.getTypeId(),
                maxPages,
                source
        );
        updateCronLastRun(siteId);
        return task.getId();
    }

    /**
     * 更新站点定时采集的上次执行时间
     */
    public void updateCronLastRun(Integer siteId) {
        CollectSite site = findSiteById(siteId);
        if (site == null) {
            return;
        }
        CronConfig config = site.getCronConfig();
        config.setLastRun(System.currentTimeMillis() / 1000);
        site.setCronConfig(config);
        updateSite(siteId, site);
    }

    /**
     * 执行任务的一页采集
     * 返回: { done: boolean, page: int, addCount: int, updateCount: int }
     */
    public Map<String, Object> executeTaskPage(String taskId) {
        CollectTask task = taskService.getTask(taskId);
        if (task == null) {
            throw new RuntimeException("任务不存在");
        }
        
        if ("stopped".equals(task.getStatus()) || "completed".equals(task.getStatus())) {
            Map<String, Object> result = new HashMap<>();
            result.put("done", true);
            result.put("msg", "任务已完成或已停止");
            return result;
        }
        
        // 第一次执行,获取总页数
        if (task.getCurrentPage() == 0) {
            taskService.updateTaskStatus(taskId, "running");
            taskService.addTaskLog(taskId, "🚀 任务开始执行");
            
            // 获取第一页来确定总页数
            try {
                CollectSite site = findSiteById(task.getSiteId());
                String apiUrl = buildTaskApiUrl(site, task, 1);
                String response = restTemplate.getForObject(apiUrl, String.class);
                
                JsonNode root = objectMapper.readTree(response);
                int totalPages = root.path("pagecount").asInt(root.path("page").asInt(1));
                totalPages = capTotalPages(totalPages, task.getMaxPages());
                taskService.updateTaskProgress(taskId, 1, totalPages);
                taskService.addTaskLog(taskId, "📊 总页数: " + totalPages);
                String collectOpt = site.getCollectOpt() != null ? site.getCollectOpt() : "0";
                String modeLabel = "1".equals(collectOpt) ? "仅新增" : "2".equals(collectOpt) ? "仅更新" : "全部";
                taskService.addTaskLog(taskId, "⚙️ 采集模式: " + modeLabel);
                List<String> upruleLabels = parseUpruleList(site);
                List<String> upruleNames = new ArrayList<>();
                if (upruleLabels.contains("a")) upruleNames.add("播放地址");
                if (upruleLabels.contains("d")) upruleNames.add("备注");
                if (!upruleNames.isEmpty()) {
                    taskService.addTaskLog(taskId, "⚙️ 更新规则: " + String.join(" + ", upruleNames));
                }
            } catch (Exception e) {
                taskService.updateTaskStatus(taskId, "failed");
                taskService.addTaskLog(taskId, "❌ 获取总页数失败: " + e.getMessage());
                throw new RuntimeException("获取总页数失败", e);
            }
        }
        
        // 执行当前页采集
        int currentPage = task.getCurrentPage() == 0 ? 1 : task.getCurrentPage();
        int totalPages = task.getTotalPages();
        
        taskService.addTaskLog(taskId, "📄 采集第 " + currentPage + " 页...");
        
        try {
            CollectSite site = findSiteById(task.getSiteId());
            String apiUrl = buildTaskApiUrl(site, task, currentPage);
            String response = restTemplate.getForObject(apiUrl, String.class);
            
            // 解析视频列表
            List<Map<String, Object>> videos = parseVideoList(response);
            taskService.addTaskLog(taskId, "   找到 " + videos.size() + " 个视频");
            
            // 从站点配置中加载分类绑定
            Map<String, Integer> bindConfig = site.getBindConfig();
            
            // 加载名称同义库
            Map<String, String> namewordsMap = getNamewordsMap();
            
            // 导入视频
            int pageAddCount = 0;
            int pageUpdateCount = 0;
            int pageSkipCount = 0;
            
            for (Map<String, Object> videoData : videos) {
                try {
                    int importResult = importVideo(videoData, bindConfig, namewordsMap, task.getSiteId());
                    if (importResult == 1) {
                        pageAddCount++;
                        taskService.incrementTaskCount(taskId, true);
                    } else if (importResult == 0) {
                        pageUpdateCount++;
                        taskService.incrementTaskCount(taskId, false);
                    } else {
                        pageSkipCount++;
                    }
                } catch (Exception e) {
                    log.warn("导入视频失败: {}", e.getMessage());
                }
            }
            
            taskService.addTaskLog(taskId, "   ✅ 入库: 新增 " + pageAddCount + ", 更新 " + pageUpdateCount + ", 跳过 " + pageSkipCount);
            
            // 更新进度
            if (currentPage >= totalPages) {
                taskService.updateTaskStatus(taskId, "completed");
                taskService.updateTaskProgress(taskId, totalPages, totalPages);
                taskService.addTaskLog(taskId, "🎉 任务完成! 总计新增 " + task.getAddCount() + ", 更新 " + task.getUpdateCount());
                
                Map<String, Object> result = new HashMap<>();
                result.put("done", true);
                result.put("page", currentPage);
                result.put("addCount", pageAddCount);
                result.put("updateCount", pageUpdateCount);
                return result;
            } else {
                taskService.updateTaskProgress(taskId, currentPage + 1, totalPages);
                
                Map<String, Object> result = new HashMap<>();
                result.put("done", false);
                result.put("page", currentPage);
                result.put("addCount", pageAddCount);
                result.put("updateCount", pageUpdateCount);
                return result;
            }
            
        } catch (Exception e) {
            log.error("执行任务页失败", e);
            taskService.updateTaskStatus(taskId, "failed");
            taskService.addTaskLog(taskId, "❌ 第 " + currentPage + " 页采集失败: " + e.getMessage());
            throw new RuntimeException("采集失败", e);
        }
    }

    /**
     * 停止任务
     */
    public void stopTask(String taskId) {
        taskService.stopTask(taskId);
    }

    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        taskService.clearAllTasks();
    }

    /**
     * 构建任务采集 API URL（含 hours / type_id 等参数）
     */
    private String buildTaskApiUrl(CollectSite site, CollectTask task, int page) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("ac", "list");
        params.put("pg", String.valueOf(page));
        if (task.getHours() != null && !task.getHours().isEmpty()) {
            params.put("h", task.getHours());
        }
        if (task.getTypeId() != null && !task.getTypeId().isEmpty()) {
            params.put("t", task.getTypeId());
        }
        return buildApiUrl(site, params);
    }

    /**
     * 构建API URL
     */
    private String buildApiUrl(CollectSite site, Map<String, String> params) {
        StringBuilder url = new StringBuilder(site.getUrl());
        if (!url.toString().contains("?")) {
            url.append("?");
        } else if (!url.toString().endsWith("&") && !url.toString().endsWith("?")) {
            url.append("&");
        }

        if (params != null) {
            params.forEach((key, value) -> url.append(key).append("=").append(value).append("&"));
        }

        if (site.getParam() != null && !site.getParam().isEmpty()) {
            url.append(site.getParam());
        }

        String finalUrl = url.toString();
        if (finalUrl.endsWith("&")) {
            finalUrl = finalUrl.substring(0, finalUrl.length() - 1);
        }
        return finalUrl;
    }

    private int capTotalPages(int totalPages, Integer maxPages) {
        if (maxPages != null && maxPages > 0 && totalPages > maxPages) {
            return maxPages;
        }
        return totalPages;
    }

    /**
     * 解析视频列表
     */
    private List<Map<String, Object>> parseVideoList(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode listNode = root.path("list");
        
        List<Map<String, Object>> videos = new ArrayList<>();
        if (listNode.isArray()) {
            for (JsonNode node : listNode) {
                Map<String, Object> video = new HashMap<>();
                video.put("vod_id", node.path("vod_id").asText());
                video.put("vod_name", node.path("vod_name").asText());
                video.put("type_id", node.path("type_id").asInt());
                video.put("vod_pic", node.path("vod_pic").asText(""));
                video.put("vod_remarks", node.path("vod_remarks").asText(""));
                video.put("vod_actor", node.path("vod_actor").asText(""));
                video.put("vod_director", node.path("vod_director").asText(""));
                video.put("vod_area", node.path("vod_area").asText(""));
                video.put("vod_lang", node.path("vod_lang").asText(""));
                video.put("vod_year", node.path("vod_year").asText(""));
                video.put("vod_content", node.path("vod_content").asText(""));
                video.put("vod_play_from", node.path("vod_play_from").asText(""));
                video.put("vod_play_url", node.path("vod_play_url").asText(""));
                videos.add(video);
            }
        }
        
        return videos;
    }

    /**
     * 导入单个视频
     * @param siteId 站点ID,用于获取智能跳过配置
     * @return 1=新增, 0=更新, -1=跳过
     */
    private int importVideo(Map<String, Object> videoData, Map<String, Integer> bindConfig, Map<String, String> namewordsMap, Integer siteId) {
        String vodName = (String) videoData.get("vod_name");
        if (vodName == null || vodName.trim().isEmpty()) {
            return -1;
        }
        
        String originalName = vodName; // 保存原始名称用于日志
        
        // === 名称绑定过滤 ===
        // 如果配置了namewords,则只采集配置中的影片
        if (!namewordsMap.isEmpty()) {
            // 构建允许采集的影片名称集合(包含资源站名称和本地名称)
            Set<String> allowedVideoNames = new HashSet<>();
            allowedVideoNames.addAll(namewordsMap.keySet());  // 资源站名称
            allowedVideoNames.addAll(namewordsMap.values());  // 本地数据库名称
            
            // 如果当前影片不在白名单中,跳过采集
            if (!allowedVideoNames.contains(vodName)) {
                log.debug("{} - 未在绑定影片配置中,跳过采集", vodName);
                return -1;
            }
            
            log.debug("{} - 在绑定影片配置中,继续采集", vodName);
        }
        
        // 智能跳过开关(从站点配置中读取,默认开启)
        CollectSite site = findSiteById(siteId);
        String collectOpt = (site != null && site.getCollectOpt() != null) ? site.getCollectOpt() : "0";
        int skipSameTotal = (site != null && site.getSkipSameTotal() != null) ? site.getSkipSameTotal() : 1;
        String filterFrom = (site != null && site.getFilterFrom() != null) ? site.getFilterFrom() : "";
        String collectFilter = (site != null && site.getCollectFilter() != null) ? site.getCollectFilter() : "0";
        List<String> upruleList = parseUpruleList(site);
        
        // 分类绑定（对齐 PHP：有绑定配置时必须在映射表中）
        String sourceTypeId = String.valueOf(videoData.getOrDefault("type_id", "0"));
        Integer localTypeId;
        if (bindConfig != null && !bindConfig.isEmpty()) {
            if (!bindConfig.containsKey(sourceTypeId)) {
                log.info("跳过视频 {} - 分类{}未绑定(不采集)", vodName, sourceTypeId);
                return -1;
            }
            localTypeId = bindConfig.get(sourceTypeId);
            if (localTypeId == null || localTypeId == 0) {
                log.info("跳过视频 {} - 分类{}绑定为0(不采集)", vodName, sourceTypeId);
                return -1;
            }
        } else {
            try {
                localTypeId = Integer.parseInt(sourceTypeId);
            } catch (NumberFormatException e) {
                localTypeId = 0;
            }
            if (localTypeId == 0) {
                log.info("跳过视频 {} - 无效分类{}", vodName, sourceTypeId);
                return -1;
            }
        }
        
        // === 多级智能匹配 ===
        Optional<Video> existingVideo = Optional.empty();
        String matchType = null;
        
        // 1. 精确匹配
        existingVideo = videoService.findByName(vodName);
        if (existingVideo.isPresent()) {
            matchType = "精确匹配";
        }
        
        // 2. 去空格匹配
        if (existingVideo.isEmpty()) {
            existingVideo = videoService.findByNameWithoutSpaces(vodName);
            if (existingVideo.isPresent()) {
                matchType = "去空格匹配";
                log.debug("{} - 去空格匹配成功 ID:{}, 数据库名称: {}", 
                    vodName, existingVideo.get().getVodId(), existingVideo.get().getVodName());
            }
        }
        
        // 3. 标准化名称匹配(季数/罗马数字)
        if (existingVideo.isEmpty()) {
            String normalizedName = VideoNameUtil.normalizeVideoName(vodName);
            existingVideo = videoService.findByNormalizedName(normalizedName);
            if (existingVideo.isPresent()) {
                matchType = "标准化匹配";
                log.debug("{} - 标准化匹配成功 ID:{}, 数据库名称: {}, 标准化: {}", 
                    vodName, existingVideo.get().getVodId(), existingVideo.get().getVodName(), normalizedName);
            }
        }
        
        // 4. 智能匹配失败，尝试使用同义词库
        if (existingVideo.isEmpty() && namewordsMap.containsKey(vodName)) {
            String synonymName = namewordsMap.get(vodName);
            log.info("同义词转换: {} -> {}", vodName, synonymName);
            vodName = synonymName; // 使用同义词替换
            
            // 4.1 同义词精确匹配
            existingVideo = videoService.findByName(vodName);
            if (existingVideo.isPresent()) {
                matchType = "同义词精确匹配";
            }
            
            // 4.2 同义词去空格匹配
            if (existingVideo.isEmpty()) {
                existingVideo = videoService.findByNameWithoutSpaces(vodName);
                if (existingVideo.isPresent()) {
                    matchType = "同义词去空格匹配";
                    log.debug("{} - 同义词去空格匹配成功 ID:{}, 数据库名称: {}", 
                        vodName, existingVideo.get().getVodId(), existingVideo.get().getVodName());
                }
            }
            
            // 4.3 同义词标准化匹配
            if (existingVideo.isEmpty()) {
                String normalizedSynonymName = VideoNameUtil.normalizeVideoName(vodName);
                existingVideo = videoService.findByNormalizedName(normalizedSynonymName);
                if (existingVideo.isPresent()) {
                    matchType = "同义词标准化匹配";
                    log.debug("{} - 同义词标准化匹配成功 ID:{}, 数据库名称: {}, 标准化: {}", 
                        vodName, existingVideo.get().getVodId(), existingVideo.get().getVodName(), normalizedSynonymName);
                }
            }
        }
        
        // 记录匹配结果
        if (existingVideo.isPresent() && matchType != null) {
            log.debug("{} - {} [ID:{}]", originalName, matchType, existingVideo.get().getVodId());
        }

        // 采集模式：1=仅新增 2=仅更新（对齐手动采集与 PHP）
        if (existingVideo.isPresent() && "1".equals(collectOpt)) {
            log.debug("{} - 仅新增模式,视频已存在,跳过", originalName);
            return -1;
        }
        if (existingVideo.isEmpty() && "2".equals(collectOpt)) {
            log.debug("{} - 仅更新模式,视频不存在,跳过", originalName);
            return -1;
        }
        
        // === 智能跳过检查（按 uprule：a=播放地址 d=备注，规则内字段均相同才跳过）===
        if (existingVideo.isPresent() && skipSameTotal == 1) {
            Video existVideo = existingVideo.get();
            String newPlayFrom = (String) videoData.getOrDefault("vod_play_from", "");
            String newPlayUrl = (String) videoData.getOrDefault("vod_play_url", "");
            String[] filteredPlay = filterPlaySources(newPlayFrom, newPlayUrl, filterFrom, collectFilter, true);

            if (shouldSkipByUprule(existVideo, videoData, filteredPlay, upruleList)) {
                log.debug("{} - 智能跳过:更新规则内字段(地址/备注)均未变化", originalName);
                return -1;
            }
        }
        
        // === 构建视频对象并保存 ===
        String newPlayFrom = (String) videoData.getOrDefault("vod_play_from", "");
        String newPlayUrl = (String) videoData.getOrDefault("vod_play_url", "");
        String[] filteredPlay = filterPlaySources(newPlayFrom, newPlayUrl, filterFrom, collectFilter, !existingVideo.isPresent());

        if (existingVideo.isEmpty()) {
            Video video = new Video();
            video.setTypeId(localTypeId);
            video.setVodName(vodName);
            video.setVodPic((String) videoData.getOrDefault("vod_pic", ""));
            video.setVodRemarks(formatRemarks((String) videoData.getOrDefault("vod_remarks", "")));
            video.setVodActor((String) videoData.getOrDefault("vod_actor", ""));
            video.setVodDirector((String) videoData.getOrDefault("vod_director", ""));
            video.setVodArea((String) videoData.getOrDefault("vod_area", ""));
            video.setVodLang((String) videoData.getOrDefault("vod_lang", ""));
            video.setVodYear((String) videoData.getOrDefault("vod_year", ""));
            video.setVodContent((String) videoData.getOrDefault("vod_content", ""));
            video.setVodPlayFrom(filteredPlay[0]);
            video.setVodPlayUrl(filteredPlay[1]);
            video.setVodStatus(1);
            int currentTime = (int) (System.currentTimeMillis() / 1000);
            video.setVodTime(currentTime);
            video.setVodTimeAdd(currentTime);
            videoService.addVideo(video);
            log.info("{} - 新增视频", vodName);
            return 1;
        }

        // 更新：按 uprule 合并线路等字段（对齐手动采集）
        Video video = existingVideo.get();

        if (upruleList.contains("a")) {
            if (!filteredPlay[0].isEmpty() && !filteredPlay[1].isEmpty()) {
                String[] merged = mergePlaySources(
                        video.getVodPlayFrom() != null ? video.getVodPlayFrom() : "",
                        video.getVodPlayUrl() != null ? video.getVodPlayUrl() : "",
                        filteredPlay[0], filteredPlay[1]);
                video.setVodPlayFrom(merged[0]);
                video.setVodPlayUrl(merged[1]);
                log.debug("{} - 合并后播放源: {}", vodName, merged[0]);
            } else {
                log.debug("{} - 过滤后无有效播放源,保留本地线路", vodName);
            }
        }
        if (upruleList.contains("d")) {
            applyRemarksUpdateIfPresent(video, videoData);
        }
        if (upruleList.contains("b")) {
            video.setVodDownFrom((String) videoData.getOrDefault("vod_down_from", ""));
            video.setVodDownUrl((String) videoData.getOrDefault("vod_down_url", ""));
        }
        if (upruleList.contains("c")) {
            video.setVodSerial((String) videoData.getOrDefault("vod_serial", ""));
        }
        if (upruleList.contains("e")) {
            video.setVodDirector((String) videoData.getOrDefault("vod_director", ""));
        }
        if (upruleList.contains("f")) {
            video.setVodActor((String) videoData.getOrDefault("vod_actor", ""));
        }
        if (upruleList.contains("g")) {
            video.setVodYear((String) videoData.getOrDefault("vod_year", ""));
        }
        if (upruleList.contains("h")) {
            video.setVodArea((String) videoData.getOrDefault("vod_area", ""));
        }
        if (upruleList.contains("i")) {
            video.setVodLang((String) videoData.getOrDefault("vod_lang", ""));
        }
        if (upruleList.contains("j")) {
            video.setVodContent((String) videoData.getOrDefault("vod_content", ""));
        }

        video.setVodTime((int) (System.currentTimeMillis() / 1000));
        videoService.updateVideo(video.getVodId(), video);
        log.info("{} - 更新视频", vodName);
        return 0;
    }
    
    /**
     * 按白名单/黑名单过滤播放源,与新增/更新入库逻辑保持一致
     * @return [vodPlayFrom, vodPlayUrl]
     */
    private String[] filterPlaySources(String playFrom, String playUrl, String filterFrom, String collectFilter, boolean forUpdate) {
        if (playFrom == null) playFrom = "";
        if (playUrl == null) playUrl = "";
        playFrom = "null".equals(playFrom) ? "" : playFrom.trim();
        playUrl = "null".equals(playUrl) ? "" : playUrl.trim();

        if (playFrom.isEmpty()) {
            return new String[]{playFrom, playUrl};
        }

        String[] fromArr = playFrom.split("\\$\\$\\$");
        String[] urlArr = playUrl.split("\\$\\$\\$");
        List<String> whitelistFrom = new ArrayList<>();
        List<String> whitelistUrl = new ArrayList<>();
        for (int i = 0; i < fromArr.length; i++) {
            String fromName = fromArr[i].trim();
            if (SYSTEM_PLAYERS.contains(fromName)) {
                whitelistFrom.add(fromArr[i]);
                whitelistUrl.add(i < urlArr.length ? urlArr[i] : "");
            }
        }
        playFrom = String.join("$$$", whitelistFrom);
        playUrl = String.join("$$$", whitelistUrl);

        if (!filterFrom.isEmpty() && !playFrom.isEmpty()) {
            boolean shouldFilter = "1".equals(collectFilter)
                || (forUpdate ? "3".equals(collectFilter) : "2".equals(collectFilter));
            if (shouldFilter) {
                List<String> filterList = new ArrayList<>();
                for (String f : filterFrom.split(",")) {
                    filterList.add(f.trim());
                }
                fromArr = playFrom.split("\\$\\$\\$");
                urlArr = playUrl.split("\\$\\$\\$");
                List<String> filteredFrom = new ArrayList<>();
                List<String> filteredUrl = new ArrayList<>();
                for (int i = 0; i < fromArr.length; i++) {
                    if (!filterList.contains(fromArr[i].trim())) {
                        filteredFrom.add(fromArr[i]);
                        filteredUrl.add(i < urlArr.length ? urlArr[i] : "");
                    }
                }
                playFrom = String.join("$$$", filteredFrom);
                playUrl = String.join("$$$", filteredUrl);
            }
        }
        return new String[]{playFrom, playUrl};
    }

    private List<String> splitPlayList(String str) {
        if (str == null || str.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.asList(str.split("\\$\\$\\$", -1)));
    }

    private boolean isInvalidPlaySource(String name) {
        if (name == null || name.trim().isEmpty()) {
            return true;
        }
        String lower = name.trim().toLowerCase();
        return "no".equals(lower) || "none".equals(lower) || "无".equals(name.trim());
    }

    /**
     * 格式化备注:纯集数备注加上"更新至"前缀(与PHP版一致)
     */
    private String formatRemarks(String remarks) {
        if (remarks == null) {
            return "";
        }
        remarks = remarks.trim();
        if (remarks.isEmpty() || "null".equals(remarks)) {
            return "";
        }
        if (remarks.matches("^第?\\d+[集话期]?$")) {
            return "更新至" + remarks;
        }
        return remarks;
    }

    /**
     * 合并播放源:保留本地已有线路,同名播放源取集数更多的地址
     */
    private String[] mergePlaySources(String existFrom, String existUrl, String newFrom, String newUrl) {
        List<String> existFromArr = splitPlayList(existFrom);
        List<String> existUrlArr = splitPlayList(existUrl);
        List<String> newFromArr = splitPlayList(newFrom);
        List<String> newUrlArr = splitPlayList(newUrl);

        while (existUrlArr.size() < existFromArr.size()) {
            existUrlArr.add("");
        }

        for (int i = 0; i < newFromArr.size(); i++) {
            String fromName = newFromArr.get(i).trim();
            String url = i < newUrlArr.size() ? newUrlArr.get(i) : "";
            if (fromName.isEmpty()) {
                continue;
            }

            int foundIdx = -1;
            for (int j = 0; j < existFromArr.size(); j++) {
                if (existFromArr.get(j).trim().equals(fromName)) {
                    foundIdx = j;
                    break;
                }
            }

            if (foundIdx >= 0) {
                String existUrlItem = foundIdx < existUrlArr.size() ? existUrlArr.get(foundIdx) : "";
                int existEpCount = existUrlItem.isEmpty() ? 0 : existUrlItem.split("#", -1).length;
                int newEpCount = url.isEmpty() ? 0 : url.split("#", -1).length;
                if (newEpCount >= existEpCount) {
                    existUrlArr.set(foundIdx, url);
                }
            } else {
                existFromArr.add(fromName);
                existUrlArr.add(url);
            }
        }
        return new String[]{String.join("$$$", existFromArr), String.join("$$$", existUrlArr)};
    }

    /**
     * 解析站点更新规则列表
     */
    private List<String> parseUpruleList(CollectSite site) {
        String raw = (site != null && site.getUprule() != null && !site.getUprule().isEmpty())
                ? site.getUprule() : "a,d";
        List<String> list = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                list.add(trimmed);
            }
        }
        return list;
    }

    /**
     * 按更新规则智能跳过：规则含 d 时比对备注（双方均有内容才比对），含 a 时比对播放地址
     */
    private boolean shouldSkipByUprule(Video existVideo, Map<String, Object> videoData,
                                       String[] filteredPlay, List<String> upruleList) {
        boolean checkRemarks = upruleList.contains("d");
        boolean checkPlay = upruleList.contains("a");

        if (!checkRemarks && !checkPlay) {
            return false;
        }

        if (checkRemarks) {
            String newRemarks = extractRemarksFromVideoData(videoData);
            String existRemarks = existVideo.getVodRemarks() != null ? existVideo.getVodRemarks().trim() : "";
            // 任一方备注为空：不更新备注，不参与跳过比对
            if (!newRemarks.isEmpty() && !existRemarks.isEmpty()) {
                if (!normalizeRemarksForCompare(existRemarks).equals(normalizeRemarksForCompare(newRemarks))) {
                    return false;
                }
            }
        }

        if (checkPlay) {
            String existPlayFrom = existVideo.getVodPlayFrom() != null ? existVideo.getVodPlayFrom() : "";
            String existPlayUrl = existVideo.getVodPlayUrl() != null ? existVideo.getVodPlayUrl() : "";
            if (!shouldSkipPlayUpdate(existPlayFrom, existPlayUrl, filteredPlay[0], filteredPlay[1])) {
                return false;
            }
        }

        return true;
    }

    private String extractRemarksFromVideoData(Map<String, Object> videoData) {
        String remarks = String.valueOf(videoData.getOrDefault("vod_remarks", "")).trim();
        return "null".equals(remarks) ? "" : remarks;
    }

    private String normalizeRemarksForCompare(String remarks) {
        if (remarks == null) {
            return "";
        }
        return remarks.trim().replaceFirst("^更新至", "");
    }

    private void applyRemarksUpdateIfPresent(Video video, Map<String, Object> videoData) {
        String newRemarks = extractRemarksFromVideoData(videoData);
        String existRemarks = video.getVodRemarks() != null ? video.getVodRemarks().trim() : "";
        // 资源站或本地备注为空时，不更新备注字段
        if (!newRemarks.isEmpty() && !existRemarks.isEmpty()) {
            video.setVodRemarks(formatRemarks(newRemarks));
        }
    }

    /**
     * 智能跳过:仅比较本地与资源站同名的播放源,本地独有线路不参与比较
     */
    private boolean shouldSkipPlayUpdate(String existPlayFrom, String existPlayUrl, String newPlayFrom, String newPlayUrl) {
        List<String> existFromArr = splitPlayList(existPlayFrom);
        List<String> existUrlArr = splitPlayList(existPlayUrl);
        List<String> newFromArr = splitPlayList(newPlayFrom);
        List<String> newUrlArr = splitPlayList(newPlayUrl);

        List<String> validNewFrom = new ArrayList<>();
        List<String> validNewUrl = new ArrayList<>();
        for (int i = 0; i < newFromArr.size(); i++) {
            String fromName = newFromArr.get(i).trim();
            if (!isInvalidPlaySource(fromName)) {
                validNewFrom.add(fromName);
                validNewUrl.add(i < newUrlArr.size() ? newUrlArr.get(i).trim() : "");
            }
        }
        if (validNewFrom.isEmpty()) {
            return true;
        }

        List<String> validExistFrom = new ArrayList<>();
        for (String fromName : existFromArr) {
            if (!isInvalidPlaySource(fromName)) {
                validExistFrom.add(fromName.trim());
            }
        }

        // 本地无线路、资源站有 → 需要写入
        if (validExistFrom.isEmpty()) {
            return false;
        }

        // 资源站有本地没有的新线路 → 需要合并添加
        for (String newFrom : validNewFrom) {
            boolean foundInLocal = false;
            for (String existFrom : validExistFrom) {
                if (newFrom.equalsIgnoreCase(existFrom)) {
                    foundInLocal = true;
                    break;
                }
            }
            if (!foundInLocal) {
                return false;
            }
        }

        boolean hasMatchingSource = false;
        for (String existFrom : validExistFrom) {
            int foundIdx = -1;
            for (int j = 0; j < validNewFrom.size(); j++) {
                if (validNewFrom.get(j).equalsIgnoreCase(existFrom)) {
                    foundIdx = j;
                    break;
                }
            }
            if (foundIdx < 0) {
                continue;
            }

            hasMatchingSource = true;
            String newUrl = validNewUrl.get(foundIdx);

            int existIdx = -1;
            for (int i = 0; i < existFromArr.size(); i++) {
                if (existFromArr.get(i).trim().equalsIgnoreCase(existFrom)) {
                    existIdx = i;
                    break;
                }
            }
            String existUrl = existIdx >= 0 && existIdx < existUrlArr.size() ? existUrlArr.get(existIdx).trim() : "";
            if (!existUrl.equals(newUrl)) {
                return false;
            }
        }

        if (!hasMatchingSource) {
            // 本地线路在资源站均无对应项，但资源站没有新线路名（上面已判断）→ 保留本地，跳过
            return true;
        }
        return true;
    }

    /**
     * 加载定时任务配置
     * @deprecated 已废弃,定时任务配置已整合到站点配置中,请使用 site.getCronConfig()
     */
    @Deprecated
    public CronConfig loadCronConfig(Integer siteId) {
        // 向后兼容:先尝试从站点配置中读取
        CollectSite site = findSiteById(siteId);
        if (site != null && site.getCronConfig() != null) {
            return site.getCronConfig();
        }
        
        // 如果站点配置中没有,则尝试从旧的单独配置文件中读取
        File file = new File("config/collect_cron_" + siteId + ".json");
        if (!file.exists()) {
            return new CronConfig(); // 返回默认配置
        }
        
        try {
            return objectMapper.readValue(file, CronConfig.class);
        } catch (IOException e) {
            log.error("读取定时任务配置失败", e);
            return new CronConfig();
        }
    }
    
    /**
     * 保存定时任务配置
     * @deprecated 已废弃,定时任务配置已整合到站点配置中,请使用 updateSite 方法
     */
    @Deprecated
    public void saveCronConfig(Integer siteId, CronConfig config) {
        // 向后兼容:保存到站点配置中
        CollectSite site = findSiteById(siteId);
        if (site != null) {
            site.setCronConfig(config);
            updateSite(siteId, site);
            log.info("定时任务配置保存成功 - 站点ID: {}", siteId);
        }
    }
}
