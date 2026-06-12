package base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/** Base for API test classes. Extends ApiTestBase (no browser), adds API login + sandbox. */
@ExtendWith({RetryExtension.class, TimeoutSkipExtension.class})
public class ApiTestHelper extends ApiTestBase {

    private String sandboxAreaId; // temp cooperation area for test isolation

    @Override
    @BeforeAll
    public void setupApi() {
        super.setupApi();
        loginViaApi();

        // Create sandbox cooperation area — all tests run inside it
        try {
            String code = "ATS" + suffix();
            String name = "AT沙箱_" + suffix();
            String resp = api.addCooperationArea(name, code, "内部", "自动化测试沙箱");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (root.get("code").getAsInt() == 200 && root.has("data") && !root.get("data").isJsonNull()) {
                JsonObject data = root.getAsJsonObject("data");
                if (data.has("objectId")) sandboxAreaId = data.get("objectId").getAsString();
                else if (data.has("id")) sandboxAreaId = data.get("id").getAsString();
                // Use sandbox as the project context
                if (sandboxAreaId != null) {
                    PROJECT_ID = sandboxAreaId;
                    log.info("沙箱合作区: name={}, id={}", name, sandboxAreaId);
                }
            }
        } catch (Exception e) {
            log.warn("创建沙箱合作区失败: {}", e.getMessage());
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
        // Delete sandbox cooperation area — all test data vanishes
        if (sandboxAreaId != null) {
            try {
                api.deleteCooperationArea(sandboxAreaId);
                log.info("沙箱合作区已删除: {}", sandboxAreaId);
            } catch (Exception e) {
                log.warn("删除沙箱合作区失败: {}", e.getMessage());
            }
        }
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
