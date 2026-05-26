package cases;

import base.BaseTest;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class CommonCompatibilityTest extends BaseTest {

    private static final Logger log = LoggerFactory.getLogger(CommonCompatibilityTest.class);

    @Test
    @DisplayName("JRXYL_001: Chrome浏览器兼容性测试 - 待实现")
    public void test_JRXYL_001() {
        log.info("JRXYL_001: Chrome浏览器兼容性测试 - 待实现");
    }

    @Test
    @DisplayName("JRXYL_002: Firefox浏览器兼容性测试 - 待实现")
    public void test_JRXYL_002() {
        log.info("JRXYL_002: Firefox浏览器兼容性测试 - 待实现");
    }

    @Test
    @DisplayName("JRXYL_003: Edge浏览器兼容性测试 - 待实现")
    public void test_JRXYL_003() {
        log.info("JRXYL_003: Edge浏览器兼容性测试 - 待实现");
    }
}
