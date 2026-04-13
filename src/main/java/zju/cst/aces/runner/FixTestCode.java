package zju.cst.aces.runner;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;
//修复
public class FixTestCode {
    TestCoreDemo2 testCoreDemo2 = new TestCoreDemo2("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");
    private enum FixMode {LINE, METHOD,CLASS}  //定义修复模式 行-体-类
    private static class CodeSpot {      //修复模式代码范围
        final String code;
        final FixMode mode;
        CodeSpot(String code, FixMode mode) {
            this.code = code;
            this.mode = mode;
        }
    }

    // 1. 新增错误类型与级别枚举
    private enum ErrorType {
        // 行级错误
        ASSERTION_ERROR, SYNTAX_ERROR, SINGLE_LINE_LOGIC_ERROR,
        // 体级错误
        MULTI_LINE_CONFLICT, MISSING_STEPS, DEPENDENCY_ERROR,
        // 类级错误
        CLASS_STRUCTURE_ERROR, CROSS_METHOD_DEPENDENCY, MISSING_TEST_METHOD, STATIC_RESOURCE_ERROR,
        // 未识别错误
        UNKNOWN_ERROR
    }
    // 2. 扩展SpotError类，添加错误类型和初始修复级别字段
    private static class SpotError {
        final int lineNo;
        final int startCol;
        final int endCol;
        final String desc;
        Integer methodStartLine;
        Integer methodEndLine;
        ErrorType errorType; // 新增：错误类型
        FixMode initialFixMode; // 新增：初始修复级别

        // 行级错误构造器（扩展）
        SpotError(int lineNo, int startCol, int endCol, String desc) {
            this.lineNo = lineNo;
            this.startCol = startCol;
            this.endCol = endCol;
            this.desc = desc;
            this.errorType = identifyErrorType(desc); // 自动识别类型
            this.initialFixMode = getInitialFixMode(this.errorType); // 确定初始修复级别
        }

        // 方法级错误构造器（扩展）
        SpotError(int lineNo, int startCol, int endCol, String desc, int methodStartLine, int methodEndLine) {
            this(lineNo, startCol, endCol, desc);
            this.methodStartLine = methodStartLine;
            this.methodEndLine = methodEndLine;
        }

        // 核心：根据错误描述识别错误类型
        private ErrorType identifyErrorType(String desc) {
            desc = desc.toLowerCase();
            // 行级错误识别
            if (desc.contains("assert") || desc.contains("预期值") || desc.contains("实际值")) {
                return ErrorType.ASSERTION_ERROR;
            }
            if (desc.contains("分号") || desc.contains("括号") || desc.contains("语法") || desc.contains("拼写")) {
                return ErrorType.SYNTAX_ERROR;
            }
            if (desc.contains("if") || desc.contains("for") || desc.contains("while") && desc.contains("单行")) {
                return ErrorType.SINGLE_LINE_LOGIC_ERROR;
            }

            // 体级错误识别
            if (desc.contains("分支") || desc.contains("循环") || desc.contains("逻辑冲突")) {
                return ErrorType.MULTI_LINE_CONFLICT;
            }
            if (desc.contains("未初始化") || desc.contains("未调用") || desc.contains("缺失步骤")) {
                return ErrorType.MISSING_STEPS;
            }
            if (desc.contains("参数传递") || desc.contains("返回值处理") || desc.contains("依赖")) {
                return ErrorType.DEPENDENCY_ERROR;
            }

            // 类级错误识别
            if (desc.contains("导入") || desc.contains("注解") || desc.contains("类结构")) {
                return ErrorType.CLASS_STRUCTURE_ERROR;
            }
            if (desc.contains("共享变量") || desc.contains("跨方法") || desc.contains("多方法依赖")) {
                return ErrorType.CROSS_METHOD_DEPENDENCY;
            }
            if (desc.contains("缺失测试方法") || desc.contains("未覆盖") || desc.contains("测试方法不足")) {
                return ErrorType.MISSING_TEST_METHOD;
            }
            if (desc.contains("静态资源") || desc.contains("常量") || desc.contains("全局配置")) {
                return ErrorType.STATIC_RESOURCE_ERROR;
            }

            // 未识别错误
            return ErrorType.UNKNOWN_ERROR;
        }

        // 根据错误类型确定初始修复级别
        private FixMode getInitialFixMode(ErrorType type) {
            switch (type) {
                case ASSERTION_ERROR:
                case SYNTAX_ERROR:
                case SINGLE_LINE_LOGIC_ERROR:
                    return FixMode.LINE;
                case MULTI_LINE_CONFLICT:
                case MISSING_STEPS:
                case DEPENDENCY_ERROR:
                    return FixMode.METHOD;
                case CLASS_STRUCTURE_ERROR:
                case CROSS_METHOD_DEPENDENCY:
                case MISSING_TEST_METHOD:
                case STATIC_RESOURCE_ERROR:
                    return FixMode.CLASS;
                default:
                    return FixMode.LINE; // 未识别错误默认从行级开始
            }
        }
    }

    // 初级错误信息存储库
    /* ---------------- 初级错误信息存储库 ---------------- */
    // 错误记录实体类
    private static class ErrorRecord {
        String className;       // 类名
        String methodName;      // 方法名
        int errorLineNo;        // 错误行号（1-base）
        String errorType;       // 错误类型（如"断言错误"、"语法错误"）
        String errorContent;    // 错误具体描述
        String errorCode;       // 错误行的原始代码
        String fixedCode;       // 修复后的代码

        public ErrorRecord(String className, String methodName, int errorLineNo,
                           String errorType, String errorContent, String errorCode, String fixedCode) {
            this.className = className;
            this.methodName = methodName;
            this.errorLineNo = errorLineNo;
            this.errorType = errorType;
            this.errorContent = errorContent;
            this.errorCode = errorCode;
            this.fixedCode = fixedCode;
        }
    }

    // 内存存储库（使用List作为临时存储）
    private List<ErrorRecord> errorRepository = new ArrayList<>();

