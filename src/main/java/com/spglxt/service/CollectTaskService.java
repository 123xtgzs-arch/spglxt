package com.spglxt.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spglxt.dto.CollectTask;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 采集任务管理服务
 * 
 * @author spglxt
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectTaskService {

    private final ObjectMapper objectMapper;
    
    /**
     * 内存中的任务存储 (实际项目中应该使用数据库或Redis)
     */
    private final Map<String, CollectTask> tasks = new ConcurrentHashMap<>();
    
    private static final String TASKS_FILE = "config/collect_tasks.json";
    
    /**
     * 获取所有任务
     */
    public List<CollectTask> getAllTasks() {
        // 从文件加载任务(如果内存中为空)
        if (tasks.isEmpty()) {
            loadTasksFromFile();
        }
        
        return tasks.values().stream()
                .sorted((t1, t2) -> t2.getCreatedAt().compareTo(t1.getCreatedAt()))
                .collect(Collectors.toList());
    }
    
    /**
     * 根据ID获取任务
     */
    public CollectTask getTask(String taskId) {
        return tasks.get(taskId);
    }
    
    /**
     * 创建新任务（手动采集，待前端推进）
     */
    public CollectTask createTask(Integer siteId, String siteName) {
        if (tasks.isEmpty()) {
            loadTasksFromFile();
        }

        CollectTask task = new CollectTask();
        task.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        task.setSiteId(siteId);
        task.setSiteName(siteName);
        task.setStatus("pending");
        task.setCreatedAt(System.currentTimeMillis());
        task.setStartTime(getCurrentTime());
        task.setProgress("0/0");
        task.setAutoExecute(false);
        task.setSource("manual");

        task.addLog("📋 任务已创建");

        tasks.put(task.getId(), task);
        saveTasksToFile();

        return task;
    }

    /**
     * 标记任务为服务端后台执行（离开页面不中断）
     */
    public void markBackgroundExecution(String taskId) {
        CollectTask task = tasks.get(taskId);
        if (task == null) {
            loadTasksFromFile();
            task = tasks.get(taskId);
        }
        if (task != null) {
            task.setAutoExecute(true);
            if ("pending".equals(task.getStatus())) {
                task.setStatus("running");
            }
            task.addLog("🚀 已切换为服务端后台执行，可关闭页面");
            saveTasksToFile();
        }
    }

    /**
     * 创建定时采集任务（带采集参数，立即进入 running）
     */
    public CollectTask createCronTask(Integer siteId, String siteName, String hours, String typeId,
                                      int maxPages, String source) {
        if (tasks.isEmpty()) {
            loadTasksFromFile();
        }

        CollectTask task = new CollectTask();
        task.setId(UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        task.setSiteId(siteId);
        task.setSiteName(siteName);
        task.setStatus("running");
        task.setCreatedAt(System.currentTimeMillis());
        task.setStartTime(getCurrentTime());
        task.setProgress("0/0");
        task.setAutoExecute(true);
        task.setHours(hours);
        task.setTypeId(typeId);
        task.setMaxPages(maxPages > 0 ? maxPages : null);
        task.setSource(source);

        task.addLog("📋 任务已创建" + ("cron".equals(source) || "scheduler".equals(source) ? "（定时采集）" : ""));
        if (hours != null && !hours.isEmpty()) {
            task.addLog("⏱ 采集范围: " + hours + " 小时内更新");
        } else {
            task.addLog("⏱ 采集范围: 不限");
        }
        if (typeId != null && !typeId.isEmpty()) {
            task.addLog("📂 指定分类: " + typeId);
        }

        tasks.put(task.getId(), task);
        saveTasksToFile();

        return task;
    }

    /**
     * 站点是否已有运行中的任务
     */
    public boolean hasRunningTaskForSite(Integer siteId) {
        if (tasks.isEmpty()) {
            loadTasksFromFile();
        }
        return tasks.values().stream()
                .anyMatch(t -> siteId.equals(t.getSiteId()) && "running".equals(t.getStatus()));
    }

    /**
     * 获取最早创建的 running 任务（用于每分钟推进一页）
     */
    public CollectTask findFirstRunningTask() {
        if (tasks.isEmpty()) {
            loadTasksFromFile();
        }
        return tasks.values().stream()
                .filter(t -> "running".equals(t.getStatus()))
                .min(Comparator.comparing(CollectTask::getCreatedAt))
                .orElse(null);
    }
    
    /**
     * 更新任务状态
     */
    public void updateTaskStatus(String taskId, String status) {
        CollectTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus(status);
            if ("completed".equals(status) || "stopped".equals(status) || "failed".equals(status)) {
                task.setEndTime(getCurrentTime());
            }
            saveTasksToFile();
        }
    }
    
    /**
     * 更新任务进度
     */
    public void updateTaskProgress(String taskId, int currentPage, int totalPages) {
        CollectTask task = tasks.get(taskId);
        if (task != null) {
            task.setCurrentPage(currentPage);
            task.setTotalPages(totalPages);
            task.setProgress(currentPage + "/" + totalPages);
            saveTasksToFile();
        }
    }
    
    /**
     * 增加任务计数
     */
    public void incrementTaskCount(String taskId, boolean isAdd) {
        CollectTask task = tasks.get(taskId);
        if (task != null) {
            if (isAdd) {
                task.setAddCount(task.getAddCount() + 1);
            } else {
                task.setUpdateCount(task.getUpdateCount() + 1);
            }
            saveTasksToFile();
        }
    }
    
    /**
     * 添加任务日志
     */
    public void addTaskLog(String taskId, String log) {
        CollectTask task = tasks.get(taskId);
        if (task != null) {
            task.addLog(log);
            saveTasksToFile();
        }
    }
    
    /**
     * 停止任务
     */
    public void stopTask(String taskId) {
        CollectTask task = tasks.get(taskId);
        if (task != null) {
            task.setStatus("stopped");
            task.setEndTime(getCurrentTime());
            task.addLog("⏸️ 任务已手动停止");
            saveTasksToFile();
        }
    }
    
    /**
     * 清空所有任务
     */
    public void clearAllTasks() {
        tasks.clear();
        saveTasksToFile();
    }
    
    /**
     * 从文件加载任务
     */
    private void loadTasksFromFile() {
        File file = new File(TASKS_FILE);
        if (!file.exists()) {
            return;
        }
        
        try {
            List<CollectTask> taskList = objectMapper.readValue(file, new TypeReference<List<CollectTask>>() {});
            for (CollectTask task : taskList) {
                tasks.put(task.getId(), task);
            }
            log.info("从文件加载任务: {} 个", taskList.size());
        } catch (IOException e) {
            log.error("加载任务文件失败", e);
        }
    }
    
    /**
     * 保存任务到文件
     */
    private void saveTasksToFile() {
        File file = new File(TASKS_FILE);
        File dir = file.getParentFile();
        
        if (!dir.exists()) {
            dir.mkdirs();
        }
        
        try {
            List<CollectTask> taskList = new ArrayList<>(tasks.values());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, taskList);
        } catch (IOException e) {
            log.error("保存任务文件失败", e);
        }
    }
    
    /**
     * 获取当前时间字符串
     */
    private String getCurrentTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }
}
