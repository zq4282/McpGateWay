// Dynamic MCP Tool Gateway Dashboard JS

const API_BASE = '/admin';

let currentMcpSession = {
    apiKey: null,
    connected: false
};

document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    loadDashboardMetrics();
    loadApiKeys();
    loadTools();
    loadBindings();
});

// Tab Switch Logic
function initTabs() {
    const tabs = document.querySelectorAll('.nav-tab');
    tabs.forEach(tab => {
        tab.addEventListener('click', () => {
            tabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');

            const targetTab = tab.dataset.tab;
            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.add('hidden');
            });
            document.getElementById(`tab-${targetTab}`).classList.remove('hidden');

            if (targetTab === 'playground') {
                loadPlaygroundApiKeys();
            }
        });
    });
}

// Load Dashboard Metrics
async function loadDashboardMetrics() {
    try {
        const [keysRes, toolsRes] = await Promise.all([
            fetch(`${API_BASE}/api-keys`),
            fetch(`${API_BASE}/tools`)
        ]);
        const keys = await keysRes.json();
        const tools = await toolsRes.json();

        document.getElementById('metric-keys').textContent = keys.length;
        const workflowCount = tools.filter(t => t.type === 'WORKFLOW').length;
        const httpCount = tools.filter(t => t.type === 'HTTP').length;
        document.getElementById('metric-workflows').textContent = workflowCount;
        document.getElementById('metric-http-tools').textContent = httpCount;
    } catch (err) {
        console.error('加载指标失败:', err);
    }
}

// ==================== API-Key 管理 ====================
async function loadApiKeys() {
    try {
        const res = await fetch(`${API_BASE}/api-keys`);
        const keys = await res.json();
        const tbody = document.getElementById('api-keys-tbody');
        tbody.innerHTML = '';

        keys.forEach(key => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>#${key.id}</strong></td>
                <td><code style="color:#60a5fa">${key.apiKey}</code></td>
                <td>
                    <span class="badge ${key.status === 1 ? 'badge-success' : 'badge-disabled'}">
                        ${key.status === 1 ? '🟢 已启用' : '🔴 已禁用'}
                    </span>
                </td>
                <td>${key.createdTime ? new Date(key.createdTime).toLocaleString() : '-'}</td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="toggleApiKeyStatus(${key.id}, ${key.status === 1 ? 0 : 1})">
                        ${key.status === 1 ? '禁用' : '启用'}
                    </button>
                    <button class="btn btn-sm btn-danger" onclick="deleteApiKey(${key.id})">删除</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('加载 API-Keys 失败:', err);
    }
}

async function createApiKey() {
    const keyInput = document.getElementById('new-api-key-input').value.trim();
    if (!keyInput) return alert('请输入 API-Key 名称或字符串！');

    try {
        const res = await fetch(`${API_BASE}/api-keys`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ apiKey: keyInput })
        });
        if (res.ok) {
            closeModal('modal-add-key');
            document.getElementById('new-api-key-input').value = '';
            loadApiKeys();
            loadDashboardMetrics();
        } else {
            const err = await res.json();
            alert('创建失败: ' + (err.error || '未知错误'));
        }
    } catch (err) {
        alert('请求失败');
    }
}

async function toggleApiKeyStatus(id, newStatus) {
    try {
        await fetch(`${API_BASE}/api-keys/${id}/status`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ status: newStatus })
        });
        loadApiKeys();
    } catch (err) {
        alert('修改状态失败');
    }
}

async function deleteApiKey(id) {
    if (!confirm('确定要删除该 API-Key 吗？')) return;
    try {
        await fetch(`${API_BASE}/api-keys/${id}`, { method: 'DELETE' });
        loadApiKeys();
        loadDashboardMetrics();
    } catch (err) {
        alert('删除失败');
    }
}

