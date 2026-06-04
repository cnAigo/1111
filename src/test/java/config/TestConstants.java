package config;

/**
 * 测试常量 — 所有测试数据名称集中管理。
 * 数据由 SetupEnvironment.createAll() 自动创建（幂等）。
 */
public class TestConstants {

    /** 项目 ID — 优先读环境变量，否则由 @BeforeAll 通过 API 动态获取 */
    public static String PROJECT_ID = System.getenv().getOrDefault("TAAS_PROJECT_ID", null);

    /** 项目名称（用于 API 动态查询 projectId） */
    public static final String PROJECT_NAME = "test";

    /** 需求树根节点名称 */
    public static final String ROOT_NODE = "需求（根节点）";

    /** 父文件夹 */
    public static final String PARENT_FOLDER = "测试父文件夹";

    /** 子文件夹 */
    public static final String CHILD_FOLDER_1 = "子文件夹_01";
    public static final String CHILD_FOLDER_2 = "子文件夹_02";
    public static final String CHILD_FOLDER_3 = "子文件夹_03";

    /** 需求规格名称 */
    public static final String REQ_NAME1 = "测试需求规格_01";
    public static final String REQ_NAME2 = "测试需求规格_02";
}
