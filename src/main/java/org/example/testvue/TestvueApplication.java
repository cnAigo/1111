package org.example.testvue;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

@SpringBootApplication
public class TestvueApplication {
    public static void main(String[] args) {
        SpringApplication.run(TestvueApplication.class, args);

        System.out.println("\n====== 正在初始化自动化测试引擎 (JUnit Launcher) ======");

        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(
                        selectClass("cases.RequirementTest"),
                        selectClass("cases.WordImportTest"),

                        selectClass("cases.ReqExportFullTest"),
                        selectClass("cases.ReqTest"),
                        selectClass("cases.ReqSpecTest"),

                        selectClass("cases.BasicAttributeTest"),
                        selectClass("cases.EnumAttributeTest"),
                        selectClass("cases.SpecialAttributeTest"),

                        selectClass("cases.CollaborativeEditTest"),
                        selectClass("cases.ReviewProcessTest"),
                        selectClass("cases.VersionTraceTest"),
                        selectClass("cases.OtherFunctionsTest")

                )
                .build();
        Launcher launcher = LauncherFactory.create();

        // 注册我们自定义的报告监听器
        CustomReportListener customListener = new CustomReportListener();
        launcher.registerTestExecutionListeners(customListener);

        System.out.println("====== 正在自动加载并执行所有测试用例 ======\n");
        launcher.execute(request);
    }

    /**
     * 自定义测试执行监听器，用于精确控制控制台输出格式
     */
    /**
     * 自定义测试执行监听器，用于精确控制控制台输出格式
     */
    static class CustomReportListener implements TestExecutionListener {
        private int passedTests = 0;
        private int skippedTests = 0;
        private int failedTests = 0;

        // 记录失败和跳过用例的详情，用于最后总结
        private final List<String> failureDetails = new ArrayList<>();

        @Override
        public void executionSkipped(TestIdentifier testIdentifier, String reason) {
            if (testIdentifier.isTest()) {
                skippedTests++;
                String testName = testIdentifier.getDisplayName();
                String shortReason = (reason != null) ? reason : "未知原因";
                // 格式: GNYL_038 跳过，跳过原因：...
                System.out.println(testName + " 跳过，跳过原因：" + shortReason);
            }
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
            if (testIdentifier.isTest()) {
                String testName = testIdentifier.getDisplayName();
                TestExecutionResult.Status status = testExecutionResult.getStatus();

                if (status == TestExecutionResult.Status.SUCCESSFUL) {
                    passedTests++;
                    // 格式: GNYL_012 成功
                    System.out.println(testName + " 成功");

                } else if (status == TestExecutionResult.Status.FAILED) {
                    failedTests++;
                    String reason = testExecutionResult.getThrowable().map(Throwable::getMessage).orElse("未知错误");
                    // 格式: GNYL_014 失败 失败原因：...
                    System.out.println(testName + " 失败 失败原因：" + reason);
                    failureDetails.add(testName + ": " + reason);

                } else if (status == TestExecutionResult.Status.ABORTED) {
                    skippedTests++;
                    String reason = testExecutionResult.getThrowable().map(Throwable::getMessage).orElse("未知中断");
                    // 格式: GNYL_031 跳过，跳过原因：...
                    System.out.println(testName + " 跳过，跳过原因：" + reason);
                }
            }
        }

        @Override
        public void testPlanExecutionFinished(TestPlan testPlan) {
            System.out.println("\n====== 总结 ======");
            System.out.println("通过: " + passedTests);
            System.out.println("跳过: " + skippedTests);
            System.out.println("失败: " + failedTests);
            System.out.println("==================\n");

            if (!failureDetails.isEmpty()) {
                System.out.println("====== 错误原因 ======");
                for (int i = 0; i < failureDetails.size(); i++) {
                    System.out.println((i + 1) + "、" + failureDetails.get(i));
                }
                System.out.println("======================");
            }
            System.out.flush();
        }
    }
}