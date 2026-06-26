package com.spglxt.controller;

import com.spglxt.common.Result;
import com.spglxt.dto.CollectSite;
import com.spglxt.dto.CollectTask;
import com.spglxt.dto.CronConfig;
import com.spglxt.entity.Type;
import com.spglxt.service.CollectCronService;
import com.spglxt.service.CollectService;
import com.spglxt.service.CollectTaskExecutor;
import com.spglxt.service.TypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 采集管理控制器
 * 
 * @author spglxt
 */
@Slf4j
@Controller
@RequestMapping("/collect")
@RequiredArgsConstructor
public class CollectController {

    private final CollectService collectService;
    private final CollectCronService collectCronService;
    private final CollectTaskExecutor collectTaskExecutor;
    private final TypeService typeService;

    /**
     * 采集站点列表页面
     */
    @GetMapping("/list")
    public String list(Model model) {
        List<CollectSite> sites = collectService.getAllSites();
        model.addAttribute("sites", sites);
        return "collect/list";
    }

    /**
     * 添加采集站点页面
     */
    @GetMapping("/add")
    public String addForm(Model model) {
        model.addAttribute("site", null);
        return "collect/form";
    }

    /**
     * 保存采集站点
     */
    @PostMapping("/add")
    public String addSite(CollectSite site, @RequestParam(required = false) List<String> uprule) {
        // 处理更新规则复选框数组
        if (uprule != null && !uprule.isEmpty()) {
            site.setUprule(String.join(",", uprule));
        } else {
            site.setUprule("");
        }
        
        // 处理智能更新复选框
        if (site.getSkipSameTotal() == null) {
            site.setSkipSameTotal(0);
        }
        
        collectService.addSite(site);
        return "redirect:/collect/list";
    }

    /**
     * 编辑采集站点页面
     */
    @GetMapping("/edit/{id}")
    public String editForm(@PathVariable Integer id, Model model) {
        CollectSite site = collectService.findSiteById(id);
        if (site == null) {
            return "redirect:/collect/list";
        }
        model.addAttribute("site", site);
        return "collect/form";
    }

    /**
     * 更新采集站点
     */
    @PostMapping("/edit/{id}")
    public String updateSite(@PathVariable Integer id, CollectSite site, @RequestParam(required = false) List<String> uprule) {
        // 处理更新规则复选框数组
        if (uprule != null && !uprule.isEmpty()) {
            site.setUprule(String.join(",", uprule));
        } else {
            site.setUprule("");
        }
        
        // 处理智能更新复选框
        if (site.getSkipSameTotal() == null) {
            site.setSkipSameTotal(0);
        }
        
        collectService.updateSite(id, site);
        return "redirect:/collect/list";
    }

    /**
     * 删除采集站点
     */
    @GetMapping("/delete/{id}")
    public String deleteSite(@PathVariable Integer id) {
        collectService.deleteSite(id);
        return "redirect:/collect/list";
    }

    /**
     * 采集执行页面
     */
    @GetMapping("/run/{id}")
    public String run(@PathVariable Integer id, Model model) {
        CollectSite site = collectService.findSiteById(id);
        if (site == null) {
            return "redirect:/collect/list";
        }
        
        List<Type> types = typeService.getAllTypes(1);
        Map<String, Integer> bindConfig = site.getBindConfig(); // 从站点配置中获取
        
        model.addAttribute("site", site);
        model.addAttribute("types", types);
        model.addAttribute("bindConfig", bindConfig);
        
        return "collect/run";
    }

    /**
     * 分类绑定页面
     */
    @GetMapping("/bind/{id}")
    public String bind(@PathVariable Integer id, Model model) {
        CollectSite site = collectService.findSiteById(id);
        if (site == null) {
            return "redirect:/collect/list";
        }
        
        List<Type> types = typeService.getAllTypes(1);
        Map<String, Integer> bindConfig = site.getBindConfig(); // 从站点配置中获取
        
        model.addAttribute("site", site);
        model.addAttribute("types", types);
        model.addAttribute("bindConfig", bindConfig);
        
        return "collect/bind";
    }

