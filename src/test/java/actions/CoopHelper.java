package actions;

import base.ApiTestBase;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Cooperation area test helper — every test body becomes one line.
 * Usage: initialize in @BeforeAll, then call one-liners in @Test.
 */
public class CoopHelper {

    private static final Logger log = LoggerFactory.getLogger(CoopHelper.class);

    private final ReqApiActions api;
    private final Supplier<String> sfx;
    private final String projectId;

    public CoopHelper(ReqApiActions api, Supplier<String> suffix, String projectId) {
        this.api = api;
        this.sfx = suffix;
        this.projectId = projectId;
    }

    // ── Create ──

    /** Create area: name="AT_{tag}_{sfx}", code="AT_{tag}C_{sfx}", assert 200, return areaId */
    public String ok(String tag) {
        String n = "AT_" + tag + "_" + sfx.get();
        String c = "AT_" + tag + "C_" + sfx.get();
        String r = api.addCooperationArea(n, c, "内部", "auto");
        assertCode(r, 200);
        return find(n);
    }

    /** Create with explicit name/code, assert 200, return areaId */
    public String ok(String name, String code) {
        String r = api.addCooperationArea(name, code, "内部", "auto");
        assertCode(r, 200);
        return find(name);
    }

    /** Create with explicit name/code/secLevel/desc, assert 200, return areaId */
    public String ok(String name, String code, String secLevel, String desc) {
        String r = api.addCooperationArea(name, code, secLevel, desc);
        assertCode(r, 200);
        return find(name);
    }

    /** Create and expect failure with keyword. Returns the response code for optional further checking. */
    public int fail(String name, String code, String keyword) {
        String r = api.addCooperationArea(name, code, "内部", "");
        assertRejected(r, keyword);
        return JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt();
    }

    /** First create OK, then create again with same name+code → expect specific code+msg */
    public void failExact(String name, String code, int expectedCode, String expectedMsg) {
        api.addCooperationArea(name, code, "内部", "auto"); // first OK
        String r = api.addCooperationArea(name, code, "内部", "auto"); // second fails
        JsonObject resp = JsonParser.parseString(r).getAsJsonObject();
        Assertions.assertEquals(expectedCode, resp.get("code").getAsInt());
        Assertions.assertEquals(expectedMsg, resp.get("msg").getAsString());
    }

    // ── Update ──

    /** Update area, assert 200 */
    public void update(String areaId, String newName, String newCode) {
        assertCode(api.updateCooperationArea(areaId, newName, newCode, "内部", "updated"), 200);
    }

    /** Update area, no assertion (some updates return 500 for business reasons) */
    public void updateRaw(String areaId, String newName, String newCode) {
        api.updateCooperationArea(areaId, newName, newCode, "内部", "updated");
    }

    /** Update area and expect failure with keyword */
    public void updateFail(String areaId, String newName, String newCode, String keyword) {
        String r = api.updateCooperationArea(areaId, newName, newCode, "内部", "updated");
        assertRejected(r, keyword);
    }

    // ── Delete ──

    /** Delete area, assert 200 */
    public void del(String areaId) {
        String r = api.deleteCooperationArea(areaId);
        Assertions.assertTrue(r.contains("\"code\":200"));
    }

    /** Delete non-existent area, just log */
    public void delInvalid(String areaId) {
        api.deleteCooperationArea(areaId);
    }

    // ── Find ──

    /** Find area by name, assert not null, return areaId */
    public String find(String name) {
        String id = api.findCooperationAreaId(name);
        Assertions.assertNotNull(id, "应能找到合作区: " + name);
        return id;
    }

    // ── Users ──

    /** Get first user's objectId from project person list */
    public String firstUser() { return user(0); }

    /** Get user by index from project person list */
    public String user(int index) {
        String r = api.searchProjectPersonList(projectId);
        JsonObject root = JsonParser.parseString(r).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt());
        JsonArray persons = root.getAsJsonArray("data");
        Assertions.assertNotNull(persons);
        Assertions.assertTrue(persons.size() > index, "项目人员列表不足" + (index + 1) + "人");
        return persons.get(index).getAsJsonObject().get("objectId").getAsString();
    }

    /** Assign user to area, assert 200 */
    public void assign(String areaId, String userId) {
        String r = api.addCooperationAreaUser(areaId, userId);
        assertCode(r, 200);
    }

    /** Remove user from area, assert 200 */
    public void unassign(String areaId, String userId) {
        assertCode(api.deleteCooperationAreaUser(areaId, userId), 200);
    }

    /** Remove user, no assertion */
    public void unassignRaw(String areaId, String userId) {
        api.deleteCooperationAreaUser(areaId, userId);
    }

    /** Assign user twice, second should be rejected */
    public void assignDup(String areaId, String userId) {
        api.addCooperationAreaUser(areaId, userId);
        String r = api.addCooperationAreaUser(areaId, userId);
        assertRejected(r, "重复分配");
    }

    /** Assign then unassign twice, second should be rejected */
    public void unassignDup(String areaId, String userId) {
        api.addCooperationAreaUser(areaId, userId);        // assign first
        api.deleteCooperationAreaUser(areaId, userId);     // remove OK
        String r = api.deleteCooperationAreaUser(areaId, userId); // remove again → fail
        assertRejected(r, "重复");
    }

    /** Get person list for a cooperation area */
    public int personCount(String areaId) {
        String r = api.searchProjectPersonList(areaId);
        JsonObject resp = JsonParser.parseString(r).getAsJsonObject();
        assertCode(r, 200);
        return resp.has("data") ? resp.getAsJsonArray("data").size() : 0;
    }

    // ── Search ──

    /** Search areas, assert 200 */
    public void search(String keyword) {
        String r = api.searchCooperationAreaList(keyword, "");
        Assertions.assertTrue(r.contains("\"code\":200"));
    }

    // ── Cleanup ──

    /** Delete all areas except objectId 1 and 2 */
    public void cleanup() {
        String r = api.searchCooperationAreaList("", "");
        JsonObject root = JsonParser.parseString(r).getAsJsonObject();
        if (root.get("code").getAsInt() != 200 || !root.has("data")) return;
        JsonArray data = root.getAsJsonArray("data");
        int deleted = 0;
        for (var el : data) {
            JsonObject item = el.getAsJsonObject();
            String id = item.has("objectId") ? item.get("objectId").getAsString() : "";
            String title = item.has("title") ? item.get("title").getAsString() : "";
            if (id.isEmpty() || "1".equals(id) || "2".equals(id)) continue;
            try {
                if (api.deleteCooperationArea(id).contains("\"code\":200")) deleted++;
            } catch (Exception ignored) {}
        }
        log.info("清理完成: 删除{}个", deleted);
    }

    // ── Internal ──

    private void assertRejected(String resp, String desc) {
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        String msg = root.has("msg") ? root.get("msg").getAsString() : "";
        Assertions.assertNotEquals(200, code,
                "【安全缺陷】服务端未校验" + desc + " — 期望≠200 实际=" + code + " msg=" + msg);
        log.info("负向通过【{}】code={}", desc, code);
    }

    private void assertCode(String json, int expected) {
        Assertions.assertEquals(expected,
            JsonParser.parseString(json).getAsJsonObject().get("code").getAsInt());
    }

    /** Helper to build name/code pair from tag */
    public String name(String tag) { return "AT_" + tag + "_" + sfx.get(); }
    public String code(String tag) { return "AT_" + tag + "C_" + sfx.get(); }
}
