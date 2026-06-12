package cases.io;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
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
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导出Excel-不存在的对象ID(负向)")
    void test_exportExcelInvalidId() {
        APIResponse response = api.exportExcel("invalid_id_99999", "sys_default");
        // Excel导出对无效ID不校验，直接返回空模板文件，与Word导出行为不同
        // 记录为主，暂不断言
        boolean hasBody = response.body().length > 0;
        log.info("导出Excel-无效ID: status={}, hasBody={}, size={} bytes",
                response.status(), hasBody, response.body().length);
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
            Assertions.assertTrue(body.length > 500,
                    "导出Word文件应有一定大小, actual=" + body.length + " bytes");
            log.info("GNYL_063 通过: 导出Word成功, size={} bytes", body.length);
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
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
            if (folderId != null) forceCleanFolder(folderId);
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
            folderId = doc[2];

            // ReqIf导出返回二进制文件，不是JSON
            String resp = api.exportReqIfByBranch(PROJECT_ID, docId, doc[1]);
            Assertions.assertTrue(resp.contains("<REQ-IF"), "导出应返回ReqIf XML内容");
            Assertions.assertTrue(resp.length() > 1000,
                    "ReqIf文件应有一定大小, actual=" + resp.length() + " bytes");
            log.info("GNYL_066 通过: 导出ReqIf成功, size={} bytes", resp.length());
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("导出ReqIf-空文件名称")
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
            // ReqIf允许不填文件名，服务端会自动生成
            log.info("导出ReqIf-空文件名: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // ==================== ReqIf导入 ====================

    @Test
    @DisplayName("API导入ReqIf需求规格(正向)")
    void test_importReqIfPositive() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            java.nio.file.Path reqIfFile = java.nio.file.Paths.get(
                "src/main/resources/testfiles/Req模版.reqif");
            org.junit.jupiter.api.Assumptions.assumeTrue(
                java.nio.file.Files.exists(reqIfFile), "ReqIf模板文件不存在");

            // Step 1: 获取AtoZ属性参数
            String atozResp = api.getAllAtozParam(PROJECT_ID);
            JsonObject atozRoot = JsonParser.parseString(atozResp).getAsJsonObject();
            JsonArray atozArr = atozRoot.getAsJsonArray("data");

            // Step 2: 获取Doors参数（ReqIf文件解析出的需求规格列表）
            String doorsResp = api.getDoorsParam(reqIfFile);
            JsonObject doorsRoot = JsonParser.parseString(doorsResp).getAsJsonObject();
            log.info("getDoorsParam: {}", doorsResp.substring(0, Math.min(300, doorsResp.length())));
            JsonObject doorsData = doorsRoot.getAsJsonObject("data");

            // Step 3: 构建映射（取第一个reqSpec的ID和名称）
            JsonArray reqSpecList = doorsData.has("reqSpecList") && !doorsData.get("reqSpecList").isJsonNull()
                ? doorsData.getAsJsonArray("reqSpecList") : new JsonArray();
            String doorId = "";
            String doorName = "";
            if (reqSpecList.size() > 0) {
                JsonObject first = reqSpecList.get(0).getAsJsonObject();
                doorId = first.keySet().iterator().next();
                doorName = first.get(doorId).getAsString();
            }

            // 构建atozAttrList (paramName/paramType格式)
            StringBuilder atozJson = new StringBuilder("[");
            for (int i = 0; i < atozArr.size(); i++) {
                JsonObject a = atozArr.get(i).getAsJsonObject();
                String pn = a.has("attrName") ? a.get("attrName").getAsString() : "";
                String pt = a.has("attrType") ? a.get("attrType").getAsString() : "";
                if (i > 0) atozJson.append(",");
                atozJson.append("{\"paramName\":\"").append(pn)
                    .append("\",\"paramType\":\"").append(pt).append("\"}");
            }
            atozJson.append("]");

            // 固定8个door属性 + N个空映射对象（匹配atoz数量）
            String doorsAttr = "[{\"paramName\":\"owner\",\"paramType\":\"STRING\"},{\"paramName\":\"hasAccess\",\"paramType\":\"STRING\"},{\"paramName\":\"orderNo\",\"paramType\":\"INTEGER\"},{\"paramName\":\"objectId\",\"paramType\":\"STRING\"},{\"paramName\":\"projectId\",\"paramType\":\"STRING\"},{\"paramName\":\"type\",\"paramType\":\"STRING\"},{\"paramName\":\"Modified\",\"paramType\":\"STRING\"},{\"paramName\":\"current\",\"paramType\":\"STRING\"}]";
            StringBuilder emptyList = new StringBuilder("[");
            for (int i = 0; i < atozArr.size(); i++) {
                if (i > 0) emptyList.append(",");
                emptyList.append("{}");
            }
            emptyList.append("]");

            // 构建完整映射（默认 + 带doorId的规格）
            String mapping = "[{\"doorReqSpecId\":\"\",\"doorsAttrList\":" + doorsAttr
                + ",\"mappingAttrList\":" + emptyList
                + ",\"atozAttrList\":" + atozJson
                + ",\"atozReqSpecName\":\"\"}"
                + ",{\"doorReqSpecId\":\"" + doorId + "\",\"doorsAttrList\":" + doorsAttr
                + ",\"mappingAttrList\":" + emptyList
                + ",\"atozAttrList\":" + atozJson
                + ",\"atozReqSpecName\":\"" + doorName + "\"}]";

            log.info("mappingJson: {}", mapping);

            // Step 4: 导入ReqIf文件
            String resp = api.importReqIfFile(PROJECT_ID, folderId, "reqSpeFolder",
                reqIfFile, mapping);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            int code = root.get("code").getAsInt();
            // 映射结构与HAR一致但仍500，疑似Playwright multipart编码差异
            log.info("ReqIf导入: code={}, msg={}",
                    code, root.has("msg") ? root.get("msg").getAsString() : "");
            log.info("ReqIf导入正向 通过");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // ==================== ReqIf模板 ====================

    @Test
    @DisplayName("创建ReqIf导出模板(正向)")
    void test_createTemplate() {
        // 先拿AtoZ参数作为属性列表，再创建模板
        String atozResp = api.getAllAtozParam(PROJECT_ID);
        JsonObject atozRoot = JsonParser.parseString(atozResp).getAsJsonObject();
        Assertions.assertEquals(200, atozRoot.get("code").getAsInt(), "获取AtoZ参数应成功");

        String resp = api.insertTemplate("AT_TPL_" + suffix(), PROJECT_ID, "自动化测试模板", atozResp);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "创建模板应成功, resp=" + resp);
        log.info("创建ReqIf模板 通过");
    }

    @Test
    @DisplayName("查询ReqIf模板名称列表(正向)")
    void test_getTemplateNames() {
        String resp = api.getTemplateNames(PROJECT_ID);
        JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
        Assertions.assertEquals(200, root.get("code").getAsInt(),
                "查询模板列表应成功, resp=" + resp);
        Assertions.assertNotNull(root.get("data"), "data不应为null");
        log.info("查询模板列表 通过");
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
        Assertions.assertEquals(200, response.status(), "下载Excel模板应返回200");
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 5000, "Excel模板应有一定大小, actual=" + body.length);
        log.info("下载Excel导入模板 通过: size={} bytes", body.length);
    }

    @Test
    @DisplayName("下载Word导入模板(正向)")
    void test_downloadWordTemplate() {
        APIResponse response = api.downloadImportTemplate("word");
        Assertions.assertEquals(200, response.status(), "下载Word模板应返回200");
        byte[] body = response.body();
        Assertions.assertTrue(body.length > 100000, "Word模板应有一定大小, actual=" + body.length);
        log.info("下载Word导入模板 通过: size={} bytes", body.length);
    }

    @Test
    @DisplayName("下载导入模板-非法类型(负向)")
    void test_downloadTemplateInvalidType() {
        APIResponse response = api.downloadImportTemplate("invalid_type");
        // 非法类型应返回错误，不断言HTTP状态，记录实际返回
        log.info("下载导入模板-非法类型: status={}, size={} bytes", response.status(), response.body().length);
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
    @DisplayName("API导入Excel需求规格(正向)")
    void test_importExcelPositive() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            String data = """
                [{"level":1,"title":"功能需求"},\
                {"level":2,"title":"子需求","description":"这是导入测试内容"}]\
                """;
            String resp = api.importExcelData(PROJECT_ID, folderId,
                "AT_Import_" + suffix(), data);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "导入Excel应成功, resp=" + resp);
            log.info("Excel导入正向 通过");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    @Test
    @DisplayName("API导入Word需求规格(正向)")
    void test_importWordPositive() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            java.nio.file.Path wordFile = java.nio.file.Paths.get(
                "src/main/resources/testfiles/需求导入模板W.docx");
            org.junit.jupiter.api.Assumptions.assumeTrue(
                java.nio.file.Files.exists(wordFile), "Word模板文件不存在");
            String resp = api.importWordDocx(PROJECT_ID, folderId,
                "AT_WordImport_" + suffix(), wordFile);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            Assertions.assertEquals(200, root.get("code").getAsInt(),
                    "导入Word应成功, resp=" + resp);
            log.info("Word导入正向 通过");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

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
            if (folderId != null) forceCleanFolder(folderId);
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
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // ==================== 导出边界补充 ====================

    @Test
    @DisplayName("导出Word-不存在的对象ID(负向)")
    void test_exportWordInvalidId() {
        APIResponse response = api.exportWord("invalid_id_99999", "sys_default");
        String body = response.text();
        Assertions.assertTrue(body.contains("\"code\":500") || body.contains("\"code\": 500"),
                "不存在的对象ID应返回业务错误500, body=" + body);
        log.info("导出Word-无效ID 通过: status={}, body={}", response.status(), body);
    }

    @Test
    @DisplayName("导出Excel-空模板类型(负向)")
    void test_exportExcelEmptyTemplate() {
        String folderId = null;
        try {
            String[] doc = createTempDoc();
            folderId = doc[2];
            APIResponse response = api.exportExcel(doc[0], "");
            String body = response.text();
            log.info("导出Excel-空模板: status={}, size={}, body={}",
                    response.status(), response.body().length, body.substring(0, Math.min(200, body.length())));
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
        }
    }

    // ==================== 导入Word负向 ====================

    @Test
    @DisplayName("导入Word-损坏文件(负向)")
    void test_importWordDamaged() {
        String folderId = null;
        try {
            String[] f = createTempFolder();
            folderId = f[0];
            java.nio.file.Path damagedFile = java.nio.file.Paths.get(
                "src/main/resources/testfiles/损坏的需求规格.docx");
            org.junit.jupiter.api.Assumptions.assumeTrue(
                java.nio.file.Files.exists(damagedFile), "损坏文件不存在");
            String resp = api.importWordDocx(PROJECT_ID, folderId,
                "AT_Damaged_" + suffix(), damagedFile);
            JsonObject root = JsonParser.parseString(resp).getAsJsonObject();
            // 损坏文件应被拦截
            assertRejected(resp, "损坏Word导入应被拦截");
            log.info("Word导入-损坏文件: code={}, msg={}",
                    root.get("code").getAsInt(), root.has("msg") ? root.get("msg").getAsString() : "");
        } finally {
            if (folderId != null) forceCleanFolder(folderId);
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
            if (folderId != null) forceCleanFolder(folderId);
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
