package com.nmaptoolkit.llm;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 内置提示词库：多套安全分析提示词模板
 */
public class PromptLibrary {

    /** 模板 id -> 模板信息 */
    public record Template(String id, String name, String description, String prompt) {}

    private static final Map<String, Template> TEMPLATES = new LinkedHashMap<>();

    static {
        put(new Template(
                "security_analysis",
                "安全综合分析",
                "对扫描结果进行全面安全分析，评估整体风险",
                """
                        你是一名资深网络安全专家。请基于以下 Nmap 扫描结果，输出一份专业的安全分析报告。

                        请从以下维度进行分析：
                        1. **总体风险评估**：根据开放端口和服务的危险程度，给出整体风险等级（低/中/高/严重）。
                        2. **关键风险点**：列出最需要关注的高危/严重端口与服务，说明其潜在威胁。
                        3. **暴露面分析**：分析目标暴露的攻击面（如远程管理、数据库、Web 服务等）。
                        4. **可能存在的漏洞**：结合服务版本，指出可能存在哪些已知漏洞（如弱口令、未授权访问、历史 CVE）。
                        5. **加固建议**：针对发现的风险，给出具体的修复和加固措施。

                        要求：
                        - 使用中文，条理清晰，分点列出。
                        - 专业准确，不要臆造不存在的漏洞。
                        - 若信息不足，明确指出需要进一步验证的内容。

                        扫描结果如下：
                        """));
        put(new Template(
                "vulnerability_assessment",
                "漏洞评估",
                "聚焦已知漏洞和版本脆弱性评估",
                """
                        你是一名漏洞研究专家。请根据以下 Nmap 扫描结果，进行漏洞评估分析。

                        请重点分析：
                        1. **版本脆弱性**：列出所有能识别出具体版本的服务，并判断该版本是否存在已知漏洞（可参考 CVE）。
                        2. **高危端口风险**：针对 445、3389、1433、3306、6379、2375 等敏感端口，说明具体风险。
                        3. **漏洞利用可能性**：评估这些漏洞被利用的难易程度和可能造成的后果。
                        4. **优先级排序**：按紧急程度对需要修复的问题排序。

                        要求：
                        - 中文输出，按优先级排列。
                        - 对每个版本相关的漏洞，标注可能的 CVE 编号（如能确定）。
                        - 不确定的信息要注明"需进一步验证"。

                        扫描结果如下：
                        """));
        put(new Template(
                "remediation",
                "修复建议",
                "输出可落地的加固与整改措施",
                """
                        你是一名安全加固专家。请根据以下 Nmap 扫描结果，输出一份可执行的整改加固方案。

                        请按以下结构输出：
                        1. **立即处理项（紧急）**：高危/严重端口和服务的处理措施。
                        2. **短期加固项**：建议在近期完成的配置加固。
                        3. **长期安全基线**：从制度、流程、技术上建立长期安全基线。
                        4. **每条措施包含**：问题描述 → 具体操作步骤 → 验证方法。

                        要求：
                        - 中文输出，步骤要具体可执行（如关闭服务的命令、修改配置的具体参数）。
                        - 区分"紧急"和"建议"两个优先级。
                        - 结合等保、企业安全实践给出建议。

                        扫描结果如下：
                        """));
        put(new Template(
                "report_summary",
                "报告摘要",
                "生成简洁的扫描结论摘要",
                """
                        你是一名安全报告撰写专家。请根据以下 Nmap 扫描结果，生成一份简洁的扫描结论摘要。

                        摘要需包含：
                        1. **一句话结论**：用一句话概括本次扫描的总体安全状况。
                        2. **关键发现**：列出 3-5 个最重要的发现。
                        3. **数据统计**：主机数、开放端口数、高危/严重端口数。
                        4. **建议**：给出 2-3 条最核心的后续行动建议。

                        要求：
                        - 中文输出，简洁明了，适合给非技术人员和管理层阅读。
                        - 控制在 300 字以内。

                        扫描结果如下：
                        """));
    }

    private static void put(Template t) {
        TEMPLATES.put(t.id(), t);
    }

    public static Template get(String id) {
        Template t = TEMPLATES.get(id);
        return t != null ? t : TEMPLATES.get("security_analysis");
    }

    public static Map<String, Template> all() {
        return TEMPLATES;
    }
}
