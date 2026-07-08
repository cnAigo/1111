package org.example.testvue.controller;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.AriaRole;
import org.example.testvue.entity.UiElement;
import org.example.testvue.entity.UiElementGroup;
import org.example.testvue.entity.UiProject;
import org.example.testvue.repository.UiElementRepository;
import org.example.testvue.repository.UiElementGroupRepository;
import org.example.testvue.repository.UiProjectRepository;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/ui-automation")
public class UiElementController {

    private final UiElementRepository repo;
    private final UiElementGroupRepository groupRepo;
    private final UiProjectRepository projectRepo;

    public UiElementController(UiElementRepository repo, UiElementGroupRepository groupRepo,
                               UiProjectRepository projectRepo) {
        this.repo = repo; this.groupRepo = groupRepo;
        this.projectRepo = projectRepo;
    }

    @GetMapping("/elements/")
    public Map<String, Object> list(@RequestParam(required = false) Long project) {
        List<UiElement> all = (project != null) ? repo.findByProjectId(project) : repo.findAll();
        List<Map<String, Object>> results = new ArrayList<>();
        for (UiElement e : all) results.add(toMap(e));
        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("count", results.size()); resp.put("results", results);
        return resp;
    }

    @GetMapping("/elements/{id}/")
    public Map<String, Object> detail(@PathVariable Long id) {
        return repo.findById(id).map(this::toMap).orElse(Map.of("error", "not found"));
    }

    @GetMapping("/elements/tree/")
    public List<Map<String, Object>> tree(@RequestParam(required = false) Long project) {
        List<UiElement> all = (project != null) ? repo.findByProjectId(project) : repo.findAll();
        List<UiElementGroup> groups = (project != null) ? groupRepo.findByProjectId(project) : groupRepo.findAll();
        Map<Long, List<Map<String, Object>>> pageChildren = new LinkedHashMap<>();
        Map<Long, String> pageNames = new LinkedHashMap<>();
        for (UiElementGroup g : groups) {
            pageChildren.put(g.getId(), new ArrayList<>());
            pageNames.put(g.getId(), g.getName());
        }

        // Match elements to groups by group_id, fallback by page name
        Set<Long> assigned = new HashSet<>();
        for (UiElement e : all) {
            Long gid = e.getGroupId();
            if (gid != null && pageChildren.containsKey(gid)) {
                pageChildren.get(gid).add(elementNode(e));
                assigned.add(e.getId());
            }
        }
        // Match remaining by page name to group name
        List<Map<String, Object>> unassigned = new ArrayList<>();
        for (UiElement e : all) {
            if (assigned.contains(e.getId())) continue;
            String pn = e.getPage();
            boolean matched = false;
            for (var pg : pageNames.entrySet()) {
                if (pn != null && pn.equals(pg.getValue())) {
                    pageChildren.get(pg.getKey()).add(elementNode(e));
                    matched = true; break;
                }
            }
            if (!matched) unassigned.add(elementNode(e));
        }

        List<Map<String, Object>> tree = new ArrayList<>();
        for (UiElementGroup g : groups) {
            // Build subtree for children groups
            List<Map<String, Object>> children = buildSubTree(g.getId(), groups, pageChildren);
            children.addAll(pageChildren.getOrDefault(g.getId(), new ArrayList<>()));
            Map<String, Object> pn = new LinkedHashMap<>();
            pn.put("id", g.getId()); pn.put("name", g.getName());
            pn.put("type", "page"); pn.put("children", children);
            tree.add(pn);
        }
        // 未分配的元素统一放到最后一个页面下，不再显示"未关联页面"节点
        if (!unassigned.isEmpty() && !groups.isEmpty()) {
            pageChildren.get(groups.get(groups.size() - 1).getId()).addAll(unassigned);
        }
        return tree;
    }