    // 添加错误记录到存储库
   /* private void addToRepository(Map<String, String> classInfo, SpotError error,
                                 String errorCode, String fixedCode, String methodName) {
        // 从错误描述中提取错误类型（简单示例）
        String errorType = error.desc.contains("预期值") ? "断言错误" : "未知错误";

        ErrorRecord record = new ErrorRecord(
                classInfo.getOrDefault("类名", ""),
                methodName,
                error.lineNo,
                errorType,
                error.desc,
                errorCode,
                fixedCode
        );
        errorRepository.add(record);
        System.out.println("已记录错误信息到存储库：" + record.className + "第" + record.errorLineNo + "行");
    }*/
    // 记录错误修复信息
    // 1. 修复ErrorRecord无参构造器问题（原类只有带参构造器）
    private void addToRepository(Map<String, String> classInfo, SpotError error, String errorCode, String fixedCode, String methodName) {
        ErrorRecord record = new ErrorRecord(
                classInfo.get("类名"),
                methodName,
                error.lineNo,
                error.desc.contains("语法") ? "语法错误" : "逻辑错误",
                error.desc,  // 补充errorContent参数（原遗漏）
                errorCode,
                fixedCode
        );
        errorRepository.add(record);
    }

    // 从存储库查询错误记录（示例方法）
    private List<ErrorRecord> queryRecordsByClass(String className) {
        return errorRepository.stream()
                .filter(r -> r.className.equals(className))
                .collect(Collectors.toList());
    }

    // 提取整个类的代码
    private String extractClassCode(Map<String, String> classInfo) throws IOException {
        Path path = Paths.get(getTestFilePath(classInfo));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
    private void replaceClass(Map<String, String> classInfo, String newClassCode) throws IOException {
        Path path = Paths.get(getTestFilePath(classInfo));
        Files.write(path, newClassCode.getBytes(StandardCharsets.UTF_8));
        System.out.println("类级替换完成");
    }

    /*// 类级修复核心方法
    private boolean attemptClassLevelFix(SpotError error, Map<String, String> classInfo) throws IOException {
        // 1. 提取整个类代码
        String classCode = extractClassCode(classInfo);
        if (classCode.isEmpty()) {
            System.err.println("类级修复失败：无法提取类代码");
            return false;
        }

        // 2. 生成类级修复提示
        String prompt = "以下测试类的第" + error.lineNo + "行存在错误，行级和方法级修复均失败，请修复整个类：\n" +
                "错误描述：" + error.desc + "\n" +
                "类完整代码：\n```java\n" + classCode + "\n```\n" +
                "修复要求：\n" +
                "1. 确保解决指定行的错误\n" +
                "2. 保持类结构、其他方法和导入语句完整\n" +
                "3. 仅返回修复后的完整类代码（无解释）";

        // 3. 调用LLM获取修复结果
        String fixedClassCode = sendRequestAndGetResponse0("", prompt).trim();
        fixedClassCode = cleanCodeBlock(fixedClassCode);
        if (fixedClassCode.isEmpty() || !isSyntaxValid(fixedClassCode)) {
            System.err.println("类级修复失败：修复结果无效");
            return false;
        }

        // 4. 替换整个类代码
        Path path = Paths.get(getTestFilePath(classInfo));
        Files.writeString(path, fixedClassCode, StandardCharsets.UTF_8);
        System.out.println("类级修复完成：替换整个类代码");
        return true;
    }*/
    // 新增：类级修复入口（处理类级错误，如缺少方法、依赖问题）
    private boolean attemptClassLevelFix(Map<String, String> classInfo, List<SpotError> remainingErrors) throws IOException {
        String classCode = extractClassCode(classInfo);
        String errorDesc = "以下错误需通过类级修复解决：\n";
        for (SpotError e : remainingErrors) {
            errorDesc += "- 行" + e.lineNo + "：" + e.desc + "\n";
        }
        errorDesc += "请补充缺失的测试方法，修复所有语法错误，并确保覆盖被测试类的全部方法。";

        String prompt = "你是专业Java测试工程师，请修复整个测试类的错误：\n" +
                "错误信息：" + errorDesc + "\n" +
                "当前测试类代码：\n```java\n" + classCode + "\n```\n" +
                "要求：1. 补充缺失的测试方法；2. 修复所有语法错误；3. 保持JUnit 5风格；4. 仅返回完整测试类代码。";

        String fixedClassCode = sendRequestAndGetResponse0("", prompt).trim();
        fixedClassCode = cleanCodeBlock(fixedClassCode);
        if (fixedClassCode.isEmpty() || !isSyntaxValid(fixedClassCode)) {
            return false;
        }

        replaceClass(classInfo, fixedClassCode);
        return true;
    }

   /* *//* 错误位置记录 *//*
    private static class SpotError {
        final int lineNo;       // 1-base行号
        final int startCol;     // 行内起始列（0-base）
        final int endCol;       // 行内结束列（0-base）
        final String desc;      // 错误描述
        Integer methodStartLine; // 方法起始行（1-base，方法级错误时使用）
        Integer methodEndLine;   // 方法结束行（1-base，方法级错误时使用）

        // 行级错误构造器
        SpotError(int lineNo, int startCol, int endCol, String desc) {
            this.lineNo = lineNo;
            this.startCol = startCol;
            this.endCol = endCol;
            this.desc = desc;
        }

        // 方法级错误构造器（含行级信息）  323123
        SpotError(int lineNo, int startCol, int endCol, String desc, int methodStartLine, int methodEndLine) {
            this(lineNo, startCol, endCol, desc);
            this.methodStartLine = methodStartLine;
            this.methodEndLine = methodEndLine;
        }
    }*/


    /* 错误类型区分 */
    private static class TrackedError {
        SpotError error;
        boolean isInitial; // 是否为初始错误（非修复后新增）
        boolean fixed;     // 是否已修复

        TrackedError(SpotError error, boolean isInitial) {
            this.error = error;
            this.isInitial = isInitial;
            this.fixed = false;
        }
    }

    /* 解析单条错误的验证报告，提取错误位置（行号）、错误内容和描述 */
    private SpotError parseSpotError(String validateReport) {
        // 正则匹配：错误位置：[行号]，错误内容：[错误片段]，错误原因：[描述]
        Pattern p = Pattern.compile(
                "错误位置\\s*[:：]\\s*\\[(\\d+)\\]\\s*[,，]\\s*错误内容\\s*[:：]\\s*\\[(.*?)\\]\\s*[,，]\\s*错误原因\\s*[:：]\\s*\\[(.*?)\\]",
                Pattern.DOTALL
        );
        Matcher m = p.matcher(validateReport);
        if (!m.find()) return null;

        int lineNo = Integer.parseInt(m.group(1));
        String errorContent = m.group(2).trim();
        String desc = m.group(3).trim();

        // 计算错误内容在该行的起止列（模拟逻辑，实际可根据原代码行匹配）
        int startCol = -1;
        int endCol = -1;

        return new SpotError(lineNo, startCol, endCol, desc);
    }

    // 解析多条错误的验证报告，依次提取所有错误的行号和描述
    private List<SpotError> parseMultipleSpotErrors(String validateReport) {
        List<SpotError> errors = new ArrayList<>();
        // 适配控制台输出的错误格式（如："错误位置：[11]，错误原因：2 + 3 的实际结果应为5，但expected值为6"）
        Pattern p = Pattern.compile(
                "错误位置\\s*[:：]\\s*\\[(\\d+)\\]\\s*[,，]\\s*错误原因\\s*[:：]\\s*(.*?)(?=\\n|$)",
                Pattern.DOTALL
        );
        Matcher m = p.matcher(validateReport);
        while (m.find()) {
            int lineNo = Integer.parseInt(m.group(1).trim()); // 提取行号
            String desc = m.group(2).trim();
            // 从错误原因中提取预期值和实际值（辅助LLM修复）
            desc += "（请修改assertEquals的预期值为正确结果）";
            errors.add(new SpotError(lineNo, -1, -1, desc)); // 行列范围暂时不填，优先保证行号正确
        }
        return errors;
    }

    // 将错误按所在方法分组 便于批量处理同一方法内的错误  323123
    private Map<String, List<SpotError>> groupErrorsByMethod(List<SpotError> errors, Map<String, String> classInfo) throws IOException {
        Map<String, List<SpotError>> methodErrors = new HashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(getTestFilePath(classInfo)));
        for (SpotError err : errors) {
            int[] range = expandToMethod(lines, Math.max(0, err.lineNo - 1));
            String methodKey = range[0] + "-" + range[1];
            methodErrors.computeIfAbsent(methodKey, k -> new ArrayList<>()).add(err);
        }
        return methodErrors;
    }

