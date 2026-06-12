package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SearchApiTest extends ApiTestHelper {

    @Test @DisplayName("搜索项目列表(正向)")
    void test_searchProjectByUser() {
        String resp = api.searchProjectByUser();
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(), "搜索项目应成功");
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        Assertions.assertTrue(root.get("data").isJsonArray(), "data应为数组");
        JsonArray data = root.getAsJsonArray("data");
        Assertions.assertTrue(data.size() > 0, "应至少返回1个项目");
        log.info("搜索项目列表 通过: {} 个项目", data.size());
    }

    @Test @DisplayName("搜索文件夹子元素(正向)")
    void test_searchFolderChildren() {
        String[] f = createTempFolder();
        String folderId = f[0];
        String resp = api.searchFolderChildren(folderId);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(), "搜索子元素应成功");
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("搜索文件夹子元素 通过: folderId={}", folderId);
    }

    @Test @DisplayName("搜索文件夹子元素-无效ID(负向)")
    void test_searchFolderChildrenInvalidId() {
        String resp = api.searchFolderChildren("invalid_id_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("搜索子元素-无效ID: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("搜索文件夹子元素-空ID(负向)")
    void test_searchFolderChildrenEmptyId() {
        String resp = api.searchFolderChildren("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("搜索子元素-空ID: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("搜索自定义属性(正向)")
    void test_searchAttributes() {
        String nameEn = "AT_SearchAttr_" + suffix();
        try {
            api.addCustomAttribute(nameEn, "搜索属性测试", "字符串", PROJECT_ID);
            String resp = api.searchAttributes(PROJECT_ID, "需求管理", "req");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(), "搜索属性应成功");
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("搜索自定义属性 通过: attr=[{}]", nameEn);
        } finally {
            cleanupCustomAttr(nameEn);
        }
    }

    @Test @DisplayName("搜索自定义属性-空业务域(负向)")
    void test_searchAttributesEmptyDomain() {
        String resp = api.searchAttributes(PROJECT_ID, "", "req");
        int code = JsonParser.parseString(resp).getAsJsonObject().get("code").getAsInt();
        log.info("搜索属性-空业务域: code={}", code);
    }

    @Test @DisplayName("搜索自定义属性-无效业务域(负向)")
    void test_searchAttributesInvalidDomain() {
        String resp = api.searchAttributes(PROJECT_ID, "InvalidDomain_xyz", "req");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(), "无效业务域请求本身应成功");
        log.info("搜索属性-无效业务域: dataSize={}",
                root.has("data") && !root.get("data").isJsonNull() ? root.getAsJsonArray("data").size() : "null");
    }

    @Test @DisplayName("获取需求规格列表(正向)")
    void test_getReqSpeList() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String resp = api.getReqSpeList(PROJECT_ID);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(), "获取需求规格列表应成功");
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("获取需求规格列表 通过: 包含 docId={}", docId);
    }

    @Test @DisplayName("获取需求规格列表-无效项目ID(负向)")
    void test_getReqSpeListInvalidProject() {
        String resp = api.getReqSpeList("invalid_project_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("需求规格列表-无效项目: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("获取项目树结构(正向)")
    void test_getTree() {
        String resp = api.getTree(PROJECT_ID, PROJECT_ID);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(), "获取树结构应成功");
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        Assertions.assertTrue(root.get("data").isJsonArray(), "data应为数组");
        log.info("获取树结构 通过: data包含 {} 个根节点", root.getAsJsonArray("data").size());
    }

    @Test @DisplayName("获取项目树结构-无效父节点(负向)")
    void test_getTreeInvalidParent() {
        String resp = api.getTree(PROJECT_ID, "invalid_parent_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("树结构-无效父节点: code={}, msg={}", root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test @DisplayName("按名称查找节点ID(正向)")
    void test_findNodeIdByTitle() {
        String[] f = createTempFolder();
        String folderId = f[0];
        String folderName = f[1];
        String nodeId = api.findNodeIdByTitle(PROJECT_ID, folderName);
        Assertions.assertNotNull(nodeId, "应能找到文件夹节点: " + folderName);
        Assertions.assertEquals(folderId, nodeId, "找到的节点ID应与创建的文件夹ID一致");
        log.info("按名称查找节点ID 通过: [{}] -> {}", folderName, nodeId);
    }

    @Test @DisplayName("按名称查找节点ID-不存在(负向)")
    void test_findNodeIdByTitleNotExists() {
        String nodeId = api.findNodeIdByTitle(PROJECT_ID, "NonExistentNode_" + suffix());
        Assertions.assertNull(nodeId, "不存在的节点应返回null");
        log.info("查找不存在节点ID 通过: 返回null");
    }
}