    /**
     * 保存分类绑定
     */
    @PostMapping("/bind/{id}")
    @ResponseBody
    public Result<Void> saveBind(@PathVariable Integer id, @RequestBody Map<String, Integer> binds) {
        // 获取站点配置
        CollectSite site = collectService.findSiteById(id);
        if (site == null) {
            return Result.error("站点不存在");
        }
        
        // 更新站点的分类绑定配置
        site.setBindConfig(binds);
        collectService.updateSite(id, site);
        
        return Result.success("保存成功");
    }

    /**
     * 名称同义库页面
     */
    @GetMapping("/namewords")
    public String namewords(Model model) {
        String namewords = collectService.loadNamewords();
        model.addAttribute("namewords", namewords);
        return "collect/namewords";
    }

    /**
     * 定时采集配置页面
     */
    @GetMapping("/cron/{siteId}")
    public String cron(@PathVariable Integer siteId, Model model) {
        CollectSite site = collectService.findSiteById(siteId);
        if (site == null) {
            return "redirect:/collect/list";
        }
        
        // 从站点配置中加载定时任务配置
        CronConfig cronConfig = site.getCronConfig();
        
        model.addAttribute("site", site);
        model.addAttribute("cronConfig", cronConfig);
        
        return "collect/cron";
    }
    
    /**
     * 保存定时采集配置
     */
    @PostMapping("/cron/save")
    public String saveCron(
            @RequestParam Integer siteId,
            @RequestParam Integer enabled,
            @RequestParam String mode,
            @RequestParam Integer interval,
            @RequestParam String cronHours,
            @RequestParam String hours,
            @RequestParam String typeId) {
        
        // 获取站点配置
        CollectSite site = collectService.findSiteById(siteId);
        if (site == null) {
            return "redirect:/collect/list";
        }
        
        CronConfig config = new CronConfig();
        config.setEnabled(enabled);
        config.setMode(mode);
        config.setInterval(interval);
        config.setCronHours(cronHours);
        config.setHours(hours);
        config.setTypeId(typeId);
        
        // 保留上次执行时间
        CronConfig oldConfig = site.getCronConfig();
        if (oldConfig != null && oldConfig.getLastRun() != null) {
            config.setLastRun(oldConfig.getLastRun());
        }
        
        // 更新站点的定时任务配置
        site.setCronConfig(config);
        collectService.updateSite(siteId, site);
        
        return "redirect:/collect/cron/" + siteId;
    }
    
    /**
     * 测试立即执行采集
     */
    @GetMapping("/cron/test/{siteId}")
    @ResponseBody
    public Result<String> cronTest(@PathVariable Integer siteId) {
        try {
            if (collectService.findSiteById(siteId) == null) {
                return Result.error("站点不存在");
            }
            String taskId = collectService.createCronTask(siteId, 100, "cron");
            collectTaskExecutor.runTaskUntilDone(taskId);
            return Result.success("采集任务已创建并在后台执行，请前往\"采集任务\"页面查看进度", taskId);
        } catch (Exception e) {
            log.error("创建采集任务失败", e);
            return Result.error("创建任务失败: " + e.getMessage());
        }
    }