    private List<Map<String, Object>> buildSubTree(Long parentId, List<UiElementGroup> all, Map<Long, List<Map<String, Object>>> pc) {
        List<Map<String, Object>> children = new ArrayList<>();
        for (UiElementGroup g : all) {
            if (parentId.equals(g.getParentId())) {
                List<Map<String, Object>> subKids = buildSubTree(g.getId(), all, pc);
                subKids.addAll(pc.getOrDefault(g.getId(), new ArrayList<>()));
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", g.getId()); node.put("name", g.getName());
                node.put("type", "page"); node.put("children", subKids);
                children.add(node);
            }
        }
        return children;
    }

    @PostMapping("/elements/")
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {
        UiElement e = new UiElement();
        updateFromBody(e, body);
        e.setCreatedAt(LocalDateTime.now());
        e.setUpdatedAt(LocalDateTime.now());
        e = repo.save(e);
        return toMap(e);
    }

    @PatchMapping("/elements/{id}/")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return repo.findById(id).map(e -> {
            updateFromBody(e, body);
            e.setUpdatedAt(LocalDateTime.now());
            repo.save(e);
            return toMap(e);
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/elements/{id}/")
    public Map<String, Object> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    @PostMapping("/elements/{id}/validate_locator/")
    public Map<String, Object> validateLocator(@PathVariable Long id) {
        UiElement el = repo.findById(id).orElse(null);
        if (el == null) return Map.of("is_valid", false, "validation_message", "元素不存在");

        String baseUrl = getBaseUrl(el.getProjectId());
        if (baseUrl.isBlank()) return Map.of("is_valid", false, "validation_message", "关联项目未配置baseUrl");

        try (Playwright pw = Playwright.create()) {
            Browser br = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = br.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)).newPage();
            page.navigate(baseUrl);
            page.waitForLoadState();

            String strategy = el.getLocatorStrategy();
            String value = el.getLocatorValue();
            long start = System.currentTimeMillis();

            try {
                Locator loc = buildLocator(page, strategy, value);
                boolean found = loc.first().isVisible();
                long ms = System.currentTimeMillis() - start;
                br.close();
                if (found) {
                    return Map.of("is_valid", true, "validation_message", "定位器有效（" + ms + "ms）");
                } else {
                    return Map.of("is_valid", false, "validation_message", "元素不可见");
                }
            } catch (Exception e) {
                long ms = System.currentTimeMillis() - start;
                br.close();
                return Map.of("is_valid", false, "validation_message", "未找到元素（" + ms + "ms）: " + e.getMessage());
            }
        } catch (Exception e) {
            return Map.of("is_valid", false, "validation_message", "验证失败: " + e.getMessage());
        }
    }

    @PostMapping("/elements/{id}/generate_suggestions/")
    public Map<String, Object> generateSuggestions(@PathVariable Long id) {
        UiElement el = repo.findById(id).orElse(null);
        if (el == null) return Map.of("suggestions", List.of());

        String baseUrl = getBaseUrl(el.getProjectId());
        if (baseUrl.isBlank()) return Map.of("suggestions", List.of());

        List<Map<String, Object>> suggestions = new ArrayList<>();

        try (Playwright pw = Playwright.create()) {
            Browser br = pw.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true));
            Page page = br.newContext(new Browser.NewContextOptions().setIgnoreHTTPSErrors(true)).newPage();
            page.navigate(baseUrl);
            page.waitForLoadState();
            page.waitForTimeout(1000);

            // Scan for interactive elements and suggest locators
            String[] roles = {"button", "textbox", "link", "checkbox", "combobox", "radio", "img", "heading"};
            for (String role : roles) {
                try {
                    Locator locs = page.getByRole(roleToAria(role));
                    int count = locs.count();
                    for (int i = 0; i < Math.min(count, 3); i++) {
                        Locator l = locs.nth(i);
                        if (l.isVisible()) {
                            String name = "";
                            try { name = l.getAttribute("name"); } catch (Exception ignored) {}
                            if (name == null || name.isBlank()) {
                                try { name = l.getAttribute("placeholder"); } catch (Exception ignored) {}
                            }
                            if (name == null || name.isBlank()) {
                                try { name = l.textContent(); } catch (Exception ignored) {}
                            }
                            if (name != null && name.length() > 50) name = name.substring(0, 50);

                            Map<String, Object> s = new LinkedHashMap<>();
                            s.put("name", (name != null && !name.isBlank()) ? name : (role + " #" + (i + 1)));
                            s.put("element_type", roleToType(role));
                            s.put("locator_strategy", "Role (ARIA)");
                            s.put("locator_value", (name != null && !name.isBlank()) ? name.trim() : "");
                            suggestions.add(s);
                        }
                    }
                } catch (Exception ignored) {}
            }