function generateRandomKey() {
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
    let result = 'mcp-key-';
    for (let i = 0; i < 16; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    document.getElementById('new-api-key-input').value = result;
}

// ==================== Tool 工具定义管理 ====================
async function loadTools() {
    try {
        const res = await fetch(`${API_BASE}/tools`);
        const tools = await res.json();
        const tbody = document.getElementById('tools-tbody');
        tbody.innerHTML = '';

        tools.forEach(tool => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td><strong>#${tool.id}</strong></td>
                <td><strong style="color:#f8fafc">${tool.name}</strong></td>
                <td>
                    <span class="badge ${tool.type === 'WORKFLOW' ? 'badge-type' : 'badge-http'}">
                        ${tool.type === 'WORKFLOW' ? '⚡ WORKFLOW' : '🌐 HTTP'}
                    </span>
                </td>
                <td style="max-width:250px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap;">${tool.description || '-'}</td>
                <td><span class="badge badge-type">v${tool.version}</span></td>
                <td>
                    <span class="badge ${tool.enabled === 1 ? 'badge-success' : 'badge-disabled'}">
                        ${tool.enabled === 1 ? '启用' : '禁用'}
                    </span>
                </td>
                <td>
                    <button class="btn btn-sm btn-secondary" onclick="editTool(${tool.id})">编辑</button>
                    <button class="btn btn-sm btn-danger" onclick="deleteTool(${tool.id})">删除</button>
                </td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('加载 Tools 失败:', err);
    }
}

function onToolTypeChange() {
    const type = document.getElementById('tool-type').value;
    if (type === 'WORKFLOW') {
        document.getElementById('form-workflow-fields').classList.remove('hidden');
        document.getElementById('form-http-fields').classList.add('hidden');
    } else {
        document.getElementById('form-workflow-fields').classList.add('hidden');
        document.getElementById('form-http-fields').classList.remove('hidden');
    }
}

async function saveTool() {
    const id = document.getElementById('tool-id').value;
    const type = document.getElementById('tool-type').value;
    const name = document.getElementById('tool-name').value.trim();
    const description = document.getElementById('tool-desc').value.trim();

    if (!name) return alert('请填写工具名称！');

    const body = { name, type, description };

    if (type === 'WORKFLOW') {
        body.inputSchema = document.getElementById('tool-schema').value;
        body.groovyScript = document.getElementById('tool-groovy').value;
    } else {
        body.method = document.getElementById('http-method').value;
        body.url = document.getElementById('http-url').value;
        body.queryTemplate = document.getElementById('http-query').value;
        body.headersTemplate = document.getElementById('http-headers').value;
        body.bodyTemplate = document.getElementById('http-body').value;
    }

    try {
        let res;
        if (id) {
            res = await fetch(`${API_BASE}/tools/${id}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
        } else {
            res = await fetch(`${API_BASE}/tools`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(body)
            });
        }

        if (res.ok) {
            closeModal('modal-add-tool');
            resetToolForm();
            loadTools();
            loadDashboardMetrics();
        } else {
            alert('保存失败');
        }
    } catch (err) {
        alert('网络错误');
    }
}

async function editTool(id) {
    try {
        const res = await fetch(`${API_BASE}/tools/${id}`);
        const tool = await res.json();

        document.getElementById('tool-id').value = tool.id;
        document.getElementById('tool-name').value = tool.name;
        document.getElementById('tool-type').value = tool.type;
        document.getElementById('tool-desc').value = tool.description || '';
        document.getElementById('tool-type').disabled = true;

        onToolTypeChange();

        if (tool.type === 'WORKFLOW') {
            document.getElementById('tool-schema').value = tool.inputSchema || '';
            document.getElementById('tool-groovy').value = tool.groovyScript || '';
        } else {
            document.getElementById('http-method').value = tool.method || 'GET';
            document.getElementById('http-url').value = tool.url || '';
            document.getElementById('http-query').value = tool.queryTemplate || '';
            document.getElementById('http-headers').value = tool.headersTemplate || '';
            document.getElementById('http-body').value = tool.bodyTemplate || '';
        }

        openModal('modal-add-tool');
    } catch (err) {
        alert('获取工具详情失败');
    }
}

async function deleteTool(id) {
    if (!confirm('确定要删除该工具及其所有配置吗？')) return;
    try {
        await fetch(`${API_BASE}/tools/${id}`, { method: 'DELETE' });
        loadTools();
        loadDashboardMetrics();
    } catch (err) {
        alert('删除失败');
    }
}

function resetToolForm() {
    document.getElementById('tool-id').value = '';
    document.getElementById('tool-name').value = '';
    document.getElementById('tool-type').disabled = false;
    document.getElementById('tool-desc').value = '';
    document.getElementById('tool-schema').value = '';
    document.getElementById('tool-groovy').value = '';
    document.getElementById('http-url').value = '';
    document.getElementById('http-query').value = '';
}

// ==================== API-Key 与 Tool 动态绑定配置 ====================
async function loadBindings() {
    try {
        const [keysRes, toolsRes] = await Promise.all([
            fetch(`${API_BASE}/api-keys`),
            fetch(`${API_BASE}/tools`)
        ]);
        const keys = await keysRes.json();
        const tools = await toolsRes.json();

        const keySelect = document.getElementById('bind-key-select');
        keySelect.innerHTML = '<option value="">-- 请选择 API-Key --</option>';
        keys.forEach(k => {
            keySelect.innerHTML += `<option value="${k.id}">${k.apiKey} (ID:${k.id})</option>`;
        });

        const workflowTools = tools.filter(t => t.type === 'WORKFLOW');
        const container = document.getElementById('bind-tools-container');
        container.innerHTML = '';

        workflowTools.forEach(tool => {
            const div = document.createElement('div');
            div.className = 'metric-card';
            div.style.padding = '1rem';
            div.innerHTML = `
                <div style="display:flex; align-items:center; gap:0.75rem;">
                    <input type="checkbox" class="bind-checkbox" value="${tool.id}" id="check-tool-${tool.id}">
                    <div>
                        <strong style="color:#f8fafc">${tool.name}</strong>
                        <div style="font-size:0.8rem; color:#94a3b8;">${tool.description || ''}</div>
                    </div>
                </div>
            `;
            container.appendChild(div);
        });
    } catch (err) {
        console.error('加载绑定配置失败:', err);
    }
}

async function onBindKeyChange() {
    const apiKeyId = document.getElementById('bind-key-select').value;
    const checkboxes = document.querySelectorAll('.bind-checkbox');
    checkboxes.forEach(cb => cb.checked = false);

    if (!apiKeyId) return;

    try {
        const res = await fetch(`${API_BASE}/api-key-tools?apiKeyId=${apiKeyId}`);
        const boundList = await res.json();
        const boundToolIds = new Set(boundList.map(b => b.toolId));

        checkboxes.forEach(cb => {
            const toolId = parseInt(cb.value);
            if (boundToolIds.has(toolId)) {
                cb.checked = true;
            }
        });
    } catch (err) {
        console.error('查询已有绑定失败:', err);
    }
}

async function saveKeyBindings() {
    const apiKeyId = document.getElementById('bind-key-select').value;
    if (!apiKeyId) return alert('请先选择 API-Key！');

    const checkboxes = document.querySelectorAll('.bind-checkbox');
    try {
        const oldRes = await fetch(`${API_BASE}/api-key-tools?apiKeyId=${apiKeyId}`);
        const oldList = await oldRes.json();
        for (const old of oldList) {
            await fetch(`${API_BASE}/api-key-tools?apiKeyId=${apiKeyId}&toolId=${old.toolId}`, { method: 'DELETE' });
        }

        for (const cb of checkboxes) {
            if (cb.checked) {
                const toolId = parseInt(cb.value);
                await fetch(`${API_BASE}/api-key-tools`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ apiKeyId: parseInt(apiKeyId), toolId })
                });
            }
        }

        alert('绑定规则保存成功！下一次 tools/list 即可实时生效！');
    } catch (err) {
        alert('保存绑定失败');
    }
}

// ==================== MCP 在线调试 Playground (符合 MCP Streamable HTTP 协议规范) ====================
async function loadPlaygroundApiKeys() {
    try {
        const res = await fetch(`${API_BASE}/api-keys`);
        const keys = await res.json();
        const select = document.getElementById('pg-key-select');
        select.innerHTML = '';
        keys.forEach(k => {
            select.innerHTML += `<option value="${k.apiKey}">${k.apiKey}</option>`;
        });
    } catch (err) {
        console.error(err);
    }
}

/**
 * 建立 MCP 协议连接握手 (Step 1: initialize + Step 2: notifications/initialized)
 * 关键：必须带上 Accept: application/json, text/event-stream Header
 */
async function connectMcp() {
    const apiKey = document.getElementById('pg-key-select').value;
    const output = document.getElementById('pg-console');
    const statusBadge = document.getElementById('pg-conn-status');

    if (!apiKey) return alert('请选择用于认证的 API-Key');

    output.textContent = `[Step 1] 正在向 /mcp 发起协议握手 (initialize)...\nHeader: Authorization: Bearer ${apiKey}\nHeader: Accept: application/json, text/event-stream\n\n`;

    try {
        // 1. 发起 initialize
        const initRes = await fetch('/mcp', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json, text/event-stream'
            },
            body: JSON.stringify({
                jsonrpc: '2.0',
                method: 'initialize',
                params: {
                    protocolVersion: '2024-11-05',
                    capabilities: {},
                    clientInfo: { name: 'mcp-web-playground', version: '1.0.0' }
                },
                id: 1
            })
        });

        if (!initRes.ok) {
            throw new Error(`HTTP ${initRes.status} ${initRes.statusText}`);
        }

        const initData = await initRes.json();
        output.textContent += `<- 握手响应 (initialize):\n${JSON.stringify(initData, null, 2)}\n\n`;

        // 2. 发起 notifications/initialized
        output.textContent += `[Step 2] 发送就绪通知 (notifications/initialized)...\n`;
        await fetch('/mcp', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json, text/event-stream'
            },
            body: JSON.stringify({
                jsonrpc: '2.0',
                method: 'notifications/initialized'
            })
        });

        currentMcpSession.apiKey = apiKey;
        currentMcpSession.connected = true;

        if (statusBadge) {
            statusBadge.innerHTML = '<span class="badge badge-success">🟢 MCP 已连接并就绪</span>';
        }
        output.textContent += `\n✅ MCP 协议连接握手成功！现在可以自由进行 tools/list 查询或 tools/call 调用！`;
        return true;
    } catch (err) {
        if (statusBadge) {
            statusBadge.innerHTML = '<span class="badge badge-disabled">🔴 连接失败</span>';
        }
        output.textContent += `\n❌ 握手失败: ${err.message}`;
        return false;
    }
}

async function testMcpList() {
    const apiKey = document.getElementById('pg-key-select').value;
    const output = document.getElementById('pg-console');

    // 如果还没有建立握手或更换了 API-Key，自动完成连接握手
    if (!currentMcpSession.connected || currentMcpSession.apiKey !== apiKey) {
        const ok = await connectMcp();
        if (!ok) return;
    }

    output.textContent += `\n\n==================================================\n[Step 3] 发起 tools/list 请求...\n`;

    try {
        const res = await fetch('/mcp', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json, text/event-stream'
            },
            body: JSON.stringify({
                jsonrpc: '2.0',
                method: 'tools/list',
                id: Date.now()
            })
        });

        const data = await res.json();
        output.textContent += `<- 响应结果 (tools/list):\n${JSON.stringify(data, null, 2)}`;
    } catch (err) {
        output.textContent += `\n❌ 请求异常: ${err.message}`;
    }
}

async function testMcpCall() {
    const apiKey = document.getElementById('pg-key-select').value;
    const toolName = document.getElementById('pg-tool-name').value.trim();
    const argsJson = document.getElementById('pg-tool-args').value.trim();
    const output = document.getElementById('pg-console');

    if (!toolName) return alert('请输入调用的工具名称！');

    let args = {};
    if (argsJson) {
        try {
            args = JSON.parse(argsJson);
        } catch (e) {
            return alert('工具参数 JSON 格式不合法！');
        }
    }

    if (!currentMcpSession.connected || currentMcpSession.apiKey !== apiKey) {
        const ok = await connectMcp();
        if (!ok) return;
    }

    output.textContent += `\n\n==================================================\n[Step 3] 发起 tools/call 请求 [${toolName}]...\n`;

    try {
        const res = await fetch('/mcp', {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json',
                'Accept': 'application/json, text/event-stream'
            },
            body: JSON.stringify({
                jsonrpc: '2.0',
                method: 'tools/call',
                params: {
                    name: toolName,
                    arguments: args
                },
                id: Date.now()
            })
        });

        const data = await res.json();
        output.textContent += `<- 响应结果 (tools/call):\n${JSON.stringify(data, null, 2)}`;
    } catch (err) {
        output.textContent += `\n❌ 调用失败: ${err.message}`;
    }
}

// Modal Toggle Helpers
function openModal(id) {
    document.getElementById(id).classList.add('active');
}

function closeModal(id) {
    document.getElementById(id).classList.remove('active');
}
