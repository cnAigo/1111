package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqItemEditManualTest extends ApiTestHelper {

    // ── 取children数组 ──
    private JsonArray children(String docId) {
        String cr=api.searchChildReqInfo(docId);
        return JsonParser.parseString(cr).getAsJsonObject().getAsJsonObject("data").getAsJsonArray("children");
    }
    // ── 取第一个条目 ──
    private JsonObject firstItem(String docId) {
        JsonArray ch=children(docId);
        Assertions.assertTrue(ch.size()>0,"应有至少一个条目");
        return ch.get(0).getAsJsonObject();
    }

    // ═══ 32. 编辑标题/正文 ═══
    @Test @DisplayName("32.1 编辑标题(正向)")
    void test_3201_title() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        String newTitle="AT_Upd_"+suffix();
        JsonObject it=firstItem(d[0]); it.addProperty("title",newTitle);
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertEquals(newTitle,firstItem(d[0]).get("title").getAsString(),"标题应已更新");
        log.info("32.1 编辑标题(正向) 通过");
    }
    @Test @DisplayName("32.2 超长>500字(负向)")
    void test_3202_long() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("title","T".repeat(501));
        String r=api.updateReqList(d[0],"["+it+"]");
        assertRejected(r,"超长");
        log.info("32.2 超长>500字(负向) 通过");
    }
    @Test @DisplayName("32.3 空标题(负向)")
    void test_3203_emptyTitle() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("title","");
        String r=api.updateReqList(d[0],"["+it+"]");
        assertRejected(r,"空");
        log.info("32.3 空标题(负向) 通过");
    }

    // ═══ 33. 加锁/解锁 ═══
    @Test @DisplayName("33.1 加锁(正向)")
    void test_3301_lock() { String[] d=createTempDoc(); String r=api.unlockMode(d[0],"lock","admin"); log.info("33.1 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("33.2 重复加锁(负向)")
    void test_3302_lockTw() { String[] d=createTempDoc(); api.unlockMode(d[0],"lock","admin"); String r=api.unlockMode(d[0],"lock","admin"); assertRejected(r,"重复"); log.info("33.2 重复加锁(负向) 通过"); }
    @Test @DisplayName("33.3 解锁(正向)")
    void test_3303_unlock() { String[] d=createTempDoc(); api.unlockMode(d[0],"lock","admin"); String r=api.unlockMode(d[0],"unlock","admin"); log.info("33.3 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("33.4 重复解锁(负向)")
    void test_3304_unlockTw() { String[] d=createTempDoc(); String r=api.unlockMode(d[0],"unlock","admin"); assertRejected(r,"重复"); log.info("33.4 重复解锁(负向) 通过"); }

    // ═══ 34. 富文本编辑 — 取完整条目→改description→发回→读回验证 ═══
    @Test @DisplayName("34.1 字号(正向)")
    void test_3401_fontSize() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<p><span style=\"font-size: 12px;\">666</span></p>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("font-size"),"应含字号样式");
        log.info("34.1 字号(正向) 通过");
    }
    @Test @DisplayName("34.2 h1标题(正向)")
    void test_3402_heading() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<h1><span style=\"font-size: 12px;\">666</span></h1>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("<h1>"),"应含h1标签");
        log.info("34.2 h1标题(正向) 通过");
    }
    @Test @DisplayName("34.3 粗体(正向)")
    void test_3403_bold() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<h4><strong>666</strong></h4>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("<strong>"),"应含粗体标签");
        log.info("34.3 粗体(正向) 通过");
    }
    @Test @DisplayName("34.4 表格(正向)")
    void test_3404_table() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<table style=\"border-collapse: collapse; width: 100%;\"><tbody><tr><td style=\"border: 1px solid #ccc; padding: 4px;\">A</td><td style=\"border: 1px solid #ccc; padding: 4px;\">B</td></tr></tbody></table>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("<table"),"应含表格标签");
        log.info("34.4 表格(正向) 通过");
    }
    @Test @DisplayName("34.5 文字颜色(正向)")
    void test_3405_fontColor() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<p><span style=\"color: rgb(255, 0, 0);\">红色文字</span></p>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("color"),"应含颜色样式");
        log.info("34.5 文字颜色(正向) 通过");
    }
    @Test @DisplayName("34.6 背景颜色(正向)")
    void test_3406_bgColor() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<p><span style=\"background-color: rgb(255, 255, 0);\">黄色背景</span></p>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("background-color"),"应含背景色样式");
        log.info("34.6 背景颜色(正向) 通过");
    }
    @Test @DisplayName("34.7 上传图片文件(正向)")
    void test_3407_image() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        // 上传真实图片到需求规格
        java.nio.file.Path img = java.nio.file.Paths.get("src/test/resources/testdata/image.jpeg");
        if(java.nio.file.Files.exists(img)){
            String upload=api.reqDocUpload(img);
            JsonObject up=com.google.gson.JsonParser.parseString(upload).getAsJsonObject();
            if(up.get("code").getAsInt()==200){
                JsonObject data=up.getAsJsonObject("data");
                String dl=data.get("downloadURL").getAsString();
                String fid=data.get("objectId").getAsString();
                String docData="[{\"objectId\":\""+fid+"\",\"title\":\"image.jpeg\",\"description\":\"图片\",\"downloadURL\":\""+dl+"\"}]";
                String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],null,null,docData);
                Assertions.assertEquals(200,com.google.gson.JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
                log.info("34.7 图片上传关联成功");
            }
        }else{ log.info("34.7 图片文件不存在跳过"); }
    }
    @Test @DisplayName("34.8 XSS注入(负向)")
    void test_3408_xss() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<img src=x onerror=alert(1)>");
        String r=api.updateReqList(d[0],"["+it+"]");
        assertRejected(r,"XSS");
        log.info("34.8 XSS注入(负向) 通过");
    }
    @Test @DisplayName("34.9 标题XSS(负向)")
    void test_3409_titleXss() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]);
        JsonObject it=firstItem(d[0]); it.addProperty("title","<script>alert(1)</script>");
        String r=api.updateReqList(d[0],"["+it+"]");
        assertRejected(r,"XSS");
        log.info("34.9 标题XSS(负向) 通过");
    }

    // ═══ 41. 复制→粘贴 ═══
    // 仅UI

    // ═══ 36. 复制需求条目 ═══
    @Test @DisplayName("36.1 复制条目到同文档(正向)")
    void test_3601_copySameDoc() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        int before=children(d[0]).size();
        String r=api.copyReq(d[0],d[0],id,"");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        int after=children(d[0]).size();
        Assertions.assertEquals(before+1,after,"复制后应多一个条目");
        log.info("36.1 复制条目到同文档(正向) 通过");
    }
    @Test @DisplayName("36.2 复制条目到指定位置(正向)")
    void test_3602_copyAtPos() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        String r=api.copyReq(d[0],d[0],id,"1");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertNotNull(firstItem(d[0]).get("objectId"),"复制出的条目应有ID");
        log.info("36.2 复制条目到指定位置(正向) 通过");
    }
    @Test @DisplayName("36.3 复制到其他文档(正向)")
    void test_3603_copyOtherDoc() {
        String[] d1=createTempDoc(); String[] d2=createTempDoc();
        String id=api.addReqItem(PROJECT_ID,d1[0],d1[0]); Assertions.assertNotNull(id);
        int before=children(d2[0]).size();
        String r=api.copyReq(d2[0],d2[0],id,"");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        int after=children(d2[0]).size();
        Assertions.assertTrue(after>before,"目标文档应多一个条目");
        log.info("36.3 复制到其他文档(正向) 通过");
    }
    @Test @DisplayName("36.4 复制不存在条目(负向)")
    void test_3604_copyInvalid() { String[] d=createTempDoc(); String r=api.copyReq(d[0],d[0],"invalid_99999",""); assertRejected(r,"不存在条目"); log.info("36.4 复制不存在条目(负向) 通过"); }

    // ═══ 37. 剪切/移动 ═══
    @Test @DisplayName("37.1 移动条目位置(正向)")
    void test_3701_move() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        String r=api.changeReqPosition(d[0],id,"1");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertNotNull(firstItem(d[0]).get("objectId"),"条目应仍存在");
        log.info("37.1 移动条目位置(正向) 通过");
    }
    @Test @DisplayName("37.2 移动不存在条目(负向)")
    void test_3702_moveInvalid() { String[] d=createTempDoc(); String r=api.changeReqPosition(d[0],"invalid_99999",""); assertRejected(r,"不存在条目"); log.info("37.2 移动不存在条目(负向) 通过"); }

    // ═══ 44. (占位) ═══

    // ═══ 39. 切换标题/内容 — 取完整条目→只改title/只改description/都改→发回→读回验证 ═══
    @Test @DisplayName("39.1 标题模式-只改title(正向)")
    void test_3901_titleOnly() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        String t="AT_Title_"+suffix();
        JsonObject it=firstItem(d[0]); it.addProperty("title",t);
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertEquals(t,firstItem(d[0]).get("title").getAsString(),"标题应已更新");
        log.info("39.1 标题模式-只改title(正向) 通过");
    }
    @Test @DisplayName("39.2 内容模式-只改description(正向)")
    void test_3902_descOnly() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        JsonObject it=firstItem(d[0]); it.addProperty("description","<p>纯内容模式</p>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        Assertions.assertTrue(firstItem(d[0]).get("description").getAsString().contains("纯内容模式"),"描述应已更新");
        log.info("39.2 内容模式-只改description(正向) 通过");
    }
    @Test @DisplayName("39.3 完整模式-title+description都改(正向)")
    void test_3903_both() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        String t="AT_Both_"+suffix();
        JsonObject it=firstItem(d[0]); it.addProperty("title",t); it.addProperty("description","<p>标题和内容一起</p>");
        String r=api.updateReqList(d[0],"["+it+"]");
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        JsonObject v=firstItem(d[0]);
        Assertions.assertEquals(t,v.get("title").getAsString(),"标题应已更新");
        Assertions.assertTrue(v.get("description").getAsString().contains("标题和内容一起"),"描述应已更新");
        log.info("39.3 完整模式-title+description都改(正向) 通过");
    }
    @Test @DisplayName("39.4 标题XSS(负向)")
    void test_3904_xss() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        JsonObject it=firstItem(d[0]); it.addProperty("title","<script>alert(1)</script>");
        String r=api.updateReqList(d[0],"["+it+"]");
        assertRejected(r,"XSS");
        log.info("39.4 标题XSS(负向) 通过");
    }
    @Test @DisplayName("39.5 空description(负向)")
    void test_3905_emptyDesc() {
        String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id);
        JsonObject it=firstItem(d[0]); it.addProperty("description","");
        String r=api.updateReqList(d[0],"["+it+"]");
        log.info("39.5 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
    }
}
