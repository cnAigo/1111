package config;

/**
 * 测试配置 - 所有 URL 和凭据集中管理。
 * BASE_URL 优先从环境变量读取，支持 Jenkins 等 CI 动态传入。
 */
public class TestConfig {

    /** 应用基础地址 */
    public static final String BASE_URL = System.getenv().getOrDefault("BASE_URL",
            "https://192.168.6.171:8088");

    /** REST API 前缀 */
    public static final String API_PREFIX = BASE_URL + "/dev-api";

    /** 登录页面 */
    public static final String LOGIN_URL = BASE_URL + "/#/login";

    /** 需求管理页面 */
    public static final String REQUIREMENT_URL = BASE_URL + "/#/RequirementManagement";

    /** 系统管理页面 */
    public static final String SYSTEM_MANAGEMENT_URL = BASE_URL + "/#/SystemManagement";

    /** 管理员账号 */
    public static final String ADMIN_USER = "admin";
    public static final String ADMIN_PWD = "Aa123456";

    /** Playwright 认证状态文件 */
    public static final String AUTH_STATE_PATH = "auth.json";
}
