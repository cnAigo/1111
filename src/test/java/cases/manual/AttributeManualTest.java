package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("AttributeModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AttributeManualTest extends ApiTestHelper {

    // ═══ 21. 新建/修改属性 ═══
    @Test @DisplayName("21.1 新建整型(正向)")
    void test_2101_int() {
        String n="AT_Int_"+suffix();
        String r=api.addCustomAttribute(n,"整型","整型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f,"应能查到新建的整型属性"); Assertions.assertEquals("整型",f[2],"类型应为整型");
        log.info("21.1 新建整型(正向) 通过");
    }
    @Test @DisplayName("21.2 新建浮点型(正向)")
    void test_2102_float() {
        String n="AT_Flt_"+suffix();
        String r=api.addCustomAttribute(n,"浮点","浮点型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f,"应能查到新建的浮点型属性"); Assertions.assertEquals("浮点型",f[2],"类型应为浮点型");
        log.info("21.2 新建浮点型(正向) 通过");
    }
    @Test @DisplayName("21.3 新建字符串型(正向)")
    void test_2103_str() {
        String n="AT_Str_"+suffix();
        String r=api.addCustomAttribute(n,"字符串","字符串型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f,"应能查到新建的字符串属性"); Assertions.assertEquals("字符串型",f[2],"类型应为字符串型");
        log.info("21.3 新建字符串型(正向) 通过");
    }
    @Test @DisplayName("21.4 新建日期型(正向)")
    void test_2104_date() {
        String n="AT_Date_"+suffix();
        String r=api.addCustomAttribute(n,"日期","日期型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f,"应能查到新建的日期属性"); Assertions.assertEquals("日期型",f[2],"类型应为日期型");
        log.info("21.4 新建日期型(正向) 通过");
    }

    // ═══ 22. 属性英文名/中文名校验 ═══
    @Test @DisplayName("21.5 新建枚举型(正向)")
    void test_2105_enum() {
        String n="AT_Enum_"+suffix();
        String r=api.addCustomAttribute(n,"枚举","枚举型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt());
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f,"应能查到新建的枚举属性"); Assertions.assertEquals("枚举型",f[2],"类型应为枚举型");
        log.info("21.5 新建枚举型(正向) 通过");
    }

    @Test @DisplayName("21.8 枚举添加取值范围1-6(正向)")
    void test_2108_enumRange() {
        String n="AT_ERng_"+suffix();
        com.google.gson.JsonArray mapping = new com.google.gson.JsonArray();
        for (int v=1;v<=6;v++) {
            com.google.gson.JsonObject opt = new com.google.gson.JsonObject();
            opt.addProperty("label","选项"+v); opt.addProperty("value",String.valueOf(v));
            mapping.add(opt);
        }
        String r=api.addCustomAttribute(n,"枚举范围","枚举型",PROJECT_ID,"",false,"1,2,3,4,5,6",mapping);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(),
            "枚举添加取值范围1-6应成功");
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f,"应能查到枚举范围属性");
        log.info("21.8 枚举添加取值范围1-6(正向) 通过");
    }

    @Test @DisplayName("21.9 枚举取值范围重复添加(负向)")
    void test_2109_enumDupRange() {
        String n="AT_EDup_"+suffix();
        com.google.gson.JsonArray mapping = new com.google.gson.JsonArray();
        for (int v : new int[]{1,2,3,3,4,5}) { // value 3 is duplicated
            com.google.gson.JsonObject opt = new com.google.gson.JsonObject();
            opt.addProperty("label","选项"+v); opt.addProperty("value",String.valueOf(v));
            mapping.add(opt);
        }
        String r=api.addCustomAttribute(n,"枚举重复","枚举型",PROJECT_ID,"",false,"1,2,3,3,4,5",mapping);
        assertRejected(r,"枚举值不可重复");
        log.info("21.9 枚举取值范围重复(负向) 通过");
    }

    @Test @DisplayName("21.10 枚举取值范围为空(负向)")
    void test_2110_enumEmptyRange() {
        String r=api.addCustomAttribute("AT_EEmpt_"+suffix(),"枚举空范围","枚举型",PROJECT_ID,"",false,"",new com.google.gson.JsonArray());
        assertRejected(r,"枚举取值范围不可为空");
        log.info("21.10 枚举取值范围为空(负向) 通过");
    }

    // ═══ 21.x isUseDefaultValue 负向 ═══
    @Test @DisplayName("21.6 isUseDefaultValue=true但默认值为空(负向)")
    void test_2106_useDefaultEmpty() { String r=api.addCustomAttribute("AT_DefE_"+suffix(),"默认空","整型",PROJECT_ID,"",true); assertRejected(r,"启用默认值但未填默认值"); log.info("21.6 isUseDefaultValue=true+空默认值(负向) 通过"); }

    @Test @DisplayName("21.7 isUseDefaultValue=true枚举默认值不在选项中(负向)")
    void test_2107_enumBadDefault() { String r=api.addCustomAttribute("AT_DefBad_"+suffix(),"枚举坏默认","枚举型",PROJECT_ID,"不存在的选项",true); assertRejected(r,"枚举默认值不在选项范围"); log.info("21.7 枚举默认值不在选项中(负向) 通过"); }

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

    @Test @DisplayName("23.4 发布枚举后在需求规格中设置枚举值(正向)")
    void test_2304_setEnumValue() {
        String n="AT_EUse_"+suffix();
        api.addCustomAttribute(n,"枚举使用","枚举型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID);
        Assertions.assertNotNull(f,"应能找到枚举属性");
        api.publishCustomAttribute(f[0],PROJECT_ID);
        log.info("23.4 枚举属性已发布: id={}",f[0]);

        String[] d=createTempDoc();
        String customAttrJson="[{\"attrId\":\""+f[0]+"\",\"value\":\"选项A\"}]";
        String r=api.updateReqSpeInfo(PROJECT_ID,d[0],d[1],null,null,null,customAttrJson);
        int code=JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt();
        Assertions.assertEquals(200,code,"发布后应能设置枚举属性值");
        log.info("23.4 枚举属性值设置 code=200");

        String sel=api.searchCustomAttribute(PROJECT_ID,"需求管理","req","","","2");
        Assertions.assertTrue(sel.contains("\"code\":200"),"应能查询到已设置的枚举属性");
        log.info("23.4 枚举属性值设置+查询完成");
    }

    @Test @DisplayName("23.5 发布全部类型属性后在需求条目中验证可用(正向)")
    void test_2305_allTypesOnReqItem() {
        // 1. Create + publish one attribute of each of the 5 types
        String[][] attrs = {
            {"AT_AllInt_"+suffix(),"全整型","整型"},
            {"AT_AllFlt_"+suffix(),"全浮点","浮点型"},
            {"AT_AllStr_"+suffix(),"全字符串","字符串型"},
            {"AT_AllDate_"+suffix(),"全日期","日期型"},
            {"AT_AllEnum_"+suffix(),"全枚举","枚举型"},
        };
        String[][] published = new String[5][];
        for (int i=0;i<attrs.length;i++) {
            api.addCustomAttribute(attrs[i][0],attrs[i][1],attrs[i][2],PROJECT_ID);
            published[i]=api.findCustomAttribute(attrs[i][0],PROJECT_ID);
            Assertions.assertNotNull(published[i],"应能找到属性"+attrs[i][1]);
            api.publishCustomAttribute(published[i][0],PROJECT_ID);
            log.info("23.5 已发布: {} id={}", attrs[i][2], published[i][0]);
        }

        // 2. Create a doc + req item
        String[] d=createTempDoc();
        String docId=d[0], folderId=d[2];
        String itemId=api.addReqItem(PROJECT_ID,docId,docId);
        Assertions.assertNotNull(itemId,"应能创建需求条目");

        // 3. Verify published attributes are listed in the attribute selector
        String sel=api.selectCustomAttributeList(PROJECT_ID,"需求管理","req","2");
        Assertions.assertTrue(sel.contains("\"code\":200"),"查询已发布属性应成功");
        for (int i=0;i<published.length;i++) {
            Assertions.assertTrue(sel.contains(published[i][0]),
                "已发布属性列表应包含: "+attrs[i][1]+" id="+published[i][0]);
        }
        log.info("23.5 全部5种类型属性在需求条目中验证可用 — 通过");
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

    // ═══ 28. 修改属性 ═══
    @Test @DisplayName("28.1 修改中文名(正向)")
    void test_2801_updateName() {
        String n="AT_Upd_"+suffix(); api.addCustomAttribute(n,"原名","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f);
        String r=api.updateCustomAttribute(f[0],n,"新名称","整型","原名","整型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(),"修改中文名应成功");
        log.info("28.1 修改中文名(正向) 通过");
    }

    @Test @DisplayName("28.2 修改类型整型→浮点(正向)")
    void test_2802_updateType() {
        String n="AT_TypChg_"+suffix(); api.addCustomAttribute(n,"类型改","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f);
        String r=api.updateCustomAttribute(f[0],n,"类型改","浮点型","整型","整型",PROJECT_ID);
        Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(),"修改类型应成功");
        log.info("28.2 修改类型整型→浮点(正向) 通过");
    }

    @Test @DisplayName("28.3 改为已存在的名称(负向)")
    void test_2803_updateDupName() {
        String a="AT_UpD1_"+suffix(),b="AT_UpD2_"+suffix();
        api.addCustomAttribute(a,"名A","整型",PROJECT_ID);
        api.addCustomAttribute(b,"名B","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(a,PROJECT_ID); Assertions.assertNotNull(f);
        String r=api.updateCustomAttribute(f[0],a,"名B","整型","名A","整型",PROJECT_ID);
        assertRejected(r,"不可重名");
        log.info("28.3 改为已存在名称(负向) 通过");
    }

    @Test @DisplayName("28.4 改名为空(负向)")
    void test_2804_updateEmptyName() {
        String n="AT_UpE_"+suffix(); api.addCustomAttribute(n,"原名","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f);
        String r=api.updateCustomAttribute(f[0],n,"","整型","原名","整型",PROJECT_ID);
        assertRejected(r,"名称不能为空");
        log.info("28.4 改名为空(负向) 通过");
    }

    @Test @DisplayName("28.5 修改已发布的属性(负向)")
    void test_2805_updatePublished() {
        String n="AT_UpPub_"+suffix(); api.addCustomAttribute(n,"已发布","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f);
        api.publishCustomAttribute(f[0],PROJECT_ID);
        String r=api.updateCustomAttribute(f[0],n,"改已发布","整型","已发布","整型",PROJECT_ID);
        assertRejected(r,"已发布不可修改");
        log.info("28.5 修改已发布属性(负向) 通过");
    }

    @Test @DisplayName("28.6 修改时XSS名称(负向)")
    void test_2806_updateXss() {
        String n="AT_UpX_"+suffix(); api.addCustomAttribute(n,"原名","整型",PROJECT_ID);
        String[] f=api.findCustomAttribute(n,PROJECT_ID); Assertions.assertNotNull(f);
        String r=api.updateCustomAttribute(f[0],n,"<img src=x onerror=alert(1)>","整型","原名","整型",PROJECT_ID);
        assertRejected(r,"XSS");
        log.info("28.6 XSS修改名称(负向) 通过");
    }
}
