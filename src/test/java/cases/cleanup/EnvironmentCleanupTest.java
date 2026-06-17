package cases.cleanup;

import base.ApiTestBase;
import com.google.gson.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import config.TestConfig;
import org.junit.jupiter.api.*;

/**
 * 环境清理 —— 作为普通测试模块，前端可选执行。
 * 使用 QuickApiCheck 模式（Browser + page.request() 共享 cookie）完成认证，
 * 清理：目录树、需求规格、指标、合作区。
 */
@Tag("CommonModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EnvironmentCleanupTest extends ApiTestBase {

    private static final String API_BASE = TestConfig.BASE_URL + "/dev-api";
    private static final String MOE_BASE = TestConfig.BASE_URL + "/api-api";
    private static final String LOGIN_URL = TestConfig.BASE_URL + "/login-api/auth/token/login";

    private static final String TREE          = "/erm/search/searchReqFolderStructureTree";
    private static final String DEL_FOLDER    = "/erm/del/delReqSpeFolder";
    private static final String CLEAN_FOLDER  = "/erm/clean/cleanReqSpeFolder";
    private static final String DEL_DOC       = "/erm/del/delReqSpe";
    private static final String CLEAN_DOC     = "/erm/clean/cleanReqSpe";
    private static final String SEARCH_SPE    = "/erm/search/searchReqSpeListFromProject";
    private static final String SEARCH_LS     = "/moe/search/searchLogicStructureList";
    private static final String SEARCH_LOGIC  = "/moe/search/searchLogicList";
    private static final String DEL_LOGIC     = "/moe/delete/deleteLogic";
    private static final String SEARCH_PARAM  = "/moe/search/searchLogicStructureParameterList";
    private static final String DEL_PARAM     = "/moe/remove/removeLogicStructureParameter";
    private static final String SEARCH_SCHEME = "/moe/search/searchPhysicalSchemeList";
    private static final String DEL_SCHEME    = "/moe/delete/deleteAPhysicalScheme";
    private static final String DEL_LS        = "/moe/delete/deleteLogicStructure";
    private static final String COOP_SEARCH   = "/common/search/searchProjectList";
    private static final String COOP_DELETE   = "/common/delete/delProject";

    private Playwright playwright;
    private Browser browser;
    private APIRequestContext req;
    private RequestOptions opts;

    private int count = 0;

    @BeforeAll
    void initBrowser() {
        super.setupApi();
        loginViaApi();

        playwright = Playwright.create();
        BrowserType.LaunchOptions launchOpts = new BrowserType.LaunchOptions().setHeadless(true);
        String chromeEnv = System.getenv("CHROME_PATH");
        if (chromeEnv != null && !chromeEnv.isBlank()) {
            launchOpts.setExecutablePath(java.nio.file.Paths.get(chromeEnv));
        }
        browser = playwright.chromium().launch(launchOpts);
        BrowserContext ctx = browser.newContext(
            new Browser.NewContextOptions().setIgnoreHTTPSErrors(true));
        ctx.newPage(); // page.request() shares cookie jar
        req = ctx.request();

        // API login → cookies auto-stored
        log.info("登录中...");
        JsonObject loginBody = new JsonObject();
        loginBody.addProperty("username", TestConfig.ADMIN_USER);
        loginBody.addProperty("password", TestConfig.ADMIN_PWD);
        req.post(LOGIN_URL, RequestOptions.create()
            .setHeader("Content-Type", "application/json").setData(loginBody.toString()));
        log.info("登录完成");

        opts = RequestOptions.create()
            .setHeader("Content-Type", "application/json")
            .setHeader("ProjectId", PROJECT_ID);
    }

    @AfterAll
    void closeBrowser() {
        try { if (browser != null) browser.close(); } catch (Exception ignored) {}
        try { if (playwright != null) playwright.close(); } catch (Exception ignored) {}
        super.teardownApi();
    }

    // ═══════════════════════ Phase 1: 目录树清理 ═══════════════════════

    @Test @Order(1)
    @DisplayName("[清理] 目录树")
    void cleanTree() {
        log.info("=== Phase 1: 清理目录树 ===");
        String treeResp = req.post(API_BASE + TREE,
            opts.setData(json("projectId", PROJECT_ID, "parentId", PROJECT_ID, "parentType", "project"))).text();
        JsonObject root = JsonParser.parseString(treeResp).getAsJsonObject();
        if (!root.has("data") || root.get("data").isJsonNull()) {
            log.info("目录树为空，跳过");
            return;
        }
        JsonArray dataList = root.getAsJsonArray("data");
        count += cleanTreeRecursive(dataList, PROJECT_ID, "project");
        log.info("Phase 1 完成: {} 个节点", count);
    }

    private int cleanTreeRecursive(JsonArray items, String parentId, String parentType) {
        int c = 0;
        for (JsonElement el : items) {
            JsonObject node = el.getAsJsonObject();
            String nodeId = node.get("objectId").getAsString();
            String type = node.get("type").getAsString();

            if (node.has("children") && !node.get("children").isJsonNull()) {
                c += cleanTreeRecursive(node.getAsJsonArray("children"), nodeId, "reqSpeFolder");
            }

            if ("reqSpeFolder".equals(type)) {
                req.post(API_BASE + DEL_FOLDER, opts.setData(json("objectId", nodeId, "parentId", PROJECT_ID, "parentType", "reqSpeFolder")));
                sleep(200);
                req.post(API_BASE + CLEAN_FOLDER, opts.setData(json("objectId", nodeId, "parentId", PROJECT_ID, "parentType", "reqSpeFolder")));
                c++;
                log.info("  [{}] 文件夹 {}", c, node.has("title") ? node.get("title").getAsString() : nodeId);
            } else if ("reqSpe".equals(type)) {
                req.post(API_BASE + DEL_DOC, opts.setData(json("objectId", nodeId, "parentId", parentId, "parentType", parentType)));
                sleep(200);
                req.post(API_BASE + CLEAN_DOC, opts.setData(json("objectId", nodeId, "parentId", parentId, "parentType", parentType)));
                c++;
                log.info("  [{}] 规格 {}", c, node.has("title") ? node.get("title").getAsString() : nodeId);
            }
            sleep(100);
        }
        return c;
    }

    // ═══════════════════════ Phase 2: 规格清理 ═══════════════════════

    @Test @Order(2)
    @DisplayName("[清理] 需求规格")
    void cleanSpecs() {
        log.info("=== Phase 2: 清理需求规格 ===");
        try {
            String resp = req.post(API_BASE + SEARCH_SPE,
                opts.setData(json("projectId", PROJECT_ID))).text();
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (!root.has("data") || root.get("data").isJsonNull()) { log.info("无规格，跳过"); return; }
            JsonArray arr = root.getAsJsonArray("data");
            log.info("找到 {} 个规格", arr.size());
            for (int i = 0; i < arr.size(); i++) {
                JsonObject spe = arr.get(i).getAsJsonObject();
                String speId = spe.has("id") ? spe.get("id").getAsString() : spe.get("objectId").getAsString();
                String parentId = spe.has("parentId") ? spe.get("parentId").getAsString() : PROJECT_ID;
                req.post(API_BASE + DEL_DOC, opts.setData(json("objectId", speId, "parentId", parentId, "parentType", "project")));
                sleep(200);
                req.post(API_BASE + CLEAN_DOC, opts.setData(json("objectId", speId, "parentId", parentId, "parentType", "project")));
                count++;
                log.info("  [{}/{}] {}", i + 1, arr.size(), spe.has("title") ? spe.get("title").getAsString() : speId);
                sleep(100);
            }
        } catch (Exception e) { log.warn("Phase 2 异常: {}", e.getMessage()); }
        log.info("Phase 2 完成");
    }

    // ═══════════════════════ Phase 3: 指标清理 ═══════════════════════

    @Test @Order(3)
    @DisplayName("[清理] 指标")
    void cleanIndicators() {
        log.info("=== Phase 3: 清理指标 ===");
        try {
            String lsResp = req.post(MOE_BASE + SEARCH_LS,
                opts.setData(json("projectId", PROJECT_ID))).text();
            JsonObject lsRoot = JsonParser.parseString(lsResp).getAsJsonObject();
            if (!lsRoot.has("data") || lsRoot.get("data").isJsonNull()) { log.info("无指标，跳过"); return; }
            JsonArray lsArr = lsRoot.getAsJsonArray("data");
            log.info("找到 {} 个逻辑结构", lsArr.size());
            for (int i = 0; i < lsArr.size(); i++) {
                JsonObject ls = lsArr.get(i).getAsJsonObject();
                String lsId = ls.has("id") ? ls.get("id").getAsString() : ls.get("objectId").getAsString();

                count += deleteAll(MOE_BASE + SEARCH_LOGIC, json("objectId", lsId),
                    MOE_BASE + DEL_LOGIC, "{\"objectId\":\"%s\",\"logicStructureId\":\"" + lsId + "\"}");
                count += deleteAll(MOE_BASE + SEARCH_PARAM, json("objectId", lsId),
                    MOE_BASE + DEL_PARAM, "{\"objectId\":\"%s\",\"logicStructureId\":\"" + lsId + "\"}");
                count += deleteAll(MOE_BASE + SEARCH_SCHEME, json("objectId", lsId),
                    MOE_BASE + DEL_SCHEME, "{\"objectId\":\"%s\"}");

                log.info("  [{}/{}] {}", i + 1, lsArr.size(), ls.has("name") ? ls.get("name").getAsString() : lsId);
                sleep(50);
            }
        } catch (Exception e) { log.warn("Phase 3 异常: {}", e.getMessage()); }
        log.info("Phase 3 完成");
    }

    private int deleteAll(String searchUrl, String searchData, String deleteUrl, String deleteFmt) {
        int c = 0;
        try {
            String resp = req.post(searchUrl, opts.setData(searchData)).text();
            JsonArray arr = JsonParser.parseString(resp).getAsJsonObject().getAsJsonArray("data");
            if (arr == null) return 0;
            for (JsonElement e : arr) {
                String id = e.getAsJsonObject().has("id")
                    ? e.getAsJsonObject().get("id").getAsString()
                    : e.getAsJsonObject().get("objectId").getAsString();
                req.post(deleteUrl, opts.setData(String.format(deleteFmt, id)));
                c++;
            }
        } catch (Exception ignored) {}
        return c;
    }

    // ═══════════════════════ Phase 4: 合作区清理 ═══════════════════════

    @Test @Order(4)
    @DisplayName("[清理] 合作区")
    void cleanCooperationAreas() {
        log.info("=== Phase 4: 清理合作区（保留 A、test） ===");
        try {
            String resp = req.get(API_BASE + COOP_SEARCH + "?title=&originated=", opts).text();
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            if (!root.has("data") || root.get("data").isJsonNull()) { log.info("无合作区，跳过"); return; }
            JsonArray arr = root.getAsJsonArray("data");
            log.info("找到 {} 个合作区", arr.size());
            int kept = 0, deleted = 0;
            for (int i = 0; i < arr.size(); i++) {
                JsonObject item = arr.get(i).getAsJsonObject();
                String name = item.has("name") ? item.get("name").getAsString() : "";
                String title = item.has("title") ? item.get("title").getAsString() : "";
                String objectId = item.has("objectId") ? item.get("objectId").getAsString() : "";
                if (objectId.isEmpty()) continue;

                if ("A".equals(name) || "A".equals(title) || "test".equals(name) || "test".equals(title)) {
                    kept++;
                    log.info("  [KEEP] {} / {}", name, title);
                    continue;
                }
                req.post(API_BASE + COOP_DELETE, opts.setData("[{\"objectId\":\"" + objectId + "\"}]"));
                deleted++;
                log.info("  [DEL {}/{}] {} / {}", deleted + kept, arr.size(), name, title);
                sleep(100);
            }
            count += deleted;
            log.info("合作区清理: 删除{}个, 保留{}个", deleted, kept);
        } catch (Exception e) { log.warn("Phase 4 异常: {}", e.getMessage()); }
        log.info("Phase 4 完成");
    }

    // ═══════════════════════ 汇总 ═══════════════════════

    @Test @Order(5)
    @DisplayName("[清理] 汇总")
    void summary() {
        log.info("====== 清理完成！共 {} 个节点 ======", count);
    }

    // ── helpers ──

    private static String json(String k1, String v1) {
        return "{\"" + k1 + "\":\"" + v1 + "\"}";
    }
    private static String json(String k1, String v1, String k2, String v2, String k3, String v3) {
        return "{\"" + k1 + "\":\"" + v1 + "\",\"" + k2 + "\":\"" + v2 + "\",\"" + k3 + "\":\"" + v3 + "\"}";
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