    // 5. 修改fixMultipleSpots方法，适配新的修复流程
    public String fixMultipleSpots(String validateReport, Map<String, String> classInfo) throws IOException {
        List<SpotError> errors = parseMultipleSpotErrors(validateReport);
        if (errors.isEmpty()) return null;

        errors.sort((e1, e2) -> Integer.compare(e2.lineNo, e1.lineNo));

        for (SpotError error : errors) {
            boolean fixed = attemptFix(error, classInfo); // 直接调用新的attemptFix方法
            if (!fixed) {
                System.err.println("所有级别修复均失败：" + error.lineNo);
            }
        }

        // 修复后验证逻辑不变
        String fixedCode = new String(Files.readAllBytes(Paths.get(getTestFilePath(classInfo))), StandardCharsets.UTF_8);
        String reValidate = validateTest(fixedCode);
        System.out.println("fixMultipleSpots修复完成再次验证：" + reValidate);

        return fixedCode;
    }
   /* *//* ---------------- 多错误修复主入口,按行号分步修复逻辑  ---------------- *//*
    public String fixMultipleSpots(String validateReport, Map<String, String> classInfo) throws IOException {
        List<SpotError> errors = parseMultipleSpotErrors(validateReport);
        if (errors.isEmpty()) return null;

        // 按行号倒序处理，避免替换后行号偏移
        errors.sort((e1, e2) -> Integer.compare(e2.lineNo, e1.lineNo));

        for (SpotError error : errors) {
            // 1. 先尝试行内片段修复
            CodeSpot spot = extractSpotByStrategy(classInfo, error);
            if (spot.code.isEmpty()) continue;

            String prompt = FIX_SPOT_PROMPT
                    .replace("{errorPrompt}", error.desc)
                    .replace("{spotCode}", spot.code); // 仅传入错误片段

            String newSpot = sendRequestAndGetResponse0("", prompt).trim();
            newSpot = cleanCodeBlock(newSpot);

            // 2. 若行内修复无效，升级为方法级修复
            if (newSpot.isEmpty() || newSpot.equals("null") || !isSyntaxValid(newSpot)) {
                System.err.println("行级修复失败，尝试方法级修复：" + error.lineNo);
                // 重新提取方法体
                spot = new CodeSpot(
                        extractMethodCode(classInfo, error.methodStartLine, error.methodEndLine),
                        FixMode.METHOD
                );
                prompt = FIX_SPOT_PROMPT
                        .replace("{errorPrompt}", error.desc + "（行级修复失败，尝试修复整个方法）")
                        .replace("{spotCode}", spot.code);
                newSpot = sendRequestAndGetResponse0("", prompt).trim();
                newSpot = cleanCodeBlock(newSpot);
                if (newSpot.isEmpty() || !isSyntaxValid(newSpot)) {
                    System.err.println("方法级修复仍失败，尝试类级修复：" + error.lineNo);
                    // 将单个error封装为List传入
                    List<SpotError> errorList = Collections.singletonList(error);
                    boolean classFixed = attemptClassLevelFix(classInfo, errorList);
                    if (!classFixed) {
                        System.err.println("类级修复仍失败，跳过：" + error.lineNo);
                        continue;
                    }
                }
            }

            // 3. 基于记录的位置放回修复结果
            replaceSpotByStrategy(classInfo, error, newSpot, spot.mode);
        }

        // 修复后再次验证
        String fixedCode = new String(Files.readAllBytes(Paths.get(getTestFilePath(classInfo))), StandardCharsets.UTF_8);
        String reValidate = validateTest(fixedCode);
        System.out.println("fixMultipleSpots修复完成再次验证：" + reValidate);

        return fixedCode;
    }*/