            // Also suggest via CSS selectors for inputs/buttons
            try {
                Locator inputs = page.locator("input, textarea, select, button");
                for (int i = 0; i < Math.min(inputs.count(), 5); i++) {
                    Locator l = inputs.nth(i);
                    String tag = "";
                    try { tag = l.evaluate("el => el.tagName").toString(); } catch (Exception ignored) {}
                    String elemId = "";
                    try { elemId = l.getAttribute("id"); } catch (Exception ignored) {}
                    String cssValue = "";
                    if (elemId != null && !elemId.isBlank()) {
                        cssValue = "#" + elemId;
                    } else {
                        String nm = "";
                        try { nm = l.getAttribute("name"); } catch (Exception e) {}
                        if (nm != null && !nm.isBlank()) {
                            cssValue = tag.toLowerCase() + "[name='" + nm + "']";
                        }
                    }
                    if (!cssValue.isBlank()) {
                        String txt = "";
                        try { txt = l.textContent(); } catch (Exception ignored) {}
                        if (txt != null && txt.length() > 50) txt = txt.substring(0, 50);
                        Map<String, Object> s = new LinkedHashMap<>();
                        s.put("name", (txt != null && !txt.isBlank()) ? txt.trim() : (tag.toUpperCase() + " #" + (i + 1)));
                        s.put("element_type", tag.equalsIgnoreCase("SELECT") ? "DROPDOWN" :
                            (tag.equalsIgnoreCase("TEXTAREA") ? "INPUT" : tag.toUpperCase()));
                        s.put("locator_strategy", "CSS Selector");
                        s.put("locator_value", cssValue);
                        suggestions.add(s);
                    }
                }
            } catch (Exception ignored) {}

