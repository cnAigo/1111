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
    void test_0701_exportExcel() { String[] d=createTempDoc(); var r=api.exportExcel(d[0],"sys_default"); Assertions.assertTrue(r.ok()&&r.body().length>0); log.info("7.1 导出Excel(正向) 通过"); }
    @Test @DisplayName("7.2 导出Excel-不存在ID(负向)")
    void test_0702_invalid() { var r=api.exportExcel("invalid_99999","sys_default"); log.info("7.2 status={}",r.status()); }
    @Test @DisplayName("7.3 下载Excel模板(正向)")
    void test_0703_downloadTpl() { Assertions.assertTrue(api.downloadImportTemplate("one").ok()); log.info("7.3 下载Excel模板(正向) 通过"); }
    @Test @DisplayName("7.4 导入Excel并验证(正向)")
    void test_0704_import() {
        String[] d=createTempDoc();
        String data="[{\"level\":1,\"title\":\"AT_Excel_"+suffix()+"\",\"description\":\"导入测试\"}]";
        String r=api.importExcelData(PROJECT_ID,d[2],"AT_Excel_"+suffix(),data);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        // 验证导入内容
        String children=api.searchFolderChildren(PROJECT_ID,d[2]);
        Assertions.assertTrue(children.contains("\"code\":200"));
        log.info("7.4 导入Excel并验证(正向) 通过");
    }
    @Test @DisplayName("7.5 导入Excel-空data(负向)")
    void test_0705_empty() { String[] d=createTempDoc(); String r=api.importExcelData(PROJECT_ID,d[2],"AT_Empty_"+suffix(),"[]"); log.info("7.5 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("7.6 导入Excel-缺必填字段(负向)")
    void test_0706_noRequired() { String[] d=createTempDoc(); String r=api.importExcelData(PROJECT_ID,d[2],"AT_NoReq_"+suffix(),"[{\"level\":1}]"); log.info("7.6 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }

    // ═══ 8. Word ═══
    @Test @DisplayName("8.1 导出Word(正向)")
    void test_0801_exportWord() { String[] d=createTempDoc(); Assertions.assertTrue(api.exportWord(d[0],"sys_default").ok()); log.info("8.1 导出Word(正向) 通过"); }
    @Test @DisplayName("8.2 下载Word模板(正向)")
    void test_0802_downloadTpl() { Assertions.assertTrue(api.downloadImportTemplate("word").ok()); log.info("8.2 下载Word模板(正向) 通过"); }
    @Test @DisplayName("8.3 导入Word并验证(正向)")
    void test_0803_import() { String[] d=createTempDoc(); Assertions.assertTrue(Files.exists(Paths.get(TD+"import_template.docx"))); String r=api.importWordDocx(PROJECT_ID,d[2],"AT_Word_"+suffix(),Paths.get(TD+"import_template.docx")); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("8.3 导入Word并验证(正向) 通过"); }

    // ═══ 9. ReqIF ═══
    @Test @DisplayName("9.1 导出ReqIf(正向)")
    void test_0901_export() { log.info("9.1 TODD: specBranchId"); }
    @Test @DisplayName("9.2 获取AtoZ参数(正向)")
    void test_0902_getAtoz() { Assertions.assertTrue(api.getAllAtozParam(PROJECT_ID).contains("data")); log.info("9.2 获取AtoZ参数(正向) 通过"); }
    @Test @DisplayName("9.3 导入ReqIf(正向)")
    void test_0903_import() { String[] d=createTempDoc(); Assertions.assertTrue(Files.exists(Paths.get(TD+"template.ReqIf"))); String doors=api.getDoorsParam(Paths.get(TD+"template.ReqIf")); String r=api.importReqIfFile(PROJECT_ID,d[2],"reqSpeFolder",Paths.get(TD+"template.ReqIf"),doors); log.info("9.3 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
}
