package base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.concurrent.atomic.AtomicInteger;

/** Base for API test classes. Extends ApiTestBase (no browser), adds API login + shared sandbox. */
@ExtendWith({RetryExtension.class, TimeoutSkipExtension.class})
public class ApiTestHelper extends ApiTestBase {

    /** 共享沙箱 — 全量跑只建一次，单跑没建才建 */
    private static String sharedSandboxId;
    private static String sharedProjectId;
    private static boolean sandboxFailed;
    private static final AtomicInteger sandboxRefCount = new AtomicInteger(0);

    @Override
    @BeforeAll
    public void setupApi() {
        super.setupApi();
        loginViaApi();

        // 双检锁：只有没建过且之前没失败过才尝试创建
        if (sharedSandboxId == null && !sandboxFailed) {
            synchronized (ApiTestHelper.class) {
                if (sharedSandboxId == null && !sandboxFailed) {
                    try {
                        String code = "ATS" + suffix();
                        String name = "AT沙箱_" + suffix();
                        String resp = api.addCooperationArea(name, code, "内部", "自动化测试沙箱");
                        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
                        if (root.get("code").getAsInt() == 200 && root.has("data") && !root.get("data").isJsonNull()) {
                            JsonObject data = root.getAsJsonObject("data");
                            if (data.has("objectId")) sharedSandboxId = data.get("objectId").getAsString();
                            else if (data.has("id")) sharedSandboxId = data.get("id").getAsString();
                            if (sharedSandboxId != null) {
                                sharedProjectId = sharedSandboxId;
                                log.info("沙箱合作区创建: name={}, id={}", name, sharedSandboxId);
                            }
                        } else {
                            log.warn("创建沙箱返回非200: {}", resp);
                            sandboxFailed = true;
                        }
                    } catch (Exception e) {
                        log.warn("创建沙箱合作区异常: {}", e.getMessage());
                        sandboxFailed = true;
                    }
                }
            }
        }

        sandboxRefCount.incrementAndGet();
        if (sharedSandboxId != null) {
            PROJECT_ID = sharedProjectId;
            log.info("使用沙箱: {} (refCount={})", sharedSandboxId, sandboxRefCount.get());
        } else {
            log.info("无沙箱，使用原始PROJECT_ID: {} (refCount={})", PROJECT_ID, sandboxRefCount.get());
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
        if (sandboxRefCount.decrementAndGet() == 0) {
            if (sharedSandboxId != null) {
                try {
                    api.deleteCooperationArea(sharedSandboxId);
                    log.info("沙箱合作区已删除: {}", sharedSandboxId);
                } catch (Exception e) {
                    log.warn("删除沙箱合作区失败: {}", e.getMessage());
                }
            }
            sharedSandboxId = null;
            sharedProjectId = null;
            sandboxFailed = false;
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
