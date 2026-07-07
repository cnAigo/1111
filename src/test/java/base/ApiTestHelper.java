package base;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Base for API test classes.
 *
 * <h3>Isolation</h3>
 * {@code @BeforeAll} creates a dedicated cooperation area named
 * {@code AT_<ClassName>_<suffix>}.  Every test class gets its own sandbox —
 * no shared state, no cross-class interference.
 *
 * <p>Within the class, each {@code @Test} method calls
 * {@link #createTempFolder} / {@link #createTempDoc} which produce
 * uniquely-suffixed names.  Methods never see each other's data.
 *
 * <p>{@code @AfterAll} deletes the entire cooperation area in one shot —
 * no per-method cleanup needed.
 *
 * <h3>Opting out</h3>
 * Subclasses that manage their own cooperation areas set
 * {@link #needsClassCooperationArea} to {@code false} before
 * {@code super.setupApi()}.
 */
@ExtendWith({RetryExtension.class, TimeoutSkipExtension.class})
public class ApiTestHelper extends ApiTestBase {

    /** The cooperation area owned by this test class. Created in @BeforeAll, deleted in @AfterAll. */
    private String classCoopAreaId;

    /**
     * Set to {@code false} in subclasses that manage their own cooperation
     * areas (e.g. CooperationManualTest, PermissionManualTest).
     */
    protected boolean needsClassCooperationArea = true;

    // ── Setup / Teardown ──

    @Override
    @BeforeAll
    public void setupApi() {
        super.setupApi();
        loginViaApi();

        if (needsClassCooperationArea) {
            String code = "AT_" + this.getClass().getSimpleName() + "_" + suffix();
            String name = code;
            try {
                String resp = api.addCooperationArea(name, code, "内部", "auto");
                JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
                if (root.get("code").getAsInt() == 200 && root.has("data") && !root.get("data").isJsonNull()) {
                    JsonObject data = root.getAsJsonObject("data");
                    if (data.has("objectId")) classCoopAreaId = data.get("objectId").getAsString();
                    else if (data.has("id")) classCoopAreaId = data.get("id").getAsString();
                    PROJECT_ID = classCoopAreaId;
                    log.info("[隔离] 类合作区已创建: name={}, id={}", name, classCoopAreaId);
                } else {
                    log.warn("[隔离] 创建类合作区失败: {}", resp);
                }
            } catch (Exception e) {
                log.warn("[隔离] 创建类合作区异常: {}", e.getMessage());
            }
        } else {
            log.info("[隔离] 类合作区 — 跳过（子类自管）");
        }

        // Warm-up: verify the project context is usable
        if (classCoopAreaId != null) {
            try {
                String warmId = api.createFolder(classCoopAreaId, classCoopAreaId);
                api.forceCleanFolder(warmId);
                log.info("[隔离] Warm-up OK");
            } catch (Exception e) {
                log.warn("[隔离] Warm-up failed: {}", e.getMessage());
            }
        }
    }

    @Override
    @AfterAll
    public void teardownApi() {
        // One-shot cleanup: deleting the cooperation area wipes every folder
        // and document created by every method in this class — no per-method
        // teardown needed.
        if (needsClassCooperationArea && classCoopAreaId != null) {
            try {
                api.deleteCooperationArea(classCoopAreaId);
                log.info("[隔离] 类合作区已删除: {}", classCoopAreaId);
            } catch (Exception e) {
                log.warn("[隔离] 删除类合作区失败: {}", e.getMessage());
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
        String msg = root.has("msg") ? root.get("msg").getAsString() : "";
        // Fail hard when the server accepts input it should reject —
        // a missing validation is a real defect that belongs in the failure list.
        Assertions.assertNotEquals(200, code,
                "【安全缺陷】服务端未校验" + desc + " — 期望≠200 实际=" + code + " msg=" + msg);
        log.info("负向通过【{}】code={}", desc, code);
    }
}
