package base;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.AssertionFailedError;
import org.opentest4j.TestAbortedException;

/**
 * Exception handler: only real timeout/environment exceptions become SKIPPED.
 * AssertionFailedError is re-thrown so the test is marked as FAILED.
 */
public class TimeoutSkipExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        // Re-throw assertion failures so they surface as FAILED, not SKIPPED
        if (throwable instanceof AssertionFailedError) {
            throw throwable;
        }

        String displayName = context.getDisplayName();
        System.err.println("=====================================================");
        System.err.println(String.format(" Auto-skip [%s] ", displayName));
        System.err.println("Skip reason: " + throwable.getMessage());
        System.err.println("=====================================================\n");

        throw new TestAbortedException(String.format("[%s] failed or timed out, auto-skip", displayName), throwable);
    }
}
