package zju.cst.aces.runner;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import okhttp3.*;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValiFixAgentDemo {

    // 验证标准枚举
    public enum ValidationStandard {
        STATIC_SYNTAX(
                "语法规则",
                "检查导包完整性、类型匹配一致性、变量/方法命名规范、括号/分号等语法符号完整性"
        ),
        DYNAMIC_LOGIC(
                "执行逻辑",
                "检查主流程覆盖完整性、边界条件验证有效性、异常场景处理正确性、状态变更逻辑合理性、方法调用顺序合法性"
        ),
        BUILD_AND_RUN(
                "编译运行",
                "检查依赖声明完整性、JDK/框架版本兼容性、构建插件配置正确性、类路径/资源路径有效性、入口方法存在性"
        ),
        EXTENSION(
                "扩展规则",
                "用于捕获未预定义的规则或错误类型，可由提示/日志动态补充说明"
        );

        private final String displayName;
        private final String description;

        ValidationStandard(String displayName, String description) {
            this.displayName = displayName;
            this.description = description;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getDescription() {
            return description;
        }
    }

    // 验证项内部类
    public static final class ValidationItem {
        private final ValidationStandard standard;
        private final String content;

        public ValidationItem(ValidationStandard standard, String content) {
            this.standard = standard;
            this.content = content;
        }

        public ValidationStandard getStandard() {
            return standard;
        }

        public String getContent() {
            return content;
        }
    }

    // 成员变量
    private OkHttpClient client;
    private Gson gson;
    private Map<Integer, Integer> methodErrorHistory = new HashMap<>();

    // 构造方法：初始化Gson和OkHttpClient
    public ValiFixAgentDemo() {
        this.gson = new Gson();
        this.client = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    // 初始验证计划
    public static List<ValidationItem> initialValidationPlan() {
        return Arrays.asList(
                new ValidationItem(ValidationStandard.STATIC_SYNTAX, ValidationStandard.STATIC_SYNTAX.getDescription()),
                new ValidationItem(ValidationStandard.DYNAMIC_LOGIC, ValidationStandard.DYNAMIC_LOGIC.getDescription()),
                new ValidationItem(ValidationStandard.BUILD_AND_RUN, ValidationStandard.BUILD_AND_RUN.getDescription())
        );
    }

    // 自适应验证计划（基于失败信息）
    public static List<ValidationItem> adaptiveValidationPlan(String failureSummary) {
        String extensionDesc = "根据上轮失败信息进行定向复查：" + failureSummary;
        return Arrays.asList(
                new ValidationItem(ValidationStandard.STATIC_SYNTAX, ValidationStandard.STATIC_SYNTAX.getDescription()),
                new ValidationItem(ValidationStandard.DYNAMIC_LOGIC, ValidationStandard.DYNAMIC_LOGIC.getDescription()),
                new ValidationItem(ValidationStandard.BUILD_AND_RUN, ValidationStandard.BUILD_AND_RUN.getDescription()),
                new ValidationItem(ValidationStandard.EXTENSION, extensionDesc)
        );
    }

    // 初始验证提示语（静态常量）
    private static final String INITIAL_VALIDATION_PROMPT;

    static {
        StringBuilder sb = new StringBuilder();
        sb.append("你是测试代码审查专家，请严格按照以下完整验证标准检查测试代码：\n");
        for (ValidationStandard standard : ValidationStandard.values()) {
            if (standard == ValidationStandard.EXTENSION) continue;
            sb.append(String.format("1. %s：%s\n", standard.getDisplayName(), standard.getDescription()));
        }
        sb.append("请逐项审查，并判断是否“通过验证”或“不通过验证”。格式如下：\n");
        sb.append("- 若全部验证通过，请返回：【全部验证通过】。\n");
        sb.append("- 若验证未通过，请使用如下格式反馈问题：\n");
        sb.append("```\n");
        sb.append("该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[详细描述]\n");
        sb.append("```\n");
        sb.append("请严格按照行号注释报告错误位置（示例：错误位置：[11]）。除给定格式外不要返回其他内容。\n");
        INITIAL_VALIDATION_PROMPT = sb.toString();
    }

    // API配置常量
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "deepseek/deepseek-r1";
    private static final String apiKey = "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";

    // 重新创建OkHttpClient（备用）
    private void recreateClient() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(300, TimeUnit.SECONDS)
                .readTimeout(300, TimeUnit.SECONDS)
                .writeTimeout(300, TimeUnit.SECONDS)
                .build();
    }

    // 读取源代码文件内容
    public static String readSourceCode(String sourceCodePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(sourceCodePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // 辅助方法：获取原代码中错误行的内容（用于上下文匹配）
    private static String getErrorLineContent(String fullCode, int errorLine) {
        String[] lines = fullCode.split("\n");
        if (errorLine < 1 || errorLine > lines.length) {
            return "";
        }
        return lines[errorLine - 1].trim();
    }

    // 辅助方法：计算字符串相似度（用于模糊匹配）
    private double calculateSimilarity(String s1, String s2) {
        if (s1.isEmpty() || s2.isEmpty()) return 0.0;
        int maxLen = Math.max(s1.length(), s2.length());
        int minLen = Math.min(s1.length(), s2.length());
        int matchCount = 0;
        for (int i = 0; i < minLen; i++) {
            if (s1.charAt(i) == s2.charAt(i)) {
                matchCount++;
            }
        }
        return (double) matchCount / maxLen;
    }

    // 构建API请求体
    private RequestBody buildRequestBody(String code, String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt + code);
        messages.add(message);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", MODEL);
        requestBodyMap.put("messages", messages);

        return RequestBody.create(gson.toJson(requestBodyMap),
                MediaType.get("application/json; charset=utf-8"));
    }

    // 构建API请求
    private Request buildRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    // 发送API请求并获取响应
    @SuppressWarnings("unchecked")
    public String sendRequestAndGetResponse(String code, String prompt) throws IOException {
        RequestBody requestBody = buildRequestBody(code, prompt);
        Request request = buildRequest(requestBody);
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Map<String, Object> responseMap = gson.fromJson(responseBody,
                        new TypeToken<Map<String, Object>>() {
                        }.getType());
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> messageObj = (Map<String, Object>) firstChoice.get("message");
                    if (messageObj != null) {
                        Object contentObj = messageObj.get("content");
                        return contentObj != null ? contentObj.toString() : "";
                    }
                }
                return "无有效内容";
            } else {
                throw new IOException("请求响应失败！状态码：" + response.code() +
                        ", 消息：" + (response.body() != null ? response.body().string() : "无响应体"));
            }
        }
    }

    /**
     * 优化修复代码提取逻辑：
     * 1. 增加原错误行内容匹配，解决行号偏移问题
     * 2. 扩大上下文匹配范围，允许修复代码包含错误行前后的关联代码
     * 3. 当行号不匹配时，通过内容相似度定位修复位置
     */
    public String extractCodeFromResponse(String response, int errorLine, String fullOriginalCode) {
        if (response == null || response.trim().isEmpty()) {
            return "";
        }

        // 清理响应中的无关文字，提取代码块
        String processed = response.replaceAll("修复成功|修复完成|已修复|修改如下|代码如下|【修复成功】|【修复失败】", "")
                .trim();
        Pattern pattern1 = Pattern.compile("```java\\s*(.*?)\\s*```", Pattern.DOTALL);
        Pattern pattern2 = Pattern.compile("```Java\\s*(.*?)\\s*```", Pattern.DOTALL);
        Pattern pattern3 = Pattern.compile("```\\s*(.*?)\\s*```", Pattern.DOTALL);
        Matcher matcher = pattern1.matcher(processed);
        if (matcher.find()) processed = matcher.group(1).trim();
        else if ((matcher = pattern2.matcher(processed)).find()) processed = matcher.group(1).trim();
        else if ((matcher = pattern3.matcher(processed)).find()) processed = matcher.group(1).trim();

        String[] originalLines = processed.split("\n");
        String errorLineContent = getErrorLineContent(fullOriginalCode, errorLine);

        // 情况1：修复代码为空，直接返回
        if (originalLines.length == 0) {
            return "";
        }

        // 情况2：通过内容匹配定位错误行（解决行号偏移）
        List<Integer> candidateLines = new ArrayList<>();
        for (int i = 0; i < originalLines.length; i++) {
            String line = originalLines[i].trim();
            // 匹配原错误行内容或带行号注释的修复行
            if (line.equals(errorLineContent) || line.contains("// 行" + errorLine)) {
                candidateLines.add(i);
            }
        }

        // 情况3：如果找到匹配行，提取该行及前后各2行上下文（确保修复完整性）
        if (!candidateLines.isEmpty()) {
            List<String> resultLines = new ArrayList<>();
            for (int candidate : candidateLines) {
                int start = Math.max(0, candidate - 2);
                int end = Math.min(originalLines.length - 1, candidate + 2);
                for (int i = start; i <= end; i++) {
                    resultLines.add(originalLines[i]);
                }
            }
            return String.join("\n", resultLines).trim();
        }

        // 情况4：未找到精确匹配，尝试模糊匹配（内容相似度>60%）
        for (int i = 0; i < originalLines.length; i++) {
            String line = originalLines[i].trim();
            if (calculateSimilarity(line, errorLineContent) > 0.6) {
                List<String> resultLines = new ArrayList<>();
                int start = Math.max(0, i - 2);
                int end = Math.min(originalLines.length - 1, i + 2);
                for (int j = start; j <= end; j++) {
                    resultLines.add(originalLines[j]);
                }
                return String.join("\n", resultLines).trim();
            }
        }

        // 情况5：所有匹配失败，返回完整修复代码并警告
        System.err.println("警告：未找到与错误行（" + errorLine + "）匹配的修复内容，返回全部修复代码");
        return processed;
    }

    /**
     * 优化修复隔离性校验：
     * 1. 允许因上下文修复导致的行数微小变化（±3行）
     * 2. 只校验非错误行的一致性，允许错误行及关联上下文修改
     */
    private boolean validateFixIsolation(String originalCode, String fixedCode, int errorLine) {
        String[] originalLines = originalCode.split("\n");
        String[] fixedLines = fixedCode.split("\n");

        // 允许行数变化范围：±3行（应对上下文修复）
        if (Math.abs(originalLines.length - fixedLines.length) > 3) {
            return false;
        }

        // 只校验非错误行的一致性（错误行及前后2行允许修改）
        int startIgnore = Math.max(0, errorLine - 3);
        int endIgnore = Math.min(originalLines.length - 1, errorLine + 2);

        for (int i = 0; i < originalLines.length && i < fixedLines.length; i++) {
            // 跳过错误行及关联上下文的校验
            if (i >= startIgnore && i <= endIgnore) {
                continue;
            }
            // 非相关行必须完全一致
            if (!originalLines[i].equals(fixedLines[i])) {
                return false;
            }
        }
        return true;
    }

    // 验证测试代码（通过文件路径）
    public String validateTestCode(String testCodePath) throws IOException {
        String testCode = readSourceCode(testCodePath);
        return sendRequestAndGetResponse(testCode, INITIAL_VALIDATION_PROMPT);
    }

    // 直接验证测试代码（传入代码字符串）
    public String validateTestCodeDirectly(String testCode) throws IOException {
        return sendRequestAndGetResponse(testCode, INITIAL_VALIDATION_PROMPT);
    }

    // 修复模式枚举
    private enum FixMode {LINE, METHOD, CLASS}

    // 代码片段定位类
    private static class CodeSpot {
        final String code;
        final FixMode mode;
        final int startLine;
        final int endLine;

        CodeSpot(String code, FixMode mode, int startLine, int endLine) {
            this.code = code;
            this.mode = mode;
            this.startLine = startLine;
            this.endLine = endLine;
        }
    }

    // 验证失败信息类
    private static class ValidationFailure {
        private final ValidationStandard validationStandard;
        private final String validationItem;
        private final int errorLine;
        private final String errorReason;
        private final String fullTestCode;

        public ValidationFailure(ValidationStandard validationStandard, String validationItem,
                                 int errorLine, String errorReason, String fullTestCode) {
            this.validationStandard = validationStandard;
            this.validationItem = validationItem;
            this.errorLine = errorLine;
            this.errorReason = errorReason;
            this.fullTestCode = fullTestCode;
        }

        public ValidationStandard getValidationStandard() {
            return validationStandard;
        }

        public String getValidationItem() {
            return validationItem;
        }

        public int getErrorLine() {
            return errorLine;
        }

        public String getErrorReason() {
            return errorReason;
        }

        public String getFullTestCode() {
            return fullTestCode;
        }
    }

    // 保存错误和修复信息到文件
    private void saveErrorAndFixInfo(String errorCode, String errorReason, String fixCode, String outputFilePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilePath, true))) {
            writer.write("错误代码：");
            String trimmedError = errorCode.trim();
            if (trimmedError.contains("\n")) {
                writer.write("\n" + trimmedError + "\n");
            } else {
                writer.write(trimmedError + "\n");
            }

            writer.write("===错误原因：" + errorReason.trim() + "\n");

            writer.write("===修复成功代码：");
            String trimmedFix = fixCode.trim();
            if (trimmedFix.contains("\n")) {
                writer.write("\n" + trimmedFix + "\n");
            } else {
                writer.write(trimmedFix + "\n");
            }

            writer.write("====================================\n\n");
        } catch (IOException e) {
            System.err.println("保存错误和修复信息时发生错误：" + e.getMessage());
        }
    }

    // 提取错误代码片段（指定行范围）
    private String extractErrorCodes(String fullCode, int startLine, int endLine) {
        String[] lines = fullCode.split("\n");
        if (startLine < 1 || endLine > lines.length || startLine > endLine) {
            return fullCode;
        }
        StringBuilder errorCode = new StringBuilder();
        for (int i = startLine - 1; i <= endLine - 1; i++) {
            errorCode.append(lines[i]).append("\n");
        }
        return errorCode.toString().trim();
    }

    // 处理验证失败信息（保存错误和修复内容）
    private void processFailure(ValidationFailure failure, String fixedCode) {
        String fullCode = failure.getFullTestCode();
        int errorLine = failure.getErrorLine();
        String errorCode = extractErrorCodes(fullCode, errorLine, errorLine);
        saveErrorAndFixInfo(
                errorCode,
                failure.getErrorReason(),
                fixedCode,
                "error_and_fix_info.txt"
        );
    }

    // 确定验证标准（根据验证项名称匹配）
    private ValidationStandard determineValidationStandard(String validationItem) {
        for (ValidationStandard standard : ValidationStandard.values()) {
            if (standard.getDisplayName().equals(validationItem) ||
                    standard.getDescription().contains(validationItem)) {
                return standard;
            }
        }
        return ValidationStandard.EXTENSION;
    }

    // 解析验证结果，提取失败信息
    public List<ValidationFailure> parseValidationResult(String validationResult, String fullTestCode) {
        List<ValidationFailure> failures = new ArrayList<>();

        if (validationResult.contains("【全部验证通过】") || validationResult.contains("全部验证通过")) {
            return failures;
        }

        Pattern pattern = Pattern.compile(
                "该测试代码在验证\\[([^\\]]+)\\]时失败，错误位置：\\[([\\d,]+)\\]，错误原因：(.*)",
                Pattern.MULTILINE | Pattern.DOTALL
        );
        Matcher matcher = pattern.matcher(validationResult);

        while (matcher.find()) {
            String validationItem = matcher.group(1).trim();
            String lineStr = matcher.group(2).trim();
            String errorReason = matcher.group(3).trim();

            ValidationStandard standard = determineValidationStandard(validationItem);
            int errorLine;
            if (lineStr.contains(",")) {
                String[] lines = lineStr.split(",");
                errorLine = Integer.parseInt(lines[0].trim());
            } else {
                errorLine = Integer.parseInt(lineStr);
            }

            failures.add(new ValidationFailure(standard, validationItem, errorLine, errorReason, fullTestCode));
        }

        // 兼容非标准格式的失败信息
        if (failures.isEmpty() && validationResult.contains("失败") && validationResult.contains("错误位置")) {
            try {
                Pattern linePattern = Pattern.compile("错误位置：\\[(\\d+)\\]");
                Matcher lineMatcher = linePattern.matcher(validationResult);
                int lineNumber = 0;
                if (lineMatcher.find()) {
                    lineNumber = Integer.parseInt(lineMatcher.group(1));
                }

                Pattern itemPattern = Pattern.compile("验证\\[([^\\]]+)\\]时失败");
                Matcher itemMatcher = itemPattern.matcher(validationResult);
                String item = "未知验证项";
                if (itemMatcher.find()) {
                    item = itemMatcher.group(1);
                }

                Pattern reasonPattern = Pattern.compile("错误原因：(.*)");
                Matcher reasonMatcher = reasonPattern.matcher(validationResult);
                String reason = "未知错误原因";
                if (reasonMatcher.find()) {
                    reason = reasonMatcher.group(1).trim();
                }

                ValidationStandard standard = determineValidationStandard(item);

                if (lineNumber > 0) {
                    failures.add(new ValidationFailure(standard, item, lineNumber, reason, fullTestCode));
                }
            } catch (Exception e) {
                failures.add(new ValidationFailure(ValidationStandard.EXTENSION, "未知验证项", 1, "解析错误信息失败: " + validationResult, fullTestCode));
            }
        }

        return failures;
    }

    // 判断错误行是否在方法内
    private boolean isLineInMethod(String[] lines, int errorLine) {
        int braceCount = 0;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
            }
            if (i + 1 == errorLine && braceCount > 0) {
                return true;
            }
        }
        return false;
    }

    // 确定修复模式（基于方法错误数）
    public FixMode determineFixMode(ValidationFailure failure, int methodErrorCount) {
        String[] lines = failure.getFullTestCode().split("\n");
        int errorLine = failure.getErrorLine();

        if (errorLine < 1 || errorLine > lines.length) {
            return FixMode.CLASS;
        }

        if (methodErrorCount >= 2) {
            return FixMode.METHOD;
        }

        return FixMode.LINE;
    }

    // 决定整体修复目标模式（基于错误分布）
    private FixMode decideTargetMode(List<ValidationFailure> failures) {
        // 1. 按方法分组统计错误（用方法起始行作为唯一标识）
        Map<Integer, Integer> methodErrorCount = new HashMap<>();
        String fullCode = failures.get(0).getFullTestCode();
        String[] lines = fullCode.split("\n");

        for (ValidationFailure f : failures) {
            int methodStart = findMethodStart(lines, f.getErrorLine() - 1);
            methodErrorCount.put(methodStart, methodErrorCount.getOrDefault(methodStart, 0) + 1);
        }

        // 2. 保存到错误历史
        methodErrorHistory = methodErrorCount;

        // 3. 统计有错误的方法数量
        int errorMethodCount = methodErrorCount.size();

        // 4. 应用策略判断
        if (errorMethodCount >= 3) {
            return FixMode.CLASS;  // 3+方法错误 → 类修复
        } else {
            // 检查是否有方法包含≥2个错误
            for (int count : methodErrorCount.values()) {
                if (count >= 2) {
                    return FixMode.METHOD;  // 存在多行错误方法 → 体修复
                }
            }
            // 所有方法都是单行错误，根据方法数量决定：1-2个方法→体修复，否则行修复（兜底）
            return errorMethodCount <= 2 ? FixMode.METHOD : FixMode.LINE;
        }
    }

    // 构建目标代码片段（基于修复模式）
    private CodeSpot buildTargetCodeSpot(String currentCode, List<ValidationFailure> failures, FixMode targetMode) {
        // 优先处理包含多行错误的方法
        String[] lines = currentCode.split("\n");
        for (ValidationFailure f : failures) {
            int methodStart = findMethodStart(lines, f.getErrorLine() - 1);
            int errorCount = methodErrorHistory.getOrDefault(methodStart, 0);
            if (errorCount >= 2) {
                return extractErrorCode(currentCode, f.getErrorLine(), FixMode.METHOD);
            }
        }

        // 没有多行错误方法时，处理第一个错误
        ValidationFailure chosen = failures.get(0);
        return extractErrorCode(currentCode, chosen.getErrorLine(), targetMode);
    }

    // 提取错误代码片段（基于修复模式）
    private CodeSpot extractErrorCode(String fullCode, int errorLine, FixMode mode) {
        String[] lines = fullCode.split("\n");
        int totalLines = lines.length;

        if (errorLine < 1 || errorLine > totalLines) {
            return new CodeSpot(fullCode, FixMode.CLASS, 1, totalLines);
        }

        int startLine, endLine;
        StringBuilder codeBuilder = new StringBuilder();

        switch (mode) {
            case LINE:
                startLine = errorLine;
                endLine = errorLine;
                codeBuilder.append(lines[errorLine - 1]).append("\n");
                break;

            case METHOD:
                startLine = findMethodStart(lines, errorLine - 1);
                endLine = findMethodEnd(lines, errorLine - 1);
                for (int i = startLine; i <= endLine && i < totalLines; i++) {
                    codeBuilder.append(lines[i]).append("\n");
                }
                startLine += 1;
                endLine += 1;
                break;

            case CLASS:
            default:
                startLine = 1;
                endLine = totalLines;
                codeBuilder.append(fullCode);
                break;
        }

        return new CodeSpot(codeBuilder.toString(), mode, startLine, endLine);
    }

    // 查找方法起始行（从错误行向上找）
    private int findMethodStart(String[] lines, int errorIndex) {
        for (int i = errorIndex; i >= 0; i--) {
            String line = lines[i].trim();
            if (line.contains("@Test") ||
                    (line.matches(".*(public|private|protected)\\s+.*void\\s+test.*\\(.*\\).*"))) {
                while (i > 0 && (lines[i - 1].trim().startsWith("@") ||
                        lines[i - 1].trim().isEmpty() ||
                        lines[i - 1].trim().startsWith("//"))) {
                    i--;
                }
                return i;
            }
        }
        return Math.max(0, errorIndex - 10);
    }

    // 查找方法结束行（从错误行向下找）
    private int findMethodEnd(String[] lines, int errorIndex) {
        int braceCount = 0;
        boolean inMethod = false;

        for (int i = errorIndex; i < lines.length; i++) {
            String line = lines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    inMethod = true;
                } else if (c == '}') {
                    braceCount--;
                    if (inMethod && braceCount == 0) {
                        return i;
                    }
                }
            }
        }

        return Math.min(lines.length - 1, errorIndex + 20);
    }

    // 构建修复提示语
    private static final String FIX_PROMPT_TEMPLATE =
            "你是专业的Java测试代码修复工程师，请使用行-体-类三级修复策略修复以下代码：\n" +
                    "错误信息：验证项[{validationItem}]，错误位置[{errorLine}]，错误原因[{errorReason}]\n" +
                    "修复范围：[{scopeDesc}]，仅对该范围内代码进行最小化修改，其他代码保持不变\n" +
                    "待修复代码：\n{codeFragment}\n" +
                    "请直接返回修复后的代码，不要添加任何额外说明、注释或标记。";

    private String buildFixPrompt(ValidationFailure failure, CodeSpot codeSpot) {
        String scopeDesc = codeSpot.mode == FixMode.LINE ? "仅错误行（严格隔离其他代码）" :
                (codeSpot.mode == FixMode.METHOD ? "仅错误所在方法体（不修改其他方法）" : "整个测试类");

        return FIX_PROMPT_TEMPLATE
                .replace("{validationItem}", failure.getValidationItem())
                .replace("{errorLine}", String.valueOf(failure.getErrorLine()))
                .replace("{errorReason}", failure.getErrorReason())
                .replace("{scopeDesc}", scopeDesc)
                .replace("{codeFragment}", codeSpot.code);
    }

    // 修复代码（核心修复逻辑）
    public String fixCode(ValidationFailure failure) throws IOException {
        String originalCode = failure.getFullTestCode();
        int errorLine = failure.getErrorLine();
        String[] lines = originalCode.split("\n");
        int methodStart = findMethodStart(lines, errorLine - 1);
        int methodErrorCount = methodErrorHistory.getOrDefault(methodStart, 1);

        // 根据当前方法错误数获取修复模式
        FixMode mode = determineFixMode(failure, methodErrorCount);
        CodeSpot codeSpot = extractErrorCode(originalCode, errorLine, mode);
        String fixPrompt = buildFixPrompt(failure, codeSpot);

        // 发送修复请求
        String response = sendRequestAndGetResponse(codeSpot.code, fixPrompt);
        // 提取修复后的代码（传入原完整代码用于内容匹配）
        String fixedFragment = extractCodeFromResponse(response, errorLine, originalCode);

        // 验证修复隔离性（仅行修复时校验，放宽限制）
        if (mode == FixMode.LINE && !validateFixIsolation(originalCode, fixedFragment, errorLine)) {
            // 不再直接抛异常，改为警告并返回原代码（或尝试体修复）
            System.err.println("行修复越界，自动切换为体修复策略");
            codeSpot = extractErrorCode(originalCode, errorLine, FixMode.METHOD);
            fixPrompt = buildFixPrompt(failure, codeSpot);
            response = sendRequestAndGetResponse(codeSpot.code, fixPrompt);
            fixedFragment = extractCodeFromResponse(response, errorLine, originalCode);
        }

        // 替换修复片段到完整代码
        return replaceCodeFragment(originalCode, codeSpot, fixedFragment);
    }

    // 重新生成整个类（类修复策略）
    private String regenerateClassFromError(ValidationFailure failure, String currentCode) throws IOException {
        StringBuilder prompt = new StringBuilder();
        prompt.append("根据以下错误重新生成完整的Java测试类：\n")
                .append("错误原因：").append(failure.getErrorReason()).append("\n")
                .append("原代码参考：\n").append(currentCode).append("\n")
                .append("请直接返回完整的可编译代码，不要添加任何额外说明。");

        String llmResponse = sendRequestAndGetResponse(currentCode, prompt.toString());
        return extractCodeFromResponse(llmResponse, failure.getErrorLine(), currentCode);
    }

    // 删除指定行范围的代码
    private String deleteLines(String fullCode, int startLine, int endLine) {
        String[] lines = fullCode.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            int lineNo = i + 1;
            if (lineNo < startLine || lineNo > endLine) {
                sb.append(lines[i]).append("\n");
            }
        }
        return sb.toString();
    }

    // 替换代码片段到完整代码中
    private String replaceCodeFragment(String fullCode, CodeSpot spot, String fixedFragment) {
        if (fixedFragment == null || fixedFragment.trim().isEmpty()) {
            return fullCode;
        }

        String[] fullLines = fullCode.split("\n");
        List<String> resultLines = new ArrayList<>(Arrays.asList(fullLines));
        // 删除原有片段
        for (int i = spot.endLine - 1; i >= spot.startLine - 1; i--) {
            if (i < resultLines.size()) {
                resultLines.remove(i);
            }
        }

        // 插入修复后的片段
        String[] fixedLines = fixedFragment.split("\n");
        int insertPos = spot.startLine - 1;
        for (int i = fixedLines.length - 1; i >= 0; i--) {
            if (insertPos < 0) insertPos = 0;
            if (insertPos > resultLines.size()) {
                resultLines.add(fixedLines[i]);
            } else {
                resultLines.add(insertPos, fixedLines[i]);
            }
        }

        // 拼接结果
        StringBuilder result = new StringBuilder();
        for (String line : resultLines) {
            result.append(line).append("\n");
        }
        return result.toString().trim();
    }

    // 构建自适应验证提示语（基于上轮失败信息）
    private String buildAdaptiveValidationPrompt(List<ValidationFailure> failures) {
        if (failures == null || failures.isEmpty()) {
            return INITIAL_VALIDATION_PROMPT;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("基于上轮错误进行定向验证：\n");
        for (ValidationFailure f : failures) {
            sb.append(String.format("重点检查：%s（位置：%d，原因：%s）\n",
                    f.getValidationItem(), f.getErrorLine(), f.getErrorReason()));
        }
        sb.append(INITIAL_VALIDATION_PROMPT);
        return sb.toString();
    }

    // 保存修复后的代码到文件
    private void saveFixedCodeToFile(String fixedCode, String filePath) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(fixedCode);
        } catch (IOException e) {
            System.err.println("保存修复后的代码到文件时发生错误：" + e.getMessage());
        }
    }

    /**
     * 验证并修复代码的主流程（适配新策略）
     */
    public String validateAndFixWithRetries(String testCode, String testCodePath, int maxRounds) throws IOException {
        String currentCode = testCode;
        String validationPrompt = INITIAL_VALIDATION_PROMPT;
        int round = 0;

        while (round < maxRounds) {
            // 1. 执行验证
            String validationResult = sendRequestAndGetResponse(currentCode, validationPrompt);
            // 打印原始验证结果（便于调试）
            System.out.println("\n---------- 第" + (round + 1) + "轮验证原始结果 ----------");
            System.out.println(validationResult);
            System.out.println("----------------------------------------");

            List<ValidationFailure> failures = parseValidationResult(validationResult, currentCode);

            // 2. 验证通过处理
            if (failures.isEmpty()) {
                saveFixedCodeToFile(currentCode, testCodePath);
                System.out.println("第" + (round + 1) + "轮验证通过，已保存修复后的代码！");
                return currentCode;
            }

            // 打印解析后的错误详情
            System.out.println("第" + (round + 1) + "轮验证失败，检测到" + failures.size() + "个错误，错误详情如下：");
            for (int i = 0; i < failures.size(); i++) {
                ValidationFailure f = failures.get(i);
                System.out.println((i + 1) + ". 验证项：" + f.getValidationItem() +
                        " | 错误行：" + f.getErrorLine() +
                        " | 错误原因：" + f.getErrorReason());
            }
            System.out.println("开始修复...");

            // 3. 确定修复模式
            FixMode targetMode = decideTargetMode(failures);
            ValidationFailure targetFailure = failures.get(0);

            // 4. 执行对应模式修复
            try {
                if (targetMode == FixMode.CLASS) {
                    System.out.println("采用类修复策略，重新生成整个测试类...");
                    currentCode = regenerateClassFromError(targetFailure, currentCode);
                } else {
                    System.out.println("采用" + (targetMode == FixMode.METHOD ? "体修复" : "行修复") + "策略，修复错误...");
                    currentCode = fixCode(targetFailure);
                }
                // 保存修复后的错误信息
                processFailure(targetFailure, currentCode);
            } catch (IOException e) {
                System.err.println("修复失败：" + e.getMessage());
                // 修复失败时尝试下一轮
            }

            // 5. 准备下一轮验证
            validationPrompt = buildAdaptiveValidationPrompt(failures);
            round++;
        }

        System.out.println("达到最大重试轮次" + maxRounds + "，返回当前修复状态的代码");
        // 保存最终状态的代码
        saveFixedCodeToFile(currentCode, testCodePath);
        return currentCode;
    }

    // 程序入口main方法
    public static void main(String[] args) {
        // 1. 配置参数：修改为你的测试代码路径
        String testCodePath = "D:\\fx\\jafx\\chatunitest-core\\src\\test\\java\\codeing\\AddExampleTest.java";
        int maxRounds = 3; // 最大修复重试轮次

        // 2. 创建实例
        ValiFixAgentDemo agent = new ValiFixAgentDemo();

        try {
            // 3. 读取测试代码
            String testCode = readSourceCode(testCodePath);
            System.out.println("成功读取测试代码，开始执行验证和修复流程...");

            // 4. 执行验证和修复主流程
            String fixedCode = agent.validateAndFixWithRetries(testCode, testCodePath, maxRounds);

            // 5. 输出结果
            System.out.println("\n验证修复流程完成，最终代码如下：");
            System.out.println("----------------------------------------");
            System.out.println(fixedCode);
            System.out.println("----------------------------------------");

        } catch (FileNotFoundException e) {
            System.err.println("错误：未找到测试代码文件，请检查路径是否正确：" + testCodePath);
        } catch (IOException e) {
            System.err.println("执行过程中发生IO错误：" + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("执行过程中发生异常：" + e.getMessage());
            e.printStackTrace();
        }
    }
}
