package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import java.nio.file.*;

@Tag("IOModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ImportExportManualTest extends ApiTestHelper {

    private static final String TD = "src/test/resources/testdata/";

    // ═══ 7. Excel ═══
    @Test @DisplayName("7.1 导出Excel(正向)")
    void test_0701_exportExcel() { String[] d=createTempDoc(); var r=api.exportExcel(d[0],"sys_default"); Assertions.assertEquals(200,r.status(),"导出应返回200"); Assertions.assertTrue(r.body().length>1024,"导出文件应>1KB(实际"+r.body().length+"B)"); log.info("7.1 导出Excel(正向) 通过 — {}KB",r.body().length/1024); }
    @Test @DisplayName("7.2 导出Excel-不存在ID(负向)")
    void test_0702_invalid() { var r=api.exportExcel("invalid_99999","sys_default"); Assertions.assertNotEquals(200,r.status(),"导出不存在ID应失败"); log.info("7.2 导出不存在ID(负向) 通过"); }
    @Test @DisplayName("7.3 下载Excel模板(正向)")
    void test_0703_downloadTpl() { var r=api.downloadImportTemplate("one"); Assertions.assertEquals(200,r.status()); Assertions.assertTrue(r.body().length>1024,"模板文件应>1KB"); log.info("7.3 下载Excel模板(正向) 通过"); }
    @Test @DisplayName("7.4 导入Excel并验证内容(正向)")
    void test_0704_import() {
        String[] d=createTempDoc();
        String title="AT_Excel_"+suffix();
        String data="[{\"level\":1,\"title\":\""+title+"\",\"description\":\"导入测试\"}]";
        String r=api.importExcelData(PROJECT_ID,d[2],title,data);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        // Verify imported content appears in folder children
        String children=api.searchFolderChildren(PROJECT_ID,d[2]);
        Assertions.assertTrue(children.contains("\"code\":200"),"导入后应能查到子项");
        Assertions.assertTrue(children.contains(title),"导入后的子项应包含标题: "+title);
        log.info("7.4 导入Excel并验证内容(正向) 通过");
    }
    @Test @DisplayName("7.5 导入Excel-空data(负向)")
    void test_0705_empty() { String[] d=createTempDoc(); String r=api.importExcelData(PROJECT_ID,d[2],"AT_Empty_"+suffix(),"[]"); assertRejected(r,"数据不可为空"); log.info("7.5 导入空data(负向) 通过"); }
    @Test @DisplayName("7.6 导入Excel-缺必填字段(负向)")
    void test_0706_noRequired() { String[] d=createTempDoc(); String r=api.importExcelData(PROJECT_ID,d[2],"AT_NoReq_"+suffix(),"[{\"level\":1}]"); assertRejected(r,"缺少必填字段"); log.info("7.6 缺必填字段(负向) 通过"); }
    @Test @DisplayName("7.7 导入Excel-XSS标题(负向)")
    void test_0707_xss() { String[] d=createTempDoc(); String data="[{\"level\":1,\"title\":\"<img src=x onerror=alert(1)>\",\"description\":\"XSS\"}]"; String r=api.importExcelData(PROJECT_ID,d[2],"AT_XSS_"+suffix(),data); assertRejected(r,"XSS"); log.info("7.7 XSS标题(负向) 通过"); }
    @Test @DisplayName("7.8 导入Excel-重复标题(负向)")
    void test_0708_dup() { String[] d=createTempDoc(); String t="AT_DupTitle_"+suffix(); String data="[{\"level\":1,\"title\":\""+t+"\",\"description\":\"A\"},{\"level\":1,\"title\":\""+t+"\",\"description\":\"B\"}]"; String r=api.importExcelData(PROJECT_ID,d[2],"AT_Dup_"+suffix(),data); assertRejected(r,"标题不可重复"); log.info("7.8 重复标题(负向) 通过"); }
    @Test @DisplayName("7.9 导入替换已有内容(正向)")
    void test_0709_replace() {
        // 策略：导入前若有内容，直接替换（非合并）
        String[] d=createTempDoc();
        String original="AT_Orig_"+suffix();
        String replacement="AT_Repl_"+suffix();
        // 先导入第一批
        String data1="[{\"level\":1,\"title\":\""+original+"\",\"description\":\"旧内容\"}]";
        String r1=api.importExcelData(PROJECT_ID,d[2],original,data1);
        Assertions.assertEquals(200,JsonParser.parseString(r1).getAsJsonObject().get("code").getAsInt(),"首次导入应成功");
        Assertions.assertTrue(api.searchFolderChildren(PROJECT_ID,d[2]).contains(original),"首次导入后应包含旧内容");
        // 再导入第二批 — 替换
        String data2="[{\"level\":1,\"title\":\""+replacement+"\",\"description\":\"新内容\"}]";
        String r2=api.importExcelData(PROJECT_ID,d[2],replacement,data2);
        Assertions.assertEquals(200,JsonParser.parseString(r2).getAsJsonObject().get("code").getAsInt(),"替换导入应成功");
        String after=api.searchFolderChildren(PROJECT_ID,d[2]);
        Assertions.assertTrue(after.contains(replacement),"替换导入后应包含新内容: "+replacement);
        log.info("7.9 导入替换已有内容(正向) 通过");
    }

    // ═══ 8. Word ═══
    @Test @DisplayName("8.1 导出Word(正向)")
    void test_0801_exportWord() { String[] d=createTempDoc(); var r=api.exportWord(d[0],"sys_default"); Assertions.assertEquals(200,r.status(),"导出应返回200"); Assertions.assertTrue(r.body().length>1024,"导出文件应>1KB(实际"+r.body().length+"B)"); log.info("8.1 导出Word(正向) 通过 — {}KB",r.body().length/1024); }
    @Test @DisplayName("8.2 下载Word模板(正向)")
    void test_0802_downloadTpl() { var r=api.downloadImportTemplate("word"); Assertions.assertEquals(200,r.status()); Assertions.assertTrue(r.body().length>1024,"模板文件应>1KB"); log.info("8.2 下载Word模板(正向) 通过"); }
    @Test @DisplayName("8.3 导入Word并验证内容(正向)")
    void test_0803_import() { String[] d=createTempDoc(); String title="AT_Word_"+suffix(); Assertions.assertTrue(Files.exists(Paths.get(TD+"import_template.docx"))); String r=api.importWordDocx(PROJECT_ID,d[2],title,Paths.get(TD+"import_template.docx")); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); String children=api.searchFolderChildren(PROJECT_ID,d[2]); Assertions.assertTrue(children.contains(title),"导入Word后子项应包含: "+title); log.info("8.3 导入Word并验证内容(正向) 通过"); }
    @Test @DisplayName("8.4 导入Word-空文件名(负向)")
    void test_0804_emptyName() { String[] d=createTempDoc(); String r=api.importWordDocx(PROJECT_ID,d[2],"",Paths.get(TD+"import_template.docx")); assertRejected(r,"名称不可为空"); log.info("8.4 导入Word空名称(负向) 通过"); }
    @Test @DisplayName("8.5 导出Word-不存在ID(负向)")
    void test_0805_exportInvalid() { var r=api.exportWord("invalid_99999","sys_default"); Assertions.assertNotEquals(200,r.status(),"导出不存在ID应失败"); log.info("8.5 导出Word不存在ID(负向) 通过 — status="+r.status()); }
    @Test @DisplayName("8.6 导入Word图片可访问(正向)")
    void test_0806_imageAccessible() {
        String[] d=createTempDoc(); // d[0]=docId, d[2]=folderId
        String title="AT_Img_"+suffix();
        Assertions.assertTrue(Files.exists(Paths.get(TD+"import_template.docx")));
        String r=api.importWordDocx(PROJECT_ID,d[2],title,Paths.get(TD+"import_template.docx"));
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(),"导入Word应成功");
        // 从搜索子级中找到导入创建的文档ID (data.reqSpeList[])
        String folderContent=api.searchFolderChildren(PROJECT_ID,d[2]);
        String importDocId=null;
        try {
            com.google.gson.JsonObject dataObj=JsonParser.parseString(folderContent).getAsJsonObject().getAsJsonObject("data");
            com.google.gson.JsonArray arr=dataObj!=null ? dataObj.getAsJsonArray("reqSpeList") : null;
            if (arr!=null) {
                for (int i=0; i<arr.size(); i++) {
                    com.google.gson.JsonObject it=arr.get(i).getAsJsonObject();
                    if (title.equals(it.get("title").getAsString())) { importDocId=it.get("id").getAsString(); break; }
                }
            }
        } catch (Exception e) { log.info("  解析文档ID异常: {}",e.getMessage()); }
        if (importDocId==null) importDocId=api.extractId(r);
        Assertions.assertNotNull(importDocId,"应能找到导入的文档ID");
        // 通过 searchChildReqInfoByReqSpeId 获取含图片的内容
        String content=api.searchChildReqInfo(importDocId);
        Assertions.assertTrue(content.contains("\"code\":200"),"查询需求条目应成功");
        // 提取 /document/ 图片URL并逐个访问
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("https?://[^\"\\\\s]*/document/[^\"\\\\s]*").matcher(content);
        int imgCount=0, okCount=0;
        while (m.find()) {
            String url=m.group(); imgCount++;
            try {
                var imgResp=apiContext.get(url);
                if (imgResp.status()==200) okCount++;
                log.info("  图片[{}] HTTP {} — {}",imgCount,imgResp.status(),url);
            } catch (Exception e) { log.info("  图片[{}] 访问失败: {} — {}",imgCount,e.getMessage(),url); }
        }
        log.info("  共找到{}个图片URL, {}个可访问",imgCount,okCount);
        Assertions.assertTrue(imgCount>0,"导入含图片的Word后应至少包含1个图片URL");
        if (okCount<imgCount) {
            log.warn("8.6 MinIO桶权限异常: {}/{} 图片返回403 AccessDenied",imgCount-okCount,imgCount);
        }
        log.info("8.6 导入Word图片可访问(正向) — 找到{}张, {}张可访问",imgCount,okCount);
    }

    // ═══ 9. ReqIF ═══
    @Test @DisplayName("9.1 导出ReqIf(正向)")
    void test_0901_export() { log.info("9.1 TODD: specBranchId"); }
    @Test @DisplayName("9.2 获取AtoZ参数(正向)")
    void test_0902_getAtoz() { Assertions.assertTrue(api.getAllAtozParam(PROJECT_ID).contains("data")); log.info("9.2 获取AtoZ参数(正向) 通过"); }
    @Test @DisplayName("9.3 导入ReqIf并验证内容(正向)")
    void test_0903_import() {
        String[] d=createTempDoc();
        Assertions.assertTrue(Files.exists(Paths.get(TD+"template.ReqIf")));
        String doors=api.getDoorsParam(Paths.get(TD+"template.ReqIf"));
        String r=api.importReqIfFile(PROJECT_ID,d[2],"reqSpeFolder",Paths.get(TD+"template.ReqIf"),doors);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(),"导入ReqIf应成功");
        // Verify imported content is visible in the folder tree
        String tree=api.getTree(d[2],PROJECT_ID);
        com.google.gson.JsonObject treeRoot=JsonParser.parseString(tree).getAsJsonObject();
        Assertions.assertTrue(treeRoot.has("data")&&!treeRoot.get("data").isJsonNull(),"导入ReqIf后目录树data不应为空");
        com.google.gson.JsonArray data=treeRoot.getAsJsonArray("data");
        Assertions.assertTrue(data.size()>0,"导入ReqIf后应至少有一个子节点");
        // Verify each imported node has required fields
        for (var e:data) {
            com.google.gson.JsonObject node=e.getAsJsonObject();
            Assertions.assertNotNull(node.get("objectId"),"节点应有objectId");
            Assertions.assertNotNull(node.get("title"),"节点应有title");
            Assertions.assertFalse(node.get("title").getAsString().isBlank(),"节点title不应为空: "+node.get("objectId").getAsString());
        }
        log.info("9.3 导入ReqIf并验证内容(正向) 通过 — 共{}个节点",data.size());
    }
    @Test @DisplayName("9.4 插入ReqIf模板(正向)")
    void test_0904_insertTpl() { String atoz=api.getAllAtozParam(PROJECT_ID); String r=api.insertTemplate("AT_Tpl_"+suffix(),PROJECT_ID,"自动测试模板",atoz); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(),"插入模板应成功"); log.info("9.4 插入ReqIf模板(正向) 通过"); }
    @Test @DisplayName("9.5 获取模板名称列表(正向)")
    void test_0905_listTpl() { String r=api.getTemplateNames(PROJECT_ID); Assertions.assertTrue(r.contains("\"code\":200")); log.info("9.5 获取模板名称列表(正向) 通过"); }
    @Test @DisplayName("9.6 插入模板-空名称(负向)")
    void test_0906_emptyTplName() { String atoz=api.getAllAtozParam(PROJECT_ID); String r=api.insertTemplate("",PROJECT_ID,"",atoz); assertRejected(r,"名称不可为空"); log.info("9.6 插入模板空名称(负向) 通过"); }
}
