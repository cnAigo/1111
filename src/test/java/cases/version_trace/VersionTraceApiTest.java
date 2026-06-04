package cases.version_trace;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("VersionTraceModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class VersionTraceApiTest extends ApiTestHelper {

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

    @Test
    @DisplayName("查询变更分析-空masterId(负向)")
    void test_searchChangeAnalysisEmptyMasterId() {
        String resp = api.searchChangeAnalysis("", "1");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("变更分析-空masterId: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 版本列表边界 ====================

    @Test
    @DisplayName("获取版本列表-空ID(负向)")
    void test_getVersionListEmptyId() {
        String resp = api.getVersionList("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("获取版本列表-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 追溯搜索边界 ====================

    @Test
    @DisplayName("搜索需求规格追溯-特殊字符(负向)")
    void test_searchReqSpecTraceSpecialChars() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchReqSpecTrace(docId, "<script>alert(1)</script>");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("搜索追溯-特殊字符: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("搜索需求规格追溯-空ID(负向)")
    void test_searchReqSpecTraceEmptyId() {
        String resp = api.searchReqSpecTrace("", "需求");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索追溯-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 打开模式检查 ====================

    @Test
    @DisplayName("检查打开模式(正向)")
    void test_checkOpenMode() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.checkOpenMode(docId, "edit", "admin");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "检查打开模式应成功, resp: " + resp);
            log.info("检查打开模式 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("检查打开模式-无效masterId(负向)")
    void test_checkOpenModeInvalidId() {
        String resp = api.checkOpenMode("invalid_id_99999", "edit", "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("检查打开模式-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("检查打开模式-空操作类型(负向)")
    void test_checkOpenModeEmptyType() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.checkOpenMode(docId, "", "admin");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("检查打开模式-空类型: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 搜索文件夹子项 ====================

    @Test
    @DisplayName("搜索文件夹子项(正向)")
    void test_searchFolderChildren() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            // search children under the folder that contains the doc
            String resp = api.searchFolderChildren(folderId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "搜索文件夹子项应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("搜索文件夹子项 通过: folderId={}", folderId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("搜索文件夹子项-无效ID(负向)")
    void test_searchFolderChildrenInvalidId() {
        String resp = api.searchFolderChildren("invalid_id_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索文件夹子项-无效ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("搜索文件夹子项-空ID(负向)")
    void test_searchFolderChildrenEmptyId() {
        String resp = api.searchFolderChildren("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索文件夹子项-空ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 搜索属性 ====================

    @Test
    @DisplayName("搜索需求属性(正向)")
    void test_searchAttributes() {
        String resp = api.searchAttributes(PROJECT_ID, "req", "reqSpe");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "搜索属性应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("搜索属性 通过");
    }

    @Test
    @DisplayName("搜索需求属性-空项目ID(负向)")
    void test_searchAttributesEmptyProject() {
        String resp = api.searchAttributes("", "req", "reqSpe");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索属性-空项目ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("搜索需求属性-空业务域(负向)")
    void test_searchAttributesEmptyDomain() {
        String resp = api.searchAttributes(PROJECT_ID, "", "reqSpe");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("搜索属性-空业务域: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
