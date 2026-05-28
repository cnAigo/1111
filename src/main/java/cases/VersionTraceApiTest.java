package cases;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VersionTraceApiTest extends BaseTest {

    // ==================== 版本列表 ====================

    @Test
    @DisplayName("获取需求规格版本列表(正向)")
    void test_getVersionList() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.getVersionList(docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "获取版本列表应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("获取版本列表 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("获取版本列表-无效ID(负向)")
    void test_getVersionListInvalidId() {
        String resp = api.getVersionList("invalid_id_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("获取版本列表-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 需求访问权限 ====================

    @Test
    @DisplayName("获取需求访问权限(正向)")
    void test_getReqAccess() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.getReqAccess(docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "获取访问权限应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("获取访问权限 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("获取需求访问权限-无效ID(负向)")
    void test_getReqAccessInvalidId() {
        String resp = api.getReqAccess("invalid_id_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("获取访问权限-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("获取需求访问权限-空ID(负向)")
    void test_getReqAccessEmptyId() {
        String resp = api.getReqAccess("");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("获取访问权限-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 需求追溯搜索 ====================

    @Test
    @DisplayName("搜索需求规格追溯(正向)")
    void test_searchReqSpecTrace() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchReqSpecTrace(docId, "需求");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "搜索需求追溯应成功, resp: " + resp);
            log.info("搜索需求追溯 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("搜索需求规格追溯-无效ID(负向)")
    void test_searchReqSpecTraceInvalidId() {
        String resp = api.searchReqSpecTrace("invalid_id_99999", "需求");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索追溯-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("搜索需求规格追溯-空类型(负向)")
    void test_searchReqSpecTraceEmptyType() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchReqSpecTrace(docId, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("搜索追溯-空类型: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 变更分析 ====================

    @Test
    @DisplayName("查询变更分析结果(正向)")
    void test_searchChangeAnalysis() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchChangeAnalysis(docId, "1");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询变更分析应成功, resp: " + resp);
            log.info("查询变更分析 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询变更分析-无效masterId(负向)")
    void test_searchChangeAnalysisInvalidId() {
        String resp = api.searchChangeAnalysis("invalid_id_99999", "1");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("变更分析-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("查询变更分析-无效版本号(负向)")
    void test_searchChangeAnalysisInvalidVersion() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchChangeAnalysis(docId, "99999");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("变更分析-无效版本: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询变更分析-空版本号(负向)")
    void test_searchChangeAnalysisEmptyVersion() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchChangeAnalysis(docId, "");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("变更分析-空版本: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }
}
