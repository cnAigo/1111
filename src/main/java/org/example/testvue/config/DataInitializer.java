package org.example.testvue.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.example.testvue.entity.*;
import org.example.testvue.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.*;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(DataInitializer.class);
    // Match @DisplayName("...")  — note: the annotation value may contain escaped quotes, keep it simple
    private static final Pattern DISPLAY_NAME = Pattern.compile("@DisplayName\\(\"([^\"]+)\"\\)");
    // JUnit5 test methods: public void X(), protected void X(), or just void X()
    private static final Pattern METHOD_DEF = Pattern.compile("(?:public\\s+|protected\\s+)?void\\s+(\\w+)\\s*\\(");

    private final TestCaseDetailRepository repo;
    private final ApiProjectRepository projectRepo;
    private final UiElementRepository elementRepo;
    private final ApiCollectionRepository collRepo;
    private final ApiRequestRepository reqRepo;
    private final ApiEnvironmentRepository envRepo;
    private final ApiTestSuiteRepository suiteRepo;
    private final ApiTestSuiteRequestRepository suiteReqRepo;
    private final ApiTestExecutionRepository execRepo;
    private final AiPromptTemplateRepository promptRepo;

    public DataInitializer(TestCaseDetailRepository repo, ApiProjectRepository projectRepo,
                           UiElementRepository elementRepo, ApiCollectionRepository collRepo,
                           ApiRequestRepository reqRepo, ApiEnvironmentRepository envRepo,
                           ApiTestSuiteRepository suiteRepo, ApiTestSuiteRequestRepository suiteReqRepo,
                           ApiTestExecutionRepository execRepo,
                           AiPromptTemplateRepository promptRepo) {
        this.repo = repo;
        this.projectRepo = projectRepo;
        this.elementRepo = elementRepo;
        this.collRepo = collRepo;
        this.reqRepo = reqRepo;
        this.envRepo = envRepo;
        this.suiteRepo = suiteRepo;
        this.suiteReqRepo = suiteReqRepo;
        this.execRepo = execRepo;
        this.promptRepo = promptRepo;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedProjects();
        seedElements();
        seedApiData();
        seedPrompts();

        // Skip if already populated (data persists in MySQL)
        long existing = repo.count();
        if (existing > 200) {
            LOG.info("test_case_detail already has {} records, skipping init", existing);
            return;
        }
        LOG.info("Populating test_case_detail...");
        repo.deleteAll();

        int jsonCount = importJson();
        LOG.info("JSON import: {} entries", jsonCount);

        int scanCount = scanAll();
        LOG.info("Source scan: {} entries", scanCount);

        LOG.info("Init done — {} total records", repo.count());
    }

    private int importJson() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("test-case-details.json")) {
            if (in == null) { LOG.warn("test-case-details.json not found"); return 0; }
            String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            List<Map<String, String>> list = new Gson().fromJson(raw,
                new TypeToken<List<Map<String, String>>>(){}.getType());
            if (list == null || list.isEmpty()) return 0;
            List<TestCaseDetail> entities = new ArrayList<>();
            for (Map<String, String> m : list) {
                entities.add(new TestCaseDetail()
                    .setCaseId(s(m, "caseId")).setModule(s(m, "module"))
                    .setTitle(s(m, "title")).setCaseType(s(m, "caseType"))
                    .setSteps(s(m, "steps")).setExpected(s(m, "expected"))
                    .setApiUrl(s(m, "apiUrl")).setHttpMethod(s(m, "httpMethod"))
                    .setJavaMethod(s(m, "javaMethod")).setClassName(s(m, "className")));
            }
            repo.saveAll(entities);
            return entities.size();
        } catch (Exception e) {
            LOG.error("JSON import failed: {}", e.getMessage());
            return 0;
        }
    }

    private int scanAll() {
        Path testRoot = Paths.get(System.getProperty("user.dir"), "src/test/java/cases");
        if (!Files.exists(testRoot)) { LOG.warn("src/test/java/cases not found"); return 0; }

        // Track existing (className, javaMethod) to skip individual methods, not entire classes
        Set<String> existingMethods = new HashSet<>();
        for (TestCaseDetail d : repo.findAll()) {
            if (d.getClassName() != null && d.getJavaMethod() != null)
                existingMethods.add(d.getClassName() + "#" + d.getJavaMethod());
        }

        int totalAdded = 0;
        try (DirectoryStream<Path> modules = Files.newDirectoryStream(testRoot)) {
            for (Path moduleDir : modules) {
                if (!Files.isDirectory(moduleDir)) continue;
                String moduleName = moduleName(moduleDir.getFileName().toString());
                try (DirectoryStream<Path> files = Files.newDirectoryStream(moduleDir, "*.java")) {
                    for (Path f : files) {
                        String className = f.getFileName().toString().replace(".java", "");
                        int n = scanOneFile(f, className, moduleName, existingMethods);
                        if (n > 0) totalAdded += n;
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Scan failed: {}", e.getMessage());
        }
        return totalAdded;
    }

    private int scanOneFile(Path file, String className, String moduleName, Set<String> existingMethods) {
        try {
            String src = Files.readString(file, StandardCharsets.UTF_8);
            List<TestCaseDetail> list = new ArrayList<>();
            Matcher dm = DISPLAY_NAME.matcher(src);
            while (dm.find()) {
                String displayName = dm.group(1);
                int pos = dm.end();
                Matcher mm = METHOD_DEF.matcher(src.substring(pos, Math.min(pos + 300, src.length())));
                if (mm.find()) {
                    String methodName = mm.group(1);
                    // Skip individual method if already exists (from JSON import)
                    if (existingMethods != null && existingMethods.contains(className + "#" + methodName)) continue;
                    String caseId = extractCaseId(displayName);
                    list.add(new TestCaseDetail()
                        .setCaseId(caseId).setModule(moduleName)
                        .setTitle(displayName).setCaseType("功能")
                        .setSteps("").setExpected("")
                        .setApiUrl("").setHttpMethod("")
                        .setJavaMethod(methodName).setClassName(className));
                }
            }
            if (!list.isEmpty()) repo.saveAll(list);
            return list.size();
        } catch (Exception e) {
            LOG.debug("Skipping {}: {}", file.getFileName(), e.getMessage());
            return 0;
        }
    }

    private String extractCaseId(String displayName) {
        if (displayName == null || displayName.isBlank()) return displayName;
        int colon = displayName.indexOf(':');
        if (colon > 0) return displayName.substring(0, colon).trim();
        int paren = displayName.indexOf('(');
        if (paren > 0) return displayName.substring(0, paren).trim();
        return displayName.trim();
    }

    private static String moduleName(String dir) {
        return switch (dir) {
            case "req_folder"     -> "需求结构树";
            case "attribute"      -> "自定义属性";
            case "io"             -> "导入导出";
            case "indicator"      -> "指标管理";
            case "collaboration"  -> "协作区";
            case "user_manage"    -> "用户管理";
            case "version_trace"  -> "版本追溯";
            case "common"         -> "通用测试";
            default               -> dir;
        };
    }

    private void seedProjects() {
        // Create default project if none exists
        if (projectRepo.count() == 0) {
            ApiProject p = new ApiProject("需求管理系统", "需求管理、指标管理、导入导出等接口集合");
            p.setProjectType("HTTP");
            p.setStatus("active");
            projectRepo.save(p);
            LOG.info("Default API project created");
        }
    }

    private void seedElements() {
        // No default seed data — user creates through UI
    }

    // ═══════════════════ Seed API Management Data ═══════════════════
    private static final String BASE = "https://192.168.6.171:8088/dev-api";
    private static final String MOE_BASE = "https://192.168.6.171:8088/api-api";

    private void seedPrompts() {
        if (promptRepo.count() > 0) return;
        AiPromptTemplate p1 = new AiPromptTemplate();
        p1.setName("严格模式"); p1.setType("system");
        p1.setContent("你是一个严谨的 Web UI 自动化测试引擎。将操作指令、DOM属性、截图转换为 Playwright 可执行的严格 JSON。\n\n【全局约束】\n1. JSON生成机器，回答以{开头}结尾。禁止思考过程。\n2. value用指令真实数据，无输入则\"\"。\n3. action: click / fill / right_click / type。\n4. 选择器优先级: button:has-text > span:has-text > [placeholder] > [type]。\n5. 禁止动态ID，禁止[name='admin']类猜测。");
        promptRepo.save(p1);

        AiPromptTemplate p2 = new AiPromptTemplate();
        p2.setName("宽松模式"); p2.setType("system");
        p2.setContent("你是Web自动化助手。观察截图和DOM，用合理的CSS选择器完成操作。返回{\"action\":\"click|fill|right_click\",\"selector\":\"...\",\"value\":\"...\"}。如果找不到精确匹配，选最接近的可见元素。");
        promptRepo.save(p2);

        AiPromptTemplate p3 = new AiPromptTemplate();
        p3.setName("登录指令"); p3.setType("user");
        p3.setContent("当前页面是登录页。请使用测试账号登录：用户名 admin，密码 Aa123456。登录后继续执行后续任务。");
        promptRepo.save(p3);
        LOG.info("Seeded 3 default prompt templates");
    }

    private void seedApiData() {
        if (collRepo.count() > 0) {
            LOG.info("Clearing old API data for re-seed...");
            suiteReqRepo.deleteAll(); execRepo.deleteAll(); suiteRepo.deleteAll();
            reqRepo.deleteAll(); collRepo.deleteAll(); envRepo.deleteAll();
        }

        ApiProject project = projectRepo.findAll().stream().findFirst().orElse(null);
        if (project == null) { LOG.warn("No project found, skip API seeding"); return; }
        Long projectId = project.getId();

        LOG.info("Seeding API management data (by test file)...");
        int total = 0;

        // Each module matches a manual test file under src/test/java/cases/manual/
        total += seedModule(projectId, "需求规格管理",       "ReqSpecManualTest",       reqSpecApis());
        total += seedModule(projectId, "需求条目编辑",       "ReqItemEditManualTest",   reqItemApis());
        total += seedModule(projectId, "自定义属性",         "AttributeManualTest",     attributeApis());
        total += seedModule(projectId, "导入导出",           "ImportExportManualTest",  ioApis());
        total += seedModule(projectId, "指标管理",           "IndicatorManualTest",     indicatorApis());
        total += seedModule(projectId, "协作区管理",         "CooperationManualTest",   collaborationApis());
        total += seedModule(projectId, "权限管理",           "PermissionManualTest",    permissionApis());
        total += seedModule(projectId, "流程定义",           "FlowDefineManualTest",    flowDefineApis());
        total += seedModule(projectId, "工作流管理",         "WorkflowManualTest",      workflowApis());

        // Seed default environments & test suites
        seedEnvironments(projectId);
        seedTestSuites(projectId);

        LOG.info("API seeding complete — {} requests across 9 modules", total);
    }

    private int seedModule(Long projectId, String name, String tag, List<String[]> apis) {
        ApiCollection coll = new ApiCollection(name, projectId);
        coll.setDescription("来源于测试文件: " + tag);
        coll.setSortOrder(0);
        coll = collRepo.save(coll);

        int count = 0;
        for (String[] api : apis) {
            ApiRequest req = new ApiRequest();
            req.setName(api[0]);
            req.setDescription(api.length > 4 ? api[4] : "");
            req.setMethod(api[1]);
            req.setUrl(api[2]);
            req.setBodyType("json");
            if (api.length > 3 && api[3] != null && !api[3].isEmpty()) {
                req.setBody(api[3]);
            }
            req.setCollectionId(coll.getId());
            req.setSortOrder(count);
            reqRepo.save(req);
            count++;
        }
        LOG.info("  {} — {} 个接口", name, count);
        return count;
    }

    private void seedEnvironments(Long projectId) {
        if (envRepo.count() > 0) return;
        ApiEnvironment dev = new ApiEnvironment();
        dev.setName("开发环境"); dev.setScope("global"); dev.setProjectId(projectId);
        dev.setBaseUrl("https://192.168.6.171:8088");
        dev.setVariables("{\"host\":\"192.168.6.171:8088\",\"basePath\":\"/dev-api\"}");
        dev.setDescription("开发测试环境");
        envRepo.save(dev);

        ApiEnvironment test = new ApiEnvironment();
        test.setName("测试环境"); test.setScope("global"); test.setProjectId(projectId);
        test.setBaseUrl("https://192.168.6.171:8088");
        test.setVariables("{\"host\":\"192.168.6.171:8088\",\"basePath\":\"/dev-api\"}");
        test.setDescription("测试环境");
        envRepo.save(test);

        ApiEnvironment prod = new ApiEnvironment();
        prod.setName("生产环境"); prod.setScope("global"); prod.setProjectId(projectId);
        prod.setBaseUrl("https://192.168.6.171:8088");
        prod.setVariables("{\"host\":\"192.168.6.171:8088\",\"basePath\":\"/dev-api\"}");
        prod.setDescription("生产环境");
        envRepo.save(prod);
        LOG.info("  Environments — 3 created");
    }

    private void seedTestSuites(Long projectId) {
        suiteReqRepo.deleteAll();
        suiteRepo.deleteAll();

        // Group requests by collection
        List<ApiCollection> collections = collRepo.findByProjectIdOrderBySortOrder(projectId);
        if (collections.isEmpty()) return;

        for (ApiCollection coll : collections) {
            List<ApiRequest> reqs = reqRepo.findByCollectionIdOrderBySortOrder(coll.getId());
            if (reqs.isEmpty()) continue;

            // Create a suite for each collection
            ApiTestSuite suite = new ApiTestSuite();
            suite.setName(coll.getName() + " - 全流程测试");
            suite.setDescription("来源于 " + coll.getDescription() + " | 包含" + reqs.size() + "个接口");
            suite.setProjectId(projectId);
            suite = suiteRepo.save(suite);

            int order = 0;
            for (ApiRequest req : reqs) {
                ApiTestSuiteRequest sr = new ApiTestSuiteRequest();
                sr.setTestSuiteId(suite.getId());
                sr.setRequestId(req.getId());
                sr.setOrderNo(++order);
                sr.setEnabled(true);
                sr.setAssertions("[{\"type\":\"status_code\",\"value\":200}]");
                suiteReqRepo.save(sr);
            }
            LOG.info("  Suite '{}' — {} 个接口", suite.getName(), order);
        }
        LOG.info("  Test Suites — {} created", collections.size());
    }

    // ══════════ ReqSpecManualTest — 需求规格管理 ══════════

    private List<String[]> reqSpecApis() {
        return List.of(
            ap("创建需求规格",          "POST", BASE+"/erm/add/addReqSpe",
                "{\"projectId\":\"...\",\"parentId\":\"...\",\"parentType\":\"reqSpeFolder\",\"title\":\"NewDoc\"}"),
            ap("重命名需求规格",        "POST", BASE+"/erm/update/updateReqSpeInfo",
                "{\"projectId\":\"...\",\"objectId\":\"...\",\"title\":\"NewName\"}"),
            ap("编辑需求规格描述",      "POST", BASE+"/erm/update/updateReqSpeInfo",
                "{\"projectId\":\"...\",\"objectId\":\"...\",\"description\":\"new desc\"}"),
            ap("修改编码前缀",          "POST", BASE+"/erm/update/updateReqSpeInfo",
                "{\"projectId\":\"...\",\"objectId\":\"...\",\"codingRule\":\"ab\"}"),
            ap("删除需求规格",          "POST", BASE+"/erm/del/delReqSpe",
                "{\"objectId\":\"...\",\"parentId\":\"...\",\"parentType\":\"reqSpeFolder\"}"),
            ap("恢复需求规格",          "POST", BASE+"/erm/recover/recoverReqSpe",
                "{\"objectId\":\"...\",\"parentId\":\"...\",\"parentType\":\"reqSpeFolder\"}"),
            ap("永久清理需求规格",      "POST", BASE+"/erm/clean/cleanReqSpe",
                "{\"objectId\":\"...\",\"parentId\":\"...\"}"),
            ap("查询需求规格信息",      "POST", BASE+"/erm/search/searchReqSpeInfo",
                "{\"objectId\":\"...\"}"),
            ap("上传需求文档附件",      "POST", BASE+"/erm/upload/reqDocUpload",
                "multipart: file"),
            ap("删除需求附件",          "POST", BASE+"/erm/reqDocDelete",
                "{\"objectId\":\"...\"}"),
            ap("查询需求规格列表",      "POST", BASE+"/erm/search/searchReqSpeBaseLineList",
                "{\"projectId\":\"...\"}"),
            ap("切换需求规格状态(工作中/冻结)","POST",BASE+"/erm/update/updateReqSpeState",
                "{\"objectId\":\"...\",\"current\":\"Inwork\"}")
        );
    }

    // ══════════ ReqItemEditManualTest — 需求条目编辑 ══════════

    private List<String[]> reqItemApis() {
        return List.of(
            ap("新增需求条目",          "POST", BASE+"/erm/add/addReq",
                "{\"projectId\":\"...\",\"parentId\":\"...\",\"parentReqSpeId\":\"...\"}"),
            ap("新增需求条目(Raw)",     "POST", BASE+"/erm/add/addReq",
                "{\"projectId\":\"...\",\"parentId\":\"...\",\"parentReqSpeId\":\"...\",\"beforeLinkOrderNo\":\"\"}"),
            ap("删除需求条目",          "POST", BASE+"/erm/del/delReqObjectList",
                "{\"objectId\":\"...\"}"),
            ap("恢复需求条目",          "POST", BASE+"/erm/recover/recoverReq",
                "{\"objectId\":\"...\"}"),
            ap("永久清除需求条目",      "POST", BASE+"/erm/clean/cleanReq",
                "{\"objectId\":\"...\",\"reqSpecId\":\"...\"}"),
            ap("查询子需求条目列表",    "POST", BASE+"/erm/search/searchChildReqInfoByReqSpeId",
                "{\"objectId\":\"...\"}"),
            ap("更新需求条目列表",      "POST", BASE+"/erm/update/updateReqList",
                "{\"reqSpeId\":\"...\",\"reqList\":[...]}"),
            ap("复制需求条目",          "POST", BASE+"/erm/update/copyReq",
                "{\"parentReqSpeId\":\"...\",\"parentId\":\"...\",\"objectId\":\"...\"}"),
            ap("移动需求条目位置",      "POST", BASE+"/erm/update/changeReqPosition",
                "{\"parentId\":\"...\",\"objectId\":\"...\"}")
        );
    }

    // ══════════ FlowDefineManualTest — 流程定义 ══════════

    private List<String[]> flowDefineApis() {
        return List.of(
            ap("创建文件夹",            "POST", BASE+"/erm/add/addReqSpeFolder",
                "{\"projectId\":\"...\",\"parentId\":\"...\",\"parentType\":\"project\",\"title\":\"NewFolder\"}"),
            ap("重命名文件夹",          "POST", BASE+"/erm/update/updateReqSpeFolderInfo",
                "{\"projectId\":\"...\",\"objectId\":\"...\",\"parentId\":\"...\",\"title\":\"NewName\"}"),
            ap("删除文件夹",            "POST", BASE+"/erm/del/delReqSpeFolder",
                "{\"objectId\":\"...\",\"parentId\":\"...\",\"parentType\":\"reqSpeFolder\"}"),
            ap("恢复文件夹",            "POST", BASE+"/erm/recover/recoverReqSpeFolder",
                "{\"objectId\":\"...\",\"parentId\":\"...\"}"),
            ap("永久清理文件夹",        "POST", BASE+"/erm/clean/cleanReqSpeFolder",
                "{\"objectId\":\"...\"}"),
            ap("查询文件夹子节点",      "POST", BASE+"/erm/search/searchReqFolderChildrenList",
                "{\"objectId\":\"...\"}"),
            ap("获取目录结构树",        "POST", BASE+"/erm/search/searchReqFolderStructureTree",
                "{\"projectId\":\"...\",\"parentId\":\"...\"}"),
            ap("查询项目子节点列表",    "POST", BASE+"/erm/search/searchChildrenListFromProject",
                "{\"projectId\":\"...\"}")
        );
    }

    // ══════════ WorkflowManualTest — 工作流管理 ══════════

    private List<String[]> workflowApis() {
        return List.of(
            ap("切换需求规格状态",      "POST", BASE+"/erm/update/updateReqSpeState",
                "{\"objectId\":\"...\",\"current\":\"Frozen\"}"),
            ap("查询版本列表",          "GET",  BASE+"/erm/search/getReqSpeVersionList?objectId=...", ""),
            ap("查询需求访问权限",      "GET",  BASE+"/erm/get/getReqAccess?objectId=...", ""),
            ap("校验打开模式",          "GET",  BASE+"/erm/get/checkOpenMode?masterId=...&operateType=edit", ""),
            ap("获取打开模式",          "GET",  BASE+"/erm/get/getOpenModel?objectId=...&hasAccess=true", ""),
            ap("解锁需求规格",          "POST", BASE+"/erm/lockAndUnLockReq",
                "{\"objectId\":\"...\",\"lockMode\":\"unlock\"}")
        );
    }

    private List<String[]> attributeApis() {
        return List.of(
            ap("新增自定义属性",        "POST", BASE+"/erm/customAttribute/addCustomAttribute",
                "{\"nameEn\":\"...\",\"name\":\"...\",\"type\":\"整型\",\"projectId\":\"...\",\"businessDomain\":\"需求管理\",\"objectType\":\"req\"}"),
            ap("校验属性名",            "GET",  BASE+"/erm/customAttribute/checkAttribute?projectId=...&nameEn=...&name=...", ""),
            ap("查询自定义属性列表",    "GET",  BASE+"/erm/customAttribute/selectCustomAttributeList?projectId=...", ""),
            ap("修改自定义属性",        "POST", BASE+"/erm/customAttribute/updateCustomAttribute",
                "{\"id\":\"...\",\"nameEn\":\"...\",\"name\":\"...\",\"type\":\"整型\",\"projectId\":\"...\"}"),
            ap("删除自定义属性",        "POST", BASE+"/erm/customAttribute/deleteCustomAttributes", "[\"attrId1\",\"attrId2\"]"),
            ap("发布自定义属性",        "POST", BASE+"/erm/customAttribute/publishCustomAttributes",
                "{\"projectId\":\"...\",\"attributeIds\":[\"...\"]}"),
            ap("批量发布自定义属性",    "POST", BASE+"/erm/customAttribute/publishCustomAttributes",
                "{\"projectId\":\"...\",\"attributeIds\":[\"id1\",\"id2\",\"id3\"]}"),
            ap("搜索自定义属性",        "GET",  BASE+"/erm/customAttribute/searchAttributes?projectId=...&businessDomain=...", ""),
            ap("设置属性值(updateReqSpeInfo)","POST",BASE+"/erm/update/updateReqSpeInfo",
                "{\"projectId\":\"...\",\"objectId\":\"...\",\"customAttribute\":[{\"attrId\":\"...\",\"value\":\"...\"}]}")
        );
    }

    private List<String[]> ioApis() {
        return List.of(
            ap("导出Excel需求规格",     "GET",  BASE+"/erm/exportExcelReqSpecification?objectId=...&templateType=one", ""),
            ap("导出Word需求规格",      "GET",  BASE+"/erm/exportWordReqSpecification?objectId=...&templateType=one", ""),
            ap("下载导入模板",          "GET",  BASE+"/erm/downloadReqImportTemplate?templateType=one", ""),
            ap("导入Excel需求规格",     "POST", BASE+"/erm/import/importReqSpecification",
                "{\"projectId\":\"...\",\"parentId\":\"...\",\"reqSpecName\":\"...\",\"data\":[...]}"),
            ap("导入Word需求规格",      "POST", BASE+"/erm/import/importReqSpecDocx", "multipart: file,parentId,projectId,reqSpecName"),
            ap("导出ReqIf",            "POST", BASE+"/erm/reqIf/post/exportReqIf",
                "{\"reqSpeBranchIds\":[\"...\"],\"projectId\":\"...\"}"),
            ap("获取AtoZ参数",         "GET",  BASE+"/erm/reqIf/get/getAllAtozParam?projectId=...", ""),
            ap("获取Doors参数(ReqIf)", "POST", BASE+"/erm/reqIf/get/getAllDoorsParam", "multipart: file"),
            ap("导入ReqIf文件",        "POST", BASE+"/erm/reqIf/add/importReqIfFile", "multipart: file,parentId,projectId,mappingAttrJson"),
            ap("插入ReqIf模板",        "POST", BASE+"/erm/attr/post/insertTemplate",
                "{\"templateName\":\"...\",\"projectId\":\"...\",\"attrTemplateInfoRspVoList\":[...]}"),
            ap("获取模板名称列表",      "GET",  BASE+"/erm/attr/get/getTemplateNames?projectId=...", ""),
            ap("获取导入属性配置",      "GET",  BASE+"/erm/import/getAttributes", "")
        );
    }

    private List<String[]> indicatorApis() {
        return List.of(
            ap("新增逻辑结构",              "POST", MOE_BASE+"/moe/add/addLogicStructure",
                "{\"name\":\"...\",\"description\":\"...\",\"projectId\":\"...\"}"),
            ap("查询逻辑结构列表",          "POST", MOE_BASE+"/moe/search/searchLogicStructureList", "{\"projectId\":\"...\"}"),
            ap("获取逻辑结构信息",          "POST", MOE_BASE+"/moe/get/getLogicStructureInfo", "{\"objectId\":\"...\"}"),
            ap("修改逻辑结构",              "POST", MOE_BASE+"/moe/update/updateLogicStructure",
                "{\"objectId\":\"...\",\"name\":\"...\"}"),
            ap("新增逻辑节点",              "POST", MOE_BASE+"/moe/add/addLogic",
                "{\"parentId\":\"...\",\"name\":\"...\",\"type\":\"system\",\"logicStructureId\":\"...\"}"),
            ap("查询逻辑节点列表",          "POST", MOE_BASE+"/moe/search/searchLogicList", "{\"objectId\":\"...\"}"),
            ap("获取逻辑节点信息",          "POST", MOE_BASE+"/moe/get/getLogicInfo", "{\"objectId\":\"...\"}"),
            ap("修改逻辑节点",              "POST", MOE_BASE+"/moe/update/updateLogic",
                "{\"objectId\":\"...\",\"name\":\"...\",\"logicStructureId\":\"...\"}"),
            ap("删除逻辑节点",              "POST", MOE_BASE+"/moe/delete/deleteLogic",
                "{\"objectId\":\"...\",\"logicStructureId\":\"...\"}"),
            ap("删除逻辑结构",              "POST", MOE_BASE+"/moe/delete/deleteLogicStructure", "{\"objectId\":\"...\"}"),
            ap("复制逻辑节点",              "POST", MOE_BASE+"/moe/add/addLogic",
                "{\"parentId\":\"...\",\"name\":\"...copy\",\"addMark\":true,...}"),
            ap("更新指标当前值(current)",    "POST", MOE_BASE+"/moe/update/updateCurrent",
                "{\"objectId\":\"...\",\"after\":\"...\"}"),
            ap("新增逻辑结构参数",          "POST", MOE_BASE+"/moe/add/addLogicStructureParameter",
                "{\"parentId\":\"...\",\"name\":\"...\",\"logicStructureId\":\"...\"}"),
            ap("修改逻辑结构参数",          "POST", MOE_BASE+"/moe/update/updateLogicStructureParameter",
                "{\"objectId\":\"...\",\"name\":\"...\",\"logicStructureId\":\"...\"}"),
            ap("导出架构Excel",            "POST", MOE_BASE+"/moe/download/downloadExcelLogic", "{\"objectId\":\"...\"}"),
            ap("导出指标Excel",            "POST", MOE_BASE+"/moe/download/downloadExcelAIndex", "{\"objectId\":\"...\"}"),
            ap("下载指标模板Excel",        "POST", MOE_BASE+"/moe/download/downloadMetricTemplateExcel", "{\"type\":\"...\"}")
        );
    }

    private List<String[]> collaborationApis() {
        return List.of(
            ap("新增合作区",              "POST", BASE+"/common/add/addProject",
                "{\"objectId\":\"\",\"title\":\"...\",\"name\":\"...\"}"),
            ap("修改合作区信息",          "POST", BASE+"/common/update/updateProjectInfo",
                "{\"objectId\":\"...\",\"title\":\"...\",\"name\":\"...\"}"),
            ap("删除合作区",              "POST", BASE+"/common/delete/delProject", "[{\"objectId\":\"...\"}]"),
            ap("查询合作区列表",          "GET",  BASE+"/common/search/searchProjectList?title=...", ""),
            ap("分配合作区人员",          "POST", BASE+"/common/update/assignProjectPersonList",
                "{\"objectId\":\"...\",\"data\":[{\"objectId\":\"...\"}]}"),
            ap("移除合作区人员",          "POST", BASE+"/common/update/removeProjectPersonList",
                "{\"objectId\":\"...\",\"data\":[{\"objectId\":\"...\"}]}"),
            ap("查询项目人员列表",        "GET",  BASE+"/common/search/searchProjectPersonList?objectId=...", "")
        );
    }

    private List<String[]> permissionApis() {
        return List.of(
            ap("设置需求写权限",          "POST", BASE+"/erm/update/updateReqSpeWritePermission",
                "{\"objectId\":\"...\",\"personData\":[{\"objectId\":\"...\",\"userName\":\"...\"}]}"),
            ap("校验打开模式",            "GET",  BASE+"/erm/get/checkOpenMode?masterId=...&operateType=edit", ""),
            ap("查询需求访问权限",        "GET",  BASE+"/erm/get/getReqAccess?objectId=...", ""),
            ap("获取打开模式详情",        "GET",  BASE+"/erm/get/getOpenModel?objectId=...&hasAccess=true", ""),
            ap("解锁操作",                "POST", BASE+"/erm/lockAndUnLockReq",
                "{\"objectId\":\"...\",\"lockMode\":\"unlock\"}"),
            ap("查询版本列表",            "GET",  BASE+"/erm/search/getReqSpeVersionList?objectId=...", "")
        );
    }


    private static String[] ap(String name, String method, String url, String body) {
        return new String[]{name, method, url, body, ""};
    }

    private static String[] ap(String name, String method, String url, String body, String desc) {
        return new String[]{name, method, url, body, desc};
    }

    private static String s(Map<String, String> m, String k) {
        String v = m.get(k);
        return v != null ? v : "";
    }
}