    /* ---------------- 路径处理 ---------------- */
    private String getTestFilePath(Map<String, String> classInfo) {
        String root = System.getProperty("user.dir");
        String packagePath = classInfo.getOrDefault("包路径", "").replace('.', File.separatorChar);
        String className = classInfo.getOrDefault("类名", "") + "Test.java";
        return Paths.get(root, "src", "test", "java", packagePath, className).toString();
    }

    /*// 获取被测试类源码（仅用于必要场景，当前修复提示不传递）
    private String getTestedClassCode(Map<String, String> classInfo) throws IOException {
        String root = System.getProperty("user.dir");
        String packagePath = classInfo.getOrDefault("包路径", "").replace('.', File.separatorChar);
        String className = classInfo.getOrDefault("类名", "") + ".java";
        Path path = Paths.get(root, "src", "main", "java", packagePath, className);
        if (!Files.exists(path)) {
            return "被测试类源码不存在";
        }
        return Files.readString(path, StandardCharsets.UTF_8);
    }*/

    // 定位错误范围
    private int[] expandToMethod(List<String> lines, int idx) {
        int start = idx;
        // 向上查找@Test注解或测试方法定义（void testXXX()）
        while (start > 0) {
            String line = lines.get(start).trim();
            if (line.startsWith("@Test") || (line.startsWith("void") && line.contains("test") && line.contains("()"))) {
                break;
            }
            start--;
        }
        if (start < 0) start = idx;

        // 向下匹配大括号（忽略字符串内的括号）
        int end = start;
        int brace = 0;
        boolean inString = false;
        for (int i = start; i < lines.size(); i++) {
            String line = lines.get(i);
            for (char c : line.toCharArray()) {
                if (c == '"') {
                    inString = !inString; // 标记字符串范围
                } else if (!inString) {
                    if (c == '{') brace++;
                    else if (c == '}') brace--;
                }
            }
            if (brace == 0) {
                end = i;
                break;
            }
        }
        return new int[]{start, end};
    }

    /* ---------------- 代码片段提取（隔离错误代码） ---------------- */
    // 提取行内具体错误片段（基于行列范围）
    private String extractErrorSnippet(List<String> lines, SpotError error) {
        if (error.lineNo < 1 || error.lineNo > lines.size()) {
            return "";
        }
        String line = lines.get(error.lineNo - 1);
        // 若行列范围有效，提取片段；否则返回整行（降级处理）
        if (error.startCol >= 0 && error.endCol < line.length()) {
            return line.substring(error.startCol, error.endCol + 1);
        } else {
            return line;
        }
    }

    // 提取指定错误方法体代码
    private String extractMethodCode(Map<String, String> classInfo, Integer startLine, Integer endLine) throws IOException {
        if (startLine == null || endLine == null) return "";
        List<String> lines = Files.readAllLines(Paths.get(getTestFilePath(classInfo)));
        int startIdx = startLine - 1;
        int endIdx = endLine - 1;
        if (startIdx < 0 || endIdx >= lines.size() || startIdx > endIdx) {
            return "";
        }
        return String.join("\n", lines.subList(startIdx, endIdx + 1));
    }

