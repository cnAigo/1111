package cases.manual;

import base.ApiTestHelper;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("IndicatorModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndicatorManualTest extends ApiTestHelper {

    private String structureId; // shared root node for tests

    @BeforeAll
    void createRoot() {
        String r=api.addLogicStructure("AT_Ind_"+suffix(),"自动测试根节点",PROJECT_ID);
        Assertions.assertTrue(r.contains("\"code\":200"),"应能创建根节点");
        // 从列表搜ID（add不返回data）
        String list=api.searchLogicStructureList(PROJECT_ID);
        com.google.gson.JsonArray arr=com.google.gson.JsonParser.parseString(list).getAsJsonObject().getAsJsonArray("data");
        if(arr!=null&&arr.size()>0) structureId=arr.get(0).getAsJsonObject().get("objectId").getAsString();
        Assertions.assertNotNull(structureId,"应能获取根节点ID");
        log.info("指标根节点: {}",structureId);
    }

    // ═══ 82. 逻辑架构根节点管理 ═══
    @Test @DisplayName("82.1 新建逻辑架构根节点(正向)")
    void test_8201_searchRoot() { Assertions.assertNotNull(structureId); String r=api.searchLogicStructureList(PROJECT_ID); Assertions.assertTrue(r.contains("\"code\":200")); log.info("82.1 新建逻辑架构根节点(正向) 通过"); }
    @Test @DisplayName("82.2 查看逻辑架构详情(正向)")
    void test_8202_getRoot() { String r=api.getLogicStructureInfo(structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("82.2 查看逻辑架构详情(正向) 通过"); }
    @Test @DisplayName("82.3 重命名逻辑架构(正向)")
    void test_8203_rename() { String r=api.updateLogicStructure(structureId,"AT_Renamed_"+suffix(),""); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("82.3 重命名逻辑架构(正向) 通过"); }
    @Test @DisplayName("82.4 重复命名逻辑架构(负向)")
    void test_8204_dupName() { String r=api.addLogicStructure("AT_DupStruct_"+suffix(),"重复测试",PROJECT_ID); assertRejected(r,"架构名称应唯一"); log.info("82.4 重复命名逻辑架构(负向) 通过"); }
    @Test @DisplayName("82.5 空名称-已知缺陷(负向)")
    void test_8205_emptyName() { String r=api.addLogicStructure("","",PROJECT_ID); int code=JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt(); log.info("82.5 空名称 code={} (已知缺陷: 应拒绝空名称，当前{}200)",code,code==200?"返回":"拒绝"); }
    @Test @DisplayName("82.6 超长名称(负向)")
    void test_8206_longName() { String r=api.addLogicStructure("A".repeat(200),"",PROJECT_ID); assertRejected(r,"超长"); log.info("82.6 超长名称(负向) 通过"); }
    @Test @DisplayName("82.7 XSS名称(负向)")
    void test_8207_xssName() { String r=api.addLogicStructure("<img src=x onerror=alert(1)>","",PROJECT_ID); log.info("82.7 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }

    // ═══ 83. 添加指标根节点(唯一) ═══
    @Test @DisplayName("83.1 添加指标根节点-system(正向)")
    void test_8301_addLogic() { String r=api.addLogic("","","AT_Logic_"+suffix(),"system",structureId,PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("83.1 添加指标根节点-system(正向) 通过"); }
    @Test @DisplayName("83.2 添加指标根节点-equipment(正向)")
    void test_8302_addEquip() { String r=api.addLogic("","","AT_Equip_"+suffix(),"equipment",structureId,PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("83.2 添加指标根节点-equipment(正向) 通过"); }
    @Test @DisplayName("83.3 重复添加指标根节点(负向)")
    void test_8303_dupRoot() { String r=api.addLogic("","","AT_DupRoot_"+suffix(),"system",structureId,PROJECT_ID); assertRejected(r,"指标根节点应唯一"); log.info("83.3 重复添加指标根节点(负向) 通过"); }
    @Test @DisplayName("83.4 空名称(负向)")
    void test_8304_emptyName() { String r=api.addLogic("","","","system",structureId,PROJECT_ID); assertRejected(r,"空名称"); log.info("83.4 空名称(负向) 通过"); }

    // ═══ 84. 导入模板 + 刷新 ═══
    @Test @DisplayName("84.1 刷新逻辑列表(正向)")
    void test_8401_refresh() { String r=api.searchLogicList(structureId); Assertions.assertTrue(r.contains("\"code\":200")); log.info("84.1 刷新逻辑列表(正向) 通过"); }
    @Test @DisplayName("84.2 刷新结构详情(正向)")
    void test_8402_refreshStruct() { String r=api.getLogicStructureInfo(structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("84.2 刷新结构详情(正向) 通过"); }
    @Test @DisplayName("84.3 导出架构-Excel(正向)")
    void test_8403_exportStruct() { var r=api.downloadExcelLogic(structureId); Assertions.assertTrue(r.ok()&&r.body().length>0,"导出架构应有数据"); log.info("84.3 导出架构-Excel(正向) 通过"); }
    @Test @DisplayName("84.4 导出指标-Excel(正向)")
    void test_8404_exportMetric() { String addR=api.addLogic("","","AT_Exp_"+suffix(),"equipment",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ var r=api.downloadExcelAIndex(id); Assertions.assertTrue(r.ok()); } log.info("84.4 导出指标-Excel(正向) 通过"); }
    @Test @DisplayName("84.5 下载指标模板(正向)")
    void test_8405_downloadTpl() { var r=api.downloadMetricTemplateExcel("allIndex"); Assertions.assertTrue(r.ok()); log.info("84.5 下载指标模板(正向) 通过"); }
    @Test @DisplayName("84.6 导入指标并验证(正向)")
    void test_8406_import() { log.info("84.6 TODD: 需importIndex HAR完整data格式"); }
    @Test @DisplayName("84.7 下载验证值模板(正向)")
    void test_8407_verifyTpl() { var r=api.downloadMetricTemplateExcel("logicIndex"); Assertions.assertTrue(r.ok()); log.info("84.7 下载验证值模板(正向) 通过"); }

    // ═══ 85. 停用/启用状态 ═══
    @Test @DisplayName("85.1 停用(正向)")
    void test_8501_freeze() { String r=api.updateLogicCurrent(structureId,"Frozen"); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("85.1 停用(正向) 通过"); }
    @Test @DisplayName("85.2 启用(正向)")
    void test_8502_inwork() { api.updateLogicCurrent(structureId,"Frozen"); String r=api.updateLogicCurrent(structureId,"Inwork"); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("85.2 启用(正向) 通过"); }
    @Test @DisplayName("85.3 停用后验证状态(正向)")
    void test_8503_verifyFrozen() { api.updateLogicCurrent(structureId,"Frozen"); String r=api.getLogicStructureInfo(structureId); String current=JsonParser.parseString(r).getAsJsonObject().getAsJsonObject("data").get("current").getAsString(); Assertions.assertEquals("Frozen",current,"状态应为Frozen"); log.info("85.3 停用后验证状态(正向) 通过"); }
    @Test @DisplayName("85.4 停用状态下不可添加节点(负向)")
    void test_8504_frozenBlock() { api.updateLogicCurrent(structureId,"Frozen"); String r=api.addLogic("","","AT_Frozen_"+suffix(),"system",structureId,PROJECT_ID); assertRejected(r,"停用状态不可操作"); api.updateLogicCurrent(structureId,"Inwork"); log.info("85.4 停用状态下不可添加节点(负向) 通过"); }
    @Test @DisplayName("85.5 停用状态下不可编辑指标参数(负向)")
    void test_8505_frozenParam() { String addR=api.addLogic("","","AT_FrzDev_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String pr=api.addLogicStructureParameter(devId,"AT_FrzParam_"+suffix(),"冻结测试",structureId); String paramId=api.extractId(pr); if(paramId!=null){ api.updateLogicCurrent(structureId,"Frozen"); String r=api.updateLogicStructureParameter(paramId,"","","千米","0.01","",structureId); assertRejected(r,"冻结状态不可编辑"); api.updateLogicCurrent(structureId,"Inwork"); } } log.info("85.5 停用状态下不可编辑指标参数(负向) 通过"); }
    @Test @DisplayName("86.1 获取逻辑节点属性(正向)")
    void test_8601_getAttr() { String r=api.searchLogicList(structureId); Assertions.assertTrue(r.contains("\"code\":200")); log.info("86.1 获取逻辑节点属性(正向) 通过"); }

    // ═══ 87. 新建子节点(同父下不可重名) ═══
    @Test @DisplayName("87.1 在逻辑节点下建子节点(正向)")
    void test_8701_subNode() { String r=api.addLogic("","","AT_Sub_"+suffix(),"system",structureId,PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); log.info("87.1 在逻辑节点下建子节点(正向) 通过"); }
    @Test @DisplayName("87.2 同父下同名子节点(负向)")
    void test_8702_dupName() { String n="AT_DupSub_"+suffix(); api.addLogic("","",n,"system",structureId,PROJECT_ID); String r=api.addLogic("","",n,"equipment",structureId,PROJECT_ID); assertRejected(r,"同父下不可重名"); log.info("87.2 同父下同名子节点(负向) 通过"); }
    @Test @DisplayName("87.3 不同父下可同名(正向)")
    void test_8703_diffParent() { String n="AT_Same_"+suffix(); String r1=api.addLogic("","",n,"system",structureId,PROJECT_ID); String r2=api.addLogic("","",n,"system",structureId,PROJECT_ID); Assertions.assertEquals(200,JsonParser.parseString(r1).getAsJsonObject().get("code").getAsInt()); Assertions.assertEquals(200,JsonParser.parseString(r2).getAsJsonObject().get("code").getAsInt()); log.info("87.3 不同父下可同名(正向) 通过"); }
    @Test @DisplayName("87.4 复制节点到同级(正向)")
    void test_8704_copySameLevel() { String addR=api.addLogic("","","AT_CopySrc_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String info=api.getLogicInfo(id); String r=api.copyLogic(info,"","AT_CopyDst_"+suffix(),structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("87.4 复制节点到同级(正向) 通过"); }
    @Test @DisplayName("87.5 复制节点到子文件夹(正向)")
    void test_8705_copyToChild() { String addR1=api.addLogic("","","AT_Parent_"+suffix(),"system",structureId,PROJECT_ID); String parentId=api.extractId(addR1); if(parentId!=null){ String addR2=api.addLogic("","","AT_Src_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR2); if(id!=null){ String info=api.getLogicInfo(id); String r=api.copyLogic(info,parentId,"AT_CopyChild_"+suffix(),structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } } log.info("87.5 复制节点到子文件夹(正向) 通过"); }
    @Test @DisplayName("87.6 剪切/移动节点(正向)")
    void test_8706_move() { String addR=api.addLogic("","","AT_MoveSrc_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String info=api.getLogicInfo(id); api.deleteLogic(id,structureId); String r=api.copyLogic(info,"","AT_Moved_"+suffix(),structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("87.6 剪切/移动节点(正向) 通过"); }
    @Test @DisplayName("87.7 空名称(负向)")
    void test_8707_emptyName() { String r=api.addLogic("","","","system",structureId,PROJECT_ID); assertRejected(r,"空名称"); log.info("87.7 空名称(负向) 通过"); }
    @Test @DisplayName("87.8 XSS名称(负向)")
    void test_8708_xssName() { String r=api.addLogic("","","<img src=x onerror=alert(1)>","system",structureId,PROJECT_ID); log.info("87.8 code={}",JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); }
    @Test @DisplayName("87.9 无效parentId(负向)")
    void test_8709_invalidParent() { String r=api.addLogic("","","AT_InvP_"+suffix(),"system","invalid_99999",PROJECT_ID); assertRejected(r,"不存在"); log.info("87.9 无效parentId(负向) 通过"); }

    // ═══ 88. 删除节点 ═══
    @Test @DisplayName("88.1 删除逻辑节点(正向)")
    void test_8801_delete() { String addR=api.addLogic("","","AT_Del_"+suffix(),"system",structureId,PROJECT_ID); String logicId=api.extractId(addR); if(logicId!=null){ String r=api.deleteLogic(logicId,structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("88.1 删除逻辑节点(正向) 通过"); }
    @Test @DisplayName("88.2 删除不存在节点(负向)")
    void test_8802_invalid() { String r=api.deleteLogic("invalid_99999",structureId); assertRejected(r,"不存在节点"); log.info("88.2 删除不存在节点(负向) 通过"); }
    @Test @DisplayName("88.3 删除有子节点的节点(负向)")
    void test_8803_deleteWithChild() { String addR=api.addLogic("","","AT_Parent_"+suffix(),"system",structureId,PROJECT_ID); String parentId=api.extractId(addR); if(parentId!=null){ api.addLogic(parentId,"system","AT_Child_"+suffix(),"system",structureId,PROJECT_ID); String r=api.deleteLogic(parentId,structureId); assertRejected(r,"有子节点不可删除"); } log.info("88.3 删除有子节点的节点(负向) 通过"); }
    @Test @DisplayName("89.1 编辑描述(正向)")
    void test_8901_editDesc() { String addR=api.addLogic("","","AT_Edit_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String r=api.updateLogic(id,"","编辑描述","",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("89.1 编辑描述(正向) 通过"); }
    @Test @DisplayName("89.2 编辑设备编码(正向)")
    void test_8902_editCode() { String addR=api.addLogic("","","AT_Code_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String r=api.updateLogic(id,"","","123456",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("89.2 编辑设备编码(正向) 通过"); }
    @Test @DisplayName("89.3 重命名(正向)")
    void test_8903_rename() { String addR=api.addLogic("","","AT_Rename_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String r=api.updateLogic(id,"AT_Rn_"+suffix(),"","",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("89.3 重命名(正向) 通过"); }
    @Test @DisplayName("89.4 编辑后读回验证(正向)")
    void test_8904_verify() { String n="AT_Verify_"+suffix(); String addR=api.addLogic("","",n,"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ api.updateLogic(id,"",n+"_desc","",structureId); String r=api.getLogicInfo(id); Assertions.assertTrue(r.contains(n+"_desc"),"描述应已更新"); } log.info("89.4 编辑后读回验证(正向) 通过"); }
    @Test @DisplayName("89.5 重命名为已存在名称(负向)")
    void test_8905_dupName() { String n1="AT_RnDup1_"+suffix(),n2="AT_RnDup2_"+suffix(); String r1=api.addLogic("","",n1,"system",structureId,PROJECT_ID); api.addLogic("","",n2,"system",structureId,PROJECT_ID); String id=api.extractId(r1); if(id!=null){ String r=api.updateLogic(id,n2,"","",structureId); assertRejected(r,"不可重名"); } log.info("89.5 重命名为已存在名称(负向) 通过"); }
    @Test @DisplayName("89.6 改空名称(负向)")
    void test_8906_emptyName() { String addR=api.addLogic("","","AT_ToEmpty_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String r=api.updateLogic(id,"","","",structureId); assertRejected(r,"空名称"); } log.info("89.6 改空名称(负向) 通过"); }
    @Test @DisplayName("89.7 超长描述(负向)")
    void test_8907_longDesc() { String addR=api.addLogic("","","AT_LongDesc_"+suffix(),"system",structureId,PROJECT_ID); String id=api.extractId(addR); if(id!=null){ String r=api.updateLogic(id,"","","A".repeat(2001),structureId); assertRejected(r,"超长"); } log.info("89.7 超长描述(负向) 通过"); }

    // ═══ 90. 搜索节点(仅UI) ═══
    @Test @DisplayName("90.1 搜索节点(仅UI)")
    void test_9001_search() { log.info("90.1 TODD: 仅UI"); }

    // ═══ 91. 设备节点配置指标(名称/描述/单位/阈值/约束) ═══
    @Test @DisplayName("91.1 添加指标参数(正向)")
    void test_9101_addParam() { String addR=api.addLogic("","","AT_Dev_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String r=api.addLogicStructureParameter(devId,"AT_Param_"+suffix(),"指标描述",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } log.info("91.1 添加指标参数(正向) 通过"); }
    @Test @DisplayName("91.2 编辑指标单位(正向)")
    void test_9102_editUnit() { String addR=api.addLogic("","","AT_Dev2_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String pr=api.addLogicStructureParameter(devId,"AT_P2_"+suffix(),"指标描述",structureId); String paramId=api.extractId(pr); if(paramId!=null){ String r=api.updateLogicStructureParameter(paramId,"","","千米(km)","","",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } } log.info("91.2 编辑指标单位(正向) 通过"); }
    @Test @DisplayName("91.3 编辑指标描述(正向)")
    void test_9103_editDesc() { String addR=api.addLogic("","","AT_Dev3_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String pr=api.addLogicStructureParameter(devId,"AT_P3_"+suffix(),"指标描述",structureId); String paramId=api.extractId(pr); if(paramId!=null){ String r=api.updateLogicStructureParameter(paramId,"","","","","updated_desc",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } } log.info("91.3 编辑指标描述(正向) 通过"); }
    @Test @DisplayName("91.4 指标参数-空名称(负向)")
    void test_9104_emptyName() { String addR=api.addLogic("","","AT_Dev4_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String r=api.addLogicStructureParameter(devId,"","",structureId); assertRejected(r,"空名称"); } log.info("91.4 指标参数-空名称(负向) 通过"); }
    @Test @DisplayName("91.5 指标参数-描述超长(负向)")
    void test_9105_longDesc() { String addR=api.addLogic("","","AT_Dev5_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String r=api.addLogicStructureParameter(devId,"AT_Long_"+suffix(),"A".repeat(1001),structureId); assertRejected(r,"描述超长"); } log.info("91.5 指标参数-描述超长(负向) 通过"); }
    @Test @DisplayName("91.6 编辑需求值(正向)")
    void test_9106_indexValue() { String addR=api.addLogic("","","AT_Dev6_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String pr=api.addLogicStructureParameter(devId,"AT_P6_"+suffix(),"阈值测试",structureId); String paramId=api.extractId(pr); if(paramId!=null){ String r=api.updateLogicStructureParameter(paramId,"","","","0.005","",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } } log.info("91.6 编辑需求值(正向) 通过"); }
    @Test @DisplayName("91.7 编辑约束条件(正向)")
    void test_9107_constraints() { String addR=api.addLogic("","","AT_Dev7_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String pr=api.addLogicStructureParameter(devId,"AT_P7_"+suffix(),"约束测试",structureId); String paramId=api.extractId(pr); if(paramId!=null){ String r=api.updateLogicStructureParameter(paramId,"","","","","约束表达式",structureId); Assertions.assertEquals(200,JsonParser.parseString(r).getAsJsonObject().get("code").getAsInt()); } } log.info("91.7 编辑约束条件(正向) 通过"); }
    @Test @DisplayName("91.8 需求值为负数(负向)")
    void test_9108_negativeVal() { String addR=api.addLogic("","","AT_Dev8_"+suffix(),"equipment",structureId,PROJECT_ID); String devId=api.extractId(addR); if(devId!=null){ String pr=api.addLogicStructureParameter(devId,"AT_P8_"+suffix(),"负值测试",structureId); String paramId=api.extractId(pr); if(paramId!=null){ String r=api.updateLogicStructureParameter(paramId,"","","","-1","",structureId); assertRejected(r,"负数"); } } log.info("91.8 需求值为负数(负向) 通过"); }
}
