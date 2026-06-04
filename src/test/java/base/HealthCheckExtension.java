package base;

import config.TestConfig;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.opentest4j.TestAbortedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URI;

/** Aborts all tests if target server is unreachable. */
public class HealthCheckExtension implements BeforeAllCallback {

    private static final Logger log = LoggerFactory.getLogger(HealthCheckExtension.class);
    private static volatile Boolean lastCheck;
    private static volatile long lastCheckTime;

    @Override
    public void beforeAll(ExtensionContext ctx) throws Exception {
        // Cache check for 30 seconds to avoid hammering the server
        if (lastCheck != null && System.currentTimeMillis() - lastCheckTime < 30_000) {
            if (!lastCheck) throw new TestAbortedException("目标服务器不可达（已缓存），跳过测试");
            return;
        }
        try {
            URI uri = new URI(TestConfig.BASE_URL + "/login-api/auth/token/login");
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            conn.disconnect();
            lastCheck = (code > 0); // any response means server is up
            lastCheckTime = System.currentTimeMillis();
            if (!lastCheck) {
                throw new TestAbortedException("目标服务器不可达: " + TestConfig.BASE_URL);
            }
            log.info("Health check OK: {}", TestConfig.BASE_URL);
        } catch (TestAbortedException e) { throw e; }
        catch (Exception e) {
            lastCheck = false; lastCheckTime = System.currentTimeMillis();
            throw new TestAbortedException("目标服务器连接失败: " + TestConfig.BASE_URL + " — " + e.getMessage());
        }
    }
}