    /**
     * 定时采集执行入口（宝塔面板访问 URL 触发，对齐 PHP collect_cron_run）
     */
    @GetMapping("/cron/run")
    @ResponseBody
    public Object cronRun(
            @RequestParam(required = false) Integer siteId,
            @RequestParam(name = "site_id", required = false) Integer siteIdAlt,
            @RequestParam(name = "max_pages", defaultValue = "100") int maxPages,
            @RequestParam(defaultValue = "1") int async,
            @RequestParam(defaultValue = "json") String format) {
        Integer targetSiteId = siteId != null ? siteId : siteIdAlt;
        try {
            Map<String, Object> data = collectCronService.triggerCronRun(targetSiteId, maxPages, async == 1);
            if ("text".equalsIgnoreCase(format)) {
                StringBuilder sb = new StringBuilder();
                sb.append("===== 采集任务触发成功 =====\n");
                sb.append("站点数: ").append(data.get("count")).append("\n");
                sb.append("执行模式: ").append(async == 1 ? "异步后台执行" : "同步执行").append("\n");
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> results = (List<Map<String, Object>>) data.get("results");
                if (results != null) {
                    for (int i = 0; i < results.size(); i++) {
                        Map<String, Object> r = results.get(i);
                        sb.append(i + 1).append(". ")
                                .append(r.get("site")).append(" - ")
                                .append(r.get("status"));
                        if (r.get("task_id") != null) {
                            sb.append(" (任务ID: ").append(r.get("task_id")).append(")");
                        }
                        sb.append("\n");
                    }
                }
                sb.append("===========================\n");
                return sb.toString();
            }
            Map<String, Object> body = new HashMap<>();
            body.put("code", 0);
            body.put("msg", async == 1 ? "任务已创建并开始后台执行" : "任务已创建");
            body.put("data", data);
            return body;
        } catch (Exception e) {
            log.error("定时采集触发失败", e);
            if ("text".equalsIgnoreCase(format)) {
                return "采集任务触发失败: " + e.getMessage();
            }
            return Result.error("触发失败: " + e.getMessage());
        }
    }

    /**
     * 保存名称同义库
     */
    @PostMapping("/namewords")
    public String saveNamewords(@RequestParam String namewords) {
        collectService.saveNamewords(namewords);
        return "redirect:/collect/namewords";
    }

    /**
     * 采集任务管理页面
     */
    @GetMapping("/tasks")
    public String tasks(Model model) {
        List<CollectSite> sites = collectService.getAllSites();
        model.addAttribute("sites", sites);
        return "collect/tasks";
    }

    /**
     * 获取所有任务状态 (API)
     */
    @GetMapping("/task/status")
    @ResponseBody
    public Result<List<CollectTask>> getTaskStatus() {
        List<CollectTask> tasks = collectService.getAllTasks();
        return Result.success(tasks);
    }

