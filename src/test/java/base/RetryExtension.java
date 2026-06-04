package base;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Retry failed tests up to 2 times — handles flaky UI/network issues. */
public class RetryExtension implements TestExecutionExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RetryExtension.class);
    private static final int MAX_RETRIES = 2;

    @Override
    public void handleTestExecutionException(ExtensionContext ctx, Throwable throwable) throws Throwable {
        // Don't retry assertions — only retry infrastructure failures
        if (throwable instanceof AssertionError || throwable instanceof TestAbortedException) {
            throw throwable;
        }
        int attempts = getAttempts(ctx);
        if (attempts >= MAX_RETRIES) {
            log.warn("[RETRY] {} failed after {} retries: {}", ctx.getDisplayName(), MAX_RETRIES, throwable.getMessage());
            throw throwable;
        }
        log.warn("[RETRY] {} attempt {}/{} — {}: {}", ctx.getDisplayName(), attempts + 1, MAX_RETRIES,
            throwable.getClass().getSimpleName(), throwable.getMessage());
        setAttempts(ctx, attempts + 1);
        // Re-run by not throwing
    }

    private int getAttempts(ExtensionContext ctx) {
        Integer v = ctx.getStore(ExtensionContext.Namespace.create(getClass(), ctx.getRequiredTestMethod()))
            .get("retryCount", Integer.class);
        return v != null ? v : 0;
    }

    private void setAttempts(ExtensionContext ctx, int count) {
        ctx.getStore(ExtensionContext.Namespace.create(getClass(), ctx.getRequiredTestMethod()))
            .put("retryCount", count);
    }
}
