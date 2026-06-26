package com.spglxt.controller;

import com.spglxt.common.Result;
import com.spglxt.entity.Video;
import com.spglxt.service.TypeService;
import com.spglxt.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频管理控制器 - 页面 + API
 * 
 * @author spglxt
 */
@Slf4j
@Controller
@RequestMapping("/video")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;
    private final TypeService typeService;

    /**
     * 视频列表页面
     */
    @GetMapping("/list")
    public String listPage(Model model) {
        model.addAttribute("pageTitle", "视频管理");
        // 获取分类列表用于筛选
        model.addAttribute("types", typeService.getAllTypes(1));
        return "video/list";
    }

    /**
     * 视频编辑页面
     */
    @GetMapping("/edit")
    public String editPage(@RequestParam(required = false) Integer id, Model model) {
        if (id != null) {
            videoService.getVideoById(id).ifPresent(video -> model.addAttribute("video", video));
            model.addAttribute("pageTitle", "编辑视频");
        } else {
            model.addAttribute("pageTitle", "添加视频");
        }
        model.addAttribute("types", typeService.getAllTypes(1));
        return "video/form";
    }

    /**
     * 获取视频列表 API (分页 + 筛选)
     */
    @GetMapping("/api/list")
    @ResponseBody
    public Result<Page<Video>> getVideoList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer typeId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String year,
            @RequestParam(required = false) String area,
            @RequestParam(required = false) String playUrl,
            @RequestParam(defaultValue = "time") String order) {

        Map<String, Object> filters = new HashMap<>();
        if (keyword != null) filters.put("keyword", keyword);
        if (typeId != null) filters.put("typeId", typeId);
        if (status != null) filters.put("status", status);
        if (year != null) filters.put("year", year);
        if (area != null) filters.put("area", area);
        if (playUrl != null) filters.put("playUrl", playUrl);
        filters.put("order", order);

        Page<Video> videoPage = videoService.getVideoList(page, pageSize, filters);
        return Result.success(videoPage);
    }

    /**
     * 获取视频详情 API
     */
    @GetMapping("/api/{id}")
    @ResponseBody
    public Result<Video> getVideo(@PathVariable Integer id) {
        return videoService.getVideoById(id)
                .map(Result::success)
                .orElse(Result.error("视频不存在"));
    }

    /**
     * 添加/更新视频 API
     */
    @PostMapping("/api/save")
    @ResponseBody
    public Result<Video> saveVideo(@RequestBody Video video) {
        if (video.getVodId() != null && video.getVodId() > 0) {
            Video updatedVideo = videoService.updateVideo(video.getVodId(), video);
            return Result.success(updatedVideo, "更新成功");
        } else {
            Video savedVideo = videoService.addVideo(video);
            return Result.success(savedVideo, "添加成功");
        }
    }

    /**
     * 删除视频 API
     */
    @PostMapping("/api/delete")
    @ResponseBody
    public Result<Void> deleteVideo(@RequestParam Integer id) {
        videoService.deleteVideo(id);
        return Result.success("删除成功");
    }

    /**
     * 批量删除视频 API
     */
    @PostMapping("/api/batch/delete")
    @ResponseBody
    public Result<Integer> batchDeleteVideos(@RequestBody List<Integer> ids) {
        int count = videoService.batchDeleteVideos(ids);
        return Result.success(count, "成功删除 " + count + " 个视频");
    }

    /**
     * 批量审核视频 API
     */
    @PostMapping("/api/batch/audit")
    @ResponseBody
    public Result<Integer> batchAuditVideos(
            @RequestBody List<Integer> ids,
            @RequestParam Integer status) {
        int count = videoService.batchAuditVideos(ids, status);
        String message = status == 1 ? "成功审核 " + count + " 个视频" : "成功取消审核 " + count + " 个视频";
        return Result.success(count, message);
    }
}
