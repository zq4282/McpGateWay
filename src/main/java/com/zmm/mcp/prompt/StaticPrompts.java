package com.zmm.mcp.prompt;

import com.zmm.mcp.auth.ApiKeyAuthService;
import io.modelcontextprotocol.spec.McpSchema.Prompt;
import io.modelcontextprotocol.spec.McpSchema.PromptArgument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 静态预置 Prompts 容器：
 * 系统完全启动后，向 PromptRegistry 自动装载开箱即用的核心 Prompt 模版。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StaticPrompts {

    private final PromptRegistry promptRegistry;
    private final ApiKeyAuthService apiKeyAuthService;

    @EventListener(ApplicationReadyEvent.class)
    public void registerStaticPrompts() {
        log.info("开始向 PromptRegistry 注册静态与预置 Prompt 模版...");

        // 1. 代码审查 Prompt
        Prompt codeReviewDef = new Prompt(
                "code_review_prompt",
                "针对指定编程语言的源代码进行深度 Review，指出潜在 Bug、性能缺陷与优化建议",
                List.of(
                        new PromptArgument("code", "待审查的代码文本内容", true),
                        new PromptArgument("language", "编程语言类型（如 java, python, go 等）", false)
                )
        );
        String codeReviewTemplate = """
                你是一位资深高级架构师与代码审查专家。请对以下 {{language}} 源代码进行全面的 Code Review：
                
                ```{{language}}
                {{code}}
                ```
                
                请从以下几个维度输出审查报告：
                1. 潜在的 Bug 或边界条件问题
                2. 性能瓶颈与代码重构建议
                3. 安全隐患（如 SQL 注入、空指针风险等）
                4. 规范性与可读性优化
                """;
        promptRegistry.registerPrompt(new WorkflowPromptCallback(codeReviewDef, codeReviewTemplate, null, apiKeyAuthService));

        // 2. SQL 生成器 Prompt
        Prompt sqlGenDef = new Prompt(
                "sql_generator_prompt",
                "根据自然语言业务需求与数据库 Schema 生成高可读性且优化的 SQL 查询语句",
                List.of(
                        new PromptArgument("requirement", "自然语言查询需求描述", true),
                        new PromptArgument("table_schema", "相关数据表的结构 DDL 或字段说明", false)
                )
        );
        String sqlGenTemplate = """
                请根据以下业务需求与表结构，编写规范高效的 SQL 查询语句：
                
                【查询需求】
                {{requirement}}
                
                【表结构信息】
                {{table_schema}}
                
                请输出：
                1. 标准 SQL 查询语句（包含必要的 JOIN、WHERE 过滤与 GROUP BY 聚合）
                2. 简要的 SQL 索引优化与执行逻辑说明
                """;
        promptRegistry.registerPrompt(new WorkflowPromptCallback(sqlGenDef, sqlGenTemplate, null, apiKeyAuthService));

        // 3. 文本摘要 Prompt
        Prompt textSummaryDef = new Prompt(
                "text_summary_prompt",
                "对长文本或文档内容进行精炼的结构化摘要与核心要点提取",
                List.of(
                        new PromptArgument("text", "需要摘要的原始长文本内容", true),
                        new PromptArgument("max_words", "期望控制的最大字数或要点条数", false)
                )
        );
        String textSummaryTemplate = """
                请阅读以下文本内容，并对其进行精准的高质量摘要提炼：
                
                {{text}}
                
                要求：
                1. 提炼核心结论与关键要点（控制在 {{max_words}} 字或若干条核心 Bullet Points 左右）
                2. 保持客观准确，逻辑层次清晰
                """;
        promptRegistry.registerPrompt(new WorkflowPromptCallback(textSummaryDef, textSummaryTemplate, null, apiKeyAuthService));

        log.info("成功初始化并注册静态 Prompts 模版 3 个");
    }
}
