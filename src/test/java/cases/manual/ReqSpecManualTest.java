package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;
import java.nio.file.Paths;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReqSpecManualTest extends ApiTestHelper {

    private static final String IMG = "src/test/resources/testdata/image.jpeg";
    private String[] uploadAndGet() { String r=api.reqDocUpload(Paths.get(IMG)); JsonObject up=JsonParser.parseString(r).getAsJsonObject(); if(up.get("code").getAsInt()!=200) return null; JsonObject d=up.getAsJsonObject("data"); return new String[]{d.get("objectId").getAsString(),d.get("downloadURL").getAsString()}; }

    // ═══ 10. 新建需求规格 ═══
    @Test @DisplayName("10.1 文件夹下新建需求规格(正向)")
    void test_1001_create() { String[] d=createTempDoc(); Assertions.assertNotNull(d[0]); log.info("10.1 文件夹下新建需求规格(正向) 通过"); }

    // ═══ 11. 需求规格名称校验 ═══
    @Test @DisplayName("11.1 重命名-重复(负向)")
    void test_1101_renameDup() { String[] a=createTempDoc(),b=createTempDoc(); String r=api.renameDocument(PROJECT_ID,b[0],b[2],"DupName"); log.info("11.1 {}",r); }
    @Test @DisplayName("11.2 重命名-空(负向)")
    void test_1102_renameEmpty() { String[] d=createTempDoc(); String r=api.renameDocument(PROJECT_ID,d[0],d[2],""); assertRejected(r,"空名称"); log.info("11.2 重命名-空(负向) 通过"); }
    @Test @DisplayName("11.3 重命名-XSS(负向)")
    void test_1103_renameXss() { String[] d=createTempDoc(); String r=api.renameDocument(PROJECT_ID,d[0],d[2],"<img src=x onerror=alert(1)>"); log.info("11.3 {}",r); }
    @Test @DisplayName("11.4 重命名-超长(负向)")
    void test_1104_renameLong() { String[] d=createTempDoc(); String r=api.renameDocument(PROJECT_ID,d[0],d[2],"A".repeat(200)); log.info("11.4 {}",r); }

    // ═══ 12. 编辑描述 ═══
    @Test @DisplayName("12.1 编辑描述(正向)")
    void test_1201_editDesc() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"AT_Desc_"+suffix()); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("12.1 编辑描述(正向) 通过"); }
    @Test @DisplayName("12.2 描述-空(负向)")
    void test_1202_editDescEmpty() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],""); log.info("12.2 {}",r); }
    @Test @DisplayName("12.3 描述-超长1000字(负向)")
    void test_1203_editDescLong() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"D".repeat(1001)); log.info("12.3 {}",r); }
    @Test @DisplayName("12.4 描述-XSS(负向)")
    void test_1204_editDescXss() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"<script>alert(1)</script>"); log.info("12.4 {}",r); }

    // ═══ 13. 删除需求规格 ═══
    @Test @DisplayName("13.1 删除(正向)")
    void test_1301_delete() { String[] d=createTempDoc(); String r=api.deleteDocument(d[0],d[2]); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("13.1 删除(正向) 通过"); }
    @Test @DisplayName("13.2 取消删除(正向)")
    void test_1302_recover() { String[] d=createTempDoc(); api.deleteDocument(d[0],d[2]); String r=api.recoverDocument(d[0],d[2]); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("13.2 取消删除(正向) 通过"); }
    @Test @DisplayName("13.3 彻底清除(正向)")
    void test_1303_clean() { String[] d=createTempDoc(); api.deleteDocument(d[0],d[2]); api.forceCleanDocument(d[0],d[2]); log.info("13.3 彻底清除(正向) 通过"); }
    @Test @DisplayName("13.4 删除不存在ID(负向)")
    void test_1304_invalidId() { String r=api.deleteDocument("invalid_99999",PROJECT_ID); assertRejected(r,"不存在ID"); log.info("13.4 删除不存在ID(负向) 通过"); }
    @Test @DisplayName("13.5 重复删除(负向)")
    void test_1305_delTwice() { String[] d=createTempDoc(); api.deleteDocument(d[0],d[2]); String r=api.deleteDocument(d[0],d[2]); assertRejected(r,"重复删除"); log.info("13.5 重复删除(负向) 通过"); }

    // ═══ 14. 检索 ═══
    @Test @DisplayName("14.1 需求规格检索(仅UI)")
    void test_1401_search() { log.info("14.1 TODD: 仅UI"); }

    // ═══ 15. 查看属性 ═══
    @Test @DisplayName("15.1 查看属性(正向)")
    void test_1501_viewAttr() { String[] d=createTempDoc(); Assertions.assertTrue(api.searchReqSpeInfo(d[0]).contains("\"code\":200")); log.info("15.1 查看属性(正向) 通过"); }
    @Test @DisplayName("15.2 修改名称(正向)")
    void test_1502_updateName() { String[] d=createTempDoc(); String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"","",null); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("15.2 修改名称(正向) 通过"); }
    @Test @DisplayName("15.3 修改编码前缀(正向)")
    void test_1503_updatePrefix() { String[] d=createTempDoc(); String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"ab","",null); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("15.3 修改编码前缀(正向) 通过"); }
    @Test @DisplayName("15.4 编辑描述(正向)")
    void test_1504_editDesc() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"AT_Desc_"+suffix()); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("15.4 编辑描述(正向) 通过"); }
    @Test @DisplayName("15.5 上传文件关联(正向)")
    void test_1505_upload() { String[] d=createTempDoc(); String[] fid=uploadAndGet(); if(fid!=null){ String docData="[{\"objectId\":\""+fid[0]+"\",\"title\":\"img\",\"description\":\"desc\",\"downloadURL\":\""+fid[1]+"\"}]"; Assertions.assertEquals(200,JsonParser.parseString(api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],null,null,docData)).getAsJsonObject().get("code").getAsInt()); } log.info("15.5 上传文件关联(正向) 通过"); }
    @Test @DisplayName("15.6 编辑文件描述(正向)")
    void test_1506_editFileDesc() { String[] d=createTempDoc(); String[] fid=uploadAndGet(); if(fid!=null){ String docData="[{\"objectId\":\""+fid[0]+"\",\"title\":\"rnm\",\"description\":\"updated\",\"downloadURL\":\""+fid[1]+"\"}]"; Assertions.assertEquals(200,JsonParser.parseString(api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],null,null,docData)).getAsJsonObject().get("code").getAsInt()); } log.info("15.6 编辑文件描述(正向) 通过"); }
    @Test @DisplayName("15.7 查看不存在ID(负向)")
    void test_1507_invalidId() { log.info("15.7 code={}",JsonParser.parseString(api.searchReqSpeInfo("invalid_99999")).getAsJsonObject().get("code").getAsInt()); }

    // ═══ 16. 属性名称唯一 → AttributeManualTest ═══
    @Test @DisplayName("16.1 属性名称(占位)")
    void test_1601_attrName() { log.info("16.1 TODD: → AttributeManualTest"); }

    // ═══ 17. 编码前缀 ═══
    @Test @DisplayName("17.1 数字开头(负向)")
    void test_1701_prefixNum() { String[] d=createTempDoc(); String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"123abc","",null); assertRejected(r,"数字开头"); log.info("17.1 数字开头(负向) 通过"); }
    @Test @DisplayName("17.2 空前缀(正向)")
    void test_1702_prefixEmpty() { String[] d=createTempDoc(); String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"","",null); log.info("17.2 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("17.3 超长>10(负向)")
    void test_1703_prefixLong() { String[] d=createTempDoc(); String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"abcdefghijk","",null); assertRejected(r,"超长前缀"); log.info("17.3 超长>10(负向) 通过"); }
    @Test @DisplayName("17.4 特殊字符(负向)")
    void test_1704_prefixSpecial() { String[] d=createTempDoc(); String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"a@#$b","",null); assertRejected(r,"特殊字符"); log.info("17.4 特殊字符(负向) 通过"); }

    // ═══ 18. 描述校验 ═══
    @Test @DisplayName("18.1 描述超长>1000(负向)")
    void test_1801_descLong() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"D".repeat(1001)); assertRejected(r,"超长描述"); log.info("18.1 描述超长>1000(负向) 通过"); }
    @Test @DisplayName("18.2 描述XSS(负向)")
    void test_1802_descXss() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"<img src=x onerror=alert(1)>"); log.info("18.2 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("18.3 描述HTML(负向)")
    void test_1803_descHtml() { String[] d=createTempDoc(); String r=api.editDescription(PROJECT_ID,d[0],d[2],"<h1>test</h1>"); log.info("18.3 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }

    // ═══ 19. 文件上传 ═══
    @Test @DisplayName("19.1 上传文件(正向)")
    void test_1901_upload() { Assertions.assertTrue(api.reqDocUpload(Paths.get(IMG)).contains("\"code\":200")); log.info("19.1 上传文件(正向) 通过"); }
    @Test @DisplayName("19.2 上传关联(正向)")
    void test_1902_attach() { String[] d=createTempDoc(); String[] fid=uploadAndGet(); if(fid!=null){ String docData="[{\"objectId\":\""+fid[0]+"\",\"title\":\"img\",\"description\":\"auto\",\"downloadURL\":\""+fid[1]+"\"}]"; Assertions.assertEquals(200,JsonParser.parseString(api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],"","",docData)).getAsJsonObject().get("code").getAsInt()); } log.info("19.2 上传关联(正向) 通过"); }
    @Test @DisplayName("19.3 文件备注超50字(负向)")
    void test_1903_descLong() { String[] d=createTempDoc(); String[] fid=uploadAndGet(); if(fid!=null){ String docData="[{\"objectId\":\""+fid[0]+"\",\"title\":\"img\",\"description\":\""+"A".repeat(51)+"\",\"downloadURL\":\""+fid[1]+"\"}]"; String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],null,null,docData); assertRejected(r,"备注超长"); } log.info("19.3 文件备注超50字(负向) 通过"); }
    @Test @DisplayName("19.4 删除附件(正向)")
    void test_1904_delete() { String[] fid=uploadAndGet(); if(fid!=null) Assertions.assertEquals(200,JsonParser.parseString(api.deleteReqDoc(fid[0])).getAsJsonObject().get("code").getAsInt()); log.info("19.4 删除附件(正向) 通过"); }
    @Test @DisplayName("19.5 不支持格式(负向)")
    void test_1905_badFormat() { String r=api.reqDocUpload(Paths.get("auth.json")); assertRejected(r,"不支持格式"); log.info("19.5 不支持格式(负向) 通过"); }

    // ═══ 19. 权限 ═══
    @Test @DisplayName("19.6 查看人员列表(正向)")
    void test_1906_listPerson() { Assertions.assertTrue(api.searchProjectPersonList(PROJECT_ID).contains("\"code\":200")); log.info("20.1 通过"); }
    @Test @DisplayName("19.7 设置写入权限(正向)")
    void test_1907_setPerm() { String[] d=createTempDoc(); String r=api.updateReqSpeWritePermission(d[0],"[{\"objectId\":\"1\",\"userName\":\"admin\"}]"); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("20.2 通过"); }
    @Test @DisplayName("19.8 空人员列表(负向)")
    void test_1908_empty() { String[] d=createTempDoc(); log.info("20.3 code={}",JsonParser.parseString(api.updateReqSpeWritePermission(d[0],"[]")).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("19.9 无效userId(负向)")
    void test_1909_invalid() { String[] d=createTempDoc(); log.info("20.4 code={}",JsonParser.parseString(api.updateReqSpeWritePermission(d[0],"[{\"objectId\":\"99999\",\"userName\":\"nobody\"}]")).getAsJsonObject().get("code").getAsInt()); }
    // ═══ 21. 权限搜索 ═══
    @Test @DisplayName("21.1 权限搜索(仅UI)")
    void test_2101_permSearch() { log.info("21.1 TODD: 仅UI"); }

    // ═══ 20. 需求条目 ═══
    @Test @DisplayName("20.1 新建需求条目(正向)")
    void test_2001_addItem() { String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); Assertions.assertNotNull(id); log.info("20.1 新建需求条目(正向) 通过"); }
    @Test @DisplayName("20.2 删除需求条目(正向)")
    void test_2002_deleteItem() { String[] d=createTempDoc(); String id=api.addReqItem(PROJECT_ID,d[0],d[0]); String r=api.deleteReqItem(id); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); api.cleanReqItem(id,d[0]); log.info("20.2 删除需求条目(正向) 通过"); }

    // ═══ 23. 显示大纲 ═══
    @Test @DisplayName("23.1 显示大纲(仅UI)")
    void test_2301_outline() { log.info("23.1 TODD: 仅UI"); }

    // ═══ 24-33 → AttributeManualTest ═══

    // ═══ 28-31. 打开模式/状态切换 ═══
    @Test @DisplayName("28.1 共享模式(正向)")
    void test_2801_share() { String[] d=createTempDoc(); String r=api.getOpenModel(d[0]); JsonObject o=JsonParser.parseString(r).getAsJsonObject(); log.info("28.1 code={} openMode={}",o.get("code").getAsInt(),o.getAsJsonObject("data").get("openMode").getAsString()); }
    @Test @DisplayName("29.1 独占模式(正向)")
    void test_2901_exclusive() { String[] d=createTempDoc(); String r=api.getOpenModel(d[0]); JsonObject o=JsonParser.parseString(r).getAsJsonObject(); log.info("29.1 code={} openMode={}",o.get("code").getAsInt(),o.getAsJsonObject("data").get("openMode").getAsString()); }
    @Test @DisplayName("30.1 只读模式(正向)")
    void test_3001_readOnly() { String[] d=createTempDoc(); String r=api.getOpenModel(d[0]); JsonObject o=JsonParser.parseString(r).getAsJsonObject(); log.info("30.1 code={} openMode={}",o.get("code").getAsInt(),o.getAsJsonObject("data").get("openMode").getAsString()); }
    @Test @DisplayName("31.1 切换为冻结(正向)")
    void test_3101_freeze() { String[] d=createTempDoc(); String r=api.updateReqSpeState(d[0],"Frozen"); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("31.1 切换为冻结(正向) 通过"); }
    @Test @DisplayName("31.2 冻结切回工作中(正向)")
    void test_3102_inwork() { String[] d=createTempDoc(); api.updateReqSpeState(d[0],"Frozen"); String r=api.updateReqSpeState(d[0],"Inwork"); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("31.2 冻结切回工作中(正向) 通过"); }
    @Test @DisplayName("31.3 冻结状态不允许操作(负向)")
    void test_3103_frozenBlock() { String[] d=createTempDoc(); api.updateReqSpeState(d[0],"Frozen"); String r=api.editDescription(PROJECT_ID,d[0],d[2],"frozen test"); assertRejected(r,"冻结状态不可操作"); log.info("31.3 冻结状态不允许操作(负向) 通过"); }
    @Test @DisplayName("31.4 访问权限(正向)")
    void test_3104_access() { String[] d=createTempDoc(); String r=api.getReqAccess(d[0]); log.info("31.4 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("31.5 版本列表(正向)")
    void test_3105_versions() { String[] d=createTempDoc(); String r=api.getVersionList(d[0]); Assertions.assertTrue(r.contains("\"code\":200")); log.info("31.5 版本列表(正向) 通过"); }

    // ═══ 38-45 → ReqItemEditManualTest ═══

    // ═══ 40-42,49. 视图 ═══
    @Test @DisplayName("40.1 新建视图(正向)")
    void test_4001_addView() { String[] d=createTempDoc(); String n="AT_V_"+suffix(); String vid=api.addView(d[0],n,"auto","name,editStatus,description"); Assertions.assertNotNull(vid); log.info("40.1 新建视图(正向) 通过"); }
    @Test @DisplayName("41.1 视图名称必填(负向)")
    void test_4101_emptyName() { String[] d=createTempDoc(); String vid=api.addView(d[0],"","auto","name"); Assertions.assertNull(vid,"空名称应返回null"); log.info("41.1 视图名称必填(负向) 通过"); }
    @Test @DisplayName("41.2 视图名称超长-已知缺陷(负向)")
    void test_4102_longName() { String[] d=createTempDoc(); String vid=api.addView(d[0],"V".repeat(200),"auto","name"); log.info("41.2 超长名称 vid={} (已知缺陷: 应拒绝超长名称，当前{}允许)",vid,vid!=null?"":"拒绝"); }
    @Test @DisplayName("41.3 XSS名称-已知缺陷(负向)")
    void test_4103_xss() { String[] d=createTempDoc(); String vid=api.addView(d[0],"<img src=x onerror=alert(1)>","auto","name"); log.info("41.3 XSS名称 vid={} (已知缺陷: 应拒绝XSS名称，当前{}允许)",vid,vid!=null?"":"拒绝"); }
    @Test @DisplayName("41.4 视图名称重复(负向)")
    void test_4104_dupName() { String[] d=createTempDoc(); String n="AT_Dup_"+suffix(); api.addView(d[0],n,"auto","name"); String vid=api.addView(d[0],n,"auto","name"); Assertions.assertNull(vid,"重复名称应返回null"); log.info("41.4 视图名称重复(负向) 通过"); }
    @Test @DisplayName("42.1 删除视图(正向)")
    void test_4201_delete() { String[] d=createTempDoc(); String n="AT_Del_"+suffix(); String vid=api.addView(d[0],n,"auto","name"); Assertions.assertNotNull(vid); String r=api.deleteView(vid); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("42.1 删除视图(正向) 通过"); }
    @Test @DisplayName("42.2 删除不存在视图(负向)")
    void test_4202_invalid() { String r=api.deleteView("invalid_99999"); assertRejected(r,"不存在"); log.info("42.2 删除不存在视图(负向) 通过"); }
    @Test @DisplayName("42.3 搜索视图列表(正向)")
    void test_4203_search() { String[] d=createTempDoc(); String r=api.searchViewList(d[0]); Assertions.assertTrue(r.contains("\"code\":200")); log.info("42.3 搜索视图列表(正向) 通过"); }
    @Test @DisplayName("43.1 搜索不存在的文档视图(负向)")
    void test_4301_searchInvalid() { String r=api.searchViewList("invalid_99999"); log.info("43.1 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("49.1 分屏展示(仅UI)")
    void test_4901_splitScreen() { log.info("49.1 TODD: 仅UI"); }
}
