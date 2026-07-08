package org.example.testvue.service;

import com.google.gson.*;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.RequestOptions;
import org.example.testvue.config.ApiConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles environment cleanup via Playwright-authenticated API calls.
 * Extracted from TestExecutionService to keep it focused.
 */
@Service
public class CleanupService {

    private static final Logger log = LoggerFactory.getLogger(CleanupService.class);

    private volatile String status = "IDLE";
    private volatile String msg = "";
    private volatile int progress = 0;
    private volatile int progressTotal = 0;
    private final StringBuilder logBuf = new StringBuilder();
    private final AtomicBoolean stopFlag = new AtomicBoolean(false);

    private WebSocketSessionManager wsManager;
    private String currentTaskId;

    public String getStatus() { return status; }
    public String getMsg() { return msg; }
    public int getProgress() { return progress; }
    public int getProgressTotal() { return progressTotal; }
    public String getLog() { synchronized (logBuf) { return logBuf.toString(); } }
    public void stop() { stopFlag.set(true); }
    public boolean isRunning() { return "RUNNING".equals(status); }

    public void startCleanup(String baseUrl, String username, String password, String projectId,
                             String taskId, WebSocketSessionManager wsManager) {
        if ("RUNNING".equals(status)) return;
        stopFlag.set(false);
        status = "RUNNING"; msg = "";
        progress = 0; progressTotal = 0;
        this.wsManager = wsManager;
        this.currentTaskId = taskId;
        synchronized (logBuf) { logBuf.setLength(0); }

        if (baseUrl == null || baseUrl.isBlank()) baseUrl = "https://192.168.6.171:8088";
        if (username == null || username.isBlank()) username = "admin";
        if (password == null || password.isBlank()) password = "Aa123456";

        doCleanup(baseUrl, username, password, projectId);
    }

    // ── Internal ──

    private void doCleanup(String base, String user, String pass, String projectId) {
        long t0 = System.currentTimeMillis();
        log("====== 开始清理环境 ======");
        if (projectId == null || projectId.isBlank()) {
            status = "FAILED"; msg = "未配置projectId"; pushResult(); return;
        }
        String apiBase = base + "/dev-api";
        String moeApiBase = base + "/api-api";

        Playwright playwright = null;
        try {
            playwright = Playwright.create();

            // Use API-only context (no browser needed) — same as ApiTestBase
            APIRequestContext api = playwright.request().newContext(
                new APIRequest.NewContextOptions().setIgnoreHTTPSErrors(true));

            // Login via API
            log("API登录中...");
            JsonObject loginBody = new JsonObject();
            loginBody.addProperty("username", user);
            loginBody.addProperty("password", pass);
            APIResponse loginResp = api.post(base + ApiConstants.LOGIN_PATH,
                RequestOptions.create().setHeader("Content-Type", "application/json").setData(loginBody.toString()));
            String loginText = loginResp.text();
            log("登录响应: HTTP " + loginResp.status() + " " + loginText.substring(0, Math.min(200, loginText.length())));

            JsonObject loginJson;
            boolean loginOk = false;
            String token = null;
            try {
                loginJson = new Gson().fromJson(loginText, JsonObject.class);
                int code = loginJson.has("code") ? loginJson.get("code").getAsInt() : -1;
                String msg = loginJson.has("msg") ? loginJson.get("msg").getAsString() : "";
                if (code == 200 || msg.contains("成功") || msg.contains("登陆成功")) {
                    loginOk = true;
                    if (loginJson.has("data") && !loginJson.get("data").isJsonNull()) {
                        JsonObject data = loginJson.getAsJsonObject("data");
                        if (data.has("token")) token = data.get("token").getAsString();
                        else if (data.has("access_token")) token = data.get("access_token").getAsString();
                    }
                }
            } catch (Exception e) {
                log("登录响应解析失败: " + e.getMessage());
            }

            if (!loginOk) {
                log("登录失败: " + loginText.substring(0, Math.min(300, loginText.length())));
                status = "FAILED"; msg = "登录失败，请检查用户名密码"; return;
            }
            log(token != null ? "登录完成(token)" : "登录完成(cookie)");

            // Activate session — first API call triggers subsystem authorization
            log("激活会话...");
            api.get(apiBase + "/common/search/searchProjectList?title=&originated=");
            log("会话已激活");

            // Auth header — only Content-Type, matching standalone test
            RequestOptions opts = RequestOptions.create()
                .setHeader("Content-Type", "application/json");
            if (token != null && !token.isEmpty()) {
                opts.setHeader(ApiConstants.HEADER_AUTHORIZATION, "Bearer " + token);
            }

            // Phase 1: Tree-based cleanup — use fresh RequestOptions each call
            log("获取目录树...");
            String treeJson = api.post(apiBase + ApiConstants.ERM_TREE,
                RequestOptions.create().setHeader("Content-Type", "application/json")
                    .setData("{\"projectId\":\"" + projectId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"project\"}")).text();
            JsonObject treeRoot;
            try {
                treeRoot = new Gson().fromJson(treeJson, JsonObject.class);
            } catch (Exception e) {
                log("树API返回非JSON: " + treeJson.substring(0, Math.min(300, treeJson.length())));
                status = "FAILED"; msg = "认证失败"; return;
            }

            int code = treeRoot.has("code") ? treeRoot.get("code").getAsInt() : -1;
            if (code != 200) {
                log("树API返回 " + code + ": " + treeJson.substring(0, Math.min(300, treeJson.length())));
                status = "FAILED"; msg = "认证失败，请先在浏览器中手动登录一次目标系统"; return;
            }

            if (!treeRoot.has("data") || treeRoot.get("data").isJsonNull()) {
                status = "SUCCESS"; msg = "空"; return;
            }

            JsonArray dataList = treeRoot.getAsJsonArray("data");
            int total = countAll(dataList);
            progressTotal = total; progress = 0;
            log("找到 " + total + " 个节点，开始清理...");

            int count = cleanTree(api, apiBase, dataList, projectId, opts, 0);

            // Phase 2: Direct spec search cleanup
            count += cleanSpecs(api, apiBase, projectId, opts, count);

            // Phase 3: Indicator cleanup
            count += cleanIndicators(api, moeApiBase, projectId, opts, count);

            // Phase 4: Cooperation area cleanup (keep A and test)
            count += cleanCooperationAreas(api, apiBase, opts, count);

            long durMs = System.currentTimeMillis() - t0;
            status = "SUCCESS"; msg = "清理完成，共 " + count + " 个节点";
            log("====== 清理完成！共 " + count + " 个节点 ======");
            pushResult();
        } catch (Exception e) {
            status = "FAILED"; msg = e.getMessage();
            log("ERROR: " + e.getMessage());
            pushResult();
        } finally {
            if (playwright != null) { try { playwright.close(); } catch (Exception ignored) {} }
        }
    }

