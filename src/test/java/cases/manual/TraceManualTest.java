package cases.manual;

import base.ApiTestHelper;
import org.junit.jupiter.api.*;

@Tag("ReqFolderModule")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TraceManualTest extends ApiTestHelper {

    @Test @DisplayName("61. 追溯(占位)")
    void test_6101() { log.info("61. 追溯: TODD"); }
    @Test @DisplayName("62. 追溯(占位)")
    void test_6201() { log.info("62. TODD"); }
    @Test @DisplayName("63. 追溯(占位)")
    void test_6301() { log.info("63. TODD"); }
    @Test @DisplayName("64. 追溯(占位)")
    void test_6401() { log.info("64. TODD"); }
    @Test @DisplayName("65. 追溯(占位)")
    void test_6501() { log.info("65. TODD"); }
    @Test @DisplayName("66. 追溯(占位)")
    void test_6601() { log.info("66. TODD"); }
    @Test @DisplayName("67. 追溯(占位)")
    void test_6701() { log.info("67. TODD"); }
    @Test @DisplayName("68. 追溯(占位)")
    void test_6801() { log.info("68. TODD"); }
}
