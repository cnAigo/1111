package cases.manual;

import actions.ReqApiActions;
import base.ApiTestHelper;
import base.AuthHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIRequest;
import com.microsoft.playwright.APIRequestContext;
import com.microsoft.playwright.Playwright;
import config.TestConfig;
import org.junit.jupiter.api.*;

@Tag("PermissionModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PermissionManualTest extends ApiTestHelper {

    private static final String TEST_USER = "11";
    private static final String TEST_PWD = "Aa123456";
    private String user11ObjId;

    @Override
    @BeforeAll
    public void setupApi() {
        needsClassCooperationArea = false;
        super.setupApi();
    }

    // ═══ 80.1 写入权限 — 两文档对比 ═══
    @Test @DisplayName("80.1 创建合作区+两文档，一个有权限一个没有")
    void test_8001_permissionFlow() {
        user11ObjId = findUserObjectId(TEST_USER);
        String n = "AT_Perm_" + suffix();
        String r = api.addCooperationArea(n, "AT_PC_" + suffix(), "内部", "auto");
        JsonObject resp = JsonParser.parseString(r).getAsJsonObject();
        Assertions.assertEquals(200, resp.get("code").getAsInt());
        String areaId = resp.has("data") && !resp.get("data").isJsonNull()
            ? resp.getAsJsonObject("data").has("objectId")
                ? resp.getAsJsonObject("data").get("objectId").getAsString()
                : resp.getAsJsonObject("data").get("id").getAsString()
            : api.findCooperationAreaId(n);
        Assertions.assertNotNull(areaId);
        api.addCooperationAreaUser(areaId, user11ObjId);

        String[] d1 = createTempDoc();
        String docA = d1[0];
        String[] d2 = createTempDoc();
        String docB = d2[0];

        api.updateReqSpeWritePermission(docA,
            "[{\"objectId\":\"1\",\"userName\":\"admin\"},{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
        api.updateReqSpeWritePermission(docB,
            "[{\"objectId\":\"1\",\"userName\":\"admin\"}]");

        Assertions.assertTrue(checkAccessAs11(getMasterId(docA), "dblClick"), "docA给了11权限应有访问权");
        Assertions.assertFalse(checkAccessAs11(getMasterId(docB), "dblClick"), "docB仅admin权限11应无访问权");

        api.deleteCooperationArea(areaId);
        log.info("80.1 两文档权限对比(正向) 通过");
    }

    // ═══ 80.2 admin删自己权限 ═══
    @Test @DisplayName("80.2 admin把自己从写入权限中移除")
    void test_8002_adminRemoveSelf() {
        String[] d = createTempDoc();
        String docId = d[0];
        if (user11ObjId == null) user11ObjId = findUserObjectId(TEST_USER);
        api.updateReqSpeWritePermission(docId,
            "[{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
        boolean hasAccess = parseHasAccess(api.checkOpenMode(getMasterId(docId), "dblClick", TestConfig.ADMIN_USER));
        log.info("80.2 admin删自己后访问权={}", hasAccess);
    }

    // ═══ 80.3 admin不被权限限制 ═══
    @Test @DisplayName("80.3 普通用户设权限仅自己，admin仍可访问")
    void test_8003_adminAlwaysHasAccess() {
        if (user11ObjId == null) user11ObjId = findUserObjectId(TEST_USER);
        Playwright pw = Playwright.create();
        APIRequestContext ctx = pw.request().newContext(new APIRequest.NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setExtraHTTPHeaders(java.util.Map.of("ProjectId", PROJECT_ID)));
        AuthHelper.login(ctx, TEST_USER, TEST_PWD);
        ctx.get(TestConfig.BASE_URL + "/login-api/auth/subapp/getList");
        ReqApiActions api11 = new ReqApiActions(ctx);
        try {
            String folderId = api.createFolder(PROJECT_ID, PROJECT_ID);
            String docId = api.createDocument(PROJECT_ID, folderId);
            api.renameDocument(PROJECT_ID, docId, folderId, "AT_Doc_11_" + suffix());
            api11.updateReqSpeWritePermission(docId,
                "[{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
            Assertions.assertTrue(parseHasAccess(api.checkOpenMode(getMasterId(docId), "dblClick", TestConfig.ADMIN_USER)),
                "admin应始终有访问权");
        } finally { ctx.dispose(); pw.close(); }
        log.info("80.3 admin绕过权限限制(正向) 通过");
    }

    // ═══ 80.4 赋权→撤销 ═══
    @Test @DisplayName("80.4 赋权后撤销用户，验证访问被拒绝")
    void test_8004_grantThenRevoke() {
        user11ObjId = findUserObjectId(TEST_USER);
        String[] d = createTempDoc();
        String docId = d[0];
        api.updateReqSpeWritePermission(docId,
            "[{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
        String master = getMasterId(docId);

        Assertions.assertTrue(checkAccessAs11(master, "dblClick"), "赋权后11应有访问权");
        api.updateReqSpeWritePermission(docId, "[{\"objectId\":\"1\",\"userName\":\"admin\"}]");
        Assertions.assertFalse(checkAccessAs11(master, "dblClick"), "撤销后11应无访问权");
        log.info("80.4 赋权后撤销(正向) 通过");
    }

    // ═══ 80.5 锁定后写入拦截 ═══
    @Test @DisplayName("80.5 锁定文档后用户无法编辑(负向)")
    void test_8005_lockedCantEdit() {
        user11ObjId = findUserObjectId(TEST_USER);
        String[] d = createTempDoc();
        String docId = d[0];
        api.updateReqSpeWritePermission(docId,
            "[{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
        api.unlockMode(docId, "lock", "admin");

        Assertions.assertFalse(checkAccessAs11(getMasterId(docId), "edit"), "锁定后用户应无法编辑");
        api.unlockMode(docId, "unlock", "admin");
        log.info("80.5 锁定后无法编辑(负向) 通过");
    }

    // ═══ 80.6 冻结后无法写入 ═══
    @Test @DisplayName("80.6 冻结后无法写入(负向)")
    void test_8006_frozenNoWrite() {
        String[] d = createTempDoc();
        String docId = d[0];
        api.updateReqSpeState(docId, "Frozen");
        Assertions.assertFalse(parseHasAccess(api.checkOpenMode(getMasterId(docId), "edit", TestConfig.ADMIN_USER)),
            "冻结后即使admin也应无法编辑");
        api.updateReqSpeState(docId, "Inwork");
        log.info("80.6 冻结后无法写入(负向) 通过");
    }

    // ═══ 80.7 读取权限状态 ═══
    @Test @DisplayName("80.7 getReqAccess读取权限状态(正向)")
    void test_8007_getAccess() {
        user11ObjId = findUserObjectId(TEST_USER);
        String[] d = createTempDoc();
        api.updateReqSpeWritePermission(d[0],
            "[{\"objectId\":\"1\",\"userName\":\"admin\"},{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
        String r = api.getReqAccess(d[0]);
        Assertions.assertTrue(r.contains("\"code\":200"), "getReqAccess应返回200");
        log.info("80.7 getReqAccess(正向) 通过");
    }

    // ═══ 80.8 无权限 ═══
    @Test @DisplayName("80.8 完全不授权→用户无任何访问权")
    void test_8008_noPermission() {
        String[] d = createTempDoc();
        api.updateReqSpeWritePermission(d[0], "[{\"objectId\":\"1\",\"userName\":\"admin\"}]");
        Assertions.assertFalse(checkAccessAs11(getMasterId(d[0]), "dblClick"), "未授权用户不应有访问权");
        log.info("80.8 完全不授权(正向) 通过");
    }

    // ═══ 80.9 空权限列表 ═══
    @Test @DisplayName("80.9 设置空权限列表(负向)")
    void test_8009_emptyPermList() {
        String[] d = createTempDoc();
        assertRejected(api.updateReqSpeWritePermission(d[0], "[]"), "权限列表不可为空");
        log.info("80.9 空权限列表(负向) 通过");
    }

    // ═══ 80.10 重复授权 ═══
    @Test @DisplayName("80.10 权限列表含重复用户(负向)")
    void test_8010_dupUser() {
        String[] d = createTempDoc();
        assertRejected(api.updateReqSpeWritePermission(d[0],
            "[{\"objectId\":\"1\",\"userName\":\"admin\"},{\"objectId\":\"1\",\"userName\":\"admin\"}]"), "不可重复授权");
        log.info("80.10 重复授权同一用户(负向) 通过");
    }

    // ── Helpers ──

    /** Login as test user and check open-mode access. Handles Playwright lifecycle. */
    private boolean checkAccessAs11(String masterId, String operation) {
        Playwright pw = Playwright.create();
        APIRequestContext ctx = pw.request().newContext(new APIRequest.NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setExtraHTTPHeaders(java.util.Map.of("ProjectId", PROJECT_ID)));
        AuthHelper.login(ctx, TEST_USER, TEST_PWD);
        ctx.get(TestConfig.BASE_URL + "/login-api/auth/subapp/getList");
        try {
            return parseHasAccess(new ReqApiActions(ctx).checkOpenMode(masterId, operation, TEST_USER));
        } finally { ctx.dispose(); pw.close(); }
    }

    private static boolean parseHasAccess(String json) {
        JsonObject ar = JsonParser.parseString(json).getAsJsonObject();
        if (!ar.has("data") || ar.get("data").isJsonNull()) return false;
        JsonObject data = ar.getAsJsonObject("data");
        if (data.has("flag")) return data.get("flag").getAsBoolean();
        if (data.has("hasAccess")) return data.get("hasAccess").getAsBoolean();
        return false;
    }

    private String getMasterId(String docId) {
        try {
            String resp = api.getVersionList(docId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (root.get("code").getAsInt() == 200 && root.has("data")) {
                JsonArray arr = root.getAsJsonArray("data");
                if (arr != null && arr.size() > 0)
                    return arr.get(0).getAsJsonObject().get("masterId").getAsString();
            }
        } catch (Exception e) { log.warn("获取masterId失败: {}", e.getMessage()); }
        return docId;
    }

    private String findUserObjectId(String loginName) {
        try {
            String resp = api.searchProjectPersonList(PROJECT_ID);
            JsonArray data = JsonParser.parseString(resp).getAsJsonObject().getAsJsonArray("data");
            if (data != null) for (var e : data) {
                JsonObject u = e.getAsJsonObject();
                if (loginName.equals(u.has("loginName") ? u.get("loginName").getAsString() : ""))
                    return u.get("objectId").getAsString();
            }
        } catch (Exception ex) { log.warn("查找用户objectId失败: {}", ex.getMessage()); }
        Assertions.fail("未在项目人员列表中找到用户: " + loginName);
        return null;
    }
}