    // 基于错误信息提取待修复片段
    private CodeSpot extractSpotByStrategy(Map<String, String> classInfo, SpotError error) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(getTestFilePath(classInfo)));
        // 优先提取行内错误片段
        String errorSnippet = extractErrorSnippet(lines, error);
        if (!errorSnippet.isEmpty()) {
            return new CodeSpot(errorSnippet, FixMode.LINE);
        }
        // 若片段提取失败，提取所在方法（并记录方法范围到error）
        int[] methodRange = expandToMethod(lines, error.lineNo - 1);
        error.methodStartLine = methodRange[0] + 1; // 转为1-base
        error.methodEndLine = methodRange[1] + 1;
        String methodCode = String.join("\n", lines.subList(methodRange[0], methodRange[1] + 1));
        return new CodeSpot(methodCode, FixMode.METHOD);
    }

    /* ---------------- 清理和校验输出代码 ---------------- */
    private String cleanCodeBlock(String code) {
        if (code == null) return "";
        code = code.replaceAll("^\\s*```java\\s*", "");
        code = code.replaceAll("\\s*```\\s*$", "");
        return code.trim();
    }

    // 简单校验代码大括号语法
    private boolean isSyntaxValid(String code) {
        int brace = 0;
        boolean inString = false;
        for (char c : code.toCharArray()) {
            if (c == '"') {
                inString = !inString;
            } else if (!inString) {
                if (c == '{') brace++;
                else if (c == '}') brace--;
            }
            if (brace < 0) return false; // 右括号多于左括号
        }
        return brace == 0; // 括号必须平衡
    }

    //  根据修复模式精准替换错误代码
    private void replaceSpotByStrategy(Map<String, String> classInfo, SpotError error, String newSpot, FixMode mode) throws IOException {
        newSpot = cleanCodeBlock(newSpot);
        Path path = Paths.get(getTestFilePath(classInfo));
        List<String> lines = Files.readAllLines(path);

        if (mode == FixMode.LINE) {
            // 行内片段替换（保留其他正确内容）
            int lineIdx = error.lineNo - 1;
            if (lineIdx < 0 || lineIdx >= lines.size()) {
                return;
            }
            String originalLine = lines.get(lineIdx);
            // 若行列范围有效，替换片段；否则替换整行
            if (error.startCol >= 0 && error.endCol < originalLine.length()) {
                String newLine = originalLine.substring(0, error.startCol)
                        + newSpot
                        + originalLine.substring(error.endCol + 1);
                lines.set(lineIdx, newLine);
            } else {
                // 降级：替换整行（保留缩进）
                String indent = originalLine.replaceAll("\\S.*", "");
                lines.set(lineIdx, indent + newSpot);
            }
        } else {
            // 方法级替换（基于记录的methodStartLine和methodEndLine）
            if (error.methodStartLine == null || error.methodEndLine == null) {
                return;
            }
            int startIdx = error.methodStartLine - 1;
            int endIdx = error.methodEndLine - 1;
            if (startIdx < 0 || endIdx >= lines.size() || startIdx > endIdx) {
                return;
            }
            // 替换整个方法体
            List<String> newLines = Arrays.asList(newSpot.split("\n"));
            String methodIndent = lines.get(startIdx).replaceAll("\\S.*", "");
            List<String> indentedLines = new ArrayList<>();
            for (String line : newLines) {
                indentedLines.add(methodIndent + line);
            }
            lines.subList(startIdx, endIdx + 1).clear();
            lines.addAll(startIdx, indentedLines);
        }

        Files.write(path, lines, StandardCharsets.UTF_8);

        String errorCode = getOriginalLine(classInfo, error.lineNo); // 获取错误行原始代码
        String methodName = "未知方法"; // 可通过expandToMethod获取方法名（需额外实现）
        addToRepository(classInfo, error, errorCode, newSpot, methodName);

    }

    /* ---------------- 修复提示模板（仅含错误片段） ---------------- */
    private static final String FIX_SPOT_PROMPT =
            "你是一名专业的java测试代码修复工程师，请修复以下测试代码片段的错误\n" +
                    "错误信息：{errorPrompt}\n" +
                    "请根据错误信息修复错误位置代码，保持代码其他部分不变。\n" +
                    "错误片段：\n```java\n{spotCode}\n```\n" +
                    "仅返回修改后的代码片段（无解释、注释，保持语法正确）：";

    private static final String generatePrompt =
            "根据以下待测试类的信息生成测试代码：\n" +
                    "包路径：{packagePath}\n" +
                    "类名：{className}\n" +
                    "全部方法信息：\n" +
                    "{allMethodsInfo}\n" +
                    "你是一位资深Java测试人员，请为上述类中的焦点方法编写JUnit 5风格且系统编译器版本为8的单元测试代码，除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等），要求如下：\n" +
                    "1. 测试用例应覆盖所有代码路径，包括分支条件和边界情况。\n" +
                    "2. 测试代码必须无编译错误。如有必要，可以使用反射测试私有方法或字段。\n" +
                    "3. 请根据注释中提供的执行路径信息生成相应测试。\n" +
                    "4. 测试类名为[类名]Test，若有命名冲突，则依次命名为[类名]Test1、Test2等。\n" +
                    "5. 正确设置包路径与包导入；禁止使用 `org.junit.jupiter.params` 包内容。\n\n" +
                    "请直接输出完整的测试代码，不包含任何注释、解释或思考过程。以下为待测试的源代码：\n";

    /* ---------------- LLM调用 ---------------- */
    private String sendRequestAndGetResponse0(String content, String prompt) throws IOException {
        return testCoreDemo2.sendRequestAndGetResponse0(content, prompt);
    }

    /* ---------------- 单行修复优化 ---------------- */
    // 修改replaceExactLine，增加文件写入日志，验证是否执行替换
    private void replaceExactLine(Map<String, String> classInfo, int lineNo, String newLine) throws IOException {
        Path path = Paths.get(getTestFilePath(classInfo));
        // 验证路径是否正确
        if (!Files.exists(path)) {
            System.err.println("替换失败：测试文件不存在，路径=" + path);
            return;
        }
        List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
        if (lineNo < 1 || lineNo > lines.size()) {
            System.err.println("替换失败：行号无效，lineNo=" + lineNo);
            return;
        }
        // 保留原行缩进
        String originalLine = lines.get(lineNo - 1);
        String indent = originalLine.replaceAll("\\S.*", "");
        String modifiedLine = indent + newLine;
        lines.set(lineNo - 1, modifiedLine);
        // 写入文件并打印日志
        Files.write(path, lines, StandardCharsets.UTF_8);
        System.out.println("已替换行 " + lineNo + "：原内容=[" + originalLine + "]，新内容=[" + modifiedLine + "]");
    }


    private String getOriginalLine(Map<String, String> classInfo, int lineNo) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(getTestFilePath(classInfo)));
        if (lineNo < 1 || lineNo > lines.size()) return "";
        return lines.get(lineNo - 1).replaceFirst("^\\s*\\d+\\s+", "");
    }

    public String fixSingleLine(int lineNo, Map<String, String> classInfo) throws IOException {
        String originalLine = getOriginalLine(classInfo, lineNo);
        if (originalLine.isEmpty()) return null;

        String prompt = FIX_SPOT_PROMPT
                .replace("{errorPrompt}", "预期值与实际不符")
                .replace("{spotCode}", originalLine);

        String newLine = sendRequestAndGetResponse0("", prompt).trim();
        newLine = cleanCodeBlock(newLine);
        if (newLine.isEmpty() || newLine.equals("null") || !isSyntaxValid(newLine)) {
            System.err.println("单行修复无效，跳过行：" + lineNo);
            return null;
        }

        replaceExactLine(classInfo, lineNo, newLine);
        return new String(Files.readAllBytes(Paths.get(getTestFilePath(classInfo))), StandardCharsets.UTF_8);
    }

    /* ---------------- 单错误修复主入口 ---------------- */
    public String fixSingleSpot(String validateReport, Map<String, String> classInfo) throws IOException {
        SpotError err = parseSpotError(validateReport);
        if (err == null) return null;

        CodeSpot spot = extractSpotByStrategy(classInfo, err);
        if (spot.code.isEmpty()) return null;

        String prompt = FIX_SPOT_PROMPT
                .replace("{errorPrompt}", err.desc)
                .replace("{spotCode}", spot.code);

        String newSpot = sendRequestAndGetResponse0("", prompt).trim();
        newSpot = cleanCodeBlock(newSpot);
        if (newSpot.isEmpty() || newSpot.equals("null") || !isSyntaxValid(newSpot)) {
            System.err.println("单错误修复无效，尝试方法级修复");
            // 升级为方法级修复
            spot = new CodeSpot(
                    extractMethodCode(classInfo, err.methodStartLine, err.methodEndLine),
                    FixMode.METHOD
            );
            prompt = FIX_SPOT_PROMPT
                    .replace("{errorPrompt}", err.desc + "（行级修复失败，尝试修复整个方法）")
                    .replace("{spotCode}", spot.code);
            newSpot = sendRequestAndGetResponse0("", prompt).trim();
            newSpot = cleanCodeBlock(newSpot);
            if (newSpot.isEmpty() || !isSyntaxValid(newSpot)) {
                System.err.println("方法级修复仍失败");
                return null;
            }
        }

        replaceSpotByStrategy(classInfo, err, newSpot, spot.mode);
        Path path = Paths.get(getTestFilePath(classInfo));
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }

    /* ---------------- 测试生成相关 ---------------- */
    public String generateTestForMethod(String srcJava, int methodIdx) throws IOException {
        String analyze = testCoreDemo2.analyzeSourceCode(srcJava);
        Map<String, String> info = testCoreDemo2.extractClassInfo3(analyze);
        String testCode;
        if (methodIdx <= 0) {
            testCode = testCoreDemo2.generateTestCode2(srcJava, analyze, "");
        } else {
            String methodInfo = findMethodInfoByIndex(info.get("allMethodsInfo"), methodIdx);
            if (methodInfo == null) return null;
            String prompt = generatePrompt
                    .replace("{packagePath}", info.get("包路径"))
                    .replace("{className}", info.get("类名"))
                    .replace("{allMethodsInfo}", methodInfo);
            testCode = sendRequestAndGetResponse0(srcJava, prompt);
        }
        TestCore.createTestClass2(testCode, info);
        return testCode;
    }

    private String findMethodInfoByIndex(String allInfo, int idx) {
        Pattern p = Pattern.compile("序号：\\s*" + idx + "\\s*\n(.*?)(?=\\n序号|\\z)", Pattern.DOTALL);
        Matcher m = p.matcher(allInfo);
        return m.find() ? m.group(1) : null;
    }

    public String validateTest(String testCode) throws IOException {
        return testCoreDemo2.validateTestCode2(testCode);
    }




    /* ---------------- 核心修复逻辑：处理初始错误+新出现的错误 ---------------- */
    public String fixWithNewErrorHandling(String initialReport, Map<String, String> classInfo) throws IOException {
        // 1. 解析初始错误（仅错误1和2）
        List<SpotError> initialErrors = parseMultipleSpotErrors(initialReport);
        if (initialErrors.isEmpty()) return null;
        initialErrors.sort((e1, e2) -> Integer.compare(e2.lineNo, e1.lineNo));

        // 跟踪初始错误（标记为isInitial=true）
        List<TrackedError> trackedErrors = new ArrayList<>();
        for (SpotError e : initialErrors) {
            trackedErrors.add(new TrackedError(e, true));
        }

        // 2. 修复初始错误
        for (TrackedError te : trackedErrors) {
            if (te.isInitial && !te.fixed) {
                te.fixed = attemptFix(te.error, classInfo);
                if (!te.fixed) {
                    System.out.println("初始错误" + te.error.lineNo + "修复失败，准备方法级修复");
                }
            }
        }


        // 3. 第一次修复后验证：检查初始错误是否修复，是否出现新错误（如错误3）
        String testCodeAfterFirstFix = new String(Files.readAllBytes(Paths.get(getTestFilePath(classInfo))), StandardCharsets.UTF_8);
        String firstReValidate = validateTest(testCodeAfterFirstFix);
        List<SpotError> allErrorsAfterFirstFix = parseMultipleSpotErrors(firstReValidate);

        // 4. 区分初始错误是否修复成功，以及是否有新错误
        List<TrackedError> remainingErrors = new ArrayList<>();
        for (SpotError e : allErrorsAfterFirstFix) {
            // 检查是否为未修复的初始错误
            TrackedError matchedInitial = trackedErrors.stream()
                    .filter(te -> te.isInitial && te.error.lineNo == e.lineNo)
                    .findFirst().orElse(null);

            if (matchedInitial != null) {
                // 初始错误未修复
                matchedInitial.fixed = false;
                remainingErrors.add(matchedInitial);
            } else {
                // 新出现的错误（如错误3）
                remainingErrors.add(new TrackedError(e, false));
            }
        }

        // 5. 处理未修复的初始错误（方法级修复）和新错误（原修复程序）
        // 5.1 先处理未修复的初始错误（方法级修复）
        for (TrackedError te : remainingErrors) {
            if (te.isInitial && !te.fixed) {
                System.out.println("对未修复的初始错误" + te.error.lineNo + "执行方法级修复");
                String methodCode = extractMethodCodeForError(te.error, classInfo);
                boolean methodFixed = attemptMethodLevelFix(te.error, methodCode, classInfo);
                te.fixed = methodFixed;
            }
        }

        // 5.2 再处理新出现的错误（如错误3，用原修复程序）
        List<SpotError> newErrors = remainingErrors.stream()
                .filter(te -> !te.isInitial)
                .map(te -> te.error)
                .collect(Collectors.toList());

        if (!newErrors.isEmpty()) {
            System.out.println("发现" + newErrors.size() + "个新错误（如错误3），启动原修复程序修复");
            // 生成新错误的验证报告，调用原修复程序
            String newErrorReport = generateErrorReport(newErrors);
            fixMultipleSpots(newErrorReport, classInfo);
        }

        // 6. 第二次修复后验证：若仍有错误，触发类级修复
        String testCodeAfterSecondFix = new String(Files.readAllBytes(Paths.get(getTestFilePath(classInfo))), StandardCharsets.UTF_8);
        String secondReValidate = validateTest(testCodeAfterSecondFix);
        List<SpotError> remainingErrorsAfterSecondFix = parseMultipleSpotErrors(secondReValidate);

        if (!remainingErrorsAfterSecondFix.isEmpty()) {
            System.out.println("发现" + remainingErrorsAfterSecondFix.size() + "个未修复错误，启动类级修复");
            boolean classFixed = attemptClassLevelFix(classInfo, remainingErrorsAfterSecondFix);
            if (!classFixed) {
                System.err.println("类级修复失败，保留最终状态");
            }
        }

        // 7. 最终验证
        return new String(Files.readAllBytes(Paths.get(getTestFilePath(classInfo))), StandardCharsets.UTF_8);
    }

   /* private boolean attemptFix(SpotError error, Map<String, String> classInfo) throws IOException {
        CodeSpot spot = extractSpotByStrategy(classInfo, error);
        if (spot.code.isEmpty()) return false;

        String prompt = FIX_SPOT_PROMPT
                .replace("{errorPrompt}", error.desc)
                .replace("{spotCode}", spot.code);

        String newSpot = sendRequestAndGetResponse0("", prompt).trim();
        newSpot = cleanCodeBlock(newSpot);

        if (newSpot.isEmpty() || newSpot.equals("null") || !isSyntaxValid(newSpot)) {
            return false;
        }

        replaceSpotByStrategy(classInfo, error, newSpot, spot.mode);
        return true;
    }*/

    // 4. 新增按修复级别提取代码的方法
    private CodeSpot extractSpotByMode(Map<String, String> classInfo, SpotError error, FixMode mode) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(getTestFilePath(classInfo)));
        if (mode == FixMode.LINE) {
            String errorSnippet = extractErrorSnippet(lines, error);
            return new CodeSpot(errorSnippet.isEmpty() ? lines.get(error.lineNo - 1) : errorSnippet, FixMode.LINE);
        } else if (mode == FixMode.METHOD) {
            int[] methodRange = expandToMethod(lines, error.lineNo - 1);
            error.methodStartLine = methodRange[0] + 1;
            error.methodEndLine = methodRange[1] + 1;
            String methodCode = String.join("\n", lines.subList(methodRange[0], methodRange[1] + 1));
            return new CodeSpot(methodCode, FixMode.METHOD);
        } else {
            // 类级直接提取完整类代码
            return new CodeSpot(extractClassCode(classInfo), FixMode.CLASS);
        }
    }
    // 3. 修改修复尝试方法，根据初始级别选择修复策略
    private boolean attemptFix(SpotError error, Map<String, String> classInfo) throws IOException {
        FixMode currentMode = error.initialFixMode; // 从初始级别开始
        CodeSpot spot = extractSpotByMode(classInfo, error, currentMode); // 按初始级别提取代码

        while (true) {
            if (spot.code.isEmpty()) {
                return false;
            }

            String prompt = FIX_SPOT_PROMPT
                    .replace("{errorPrompt}", error.desc)
                    .replace("{spotCode}", spot.code);

            String newSpot = sendRequestAndGetResponse0("", prompt).trim();
            newSpot = cleanCodeBlock(newSpot);

            if (newSpot.isEmpty() || newSpot.equals("null") || !isSyntaxValid(newSpot)) {
                // 修复失败，升级级别
                if (currentMode == FixMode.LINE) {
                    currentMode = FixMode.METHOD;
                    spot = extractSpotByMode(classInfo, error, currentMode);
                } else if (currentMode == FixMode.METHOD) {
                    currentMode = FixMode.CLASS;
                    // 类级修复直接调用专门方法
                    return attemptClassLevelFix(classInfo, Collections.singletonList(error));
                } else {
                    // 类级修复失败
                    return false;
                }
            } else {
                // 修复成功，替换代码
                replaceSpotByStrategy(classInfo, error, newSpot, currentMode);
                return true;
            }
        }
    }

    private String extractMethodCodeForError(SpotError error, Map<String, String> classInfo) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(getTestFilePath(classInfo)));
        int[] methodRange = expandToMethod(lines, error.lineNo - 1);
        error.methodStartLine = methodRange[0] + 1; // 转为1-base
        error.methodEndLine = methodRange[1] + 1;
        return String.join("\n", lines.subList(methodRange[0], methodRange[1] + 1));
    }

    /* ---------------- 新增辅助方法 ---------------- */
