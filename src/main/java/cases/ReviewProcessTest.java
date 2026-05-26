package cases;

import actions.ReqApiActions;
import base.BaseTest;
import config.TestConstants;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pages.RequirementPage;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReviewProcessTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(ReviewProcessTest.class);
    private ReqApiActions api;
    private RequirementPage rPage;

    @BeforeAll
    public void init() {
        api = new ReqApiActions(page.request());
        rPage = new RequirementPage(page);
    }

    @BeforeEach
    public void navigate() {
        navigateToRequirementModule();
    }

    // ========== 需求审签 - 流程定义 ==========
    @Test
    @Order(2390)
    @DisplayName("GNYL_239: 进入流程定义页面")
    public void test_GNYL_239() {
        // TODO: 悬浮"应用" → 点击"流程定义"
    }

    @Test
    @Order(2400)
    @DisplayName("GNYL_240: 存在的流程关键字检索")
    public void test_GNYL_240() {
        // TODO: 输入存在的流程关键字 → 搜索
    }

    @Test
    @Order(2410)
    @DisplayName("GNYL_241: 流程关键字模糊查询")
    public void test_GNYL_241() {
        // TODO: 输入部分关键字 → 搜索 → 筛选出包含关键字的流程
    }

    @Test
    @Order(2420)
    @DisplayName("GNYL_242: 不存在的流程关键字检索")
    public void test_GNYL_242() {
        // TODO: 输入不存在关键字 → 搜索 → 暂无数据
    }

    @Test
    @Order(2430)
    @DisplayName("GNYL_243: 存在的流程名称检索")
    public void test_GNYL_243() {
        // TODO: 输入存在的流程名称 → 搜索
    }

    @Test
    @Order(2440)
    @DisplayName("GNYL_244: 流程名称模糊查询")
    public void test_GNYL_244() {
        // TODO: 输入部分流程名称关键字 → 搜索
    }

    @Test
    @Order(2450)
    @DisplayName("GNYL_245: 不存在的流程名称检索")
    public void test_GNYL_245() {
        // TODO: 输入不存在的流程名称 → 搜索 → 暂无数据
    }

    @Test
    @Order(2460)
    @DisplayName("GNYL_246: 组合查询")
    public void test_GNYL_246() {
        // TODO: 输入流程关键字+流程名称 → 搜索
    }

    @Test
    @Order(2470)
    @DisplayName("GNYL_247: 重置")
    public void test_GNYL_247() {
        // TODO: 输入条件 → 搜索 → 重置 → 清空条件
    }

    @Test
    @Order(2480)
    @DisplayName("GNYL_248: 流程查看")
    public void test_GNYL_248() {
        // TODO: 选择流程 → 操作栏"查看"
    }

    @Test
    @Order(2490)
    @DisplayName("GNYL_249: 切换流程状态(活动→非活动)")
    public void test_GNYL_249() {
        // TODO: 选择活动状态流程 → 点击"活动" → 变为非活动
    }

    @Test
    @Order(2500)
    @DisplayName("GNYL_250: 切换流程状态(非活动→活动)")
    public void test_GNYL_250() {
        // TODO: 选择非活动状态流程 → 点击"非活动" → 变为活动
    }

    @Test
    @Order(2510)
    @DisplayName("GNYL_251: 流程删除")
    public void test_GNYL_251() {
        // TODO: 选择流程 → 删除 → 确认
    }

    @Test
    @Order(2520)
    @DisplayName("GNYL_252: 流程刷新")
    public void test_GNYL_252() {
        // TODO: 点击列表右上角"刷新"
    }

    @Test
    @Order(2530)
    @DisplayName("GNYL_253: 保存流程图")
    public void test_GNYL_253() {
        // TODO: 在线绘制流程图 → 保存为BPMN
    }

    @Test
    @Order(2540)
    @DisplayName("GNYL_254: 部署流程图")
    public void test_GNYL_254() {
        // TODO: 在线绘制流程图 → 部署
    }

    @Test
    @Order(2550)
    @DisplayName("GNYL_255: 绘制流程图弹框全屏展示")
    public void test_GNYL_255() {
        // TODO: 点击"全屏"图标 → 全屏展示
    }

    @Test
    @Order(2560)
    @DisplayName("GNYL_256: 导入流程图")
    public void test_GNYL_256() {
        // TODO: 导入流程图 → 上传文件 → 确定
    }

    @Test
    @Order(2570)
    @DisplayName("GNYL_257: 上传符合要求的文件(bpmn/bar/zip)")
    public void test_GNYL_257() {
        // TODO: 上传bpmn/bar/zip格式文件 → 上传成功
    }

    @Test
    @Order(2580)
    @DisplayName("GNYL_258: 上传不符合要求的文件")
    public void test_GNYL_258() {
        // TODO: 上传非bpmn/bar/zip格式文件 → 提示
    }

    @Test
    @Order(2590)
    @DisplayName("GNYL_259: 删除上传的文件")
    public void test_GNYL_259() {
        // TODO: 上传文件 → 点击文件后x → 删除
    }

    // ========== 需求规格审签(送审) ==========
    @Test
    @Order(2600)
    @DisplayName("GNYL_260: 另存为草稿")
    public void test_GNYL_260() {
        // TODO: 打开需求规格 → 悬浮"工作中" → 审签 → 完善信息 → 另存为草稿
    }

    @Test
    @Order(2610)
    @DisplayName("GNYL_261: 提交审签")
    public void test_GNYL_261() {
        // TODO: 审签弹框 → 完善信息 → 启动
    }

    @Test
    @Order(2620)
    @DisplayName("GNYL_262: 我的送审单重新提交")
    public void test_GNYL_262() {
        // TODO: 我的送审单 → 选择工作中/已退回任务 → 编辑 → 启动
    }

    // ========== 需求规格审批 ==========
    @Test
    @Order(2630)
    @DisplayName("GNYL_263: 一次审批任务(同意)")
    public void test_GNYL_263() {
        // TODO: 待办任务 → 第一次审批 → 同意 → 填写批注 → 确定
    }

    @Test
    @Order(2640)
    @DisplayName("GNYL_264: 一次审批任务(不同意)")
    public void test_GNYL_264() {
        // TODO: 待办任务 → 第一次审批 → 不同意 → 填写批注 → 确定
    }

    @Test
    @Order(2650)
    @DisplayName("GNYL_265: 二次审批任务(同意)")
    public void test_GNYL_265() {
        // TODO: 待办任务 → 第二次审批 → 同意 → 填写批注 → 确定
    }

    @Test
    @Order(2660)
    @DisplayName("GNYL_266: 二次审批任务(不同意)")
    public void test_GNYL_266() {
        // TODO: 待办任务 → 第二次审批 → 不同意 → 填写批注 → 确定
    }

}