    /**
     * 创建并启动新任务 (API)
     */
    @PostMapping("/task/start")
    @ResponseBody
    public Result<String> startTask(
            @RequestParam(value = "site_id", required = false) Integer siteIdUnderscore,
            @RequestParam(value = "siteId", required = false) Integer siteIdCamel) {
        // 兼容两种参数名称格式
        Integer siteId = siteIdCamel != null ? siteIdCamel : siteIdUnderscore;
        if (siteId == null) {
            return Result.error("缺少site_id或siteId参数");
        }
        
        try {
            String taskId = collectService.createTask(siteId);
            collectTaskExecutor.runTaskUntilDone(taskId);
            return Result.success("任务已在服务端后台执行，可关闭页面", taskId);
        } catch (Exception e) {
            log.error("创建任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 恢复后台执行（用于离开页面后中断的任务）
     */
    @PostMapping("/task/resume")
    @ResponseBody
    public Result<String> resumeTask(
            @RequestParam(value = "task_id", required = false) String taskIdUnderscore,
            @RequestParam(value = "taskId", required = false) String taskIdCamel) {
        String taskId = taskIdCamel != null ? taskIdCamel : taskIdUnderscore;
        if (taskId == null || taskId.isEmpty()) {
            return Result.error("缺少task_id或taskId参数");
        }
        try {
            collectService.resumeTaskInBackground(taskId);
            collectTaskExecutor.runTaskUntilDone(taskId);
            return Result.success("任务已恢复后台执行", taskId);
        } catch (Exception e) {
            log.error("恢复任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 执行任务的一页采集 (API)
     */
    @GetMapping("/task/exec")
    @ResponseBody
    public Result<Map<String, Object>> executeTask(
            @RequestParam(value = "task_id", required = false) String taskIdUnderscore,
            @RequestParam(value = "taskId", required = false) String taskIdCamel) {
        // 兼容两种参数名称格式
        String taskId = taskIdCamel != null ? taskIdCamel : taskIdUnderscore;
        if (taskId == null || taskId.isEmpty()) {
            return Result.error("缺少task_id或taskId参数");
        }
        
        try {
            Map<String, Object> result = collectService.executeTaskPage(taskId);
            return Result.success(result);
        } catch (Exception e) {
            log.error("执行任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 停止任务 (API)
     */
    @PostMapping("/task/stop")
    @ResponseBody
    public Result<Void> stopTask(
            @RequestParam(value = "task_id", required = false) String taskIdUnderscore,
            @RequestParam(value = "taskId", required = false) String taskIdCamel) {
        // 兼容两种参数名称格式
        String taskId = taskIdCamel != null ? taskIdCamel : taskIdUnderscore;
        if (taskId == null || taskId.isEmpty()) {
            return Result.error("缺少task_id或taskId参数");
        }
        
        collectService.stopTask(taskId);
        return Result.success("任务已停止");
    }

    /**
     * 清空所有任务 (API)
     */
    @PostMapping("/task/clear")
    @ResponseBody
    public Result<Void> clearTasks() {
        collectService.clearAllTasks();
        return Result.success("已清空所有任务");
    }

    /**
     * 采集API - 处理各种采集操作
     */
    @RequestMapping(value = "/api", method = {RequestMethod.GET, RequestMethod.POST})
    @ResponseBody
    public Result<?> api(
            @RequestParam String act,
            @RequestParam(required = false) Integer siteId,
            @RequestParam(defaultValue = "1") int pg,
            @RequestParam(required = false) String t,
            @RequestParam(required = false) String h,
            @RequestParam(required = false) String wd,
            @RequestParam(required = false) String ids,
            @RequestBody(required = false) Map<String, Object> body) {
        
        if ("fetch_list".equals(act)) {
            return handleFetchList(siteId, pg, t, h, wd);
        } else if ("fetch_detail".equals(act)) {
            return handleFetchDetail(siteId, ids);
        } else if ("import".equals(act)) {
            return handleImport(body);
        }
        
        return Result.error("未知操作");
    }

    /**
     * 获取资源列表
     */
    private Result<?> handleFetchList(Integer siteId, int pg, String t, String h, String wd) {
        CollectSite site = collectService.findSiteById(siteId);
        if (site == null) {
            return Result.error("站点不存在");
        }
        
        Map<String, String> params = new HashMap<>();
        params.put("ac", "list");
        params.put("pg", String.valueOf(pg));
        if (t != null && !t.isEmpty()) params.put("t", t);
        if (h != null && !h.isEmpty()) params.put("h", h);
        if (wd != null && !wd.isEmpty()) params.put("wd", wd);
        
        try {
            Map<String, Object> data = collectService.fetchAndParseList(site, params);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取资源列表失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 获取资源详情
     */
    private Result<?> handleFetchDetail(Integer siteId, String ids) {
        CollectSite site = collectService.findSiteById(siteId);
        if (site == null) {
            return Result.error("站点不存在");
        }
        
        if (ids == null || ids.isEmpty()) {
            return Result.error("ids参数不能为空");
        }
        
        Map<String, String> params = new HashMap<>();
        params.put("ac", "detail");
        params.put("ids", ids);
        
        try {
            Map<String, Object> data = collectService.fetchAndParseList(site, params);
            return Result.success(data);
        } catch (Exception e) {
            log.error("获取资源详情失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 导入视频数据
     */
    private Result<?> handleImport(Map<String, Object> body) {
        if (body == null || !body.containsKey("videos")) {
            return Result.error("无数据");
        }
        
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> videos = (List<Map<String, Object>>) body.get("videos");
            Integer siteId = body.containsKey("siteId") ? 
                Integer.valueOf(String.valueOf(body.get("siteId"))) : 0;
            String mode = body.containsKey("mode") ? 
                String.valueOf(body.get("mode")) : "all";
            
            Map<String, Object> result = collectService.importVideos(videos, siteId, mode);
            
            int addCount = (Integer) result.getOrDefault("add", 0);
            int updateCount = (Integer) result.getOrDefault("update", 0);
            int skipCount = (Integer) result.getOrDefault("skip", 0);
            
            String msg = String.format("导入完成: 新增 %d, 更新 %d, 跳过 %d", addCount, updateCount, skipCount);
            return Result.success(result, msg);
        } catch (Exception e) {
            log.error("导入视频失败", e);
            return Result.error("导入失败: " + e.getMessage());
        }
    }
}
