package cases.indicator;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.*;

@Tag("IndicatorModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ParameterApiTest extends IndicatorTestBase {

    private String logicStructureId;
    private String logicId;

    @BeforeAll
    void ensureStructure() {
        String resp = ind.addLogicStructure("AT_ParamTest_" + suffix(), "参数测试容器");
        logicStructureId = ind.extractId(resp);
        Assertions.assertNotNull(logicStructureId, "测试前需创建逻辑结构");

        String nodeId = String.valueOf(System.currentTimeMillis() % 10000000000L);
        String r2 = ind.addLogic(nodeId, "", "AT_Logic_Param", logicStructureId);
        logicId = ind.extractId(r2);
        Assertions.assertNotNull(logicId, "测试前需创建逻辑项");
    }

    @Test
    @DisplayName("IND_201: 逻辑项下新建指标参数(正向)")
    void test_addParameter() {
        String oid = String.valueOf(System.currentTimeMillis() % 10000000000L);
        String resp = ind.addParameter(oid, logicId, "AT_指标_" + suffix(),
                "自动化测试参数", logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "新建参数应成功, resp: " + resp);
        log.info("IND_201 通过: 新建参数");
    }

    @Test
    @DisplayName("IND_202: 查询参数列表(正向)")
    void test_searchParameterList() {
        String resp = ind.searchParameterList(logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "查询参数列表应成功, resp: " + resp);
        Assertions.assertTrue(resp.contains("data"), "应包含data字段");
        log.info("IND_202 通过: 查询参数列表");
    }

    @Test
    @DisplayName("IND_203: 更新指标参数值(正向)")
    void test_updateParameter() {
        String oid = String.valueOf(System.currentTimeMillis() % 10000000000L);
        String addResp = ind.addParameter(oid, logicId, "AT_Update_" + suffix(),
                "待更新", logicStructureId);
        String id = ind.extractId(addResp);
        Assertions.assertNotNull(id, "新建应返回ID");

        String resp = ind.updateParameter(id, null, "99", logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "更新应成功, resp: " + resp);
        log.info("IND_203 通过: 更新参数值 id={}", id);
    }

    @Test
    @DisplayName("IND_204: 查询参数值列表(正向)")
    void test_searchParameterValueList() {
        String resp = ind.searchParameterValueList(logicStructureId);
        Assertions.assertTrue(ind.isOk(resp), "查询参数值列表应成功, resp: " + resp);
        log.info("IND_204 通过: 查询参数值列表");
    }

    @Test
    @DisplayName("IND_205: 查询参数列表-无效结构ID(负向)")
    void test_searchInvalidStructure() {
        String resp = ind.searchParameterList("invalid_id_99999");
        log.info("IND_205 无效结构ID: {}", resp);
    }

    @Test
    @DisplayName("IND_206: 物理方案-新建与查询(正向)")
    void test_physicalScheme() {
        String name = "AT_Scheme_" + suffix();
        String resp = ind.addPhysicalScheme(logicStructureId, name);
        Assertions.assertTrue(ind.isOk(resp), "新建物理方案应成功, resp: " + resp);
        log.info("IND_206 通过: 新建物理方案 [{}]", name);

        String listResp = ind.searchPhysicalSchemeList(logicStructureId);
        Assertions.assertTrue(ind.isOk(listResp), "查询物理方案列表应成功");
    }
}