            br.close();
        } catch (Exception e) {
            return Map.of("suggestions", suggestions, "error", e.getMessage());
        }

        return Map.of("suggestions", suggestions);
    }

    // ── helpers ──
    private String getBaseUrl(Long projectId) {
        if (projectId == null) return "";
        return projectRepo.findById(projectId).map(UiProject::getBaseUrl).orElse("");
    }

    private AriaRole roleToAria(String role) {
        return switch (role) {
            case "button" -> AriaRole.BUTTON;
            case "textbox" -> AriaRole.TEXTBOX;
            case "link" -> AriaRole.LINK;
            case "checkbox" -> AriaRole.CHECKBOX;
            case "combobox" -> AriaRole.COMBOBOX;
            case "radio" -> AriaRole.RADIO;
            case "img" -> AriaRole.IMG;
            case "heading" -> AriaRole.HEADING;
            default -> AriaRole.BUTTON;
        };
    }

    private String roleToType(String role) {
        return switch (role) {
            case "button" -> "BUTTON";
            case "textbox" -> "INPUT";
            case "link" -> "LINK";
            case "checkbox" -> "CHECKBOX";
            case "combobox" -> "DROPDOWN";
            case "radio" -> "RADIO";
            case "img" -> "IMAGE";
            case "heading" -> "HEADING";
            default -> "BUTTON";
        };
    }

    private Locator buildLocator(Page page, String strategy, String value) {
        if (value == null) value = "";
        String s = strategy != null ? strategy.toLowerCase() : "";
        if (s.startsWith("role")) s = "role";
        if (s.startsWith("text")) s = "text";
        return switch (s) {
            case "xpath" -> page.locator("xpath=" + value);
            case "id" -> page.locator("#" + value.replaceFirst("^#", ""));
            case "name" -> page.locator("[name='" + value.replace("'", "\\'") + "']");
            case "placeholder" -> page.getByPlaceholder(value);
            case "text" -> page.getByText(value);
            case "role" -> page.locator("[role='" + value.replace("'", "\\'") + "']");
            default -> page.locator(value); // CSS Selector
        };
    }

    // ── Element Groups / Pages (JPA) ──
    @GetMapping("/element-groups/")
    public List<Map<String, Object>> groups(@RequestParam(required = false) Long project) {
        List<UiElementGroup> all = (project != null) ? groupRepo.findByProjectId(project) : groupRepo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (UiElementGroup g : all) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", g.getId()); m.put("name", g.getName());
            m.put("parent", g.getParentId()); m.put("project_id", g.getProjectId());
            result.add(m);
        }
        return result;
    }

    @GetMapping("/element-groups/tree/")
    public List<Map<String, Object>> groupTree(@RequestParam(required = false) Long project) {
        List<UiElementGroup> all = (project != null) ? groupRepo.findByProjectId(project) : groupRepo.findAll();
        return buildGroupTree(null, all);
    }

    private List<Map<String, Object>> buildGroupTree(Long parentId, List<UiElementGroup> all) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (UiElementGroup g : all) {
            if ((parentId == null && g.getParentId() == null) || (parentId != null && parentId.equals(g.getParentId()))) {
                Map<String, Object> node = new LinkedHashMap<>();
                node.put("id", g.getId()); node.put("name", g.getName());
                node.put("parent", g.getParentId());
                node.put("children", buildGroupTree(g.getId(), all));
                result.add(node);
            }
        }
        return result;
    }

    @PostMapping("/element-groups/")
    public Map<String, Object> createGroup(@RequestBody Map<String, Object> body) {
        UiElementGroup g = new UiElementGroup();
        g.setName((String) body.getOrDefault("name", "New Page"));
        Object parentVal = body.getOrDefault("parent_page", body.get("parent_group"));
        if (parentVal != null && !"".equals(parentVal)) g.setParentId(Long.valueOf(parentVal.toString()));
        if (body.containsKey("project")) g.setProjectId(Long.valueOf(body.get("project").toString()));
        g = groupRepo.save(g);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("id", g.getId()); r.put("name", g.getName());
        r.put("parent", g.getParentId()); r.put("message", "success");
        return r;
    }

    @PatchMapping("/element-groups/{id}/")
    public Map<String, Object> updateGroup(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return groupRepo.findById(id).map(g -> {
            if (body.containsKey("name")) g.setName((String) body.get("name"));
            Object parentValUpdate = body.getOrDefault("parent_page", body.get("parent_group"));
            g.setParentId(parentValUpdate != null && !"".equals(parentValUpdate) ? Long.valueOf(parentValUpdate.toString()) : null);
            groupRepo.save(g);
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("id", g.getId()); r.put("name", g.getName()); r.put("message", "success");
            return r;
        }).orElse(Map.of("error", "not found"));
    }

    @DeleteMapping("/element-groups/{id}/")
    public Map<String, Object> deleteGroup(@PathVariable Long id) {
        groupRepo.deleteById(id);
        return Map.of("id", id, "message", "success");
    }

    // ── Helper ──
    private Map<String, Object> toMap(UiElement e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("name", e.getName());
        m.put("description", e.getDescription());
        m.put("element_type", e.getElementType());
        int sid = strategyIdFromName(e.getLocatorStrategy());
        m.put("locator_strategy", Map.of("id", sid, "name", e.getLocatorStrategy()));
        m.put("locator_strategy_id", sid);
        m.put("locator_value", e.getLocatorValue()); m.put("page", e.getPage());
        m.put("group_id", e.getGroupId()); m.put("project_id", e.getProjectId());
        m.put("component_name", e.getComponentName());
        m.put("wait_timeout", e.getWaitTimeout()); m.put("force_action", e.getForceAction());
        m.put("usage_count", e.getUsageCount());
        m.put("created_at", e.getCreatedAt() != null ? e.getCreatedAt().toString() : null);
        m.put("updated_at", e.getUpdatedAt() != null ? e.getUpdatedAt().toString() : null);
        return m;
    }

    private Map<String, Object> elementNode(UiElement e) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", e.getId()); m.put("name", e.getName());
        m.put("type", "element"); m.put("element_type", e.getElementType());
        m.put("locator_value", e.getLocatorValue());
        m.put("locator_strategy", Map.of("name", e.getLocatorStrategy()));
        return m;
    }

    private void updateFromBody(UiElement e, Map<String, Object> body) {
        if (body.containsKey("name")) e.setName((String) body.get("name"));
        if (body.containsKey("description")) e.setDescription((String) body.get("description"));
        if (body.containsKey("element_type")) e.setElementType((String) body.get("element_type"));
        if (body.containsKey("locator_strategy")) {
            Object ls = body.get("locator_strategy");
            if (ls instanceof Map) e.setLocatorStrategy((String) ((Map) ls).getOrDefault("name", "CSS Selector"));
            else if (ls instanceof String) e.setLocatorStrategy((String) ls);
        }
        if (body.containsKey("locator_strategy_id") && body.get("locator_strategy_id") != null) {
            try { e.setLocatorStrategy(mapStrategyId(Integer.parseInt(body.get("locator_strategy_id").toString()))); } catch(Exception ignored) {}
        }
        if (body.containsKey("locator_value")) e.setLocatorValue(body.get("locator_value") != null ? body.get("locator_value").toString() : null);
        if (body.containsKey("page")) e.setPage(body.get("page") != null ? body.get("page").toString() : null);
        if (body.containsKey("group_id") && body.get("group_id") != null)
            e.setGroupId(Long.valueOf(body.get("group_id").toString()));
        if (body.containsKey("project_id") && body.get("project_id") != null)
            e.setProjectId(Long.valueOf(body.get("project_id").toString()));
        if (body.containsKey("component_name")) e.setComponentName(body.get("component_name") != null ? body.get("component_name").toString() : null);
        if (body.containsKey("wait_timeout") && body.get("wait_timeout") != null)
            e.setWaitTimeout(Integer.valueOf(body.get("wait_timeout").toString()));
        if (body.containsKey("force_action") && body.get("force_action") != null) {
            Object fa = body.get("force_action");
            e.setForceAction(fa instanceof Boolean ? (Boolean) fa : Boolean.valueOf(fa.toString()));
        }
    }

    private int strategyIdFromName(String name) {
        return switch (name) {
            case "CSS Selector" -> 1; case "XPath" -> 2; case "ID" -> 3;
            case "Name" -> 4; case "Class Name" -> 5; case "Tag Name" -> 6;
            case "Link Text" -> 7; case "Partial Link Text" -> 8; case "Text Content" -> 9;
            case "Placeholder" -> 10; case "Test ID (data-testid)" -> 11;
            case "Role (ARIA)" -> 12; case "Alt Text" -> 13; case "Title Attribute" -> 14;
            case "Custom Attribute" -> 15; default -> 1;
        };
    }

    private String mapStrategyId(int id) {
        return switch (id) {
            case 1 -> "CSS Selector"; case 2 -> "XPath"; case 3 -> "ID";
            case 4 -> "Name"; case 5 -> "Class Name"; case 6 -> "Tag Name";
            case 7 -> "Link Text"; case 8 -> "Partial Link Text"; case 9 -> "Text Content";
            case 10 -> "Placeholder"; case 11 -> "Test ID (data-testid)";
            case 12 -> "Role (ARIA)"; case 13 -> "Alt Text"; case 14 -> "Title Attribute";
            case 15 -> "Custom Attribute"; default -> "CSS Selector";
        };
    }
}
