package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class FavoriteApiTest extends ApiTestHelper {

    // ==================== 添加收藏-文件夹 ====================

    @Test
    @DisplayName("QTYL_SC_001: 添加收藏-文件夹(正向)")
    void test_addFavoriteFolder() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        try {
            String resp = api.addFavorite(PROJECT_ID, folderId, "reqSpeFolder");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "添加文件夹收藏应成功, resp: " + resp);
            Assertions.assertNotNull(root.getAsJsonObject("data").get("id"),
                    "应返回收藏ID");
            log.info("QTYL_SC_001 通过: 收藏文件夹 folderId={}", folderId);
        } finally {
            hardCleanFolder(folderId);
        }
    }

    // ==================== 添加收藏-需求规格 ====================

    @Test
    @DisplayName("QTYL_SC_002: 添加收藏-需求规格(正向)")
    void test_addFavoriteReqSpec() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        try {
            String resp = api.addFavorite(PROJECT_ID, docId, "reqSpe", folderId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "添加需求规格收藏应成功, resp: " + resp);
            Assertions.assertNotNull(root.getAsJsonObject("data").get("id"),
                    "应返回收藏ID");
            log.info("QTYL_SC_002 通过: 收藏需求规格 docId={}", docId);
        } finally {
            hardCleanFolder(folderId);
        }
    }

    // ==================== 搜索收藏列表 ====================

    @Test
    @DisplayName("QTYL_SC_003: 搜索收藏列表(正向)")
    void test_searchFavoriteList() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        try {
            api.addFavorite(PROJECT_ID, folderId, "reqSpeFolder");

            String resp = api.searchFavoriteList(PROJECT_ID);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "搜索收藏列表应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("QTYL_SC_003 通过: 收藏列表dataSize={}",
                    root.getAsJsonArray("data").size());
        } finally {
            hardCleanFolder(folderId);
        }
    }

    // ==================== 删除收藏 ====================

    @Test
    @DisplayName("QTYL_SC_004: 删除收藏(正向)")
    void test_deleteFavorite() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        try {
            String addResp = api.addFavorite(PROJECT_ID, folderId, "reqSpeFolder");
            JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
            Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "添加收藏应成功");
            String favoriteId = addRoot.getAsJsonObject("data").get("id").getAsString();

            String resp = api.deleteFavorite(favoriteId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除收藏应成功, resp: " + resp);
            log.info("QTYL_SC_004 通过: 删除收藏 favoriteId={}", favoriteId);
        } finally {
            hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("QTYL_SC_005: 收藏完整流程(添加→搜索→删除→验证)")
    void test_favoriteFullCycle() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        String folderName = folder[1];
        try {
            // 1. Add favorite
            String addResp = api.addFavorite(PROJECT_ID, folderId, "reqSpeFolder");
            JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
            Assertions.assertEquals(200, addRoot.get("code").getAsInt(), "添加收藏应成功");
            String favoriteId = addRoot.getAsJsonObject("data").get("id").getAsString();

            // 2. Verify in list
            String listResp = api.searchFavoriteList(PROJECT_ID);
            JsonObject listRoot = JsonParser.parseString(listResp).getAsJsonObject();
            Assertions.assertEquals(200, listRoot.get("code").getAsInt(), "搜索列表应成功");
            JsonArray data = listRoot.getAsJsonArray("data");
            boolean found = false;
            for (int i = 0; i < data.size(); i++) {
                if (favoriteId.equals(data.get(i).getAsJsonObject().get("id").getAsString())) {
                    found = true;
                    break;
                }
            }
            Assertions.assertTrue(found, "收藏列表中应包含刚添加的收藏");

            // 3. Delete
            String delResp = api.deleteFavorite(favoriteId);
            JsonObject delRoot = JsonParser.parseString(delResp).getAsJsonObject();
            Assertions.assertEquals(200, delRoot.get("code").getAsInt(), "删除收藏应成功");

            // 4. Verify removed from list
            String listResp2 = api.searchFavoriteList(PROJECT_ID);
            JsonObject listRoot2 = JsonParser.parseString(listResp2).getAsJsonObject();
            JsonArray data2 = listRoot2.getAsJsonArray("data");
            boolean stillFound = false;
            for (int i = 0; i < data2.size(); i++) {
                if (favoriteId.equals(data2.get(i).getAsJsonObject().get("id").getAsString())) {
                    stillFound = true;
                    break;
                }
            }
            Assertions.assertFalse(stillFound, "删除后收藏列表不应再包含该收藏");

            log.info("QTYL_SC_005 通过: 完整流程 favoriteId={}", favoriteId);
        } finally {
            hardCleanFolder(folderId);
        }
    }

    // ==================== 添加收藏-负向 ====================

    @Test
    @DisplayName("QTYL_SC_006: 添加收藏-空projectId(负向)")
    void test_addFavoriteEmptyProjectId() {
        String resp = api.addFavorite("", "someObjectId", "reqSpeFolder");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_006: 空projectId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SC_007: 添加收藏-空objectId(负向)")
    void test_addFavoriteEmptyObjectId() {
        String resp = api.addFavorite(PROJECT_ID, "", "reqSpeFolder");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "空objectId应被拦截, resp: ");
        log.info("QTYL_SC_007 通过: 空objectId被拦截, code={}", code);
    }

    @Test
    @DisplayName("QTYL_SC_008: 添加收藏-空type(负向)")
    void test_addFavoriteEmptyType() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        try {
            String resp = api.addFavorite(PROJECT_ID, folderId, "");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("QTYL_SC_008: 空type code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("QTYL_SC_009: 添加收藏-无效objectId(负向)")
    void test_addFavoriteInvalidObjectId() {
        String resp = api.addFavorite(PROJECT_ID, "invalid_object_99999", "reqSpeFolder");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_009: 无效objectId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SC_010: 添加收藏-重复收藏(负向)")
    void test_addFavoriteDuplicate() {
        String[] folder = createTempFolder();
        String folderId = folder[0];
        try {
            String resp1 = api.addFavorite(PROJECT_ID, folderId, "reqSpeFolder");
            Assertions.assertEquals(200,
                    JsonParser.parseString(resp1).getAsJsonObject().get("code").getAsInt(),
                    "首次添加应成功");

            String resp2 = api.addFavorite(PROJECT_ID, folderId, "reqSpeFolder");
            JsonObject root2 = JsonParser.parseString(resp2).getAsJsonObject();
            int code2 = root2.get("code").getAsInt();
            log.info("QTYL_SC_010: 重复收藏 code={}, msg={}",
                    code2, root2.has("msg") ? root2.get("msg").getAsString() : "");
        } finally {
            hardCleanFolder(folderId);
        }
    }

    // ==================== 删除收藏-负向 ====================

    @Test
    @DisplayName("QTYL_SC_011: 删除收藏-空favoriteId(负向)")
    void test_deleteFavoriteEmptyId() {
        String resp = api.deleteFavorite("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_011: 空favoriteId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SC_012: 删除收藏-无效favoriteId(负向)")
    void test_deleteFavoriteInvalidId() {
        String resp = api.deleteFavorite("invalid_fav_99999");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_012: 无效favoriteId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 搜索收藏列表-负向 ====================

    @Test
    @DisplayName("QTYL_SC_013: 搜索收藏列表-空projectId(负向)")
    void test_searchFavoriteListEmptyProjectId() {
        String resp = api.searchFavoriteList("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_013: 空projectId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 获取打开模式 ====================

    @Test
    @DisplayName("QTYL_SC_014: 获取打开模式(正向)")
    void test_getOpenModel() {
        String[] doc = createTempDoc();
        String docId = doc[0];
        String folderId = doc[2];
        try {
            String resp = api.getOpenModel(docId, "true", "admin");
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "获取打开模式应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("QTYL_SC_014 通过: 获取打开模式 docId={}", docId);
        } finally {
            hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("QTYL_SC_015: 获取打开模式-空objectId(负向)")
    void test_getOpenModelEmptyId() {
        String resp = api.getOpenModel("", "true", "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_015: 空objectId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("QTYL_SC_016: 获取打开模式-无效objectId(负向)")
    void test_getOpenModelInvalidId() {
        String resp = api.getOpenModel("invalid_id_99999", "true", "admin");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("QTYL_SC_016: 无效objectId code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
