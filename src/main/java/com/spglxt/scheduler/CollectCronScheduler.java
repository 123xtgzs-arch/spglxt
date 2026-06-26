package com.spglxt.scheduler;

import com.spglxt.service.CollectCronService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 定时采集调度器（每分钟检查一次，对齐 PHP task_worker.php）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CollectCronScheduler {

    private final CollectCronService cronService;

    @Scheduled(cron = "0 * * * * *")
    public void tick() {
        try {
            cronService.scheduledTick();
        } catch (Exception e) {
            log.error("定时采集调度失败", e);
        }
    }
}
