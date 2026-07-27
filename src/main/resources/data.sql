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
