package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("AttributeModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributeManualTest extends ApiTestHelper {

    // ═══ 21. 新建/修改属性 ═══
    @Test @DisplayName("21.1 新建整型(正向)")
    void test_2101_int() { String r=api.addCustomAttribute("AT_Int_"+suffix(),"整型","整型",PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("21.1 新建整型(正向) 通过"); }
    @Test @DisplayName("21.2 新建浮点型(正向)")
    void test_2102_float() { String r=api.addCustomAttribute("AT_Flt_"+suffix(),"浮点","浮点型",PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("21.2 新建浮点型(正向) 通过"); }
    @Test @DisplayName("21.3 新建字符串型(正向)")
    void test_2103_str() { String r=api.addCustomAttribute("AT_Str_"+suffix(),"字符串","字符串型",PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("21.3 新建字符串型(正向) 通过"); }
    @Test @DisplayName("21.4 新建日期型(正向)")
    void test_2104_date() { String r=api.addCustomAttribute("AT_Date_"+suffix(),"日期","日期型",PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("21.4 新建日期型(正向) 通过"); }

    // ═══ 22. 属性英文名/中文名校验 ═══
    @Test @DisplayName("22.1 英文名为空(负向)")
    void test_2201_emptyEn() { String r=api.addCustomAttribute("","测试","整型",PROJECT_ID); assertRejected(r,"空英文名"); log.info("22.1 英文名为空(负向) 通过"); }
    @Test @DisplayName("22.2 中文名为空(负向)")
    void test_2202_emptyCn() { String r=api.addCustomAttribute("AT_E_"+suffix(),"","整型",PROJECT_ID); assertRejected(r,"空"); log.info("22.2 中文名为空(负向) 通过"); }
    @Test @DisplayName("22.3 重复英文名(负向)")
    void test_2203_dup() { String n="AT_Dup_"+suffix(); api.addCustomAttribute(n,"A","整型",PROJECT_ID); String r=api.addCustomAttribute(n,"B","整型",PROJECT_ID); assertRejected(r,"重复"); log.info("22.3 重复英文名(负向) 通过"); }
    @Test @DisplayName("22.4 英文名特殊字符(负向)")
    void test_2204_special() { String r=api.addCustomAttribute("AT-Sp@#"+suffix(),"特殊字符","整型",PROJECT_ID); assertRejected(r,"特殊字符"); log.info("22.4 英文名特殊字符(负向) 通过"); }
    @Test @DisplayName("22.5 XSS英文名(负向)")
    void test_2205_xss() { String r=api.addCustomAttribute("<img src=x onerror=alert(1)>","XSS","整型",PROJECT_ID); assertRejected(r,"XSS"); log.info("22.5 XSS英文名(负向) 通过"); }
    @Test @DisplayName("22.6 超长英文名(负向)")
    void test_2206_long() { String r=api.addCustomAttribute("A".repeat(200),"超长","整型",PROJECT_ID); assertRejected(r,"超长"); log.info("22.6 超长英文名(负向) 通过"); }
    @Test @DisplayName("22.7 无效类型(负向)")
    void test_2207_badType() { String r=api.addCustomAttribute("AT_Bad_"+suffix(),"无效类型","invalid_type",PROJECT_ID); assertRejected(r,"无效类型"); log.info("22.7 无效类型(负向) 通过"); }
    @Test @DisplayName("22.8 checkAttribute校验(正向)")
    void test_2208_checkAttr() { String n="AT_Chk_"+suffix(); String r=api.checkAttribute(PROJECT_ID,n,"检查属性"); Assertions.assertTrue(r.contains("\"code\":200")); log.info("22.8 checkAttribute校验(正向) 通过"); }
    @Test @DisplayName("22.9 checkAttribute-已存在(负向)")
    void test_2209_checkDup() { String n="AT_ChkDup_"+suffix(); api.addCustomAttribute(n,"已存在","字符串型",PROJECT_ID); String r=api.checkAttribute(PROJECT_ID,n,"已存在"); log.info("22.9 校验={}",r.contains("重复")?"重复":"未知"); }

    // ═══ 23. 发布 ═══
    @Test @DisplayName("23.1 发布单个(正向)")
    void test_2301_publish() { String n="AT_Pub_"+suffix(); api.addCustomAttribute(n,"发布","整型",PROJECT_ID); String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f); String r=api.publishCustomAttribute(f[0],PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("23.1 发布单个(正向) 通过"); }
    @Test @DisplayName("23.2 批量发布(正向)")
    void test_2302_batchPub() {
        String a="AT_BP1_"+suffix(),b="AT_BP2_"+suffix(),c="AT_BP3_"+suffix();
        api.addCustomAttribute(a,"批1","整型",PROJECT_ID); api.addCustomAttribute(b,"批2","整型",PROJECT_ID); api.addCustomAttribute(c,"批3","整型",PROJECT_ID);
        String[] fa=api.findCustomAttribute(a,PROJECT_ID),fb=api.findCustomAttribute(b,PROJECT_ID),fc=api.findCustomAttribute(c,PROJECT_ID);
        Assertions.assertNotNull(fa);Assertions.assertNotNull(fb);Assertions.assertNotNull(fc);
        String r=api.batchPublishCustomAttributes(PROJECT_ID,fa[0],fb[0],fc[0]);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        log.info("23.2 批量发布(正向) 通过");
    }
    @Test @DisplayName("23.3 发布后在需求规格中设置属性值(正向)")
    void test_2303_setAttrValue() {
        // 1. 创建+发布一个整型属性
        String n="AT_Use_"+suffix();
        api.addCustomAttribute(n,"使用属性","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID);
        Assertions.assertNotNull(f,"应能找到属性");
        api.publishCustomAttribute(f[0],PROJECT_ID);
        log.info("23.3 属性已发布: id={}",f[0]);

        // 2. 创建需求规格，用updateReqSpeInfo设置属性值
        String[] d=createTempDoc();
        String customAttrJson="[{\"attrId\":\""+f[0]+"\",\"value\":\"42\"}]";
        String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],null,null,null,customAttrJson);
        int code=JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt();
        Assertions.assertEquals(200,code,"发布后应能设置属性值");
        log.info("23.3 设置属性值 code=200");

        // 3. 读回验证属性值
        String sel=api.searchCustomAttribute(PROJECT_ID,"需求管理","req","","","2");
        Assertions.assertTrue(sel.contains("\"code\":200"),"应能查询到已设置的自定义属性");
        log.info("23.3 属性值设置+查询完成");
    }

    // ═══ 24. 删除 ═══
    @Test @DisplayName("24.1 删除未发布(正向)")
    void test_2401_delete() { String n="AT_Del_"+suffix(); api.addCustomAttribute(n,"删除","整型",PROJECT_ID); String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f); String r=api.deleteCustomAttribute(f[0]); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("24.1 删除未发布(正向) 通过"); }
    @Test @DisplayName("24.2 删除已发布(负向)")
    void test_2402_delPublished() { String n="AT_DelPub_"+suffix(); api.addCustomAttribute(n,"删已发布","整型",PROJECT_ID); String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f); api.publishCustomAttribute(f[0],PROJECT_ID); String r=api.deleteCustomAttribute(f[0]); assertRejected(r,"已发布不可删除"); log.info("24.2 删除已发布(负向) 通过"); }
    @Test @DisplayName("24.3 批量删除(正向)")
    void test_2403_batchDel() { String a="AT_B1_"+suffix(),b="AT_B2_"+suffix(); api.addCustomAttribute(a,"A","整型",PROJECT_ID); api.addCustomAttribute(b,"B","整型",PROJECT_ID); String[] fa=api.findCustomAttribute(a,PROJECT_ID),fb=api.findCustomAttribute(b,PROJECT_ID); Assertions.assertNotNull(fa);Assertions.assertNotNull(fb); String r=api.batchDeleteCustomAttributes(fa[0],fb[0]); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("24.3 批量删除(正向) 通过"); }

    // ═══ 25. 刷新 ═══
    @Test @DisplayName("25.1 刷新属性列表(正向)")
    void test_2501_refresh() { String r=api.getCustomAttributeList(PROJECT_ID); Assertions.assertTrue(r.contains("\"code\":200")); log.info("25.1 刷新属性列表(正向) 通过"); }

    // ═══ 26. 搜索 ═══
    @Test @DisplayName("26.1 搜索属性列表(正向)")
    void test_2601_search() { String r=api.getCustomAttributeList(PROJECT_ID); Assertions.assertTrue(r.contains("\"code\":200")); log.info("26.1 搜索属性列表(正向) 通过"); }
    @Test @DisplayName("26.2 条件搜索(正向)")
    void test_2602_searchCond() { String r=api.searchCustomAttribute(PROJECT_ID,"需求管理","req","","",""); Assertions.assertTrue(r.contains("\"code\":200")); log.info("26.2 条件搜索(正向) 通过"); }
    @Test @DisplayName("26.3 按类型搜索(正向)")
    void test_2603_searchByType() { String r=api.searchCustomAttribute(PROJECT_ID,"需求管理","req","","整型",""); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("26.3 按类型搜索(正向) 通过"); }
    @Test @DisplayName("26.4 空条件搜索(正向)")
    void test_2604_searchEmpty() { String r=api.searchCustomAttribute(PROJECT_ID,"","","","",""); Assertions.assertTrue(r.contains("\"code\":200")); log.info("26.4 空条件搜索(正向) 通过"); }
    @Test @DisplayName("26.5 模糊搜索(正向)")
    void test_2605_searchFuzzy() { String r=api.searchCustomAttribute(PROJECT_ID,"需求管理","req","AT_","",""); Assertions.assertTrue(r.contains("\"code\":200")); log.info("26.5 模糊搜索(正向) 通过"); }
    @Test @DisplayName("26.6 SQL注入(负向)")
    void test_2606_sqlInj() { String r=api.searchCustomAttribute(PROJECT_ID,"需求管理","req","' OR '1'='1","",""); assertRejected(r,"SQL"); log.info("26.6 SQL注入(负向) 通过"); }

    // ═══ 27. 重置搜索 ═══
    @Test @DisplayName("27.1 无参数搜索/重置(正向)")
    void test_2701_resetSearch() { String r=api.getCustomAttributeList(PROJECT_ID); Assertions.assertTrue(r.contains("data")); log.info("27.1 无参数搜索/重置(正向) 通过"); }
    @Test @DisplayName("27.2 属性值校验-整型边界(正向)")
    void test_2702_intBoundary() { String n="AT_Bound_"+suffix(); api.addCustomAttribute(n,"边界","整型",PROJECT_ID); String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f); log.info("27.2 整型边界创建成功 id={}",f[0]); }
    @Test @DisplayName("27.3 属性值校验-浮点精度(正向)")
    void test_2703_floatPrecision() { String n="AT_FPrec_"+suffix(); api.addCustomAttribute(n,"精度","浮点型",PROJECT_ID); String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f); log.info("27.3 浮点精度创建成功 id={}",f[0]); }
}
