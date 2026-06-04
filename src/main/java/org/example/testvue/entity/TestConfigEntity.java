package org.example.testvue.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "test_config")
public class TestConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 128)
    private String configName;

    @Column(length = 512)
    private String url;

    @Column(length = 64)
    private String projectId;

    @Column(length = 64)
    private String username;

    @Column(length = 128)
    private String password;

    public TestConfigEntity() {}

    public TestConfigEntity(String configName, String url, String projectId, String username, String password) {
        this.configName = configName;
        this.url = url;
        this.projectId = projectId;
        this.username = username;
        this.password = password;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getConfigName() { return configName; }
    public void setConfigName(String configName) { this.configName = configName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getProjectId() { return projectId; }
    public void setProjectId(String projectId) { this.projectId = projectId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
