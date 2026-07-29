-- ===========================================================
-- 测试数据（天气查询 & 全网热榜 工具）
-- ===========================================================

-- 1. 插入测试 API-Key
INSERT OR IGNORE INTO api_key (id, api_key, status, created_time)
VALUES (1, 'test-api-key-a', 1, datetime('now')),
       (2, 'test-api-key-b', 1, datetime('now')),
       (3, 'test-api-key-single', 1, datetime('now'));

-- 2. 天气查询 Tool (uapis.cn)
INSERT OR IGNORE INTO tool_definition (id, name, type, description, version, enabled, created_time)
VALUES (10, 'http_get_weather', 'HTTP', '调用 uapis.cn 查询气象数据 HTTP 接口', 1, 1, datetime('now'));

INSERT OR IGNORE INTO http_tool_config (tool_id, method, url, query_template)
VALUES (10, 'GET', 'https://uapis.cn/api/v1/misc/weather', '{"city":"{{city}}","lang":"{{lang}}"}');

INSERT OR IGNORE INTO tool_definition (id, name, type, description, input_schema, version, enabled, created_time)
VALUES (11, 'get_weather', 'WORKFLOW', '查询指定城市实时天气预报（例如：北京、上海、Tokyo）',
        '{"type":"object","properties":{"city":{"type":"string","description":"城市名称，中文或英文，例如：北京、Shanghai"},"lang":{"type":"string","description":"语言选填：zh（中文）、en（英文）"}}}',
        1, 1, datetime('now'));