// 生成错误报告（用于新错误传递给原修复程序）
    private String generateErrorReport(List<SpotError> errors) {
        StringBuilder report = new StringBuilder();
        for (SpotError e : errors) {
            report.append("该测试代码在验证[数值正确性]时失败，错误位置：[").append(e.lineNo)
                    .append("]，错误原因：").append(e.desc).append("\n");
        }
        return report.toString();
    }

    // 方法级修复（针对未修复的初始错误）
    private boolean attemptMethodLevelFix(SpotError error, String methodCode, Map<String, String> classInfo) throws IOException {
        String prompt = "以下测试方法中的行" + error.lineNo + "存在错误，请修复：\n" +
                "错误信息：" + error.desc + "\n" +
                "方法完整代码：\n```java\n" + methodCode + "\n```\n" +
                "仅返回修复后的完整方法代码（保留结构，不解释）";

        String fixedMethod = sendRequestAndGetResponse0("", prompt).trim();
        fixedMethod = cleanCodeBlock(fixedMethod);
        if (fixedMethod.isEmpty() || !isSyntaxValid(fixedMethod)) {
            return false;
        }

        // 替换整个方法
        replaceMethod(classInfo, error.methodStartLine, error.methodEndLine, fixedMethod);
        return true;
    }

  /*  private void replaceMethod(Map<String, String> classInfo, int startLine, int endLine, String newMethodCode) throws IOException {
        Path path = Paths.get(getTestFilePath(classInfo));
        List<String> lines = Files.readAllLines(path);

        int startIdx = startLine - 1;
        int endIdx = endLine - 1;
        if (startIdx < 0 || endIdx >= lines.size() || startIdx > endIdx) {
            return;
        }
    }*/
