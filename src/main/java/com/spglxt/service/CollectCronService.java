package com.spglxt.service;

import com.spglxt.dto.CollectSite;
import com.spglxt.dto.CronConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 定时采集调度服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectCronService {

    private static final int DEFAULT_MAX_PAGES = 100;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private final CollectService collectService;
    private final CollectTaskService taskService;
    private final CollectTaskExecutor taskExecutor;

    /**
     * 每分钟调度：到点创建任务并由服务端后台执行
     */
    public void scheduledTick() {
        createScheduledTasks();
    }

    /**
     * 宝塔 URL 触发采集（对齐 PHP collect_cron_run）
     */
    public Map<String, Object> triggerCronRun(Integer siteId, int maxPages, boolean async) {
        List<CollectSite> sites = collectService.getAllSites();
        List<Map<String, Object>> results = new ArrayList<>();
        List<String> taskIds = new ArrayList<>();

        for (CollectSite site : sites) {
            if (siteId != null && !siteId.equals(site.getId())) {
                continue;
            }

            CronConfig cronConfig = site.getCronConfig();
            if (siteId == null && (cronConfig.getEnabled() == null || cronConfig.getEnabled() != 1)) {
                continue;
            }

            if (taskService.hasRunningTaskForSite(site.getId())) {
                results.add(resultEntry(site.getName(), null, "已有运行中的任务，跳过"));
                continue;
            }

            String taskId = collectService.createCronTask(site.getId(), maxPages, "cron");
            taskIds.add(taskId);

            if (async) {
                taskExecutor.runTaskUntilDone(taskId);
                results.add(resultEntry(site.getName(), taskId, "后台执行中"));
            } else {
                taskExecutor.runTaskUntilDone(taskId);
                results.add(resultEntry(site.getName(), taskId, "执行完成"));
            }
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("count", results.size());
        data.put("mode", async ? "async" : "sync");
        data.put("task_ids", taskIds);
        data.put("results", results);
        return data;
    }

    private void createScheduledTasks() {
        for (CollectSite site : collectService.getAllSites()) {
            CronConfig config = site.getCronConfig();
            if (config.getEnabled() == null || config.getEnabled() != 1) {
                continue;
            }
            if (!shouldRunBySchedule(config)) {
                continue;
            }
            if (taskService.hasRunningTaskForSite(site.getId())) {
                log.debug("站点{}已有运行中的任务，跳过创建", site.getId());
                continue;
            }

            String taskId = collectService.createCronTask(site.getId(), DEFAULT_MAX_PAGES, "scheduler");
            taskExecutor.runTaskUntilDone(taskId);
            log.info("定时采集：已为站点 {} 创建任务 {}", site.getName(), taskId);
        }
    }

    /**
     * 判断站点是否到达计划执行时间
     */
    public boolean shouldRunBySchedule(CronConfig config) {
        if (config.getEnabled() == null || config.getEnabled() != 1) {
            return false;
        }

        long now = System.currentTimeMillis() / 1000;
        long lastRun = config.getLastRun() != null ? config.getLastRun() : 0;

        if ("cron".equalsIgnoreCase(config.getMode())) {
            String currentSlot = LocalTime.now().format(TIME_FMT);
            if (!parseCronSlots(config.getCronHours()).contains(currentSlot)) {
                return false;
            }
            return lastRun == 0 || !isSameMinute(lastRun, now);
        }

        int intervalHours = config.getInterval() != null ? config.getInterval() : 6;
        return lastRun == 0 || (now - lastRun) >= intervalHours * 3600L;
    }

    private List<String> parseCronSlots(String cronHours) {
        if (cronHours == null || cronHours.trim().isEmpty()) {
            return Collections.emptyList();
        }
        List<String> slots = new ArrayList<>();
        for (String part : cronHours.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            if (part.contains(":")) {
                String[] hm = part.split(":", 2);
                try {
                    int h = Integer.parseInt(hm[0].trim());
                    int m = hm.length > 1 ? Integer.parseInt(hm[1].trim()) : 0;
                    slots.add(String.format("%02d:%02d", h, m));
                } catch (NumberFormatException ignored) {
                    // skip invalid slot
                }
            } else {
                try {
                    int h = Integer.parseInt(part);
                    slots.add(String.format("%02d:00", h));
                } catch (NumberFormatException ignored) {
                    // skip invalid slot
                }
            }
        }
        return slots;
    }

    private boolean isSameMinute(long lastRunSec, long nowSec) {
        return lastRunSec / 60 == nowSec / 60;
    }

    private Map<String, Object> resultEntry(String siteName, String taskId, String status) {
        Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("site", siteName);
        entry.put("task_id", taskId);
        entry.put("status", status);
        return entry;
    }
}