INSERT OR IGNORE INTO workflow_tool_config (tool_id, groovy_script)
VALUES (11, '
def targetCity = binding.hasVariable("city") && city ? city : "北京"
def targetLang = binding.hasVariable("lang") && lang ? lang : "zh"
def res = tools.call(10L, [city: targetCity, lang: targetLang])
return res
');

-- 3. 全网热榜 Tool (uapis.cn)
INSERT OR IGNORE INTO tool_definition (id, name, type, description, version, enabled, created_time)
VALUES (20, 'http_get_hotboard', 'HTTP', '调用 uapis.cn 获取全网平台热榜 HTTP 接口', 1, 1, datetime('now'));

INSERT OR IGNORE INTO http_tool_config (tool_id, method, url, query_template)
VALUES (20, 'GET', 'https://uapis.cn/api/v1/misc/hotboard', '{"type":"{{type}}"}');

INSERT OR IGNORE INTO tool_definition (id, name, type, description, input_schema, version, enabled, created_time)
VALUES (21, 'get_hotboard', 'WORKFLOW', '获取指定平台的实时热搜榜单（支持 weibo, zhihu, bilibili, douyin, toutiao 等）',
        '{"type":"object","properties":{"type":{"type":"string","description":"热榜平台类型，如：weibo(微博), zhihu(知乎), bilibili(B站), douyin(抖音), toutiao(头条)"}},"required":["type"]}',
        1, 1, datetime('now'));

INSERT OR IGNORE INTO workflow_tool_config (tool_id, groovy_script)
VALUES (21, '
def boardType = binding.hasVariable("type") && type ? type : "weibo"
def res = tools.call(20L, [type: boardType])
return res
');

-- 4. 绑定关系：
-- api-key-a (id=1) 绑定 2 个工具 (get_weather, get_hotboard)
INSERT OR IGNORE INTO api_key_tool (api_key_id, tool_id, enabled) VALUES (1, 11, 1);
INSERT OR IGNORE INTO api_key_tool (api_key_id, tool_id, enabled) VALUES (1, 21, 1);

-- test-api-key-single (id=3) 仅绑定 1 个工具 (get_weather)
INSERT OR IGNORE INTO api_key_tool (api_key_id, tool_id, enabled) VALUES (3, 11, 1);

-- ===========================================================
-- 5. 数据库持久化 Prompt 模板（支持引导 LLM 调用 MCP Tool）
-- ===========================================================

-- Prompt 1: 智能天气出行顾问 (引导调用 get_weather 工具)
INSERT OR IGNORE INTO prompt_definition (id, name, description, arguments_json, template, enabled, created_time)
VALUES (101, 'weather_advisor_prompt', '分析指定城市天气并提供智能穿衣与出行建议（引导调用 get_weather 工具）',
        '[{"name":"city","description":"需要查询天气的城市名称（如：北京、上海、东京）","required":true},{"name":"activity","description":"拟进行的活动类型（如：户外登山、商旅出差、露营、居家）","required":false}]',
        '你是一位专业的智能生活与出行顾问。用户希望了解 {{city}} 的天气并获得关于【{{activity}}】的针对性建议。

请按以下步骤处理：
1. **调用工具**：请优先调用 `get_weather` 工具（参数：city="{{city}}"）获取实时气象数据。
2. **分析天气**：读取工具返回的温度、天气状况、风向风力与湿度等核心指标。
3. **输出建议**：
   - 针对【{{activity}}】活动给出针对性指导（如穿衣搭配、携带雨具、防晒指数或避险提示）。
   - 提供 1-2 条暖心提醒。',
        1, datetime('now'));

-- Prompt 2: 全网热点趋势分析助手 (引导调用 get_hotboard 工具)
INSERT OR IGNORE INTO prompt_definition (id, name, description, arguments_json, template, enabled, created_time)
VALUES (102, 'hot_topics_analyst_prompt', '获取指定平台的实时热搜榜单并提炼热度趋势（引导调用 get_hotboard 工具）',
        '[{"name":"type","description":"平台类型（weibo:微博, zhihu:知乎, bilibili:B站, douyin:抖音, toutiao:头条）","required":true},{"name":"focus_topic","description":"重点关注的主题词或领域（选填，如：科技、AI、美食）","required":false}]',
        '你是一位资深的网络舆情与热点趋势分析师。

请执行以下任务：
1. **调用工具**：使用 `get_hotboard` 工具（参数：type="{{type}}"）获取当前平台的实时热榜榜单。
2. **提取焦点**：从返回的热搜列表中遴选前 10 个热门话题。若指定了重点关注主题【{{focus_topic}}】，请重点提炼与该主题关联的讨论。
3. **输出报告**：
   - 给出 TOP 5 热搜话题摘要清单。
   - 分析当前平台用户的关注热点趋势与情绪走向。',
        1, datetime('now'));

-- Prompt 3: 每日综合早报生成器 (链式引导调用 get_weather 和 get_hotboard 工具)
INSERT OR IGNORE INTO prompt_definition (id, name, description, arguments_json, template, enabled, created_time)
VALUES (103, 'daily_briefing_prompt', '生成一份融合当地天气与今日全网热点的结构化每日早报（组合调用 get_weather 与 get_hotboard 工具）',
        '[{"name":"city","description":"用户所在城市","required":true},{"name":"hot_platform","description":"想要看热搜的平台，默认 weibo","required":false}]',
        '请为用户打造一份版面精致、信息丰富的《每日综合早报》。

请按照以下顺序发起工具调用并生成内容：
1. **获取天气**：调用 `get_weather` 工具（city="{{city}}"）获取本地天气预报。
2. **获取热点**：调用 `get_hotboard` 工具（type="{{hot_platform}}"）获取平台热榜数据。
3. **合成早报**：
   - 【早安问候与天气专栏】：播报今日天气状况、气温区间与穿衣出行指南。
   - 【热点早知道】：呈现 3-5 条精选热搜标题及简要解读。
   - 【今日励志语录】：附上一句积极向上的早安金句。',
        1, datetime('now'));

-- 6. API-Key 与 Prompt 的授权关联
-- api-key-a (id=1) 授权使用全部 3 个 Prompt
INSERT OR IGNORE INTO api_key_prompt (api_key_id, prompt_id, enabled) VALUES (1, 101, 1);
INSERT OR IGNORE INTO api_key_prompt (api_key_id, prompt_id, enabled) VALUES (1, 102, 1);
INSERT OR IGNORE INTO api_key_prompt (api_key_id, prompt_id, enabled) VALUES (1, 103, 1);

-- test-api-key-single (id=3) 仅授权使用 1 个 Prompt (weather_advisor_prompt)
INSERT OR IGNORE INTO api_key_prompt (api_key_id, prompt_id, enabled) VALUES (3, 101, 1);

-- ===========================================================
-- 7. 数据库持久化 Resource 动态资源数据
-- ===========================================================

-- Resource 1: 系统开发与操作手册 (Markdown 文本)
INSERT OR IGNORE INTO resource_definition (id, uri, name, description, mime_type, content, enabled, created_time)
VALUES (201, 'file:///docs/gateway_manual.md', 'MCP Gateway 系统操作手册', '包含 Gateway 网关配置、Tool、Prompt 与 Resource 的核心使用说明文档', 'text/markdown',
        '# MCP Gateway 网关使用手册

## 概述
MCP Gateway 是一款基于 Spring Boot 3 与 MCP 协议规范构建的高性能 AI 代理网关。

## 核心功能
1. **Tools 动态管理**：支持 HTTP 与 Groovy Workflow 两种类型的动态工具扩展。
2. **Prompts 持久化**：支持模版渲染与工具联动提示词。
3. **Resources 动态资源**：支持全类型静态与动态资源的客户端读取与隔离。',
        1, datetime('now'));

-- Resource 2: 网关实时运行状态 (JSON)
INSERT OR IGNORE INTO resource_definition (id, uri, name, description, mime_type, content, enabled, created_time)
VALUES (202, 'system://config/system_status.json', '系统运行状态配置', '静态/动态服务节点状态与版本指标配置', 'application/json',
        '{"gateway_version":"0.0.1","status":"UP","active_mcp_listeners":1,"enabled_features":["tools","prompts","resources","aop_logging"]}',
        1, datetime('now'));

-- Resource 3: 数据字典 (CSV)
INSERT OR IGNORE INTO resource_definition (id, uri, name, description, mime_type, content, enabled, created_time)
VALUES (203, 'db://data/sample_dictionary.csv', '系统数据字典词条', '核心业务字段与映射说明数据字典', 'text/csv',
        'field_name,field_type,description
city,string,城市名称
type,string,热搜榜单类型
activity,string,拟进行的活动',
        1, datetime('now'));

-- 8. API-Key 与 Resource 的授权关联
-- api-key-a (id=1) 授权读取全部 3 个 Resource
INSERT OR IGNORE INTO api_key_resource (api_key_id, resource_id, enabled) VALUES (1, 201, 1);
INSERT OR IGNORE INTO api_key_resource (api_key_id, resource_id, enabled) VALUES (1, 202, 1);
INSERT OR IGNORE INTO api_key_resource (api_key_id, resource_id, enabled) VALUES (1, 203, 1);

-- test-api-key-single (id=3) 仅授权读取 1 个 Resource (gateway_manual.md)
INSERT OR IGNORE INTO api_key_resource (api_key_id, resource_id, enabled) VALUES (3, 201, 1);