// 补充方法级修复的完整替换逻辑（原replaceMethod方法未完成文件写入）
  private void replaceMethod(Map<String, String> classInfo, int startLine, int endLine, String newMethodCode) throws IOException {
      Path path = Paths.get(getTestFilePath(classInfo));
      List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);

      int startIdx = startLine - 1;
      int endIdx = endLine - 1;
      if (startIdx < 0 || endIdx >= lines.size() || startIdx > endIdx) {
          System.err.println("方法替换失败：范围无效");
          return;
      }

      // 清除原方法行，插入新方法代码（保留缩进）
      String methodIndent = lines.get(startIdx).replaceAll("\\S.*", ""); // 提取方法首行缩进
      List<String> newMethodLines = Arrays.asList(newMethodCode.split("\n"));
      List<String> indentedNewLines = new ArrayList<>();
      for (String line : newMethodLines) {
          indentedNewLines.add(methodIndent + line); // 保持与原方法相同缩进
      }

      lines.subList(startIdx, endIdx + 1).clear();
      lines.addAll(startIdx, indentedNewLines);
      Files.write(path, lines, StandardCharsets.UTF_8);
      System.out.println("方法替换完成：行[" + startLine + "-" + endLine + "]");
  }

    // 生成临时验证提示，让LLM检查修复是否满足要求
    private String generateValidationPrompt(SpotError error, String fixedCode) {
        return "请验证以下修复是否解决了指定错误：\n" +
                "错误位置：第" + error.lineNo + "行\n" +
                "错误描述：" + error.desc + "\n" +
                "修复后的代码：\n```java\n" + fixedCode + "\n```\n" +
                "验证要求：\n" +
                "1. 错误是否已修复（如预期值是否修改为正确结果）\n" +
                "2. 修复后的代码是否存在语法错误\n" +
                "3. 仅返回'通过'或'未通过'，无需额外解释";
    }

    // 调用LLM执行临时验证
    private boolean validateFixByLLM(SpotError error, String fixedCode) throws IOException {
        String prompt = generateValidationPrompt(error, fixedCode);
        String response = sendRequestAndGetResponse0("", prompt).trim();
        return "通过".equals(response);
    }



    /*public static void main(String[] args) throws IOException {
        FixTestCode fixer = new FixTestCode();
        Map<String, String> classInfo = new HashMap<>();
        classInfo.put("包路径", "codeing");
        classInfo.put("类名", "Fix");
        String testFilePath = fixer.getTestFilePath(classInfo);
        System.out.println("测试文件路径：" + testFilePath);
        File testFile = new File(testFilePath);
        if (!testFile.exists()) {
            System.out.println("错误：测试文件不存在！");
            return;
        }
        //获取测试代码
        String testCode = new String(Files.readAllBytes(Paths.get(testFilePath)), StandardCharsets.UTF_8);
        //验证测试代码
        String validateReport = fixer.validateTest(testCode);
        System.out.println(">>> main本次验证结果：\n" + validateReport);
        //验证通过
        if (validateReport.isEmpty() || !validateReport.contains("错误位置")) {
            System.out.println("未发现错误，无需修复");
            return;
        }
        //验证不通过
        if (!validateReport.contains("全部验证通过")) {
            // 调用多错误修复入口（而非循环单行修复）
            String fixed = fixer.fixMultipleSpots(validateReport, classInfo);
            if (fixed != null) {
                System.out.println(">>> 修复后测试类：\n" + fixed);
                String reValidate = fixer.validateTest(fixed);
                System.out.println(">>> 再次验证：\n" + reValidate);
            } else {
                System.out.println("修复失败");
            }
        }
    }*/

    public static void main(String[] args) throws IOException {
        // 1. 初始化修复器实例
        FixTestCode fixer = new FixTestCode();

        // 2. 配置待测试源代码路径
        String apiKey = "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";
        String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\cli";
        String sourceCodePath = "src/main/java/cli/AlreadySelectedException.java";
        String targetPackage = "cli";
        TestCoreDemo2 testCoreClass = new TestCoreDemo2(apiKey);
        testCoreClass.className = "AlreadySelectedException"; // 设置当前处理的类名

        String sourceCodeFile=testCoreClass.readSourceCode(sourceCodePath);
        String conversationHistory = ""; // 初始化对话历史


        Path srcPath = Paths.get(sourceCodePath);
        if (!Files.exists(srcPath)) {
            System.err.println("错误：待测试源代码文件不存在，路径=" + sourceCodePath);
            return;
        }


        //分析
        String analyzePromt=testCoreClass.analyzeSourceCode(sourceCodeFile);
        if (analyzePromt == null || analyzePromt.isEmpty()) {
            System.out.println("1.2分析阶段失败：LLM 未返回有效结果");
            return;
        }

        //提取
        Map<String, String> classInfo = testCoreClass.extractClassInfo3(analyzePromt);
        System.out.println("2.1解析到的类信息：" + classInfo);
        if (analyzePromt == null || analyzePromt.isEmpty()) {
            System.out.println("2.2分析阶段失败：LLM 未返回有效结果");
            return;
        }

        //生成
        String testCode=testCoreClass.generateTestCode2(sourceCodeFile,analyzePromt,conversationHistory);
        if (testCode == null || testCode.isEmpty()) {
            System.out.println("2.2生成阶段失败：LLM 未返回有效结果");
            return;
        }

        //类
        testCoreClass.createTestClass2(testCode, classInfo);
        String testFilePath = fixer.getTestFilePath(classInfo);
        System.out.println("生成的测试文件路径：" + testFilePath);

        // 8. 读取生成的测试代码
        String generatedTestCode = new String(Files.readAllBytes(Paths.get(testFilePath)), StandardCharsets.UTF_8);

        // 9. 验证生成的测试代码
        String validateReport = fixer.validateTest(generatedTestCode);
        System.out.println(">>> 初始测试代码验证结果：\n" + validateReport);

        // 10. 若存在错误则进行修复
        if (validateReport.contains("错误位置")) {
            System.out.println("开始修复测试代码...");
            String fixedTestCode = fixer.fixWithNewErrorHandling(validateReport, classInfo);
            if (fixedTestCode != null) {
                System.out.println(">>> 修复后测试代码：\n" + fixedTestCode);
                // 最终验证
                String finalValidate = fixer.validateTest(fixedTestCode);
                System.out.println(">>> 修复后最终验证结果：\n" + finalValidate);
            } else {
                System.err.println("测试代码修复失败");
            }
        } else {
            System.out.println("生成的测试代码无错误，无需修复");
        }
    }



}
