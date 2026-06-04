package cases.io;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.APIResponse;
import org.junit.jupiter.api.*;

@Tag("IOModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ExportImportApiTest extends ApiTestHelper {

    // ==================== 导出 Excel ====================

    @Test
    @DisplayName("GNYL_060: API导出Excel(正向)")
    void test_exportExcel() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            APIResponse response = api.exportExcel(docId, "sys_default");

            Assertions.assertTrue(response.ok(),
                    "导出请求应成功, status=" + response.status());
            Assertions.assertEquals(200, response.status(),
                    "HTTP状态码应为200");

            byte[] body = response.body();
            Assertions.assertTrue(body.length > 0, "导出文件不应为空");
            Assertions.assertTrue(body.length > 100,
                    "导出文件应有一定大小, actual=" + body.length + " bytes");

            String contentType = response.headers().get("content-type");
            log.info("GNYL_060 通过: 导出Excel成功, size={} bytes{}",
                    body.length, contentType != null ? ", contentType=" + contentType : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导出Excel-不存在的对象ID(负向)")
    void test_exportExcelInvalidId() {
        APIResponse response = api.exportExcel("invalid_id_99999", "sys_default");
        Assertions.assertFalse(response.ok(),
                "不存在的对象ID应返回错误, status=" + response.status());
        log.info("导出Excel-无效ID 通过: status={}", response.status());
    }

    // ==================== 导出 Word ====================

    @Test
    @DisplayName("GNYL_063: API导出Word(正向)")
    void test_exportWord() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            APIResponse response = api.exportWord(docId, "sys_default");

            Assertions.assertTrue(response.ok(),
                    "导出请求应成功, status=" + response.status());

            byte[] body = response.body();
            Assertions.assertTrue(body.length > 0, "导出文件不应为空");
            Assertions.assertTrue(body.length > 100,
                    "导出文件应有一定大小, actual=" + body.length + " bytes");
            log.info("GNYL_063 通过: 导出Word成功, size={} bytes", body.length);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导出Word-空模板类型(负向)")
    void test_exportWordEmptyTemplate() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            folderId = doc[2];

            APIResponse response = api.exportWord(docId, "");

            log.info("导出Word-空模板: status={}, size={} bytes", response.status(),
                    response.body().length);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 导出 ReqIf ====================

    @Test
    @DisplayName("GNYL_066: API导出ReqIf(正向)")
    void test_exportReqIf() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            String docId = doc[0];
            String docName = doc[1];
            folderId = doc[2];

            String atozResp = api.getAllAtozParam(PROJECT_ID);
            JsonObject atozRoot = JsonParser.parseString(atozResp).getAsJsonObject();
            Assertions.assertEquals(200, atozRoot.get("code").getAsInt(),
                    "获取ReqIf参数应成功, resp: " + atozResp);

            String payload = """
                    {
                        "reqIfFileName": "AT_ReqIfExport_%s",
                        "parentId": "%s",
                        "selectedList": [{"objectId": "%s", "type": "reqSpe"}],
                        "attributeList": [],
                        "projectId": "%s"
                    }
                    """.formatted(suffix(), folderId, docId, PROJECT_ID);

            String resp = api.exportReqIf(payload);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "导出ReqIf应成功, resp: " + resp);
            log.info("GNYL_066 通过: 导出ReqIf成功, docName={}", docName);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导出ReqIf-空文件名称(负向)")
    void test_exportReqIfEmptyFileName() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];

            String payload = """
                    {
                        "reqIfFileName": "",
                        "parentId": "%s",
                        "selectedList": [],
                        "attributeList": [],
                        "projectId": "%s"
                    }
                    """.formatted(folderId, PROJECT_ID);

            String resp = api.exportReqIf(payload);

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "空ReqIf文件名称应被拦截");
            log.info("导出ReqIf-空文件名 通过: 被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 获取AtoZ参数 ====================

    @Test
    @DisplayName("获取ReqIf AtoZ参数(正向)")
    void test_getAllAtozParam() {
        String resp = api.getAllAtozParam(PROJECT_ID);

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "获取AtoZ参数应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("获取AtoZ参数 通过");
    }

    // ==================== 导入模板下载 ====================

    @Test
    @DisplayName("下载Excel导入模板(正向)")
    void test_downloadExcelTemplate() {
        APIResponse response = api.downloadImportTemplate("excel");

        Assertions.assertTrue(response.ok(),
                "下载Excel模板应成功, status=" + response.status());
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 0, "模板文件不应为空");
        Assertions.assertTrue(body.length > 500,
                "模板文件应有一定大小, actual=" + body.length + " bytes");
        log.info("下载Excel导入模板 通过: size={} bytes", body.length);
    }

    @Test
    @DisplayName("下载Word导入模板(正向)")
    void test_downloadWordTemplate() {
        APIResponse response = api.downloadImportTemplate("word");

        Assertions.assertTrue(response.ok(),
                "下载Word模板应成功, status=" + response.status());
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 0, "模板文件不应为空");
        log.info("下载Word导入模板 通过: size={} bytes", body.length);
    }

    @Test
    @DisplayName("下载ReqIf导入模板(正向)")
    void test_downloadReqIfTemplate() {
        APIResponse response = api.downloadImportTemplate("reqif");

        Assertions.assertTrue(response.ok(),
                "下载ReqIf模板应成功, status=" + response.status());
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 0, "模板文件不应为空");
        log.info("下载ReqIf导入模板 通过: size={} bytes", body.length);
    }

    @Test
    @DisplayName("下载导入模板-非法类型(负向)")
    void test_downloadTemplateInvalidType() {
        APIResponse response = api.downloadImportTemplate("invalid_type");

        Assertions.assertFalse(response.ok(),
                "非法模板类型应返回错误, status=" + response.status());
        log.info("下载导入模板-非法类型 通过: status={}", response.status());
    }

    // ==================== 导入属性查询 ====================

    @Test
    @DisplayName("获取导入属性列表(正向)")
    void test_getImportAttributes() {
        String resp = api.getImportAttributes();

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "获取导入属性应成功, resp: " + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("获取导入属性列表 通过");
    }

    // ==================== 导入需求规格 ====================

    @Test
    @DisplayName("GNYL_042: API导入Excel需求规格(负向-空数据)")
    void test_importExcelEmptyData() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];

            String resp = api.importReqSpecification(PROJECT_ID, folderId,
                    "AT_Import_" + suffix(), "[]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "空数据导入应被拦截");
            log.info("GNYL_042 通过: 空数据导入被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("API导入-空需求规格名称(负向)")
    void test_importEmptyName() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];

            String resp = api.importReqSpecification(PROJECT_ID, folderId, "",
                    "[{\"name\":\"test_req\"}]");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "空名称导入应被拦截");
            log.info("导入-空名称 通过: 被拦截, code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    // ==================== 导出边界补充 ====================

    @Test
    @DisplayName("导出Word-不存在的对象ID(负向)")
    void test_exportWordInvalidId() {
        APIResponse response = api.exportWord("invalid_id_99999", "sys_default");
        Assertions.assertFalse(response.ok(),
                "不存在的对象ID应返回错误, status=" + response.status());
        log.info("导出Word-无效ID 通过: status={}", response.status());
    }

    @Test
    @DisplayName("导出Excel-空模板类型(负向)")
    void test_exportExcelEmptyTemplate() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            folderId = doc[2];
            APIResponse response = api.exportExcel(doc[0], "");
            log.info("导出Excel-空模板: status={}, size={} bytes",
                    response.status(), response.body().length);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导出ReqIf-不存在的对象(负向)")
    void test_exportReqIfInvalidObject() {
        String payload = """
                {
                    "reqIfFileName": "AT_InvalidExport",
                    "parentId": "%s",
                    "selectedList": [{"objectId": "invalid_id_99999", "type": "reqSpe"}],
                    "attributeList": [],
                    "projectId": "%s"
                }
                """.formatted(PROJECT_ID, PROJECT_ID);

        String resp = api.exportReqIf(payload);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "不存在的对象导出应被拦截");
        log.info("导出ReqIf-无效对象 通过: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    @Test
    @DisplayName("导出ReqIf-空selectedList(负向)")
    void test_exportReqIfEmptyList() {
        String payload = """
                {
                    "reqIfFileName": "AT_EmptyList",
                    "parentId": "%s",
                    "selectedList": [],
                    "attributeList": [],
                    "projectId": "%s"
                }
                """.formatted(PROJECT_ID, PROJECT_ID);

        String resp = api.exportReqIf(payload);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("导出ReqIf-空selectedList: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }

    // ==================== 导入边界补充 ====================

    @Test
    @DisplayName("导入-非法JSON格式(负向)")
    void test_importMalformedJson() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];

            String resp = api.importReqSpecification(PROJECT_ID, folderId,
                    "AT_Import_" + suffix(), "{invalid json}}}}");

            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            assertRejected(resp, "非法JSON应被拦截");
            log.info("导入-非法JSON 通过: code={}", code);
        } finally {
            if (folderId != null) hardCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导入-不存在的目标文件夹(负向)")
    void test_importNonExistingFolder() {
        String resp = api.importReqSpecification(PROJECT_ID, "nonexistent_id_99999",
                "AT_Import_" + suffix(), "[{\"name\":\"test_req\"}]");

        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        assertRejected(resp, "不存在的目标文件夹导入应失败");
        log.info("导入-不存在的文件夹 通过: code={}", code);
    }

    @Test
    @DisplayName("获取AtoZ参数-空项目ID(负向)")
    void test_getAllAtozParamEmptyProject() {
        String resp = api.getAllAtozParam("");
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        int code = root.get("code").getAsInt();
        log.info("AtoZ参数-空项目ID: code={}, msg={}",
                code, root.has("msg") ? root.get("msg").getAsString() : "");
    }
}
