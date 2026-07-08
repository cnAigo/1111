package org.example.testvue.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "test_case_steps")
public class TestCaseStep {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "test_case_id")
    private Long testCaseId;

    @Column(name = "step_order")
    private Integer stepOrder;

    @Column(name = "action_type", length = 50)
    private String actionType;        // goto / click / right_click / fill / type / assert

    @Column(length = 500)
    private String selector;           // Playwright locator

    @Column(name = "input_value", length = 2000)
    private String inputValue;         // fill value or URL

    @Column(name = "original_instruction", length = 1000)
    private String originalInstruction; // raw user instruction

    // ── Recording detail fields ──
    @Column(length = 50)
    private String elementTag;         // e.g. "button", "input", "span"

    @Column(length = 500)
    private String elementText;        // visible text of the element

    @Column(length = 1000)
    private String elementAttributes;  // JSON: {class, id, aria, placeholder, ...}

    @Column(length = 2000)
    private String pageUrl;            // URL when step was recorded

    @Column(length = 500)
    private String screenshotPath;     // screenshot filename

    @Column(length = 500)
    private String playwrightCode;     // generated Playwright code line

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTestCaseId() { return testCaseId; }
    public void setTestCaseId(Long v) { this.testCaseId = v; }
    public Integer getStepOrder() { return stepOrder; }
    public void setStepOrder(Integer v) { this.stepOrder = v; }
    public String getActionType() { return actionType; }
    public void setActionType(String v) { this.actionType = v; }
    public String getSelector() { return selector; }
    public void setSelector(String v) { this.selector = v; }
    public String getInputValue() { return inputValue; }
    public void setInputValue(String v) { this.inputValue = v; }
    public String getOriginalInstruction() { return originalInstruction; }
    public void setOriginalInstruction(String v) { this.originalInstruction = v; }
    public String getElementTag() { return elementTag; }
    public void setElementTag(String v) { this.elementTag = v; }
    public String getElementText() { return elementText; }
    public void setElementText(String v) { this.elementText = v; }
    public String getElementAttributes() { return elementAttributes; }
    public void setElementAttributes(String v) { this.elementAttributes = v; }
    public String getPageUrl() { return pageUrl; }
    public void setPageUrl(String v) { this.pageUrl = v; }
    public String getScreenshotPath() { return screenshotPath; }
    public void setScreenshotPath(String v) { this.screenshotPath = v; }
    public String getPlaywrightCode() { return playwrightCode; }
    public void setPlaywrightCode(String v) { this.playwrightCode = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}
