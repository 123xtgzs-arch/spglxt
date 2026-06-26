package com.spglxt.controller;

import com.spglxt.entity.Type;
import com.spglxt.entity.Video;
import com.spglxt.service.TypeService;
import com.spglxt.service.VideoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 仪表盘控制器
 * 
 * @author spglxt
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final VideoService videoService;
    private final TypeService typeService;

    /**
     * 仪表盘首页
     */
    @GetMapping({"/", "/dashboard"})
    public String index(Model model) {
        // 获取统计数据
        Map<String, Long> stats = videoService.getStats();
        
        // 获取分类数量
        List<Type> types = typeService.getAllTypes(1);
        long typeCount = types.size();
        
        // 获取最近更新的视频 (前10条)
        Map<String, Object> filters = new HashMap<>();
        filters.put("order", "time");
        Page<Video> recentVideos = videoService.getVideoList(1, 10, filters);
        
        model.addAttribute("stats", stats);
        model.addAttribute("typeCount", typeCount);
        model.addAttribute("recentVideos", recentVideos);
        
        return "dashboard";
    }
}
