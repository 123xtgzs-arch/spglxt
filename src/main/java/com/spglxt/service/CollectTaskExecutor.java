package com.spglxt.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 采集任务后台执行器
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CollectTaskExecutor {

    private final CollectService collectService;

    /**
     * 在后台循环执行采集任务，直到完成或失败
     */
    @Async
    public void runTaskUntilDone(String taskId) {
        log.info("后台开始执行采集任务: {}", taskId);
        try {
            while (true) {
                Map<String, Object> result = collectService.executeTaskPage(taskId);
                if (Boolean.TRUE.equals(result.get("done"))) {
                    log.info("采集任务已完成: {}", taskId);
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("采集任务被中断: {}", taskId);
        } catch (Exception e) {
            log.error("后台执行采集任务失败: {}", taskId, e);
        }
    }
}
