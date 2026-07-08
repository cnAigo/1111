package org.example.testvue.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * Configures the async executor dedicated to test-run tasks.
 *
 * Design goals:
 *  (a) Never let a long-running Maven/Playwright test occupy an HTTP thread
 *      — all test execution is dispatched onto this pool.
 *  (b) When the pool and queue are full, reject immediately rather than
 *      running the task on the caller's (HTTP) thread.  {@code CallerRunsPolicy}
 *      would block the HTTP response for the duration of the test (minutes),
 *      causing frontend timeouts.  {@code AbortPolicy} throws immediately so
 *      the caller can return a controlled "server busy" response.
 *  (c) Idle core threads time out after 60 s so the server releases resources
 *      during quiet periods.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean("testExecutor")
    public ThreadPoolTaskExecutor testExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("test-run-");

        // AbortPolicy: throw RejectedExecutionException when the pool and
        // queue are at capacity.  The caller (TestExecutionService.startRun)
        // catches this and marks the task as FAILED in the DB, then returns
        // a busy-signal to the frontend.  This keeps the HTTP thread free
        // and responsive — it never blocks on a 30-minute Maven run.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {
                log.warn("Test-executor rejected a task: pool={}, active={}, queue={}, completed={}",
                        e.getPoolSize(), e.getActiveCount(), e.getQueue().size(), e.getCompletedTaskCount());
                super.rejectedExecution(r, e);
            }
        });

        // Let idle core threads exit after 60 seconds so we don't hold
        // resources during periods with no test activity.
        executor.setAllowCoreThreadTimeOut(true);
        executor.setKeepAliveSeconds(60);

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}
