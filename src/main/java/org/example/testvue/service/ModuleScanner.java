package org.example.testvue.service;

import org.example.testvue.dto.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * Scans src/test/java/cases for test classes and builds the module tree
 * that the frontend renders in its test-selection sidebar.
 */
@Component
public class ModuleScanner {

    private static final Logger LOG = LoggerFactory.getLogger(ModuleScanner.class);

    /** Class name → [tag, label, desc, icon, color] */
    private static final Map<String, String[]> CLASS_MODULE = new LinkedHashMap<>();
    static {
        CLASS_MODULE.put("FolderManualTest",      new String[]{"FolderModule",      "文件夹操作",   "新建/重命名/删除/描述/刷新",          "📁", "#f59e0b"});
        CLASS_MODULE.put("ImportExportManualTest",new String[]{"IOModule",          "导入导出",     "Excel/Word/ReqIF导入导出",            "📤", "#10b981"});
        CLASS_MODULE.put("ReqSpecManualTest",     new String[]{"ReqSpecModule",     "需求规格",     "CRUD/属性/文件/权限/模式/视图",        "📋", "#3b82f6"});
        CLASS_MODULE.put("AttributeManualTest",   new String[]{"AttributeModule",   "自定义属性",   "新建/校验/发布/删除/搜索",             "🏷️", "#8b5cf6"});
        CLASS_MODULE.put("ReqItemEditManualTest", new String[]{"ReqItemEditModule", "需求条目编辑", "富文本/复制/剪切/加锁解锁",            "✏️", "#ec4899"});
        CLASS_MODULE.put("FlowDefineManualTest",  new String[]{"FlowModule",        "流程定义",     "流程定义(TODD)",                       "🔀", "#94a3b8"});
        CLASS_MODULE.put("WorkflowManualTest",    new String[]{"WorkflowModule",    "需求审签",     "草稿/审批/更改(TODD)",                 "✅", "#f97316"});
        CLASS_MODULE.put("TraceManualTest",       new String[]{"TraceModule",       "需求追溯",     "追溯(TODD)",                           "🔍", "#6366f1"});
        CLASS_MODULE.put("CooperationManualTest", new String[]{"CooperationModule", "合作区管理",   "添加/修改/删除/分配人员",              "🤝", "#14b8a6"});
        CLASS_MODULE.put("IndicatorManualTest",   new String[]{"IndicatorModule",   "指标管理",     "逻辑架构/节点/指标参数/导入导出",       "📊", "#eab308"});
        CLASS_MODULE.put("UiOnlyManualTest",      new String[]{"UiOnlyModule",      "纯UI记录",     "仅UI操作记录",                         "🖥️", "#64748b"});
        CLASS_MODULE.put("PermissionManualTest", new String[]{"PermissionModule",  "权限验证",     "跨用户写入权限校验",                   "🔐", "#ef4444"});
        CLASS_MODULE.put("ModelManualTest",     new String[]{"ModelModule",       "模型管理",     "模型管理(待开发)",                     "🔷", "#3b82f6"});
        CLASS_MODULE.put("FuncManualTest",      new String[]{"FuncModule",        "功能管理",     "功能管理(待开发)",                     "🔶", "#8b5cf6"});
        CLASS_MODULE.put("ArchManualTest",      new String[]{"ArchModule",        "架构管理",     "架构管理(待开发)",                     "🏛️", "#14b8a6"});
    }

    private final Path casesDir;

    public ModuleScanner() {
        this.casesDir = Paths.get(System.getProperty("user.dir"), "src", "test", "java", "cases");
    }

    public ModuleScanner(Path casesDir) { this.casesDir = casesDir; }

    public List<ModuleDto> scan() {
        if (!Files.isDirectory(casesDir)) {
            LOG.warn("Test cases directory not found: {}", casesDir);
            return Collections.emptyList();
        }
        Map<String, ModuleDto> modules = new LinkedHashMap<>();
        Path manualDir = casesDir.resolve("manual");
        if (!Files.isDirectory(manualDir)) return Collections.emptyList();

        try (DirectoryStream<Path> files = Files.newDirectoryStream(manualDir, "*.java")) {
            for (Path file : files) {
                String className = file.getFileName().toString().replace(".java", "");
                String[] meta = CLASS_MODULE.get(className);
                if (meta == null) continue;

                String tag = meta[0];
                ModuleDto mod = modules.computeIfAbsent(tag, k -> {
                    ModuleDto m = new ModuleDto();
                    m.tag = tag; m.label = meta[1]; m.icon = meta[3]; m.color = meta[4]; m.classes = new ArrayList<>();
                    return m;
                });
                ClassDto cls = parseClass(file);
                if (cls != null) {
                    cls.desc = meta[2] + " · " + (cls.desc != null ? cls.desc : cls.name);
                    mod.classes.add(cls);
                }
            }
        } catch (IOException e) {
            LOG.error("Failed to scan manual directory", e);
        }
        List<ModuleDto> result = new ArrayList<>();
        // 需求规格: 文件夹操作, 导入导出, 需求规格, 编辑需求条目, 需求追溯
        addParent(result, modules, "ReqSpecParent", "需求规格", "📋", "#3b82f6",
            "FolderModule", "IOModule", "ReqSpecModule", "ReqItemEditModule", "TraceModule", "WorkflowModule");
        // 系统管理: 自定义属性, 合作区管理, 指标管理
        addParent(result, modules, "SystemParent", "系统管理", "⚙️", "#64748b",
            "AttributeModule", "CooperationModule", "IndicatorModule");
        // 架构管理: 模型管理, 功能管理, 架构管理
        addParent(result, modules, "ArchParent", "架构管理", "🏗️", "#f59e0b",
            "ModelModule", "FuncModule", "ArchModule");
        result.addAll(modules.values());
        return result;
    }

