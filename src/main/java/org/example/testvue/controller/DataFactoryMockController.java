package org.example.testvue.controller;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/data-factory")
public class DataFactoryMockController {

    // ── Categories: matches reference project tool_list.py structure ──
    @GetMapping("/categories/")
    public Map<String, Object> categories() {
        List<Map<String, Object>> list = new ArrayList<>();

        list.add(cat("test_data", "测试数据", "User", List.of(
            tool("generate_chinese_name", "生成中文姓名", "生成随机中文姓名", "test_data", "user"),
            tool("generate_chinese_phone", "生成手机号", "生成中国大陆手机号", "test_data", "phone"),
            tool("generate_chinese_email", "生成邮箱地址", "生成随机邮箱地址", "test_data", "message"),
            tool("generate_id_card", "生成身份证号", "生成18位身份证号", "test_data", "id-card"),
            tool("generate_chinese_address", "生成地址", "生成省市区地址", "test_data", "location"),
            tool("generate_company_name", "生成公司名称", "生成公司名称", "test_data", "office-building"),
            tool("generate_bank_card", "生成银行卡号", "生成银行卡号", "test_data", "bank-card"),
            tool("generate_user_profile", "生成用户档案", "批量生成完整用户信息", "test_data", "user-filled"),
            tool("generate_business_license", "生成统一信用代码", "生成统一社会信用代码", "test_data", "document")
        )));

        list.add(cat("random", "随机工具", "Coin", List.of(
            tool("random_int", "随机整数", "生成指定范围的随机整数", "random", "trend-charts"),
            tool("random_float", "随机浮点数", "生成指定范围的随机小数", "random", "scale-to-original"),
            tool("random_string", "随机字符串", "生成随机字符串", "random", "document"),
            tool("random_uuid", "UUID", "生成UUID", "random", "key"),
            tool("random_password", "随机密码", "生成指定强度密码", "random", "lock"),
            tool("random_date", "随机日期", "生成日期范围内随机日期", "random", "calendar"),
            tool("random_boolean", "随机布尔值", "生成随机 true/false", "random", "switch"),
            tool("random_ip_address", "随机IP地址", "生成随机IPv4/IPv6", "random", "connection")
        )));

        list.add(cat("json", "JSON工具", "Operation", List.of(
            tool("format_json", "格式化JSON", "美化或压缩JSON", "json", "code"),
            tool("validate_json", "验证JSON", "检查JSON合法性", "json", "circle-check"),
            tool("json_diff", "JSON差异对比", "比较两个JSON的差异", "json", "sort"),
            tool("mock_data", "生成Mock数据", "按模板生成模拟JSON数据", "json", "data-line")
        )));

        list.add(cat("string", "字符工具", "Edit", List.of(
            tool("remove_whitespace", "去除空格", "去除文本中的空格", "string", "delete"),
            tool("replace_string", "替换字符", "替换文本中的指定字符", "string", "refresh"),
            tool("word_count", "字数统计", "统计文本字数/行数", "string", "data-analysis"),
            tool("case_convert", "大小写转换", "大小写/首字母大写", "string", "font-colors")
        )));

        list.add(cat("encoding", "编码工具", "Link", List.of(
            tool("base64_encode", "Base64编码", "文本转Base64", "encoding", "connection"),
            tool("base64_decode", "Base64解码", "Base64转文本", "encoding", "link"),
            tool("url_encode", "URL编码", "文本转URL编码", "encoding", "link"),
            tool("url_decode", "URL解码", "URL编码转文本", "encoding", "link"),
            tool("generate_qrcode", "生成二维码", "生成QR Code图片", "encoding", "picture"),
            tool("timestamp_convert", "时间戳转换", "时间戳与日期互转", "encoding", "clock")
        )));

        list.add(cat("encryption", "加密工具", "Lock", List.of(
            tool("md5_hash", "MD5哈希", "计算MD5哈希值", "encryption", "key"),
            tool("sha256_hash", "SHA256哈希", "计算SHA256哈希值", "encryption", "key"),
            tool("aes_encrypt", "AES加密", "AES对称加密", "encryption", "lock"),
            tool("password_strength", "密码强度", "评估密码强度", "encryption", "warning")
        )));

        list.add(cat("crontab", "Crontab工具", "Clock", List.of(
            tool("generate_expression", "生成表达式", "生成Cron表达式", "crontab", "edit"),
            tool("parse_expression", "解析表达式", "解析Cron各字段", "crontab", "view"),
            tool("get_next_runs", "执行时间", "计算未来执行时间", "crontab", "calendar")
        )));

        return Map.of("categories", list, "total_tools", countTools(list));
    }