    /** Recursively clean children bottom-up, then the nodes themselves. */
    private int cleanTree(APIRequestContext api, String apiBase, JsonArray dataList,
                           String projectId, RequestOptions opts, int startCount) {
        int count = startCount;
        for (JsonElement el : dataList) {
            if (stopFlag.get()) { status = "STOPPED"; return count; }
            JsonObject node = el.getAsJsonObject();
            String nodeId = node.get("objectId").getAsString();
            String nodeTitle = node.has("title") ? node.get("title").getAsString() : nodeId;
            String nodeType = node.get("type").getAsString();

            // Clean nested children bottom-up (recurse first)
            if (node.has("children") && !node.get("children").isJsonNull()) {
                count = cleanTree(api, apiBase, node.getAsJsonArray("children"),
                    nodeId, opts, count);
            }

            // Delete the node itself AFTER children are cleaned
            if ("reqSpeFolder".equals(nodeType)) {
                api.post(apiBase + ApiConstants.ERM_DEL_FOLDER,
                    opts.setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"reqSpeFolder\"}"));
                sleep(300);
                api.post(apiBase + ApiConstants.ERM_CLEAN_FOLDER,
                    opts.setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"reqSpeFolder\"}"));
                sleep(200);
                count++; progress = count; pushProgress();
                log("[CLEANUP] " + count + "/" + progressTotal + " 文件夹 " + nodeTitle);
            } else if ("reqSpe".equals(nodeType)) {
                api.post(apiBase + ApiConstants.ERM_DEL_DOC,
                    opts.setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"reqSpeFolder\"}"));
                sleep(300);
                api.post(apiBase + ApiConstants.ERM_CLEAN_DOC,
                    opts.setData("{\"objectId\":\"" + nodeId + "\",\"parentId\":\"" + projectId + "\",\"parentType\":\"reqSpeFolder\"}"));
                sleep(200);
                count++; progress = count; pushProgress();
                log("[CLEANUP] " + count + "/" + progressTotal + " 需求规格 " + nodeTitle);
            }
        }
        return count;
    }

    private int cleanSpecs(APIRequestContext api, String apiBase, String projectId,
                            RequestOptions opts, int count) {
        log("--- 第二阶段：搜索并清理所有需求规格 ---");
        try {
            String resp = api.post(apiBase + ApiConstants.ERM_SEARCH_SPE,
                opts.setData("{\"projectId\":\"" + projectId + "\"}")).text();
            JsonObject root = new Gson().fromJson(resp, JsonObject.class);
            if (!root.has("data") || root.get("data").isJsonNull()) return count;

            JsonArray arr = root.getAsJsonArray("data");
            log("  找到 " + arr.size() + " 个需求规格");
            for (int i = 0; i < arr.size(); i++) {
                if (stopFlag.get()) { status = "STOPPED"; return count; }
                JsonObject spe = arr.get(i).getAsJsonObject();
                String speId = spe.has("id") ? spe.get("id").getAsString() : spe.get("objectId").getAsString();
                String speTitle = spe.has("title") ? spe.get("title").getAsString() : speId;
                String parentId = spe.has("parentId") ? spe.get("parentId").getAsString() : projectId;

                api.post(apiBase + ApiConstants.ERM_DEL_DOC,
                    opts.setData("{\"objectId\":\"" + speId + "\",\"parentId\":\"" + parentId + "\",\"parentType\":\"project\"}"));
                sleep(300);
                api.post(apiBase + ApiConstants.ERM_CLEAN_DOC,
                    opts.setData("{\"objectId\":\"" + speId + "\",\"parentId\":\"" + parentId + "\",\"parentType\":\"project\"}"));
                sleep(200);
                count++; progress = count; pushProgress();
                log("  [SPE] " + (i + 1) + "/" + arr.size() + " 需求规格 " + speTitle);
            }
        } catch (Exception e) {
            log("  需求规格搜索清理异常: " + e.getMessage());
        }
        return count;
    }

    private int cleanIndicators(APIRequestContext api, String moeApiBase, String projectId,
                                 RequestOptions opts, int count) {
        log("--- 第三阶段：搜索并清理所有指标 ---");
        try {
            RequestOptions moeOpts = RequestOptions.create()
                .setHeader("Content-Type", "application/json");

            String lsResp = api.post(moeApiBase + ApiConstants.MOE_SEARCH_LS,
                moeOpts.setData("{\"projectId\":\"" + projectId + "\"}")).text();
            JsonObject lsRoot = new Gson().fromJson(lsResp, JsonObject.class);
            if (!lsRoot.has("data") || lsRoot.get("data").isJsonNull()) return count;

            JsonArray lsArr = lsRoot.getAsJsonArray("data");
            log("  找到 " + lsArr.size() + " 个逻辑结构");
            for (int i = 0; i < lsArr.size(); i++) {
                if (stopFlag.get()) { status = "STOPPED"; return count; }
                JsonObject ls = lsArr.get(i).getAsJsonObject();
                String lsId = ls.has("id") ? ls.get("id").getAsString() : ls.get("objectId").getAsString();
                String lsName = ls.has("name") ? ls.get("name").getAsString() : lsId;

                // Delete logics
                count += deleteAll(api, moeApiBase + ApiConstants.MOE_SEARCH_LOGIC,
                    "{\"objectId\":\"" + lsId + "\"}", moeOpts,
                    moeApiBase + ApiConstants.MOE_DEL_LOGIC,
                    "{\"objectId\":\"%s\",\"logicStructureId\":\"" + lsId + "\"}", moeOpts);

                // Delete parameters
                count += deleteAll(api, moeApiBase + ApiConstants.MOE_SEARCH_PARAM,
                    "{\"objectId\":\"" + lsId + "\"}", moeOpts,
                    moeApiBase + ApiConstants.MOE_DEL_PARAM,
                    "{\"objectId\":\"%s\",\"logicStructureId\":\"" + lsId + "\"}", moeOpts);

                // Delete physical schemes
                count += deleteAll(api, moeApiBase + ApiConstants.MOE_SEARCH_SCHEME,
                    "{\"objectId\":\"" + lsId + "\"}", moeOpts,
                    moeApiBase + ApiConstants.MOE_DEL_SCHEME,
                    "{\"objectId\":\"%s\"}", moeOpts);

                // Delete the logic structure root itself
                try {
                    String delResp = api.post(moeApiBase + ApiConstants.MOE_DEL_LS,
                        moeOpts.setData("{\"objectId\":\"" + lsId + "\"}")).text();
                    if (delResp.contains("\"code\":200")) count++;
                } catch (Exception e) { log("  删除逻辑结构异常: " + e.getMessage()); }

                log("  [IND] " + (i + 1) + "/" + lsArr.size() + " 逻辑结构 " + lsName);
                sleep(100);
                progress = count; pushProgress();
            }
        } catch (Exception e) {
            log("  指标清理异常: " + e.getMessage());
        }
        return count;
    }

    private int deleteAll(APIRequestContext api, String searchUrl, String searchData,
                           RequestOptions searchOpts, String deleteUrl, String deleteDataFmt,
                           RequestOptions deleteOpts) {
        int count = 0;
        try {
            String resp = api.post(searchUrl, searchOpts.setData(searchData)).text();
            JsonObject r = new Gson().fromJson(resp, JsonObject.class);
            JsonElement dataEl = r.get("data");
            if (dataEl == null || dataEl.isJsonNull()) return 0;
            // Handle both array and object-wrapped (records/list/rows) responses
            JsonArray arr = null;
            if (dataEl.isJsonArray()) {
                arr = dataEl.getAsJsonArray();
            } else if (dataEl.isJsonObject()) {
                JsonObject dataObj = dataEl.getAsJsonObject();
                for (String k : new String[]{"list", "records", "rows"}) {
                    if (dataObj.has(k) && dataObj.get(k).isJsonArray()) {
                        arr = dataObj.getAsJsonArray(k);
                        break;
                    }
                }
            }
            if (arr == null) return 0;
            for (JsonElement e : arr) {
                if (stopFlag.get()) return count;
                JsonObject obj = e.getAsJsonObject();
                String id = obj.has("id") ? obj.get("id").getAsString() : obj.get("objectId").getAsString();
                String delResp = api.post(deleteUrl, deleteOpts.setData(String.format(deleteDataFmt, id))).text();
                if (delResp.contains("\"code\":200")) count++;
            }
        } catch (Exception ex) {
            log("  deleteAll异常: " + ex.getMessage());
        }
        return count;
    }

    /** Phase 4: Clean all cooperation areas except those named "A" or "test". */
    private int cleanCooperationAreas(APIRequestContext api, String apiBase, RequestOptions opts, int count) {
        log("--- 第四阶段：清理合作区（保留 A 和 test） ---");
        try {
            String resp = api.get(apiBase + ApiConstants.COOP_SEARCH + "?title=&originated=", opts).text();
            JsonObject root = new Gson().fromJson(resp, JsonObject.class);
            if (!root.has("data") || root.get("data").isJsonNull()) return count;

            JsonArray arr = root.getAsJsonArray("data");
            log("  找到 " + arr.size() + " 个合作区");
            int kept = 0, deleted = 0;
            for (int i = 0; i < arr.size(); i++) {
                if (stopFlag.get()) { status = "STOPPED"; return count; }
                JsonObject item = arr.get(i).getAsJsonObject();
                String name = item.has("name") ? item.get("name").getAsString() : "";
                String title = item.has("title") ? item.get("title").getAsString() : "";
                String objectId = item.has("objectId") ? item.get("objectId").getAsString() : "";
                if (objectId.isEmpty()) continue;

                if ("A".equals(name) || "A".equals(title) || "test".equals(name) || "test".equals(title)) {
                    kept++;
                    log("  [KEEP] " + name + " / " + title);
                    continue;
                }

                String delBody = "[{\"objectId\":\"" + objectId + "\"}]";
                APIResponse delResp = api.post(apiBase + ApiConstants.COOP_DELETE,
                    opts.setData(delBody));
                String delText = delResp.text();
                if (delText.contains("\"code\":200")) {
                    deleted++;
                    log("  [DEL] " + (kept + deleted) + "/" + arr.size() + " " + name + " / " + title);
                } else {
                    log("  [FAIL] " + name + " / " + title + " -> " + delText.substring(0, Math.min(150, delText.length())));
                }
                sleep(100);
            }
            log("  合作区清理完成: 删除" + deleted + "个, 保留" + kept + "个");
            count += deleted;
            progress = count; pushProgress();
        } catch (Exception e) {
            log("  合作区清理异常: " + e.getMessage());
        }
        return count;
    }

    // ── Helpers ──

    private void log(String s) {
        synchronized (logBuf) { logBuf.append(s).append('\n'); }
        if (wsManager != null && currentTaskId != null) {
            wsManager.pushLine(currentTaskId, s);
        }
    }

    private void pushProgress() {
        if (wsManager != null && currentTaskId != null) {
            wsManager.pushProgress(currentTaskId, progress, progressTotal);
        }
    }

    private void pushResult() {
        if (wsManager != null && currentTaskId != null) {
            wsManager.pushResult(currentTaskId, status, "", msg, progress, progressTotal);
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private int countAll(JsonArray dataList) {
        List<JsonObject> all = new ArrayList<>();
        for (JsonElement el : dataList) {
            JsonObject node = el.getAsJsonObject();
            all.add(node);
            collectChildren(node, all);
        }
        return all.size();
    }

    private void collectChildren(JsonObject node, List<JsonObject> acc) {
        if (node.has("children") && !node.get("children").isJsonNull()) {
            for (JsonElement c : node.getAsJsonArray("children")) {
                JsonObject child = c.getAsJsonObject();
                acc.add(child);
                collectChildren(child, acc);
            }
        }
    }
}
