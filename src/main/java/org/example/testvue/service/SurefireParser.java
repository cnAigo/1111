package org.example.testvue.service;

import org.example.testvue.dto.Dtos.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Parse surefire TEST-*.xml files using DOM — no hand-rolled indexOf nonsense. */
public class SurefireParser {

    private static final Logger LOG = LoggerFactory.getLogger(SurefireParser.class);
    private static final DocumentBuilderFactory DBF = DocumentBuilderFactory.newInstance();

    public static List<ClassResult> parseDir(Path dir) {
        List<ClassResult> list = new ArrayList<>();
        if (!Files.exists(dir)) return list;
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir, "TEST-*.xml")) {
            DocumentBuilder db = DBF.newDocumentBuilder();
            for (Path f : ds) {
                try {
                    Document doc = db.parse(f.toFile());
                    Element suite = doc.getDocumentElement();
                    ClassResult cr = new ClassResult();
                    cr.className = shortName(suite.getAttribute("name"));
                    cr.tests     = Integer.parseInt(suite.getAttribute("tests"));
                    cr.failures  = Integer.parseInt(suite.getAttribute("failures"));
                    cr.errors    = Integer.parseInt(suite.getAttribute("errors"));
                    cr.skipped   = Integer.parseInt(suite.getAttribute("skipped"));
                    cr.time      = suite.getAttribute("time");
                    cr.cases     = new ArrayList<>();

                    NodeList testcases = doc.getElementsByTagName("testcase");
                    // Deduplication map keyed by "className#methodName".
                    // Retried tests produce duplicate <testcase> entries for the
                    // same logical test method — we keep only the best outcome:
                    // a passing retry replaces a failure; otherwise the first
                    // occurrence wins.  Aggregate counts are recalculated from
                    // the deduplicated set so the progress bar never exceeds 100%.
                    Map<String, TestCaseResult> dedupMap = new LinkedHashMap<>();
                    for (int i = 0; i < testcases.getLength(); i++) {
                        Element tc = (Element) testcases.item(i);
                        TestCaseResult tcr = new TestCaseResult();
                        tcr.name = tc.getAttribute("name");
                        tcr.time = tc.getAttribute("time");

                        NodeList failures = tc.getElementsByTagName("failure");
                        NodeList errors   = tc.getElementsByTagName("error");
                        if (failures.getLength() > 0 || errors.getLength() > 0) {
                            tcr.status = "FAIL";
                            Element err = failures.getLength() > 0 ? (Element) failures.item(0) : (Element) errors.item(0);
                            tcr.reason = err.getAttribute("message");
                            String text = err.getTextContent();
                            if (text != null && !text.isBlank()) {
                                String[] lines = text.trim().split("\\r?\\n");
                                StringBuilder sb = new StringBuilder();
                                int taken = 0;
                                for (String ln : lines) {
                                    if (ln.trim().isEmpty()) continue;
                                    sb.append(ln.trim()).append("\n");
                                    if (++taken >= 4) break;
                                }
                                tcr.reason = tcr.reason + "\n" + sb.toString().trim();
                            }
                        } else {
                            tcr.status = "PASS";
                            tcr.reason = "";
                        }

                        // Unique identity = class name + method name.
                        // A single test method may appear more than once when
                        // surefire rerunFailingTestsCount > 0 or when the runner
                        // forks multiple JVMs.  The dedup key collapses those
                        // duplicates into one definitive result per method.
                        String dedupKey = cr.className + "#" + tcr.name;
                        TestCaseResult existing = dedupMap.get(dedupKey);
                        if (existing == null) {
                            dedupMap.put(dedupKey, tcr);
                        } else if ("FAIL".equals(existing.status) && "PASS".equals(tcr.status)) {
                            // A previously-failing test passed on retry —
                            // replace the failed record with the passing one.
                            dedupMap.put(dedupKey, tcr);
                        }
                        // If existing is already PASS, or both are FAIL,
                        // keep the first occurrence — later retries are redundant.
                    }

                    // Recalculate aggregate counts from the deduplicated set
                    // so the totals always reflect unique test cases, never
                    // inflated by retry duplicates.
                    cr.cases = new ArrayList<>(dedupMap.values());
                    cr.tests = cr.cases.size();
                    cr.failures = (int) cr.cases.stream().filter(c -> "FAIL".equals(c.status)).count();
                    cr.errors = 0;
                    cr.skipped = 0;
                    list.add(cr);
                } catch (Exception e) { LOG.warn("Failed to parse {}: {}", f.getFileName(), e.getMessage()); }
            }
        } catch (Exception e) { LOG.warn("Failed to read surefire dir: {}", e.getMessage()); }
        return list;
    }

    private static String shortName(String full) {
        if (full == null || full.isEmpty()) return "";
        int dot = full.lastIndexOf('.');
        return dot >= 0 ? full.substring(dot + 1) : full;
    }
}
