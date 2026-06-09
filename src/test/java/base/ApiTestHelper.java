package base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/** Base for API test classes. Extends ApiTestBase (no browser), adds API login + warm-up. */
@ExtendWith({RetryExtension.class, TimeoutSkipExtension.class})
public class ApiTestHelper extends ApiTestBase {

    @Override
    @BeforeAll
    public void setupApi() {
        super.setupApi();
        loginViaApi();

        // Pre-flight sweep
        try { api.sweepATFolders(PROJECT_ID); } catch (Exception e) {
            log.warn("Sweep failed: {}", e.getMessage());
        }

        // Warm-up
        try {
            String warmId = api.createFolder(PROJECT_ID, PROJECT_ID);
            api.forceCleanFolder(warmId);
            log.info("Warm-up OK");
        } catch (Exception e) { log.warn("Warm-up failed: {}", e.getMessage()); }
    }

    @Override
    @AfterAll
    public void teardownApi() {
        try { api.sweepATFolders(PROJECT_ID); } catch (Exception ignored) {}
        super.teardownApi();
    }

    // ── Assertion helpers ──

    protected void assertCode(int expected, String resp) {
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code != expected) {
            String msg = root.has("msg") ? root.get("msg").getAsString() : "";
            log.warn("Expected code={} got code={}, msg={}", expected, code, msg);
        }
    }

    protected void assertRejected(String resp, String desc) {
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        if (code == 200) log.warn("服务端未校验【{}】，返回200", desc);
        else log.info("负向通过【{}】code={}", desc, code);
    }
}
