package cases.manual;

import actions.ReqApiActions;
import base.ApiTestHelper;
import base.AuthHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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

    private static final String REAL_PROJECT_ID = "2058851105448046592";
    private static final String TEST_USER = "11";
    private static final String TEST_PWD = "Aa123456";
    private static final String COOP_USER_OBJ_ID = "384"; // test用户的objectId
    private String user11ObjId; // 运行时查询

    @Override
    @BeforeAll
    public void setupApi() {
        super.setupApi();
        PROJECT_ID = REAL_PROJECT_ID;
    }

    // ═══ 80. 写入权限校验（两文档对比） ═══

    @Test @DisplayName("80.1 创建合作区+两文档，一个有权限一个没有")
    void test_8001_permissionFlow() {
        // ── Step 1: 创建合作区，分配 test 用户 ──
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
        api.addCooperationAreaUser(areaId, COOP_USER_OBJ_ID);
        log.info("80.1-1 合作区+分配test(384): areaId={}", areaId);

        // ── Step 2: 创建两个需求规格 ──
        String[] d1 = createTempDoc();
        String docA = d1[0], folderA = d1[2];
        String[] d2 = createTempDoc();
        String docB = d2[0], folderB = d2[2];
        Assertions.assertNotNull(docA);
        Assertions.assertNotNull(docB);
        log.info("80.1-2 docA(有权限)={}, docB(无权限)={}", docA, docB);

        // ── Step 3: 查询11的objectId，docA给admin+11，docB仅admin ──
        user11ObjId = findUserObjectId(TEST_USER);
        log.info("80.1-3 11的objectId={}", user11ObjId);
        String permA = api.updateReqSpeWritePermission(docA,
            "[{\"objectId\":\"1\",\"userName\":\"admin\"},{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
        log.info("80.1-3 docA权限设置 resp={}", permA.substring(0, Math.min(100, permA.length())));
        api.updateReqSpeWritePermission(docB,
            "[{\"objectId\":\"1\",\"userName\":\"admin\"}]");

        // ── Step 4: 获取masterId，11登录checkOpenMode对比 ──
        String masterA = getMasterId(docA);
        String masterB = getMasterId(docB);
        log.info("80.1-4 masterA={}, masterB={}", masterA, masterB);

        Playwright pw = Playwright.create();
        APIRequestContext ctx = pw.request().newContext(new APIRequest.NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setExtraHTTPHeaders(java.util.Map.of("ProjectId", REAL_PROJECT_ID)));
        AuthHelper.login(ctx, TEST_USER, TEST_PWD);
        ctx.get(TestConfig.BASE_URL + "/login-api/auth/subapp/getList");
        ReqApiActions api11 = new ReqApiActions(ctx);
        try {
            String rA = api11.checkOpenMode(masterA, "dblClick", TEST_USER);
            String rB = api11.checkOpenMode(masterB, "dblClick", TEST_USER);
            boolean accessA = parseHasAccess(rA);
            boolean accessB = parseHasAccess(rB);
            log.info("80.1-4 docA={}", rA);
            log.info("80.1-4 docB={}", rB);
            Assertions.assertTrue(accessA, "docA给了11权限应有访问权");
            Assertions.assertFalse(accessB, "docB仅admin权限11应无访问权");
        } finally {
            ctx.dispose();
            pw.close();
        }

        // ── 清理 ──
        api.deleteCooperationArea(areaId);
        log.info("80.1-5 清理完成");
    }

    // ═══ 80.2 admin删自己权限 ═══

    @Test @DisplayName("80.2 admin把自己从写入权限中移除")
    void test_8002_adminRemoveSelf() {
        // admin创建文档，设权限仅11（排除自己），然后check
        String[] d = createTempDoc();
        String docId = d[0];
        log.info("80.2 admin创建文档: docId={}", docId);

        // admin把自己移除，只留11
        String permResp = api.updateReqSpeWritePermission(docId,
            "[{\"objectId\":\"" + (user11ObjId != null ? user11ObjId : findUserObjectId(TEST_USER)) + "\",\"userName\":\"" + TEST_USER + "\"}]");
        log.info("80.2 设权限仅11 resp={}", permResp.substring(0, Math.min(150, permResp.length())));

        // admin再去checkOpenMode → 看系统拦不拦
        String master = getMasterId(docId);
        String r = api.checkOpenMode(master, "dblClick", TestConfig.ADMIN_USER);
        boolean hasAccess = parseHasAccess(r);
        log.info("80.2 admin checkOpenMode hasAccess={}", hasAccess);
        log.info("80.2 完整响应: {}", r);
    }

    // ═══ 80.3 admin不被权限限制 ═══

    @Test @DisplayName("80.3 普通用户设权限仅自己，admin仍可访问")
    void test_8003_adminAlwaysHasAccess() {
        // 用11身份创建一个权限仅11的文档，admin来check
        Playwright pw = Playwright.create();
        APIRequestContext ctx = pw.request().newContext(new APIRequest.NewContextOptions()
            .setIgnoreHTTPSErrors(true)
            .setExtraHTTPHeaders(java.util.Map.of("ProjectId", REAL_PROJECT_ID)));
        AuthHelper.login(ctx, TEST_USER, TEST_PWD);
        ctx.get(TestConfig.BASE_URL + "/login-api/auth/subapp/getList");
        ReqApiActions api11 = new ReqApiActions(ctx);
        try {
            // 11创建文件夹和文档
            String folderId = api.createFolder(REAL_PROJECT_ID, REAL_PROJECT_ID);
            String docId = api.createDocument(REAL_PROJECT_ID, folderId);
            api.renameDocument(REAL_PROJECT_ID, docId, folderId, "AT_Doc_11_" + suffix());

            // 11设写入权限仅自己（排除admin）
            user11ObjId = findUserObjectId(TEST_USER);
            api11.updateReqSpeWritePermission(docId,
                "[{\"objectId\":\"" + user11ObjId + "\",\"userName\":\"" + TEST_USER + "\"}]");
            log.info("80.3 11创建文档并设权限仅自己: docId={}", docId);

            // admin来查 → 应该有权限
            String master = getMasterId(docId);
            String r = api.checkOpenMode(master, "dblClick", TestConfig.ADMIN_USER);
            boolean hasAccess = parseHasAccess(r);
            log.info("80.3 admin checkOpenMode={}", r);
            Assertions.assertTrue(hasAccess, "admin应始终有访问权");
        } finally {
            ctx.dispose();
            pw.close();
        }
    }

    private static boolean parseHasAccess(String json) {
        JsonObject ar = JsonParser.parseString(json).getAsJsonObject();
        if (!ar.has("data") || ar.get("data").isJsonNull()) return false;
        JsonObject data = ar.getAsJsonObject("data");
        // getReqAccess returns data.flag
        if (data.has("flag")) return data.get("flag").getAsBoolean();
        // checkOpenMode returns data.hasAccess
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
            String resp = api.searchProjectPersonList(REAL_PROJECT_ID);
            JsonArray data = JsonParser.parseString(resp).getAsJsonObject().getAsJsonArray("data");
            if (data != null) for (var e : data) {
                JsonObject u = e.getAsJsonObject();
                if (loginName.equals(u.has("loginName") ? u.get("loginName").getAsString() : ""))
                    return u.get("objectId").getAsString();
            }
        } catch (Exception ex) { log.warn("查找用户objectId失败: {}", ex.getMessage()); }
        return "11"; // fallback
    }
}