    private void addPlaceholderParent(List<ModuleDto> result, String tag, String label,
                                       String icon, String color, String[]... children) {
        ModuleDto parent = new ModuleDto();
        parent.tag = tag; parent.label = label; parent.icon = icon; parent.color = color;
        parent.children = new ArrayList<>();
        for (String[] c : children) {
            ModuleDto child = new ModuleDto();
            child.tag = c[0]; child.label = c[1]; child.icon = c[2]; child.color = c[3];
            child.classes = new ArrayList<>();
            parent.children.add(child);
        }
        result.add(parent);
    }

    private void addParent(List<ModuleDto> result, Map<String, ModuleDto> modules,
                           String tag, String label, String icon, String color, String... childTags) {
        ModuleDto parent = null;
        for (String ct : childTags) {
            ModuleDto child = modules.remove(ct);
            if (child != null) {
                if (parent == null) {
                    parent = new ModuleDto();
                    parent.tag = tag; parent.label = label; parent.icon = icon; parent.color = color;
                    parent.children = new ArrayList<>();
                }
                parent.children.add(child);
            }
        }
        if (parent != null) result.add(parent);
    }

    private static final Pattern DISPLAY_NAME_RE = Pattern.compile("@DisplayName\\(\"([^\"]+)\"\\)");
    private static final Pattern TEST_METHOD_RE = Pattern.compile("^\\s*(?:public\\s+)?void\\s+(\\w+)\\s*\\(");

    private ClassDto parseClass(Path file) {
        try {
            String content = Files.readString(file);
            String fileName = file.getFileName().toString();
            String className = fileName.substring(0, fileName.length() - 5);

            boolean isUI = content.contains("com.microsoft.playwright")
                        || content.contains("org.openqa.selenium");

            ClassDto cls = new ClassDto();
            cls.name = className;
            cls.type = isUI ? "ui" : "api";
            cls.methods = new ArrayList<>();
            List<String> methodNames = new ArrayList<>();

            String[] lines = content.split("\\r?\\n");
            String pendingDisplay = null;
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();

                Matcher dm = DISPLAY_NAME_RE.matcher(line);
                if (dm.find()) {
                    pendingDisplay = dm.group(1).trim();
                }

                if (line.equals("@Test")) {
                    String methodName = null;
                    for (int j = i + 1; j < Math.min(i + 4, lines.length); j++) {
                        Matcher vm = TEST_METHOD_RE.matcher(lines[j]);
                        if (vm.find()) {
                            methodName = vm.group(1).trim();
                            break;
                        }
                    }

                    if (methodName != null) {
                        MethodDto m = new MethodDto();
                        m.name = pendingDisplay != null ? pendingDisplay : methodName;
                        String nl = m.name.toLowerCase();
                        if (nl.contains("负向") || nl.contains("异常") || nl.contains("错误")
                            || nl.contains("失败") || nl.contains("空") || nl.contains("重复")
                            || nl.contains("越界") || nl.contains("安全") || nl.contains("不足")
                            || nl.contains("不存在") || nl.contains("无权")) {
                            m.type = "bad";
                        }
                        cls.methods.add(m);
                        methodNames.add(m.name);
                    }
                    pendingDisplay = null;
                }
            }

            cls.caseCount = cls.methods.size();
            if (cls.methods.isEmpty()) {
                cls.methods = null;
            }
            cls.desc = buildDesc(methodNames, isUI);
            return cls;
        } catch (IOException e) {
            LOG.debug("Failed to read {}", file, e);
            return null;
        }
    }

    private String buildDesc(List<String> methodNames, boolean isUI) {
        if (methodNames.isEmpty()) return null;
        Set<String> hints = new LinkedHashSet<>();
        for (String name : methodNames) {
            String cleaned = name.replaceFirst("^[A-Z0-9_-]+:\\s*", "")
                                 .replaceAll("[（(]正[向向]*[)）]|[（(]负[向向]*[)）]", "")
                                 .replaceAll("\\s*\\(.*\\)$", "")
                                 .trim();
            if (cleaned.length() > 1 && cleaned.length() < 20) hints.add(cleaned);
            if (hints.size() >= 6) break;
        }
        if (hints.isEmpty()) return null;
        String prefix = isUI ? "UI 测试：" : "API 测试：";
        return prefix + String.join("、", hints) + " … 等 " + methodNames.size() + " 个用例";
    }
}
