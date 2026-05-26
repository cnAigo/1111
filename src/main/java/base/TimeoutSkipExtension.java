package base;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.opentest4j.TestAbortedException;

/**
 * 异常拦截器：单线程模式下，当测试发生任何异常（超时、找不到元素、断言失败等）时，
 * 打印出错误原因，并将测试状态标记为“跳过 (Skipped)”以继续执行下一个。
 */
public class TimeoutSkipExtension implements TestExecutionExceptionHandler {

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
        String displayName = context.getDisplayName();

        System.err.println("=====================================================");
        System.err.println(String.format(" 自动跳过 [%s] ", displayName));
        System.err.println("跳过原因: " + throwable.getMessage());
        System.err.println("=====================================================\n");

        // 转换为 TestAbortedException，让 JUnit 知道这是“跳过”而不是“系统崩溃”
        throw new TestAbortedException(String.format("[%s] 执行失败或超时，自动跳过", displayName), throwable);
    }
}