    // ── Execute Tool ──
    @PostMapping("/")
    public Map<String, Object> execute(@RequestBody Map<String, Object> body) {
        String toolName = (String) body.getOrDefault("tool_name", "unknown");
        Map<String, Object> input = (Map<String, Object>) body.getOrDefault("input_data", Map.of());
        int count = 1;
        if (input.containsKey("count")) {
            try { count = Integer.parseInt(input.get("count").toString()); } catch (Exception e) { count = 1; }
        }
        count = Math.max(1, Math.min(count, 100));

        Map<String, Object> result = executeTool(toolName, input, count);
        if (body.get("is_saved") == null || Boolean.TRUE.equals(body.get("is_saved"))) {
            result.put("record_id", UUID.randomUUID().toString());
            result.put("created_at", LocalDateTime.now().toString());
        }
        return result;
    }

    // ── Batch Generate ──
    @PostMapping("/batch_generate/")
    public Map<String, Object> batchGenerate(@RequestBody Map<String, Object> body) {
        String toolName = (String) body.getOrDefault("tool_name", "random_int");
        Map<String, Object> input = (Map<String, Object>) body.getOrDefault("input_data", Map.of());
        int count = 10;
        try { count = Integer.parseInt(body.getOrDefault("count", "10").toString()); } catch (Exception e) {}
        count = Math.min(count, 100);

        List<Object> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            results.add(executeTool(toolName, input, 1).get("result"));
        }
        return Map.of("results", results, "count", results.size(), "total_requested", count);
    }

    // ── History ──
    @GetMapping("/")
    public Map<String, Object> history() { return paginated(List.of()); }

    @DeleteMapping("/{id}/")
    public Map<String, Object> delete(@PathVariable String id) { return Map.of("id", id, "message", "success"); }

    // ── Statistics ──
    @GetMapping("/statistics/")
    public Map<String, Object> statistics() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("total_records", 15280); s.put("today_records", 45);
        s.put("category_stats", Map.of("test_data", 8200, "random", 3200, "json", 1500, "string", 1000, "encoding", 800, "encryption", 400, "crontab", 180));
        s.put("scenario_stats", Map.of("test_data", 9000, "random", 3500, "json", 1800, "other", 980));
        s.put("recent_tools", List.of("generate_chinese_name", "random_int", "md5_hash", "base64_encode", "random_uuid"));
        return s;
    }

    // ── Tags ──
    @GetMapping("/tags/")
    public List<String> tags() { return List.of("回归测试", "冒烟测试", "性能测试"); }

    // ── Download ──
    @GetMapping("/download_static_file/")
    public Map<String, String> download(@RequestParam String filename) { return Map.of("url", "/static/" + filename); }

    // ── Variable Functions ──
    @GetMapping("/variable_functions/")
    public List<Map<String, String>> variableFunctions() {
        return List.of(
            Map.of("name", "random_int", "syntax", "${random_int(min, max)}", "desc", "生成随机整数", "example", "${random_int(1, 100)}", "category", "随机数"),
            Map.of("name", "random_float", "syntax", "${random_float(min, max, precision)}", "desc", "生成随机浮点数", "example", "${random_float(0, 100, 2)}", "category", "随机数"),
            Map.of("name", "random_string", "syntax", "${random_string(length)}", "desc", "生成随机字符串", "example", "${random_string(10)}", "category", "随机数"),
            Map.of("name", "random_uuid", "syntax", "${random_uuid()}", "desc", "生成UUID", "example", "${random_uuid()}", "category", "随机数"),
            Map.of("name", "generate_chinese_name", "syntax", "${generate_chinese_name()}", "desc", "生成中文姓名", "example", "${generate_chinese_name()}", "category", "测试数据"),
            Map.of("name", "generate_chinese_phone", "syntax", "${generate_chinese_phone()}", "desc", "生成手机号", "example", "${generate_chinese_phone()}", "category", "测试数据"),
            Map.of("name", "generate_chinese_email", "syntax", "${generate_chinese_email()}", "desc", "生成邮箱", "example", "${generate_chinese_email()}", "category", "测试数据"),
            Map.of("name", "generate_id_card", "syntax", "${generate_id_card()}", "desc", "生成身份证号", "example", "${generate_id_card()}", "category", "测试数据"),
            Map.of("name", "random_date", "syntax", "${random_date(start, end)}", "desc", "生成随机日期", "example", "${random_date('2024-01-01', '2024-12-31')}", "category", "随机数"),
            Map.of("name", "md5_hash", "syntax", "${md5_hash(text)}", "desc", "MD5哈希", "example", "${md5_hash('hello')}", "category", "加密"),
            Map.of("name", "base64_encode", "syntax", "${base64_encode(text)}", "desc", "Base64编码", "example", "${base64_encode('hello')}", "category", "编码"),
            Map.of("name", "timestamp", "syntax", "${timestamp()}", "desc", "当前时间戳", "example", "${timestamp()}", "category", "编码")
        );
    }

    // ── Tool execution logic ──
    private Map<String, Object> executeTool(String toolName, Map<String, Object> input, int count) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("success", true);

        if (count == 1) {
            r.put("result", generateSingle(toolName, 0));
        } else {
            List<Object> results = new ArrayList<>();
            for (int i = 0; i < count; i++) results.add(generateSingle(toolName, i));
            r.put("result", results);
            r.put("count", count);
        }
        return r;
    }

    private Object generateSingle(String toolName, int idx) {
        switch (toolName) {
            case "generate_chinese_name": {
                String[] names = {"张伟", "王芳", "李娜", "刘洋", "陈静", "杨帆", "赵敏", "黄磊", "周杰", "吴鑫",
                    "徐慧", "孙宇", "马丽", "朱峰", "胡涛", "林婷", "何刚", "郭宁", "高远", "罗琳"};
                return names[idx % names.length];
            }
            case "generate_chinese_phone": {
                String[] phones = {"13800138001", "13912345678", "15812340987", "18655556666", "17788990011"};
                return phones[idx % phones.length];
            }
            case "generate_chinese_email": {
                String[] users = {"zhangwei", "wangfang", "lina", "test", "admin"};
                String[] domains = {"qq.com", "163.com", "gmail.com", "outlook.com", "example.com"};
                return users[idx % users.length] + (idx + 1) + "@" + domains[idx % domains.length];
            }
            case "generate_id_card": return "11010119900101" + String.format("%04d", 1000 + idx);
            case "generate_chinese_address": return Map.of("province", "北京市", "city", "市辖区", "district", "朝阳区", "detail", "某某路" + (100 + idx) + "号");
            case "generate_company_name": return new String[]{"华兴科技", "鼎新信息", "中盛贸易", "灵动网络", "启明数据"}[idx % 5] + "有限公司";
            case "generate_bank_card": return "6222" + String.format("%012d", 100000000000L + idx * 777);
            case "generate_user_profile": return Map.of("name", generateSingle("generate_chinese_name", idx), "phone", generateSingle("generate_chinese_phone", idx), "email", generateSingle("generate_chinese_email", idx), "age", 20 + idx % 40, "gender", idx % 2 == 0 ? "男" : "女");
            case "generate_business_license": return "91110000MA00" + String.format("%06d", 100000 + idx) + "X";
            case "random_int": return 1 + idx * 42;
            case "random_float": return String.format("%.2f", 0.5 + idx * 3.14);
            case "random_string": return "str_" + UUID.randomUUID().toString().substring(0, 8);
            case "random_uuid": return UUID.randomUUID().toString();
            case "random_password": return "Pwd@" + UUID.randomUUID().toString().substring(0, 8);
            case "random_date": return "2026-" + String.format("%02d", 1 + idx % 12) + "-" + String.format("%02d", 1 + idx % 28);
            case "random_boolean": return idx % 2 == 0;
            case "random_ip_address": return "192.168." + (1 + idx / 255) + "." + (1 + idx % 255);
            case "format_json": return "{\n  \"name\": \"test\",\n  \"value\": " + (idx + 1) + "\n}";
            case "validate_json": return Map.of("valid", true, "message", "JSON格式正确");
            case "json_diff": return Map.of("added", 2, "removed", 1, "changed", 3);
            case "mock_data": return Map.of("id", idx + 1, "name", generateSingle("generate_chinese_name", idx), "email", generateSingle("generate_chinese_email", idx));
            case "remove_whitespace": return "text_without_spaces_" + idx;
            case "replace_string": return "replaced_text_" + idx;
            case "word_count": return Map.of("chars", 100 + idx, "words", 20);
            case "case_convert": return "CONVERTED_TEXT_" + idx;
            case "base64_encode": return "YmFzZTY0X2VuY29kZWRfdGV4dF8=" + idx;
            case "base64_decode": return "decoded_text_" + idx;
            case "url_encode": return "url%20encoded%20text%20" + idx;
            case "url_decode": return "url decoded text " + idx;
            case "generate_qrcode": return Map.of("url", "/static/img/qr_" + idx + ".png", "size", 256);
            case "timestamp_convert": return Map.of("timestamp", System.currentTimeMillis(), "datetime", "2026-07-03 12:00:00");
            case "md5_hash": return "e10adc3949ba59abbe56e057f20f883e";
            case "sha256_hash": return "8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92";
            case "aes_encrypt": return "U2FsdGVkX1" + UUID.randomUUID().toString().substring(0, 8);
            case "password_strength": return Map.of("score", 4, "level", "强", "suggestions", List.of());
            case "generate_expression": return "0 8 * * *";
            case "parse_expression": return Map.of("minute", "0", "hour", "8", "day", "*", "month", "*", "weekday", "*");
            case "get_next_runs": return List.of("2026-07-04 08:00:00", "2026-07-05 08:00:00", "2026-07-06 08:00:00");
            default: return "generated_" + toolName + "_" + (idx + 1);
        }
    }

    // ── Helpers ──
    private Map<String, Object> cat(String category, String name, String icon, List<Map<String, Object>> tools) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("category", category); c.put("name", name); c.put("scenario", category);
        c.put("icon", icon); c.put("tools", tools);
        return c;
    }

    private Map<String, Object> tool(String name, String displayName, String desc, String scenario, String icon) {
        Map<String, Object> t = new LinkedHashMap<>();
        t.put("name", name); t.put("display_name", displayName); t.put("description", desc);
        t.put("scenario", scenario); t.put("icon", icon);
        return t;
    }

    private int countTools(List<Map<String, Object>> cats) {
        int n = 0;
        for (Map<String, Object> c : cats) n += ((List) c.get("tools")).size();
        return n;
    }

    private Map<String, Object> paginated(List<Map<String, Object>> results) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("count", results.size()); r.put("results", results);
        return r;
    }
}
