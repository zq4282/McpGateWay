-- API Key 表
CREATE TABLE IF NOT EXISTS api_key
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    api_key      VARCHAR(128) UNIQUE NOT NULL,
    status       INTEGER DEFAULT 1,
    created_time DATETIME
);

-- Tool 定义表（WORKFLOW / HTTP 两种类型）
CREATE TABLE IF NOT EXISTS tool_definition
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    name         VARCHAR(100),
    type         VARCHAR(20),
    description  TEXT,
    input_schema TEXT,
    output_schema TEXT,
    version      INTEGER DEFAULT 1,
    enabled      INTEGER DEFAULT 1,
    created_time DATETIME
);

-- API-Key 与 Tool 的关联关系（控制哪个 Key 能看到哪些 Tool）
CREATE TABLE IF NOT EXISTS api_key_tool
(
    api_key_id INTEGER,
    tool_id    INTEGER,
    enabled    INTEGER DEFAULT 1,
    PRIMARY KEY (api_key_id, tool_id)
);

-- Workflow Tool 额外配置（存储 Groovy 脚本）
CREATE TABLE IF NOT EXISTS workflow_tool_config
(
    tool_id      INTEGER PRIMARY KEY,
    groovy_script TEXT
);

-- HTTP Tool 额外配置（存储 HTTP 请求模板）
CREATE TABLE IF NOT EXISTS http_tool_config
(
    tool_id          INTEGER PRIMARY KEY,
    method           VARCHAR(10),
    url              TEXT,
    headers_template TEXT,
    query_template   TEXT,
    body_template    TEXT
);

-- Prompt 定义表
CREATE TABLE IF NOT EXISTS prompt_definition
(
    id             INTEGER PRIMARY KEY AUTOINCREMENT,
    name           VARCHAR(100) UNIQUE NOT NULL,
    description    TEXT,
    arguments_json TEXT,
    template       TEXT,
    enabled        INTEGER DEFAULT 1,
    created_time   DATETIME
);

-- API-Key 与 Prompt 的关联关系（控制哪个 Key 能看到与调用哪些 Prompt）
CREATE TABLE IF NOT EXISTS api_key_prompt
(
    api_key_id INTEGER,
    prompt_id  INTEGER,
    enabled    INTEGER DEFAULT 1,
    PRIMARY KEY (api_key_id, prompt_id)
);

-- Resource 定义表
CREATE TABLE IF NOT EXISTS resource_definition
(
    id           INTEGER PRIMARY KEY AUTOINCREMENT,
    uri          VARCHAR(255) UNIQUE NOT NULL,
    name         VARCHAR(100),
    description  TEXT,
    mime_type    VARCHAR(50),
    content      TEXT,
    enabled      INTEGER DEFAULT 1,
    created_time DATETIME
);

-- API-Key 与 Resource 的关联关系（控制哪个 Key 能看到与读取哪些 Resource）
CREATE TABLE IF NOT EXISTS api_key_resource
(
    api_key_id  INTEGER,
    resource_id INTEGER,
    enabled     INTEGER DEFAULT 1,
    PRIMARY KEY (api_key_id, resource_id)
);
