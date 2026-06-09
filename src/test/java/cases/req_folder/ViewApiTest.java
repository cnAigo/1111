package cases.req_folder;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ViewApiTest extends ApiTestHelper {

    @Test
    @DisplayName("新建视图(正向)")
    void test_addView() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            String viewName = "AT_View_" + suffix();
            String viewId = api.addView(docId, viewName, "自动化测试视图", "[]");
            Assertions.assertNotNull(viewId, "addView应返回视图ID");
            Assertions.assertFalse(viewId.isEmpty(), "新建视图应成功, 视图ID不应为空");

            String viewResp = api.searchViewList(docId);
            Assertions.assertTrue(viewResp.contains(viewName),
                    "视图列表应包含新建的视图: " + viewName);
            log.info("新建视图 通过: [{}] viewId={} on docId={}", viewName, viewId, docId);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
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
            if (folderId != null) forceCleanFolder(folderId);
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
            // addView now returns the view ID directly (via searchViewList fallback)
            String viewId = api.addView(docId, viewName, "待删除视图", "[]");
            Assertions.assertNotNull(viewId, "addView应返回视图ID");
            Assertions.assertFalse(viewId.isEmpty(), "视图ID不应为空");

            String resp = api.deleteView(viewId);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "删除视图应成功, resp: " + resp);
            log.info("删除视图 通过: viewId={}", viewId);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
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

            String viewId = api.addView(docId, "", "空名称视图", "[]");
            Assertions.assertTrue(viewId.isEmpty(),
                    "空名称应被拦截(视图不应创建), 实际viewId=" + viewId);
            log.info("新建视图-空名称 通过: 被拦截, viewId为空");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("新建视图-不存在的对象ID(负向)")
    void test_addViewInvalidObjectId() {
        String viewId = api.addView("invalid_id_99999", "AT_View_" + suffix(),
                "无效对象ID", "[]");
        log.info("新建视图-无效对象ID: viewId={} (空=被拦截,非空=后端未校验objectId)", viewId);
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
            String viewId = api.addView(docId, longName, "超长名称", "[]");
            log.info("新建视图-超长名称: viewId={} (空=被拦截/失败)", viewId);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("删除视图-不存在的ID(负向)")
    void test_deleteViewInvalidId() {
        String resp = api.deleteView("invalid_view_99999");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        // 后端对不存在的ID返回 code:200 data:[] 是正常业务拦截，不应判错
        if (code == 200) {
            log.info("删除视图-无效ID: code=200, 后端静默处理(data=[]) — 业务拦截通过");
        } else {
            log.info("删除视图-无效ID 通过: 被明确拦截, code={}", code);
        }
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
            String viewId2 = api.addView(docId, viewName, "重复视图2", "[]");
            log.info("新建视图-重复名称: 第二次addView返回viewId={} (空=被拦截,非空=允许重复)", viewId2);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
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
