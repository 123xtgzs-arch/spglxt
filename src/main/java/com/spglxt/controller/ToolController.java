package com.spglxt.controller;

import com.spglxt.common.Result;
import com.spglxt.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 工具箱控制器
 * 
 * @author spglxt
 */
@Slf4j
@Controller
@RequestMapping("/tool")
@RequiredArgsConstructor
public class ToolController {

    private final VideoService videoService;

    /**
     * 链接生成器页面
     */
    @GetMapping("/link_gen")
    public String linkGenPage(Model model) {
        model.addAttribute("pageTitle", "链接生成器");
        return "tool/link_gen";
    }

    /**
     * 链接对比器页面
     */
    @GetMapping("/link_compare")
    public String linkComparePage(Model model) {
        model.addAttribute("pageTitle", "链接对比器");
        return "tool/link_compare";
    }

    /**
     * 视频合并页面
     */
    @GetMapping("/merge")
    public String mergePage(Model model) {
        model.addAttribute("pageTitle", "视频合并");
        return "tool/merge";
    }

    /**
     * 检测重复视频
     */
    @GetMapping("/merge/search")
    @ResponseBody
    public Result<List<Map<String, Object>>> searchDuplicates() {
        try {
            List<Map<String, Object>> duplicates = videoService.findDuplicateVideos();
            return Result.success(duplicates);
        } catch (Exception e) {
            log.error("检测重复视频失败", e);
            return Result.error("检测失败: " + e.getMessage());
        }
    }

    /**
     * 执行视频合并
     */
    @PostMapping("/merge/do")
    @ResponseBody
    public Result<Void> mergeVideos(@RequestParam Integer targetId, @RequestParam Integer sourceId) {
        try {
            if (targetId == null || sourceId == null) {
                return Result.error("参数错误");
            }
            if (targetId.equals(sourceId)) {
                return Result.error("不能合并自己");
            }
            
            videoService.mergeVideos(targetId, sourceId);
            return Result.success("合并成功");
        } catch (Exception e) {
            log.error("合并视频失败", e);
            return Result.error("合并失败: " + e.getMessage());
        }
    }
}
