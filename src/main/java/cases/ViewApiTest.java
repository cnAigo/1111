package cases;

import base.BaseTest;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ViewApiTest extends BaseTest {

    @Test
    @DisplayName("新建视图(正向)")
    void test_addView() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String viewName = "AT_View_" + suffix();
            String resp = api.addView(docId, viewName, "自动化测试视图", "[]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "新建视图应成功, resp: " + resp);

            String viewResp = api.searchViewList(docId);
            Assertions.assertTrue(viewResp.contains(viewName),
                    "视图列表应包含新建的视图: " + viewName);
            log.info("新建视图 通过: [{}] on docId={}", viewName, docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询视图列表(正向)")
    void test_searchViewList() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.searchViewList(docId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "查询视图列表应成功, resp: " + resp);
            Assertions.assertNotNull(root.get("data"), "data不应为null");
            log.info("查询视图列表 通过: docId={}", docId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("删除视图(正向)")
    void test_deleteView() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String viewName = "AT_ViewDel_" + suffix();
            String addResp = api.addView(docId, viewName, "待删除视图", "[]");
            JsonObject addRoot = JsonParser.parseString(addResp).getAsJsonObject();
            Assertions.assertEquals(200, addRoot.get("code").getAsInt(),
                    "新建视图应成功, resp: " + addResp);

            String viewId = addRoot.getAsJsonObject("data").get("id").getAsString();
            String resp = api.deleteView(viewId);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除视图应成功, resp: " + resp);
            log.info("删除视图 通过: viewId={}", viewId);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("新建视图-空名称(负向)")
    void test_addViewEmptyName() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String resp = api.addView(docId, "", "空名称视图", "[]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            Assertions.assertNotEquals(200, code,
                    "空名称应被拦截, 实际code=" + code + ", resp: " + resp);
            log.info("新建视图-空名称 通过: 被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("新建视图-不存在的对象ID(负向)")
    void test_addViewInvalidObjectId() {
        String resp = api.addView("invalid_id_99999", "AT_View_" + suffix(),
                "无效对象ID", "[]");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "不存在的对象ID应被拦截, 实际code=" + code + ", resp: " + resp);
        log.info("新建视图-无效对象ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("新建视图-超长名称(负向)")
    void test_addViewTooLongName() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String longName = "V".repeat(200);
            String resp = api.addView(docId, longName, "超长名称", "[]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("新建视图-超长名称: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("删除视图-不存在的ID(负向)")
    void test_deleteViewInvalidId() {
        String resp = api.deleteView("invalid_view_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        Assertions.assertNotEquals(200, code,
                "不存在的视图ID应失败, 实际code=" + code + ", resp: " + resp);
        log.info("删除视图-无效ID 通过: 被拦截, code={}", code);
    }

    @Test
    @DisplayName("新建视图-重复名称(负向)")
    void test_addViewDuplicateName() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String viewName = "AT_ViewDup_" + suffix();
            api.addView(docId, viewName, "重复视图1", "[]");

            String resp = api.addView(docId, viewName, "重复视图2", "[]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            log.info("新建视图-重复名称: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("查询视图列表-不存在的对象ID(负向)")
    void test_searchViewListInvalidId() {
        String resp = api.searchViewList("invalid_id_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        log.info("查询视图列表-无效ID: code={}, msg={}",
                root.get("code").getAsInt(),
                root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
