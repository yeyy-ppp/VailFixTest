package zju.cst.aces.runner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
public class ValiFixTextCoreDemo5 {

    // ========== 新增：错误信息封装类 ==========
    static class ErrorInfo {
        private int errorLine; // 错误行号
        private String errorType; // 错误类型（验证项）
        private String errorReason; // 错误原因
        private String testCode; // 待修复的测试代码全文
        private int errorEndLine; // 错误结束行（单行则与start一致）
        private ErrorSeverity severity; // 错误严重程度
        //   private List<String> suggestions; // 优化建议（非致命问题）

        // getter/setter
        public int getErrorLine() { return errorLine; }
        public void setErrorLine(int errorLine) { this.errorLine = errorLine; }
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
        public String getErrorReason() { return errorReason; }
        public void setErrorReason(String errorReason) { this.errorReason = errorReason; }
        public String getTestCode() { return testCode; }
        public void setTestCode(String testCode) { this.testCode = testCode; }
        public int getErrorEndLine() { return errorEndLine; }
        public void setErrorEndLine(int errorEndLine) { this.errorEndLine = errorEndLine; }
        public ErrorSeverity getSeverity() { return severity; }
        public void setSeverity(ErrorSeverity severity) { this.severity = severity; }
        //  public List<String> getSuggestions() { return suggestions; }
        //   public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }

        public ErrorInfo() {
            //       this.suggestions = new ArrayList<>();
            this.severity = ErrorSeverity.UNKNOWN;
        }
    }

    // ========== 新增：错误严重程度枚举 ==========
    enum ErrorSeverity {
        FATAL,      // 致命错误：必须修复，否则无法编译/运行
        WARNING,    // 警告：建议修复，但不影响基本运行
        //    SUGGESTION, // 建议：优化建议，可选修复
        UNKNOWN     // 未知：无法判断严重程度
    }

    // ========== 新增：修复范围枚举 ==========
    enum FixScope {
        SINGLE_LINE, // 单行修复
        METHOD_BODY, // 方法体修复
        FULL_CLASS   // 全类修复
    }

    // ========== 新增：分析结果封装类 ==========
    static class AnalysisInfo {
        private String packagePath;      // 包路径
        private String className;        // 类名
        private String parentClass;      // 父类
        private String dependencies;     // 依赖类
        private List<MethodInfo> methods; // 方法列表

        public AnalysisInfo() {
            this.methods = new ArrayList<>();
        }

        // getter/setter
        public String getPackagePath() { return packagePath; }
        public void setPackagePath(String packagePath) { this.packagePath = packagePath; }
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        public String getParentClass() { return parentClass; }
        public void setParentClass(String parentClass) { this.parentClass = parentClass; }
        public String getDependencies() { return dependencies; }
        public void setDependencies(String dependencies) { this.dependencies = dependencies; }
        public List<MethodInfo> getMethods() { return methods; }
        public void setMethods(List<MethodInfo> methods) { this.methods = methods; }

        /**
         * 获取方法列表的格式化字符串（用于提示词）
         */
        public String getMethodListString() {
            if (methods == null || methods.isEmpty()) {
                return "（注意：未能解析到方法列表，请检查测试代码是否覆盖了源代码中的所有public方法）";
            }
            StringBuilder sb = new StringBuilder();
            for (MethodInfo method : methods) {
                sb.append("  - 序号：").append(method.getIndex())
                        .append("，方法名：").append(method.getMethodName())
                        .append("，返回类型：").append(method.getReturnType())
                        .append("，功能：").append(method.getDescription())
                        .append("\n");
            }
            return sb.toString();
        }

        /**
         * 获取所有方法名列表
         */
        public List<String> getMethodNames() {
            List<String> names = new ArrayList<>();
            if (methods != null) {
                for (MethodInfo method : methods) {
                    // 提取纯方法名（不含参数）
                    String name = method.getMethodName();
                    if (name.contains("(")) {
                        name = name.substring(0, name.indexOf("("));
                    }
                    names.add(name);
                }
            }
            return names;
        }
    }

    // ========== 新增：方法信息封装类 ==========
    static class MethodInfo {
        private int index;           // 序号
        private String methodName;   // 方法名（含参数）
        private String returnType;   // 返回类型
        private String description;  // 功能描述

        // getter/setter
        public int getIndex() { return index; }
        public void setIndex(int index) { this.index = index; }
        public String getMethodName() { return methodName; }
        public void setMethodName(String methodName) { this.methodName = methodName; }
        public String getReturnType() { return returnType; }
        public void setReturnType(String returnType) { this.returnType = returnType; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }

    // ========== 新增：扩展的验证结果类（包含方法覆盖信息） ==========
    static class ExtendedValidationResult {
        private ErrorInfo errorInfo;              // 基础错误信息
        private List<String> coveredMethods;      // 已覆盖的方法
        private List<String> uncoveredMethods;    // 未覆盖的方法
        //   private List<String> optimizeSuggestions; // 优化建议
        private boolean allMethodsCovered;        // 是否所有方法都已覆盖

        public ExtendedValidationResult() {
            this.coveredMethods = new ArrayList<>();
            this.uncoveredMethods = new ArrayList<>();
            //       this.optimizeSuggestions = new ArrayList<>();
            this.allMethodsCovered = false;
        }

        // getter/setter
        public ErrorInfo getErrorInfo() { return errorInfo; }
        public void setErrorInfo(ErrorInfo errorInfo) { this.errorInfo = errorInfo; }
        public List<String> getCoveredMethods() { return coveredMethods; }
        public void setCoveredMethods(List<String> coveredMethods) { this.coveredMethods = coveredMethods; }
        public List<String> getUncoveredMethods() { return uncoveredMethods; }
        public void setUncoveredMethods(List<String> uncoveredMethods) { this.uncoveredMethods = uncoveredMethods; }
        //   public List<String> getOptimizeSuggestions() { return optimizeSuggestions; }
        //   public void setOptimizeSuggestions(List<String> optimizeSuggestions) { this.optimizeSuggestions = optimizeSuggestions; }
        public boolean isAllMethodsCovered() { return allMethodsCovered; }
        public void setAllMethodsCovered(boolean allMethodsCovered) { this.allMethodsCovered = allMethodsCovered; }

        /**
         * 判断是否需要修复（有致命错误或有未覆盖方法）
         */
        public boolean needsFixOrOptimize() {
            // 有致命错误
            if (errorInfo != null && errorInfo.getSeverity() == ErrorSeverity.FATAL && errorInfo.getErrorLine() > 0) {
                return true;
            }
            // 有未覆盖的方法
            if (uncoveredMethods != null && !uncoveredMethods.isEmpty()) {
                return true;
            }
            return false;
        }

        /**
         * 获取验证结果摘要
         */
        public String getSummary() {
            StringBuilder sb = new StringBuilder();
            if (errorInfo != null && errorInfo.getErrorLine() > 0) {
                sb.append("【致命错误】").append(errorInfo.getErrorType())
                        .append(" - ").append(errorInfo.getErrorReason()).append("\n");
            }
            if (uncoveredMethods != null && !uncoveredMethods.isEmpty()) {
                sb.append("【未覆盖方法】").append(String.join(", ", uncoveredMethods)).append("\n");
            }
        /*    if (optimizeSuggestions != null && !optimizeSuggestions.isEmpty()) {
                sb.append("【优化建议】\n");
                for (String suggestion : optimizeSuggestions) {
                    sb.append("  - ").append(suggestion).append("\n");
                }
            }*/
            if (sb.length() == 0) {
                sb.append("【全部验证通过】");
            }
            return sb.toString();
        }
    }

    // ========== 辅助方法：重复字符串（Java 8兼容） ==========
    private static String repeatStr(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private static final String API_URL="https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL= "deepseek/deepseek-r1";
    private final String apiKey;
    private OkHttpClient client;
    private final Gson gson;
    private long startTime=System.currentTimeMillis();  // 测试代码生成开始时间
    private long endTime;  // 测试代码生成结束时间
    private String sourceCodeFile;  //待测试源代码
    private String sourceCodePath;  //待测试源代码路径
    private long totalTime;  // 测试代码生成总耗时
    private int tokenCount;  // API 调用消耗的 token 数量
    private List<String> failedClasses;  // 记录修复失败的待测试类
    public String className; // 当前处理的类名
    private static final String analyzeCodePromt =
            "你是一位Java程序分析专家，请根据以下要求从专业角度分析给定的Java代码，完整列出所有方法、依赖类和父类，不要省略任何内容，除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等）：\n" +
                    "1. **类级信息**（仅输出一次）：\n" +
                    "   - 包路径：类的完整包路径\n" +
                    "   - 类名：类的名称\n" +
                    "   - 父类：该类继承的父类（若没有则填“无”）\n" +
                    "   - 依赖类：该类直接使用的非JDK自定义类（多个用逗号分隔，若没有则填“无”）\n" +
                    "2. **方法级信息**（按序号依次列出所有方法）：\n" +
                    "   - 序号：从1开始的方法编号\n" +
                    "   - 方法名：方法的完整名称（含参数列表简要标识，如method(int a)）\n" +
                    "   - 返回类型：方法的返回类型\n" +
                    "   - 功能描述：简洁描述方法的核心功能\n" +
                    "请严格按照以下格式输出：\n" +
                    "```\n" +
                    "包路径：[包路径]\n" +
                    "类名：[类名]\n" +
                    "父类：[父类名或无]\n" +
                    "依赖类：[依赖类名或无]\n" +
                    "---\n" +
                    "序号：1\n" +
                    "方法名：[方法1名称]\n" +
                    "返回类型：[方法1返回类型]\n" +
                    "功能描述：[方法1功能]\n" +
                    "序号：2\n" +
                    "方法名：[方法2名称]\n" +
                    "返回类型：[方法2返回类型]\n" +
                    "功能描述：[方法2功能]\n";
    //生成提示
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
                    "5. 正确设置包路径与包导入。\n" +
                    "6. 【重要】禁止在测试类中定义任何内部类，必须直接使用待测包中已有的类。\n" +
                    "7. 【重要】待测类的依赖类已存在于同一包中，直接使用即可，不要自行定义。\n" +
                    "8. 【重要】必须严格按照源代码中实际存在的构造函数和方法来创建对象，不要臆造不存在的方法或对象。\n" +
                    "9. 仔细阅读源代码，确保调用的方法签名与源代码完全一致。\n" +
                    "10. 【关键】所有import语句必须使用上面给出的包路径 {packagePath}，绝对禁止使用 org.apache.commons.xxx 包！\n" +
                    "    正确示例：import {packagePath}.Option;\n" +
                    "    错误示例：import org.apache.commons.xx.xxx; （禁止！）\n" +
                    "11. 【关键】测试类的package声明必须是 {packagePath}，不是 org.apache.commons.xxx！\n" +
                    "12. 【关键-受检异常处理】如果待测方法声明了throws异常（如ParseException），测试方法必须处理：\n" +
                    "    - 推荐方式：在@Test方法签名中声明 throws 异常类型\n" +
                    "    - 示例：@Test void testMethod() throws ParseException { ... }\n" +
                    "    - 或者使用try-catch捕获异常\n" +
                    "    - 不处理受检异常会导致编译错误！\n\n" +
                    "请直接输出完整的测试代码，不包含任何注释、解释或思考过程。以下为待测试的源代码：\n";

    //验证提示（基础版本，用于无分析结果时的兜底）
    private static final String validatePrompt=
            "你是测试代码审查专家，请检查以下测试代码是否存在【会导致编译失败或运行时错误】的问题。\n\n" +
                    "=== 重要：以下问题【不算错误】，请直接忽略并返回验证通过 ===\n" +
                    "- 代码缩进不一致、对齐问题、空格数量、换行风格等格式问题\n" +
                    "- 代码风格不规范（如命名风格、注释缺失）\n" +
                    "- 边界情况覆盖不全（不视为错误）\n" +
                    "- 待测试类未手动导入（系统会自动导入）\n\n" +
                    "=== 需要检查的【真正错误】 ===\n" +
                    "1. **语法错误**：缺少分号、括号不匹配、关键字拼写错误等会导致编译失败的问题；\n" +
                    "2. **编译错误**：类型不匹配、方法不存在、变量未定义等编译期错误（排除待测试类导入问题）；\n" +
                    "3. **受检异常未处理**：调用声明throws的方法时，必须用try-catch捕获或在方法签名中声明throws，否则编译失败；\n" +
                    "4. **运行时错误**：空指针、数组越界、类型转换异常等运行时会抛出异常的问题；\n" +
                    "5. **断言值错误**：assertEquals等断言中expected值与实际运行结果明显不符（需手工推演验证）。\n\n" +
                    "=== 输出格式 ===\n" +
                    "- 若无上述【真正错误】，请返回：【全部验证通过】\n" +
                    "- 若存在【真正错误】，请使用如下格式反馈：\n" +
                    "```\n" +
                    "该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[详细描述]\n" +
                    "```\n" +
                    "请严格按照行号注释报告错误位置。除了给定的格式要求不要返回其他内容。\n";

    // ========== 新增：动态验证提示模板（结合分析结果） ==========
    private static final String dynamicValidatePromptTemplate =
            "你是测试代码审查专家，请根据【待测类分析结果】对测试代码进行针对性验证。\n\n" +
                    "=== 待测类分析结果 ===\n" +
                    "包路径：{PACKAGE_PATH}\n" +
                    "类名：{CLASS_NAME}\n" +
                    "待测方法列表：\n{METHOD_LIST}\n\n" +
                    "=== 验证要求（按优先级排序） ===\n\n" +
                    "【第零优先级：包路径检查】\n" +
                    "0. **包路径错误**：检查import语句和package声明是否使用了正确的包路径 {PACKAGE_PATH}；\n" +
                    "   - 【重要】org.junit.jupiter.api.Test 等 JUnit 标准库导入是正确的，不算错误！\n" +
                    "   - 【重要】java.* 和 javax.* 等 JDK 标准库导入是正确的，不算错误！\n" +
                    "   - 只有待测类（如Option、OptionGroup等）的导入必须使用 {PACKAGE_PATH}\n" +
                    "   - 如果发现待测类使用了 org.apache.commons.xxx 包，这是【致命错误】！\n\n" +
                    "【第一优先级：致命错误检查（必须修复）】\n" +
                    "1. **语法错误**：缺少分号、括号不匹配、关键字拼写错误等；\n" +
                    "2. **编译错误**：类型不匹配、方法不存在、变量未定义、构造器不存在等；\n" +
                    "3. **受检异常未处理**：调用声明throws的方法时，必须用try-catch或在方法签名中声明throws；\n" +
                    "4. **运行时错误**：空指针、数组越界、类型转换异常等明显会抛异常的代码；\n" +
                    "5. **断言值错误**：assertEquals等断言中expected值与实际运行结果明显不符。\n\n" +
                    "【第二优先级：方法覆盖检查（必须修复）】\n" +
                    "6. **方法覆盖率**：检查测试代码是否覆盖了上述【待测方法列表】中的所有方法；\n" +
                    "   - 每个待测方法至少应有一个对应的测试用例\n" +
                    "   - 列出未被覆盖的方法名（如有）\n\n" +
                    "【第三优先级：测试质量建议（可选修复，不强制）】\n" +
                    "7. **边界情况**：是否覆盖了边界值、空值、异常输入等场景（建议补充，非强制）；\n" +
                    "8. **分支覆盖**：是否覆盖了条件分支的各种情况（建议补充，非强制）；\n" +
                    "9. **断言完整性**：断言是否充分验证了方法的返回值和副作用（建议补充，非强制）。\n\n" +
                    "=== 重要：以下问题【不算错误】 ===\n" +
                    "- 代码格式问题（缩进、空格、换行）\n" +
                    "- 待测试类未手动导入（系统会自动导入）\n" +
                    "- JUnit、JDK标准库的导入（org.junit.*、java.*、javax.*）\n" +
                    "- 代码风格问题（命名、注释）\n\n" +
                    "=== 输出格式（严格遵守） ===\n" +
                    "请严格按以下格式输出，不要添加详细说明、解释、思考过程：\n\n" +
                    "【验证结果】：通过/未通过\n\n" +
                    "【致命错误】：无 或 该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[简短描述]\n\n" +
                    "【方法覆盖】：\n" +
                    "| 待测方法 | 测试用例数 | 覆盖场景 |\n" +
                    "|----------|------------|----------|\n" +
                    "| 方法名1 | 2个 | 场景1/场景2 |\n" +
                    "| 方法名2 | 1个 | 场景1 |\n" +
                    "| 方法名3 | 0个 | 未覆盖 |\n" +
                    "（注意：\n" +
                    "  1. 必须列出【待测方法列表】中的每个方法，不能只输出表头！\n" +
                    "  2. 测试用例数必须是具体数字（1个、2个、3个...），不能用多个等模糊描述！\n" +
                    "  3. 统计方法：数一下测试代码中有几个@Test方法调用了该待测方法\n" +
                    "  4. 如果某方法未覆盖，测试用例数填0个）\n\n" +
                    "【测试质量建议】（可选，仅当有明显缺陷时输出）：\n" +
                    "- 建议1：...\n" +
                    "- 建议2：...\n\n" +
                    "注意：\n" +
                    "1. 若无致命错误且所有方法已覆盖，【验证结果】填写：通过\n" +
                    "2. 测试质量建议不影响验证结果，仅作参考\n" +
                    "3. 不要输出详细说明、代码示例、分析过程\n" +
                    "4. 【方法覆盖】表格必须包含所有待测方法的数据行，不能只有表头！\n" +
                    "5. 测试用例数必须是精确的数字，禁止使用多个等模糊词汇！\n";

    // ========== 新增：基于验证结果的修复提示模板 ==========
    private static final String optimizeFixPromptTemplate =
            "你是Java测试代码修复专家，请根据验证结果对测试代码进行修复。\n\n" +
                    "=== 待测类信息 ===\n" +
                    "包路径：{PACKAGE_PATH}\n" +
                    "类名：{CLASS_NAME}\n" +
                    "待测方法列表：\n{METHOD_LIST}\n\n" +
                    "=== 验证结果 ===\n" +
                    "{VALIDATION_RESULT}\n\n" +
                    "=== 当前测试代码 ===\n" +
                    "```java\n{TEST_CODE}\n```\n\n" +
                    "=== 修复要求 ===\n" +
                    "1. 【必须修复】致命错误（语法、编译、运行时、断言值错误）\n" +
                    "2. 【必须补充】未覆盖的待测方法（为每个未覆盖方法添加测试用例）\n" +
                    "3. 【重要约束】禁止在测试类中定义任何内部类（static class或inner class），必须直接使用待测包中已有的类\n" +
                    "4. 【关键约束】所有import必须使用 {PACKAGE_PATH} 包，禁止使用 org.apache.commons.xx！\n\n" +
                    "=== 输出要求 ===\n" +
                    "- 直接输出修复后的完整测试代码\n" +
                    "- 不要输出解释、注释、思考过程\n" +
                    "- 确保代码可以编译通过\n" +
                    "- 若无法修复，返回：【修复失败】+原因\n";

    //修复提示
    private static final String fixPrompt =
            "你是一名专业的java测试代码修复工程师，请仅对下面代码中【错误位置】所示的片段进行最小化修改，使其通过验证\n" +
                    " 除【修改后片段】外不要返回任何解释、注释、行号或其他内容。\n " +
                    "错误信息：{errorPrompt}\n" +
                    "【重要约束】禁止在测试类中定义任何内部类（static class或inner class），必须直接使用待测包中已有的类。\n" +
                    "返回格式：\n" +
                    "- 若修复失败，返回：【修复失败】，并附原因，格式：该测试代码在修复[修复项]时失败，错误位置：[行号]，错误原因：[详细描述]\\n\"。\n" +
                    "- 若修复成功，返回：【修复成功】，和修改后的测试代码。\n" +
                    "不需要返回解释、注释、思考过程、分析思路等内容，除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等）。" +
                    "如果第三次修复还是失败，就将错误的代码位置或整个测试方法删除或注释掉，以帮助成功构建编译测试类。";

    // ========== 修复提示词，适配不同修复范围 ==========
    private static final String fixPromptTemplate =
            "修复以下测试代码的错误，严格遵循要求：\n" +
                    "1. 错误信息：{ERROR_TYPE} - {ERROR_REASON}\n" +
                    "2. 修复范围：仅修改错误部分，不改动其他逻辑（{FIX_SCOPE_DESC}）\n" +
                    "3. 待修复代码片段：\n{CODE_SNIPPET}\n" +
                    "4. 【重要约束】禁止定义任何内部类，直接使用待测包中已有的类（如Option、OptionGroup等）。\n" +
                    "5. 输出要求：只返回修复后的代码片段，无多余解释、注释、行号；若无法修复，返回【修复失败】+原因；连续3次修复失败则删除/注释错误片段保证编译通过。";

    //截取错误片段-原地修复测试代码
    public ValiFixTextCoreDemo5(String apiKey) {
        this.apiKey=apiKey;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.SECONDS) // 连接超时时间设为60秒
                .readTimeout(3000, TimeUnit.SECONDS)    // 读取超时时间设为60秒
                .writeTimeout(3000, TimeUnit.SECONDS)   // 写入超时时间设为60秒
                .build();
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.failedClasses = new ArrayList<>(); // 初始化 failedClasses 列表
    }
    private void recreateClient() {
        // 重新创建 OkHttpClient 实例
        client = new OkHttpClient.Builder()
                .connectTimeout(3000, TimeUnit.SECONDS)
                .readTimeout(3000, TimeUnit.SECONDS)
                .writeTimeout(3000, TimeUnit.SECONDS)
                .build();
    }
    //1.读取源代码
    static String readSourceCode(String sourceCodePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(sourceCodePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    //2.构建请求体
    private RequestBody buildRequestBody(String code, String prompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt+ code);
        messages.add(message);
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", MODEL);
        requestBodyMap.put("messages", messages);

        return RequestBody.create(gson.toJson(requestBodyMap), MediaType.get("application/json; charset=utf-8"));
    }
    //2.2构建请求体，历史对话缓存
    private RequestBody buildRequestBody2(String code, String prompt, String conversationHistory) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (conversationHistory != null && !conversationHistory.isEmpty()) {
            // 添加历史对话
            String[] historyParts = conversationHistory.split("\n");
            for (String part : historyParts) {
                if (part.startsWith("user:")) {
                    Map<String, String> userMessage = new HashMap<>();
                    userMessage.put("role", "user");
                    userMessage.put("content", part.substring(5));
                    messages.add(userMessage);
                } else if (part.startsWith("assistant:")) {
                    Map<String, String> assistantMessage = new HashMap<>();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", part.substring(10));
                    messages.add(assistantMessage);
                }
            }
        }
        // 添加当前请求
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt + code);
        messages.add(userMessage);
        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", MODEL);
        requestBodyMap.put("messages", messages);
        return RequestBody.create(gson.toJson(requestBodyMap), MediaType.get("application/json; charset=utf-8"));
    }
    //3.构建请求
    private Request buildRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
    }
    //4.发送请求获取响应
    private String sendRequestAndGetResponse(String content, String prompt) throws IOException {
        RequestBody requestBody = buildRequestBody(content, prompt);
        Request request = buildRequest(requestBody);
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody=response.body().string();
                Map<String, Object> responseMap = gson.fromJson(responseBody, HashMap.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    return message != null ? message.get("content") : "";
                }
                return "";
            } else {
                throw new IOException("请求响应失败！" + response);
            }
        }
    }
    public String sendRequestAndGetResponse0(String content, String prompt) throws IOException {
        int maxRetries = 3; // 最大重试次数
        int retryDelayMillis = 2000; // 重试间隔时间（毫秒）
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try (Response response = client.newCall(buildRequest(buildRequestBody(content, prompt))).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    if (responseBody == null || responseBody.trim().isEmpty()) {
                        throw new IOException("LLM 返回空响应，请检查 API 配额或网络。");
                    }
                    Map<String, Object> responseMap = gson.fromJson(responseBody, HashMap.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                        return message != null ? message.get("content") : "";
                    }
                    return "";
                } else {
                    if (response.code() == 500) {
                        // 如果服务器返回500错误，关闭当前客户端并重新创建
                        client.dispatcher().cancelAll();
                        client.connectionPool().evictAll();
                        recreateClient(); // 调用方法重新创建客户端
                    }
                    throw new IOException("请求响应失败！状态码：" + response.code());
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e; // 如果达到最大重试次数，则抛出异常
                }
                System.out.println("请求失败，正在进行第 " + retryCount + " 次重试...");
                try {
                    Thread.sleep(retryDelayMillis); // 等待一段时间后重试
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("请求被中断", ex);
                }
            }
        }
        //  return null;
        throw new IOException("LLM 请求失败，重试3次后仍无响应，请检查网络或API密钥。");
    }
    //4.2发送请求获取响应，缓存历史对话
    private String sendRequestAndGetResponse2(String content, String prompt, String conversationHistory) throws IOException {
        RequestBody requestBody = buildRequestBody2(content, prompt, conversationHistory);
        Request request = buildRequest(requestBody);
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                String responseBody = response.body().string();
                Map<String, Object> responseMap = gson.fromJson(responseBody, HashMap.class);
                List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                    return message != null ? message.get("content") : "";
                }
                return "";
            } else {
                throw new IOException("请求响应失败！" + response);
            }
        }
    }
    private String sendRequestAndGetResponse3(String content, String prompt, String conversationHistory) throws IOException {
        int maxRetries = 3; // 最大重试次数
        int retryDelayMillis = 2000; // 重试间隔时间（毫秒）
        int retryCount = 0;
        while (retryCount < maxRetries) {
            try (Response response = client.newCall(buildRequest(buildRequestBody2(content, prompt, conversationHistory))).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Map<String, Object> responseMap = gson.fromJson(responseBody, HashMap.class);
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    if (choices != null && !choices.isEmpty()) {
                        Map<String, Object> firstChoice = choices.get(0);
                        Map<String, String> message = (Map<String, String>) firstChoice.get("message");
                        return message != null ? message.get("content") : "";
                    }
                    return "";
                } else {
                    if (response.code() == 500) {
                        // 如果服务器返回500错误，关闭当前客户端并重新创建
                        client.dispatcher().cancelAll();
                        client.connectionPool().evictAll();
                        recreateClient(); // 调用方法重新创建客户端
                    }
                    throw new IOException("请求响应失败！状态码：" + response.code());
                }
            } catch (IOException e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    throw e; // 如果达到最大重试次数，则抛出异常
                }
                System.out.println("请求失败，正在进行第 " + retryCount + " 次重试...");
                try {
                    Thread.sleep(retryDelayMillis); // 等待一段时间后重试
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    throw new IOException("请求被中断", ex);
                }
            }
        }
        // return null;
        throw new IOException("LLM 请求失败，重试3次后仍无响应，请检查网络或API密钥。");
    }
    //4.3请求切割
    private List<String> divideCodeIntoSegments(String sourceCode, int maxSegmentLength) {
        List<String> segments = new ArrayList<>();
        int codeLength = sourceCode.length();
        for (int i = 0; i < codeLength; i += maxSegmentLength) {
            int end = Math.min(i + maxSegmentLength, codeLength);
            segments.add(sourceCode.substring(i, end));
        }
        return segments;
    }
    //5.LLM-api响应
    private static class ApiResponse {
        private final String content;
        private final int tokensUsed;
        public ApiResponse(String content, int tokensUsed) {
            this.content = content;
            this.tokensUsed = tokensUsed;
        }
        public String getContent() {
            return content;
        }
        public int getTokensUsed() {
            return tokensUsed;
        }
    }
    //6.LLM解析源代码
    public String analyzeSourceCode(String sourceCodeFile) throws IOException {
        String analyzePromt=sendRequestAndGetResponse0(sourceCodeFile, analyzeCodePromt);
        System.out.println("解析提示内容：\n"+analyzePromt + "over\n");
        return analyzePromt;
    }
    //6.1 解析提取源代码信息构建测试类
    public Map<String, String> extractClassInfo(String analyzeResult) {
        //分析开始前输出分析结果
        //System.out.println("分析结果："+ analyzeResult);
        Map<String, String> classInfo = new HashMap<>();
        String pattern = "包路径：(.*)\n类名：(.*)\n方法名：(.*)\n返回类型：(.*)\n功能描述：(.*)";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(analyzeResult);
        if (m.find()) {
            classInfo.put("包路径", m.group(1).trim());
            classInfo.put("类名", m.group(2).trim());
            classInfo.put("方法名", m.group(3).trim());
            classInfo.put("返回类型", m.group(4).trim());
            classInfo.put("功能描述", m.group(5).trim());
        }else {
            // 如果未找到匹配，尝试更宽松的正则表达式
            pattern = "包路径：(.*)类名：(.*)方法名：(.*)返回类型：(.*)功能描述：(.*)";
            r = Pattern.compile(pattern);
            m = r.matcher(analyzeResult.replace("\n", ""));
            if (m.find()) {
                classInfo.put("包路径", m.group(1).trim());
                classInfo.put("类名", m.group(2).trim());
                classInfo.put("方法名", m.group(3).trim());
                classInfo.put("返回类型", m.group(4).trim());
                classInfo.put("功能描述", m.group(5).trim());
            } else {
                System.err.println("未能从分析结果中提取包路径和类名");
            }
        }
        return classInfo;
    }
    //6.2   类中多方法提取+解析
    /**
     * 从 LLM 返回的分析结果里提取
     * 1. 包路径
     * 2. 类名
     * 3. 所有方法（按序号依次提取：序号、方法名、返回类型、功能描述）
     *
     * @param analyzeResult LLM 原始返回字符串
     * @return Map 中 key：包路径、类名、allMethodsInfo（可直接用于 prompt 替换）
     */
    public Map<String, String> extractClassInfo3(String analyzeResult) {
        Map<String, String> classInfo = new HashMap<>();
        /* ---------- 1. 提取包路径 & 类名 ---------- */
        // 单行模式，允许中间有换行
        Pattern headerPattern = Pattern.compile(
                "包路径：(.*?)\\n类名：(.*?)\\n", Pattern.DOTALL);
        Matcher headerMatcher = headerPattern.matcher(analyzeResult);
        if (headerMatcher.find()) {
            classInfo.put("包路径", headerMatcher.group(1).trim());

            // 提取类名并移除泛型参数（如果有）
            String className = headerMatcher.group(2).trim();
            if (className.contains("<")) {
                className = className.substring(0, className.indexOf("<")).trim();
            }
            classInfo.put("类名", className);
        } else {
            System.err.println("未能提取包路径或类名");
            return classInfo;   // 早退
        }
        Pattern methodPattern = Pattern.compile(
                "序号：(.*?)\n方法名：(.*?)\n返回类型：(.*?)\n功能描述：(.*?)(?=\\n序号|\\n```|$)", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(analyzeResult);
        StringBuilder allMethodsInfo = new StringBuilder(512);
        while (methodMatcher.find()) {
            String index   = methodMatcher.group(1).trim();
            String method  = methodMatcher.group(2).trim();
            String returnT = methodMatcher.group(3).trim();
            String desc    = methodMatcher.group(4).replaceAll("\\s+", " ").trim(); // 去多余空白
            // 拼成一段，保持和原来 prompt 一样的格式
            allMethodsInfo
                    .append("序号：").append(index).append('\n')
                    .append("方法名：").append(method).append('\n')
                    .append("返回类型：").append(returnT).append('\n')
                    .append("功能描述：").append(desc).append("\n\n");
        }

        if (allMethodsInfo.length() == 0) {
            System.err.println("未能提取到任何方法信息");
        }
        classInfo.put("allMethodsInfo", allMethodsInfo.toString());
        return classInfo;
    }

    // ========== 新增：解析分析结果为 AnalysisInfo 对象 ==========
    /**
     * 将LLM分析结果解析为结构化的 AnalysisInfo 对象
     * 用于动态生成验证提示和方法覆盖检查
     *
     * @param analyzeResult LLM 原始返回字符串
     * @return AnalysisInfo 结构化分析结果
     */
    public AnalysisInfo parseAnalysisResult(String analyzeResult) {
        AnalysisInfo analysisInfo = new AnalysisInfo();

        // 1. 提取类级信息
        Pattern headerPattern = Pattern.compile(
                "包路径：(.*?)\\n类名：(.*?)\\n父类：(.*?)\\n依赖类：(.*?)\\n", Pattern.DOTALL);
        Matcher headerMatcher = headerPattern.matcher(analyzeResult);
        if (headerMatcher.find()) {
            analysisInfo.setPackagePath(headerMatcher.group(1).trim());

            // 提取类名并移除泛型参数（如果有）
            String className = headerMatcher.group(2).trim();
            if (className.contains("<")) {
                className = className.substring(0, className.indexOf("<")).trim();
            }
            analysisInfo.setClassName(className);

            analysisInfo.setParentClass(headerMatcher.group(3).trim());
            analysisInfo.setDependencies(headerMatcher.group(4).trim());
        } else {
            // 尝试简化版匹配（兼容旧格式）
            Pattern simplePattern = Pattern.compile("包路径：(.*?)\\n类名：(.*?)\\n", Pattern.DOTALL);
            Matcher simpleMatcher = simplePattern.matcher(analyzeResult);
            if (simpleMatcher.find()) {
                analysisInfo.setPackagePath(simpleMatcher.group(1).trim());

                // 提取类名并移除泛型参数（如果有）
                String className = simpleMatcher.group(2).trim();
                if (className.contains("<")) {
                    className = className.substring(0, className.indexOf("<")).trim();
                }
                analysisInfo.setClassName(className);
            }
        }

        // 2. 提取方法信息
        Pattern methodPattern = Pattern.compile(
                "序号：(\\d+)\\n方法名：(.*?)\\n返回类型：(.*?)\\n功能描述：(.*?)(?=\\n序号|\\n```|$)", Pattern.DOTALL);
        Matcher methodMatcher = methodPattern.matcher(analyzeResult);

        List<MethodInfo> methods = new ArrayList<>();
        while (methodMatcher.find()) {
            MethodInfo method = new MethodInfo();
            method.setIndex(Integer.parseInt(methodMatcher.group(1).trim()));
            method.setMethodName(methodMatcher.group(2).trim());
            method.setReturnType(methodMatcher.group(3).trim());
            method.setDescription(methodMatcher.group(4).replaceAll("\\s+", " ").trim());
            methods.add(method);
        }
        analysisInfo.setMethods(methods);

        return analysisInfo;
    }

    // ========== 新增：基于分析结果的动态验证方法 ==========
    /**
     * 动态验证测试代码（结合分析结果）
     *
     * 核心功能：
     * 1. 检查致命错误（语法、编译、运行时、断言值）
     * 2. 检查方法覆盖率（是否覆盖了所有待测方法）
     *
     * @param testCode 待验证的测试代码
     * @param analysisInfo 源代码分析结果
     * @return ExtendedValidationResult 扩展的验证结果
     */
    public ExtendedValidationResult validateTestCodeWithAnalysis(String testCode, AnalysisInfo analysisInfo) throws IOException {
        // 1. 构建动态验证提示词
        String dynamicPrompt = buildDynamicValidatePromptWithAnalysis(analysisInfo);

        // 2. 添加行号便于定位
        String numberedCode = addLineNumbers(testCode);

        // 3. 完整提示词
        String fullPrompt = dynamicPrompt +
                "\n=== 待验证的测试代码 ===\n" +
                "```java\n" + numberedCode + "\n```";

        // 4. 调用大模型验证
        String validateResult = sendRequestAndGetResponse0("", fullPrompt);
        System.out.println("动态验证结果（含方法覆盖检查）：\n" + validateResult);

        // 5. 解析扩展验证结果
        return parseExtendedValidationResult(validateResult, testCode, analysisInfo);
    }

    /**
     * 根据分析结果构建动态验证提示词
     */
    private String buildDynamicValidatePromptWithAnalysis(AnalysisInfo analysisInfo) {
        String prompt = dynamicValidatePromptTemplate
                .replace("{PACKAGE_PATH}", analysisInfo.getPackagePath() != null ? analysisInfo.getPackagePath() : "未知")
                .replace("{CLASS_NAME}", analysisInfo.getClassName() != null ? analysisInfo.getClassName() : "未知")
                .replace("{METHOD_LIST}", analysisInfo.getMethodListString());
        return prompt;
    }

    /**
     * 解析扩展验证结果（包含方法覆盖信息）
     */
    private ExtendedValidationResult parseExtendedValidationResult(String validateResult, String testCode, AnalysisInfo analysisInfo) {
        ExtendedValidationResult result = new ExtendedValidationResult();

        // 1. 解析基础错误信息
        ErrorInfo errorInfo = parseValidateResult(validateResult, testCode);
        result.setErrorInfo(errorInfo);

        // 2. 解析方法覆盖情况
        parseMethodCoverage(validateResult, result, analysisInfo);

        // 3. 解析优化建议
        //  parseOptimizeSuggestions(validateResult, result);

        return result;
    }

    /**
     * 解析方法覆盖情况（支持表格格式和列表格式）
     */
    private void parseMethodCoverage(String validateResult, ExtendedValidationResult result, AnalysisInfo analysisInfo) {
        List<String> covered = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();

        // 方式1：解析表格格式 | 方法名 | 测试用例数 | 覆盖场景 |
        Pattern tablePattern = Pattern.compile("\\|\\s*`?([a-zA-Z_][a-zA-Z0-9_]*)`?\\s*\\|\\s*(\\d+)\\s*个?\\s*\\|");
        Matcher tableMatcher = tablePattern.matcher(validateResult);
        while (tableMatcher.find()) {
            String methodName = tableMatcher.group(1).trim();
            if (!methodName.equals("待测方法") && !methodName.equals("方法名") && !methodName.isEmpty()) {
                covered.add(methodName);
            }
        }

        // 方式2：解析列表格式 已覆盖方法：[方法1, 方法2]
        if (covered.isEmpty()) {
            Pattern coveredPattern = Pattern.compile("已覆盖方法[：:](.*?)(?=\\n|未覆盖|$)", Pattern.DOTALL);
            Matcher coveredMatcher = coveredPattern.matcher(validateResult);
            if (coveredMatcher.find()) {
                String coveredStr = coveredMatcher.group(1).trim();
                if (!coveredStr.isEmpty() && !coveredStr.equals("无") && !coveredStr.equals("[]")) {
                    covered = parseMethodList(coveredStr);
                }
            }
        }

        result.setCoveredMethods(covered);

        // 解析未覆盖方法
        Pattern uncoveredPattern = Pattern.compile("未覆盖方法[：:](.*?)(?=\\n|【|$)", Pattern.DOTALL);
        Matcher uncoveredMatcher = uncoveredPattern.matcher(validateResult);
        if (uncoveredMatcher.find()) {
            String uncoveredStr = uncoveredMatcher.group(1).trim();
            if (!uncoveredStr.isEmpty() && !uncoveredStr.equals("无") && !uncoveredStr.equals("全部覆盖") && !uncoveredStr.equals("[]")) {
                uncovered = parseMethodList(uncoveredStr);
            }
        }
        result.setUncoveredMethods(uncovered);

        // 判断是否全部覆盖
        if (validateResult.contains("全部覆盖") || validateResult.contains("未覆盖方法：无") ||
                validateResult.contains("未覆盖方法:无") ||
                (result.getUncoveredMethods() == null || result.getUncoveredMethods().isEmpty())) {
            result.setAllMethodsCovered(true);
        }

        // 如果LLM没有返回覆盖信息，尝试自动检测
        if ((result.getCoveredMethods() == null || result.getCoveredMethods().isEmpty()) && analysisInfo != null) {
            autoDetectMethodCoverage(result, analysisInfo);
        }
    }

    /**
     * 自动检测方法覆盖情况（当LLM未返回时的兜底）
     */
    private void autoDetectMethodCoverage(ExtendedValidationResult result, AnalysisInfo analysisInfo) {
        if (result.getErrorInfo() == null || result.getErrorInfo().getTestCode() == null) {
            return;
        }

        String testCode = result.getErrorInfo().getTestCode().toLowerCase();
        List<String> allMethods = analysisInfo.getMethodNames();
        List<String> covered = new ArrayList<>();
        List<String> uncovered = new ArrayList<>();

        for (String methodName : allMethods) {
            // 简单检测：测试代码中是否包含方法名调用
            if (testCode.contains(methodName.toLowerCase() + "(") ||
                    testCode.contains("." + methodName.toLowerCase())) {
                covered.add(methodName);
            } else {
                uncovered.add(methodName);
            }
        }

        result.setCoveredMethods(covered);
        result.setUncoveredMethods(uncovered);
        result.setAllMethodsCovered(uncovered.isEmpty());
    }

    /**
     * 解析方法名列表字符串
     */
    private List<String> parseMethodList(String methodListStr) {
        List<String> methods = new ArrayList<>();
        // 移除方括号
        methodListStr = methodListStr.replaceAll("[\\[\\]]", "");
        // 按逗号或换行分割
        String[] parts = methodListStr.split("[,，\\n]");
        for (String part : parts) {
            String method = part.trim();
            if (!method.isEmpty() && !method.equals("-")) {
                // 提取纯方法名（去除参数）
                if (method.contains("(")) {
                    method = method.substring(0, method.indexOf("("));
                }
                methods.add(method.trim());
            }
        }
        return methods;
    }

    /**
     * 解析优化建议
     */
   /* private void parseOptimizeSuggestions(String validateResult, ExtendedValidationResult result) {
        // 匹配优化建议部分
        Pattern suggestionPattern = Pattern.compile("【优化建议】[：:]?(.*?)(?=【|$)", Pattern.DOTALL);
        Matcher matcher = suggestionPattern.matcher(validateResult);

        List<String> suggestions = new ArrayList<>();
        if (matcher.find()) {
            String suggestionBlock = matcher.group(1).trim();
            // 按行或按 "-" 分割
            String[] lines = suggestionBlock.split("\\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("-")) {
                    line = line.substring(1).trim();
                }
                if (!line.isEmpty()) {
                    suggestions.add(line);
                }
            }
        }
        result.setOptimizeSuggestions(suggestions);
    }*/

    // ========== 新增：基于验证结果的优化修复方法 ==========
    /**
     * 根据扩展验证结果进行优化修复
     *
     * 修复策略：
     * 1. 优先修复致命错误
     * 2. 补充未覆盖的方法测试
     * 3. 可选：根据优化建议改进测试质量
     *
     * @param testCode 当前测试代码
     * @param validationResult 扩展验证结果
     * @param analysisInfo 源代码分析结果
     * @return 修复/优化后的测试代码
     */
   /* public String optimizeTestCodeWithValidation(String testCode, ExtendedValidationResult validationResult,
                                                 AnalysisInfo analysisInfo, boolean applyOptimizations) throws IOException {
        // 如果不需要修复或优化，直接返回
        if (!validationResult.needsFixOrOptimize() && !applyOptimizations) {
            System.out.println("测试代码无需修复或优化");
            return testCode;
        }

        // 构建优化修复提示词
        String optimizePrompt = buildOptimizeFixPrompt(testCode, validationResult, analysisInfo, applyOptimizations);

        // 调用大模型进行修复/优化
        String fixedCode = sendRequestAndGetResponse0("", optimizePrompt);
        System.out.println("优化修复结果：\n" + fixedCode);

        // 提取纯代码
        fixedCode = extractPureCode(fixedCode);

        // 检查是否修复失败
        if (fixedCode.contains("【修复失败】") || fixedCode.isEmpty()) {
            System.out.println("优化修复失败，返回原代码");
            return testCode;
        }

        return fixedCode;
    }*/
    public String optimizeTestCodeWithValidation(String testCode, ExtendedValidationResult validationResult,
                                                 AnalysisInfo analysisInfo) throws IOException {
        // 如果不需要修复或优化，直接返回
        if (!validationResult.needsFixOrOptimize() ) {
            System.out.println("测试代码无需修复");
            return testCode;
        }

        // 构建优化修复提示词
        String optimizePrompt = buildOptimizeFixPrompt(testCode, validationResult, analysisInfo);

        // 调用大模型进行修复/优化
        String fixedCode = sendRequestAndGetResponse0("", optimizePrompt);
        System.out.println("优化修复结果：\n" + fixedCode);

        // 提取纯代码
        fixedCode = extractPureCode(fixedCode);

        // 检查是否修复失败
        if (fixedCode.contains("【修复失败】") || fixedCode.isEmpty()) {
            System.out.println("优化修复失败，返回原代码");
            return testCode;
        }

        return fixedCode;
    }
    /**
     * 构建优化修复提示词
     */
   /* private String buildOptimizeFixPrompt(String testCode, ExtendedValidationResult validationResult,
                                          AnalysisInfo analysisInfo, boolean applyOptimizations) {
        StringBuilder validationSummary = new StringBuilder();

        // 1. 致命错误
        ErrorInfo errorInfo = validationResult.getErrorInfo();
        if (errorInfo != null && errorInfo.getErrorLine() > 0) {
            validationSummary.append("【致命错误】\n")
                    .append("  类型：").append(errorInfo.getErrorType()).append("\n")
                    .append("  行号：").append(errorInfo.getErrorLine()).append("\n")
                    .append("  原因：").append(errorInfo.getErrorReason()).append("\n\n");
        }

        // 2. 未覆盖方法
        List<String> uncovered = validationResult.getUncoveredMethods();
        if (uncovered != null && !uncovered.isEmpty()) {
            validationSummary.append("【未覆盖方法】\n")
                    .append("  ").append(String.join(", ", uncovered)).append("\n")
                    .append("  请为这些方法添加测试用例\n\n");
        }

      *//*  // 3. 优化建议（可选）
        if (applyOptimizations) {
            List<String> suggestions = validationResult.getOptimizeSuggestions();
            if (suggestions != null && !suggestions.isEmpty()) {
                validationSummary.append("【优化建议】\n");
                for (String suggestion : suggestions) {
                    validationSummary.append("  - ").append(suggestion).append("\n");
                }
            }
        }*//*

        // 构建完整提示词
        String prompt = optimizeFixPromptTemplate
                .replace("{PACKAGE_PATH}", analysisInfo.getPackagePath() != null ? analysisInfo.getPackagePath() : "未知")
                .replace("{CLASS_NAME}", analysisInfo.getClassName() != null ? analysisInfo.getClassName() : "未知")
                .replace("{METHOD_LIST}", analysisInfo.getMethodListString())
                .replace("{VALIDATION_RESULT}", validationSummary.toString())
                .replace("{TEST_CODE}", testCode);

        return prompt;
    }*/


    private String buildOptimizeFixPrompt(String testCode, ExtendedValidationResult validationResult,
                                          AnalysisInfo analysisInfo) {
        StringBuilder validationSummary = new StringBuilder();

        // 1. 致命错误
        ErrorInfo errorInfo = validationResult.getErrorInfo();
        if (errorInfo != null && errorInfo.getErrorLine() > 0) {
            validationSummary.append("【致命错误】\n")
                    .append("  类型：").append(errorInfo.getErrorType()).append("\n")
                    .append("  行号：").append(errorInfo.getErrorLine()).append("\n")
                    .append("  原因：").append(errorInfo.getErrorReason()).append("\n\n");
        }

        // 2. 未覆盖方法
        List<String> uncovered = validationResult.getUncoveredMethods();
        if (uncovered != null && !uncovered.isEmpty()) {
            validationSummary.append("【未覆盖方法】\n")
                    .append("  ").append(String.join(", ", uncovered)).append("\n")
                    .append("  请为这些方法添加测试用例\n\n");
        }

      /*  // 3. 优化建议（可选）
        if (applyOptimizations) {
            List<String> suggestions = validationResult.getOptimizeSuggestions();
            if (suggestions != null && !suggestions.isEmpty()) {
                validationSummary.append("【优化建议】\n");
                for (String suggestion : suggestions) {
                    validationSummary.append("  - ").append(suggestion).append("\n");
                }
            }
        }*/

        // 构建完整提示词
        String prompt = optimizeFixPromptTemplate
                .replace("{PACKAGE_PATH}", analysisInfo.getPackagePath() != null ? analysisInfo.getPackagePath() : "未知")
                .replace("{CLASS_NAME}", analysisInfo.getClassName() != null ? analysisInfo.getClassName() : "未知")
                .replace("{METHOD_LIST}", analysisInfo.getMethodListString())
                .replace("{VALIDATION_RESULT}", validationSummary.toString())
                .replace("{TEST_CODE}", testCode);

        return prompt;
    }
    // ========== 新增：完整的动态验证-优化修复流程 ==========
    /**
     * 完整的动态验证-优化修复流程
     *
     * 流程：
     * 1. 解析源代码分析结果
     * 2. 使用动态验证提示检查测试代码（含方法覆盖检查）
     * 3. 根据验证结果进行修复/优化
     * 4. 循环直到通过验证或达到最大尝试次数
     *
     * @param testCode 初始测试代码
     * @param analyzeResult 源代码分析结果字符串
     * @param outputPath 输出路径
     * @param maxAttempts 最大尝试次数
     * @param applyOptimizations 是否应用优化建议
     * @return 是否成功
     */
    public boolean runDynamicValiFixProcess(String testCode, String analyzeResult, String outputPath,
                                            int maxAttempts, boolean applyOptimizations) throws IOException {
        System.out.println("\n================================================================================");
        System.out.println("                    动态验证-优化修复流程开始");
        System.out.println("================================================================================");

        // 1. 解析分析结果
        AnalysisInfo analysisInfo = parseAnalysisResult(analyzeResult);
        System.out.println("【分析结果】");
        System.out.println("  包路径：" + analysisInfo.getPackagePath());
        System.out.println("  类名：" + analysisInfo.getClassName());
        System.out.println("  待测方法数：" + analysisInfo.getMethods().size());
        for (MethodInfo method : analysisInfo.getMethods()) {
            System.out.println("    - " + method.getMethodName() + " : " + method.getReturnType());
        }
        System.out.println("--------------------------------------------------------------------------------");

        String currentTestCode = testCode;
        int attempt = 0;

        while (attempt < maxAttempts) {
            attempt++;
            System.out.println("\n【第 " + attempt + " 轮验证-修复】");

            // 2. 动态验证（含方法覆盖检查）
            ExtendedValidationResult validationResult = validateTestCodeWithAnalysis(currentTestCode, analysisInfo);

            // 打印验证结果
            System.out.println("验证结果摘要：");
            System.out.println(validationResult.getSummary());

            // 3. 检查是否通过
            if (!validationResult.needsFixOrOptimize()) {
                System.out.println("✓ 验证通过！所有方法已覆盖，无致命错误");

              /*  // 如果需要应用优化建议且有建议
                if (applyOptimizations && validationResult.getOptimizeSuggestions() != null
                        && !validationResult.getOptimizeSuggestions().isEmpty()) {
                    System.out.println("应用优化建议...");
                    currentTestCode = optimizeTestCodeWithValidation(currentTestCode, validationResult, analysisInfo, true);
                }*/

                // 写入文件
                writeTestCodeToFile(currentTestCode, outputPath);
                return true;
            }

            // 4. 执行修复/优化
            System.out.println("执行修复/优化...");
            currentTestCode = optimizeTestCodeWithValidation(currentTestCode, validationResult, analysisInfo);

            // 检查是否修复失败（代码没有变化）
            if (currentTestCode.equals(testCode) && attempt > 1) {
                System.out.println("✗ 修复未产生变化，终止循环");
                break;
            }
        }

        // 达到最大尝试次数
        System.out.println("✗ 达到最大尝试次数 " + maxAttempts + "，保存当前结果");
        writeTestCodeToFile(currentTestCode, outputPath);
        return false;
    }


    //7.LLM生成测试代码 out
    public String generateTestCode(String sourceCodeFile,String analyzePromt) throws IOException {
        String testCode=sendRequestAndGetResponse0(sourceCodeFile, analyzePromt+generatePrompt);
        System.out.println("生成测试代码：\n"+testCode + "over\n");
        // 提取纯代码，去除markdown标记
        testCode = extractPureCode(testCode);
        return testCode;
    }

    //7.2 LLM生成测试代码，缓存中获取代码
    public String generateTestCode2(String sourceCodeFile, String analyzePromt, String conversationHistory) throws IOException {
        // 调用 sendRequestAndGetResponse 方法发送请求并获取测试代码
        // 提取分析结果中的信息
        Map<String, String> classInfo = extractClassInfo3(analyzePromt);
        String packagePath = classInfo.get("包路径");
        String className = classInfo.get("类名");
        String allMethodsInfo=classInfo.get("allMethodsInfo");
        if (packagePath == null || className == null || allMethodsInfo==null ) {
            System.err.println("提取的类信息不完整，无法生成测试代码。");
            return null;
        }
        //把整个 analyzePromt 塞进 {allMethodsInfo}
        String newGprompt = generatePrompt
                .replace("{packagePath}", packagePath)
                .replace("{className}",   className)
                .replace("{allMethodsInfo}", allMethodsInfo);   // <-- 整段落入
        String testCode = sendRequestAndGetResponse3(sourceCodeFile, newGprompt, conversationHistory);
        System.out.println("生成测试代码：\n" + testCode + "over\n");
        // 提取纯代码，去除markdown标记
        testCode = extractPureCode(testCode);
        return testCode;
    }



    /* ====== 行号注释工具 ====== */
    private String addLineNumbers(String code) {
        String[] lines = code.split("\n");
        StringBuilder sb = new StringBuilder(code.length() + lines.length * 6);
        for (int i = 0; i < lines.length; i++) {
            // 4 位宽度右对齐 + 原行内容
            sb.append(String.format("%4d ", i + 1)).append(lines[i]).append('\n');
        }
        return sb.toString();
    }
    //8.LLM验证测试代码（带行号注释版）
    public String validateTestCode2(String testCode) throws IOException {
        String numbered = addLineNumbers(testCode);          // ← 新增
        //   System.out.println("待验证测试代码（带行号）：\n" + numbered); // ← 调试可删
        /* 把带行号版本发给 LLM，并提示它以注释行号为准 */
        String promptWithNum = validatePrompt +
                "\n=== 重要补充规则 ===\n" +
                "1. 待测试类（如AddExample）无需手动导入，即使代码中无import语句也不算错误；\n" +
                "2. 仅当核心代码路径完全缺失时才判定“测试逻辑完整性”失败，边界覆盖不全仅为建议，不返回错误；\n" +
                "3. 编译错误判定时，排除“未导入AddExample”相关错误；\n" +
                "\n注意：以下代码已添加行号注释，请以行号注释为准报告错误位置。\n" +
                "```java\n" + numbered + "\n```";
        String validateResult = sendRequestAndGetResponse0("", promptWithNum);
        System.out.println("validateTestCode2测试代码验证结果：\n" + validateResult + "over\n");
        return validateResult;
    }


       /* //8.LLM验证测试代码
        public ErrorInfo validateTestCode(String testCode) throws IOException {
            String validateResult=sendRequestAndGetResponse0(testCode, validatePrompt);
            System.out.println("validateTestCode测试代码验证结果：\n"+validateResult + "over\n");
            return validateResult;
        }*/

    public ErrorInfo validateTestCode(String testCode) throws IOException {
        // 1. 调用大模型验证测试代码
        String validateResult = sendRequestAndGetResponse0(testCode, validatePrompt);

        // 2. 解析验证结果，提取错误信息
        return parseValidateResult(validateResult, testCode);
    }

    // ========== 新增：解析验证结果，提取错误信息（区分致命错误和建议） ==========
    private ErrorInfo parseValidateResult(String validateResult, String testCode) {
        ErrorInfo errorInfo = new ErrorInfo();
        errorInfo.setTestCode(testCode);

        // 检查是否验证通过（支持多种格式）
        // 格式1: 【全部验证通过】
        // 格式2: 全部验证通过
        // 格式3: 【验证结果】：通过 或 【验证结果】:通过
        // 格式4: 验证结果：通过
        // 格式5: 【致命错误】：无 或 【致命错误】:无
        boolean isValidationPassed = false;

        if (validateResult.contains("全部验证通过") || validateResult.contains("【全部验证通过】")) {
            isValidationPassed = true;
        } else if (validateResult.contains("【验证结果】") &&
                (validateResult.contains("：通过") || validateResult.contains(":通过") || validateResult.contains("： 通过") || validateResult.contains(": 通过"))) {
            isValidationPassed = true;
        } else if (validateResult.contains("验证结果") && validateResult.contains("通过") && !validateResult.contains("未通过") && !validateResult.contains("不通过")) {
            // 检查是否有致命错误
            if (validateResult.contains("【致命错误】") &&
                    (validateResult.contains("：无") || validateResult.contains(":无") || validateResult.contains("： 无") || validateResult.contains(": 无"))) {
                isValidationPassed = true;
            } else if (!validateResult.contains("【致命错误】") || !validateResult.contains("错误位置")) {
                // 没有致命错误部分，或者致命错误部分没有具体错误位置
                isValidationPassed = true;
            }
        }

        // 额外检查：如果包含"未覆盖方法：全部覆盖"或"未覆盖方法：无"，且没有致命错误，也算通过
        if (!isValidationPassed) {
            boolean noFatalError = !validateResult.contains("该测试代码在验证") ||
                    (validateResult.contains("【致命错误】") &&
                            (validateResult.contains("【致命错误】：无") || validateResult.contains("【致命错误】:无") ||
                                    validateResult.contains("【致命错误】（如有）：") && validateResult.indexOf("【方法覆盖情况】") - validateResult.indexOf("【致命错误】（如有）：") < 30));
            boolean allMethodsCovered = validateResult.contains("全部覆盖") ||
                    (validateResult.contains("未覆盖方法") &&
                            (validateResult.contains("未覆盖方法：无") || validateResult.contains("未覆盖方法:无") ||
                                    validateResult.contains("未覆盖方法】：无") || validateResult.contains("未覆盖方法】:无")));

            if (noFatalError && allMethodsCovered) {
                isValidationPassed = true;
            }
        }

        if (isValidationPassed) {
            errorInfo.setErrorLine(-1);
            errorInfo.setErrorType("无");
            errorInfo.setErrorReason("验证通过");
            //   errorInfo.setSeverity(ErrorSeverity.SUGGESTION);

            // 提取优化建议（如果有）
            extractSuggestions(validateResult, errorInfo);
            return errorInfo;
        }

        // 匹配致命错误格式（支持多种格式）
        // 格式1: 该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[原因]
        // 格式2: 【致命错误】该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[原因]
        // 格式3: 错误位置：[3,4,5行] （支持多行号，可能带"行"字）
        Pattern fatalPattern = Pattern.compile("(?:【致命错误】[：:])?该测试代码在验证\\[(.*?)\\]时失败，错误位置[：:]\\s*\\[?([\\d,]+)(?:行)?\\]?，错误原因[：:]\\s*(.*)");
        Matcher fatalMatcher = fatalPattern.matcher(validateResult);

        if (fatalMatcher.find()) {
            errorInfo.setErrorType(fatalMatcher.group(1));

            // 解析行号（可能是单个或多个，如 "3" 或 "3,4,5"）
            String lineNumbers = fatalMatcher.group(2);
            if (lineNumbers.contains(",")) {
                // 多行号，取第一个
                errorInfo.setErrorLine(Integer.parseInt(lineNumbers.split(",")[0].trim()));
            } else {
                errorInfo.setErrorLine(Integer.parseInt(lineNumbers.trim()));
            }

            errorInfo.setErrorReason(fatalMatcher.group(3).trim());

            // 根据错误类型判断严重程度
            errorInfo.setSeverity(classifyErrorSeverity(errorInfo.getErrorType(), errorInfo.getErrorReason()));

            // 提取优化建议（如果有）
            extractSuggestions(validateResult, errorInfo);
        } else {
            // 尝试匹配其他错误格式
            // 格式: 错误位置：第X行 或 错误行号：X
            Pattern altPattern = Pattern.compile("错误(?:位置|行号)[：:]\\s*(?:第)?(\\d+)(?:行)?");
            Matcher altMatcher = altPattern.matcher(validateResult);

            if (altMatcher.find()) {
                errorInfo.setErrorLine(Integer.parseInt(altMatcher.group(1)));

                // 尝试提取错误类型
                Pattern typePattern = Pattern.compile("(?:错误类型|验证项)[：:]\\s*(.+?)(?:\\n|，|$)");
                Matcher typeMatcher = typePattern.matcher(validateResult);
                if (typeMatcher.find()) {
                    errorInfo.setErrorType(typeMatcher.group(1).trim());
                } else {
                    errorInfo.setErrorType("未知错误");
                }

                // 尝试提取错误原因
                Pattern reasonPattern = Pattern.compile("(?:错误原因|原因)[：:]\\s*(.+?)(?:\\n|$)");
                Matcher reasonMatcher = reasonPattern.matcher(validateResult);
                if (reasonMatcher.find()) {
                    errorInfo.setErrorReason(reasonMatcher.group(1).trim());
                } else {
                    errorInfo.setErrorReason("详见验证结果");
                }

                errorInfo.setSeverity(classifyErrorSeverity(errorInfo.getErrorType(), errorInfo.getErrorReason()));
            } else {
                // 真的无法解析，但如果没有明确的错误信息，默认为通过
                // 检查是否有明确的失败标志
                if (validateResult.contains("未通过") || validateResult.contains("不通过") ||
                        validateResult.contains("失败") || validateResult.contains("错误")) {
                    errorInfo.setErrorLine(-1);
                    errorInfo.setErrorType("未知");
                    errorInfo.setErrorReason("无法解析验证结果，但检测到可能存在问题");
                    errorInfo.setSeverity(ErrorSeverity.WARNING);
                } else {
                    // 没有明确的失败标志，默认为通过
                    errorInfo.setErrorLine(-1);
                    errorInfo.setErrorType("无");
                    errorInfo.setErrorReason("验证通过（未检测到错误）");
                    // errorInfo.setSeverity(ErrorSeverity.SUGGESTION);
                }
            }

            // 提取优化建议（如果有）
            extractSuggestions(validateResult, errorInfo);
        }

        return errorInfo;
    }

    /**
     * 根据错误类型和原因分类错误严重程度
     * FATAL: 必须修复（编译错误、运行时错误、断言值错误）
     * WARNING: 建议修复（可能影响测试质量）
     * SUGGESTION: 可选修复（不影响编译运行）
     */
    private ErrorSeverity classifyErrorSeverity(String errorType, String errorReason) {
        String typeLower = errorType.toLowerCase();
        String reasonLower = errorReason.toLowerCase();

        // ===== 致命错误：必须修复 =====
        // 语法错误
        if (typeLower.contains("语法") || reasonLower.contains("syntax") ||
                reasonLower.contains("括号") || reasonLower.contains("分号")) {
            return ErrorSeverity.FATAL;
        }
        // 编译错误
        if (typeLower.contains("编译") || reasonLower.contains("compile") ||
                reasonLower.contains("cannot find symbol") || reasonLower.contains("找不到符号") ||
                reasonLower.contains("类型不匹配") || reasonLower.contains("type mismatch") ||
                reasonLower.contains("未定义") || reasonLower.contains("undefined")) {
            return ErrorSeverity.FATAL;
        }
        // 断言值错误
        if (typeLower.contains("数值") || typeLower.contains("断言") ||
                reasonLower.contains("expected") || reasonLower.contains("期望") ||
                reasonLower.contains("assertEquals") || reasonLower.contains("断言失败")) {
            return ErrorSeverity.FATAL;
        }
        // 运行时错误
        if (typeLower.contains("运行") || reasonLower.contains("runtime") ||
                reasonLower.contains("nullpointer") || reasonLower.contains("空指针") ||
                reasonLower.contains("arrayindexoutofbounds") || reasonLower.contains("数组越界")) {
            return ErrorSeverity.FATAL;
        }

        // ===== 建议类：不影响编译运行 =====
        // 健壮性建议
        if (typeLower.contains("健壮") || reasonLower.contains("robust") ||
                reasonLower.contains("建议") || reasonLower.contains("suggest")) {
            //  return ErrorSeverity.SUGGESTION;
        }
        // 覆盖率建议
        if (typeLower.contains("覆盖") || typeLower.contains("边界") ||
                reasonLower.contains("coverage") || reasonLower.contains("boundary")) {
            //  return ErrorSeverity.SUGGESTION;
        }
        // 代码风格
        if (typeLower.contains("风格") || typeLower.contains("命名") ||
                reasonLower.contains("style") || reasonLower.contains("naming")) {
            //   return ErrorSeverity.SUGGESTION;
        }
        // 未使用的变量（不影响编译运行）
        if (reasonLower.contains("未使用") || reasonLower.contains("unused") ||
                reasonLower.contains("never used") || reasonLower.contains("从未使用")) {
            //   return ErrorSeverity.SUGGESTION;
        }

        // 默认为警告级别
        return ErrorSeverity.WARNING;
    }

    /**
     * 从验证结果中提取优化建议
     */
    private void extractSuggestions(String validateResult, ErrorInfo errorInfo) {
        // 匹配优化建议格式
        Pattern suggestionPattern = Pattern.compile("【优化建议】[：:]?(.+?)(?=【|$)", Pattern.DOTALL);
        Matcher matcher = suggestionPattern.matcher(validateResult);

        List<String> suggestions = new ArrayList<>();
        while (matcher.find()) {
            String suggestion = matcher.group(1).trim();
            if (!suggestion.isEmpty()) {
                suggestions.add(suggestion);
            }
        }
        //  errorInfo.setSuggestions(suggestions);
    }

    /**
     * 判断错误是否需要修复（只有致命错误才需要修复）
     */
    private boolean needsFix(ErrorInfo errorInfo) {
        return errorInfo.getSeverity() == ErrorSeverity.FATAL && errorInfo.getErrorLine() > 0;
    }

    // ========== 新增：动态二次验证方法 ==========
    /**
     * 动态二次验证：根据原始错误和修复策略动态调整验证重点
     *
     * 核心思想：
     * - 修复后的验证应聚焦于原错误是否被修复
     * - 同时检查修复是否引入新问题
     * - 不重复检查已通过的验证项
     *
     * @param fixedTestCode 修复后的测试代码
     * @param originalError 原始错误信息
     * @param usedStrategy  使用的修复策略
     * @return 二次验证结果
     */
    private ErrorInfo revalidateTestCode(String fixedTestCode, ErrorInfo originalError, FixScope usedStrategy) throws IOException {
        // 构建动态验证提示词
        String dynamicValidatePrompt = buildDynamicValidatePrompt(originalError, usedStrategy);

        // 添加行号便于定位
        String numberedCode = addLineNumbers(fixedTestCode);

        // 完整提示词
        String fullPrompt = dynamicValidatePrompt +
                "\n=== 待验证的修复后代码 ===\n" +
                "```java\n" + numberedCode + "\n```";

        // 调用大模型验证
        String validateResult = sendRequestAndGetResponse0("", fullPrompt);
        System.out.println("动态二次验证结果：\n" + validateResult);

        // 解析验证结果
        return parseValidateResult(validateResult, fixedTestCode);
    }

    /**
     * 根据原始错误和修复策略构建动态验证提示词
     */
    private String buildDynamicValidatePrompt(ErrorInfo originalError, FixScope usedStrategy) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是测试代码审查专家，请对修复后的测试代码进行针对性验证。\n\n");

        // 1. 说明原始错误
        prompt.append("=== 原始错误信息 ===\n");
        prompt.append("错误类型：").append(originalError.getErrorType()).append("\n");
        prompt.append("错误行号：").append(originalError.getErrorLine()).append("\n");
        prompt.append("错误原因：").append(originalError.getErrorReason()).append("\n");
        prompt.append("修复策略：").append(getFixScopeDesc(usedStrategy)).append("\n\n");

        // 2. 动态验证重点
        prompt.append("=== 验证重点（按优先级） ===\n");

        // 根据原始错误类型确定验证重点
        String errorType = originalError.getErrorType().toLowerCase();
        String errorReason = originalError.getErrorReason().toLowerCase();

        // 优先验证原错误是否修复
        prompt.append("1. 【核心】原错误是否已修复：检查第 ").append(originalError.getErrorLine())
                .append(" 行附近的 ").append(originalError.getErrorType()).append(" 问题是否已解决\n");

        // 根据修复策略添加额外验证项
        if (usedStrategy == FixScope.SINGLE_LINE) {
            prompt.append("2. 【重要】单行修复副作用：检查修复是否破坏了与其他行的依赖关系\n");
            prompt.append("3. 【次要】语法正确性：确保修复后的行语法正确\n");
        } else if (usedStrategy == FixScope.METHOD_BODY) {
            prompt.append("2. 【重要】方法体完整性：检查修复后的方法体逻辑是否完整\n");
            prompt.append("3. 【重要】变量作用域：检查方法内变量声明和使用是否正确\n");
            prompt.append("4. 【次要】断言正确性：检查断言的期望值是否合理\n");
        } else {
            prompt.append("2. 【重要】类结构完整性：检查import、类声明、方法签名是否正确\n");
            prompt.append("3. 【重要】编译可行性：确保代码可以编译通过\n");
            prompt.append("4. 【次要】测试逻辑：检查测试用例是否有意义\n");
        }

        // 3. 明确不需要检查的项（避免重复报告已知问题）
        prompt.append("\n=== 无需检查的项 ===\n");
        prompt.append("- 待测试类的导入（系统会自动导入）\n");
        prompt.append("- 边界情况覆盖不全（仅作为优化建议，不视为错误）\n");
        prompt.append("- 代码风格问题（不影响编译运行）\n");
        prompt.append("- 未使用的变量（不影响编译运行，可保留）\n");

        // 4. 输出格式
        prompt.append("\n=== 输出格式 ===\n");
        prompt.append("- 若原错误已修复且无新的致命错误，返回：【全部验证通过】\n");
        prompt.append("- 若原错误未修复或出现新的致命错误，返回：\n");
        prompt.append("  该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[详细描述]\n");
        prompt.append("- 若有优化建议（非致命），可在最后附加：【优化建议】：[建议内容]\n");

        return prompt.toString();
    }

    /**
     * 判断错误是否影响编译运行（用于兜底策略）
     *
     * @param errorInfo 错误信息
     * @return true=影响编译运行，需要处理；false=不影响，可保留
     */
    private boolean affectsCompilationOrRuntime(ErrorInfo errorInfo) {
        // 只有致命错误才影响编译运行
        if (errorInfo.getSeverity() != ErrorSeverity.FATAL) {
            return false;
        }

        String errorReason = errorInfo.getErrorReason().toLowerCase();

        // 不影响编译运行的情况
        if (errorReason.contains("未使用") || errorReason.contains("unused") ||
                errorReason.contains("never used") || errorReason.contains("从未使用") ||
                errorReason.contains("建议") || errorReason.contains("suggest") ||
                errorReason.contains("风格") || errorReason.contains("style") ||
                errorReason.contains("命名") || errorReason.contains("naming") ||
                errorReason.contains("覆盖率") || errorReason.contains("coverage") ||
                errorReason.contains("健壮性") || errorReason.contains("robust")) {
            return false;
        }

        // 影响编译运行的情况
        return true;
    }

    /**
     * 统计字符串中指定字符的数量
     */
    private int countChar(String str, char c) {
        int count = 0;
        for (char ch : str.toCharArray()) {
            if (ch == c) {
                count++;
            }
        }
        return count;
    }


    private int getLineCount(String code) {
        return code.split("\\n").length;
    }

    // ========== 目标最优修复策略 ==========
    /**
     * 目标最优修复策略：根据错误特征智能选择最小有效修复范围
     *
     * 核心原则：
     * - 最小修改原则：优先选择影响范围最小的修复策略
     * - 渐进升级原则：低级策略失败后自动升级到高级策略
     *
     * 策略判定规则：
     *
     * 1. SINGLE_LINE（单行修复）- 最优先
     *    适用场景：
     *    - 断言期望值错误（assertEquals的expected值计算错误）
     *    - 单行字面量/常量错误
     *    - 该行不依赖同方法内其他变量，或依赖的变量值是正确的
     *    判定条件：
     *    - 错误行以分号结尾（完整语句）
     *    - 错误行不包含变量声明（不影响后续行）
     *    - 错误行是断言语句或简单方法调用
     *
     * 2. METHOD_BODY（方法体修复）- 次优先
     *    适用场景：
     *    - 错误行依赖同方法内其他行的变量
     *    - 变量声明错误（会影响后续使用该变量的行）
     *    - 测试逻辑错误（需要调整多行代码）
     *    - 单行修复失败后自动升级
     *    判定条件：
     *    - 错误位于@Test方法体内
     *    - 错误行包含变量声明，或使用了同方法内声明的变量
     *
     * 3. FULL_CLASS（全类修复）- 兜底
     *    适用场景：
     *    - import语句错误
     *    - 类声明/包声明错误
     *    - 错误不在任何@Test方法内
     *    - 方法体修复失败后自动升级
     */
    private FixScope determineFixScope(ErrorInfo errorInfo) {
        String testCode = errorInfo.getTestCode();

        // 防御性检查：无测试代码
        if (testCode == null || testCode.isEmpty()) {
            System.out.println("策略判定：测试代码为空，采用全类修复");
            return FixScope.FULL_CLASS;
        }

        int errorLine = errorInfo.getErrorLine();
        String[] codeLines = testCode.split("\\n");

        // 防御性检查：无有效错误行号
        if (errorLine <= 0 || errorLine > codeLines.length) {
            System.out.println("策略判定：无有效错误行号，采用全类修复");
            return FixScope.FULL_CLASS;
        }

        String errorType = errorInfo.getErrorType();
        String errorReason = errorInfo.getErrorReason();
        String errorLineContent = codeLines[errorLine - 1].trim();

        // ===== 规则1：类级别错误 → 全类修复 =====
        if (isClassLevelError(errorLineContent, errorReason)) {
            System.out.println("策略判定：类级别错误（import/package/class声明），采用全类修复");
            return FixScope.FULL_CLASS;
        }

        // ===== 规则2：错误不在@Test方法内 → 全类修复 =====
        if (!isInTestMethodBody(errorLine, testCode)) {
            System.out.println("策略判定：错误不在@Test方法体内，采用全类修复");
            return FixScope.FULL_CLASS;
        }

        // ===== 规则3：判断是否可以单行修复 =====
        // 单行修复条件：独立语句 + 不影响其他行
        if (canFixSingleLine(errorLineContent, errorLine, codeLines, errorReason)) {
            System.out.println("策略判定：独立单行错误，采用单行修复");
            return FixScope.SINGLE_LINE;
        }

        // ===== 规则4：默认方法体修复 =====
        System.out.println("策略判定：错误行与其他行有依赖关系，采用方法体修复");
        return FixScope.METHOD_BODY;
    }

    /**
     * 判断是否为类级别错误
     */
    private boolean isClassLevelError(String errorLineContent, String errorReason) {
        // 错误行是import/package/class声明
        if (errorLineContent.startsWith("import ") ||
                errorLineContent.startsWith("package ") ||
                errorLineContent.contains("public class ") ||
                errorLineContent.contains("class ") && errorLineContent.contains("{")) {
            return true;
        }

        // 错误原因涉及类级别问题
        String reasonLower = errorReason.toLowerCase();
        return reasonLower.contains("import") ||
                reasonLower.contains("cannot find symbol") ||
                reasonLower.contains("找不到符号") ||
                reasonLower.contains("包") && reasonLower.contains("错误") ||
                reasonLower.contains("类名");
    }

    /**
     * 判断是否可以单行修复
     *
     * 条件：
     * 1. 是完整语句（以分号结尾）
     * 2. 不是变量声明语句（变量声明会影响后续行）
     * 3. 是断言语句或简单表达式
     * 4. 不依赖同方法内其他行定义的可变状态
     */
    private boolean canFixSingleLine(String errorLineContent, int errorLine,
                                     String[] codeLines, String errorReason) {
        // 条件1：必须是完整语句
        if (!errorLineContent.endsWith(";")) {
            return false;
        }

        // 条件2：不能是变量声明语句（会影响后续行）
        // 匹配模式：Type varName = ... 或 Type varName;
        if (isVariableDeclaration(errorLineContent)) {
            return false;
        }

        // 条件3：不能包含块结构
        if (errorLineContent.contains("{") || errorLineContent.contains("}")) {
            return false;
        }

        // 条件4：断言语句的期望值错误 → 可以单行修复
        if (isAssertionStatement(errorLineContent)) {
            String reasonLower = errorReason.toLowerCase();
            // 如果是期望值/结果值错误，可以单行修复
            if (reasonLower.contains("期望") || reasonLower.contains("expected") ||
                    reasonLower.contains("数值") || reasonLower.contains("结果") ||
                    reasonLower.contains("值")) {
                return true;
            }
        }

        // 条件5：简单方法调用（不涉及变量赋值）
        // 例如：obj.method(); 或 ClassName.staticMethod();
        if (errorLineContent.matches("^[a-zA-Z_][a-zA-Z0-9_]*\\.[a-zA-Z_][a-zA-Z0-9_]*\\(.*\\);$")) {
            // 检查是否依赖局部变量
            if (!usesLocalVariable(errorLineContent, errorLine, codeLines)) {
                return true;
            }
        }

        return false;
    }

    /**
     * 判断是否为变量声明语句
     */
    private boolean isVariableDeclaration(String line) {
        // 匹配常见的变量声明模式
        // Type varName = value;
        // Type varName;
        // final Type varName = value;
        String trimmed = line.trim();

        // 排除方法调用和断言
        if (trimmed.startsWith("assert") || trimmed.contains("(") && trimmed.indexOf("(") < trimmed.indexOf("=")) {
            return false;
        }

        // 匹配：Type varName = 或 Type varName;
        return trimmed.matches("^(final\\s+)?[A-Z][a-zA-Z0-9_<>,\\s]*\\s+[a-z][a-zA-Z0-9_]*\\s*[=;].*");
    }

    /**
     * 判断是否为断言语句
     */
    private boolean isAssertionStatement(String line) {
        return line.contains("assertEquals") ||
                line.contains("assertTrue") ||
                line.contains("assertFalse") ||
                line.contains("assertNull") ||
                line.contains("assertNotNull") ||
                line.contains("assertThrows") ||
                line.contains("assertArrayEquals") ||
                line.startsWith("assert ");
    }

    /**
     * 检查错误行是否使用了同方法内定义的局部变量
     */
    private boolean usesLocalVariable(String errorLineContent, int errorLine, String[] codeLines) {
        // 提取错误行中的标识符
        Set<String> identifiers = extractIdentifiers(errorLineContent);

        // 向上查找方法体内的变量声明
        int methodStartLine = findMethodStartLine(errorLine, codeLines);

        for (int i = methodStartLine; i < errorLine - 1; i++) {
            String line = codeLines[i].trim();
            // 检查是否有变量声明
            if (isVariableDeclaration(line)) {
                // 提取变量名
                String varName = extractVariableName(line);
                if (varName != null && identifiers.contains(varName)) {
                    return true; // 使用了局部变量
                }
            }
        }
        return false;
    }

    /**
     * 提取代码行中的标识符
     */
    private Set<String> extractIdentifiers(String line) {
        Set<String> identifiers = new HashSet<>();
        Pattern pattern = Pattern.compile("\\b([a-zA-Z_][a-zA-Z0-9_]*)\\b");
        Matcher matcher = pattern.matcher(line);
        while (matcher.find()) {
            String id = matcher.group(1);
            if (!isKeywordOrCommonClass(id)) {
                identifiers.add(id);
            }
        }
        return identifiers;
    }

    /**
     * 从变量声明语句中提取变量名
     */
    private String extractVariableName(String declarationLine) {
        // 匹配：Type varName = 或 Type varName;
        Pattern pattern = Pattern.compile("(?:final\\s+)?[A-Z][a-zA-Z0-9_<>,\\s]*\\s+([a-z][a-zA-Z0-9_]*)\\s*[=;]");
        Matcher matcher = pattern.matcher(declarationLine);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 查找错误行所在方法的起始行
     */
    private int findMethodStartLine(int errorLine, String[] codeLines) {
        for (int i = errorLine - 1; i >= 0; i--) {
            String line = codeLines[i].trim();
            if (line.startsWith("@Test") ||
                    (line.contains("void ") && line.contains("(") && line.contains("{"))) {
                return i;
            }
        }
        return 0;
    }    /**
     * 判断是否为Java关键字或常见类名
     */
    private boolean isKeywordOrCommonClass(String word) {
        Set<String> keywords = new HashSet<>(Arrays.asList(
                "int", "long", "double", "float", "boolean", "char", "byte", "short", "void",
                "if", "else", "for", "while", "do", "switch", "case", "break", "continue", "return",
                "try", "catch", "finally", "throw", "throws", "new", "this", "super", "null", "true", "false",
                "public", "private", "protected", "static", "final", "abstract", "class", "interface",
                "String", "Integer", "Long", "Double", "Float", "Boolean", "Object", "List", "Map", "Set",
                "assertEquals", "assertTrue", "assertFalse", "assertNull", "assertNotNull", "assertThrows",
                "Test", "Before", "After", "BeforeEach", "AfterEach"
        ));
        return keywords.contains(word);
    }
    /**
     * 判定错误行是否在@Test方法体内
     */
    private boolean isInTestMethodBody(int errorLine, String testCode) {
        String[] lines = testCode.split("\\n");

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            // 查找@Test注解或测试方法声明
            if (line.startsWith("@Test") || (line.contains("public void test") && line.contains("("))) {
                // 找到方法开始，计算方法体范围
                int methodStartLine = i + 1;
                int braceCount = 0;
                boolean foundOpenBrace = false;

                for (int j = i; j < lines.length; j++) {
                    String innerLine = lines[j];
                    for (char c : innerLine.toCharArray()) {
                        if (c == '{') {
                            braceCount++;
                            foundOpenBrace = true;
                        } else if (c == '}') {
                            braceCount--;
                        }
                    }

                    if (foundOpenBrace && braceCount == 0) {
                        int methodEndLine = j + 1;
                        // 检查错误行是否在此方法体内
                        if (errorLine >= methodStartLine && errorLine <= methodEndLine) {
                            return true;
                        }
                        break;
                    }
                }
            }
        }
        return false;
    }


    /**
     * 根据修复范围提取错误代码片段
     *
     * @param errorInfo 错误信息
     * @param fixScope  修复范围
     * @return 错误代码片段 + 片段在原代码中的行号范围（数组：[startLine, endLine]）
     */
    private Map<String, Object> extractCodeSnippet(ErrorInfo errorInfo, FixScope fixScope) {
        Map<String, Object> result = new HashMap<>();
        String testCode = errorInfo.getTestCode();
        int errorLine = errorInfo.getErrorLine();
        String[] codeLines = testCode.split("\\n");

        switch (fixScope) {
            case SINGLE_LINE:
                // 提取错误行 + 前后1行上下文（避免单行依赖）
                int singleStart = Math.max(0, errorLine - 2);
                int singleEnd = Math.min(codeLines.length - 1, errorLine);
                StringBuilder singleSnippet = new StringBuilder();
                for (int i = singleStart; i <= singleEnd; i++) {
                    singleSnippet.append(codeLines[i]).append("\n");
                }
                result.put("snippet", singleSnippet.toString());
                result.put("lineRange", new int[]{singleStart + 1, singleEnd + 1}); // 转成1-based行号
                break;

            case METHOD_BODY:
                // 提取错误行所在的@Test方法体
                Pattern testMethodPattern = Pattern.compile("@Test\\s+public\\s+void\\s+(\\w+)\\(.*?\\)\\s*\\{([\\s\\S]*?)\\}", Pattern.MULTILINE);
                Matcher matcher = testMethodPattern.matcher(testCode);
                int methodStart = -1;
                int methodEnd = -1;
                String methodSnippet = "";

                while (matcher.find()) {
                    int start = testCode.indexOf(matcher.group(0));
                    int end = start + matcher.group(0).length();
                    // 计算方法体的行号范围
                    int startLine = countLine(testCode, start) + 1;
                    int endLine = countLine(testCode, end) + 1;

                    if (errorLine >= startLine && errorLine <= endLine) {
                        methodStart = startLine;
                        methodEnd = endLine;
                        methodSnippet = matcher.group(0);
                        break;
                    }
                }

                if (methodStart == -1) {
                    // 未找到@Test方法，降级为全类修复
                    result.put("snippet", testCode);
                    result.put("lineRange", new int[]{1, codeLines.length});
                } else {
                    result.put("snippet", methodSnippet);
                    result.put("lineRange", new int[]{methodStart, methodEnd});
                }
                break;

            case FULL_CLASS:
                // 提取全类代码
                result.put("snippet", testCode);
                result.put("lineRange", new int[]{1, codeLines.length});
                break;
        }

        return result;
    }

    /**
     * 计算字符串中指定索引位置的行号（0-based → 1-based）
     */
    private int countLine(String str, int index) {
        if (index >= str.length()) {
            return str.split("\\n").length - 1;
        }
        int lineCount = 0;
        for (int i = 0; i < index; i++) {
            if (str.charAt(i) == '\n') {
                lineCount++;
            }
        }
        return lineCount;
    }

    /**
     * 将修复后的测试代码写回文件（覆盖原文件）
     *
     * @param testCode     修复后的测试代码
     * @param outputPath   文件输出路径
     * @throws IOException 文件写入异常
     */
    public void writeTestCodeToFile(String testCode, String outputPath) throws IOException {
        Path path = Paths.get(outputPath);
        // 确保目录存在
        if (Files.notExists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        // 覆盖写入文件
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write(testCode);
        }
        System.out.println("修复后的测试代码已覆盖写入：" + outputPath);
    }

    // ========== 真实编译验证相关方法 ==========

    /**
     * 真实编译验证结果类
     */
    static class CompileResult {
        private boolean success;           // 编译是否成功
        private String errorMessage;       // 编译错误信息
        private int errorLine;             // 错误行号
        private String errorType;          // 错误类型

        public CompileResult(boolean success) {
            this.success = success;
            this.errorMessage = "";
            this.errorLine = -1;
            this.errorType = "";
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getErrorLine() { return errorLine; }
        public void setErrorLine(int errorLine) { this.errorLine = errorLine; }
        public String getErrorType() { return errorType; }
        public void setErrorType(String errorType) { this.errorType = errorType; }
    }

    /**
     * 执行真实编译验证
     *
     * @param testCode 测试代码
     * @param classInfo 类信息（包含包路径、类名）
     * @return 编译结果
     */
    public CompileResult realCompileValidation(String testCode, Map<String, String> classInfo) {
        CompileResult result = new CompileResult(false);
        String testFilePath = getTestFilePath(classInfo);

        try {
            // 1. 先将测试代码写入临时文件
            writeTestCodeToFile(testCode, testFilePath);

            // 2. 执行 mvn test-compile 编译测试类
            String compileOutput = executeMavenCompileTest();

            // 3. 解析编译结果
            if (compileOutput.contains("BUILD SUCCESS")) {
                result.setSuccess(true);
                System.out.println("    ✓ 真实编译验证通过");
            } else {
                result.setSuccess(false);
                // 解析编译错误信息
                parseCompileError(compileOutput, result, classInfo.get("类名"));
                System.out.println("    ✗ 真实编译验证失败: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            result.setSuccess(false);
            result.setErrorMessage("编译过程异常: " + e.getMessage());
            result.setErrorType("编译异常");
        }

        return result;
    }

    // ========== 新增：运行验证结果类 ==========
    static class RunResult {
        private boolean success;           // 运行是否成功
        private String errorMessage;       // 运行错误信息
        private int failedTests;           // 失败的测试数
        private int totalTests;            // 总测试数
        private String failureDetails;     // 失败详情

        public RunResult(boolean success) {
            this.success = success;
            this.errorMessage = "";
            this.failedTests = 0;
            this.totalTests = 0;
            this.failureDetails = "";
        }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }
        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }
        public String getFailureDetails() { return failureDetails; }
        public void setFailureDetails(String failureDetails) { this.failureDetails = failureDetails; }
    }

    /**
     * 执行真实运行验证（编译+运行测试）
     *
     * @param testCode 测试代码
     * @param classInfo 类信息
     * @return 运行结果
     */
    public RunResult realRunValidation(String testCode, Map<String, String> classInfo) {
        RunResult result = new RunResult(false);
        String testFilePath = getTestFilePath(classInfo);

        // 处理泛型类名：移除泛型参数部分
        String className = classInfo.get("类名");
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<")).trim();
        }
        String testClassName = className + "Test";

        try {
            // 1. 先写入测试代码
            writeTestCodeToFile(testCode, testFilePath);

            // 2. 执行 mvn test 运行测试
            String testOutput = executeMavenTest(testClassName);

            // 3. 解析运行结果
            if (testOutput.contains("BUILD SUCCESS") && !testOutput.contains("Failures: 0") && !testOutput.contains("Errors: 0")) {
                result.setSuccess(true);
                System.out.println("    ✓ 真实运行验证通过");
            } else {
                result.setSuccess(false);
                // 解析运行错误信息
                parseRunError(testOutput, result, testClassName);
                System.out.println("    ✗ 真实运行验证失败: " + result.getErrorMessage());
            }
        } catch (IOException e) {
            result.setSuccess(false);
            result.setErrorMessage("运行过程异常: " + e.getMessage());
        }

        return result;
    }

    /**
     * 执行 Maven 测试运行命令
     */
    private String executeMavenTest(String testClassName) throws IOException {
        String mavenPath = "D:\\TopNew-SpringBoot\\maven\\apache-maven-3.9.1-bin\\apache-maven-3.9.1\\bin\\mvn.cmd";

        List<String> commandList = new ArrayList<>();
        commandList.add(mavenPath);
        commandList.add("test");
        commandList.add("-Dtest=" + testClassName);

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Windows系统Maven输出使用GBK编码
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(),
                System.getProperty("os.name").toLowerCase().contains("win") ? java.nio.charset.Charset.forName("GBK") : StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 根据退出码兜底判断
        if (!output.toString().contains("BUILD SUCCESS") && !output.toString().contains("BUILD FAILURE")) {
            if (exitCode == 0) {
                output.append("\nBUILD SUCCESS\n");
            } else {
                output.append("\nBUILD FAILURE\n");
            }
        }

        return output.toString();
    }

    /**
     * 解析运行错误信息
     */
    private void parseRunError(String testOutput, RunResult result, String testClassName) {
        String[] lines = testOutput.split("\n");

        StringBuilder errorMsg = new StringBuilder();
        int failedTests = 0;
        int totalTests = 0;
        StringBuilder failureDetails = new StringBuilder();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 匹配测试统计信息：Tests run: X, Failures: Y, Errors: Z
            if (line.contains("Tests run:")) {
                Pattern statsPattern = Pattern.compile("Tests run: (\\d+), Failures: (\\d+), Errors: (\\d+)");
                Matcher matcher = statsPattern.matcher(line);
                if (matcher.find()) {
                    totalTests = Integer.parseInt(matcher.group(1));
                    failedTests = Integer.parseInt(matcher.group(2)) + Integer.parseInt(matcher.group(3));
                }
            }

            // 匹配失败详情：[ERROR]   TestClass.testMethod:lineNum expected: <X> but was: <Y>
            if (line.contains("[ERROR]") && line.contains(testClassName)) {
                failureDetails.append(line).append("\n");
                // 读取后续几行的详细信息
                for (int j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                    String nextLine = lines[j];
                    // 如果遇到下一个[ERROR]或空行，停止
                    if (nextLine.trim().isEmpty() || (nextLine.contains("[ERROR]") && nextLine.contains(testClassName))) {
                        break;
                    }
                    failureDetails.append(nextLine).append("\n");
                }
            }

            // 匹配FAILURE!或ERROR!
            if (line.contains("FAILURE!") || line.contains("ERROR!")) {
                failureDetails.append(line).append("\n");
                for (int j = i + 1; j < Math.min(i + 5, lines.length); j++) {
                    failureDetails.append(lines[j]).append("\n");
                }
            }
        }

        if (failedTests > 0) {
            errorMsg.append("运行失败: ").append(failedTests).append("/").append(totalTests).append(" 个测试失败");
        } else {
            errorMsg.append("运行失败，请检查测试代码");
        }

        result.setErrorMessage(errorMsg.toString());
        result.setFailedTests(failedTests);
        result.setTotalTests(totalTests);
        result.setFailureDetails(failureDetails.toString());
    }

    /**
     * 执行 Maven 测试编译命令
     */
    private String executeMavenCompileTest() throws IOException {
        String mavenPath = "D:\\TopNew-SpringBoot\\maven\\apache-maven-3.9.1-bin\\apache-maven-3.9.1\\bin\\mvn.cmd";

        List<String> commandList = new ArrayList<>();
        commandList.add(mavenPath);
        commandList.add("test-compile");
        // 移除-q静默模式，确保能获取完整输出包括BUILD SUCCESS/FAILURE

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();

        // Windows系统Maven输出使用GBK编码，需要用系统默认编码读取
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), System.getProperty("os.name").toLowerCase().contains("win") ? java.nio.charset.Charset.forName("GBK") : StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        int exitCode = 0;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 如果输出中没有BUILD SUCCESS/FAILURE，根据退出码判断
        if (!output.toString().contains("BUILD SUCCESS") && !output.toString().contains("BUILD FAILURE")) {
            if (exitCode == 0) {
                output.append("\nBUILD SUCCESS\n");
            } else {
                output.append("\nBUILD FAILURE\n");
            }
        }

        return output.toString();
    }

    /**
     * 解析编译错误信息
     */
    private void parseCompileError(String compileOutput, CompileResult result, String className) {
        // 查找与当前测试类相关的错误
        String testClassName = className + "Test";
        String[] lines = compileOutput.split("\n");

        StringBuilder errorMsg = new StringBuilder();
        int errorLine = -1;
        String errorType = "编译错误";

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            // 匹配错误行，格式如：[ERROR] /path/to/File.java:[行号,列号] 错误信息
            if (line.contains("[ERROR]") && line.contains(testClassName)) {
                // 提取行号
                Pattern linePattern = Pattern.compile(":\\[(\\d+),\\d+\\]|:(\\d+):");
                Matcher matcher = linePattern.matcher(line);
                if (matcher.find()) {
                    String lineNum = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
                    if (lineNum != null) {
                        errorLine = Integer.parseInt(lineNum);
                    }
                }

                // 提取错误信息
                int errorIdx = line.indexOf("错误:") != -1 ? line.indexOf("错误:") : line.indexOf("error:");
                if (errorIdx != -1) {
                    errorMsg.append(line.substring(errorIdx)).append(" ");
                } else {
                    errorMsg.append(line.substring(line.indexOf("[ERROR]") + 7).trim()).append(" ");
                }

                // 判断错误类型
                if (line.contains("找不到符号") || line.contains("cannot find symbol")) {
                    errorType = "符号未找到";
                } else if (line.contains("程序包") && line.contains("不存在") || line.contains("package") && line.contains("does not exist")) {
                    errorType = "包不存在";
                } else if (line.contains("不兼容的类型") || line.contains("incompatible types")) {
                    errorType = "类型不兼容";
                }
            }
        }

        if (errorMsg.length() == 0) {
            // 如果没找到具体错误，取整体错误信息
            errorMsg.append("编译失败，请检查代码");
        }

        result.setErrorMessage(errorMsg.toString().trim());
        result.setErrorLine(errorLine);
        result.setErrorType(errorType);
    }

    /**
     * 基于真实编译错误进行修复
     *
     * @param testCode 原测试代码
     * @param compileResult 编译结果
     * @param classInfo 类信息
     * @param targetPackage 目标包名
     * @return 修复后的代码
     */
    public String fixByCompileError(String testCode, CompileResult compileResult,
                                    Map<String, String> classInfo, String targetPackage) throws IOException {
        String className = classInfo.get("类名");

        // 使用classInfo中的实际包路径，而不是传入的targetPackage
        String actualPackage = classInfo.get("包路径");
        if (actualPackage == null || actualPackage.isEmpty()) {
            actualPackage = targetPackage; // 兜底使用传入的包路径
        }

        // 读取待测类及其依赖类的源代码，提供给LLM作为参考
        StringBuilder sourceCodeContext = new StringBuilder();
        String sourceBasePath = "src/main/java/" + actualPackage.replace(".", "/") + "/";

        // 从编译错误和测试代码中动态提取涉及的类名
        String errorMsg = compileResult.getErrorMessage() != null ? compileResult.getErrorMessage() : "";
        Map<String, String> relatedClassesWithPackage = new LinkedHashMap<>(); // className -> packagePath
        relatedClassesWithPackage.put(className, actualPackage); // 待测类优先

        // 为lambda表达式创建final变量
        final String finalTestCode = testCode;
        final String finalErrorMsg = errorMsg;
        final String finalActualPackage = actualPackage;

        // ========== 依赖查找策略：优先当前包，找不到再去父包 ==========
        // 1. 先扫描当前包目录下的类文件
        Path packageDir = Paths.get(sourceBasePath);
        if (Files.exists(packageDir) && Files.isDirectory(packageDir)) {
            try {
                Files.list(packageDir)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(p -> {
                            String clsName = p.getFileName().toString().replace(".java", "");
                            // 如果测试代码或错误信息中引用了该类，则加入
                            if (finalTestCode.contains(clsName) || finalErrorMsg.contains(clsName)) {
                                relatedClassesWithPackage.put(clsName, finalActualPackage);
                            }
                        });
            } catch (IOException e) {
                // 忽略扫描失败
            }
        }

        // 2. 如果当前包是子包（如cli.help），尝试从父包查找依赖
        if (actualPackage.contains(".")) {
            String parentPackage = actualPackage.substring(0, actualPackage.lastIndexOf("."));
            String parentBasePath = "src/main/java/" + parentPackage.replace(".", "/") + "/";
            Path parentDir = Paths.get(parentBasePath);

            if (Files.exists(parentDir) && Files.isDirectory(parentDir)) {
                try {
                    final String finalParentPackage = parentPackage;
                    Files.list(parentDir)
                            .filter(p -> p.toString().endsWith(".java"))
                            .forEach(p -> {
                                String clsName = p.getFileName().toString().replace(".java", "");
                                // 如果测试代码或错误信息中引用了该类，且当前包没有同名类
                                if ((finalTestCode.contains(clsName) || finalErrorMsg.contains(clsName))
                                        && !relatedClassesWithPackage.containsKey(clsName)) {
                                    relatedClassesWithPackage.put(clsName, finalParentPackage);
                                }
                            });
                } catch (IOException e) {
                    // 忽略扫描失败
                }
            }
        }

        // 读取相关类的源代码（限制数量避免token过多）
        int maxClasses = 5;
        int count = 0;
        for (Map.Entry<String, String> entry : relatedClassesWithPackage.entrySet()) {
            if (count >= maxClasses) break;

            String cls = entry.getKey();
            String pkgPath = entry.getValue();
            String sourcePath = "src/main/java/" + pkgPath.replace(".", "/") + "/" + cls + ".java";
            Path path = Paths.get(sourcePath);

            try {
                if (Files.exists(path)) {
                    String sourceCode = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    // 明确标注包路径，避免混淆
                    sourceCodeContext.append("\n=== ").append(pkgPath).append(".").append(cls).append(" 源代码 ===\n");
                    sourceCodeContext.append(sourceCode).append("\n");
                    count++;
                }
            } catch (IOException e) {
                // 忽略读取失败的文件
            }
        }

        // 构建修复提示词，包含源代码上下文
        String fixPrompt = "你是Java测试代码修复专家，请根据真实编译错误和源代码修复以下测试代码。\n\n" +
                "=== 重要约束 ===\n" +
                "1. 【关键】待测类在 " + actualPackage + " 包下，不是 org.apache.commons.xx！\n" +
                "2. 所有 import 语句必须使用 " + actualPackage + ".* 而不是 org.apache.commons.xx.*\n" +
                "3. 【关键】必须严格按照下面源代码中实际存在的构造函数和方法来创建对象！\n" +
                "4. 不要臆造不存在的构造器或方法，仔细查看源代码中的构造函数签名。\n" +
                "5. 【关键-依赖类查找规则】：\n" +
                "   - 优先使用当前包（" + actualPackage + "）下的类\n" +
                "   - 如果当前包没有需要的类，再使用父包的类\n" +
                "   - 在有子包情况中，优先用子包类文件，找不到再用父包类文件\n" +
                "   - 下面提供的源代码已经按照这个规则收集，直接使用即可\n" +
                "6. 【关键-受检异常处理】如果错误是\"未报告的异常错误\"或\"unreported exception\"：\n" +
                "   - 最简单的修复：在@Test方法签名中添加 throws 异常类型\n" +
                "   - 示例：将 @Test void testMethod() 改为 @Test void testMethod() throws ParseException\n" +
                "   - 不要使用try-catch，直接throws更简洁\n" +
                "   - 确保import了异常类（如 import " + actualPackage + ".ParseException;）\n\n" +
                "=== 编译错误信息 ===\n" +
                "错误类型: " + compileResult.getErrorType() + "\n" +
                "错误行号: " + compileResult.getErrorLine() + "\n" +
                "错误详情: " + errorMsg + "\n\n" +
                "=== 相关类的源代码（请仔细查看构造函数和方法签名）===\n" +
                sourceCodeContext.toString() + "\n" +
                "=== 待修复的测试代码 ===\n" +
                "```java\n" + testCode + "\n```\n\n" +
                "=== 修复策略（根据错误类型选择）===\n" +
                "- 如果是\"未报告的异常\"：在测试方法签名添加 throws 异常类型\n" +
                "- 如果是\"找不到构造器\"：查看源代码，使用正确的构造器参数\n" +
                "- 如果是\"方法不存在\"：查看源代码，使用正确的方法名和参数\n" +
                "- 如果是\"包路径错误\"：检查是否应该使用当前包（" + actualPackage + "）的类\n\n" +
                "=== 输出要求 ===\n" +
                "- 直接输出修复后的完整测试代码\n" +
                "- 不要输出解释、注释、思考过程\n" +
                "- 确保所有 import 使用正确的包路径: " + actualPackage + "\n" +
                "- 确保使用源代码中实际存在的构造器创建对象\n" +
                "- 受检异常问题优先使用 throws 声明而不是 try-catch\n";

        // 先尝试智能补全缺失的import
        String codeWithImports = autoFixMissingImports(testCode, compileResult);
        if (!codeWithImports.equals(testCode)) {
            // 如果添加了import，直接返回，让编译验证再次检查
            return codeWithImports;
        }

        // 如果不是import问题，再调用LLM修复
        String fixedCode = sendRequestAndGetResponse0("", fixPrompt);
        fixedCode = extractPureCode(fixedCode);

        if (fixedCode == null || fixedCode.isEmpty() || fixedCode.contains("修复失败")) {
            return testCode; // 修复失败返回原代码
        }

        return fixedCode;
    }

    /**
     * 基于运行时错误修复测试代码
     *
     * @param testCode 测试代码
     * @param runResult 运行结果
     * @param classInfo 类信息
     * @param fallbackPackage 兜底包路径
     * @return 修复后的代码
     */
    public String fixByRunError(String testCode, RunResult runResult,
                                Map<String, String> classInfo, String fallbackPackage) throws IOException {
        String className = classInfo.get("类名");
        String actualPackage = classInfo.get("包路径");
        if (actualPackage == null || actualPackage.isEmpty()) {
            actualPackage = fallbackPackage;
        }

        // 构建修复提示词
        String fixPrompt = "你是Java测试代码修复专家，请根据运行时错误修复以下测试代码。\n\n" +
                "=== 待测类信息 ===\n" +
                "包路径: " + actualPackage + "\n" +
                "类名: " + className + "\n\n" +
                "=== 运行时错误信息 ===\n" +
                "失败测试数: " + runResult.getFailedTests() + "/" + runResult.getTotalTests() + "\n" +
                "错误详情:\n" + runResult.getFailureDetails() + "\n\n" +
                "=== 待修复的测试代码 ===\n" +
                "```java\n" + testCode + "\n```\n\n" +
                "=== 修复策略 ===\n" +
                "1. 断言值错误：检查expected和actual值，修正断言\n" +
                "2. 异常类型错误：检查预期异常类型，修正assertThrows\n" +
                "3. 空指针异常：添加null检查或修正对象初始化\n" +
                "4. 数组越界：检查索引范围，修正访问逻辑\n" +
                "5. 未预期异常：添加异常处理或修正测试逻辑\n\n" +
                "=== 输出要求 ===\n" +
                "- 直接输出修复后的完整测试代码\n" +
                "- 不要输出解释、注释、思考过程\n" +
                "- 确保所有import使用正确的包路径: " + actualPackage + "\n" +
                "- 如果无法修复，返回原代码\n";

        String fixedCode = sendRequestAndGetResponse0("", fixPrompt);
        fixedCode = extractPureCode(fixedCode);

        if (fixedCode == null || fixedCode.isEmpty() || fixedCode.contains("修复失败")) {
            return testCode;
        }

        return fixedCode;
    }

   /*
    private String replaceCodeSnippet(String originalCode, String fixedSnippet, int[] lineRange) {
        String[] originalLines = originalCode.split("\\n");
        String[] fixedLines = fixedSnippet.split("\\n");

        // 构建新代码行列表
        List<String> newCodeLines = new ArrayList<>();
        int start = lineRange[0] - 1; // 转0-based
        int end = lineRange[1] - 1;

        // 1. 添加替换范围前的行
        for (int i = 0; i < start; i++) {
            newCodeLines.add(originalLines[i]);
        }

        // 2. 添加修复后的行
        newCodeLines.addAll(Arrays.asList(fixedLines));

        // 3. 添加替换范围后的行
        for (int i = end + 1; i < originalLines.length; i++) {
            newCodeLines.add(originalLines[i]);
        }

        // 拼接成完整代码
        StringBuilder newCode = new StringBuilder();
        for (String line : newCodeLines) {
            newCode.append(line).append("\n");
        }
        return newCode.toString();
    }
*/
/*
    // ========== 新增：替换修复后的代码片段 ==========
    private String replaceCodeSnippet(String originalCode, String fixedSnippet, ErrorInfo errorInfo, FixScope fixScope) {
        if (fixedSnippet.contains("【修复失败】")) {
            // 修复失败：注释/删除错误代码
            return handleFixFailure(originalCode, errorInfo, fixScope);
        }

        String[] originalLines = originalCode.split("\n");
        int errorLine = errorInfo.getErrorLine();

        switch (fixScope) {
            case SINGLE_LINE:
                // 替换错误行
                if (errorLine > 0 && errorLine <= originalLines.length) {
                    String[] fixedLines = fixedSnippet.split("\n");
                    // 取修复后的最后一行（核心错误行）替换原错误行
                    String fixedLine = fixedLines[fixedLines.length - 1].trim();
                    originalLines[errorLine - 1] = fixedLine;
                }
                break;

            case METHOD_BODY:
                // 替换方法体：先找到原方法体位置，再替换
                int methodStart = -1;
                int methodEnd = -1;
                // 定位原方法体
                for (int i = errorLine - 1; i >= 0; i--) {
                    String line = originalLines[i].trim();
                    if (line.startsWith("@Test") || (methodStart != -1 && line.matches("^[public|private|protected]?\\s*void\\s+\\w+\\(.*\\)\\s*\\{?"))) {
                        if (methodStart == -1) {
                            methodStart = i;
                        }
                        if (line.contains("{")) {
                            break;
                        }
                    }
                }
                if (methodStart != -1) {
                    int braceCount = 0;
                    for (int i = methodStart; i < originalLines.length; i++) {
                        String line = originalLines[i];
                        for (char c : line.toCharArray()) {
                            if (c == '{') braceCount++;
                            if (c == '}') braceCount--;
                        }
                        methodEnd = i;
                        if (braceCount == 0) {
                            break;
                        }
                    }
                }
                // 替换方法体
                if (methodStart != -1 && methodEnd != -1) {
                    String[] fixedLines = fixedSnippet.split("\n");
                    StringBuilder newCode = new StringBuilder();
                    // 拼接方法体之前的代码
                    for (int i = 0; i < methodStart; i++) {
                        newCode.append(originalLines[i]).append("\n");
                    }
                    // 拼接修复后的方法体
                    for (String fixedLine : fixedLines) {
                        newCode.append(fixedLine).append("\n");
                    }
                    // 拼接方法体之后的代码
                    for (int i = methodEnd + 1; i < originalLines.length; i++) {
                        newCode.append(originalLines[i]).append("\n");
                    }
                    return newCode.toString();
                }
                break;

            case FULL_CLASS:
            default:
                return fixedSnippet;
        }

        // 重组单行/方法体修复后的代码
        StringBuilder result = new StringBuilder();
        for (String line : originalLines) {
            result.append(line).append("\n");
        }
        return result.toString();
    }*/
    /**
     * 将修复后的代码片段替换回原代码的指定行范围
     *
     * @param originalCode 原始测试代码
     * @param fixedSnippet 修复后的代码片段
     * @param lineRange    原代码中需要替换的行范围 [startLine, endLine]（1-based）
     * @return 替换后的完整代码
     */
    private String replaceCodeSnippet(String originalCode, String fixedSnippet, int[] lineRange) {
        String[] originalLines = originalCode.split("\\n");
        String[] fixedLines = fixedSnippet.split("\\n");

        // 构建新代码行列表
        List<String> newCodeLines = new ArrayList<>();
        int start = lineRange[0] - 1; // 转0-based
        int end = lineRange[1] - 1;

        // 1. 添加替换范围前的行
        for (int i = 0; i < start; i++) {
            newCodeLines.add(originalLines[i]);
        }

        // 2. 添加修复后的行
        newCodeLines.addAll(Arrays.asList(fixedLines));

        // 3. 添加替换范围后的行
        for (int i = end + 1; i < originalLines.length; i++) {
            newCodeLines.add(originalLines[i]);
        }

        // 拼接成完整代码
        StringBuilder newCode = new StringBuilder();
        for (String line : newCodeLines) {
            newCode.append(line).append("\n");
        }
        return newCode.toString();
    }
    // ========== 新增：处理修复失败逻辑 ==========
    private String handleFixFailure(String originalCode, ErrorInfo errorInfo, FixScope fixScope) {
        String[] originalLines = originalCode.split("\n");
        int errorLine = errorInfo.getErrorLine();

        switch (fixScope) {
            case SINGLE_LINE:
                // 注释错误行
                if (errorLine > 0 && errorLine <= originalLines.length) {
                    originalLines[errorLine - 1] = "// 修复失败，注释错误行：" + originalLines[errorLine - 1];
                }
                break;

            case METHOD_BODY:
                // 注释整个测试方法
                int methodStart = -1;
                int methodEnd = -1;
                // 定位方法体
                for (int i = errorLine - 1; i >= 0; i--) {
                    String line = originalLines[i].trim();
                    if (line.startsWith("@Test") || (methodStart != -1 && line.matches("^[public|private|protected]?\\s*void\\s+\\w+\\(.*\\)\\s*\\{?"))) {
                        if (methodStart == -1) {
                            methodStart = i;
                        }
                        if (line.contains("{")) {
                            break;
                        }
                    }
                }
                if (methodStart != -1) {
                    int braceCount = 0;
                    for (int i = methodStart; i < originalLines.length; i++) {
                        String line = originalLines[i];
                        for (char c : line.toCharArray()) {
                            if (c == '{') braceCount++;
                            if (c == '}') braceCount--;
                        }
                        methodEnd = i;
                        if (braceCount == 0) {
                            break;
                        }
                    }
                }
                // 注释方法体
                if (methodStart != -1 && methodEnd != -1) {
                    for (int i = methodStart; i <= methodEnd; i++) {
                        originalLines[i] = "// 修复失败，注释错误方法：" + originalLines[i];
                    }
                }
                break;

            case FULL_CLASS:
                // 全类修复失败 → 返回空（或自定义处理）
                return "// 全类修复失败，建议手动处理\n";
        }

        StringBuilder result = new StringBuilder();
        for (String line : originalLines) {
            result.append(line).append("\n");
        }
        return result.toString();
    }

    /**
     * 执行分级修复策略（支持策略升级）
     *
     * @param errorInfo 错误信息
     * @param fixScope  初始修复范围
     * @return 修复后的完整测试代码
     * @throws IOException API调用异常
     */
    private String fixTestCode(ErrorInfo errorInfo, FixScope fixScope) throws IOException {
        // 1. 提取错误代码片段
        Map<String, Object> snippetMap = extractCodeSnippet(errorInfo, fixScope);
        String codeSnippet = (String) snippetMap.get("snippet");
        int[] lineRange = (int[]) snippetMap.get("lineRange");

        // 2. 构建修复提示词
        String fixScopeDesc = getFixScopeDesc(fixScope);
        String fixPrompt = fixPromptTemplate
                .replace("{ERROR_TYPE}", errorInfo.getErrorType())
                .replace("{ERROR_REASON}", errorInfo.getErrorReason())
                .replace("{FIX_SCOPE_DESC}", fixScopeDesc)
                .replace("{CODE_SNIPPET}", codeSnippet);

        // 3. 调用大模型修复代码
        String fixedSnippet = sendRequestAndGetResponse0(codeSnippet, fixPrompt);

        // 4. 提取纯代码（去除markdown标记）
        fixedSnippet = extractPureCode(fixedSnippet);

        // 5. 处理修复失败（删除/注释错误片段）
        if (fixedSnippet.contains("【修复失败】") || fixedSnippet.isEmpty()) {
            fixedSnippet = commentOrDeleteErrorSnippet(codeSnippet, fixScope);
        }

        // 6. 将修复后的片段替换回原代码
        return replaceCodeSnippet(errorInfo.getTestCode(), fixedSnippet, lineRange);
    }

    /**
     * 执行带策略升级的修复流程
     * 修复失败时自动升级策略：SINGLE_LINE → METHOD_BODY → FULL_CLASS
     *
     * 改进点：
     * 1. 使用动态二次验证（revalidateTestCode）替代标准验证
     * 2. 验证时聚焦于原错误是否修复，避免重复报告无关问题
     * 3. 只有致命错误才触发策略升级
     *
     * @param errorInfo 错误信息
     * @return 修复后的测试代码
     */
    public String fixWithStrategyUpgrade(ErrorInfo errorInfo) throws IOException {
        // 检查是否需要修复（只有致命错误才需要）
        if (!needsFix(errorInfo)) {
            System.out.println("错误级别为 " + errorInfo.getSeverity() + "，无需修复");
            return errorInfo.getTestCode();
        }

        // 初始策略判定
        FixScope currentScope = determineFixScope(errorInfo);
        String currentTestCode = errorInfo.getTestCode();
        ErrorInfo currentError = errorInfo;

        // 策略升级顺序
        FixScope[] scopeOrder = {FixScope.SINGLE_LINE, FixScope.METHOD_BODY, FixScope.FULL_CLASS};
        int startIndex = 0;
        for (int i = 0; i < scopeOrder.length; i++) {
            if (scopeOrder[i] == currentScope) {
                startIndex = i;
                break;
            }
        }

        // 从当前策略开始尝试，失败则升级
        for (int i = startIndex; i < scopeOrder.length; i++) {
            FixScope scope = scopeOrder[i];
            System.out.println("===== 尝试修复策略：" + scope + " =====");

            // 执行修复
            String fixedCode = fixTestCode(currentError, scope);

            // 使用动态二次验证（聚焦于原错误是否修复）
            ErrorInfo verifyResult = revalidateTestCode(fixedCode, currentError, scope);

            // 检查是否需要继续修复
            if (!needsFix(verifyResult)) {
                // 修复成功（无致命错误）
                System.out.println("修复成功！策略：" + scope);

             /*   // 如果有优化建议，打印出来但不阻止成功
                if (verifyResult.getSuggestions() != null && !verifyResult.getSuggestions().isEmpty()) {
                    System.out.println("优化建议：" + String.join("; ", verifyResult.getSuggestions()));
                }*/

                return fixedCode;
            } else {
                // 修复失败，检查是否需要升级策略
                System.out.println("策略 " + scope + " 修复后仍有致命错误：" + verifyResult.getErrorReason());

                if (i < scopeOrder.length - 1) {
                    System.out.println("升级到更高级策略...");
                    // 更新errorInfo为新的错误信息，继续下一轮
                    currentError = verifyResult;
                    currentError.setTestCode(fixedCode);
                    currentTestCode = fixedCode;
                } else {
                    // 已经是最高级策略，执行兜底处理
                    System.out.println("所有策略均已尝试，执行兜底处理");

                    // 兜底策略：只处理影响编译运行的错误
                    if (affectsCompilationOrRuntime(verifyResult)) {
                        return smartFallback(fixedCode, verifyResult, scope);
                    } else {
                        // 不影响编译运行的错误，保留代码
                        System.out.println("剩余错误不影响编译运行，保留当前代码");
                        return fixedCode;
                    }
                }
            }
        }

        return currentTestCode; // 兜底返回原代码
    }

    /**
     * 智能兜底策略：只注释/删除影响编译运行的错误代码
     *
     * 改进点：
     * 1. 区分致命错误和非致命错误
     * 2. 只处理影响编译运行的代码
     * 3. 保留不影响编译运行的代码（如未使用的变量）
     */
    private String smartFallback(String testCode, ErrorInfo errorInfo, FixScope fixScope) {
        // 如果错误不影响编译运行，直接返回原代码
        if (!affectsCompilationOrRuntime(errorInfo)) {
            System.out.println("兜底策略：错误不影响编译运行，保留原代码");
            return testCode;
        }

        System.out.println("兜底策略：注释/删除影响编译运行的错误代码");

        String[] lines = testCode.split("\n");
        StringBuilder newCode = new StringBuilder();
        int errorLine = errorInfo.getErrorLine() - 1; // 转为0-based索引

        switch (fixScope) {
            case SINGLE_LINE:
                // 只注释错误行
                for (int i = 0; i < lines.length; i++) {
                    if (i == errorLine) {
                        newCode.append("// [兜底] 注释错误行：").append(lines[i]).append("\n");
                    } else {
                        newCode.append(lines[i]).append("\n");
                    }
                }
                break;

            case METHOD_BODY:
                // 注释整个测试方法
                int methodStart = findMethodStartLine(errorLine + 1, lines);
                int methodEnd = findMethodEndLine(methodStart, lines);

                for (int i = 0; i < lines.length; i++) {
                    if (i >= methodStart && i <= methodEnd) {
                        newCode.append("// [兜底] ").append(lines[i]).append("\n");
                    } else {
                        newCode.append(lines[i]).append("\n");
                    }
                }
                break;

            case FULL_CLASS:
                // 保留基础结构，删除所有测试方法
                newCode.append("// [兜底] 全类修复失败，保留空类结构\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    // 保留package、import、类声明
                    if (trimmed.startsWith("package ") ||
                            trimmed.startsWith("import ") ||
                            trimmed.contains("public class ") ||
                            trimmed.equals("}")) {
                        newCode.append(line).append("\n");
                    }
                }
                // 确保类有闭合括号
                if (!newCode.toString().trim().endsWith("}")) {
                    newCode.append("}\n");
                }
                break;
        }

        return newCode.toString();
    }

    /**
     * 查找方法结束行
     */
    private int findMethodEndLine(int methodStartLine, String[] codeLines) {
        int braceCount = 0;
        boolean foundOpenBrace = false;

        for (int i = methodStartLine; i < codeLines.length; i++) {
            String line = codeLines[i];
            for (char c : line.toCharArray()) {
                if (c == '{') {
                    braceCount++;
                    foundOpenBrace = true;
                } else if (c == '}') {
                    braceCount--;
                }
            }

            if (foundOpenBrace && braceCount == 0) {
                return i;
            }
        }

        return codeLines.length - 1;
    }

    /**
     * 获取修复范围描述
     */
    private String getFixScopeDesc(FixScope fixScope) {
        switch (fixScope) {
            case SINGLE_LINE:
                return "仅修改错误单行，保留其他所有代码";
            case METHOD_BODY:
                return "仅修改错误@Test方法体，保留类中其他方法和导入";
            case FULL_CLASS:
                return "可修改整个测试类，保证编译通过";
            default:
                return "最小化修改错误部分";
        }
    }

    /**
     * 修复失败时，注释/删除错误片段
     */
    private String commentOrDeleteErrorSnippet(String codeSnippet, FixScope fixScope) {
        if (fixScope == FixScope.SINGLE_LINE) {
            // 单行：注释掉
            return "// 修复失败，注释错误行：\n//" + codeSnippet;
        } else if (fixScope == FixScope.METHOD_BODY) {
            // 方法体：注释整个方法
            return "// 修复失败，注释错误测试方法：\n/*" + codeSnippet + "*/";
        } else {
            // 全类：保留基础结构，删除错误逻辑
            // 尝试从代码片段中提取package语句
            String packageLine = extractPackageFromCode(codeSnippet);
            StringBuilder sb = new StringBuilder();
            if (packageLine != null && !packageLine.isEmpty()) {
                sb.append(packageLine).append("\n");
            }
            sb.append("import org.junit.jupiter.api.Test;\n");
            sb.append("public class ").append(className).append("Test {\n");
            sb.append("   // 全类修复失败，保留空类保证编译通过\n");
            sb.append("}");
            return sb.toString();
        }
    }

    // 兜底：注释/删除错误代码（只删除错误部分，保留正确的测试方法）
    private String commentErrorCode(String testCode, ErrorInfo errorInfo, FixScope fixScope) {
        String[] lines = testCode.split("\n");
        StringBuilder newCode = new StringBuilder();

        switch (fixScope) {
            case SINGLE_LINE:
                int errorLine = errorInfo.getErrorLine() - 1; // 数组索引
                for (int i = 0; i < lines.length; i++) {
                    if (i == errorLine) {
                        newCode.append("// [兜底] 修复失败，注释错误行：").append(lines[i]).append("\n");
                    } else {
                        newCode.append(lines[i]).append("\n");
                    }
                }
                break;
            case METHOD_BODY:
                // 只注释包含错误的测试方法，保留其他正确的方法
                int errorLineNum = errorInfo.getErrorLine();
                int methodStart = -1;
                int methodEnd = -1;

                // 找到包含错误行的测试方法
                int braceCount = 0;
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i].trim();
                    // 找到错误行所在的@Test方法
                    if (line.startsWith("@Test") || line.contains("@Test")) {
                        int testMethodStart = i;
                        braceCount = 0;
                        // 找到这个方法的结束位置
                        for (int j = i; j < lines.length; j++) {
                            String jLine = lines[j];
                            for (char c : jLine.toCharArray()) {
                                if (c == '{') braceCount++;
                                if (c == '}') braceCount--;
                            }
                            if (braceCount == 0 && j > i && lines[j].contains("}")) {
                                // 检查错误行是否在这个方法内
                                if (errorLineNum >= testMethodStart + 1 && errorLineNum <= j + 1) {
                                    methodStart = testMethodStart;
                                    methodEnd = j;
                                    break;
                                }
                                break;
                            }
                        }
                        if (methodStart != -1) break;
                    }
                }

                // 注释错误方法，保留其他方法
                for (int i = 0; i < lines.length; i++) {
                    if (methodStart != -1 && i >= methodStart && i <= methodEnd) {
                        newCode.append("// [兜底] ").append(lines[i]).append("\n");
                    } else {
                        newCode.append(lines[i]).append("\n");
                    }
                }
                break;
            case FULL_CLASS:
                // 保留类结构和正确的测试方法，只删除有明显错误的方法
                // 不要全部删除！
                String packageLine = extractPackageFromCode(testCode);
                List<String> imports = new ArrayList<>();
                List<String> correctMethods = new ArrayList<>();

                // 解析代码，提取import和测试方法
                boolean inMethod = false;
                StringBuilder currentMethod = new StringBuilder();
                int methodBraceCount = 0;

                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    String trimmed = line.trim();

                    // 收集import语句
                    if (trimmed.startsWith("import ")) {
                        imports.add(line);
                        continue;
                    }

                    // 检测测试方法开始
                    if (trimmed.startsWith("@Test") || trimmed.contains("@Test")) {
                        inMethod = true;
                        currentMethod = new StringBuilder();
                        currentMethod.append(line).append("\n");
                        methodBraceCount = 0;
                        continue;
                    }

                    // 在方法内
                    if (inMethod) {
                        currentMethod.append(line).append("\n");
                        for (char c : line.toCharArray()) {
                            if (c == '{') methodBraceCount++;
                            if (c == '}') methodBraceCount--;
                        }

                        // 方法结束
                        if (methodBraceCount == 0 && trimmed.endsWith("}")) {
                            // 简单检查：如果方法不包含明显错误标记，保留它
                            String methodCode = currentMethod.toString();
                            if (!methodCode.contains("修复失败") && !methodCode.contains("ERROR")) {
                                correctMethods.add(methodCode);
                            }
                            inMethod = false;
                        }
                    }
                }

                // 重新组装代码
                if (packageLine != null) {
                    newCode.append(packageLine).append("\n\n");
                }
                for (String imp : imports) {
                    newCode.append(imp).append("\n");
                }
                newCode.append("\npublic class ").append(className).append("Test {\n");
                newCode.append("    // [兜底] 保留了 ").append(correctMethods.size()).append(" 个正确的测试方法\n\n");
                for (String method : correctMethods) {
                    newCode.append(method).append("\n");
                }
                newCode.append("}\n");
                break;
        }

        return newCode.toString();
    }

    /**
     * 注释掉运行时出错的测试方法（兜底策略）
     * 与 commentErrorCode 不同，这个方法专门处理运行时错误，会从错误消息中提取失败的测试方法名
     *
     * @param testCode 测试代码
     * @param errorInfo 错误信息（包含运行时错误消息）
     * @return 注释掉出错方法后的代码
     */
    private String commentErrorTestMethod(String testCode, ErrorInfo errorInfo) {
        String[] lines = testCode.split("\n");
        StringBuilder newCode = new StringBuilder();

        // 从错误消息中提取失败的测试方法名
        // 错误消息格式通常是：ParserTest.testProcessArgsHandlesValuesCorrectly ? NullPointer
        String errorMsg = errorInfo.getErrorReason();
        String failedMethodName = null;

        if (errorMsg != null && errorMsg.contains(".")) {
            // 提取方法名：ParserTest.testProcessArgsHandlesValuesCorrectly -> testProcessArgsHandlesValuesCorrectly
            String[] parts = errorMsg.split("\\.");
            if (parts.length >= 2) {
                String methodPart = parts[1].split("\\s")[0]; // 去掉后面的 "? NullPointer" 等
                failedMethodName = methodPart;
            }
        }

        // 如果无法从错误消息提取方法名，使用行号定位
        if (failedMethodName == null && errorInfo.getErrorLine() > 0) {
            return commentErrorCode(testCode, errorInfo, FixScope.METHOD_BODY);
        }

        // 如果仍然无法确定，注释所有测试方法（极端情况）
        if (failedMethodName == null) {
            System.out.println("    [警告] 无法确定失败的测试方法，将注释所有测试方法");
            return commentAllTestMethods(testCode);
        }

        // 找到并注释失败的测试方法
        boolean inTargetMethod = false;
        int braceCount = 0;
        boolean foundMethod = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 检测是否是目标测试方法
            if (!inTargetMethod && (trimmed.startsWith("@Test") || trimmed.contains("@Test"))) {
                // 检查下一行或当前行是否包含方法名
                String nextLine = (i + 1 < lines.length) ? lines[i + 1] : "";
                if (line.contains(failedMethodName) || nextLine.contains(failedMethodName)) {
                    inTargetMethod = true;
                    foundMethod = true;
                    braceCount = 0;
                    newCode.append("    // [兜底] 运行时错误，注释失败的测试方法: ").append(failedMethodName).append("\n");
                    newCode.append("    // ").append(line).append("\n");
                    continue;
                }
            }

            // 在目标方法内
            if (inTargetMethod) {
                // 统计大括号
                for (char c : line.toCharArray()) {
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                }

                newCode.append("    // ").append(line).append("\n");

                // 方法结束
                if (braceCount == 0 && trimmed.endsWith("}")) {
                    inTargetMethod = false;
                }
            } else {
                newCode.append(line).append("\n");
            }
        }

        if (!foundMethod) {
            System.out.println("    [警告] 未找到失败的测试方法: " + failedMethodName + "，使用行号定位");
            return commentErrorCode(testCode, errorInfo, FixScope.METHOD_BODY);
        }

        return newCode.toString();
    }

    /**
     * 注释所有测试方法（极端兜底策略）
     */
    private String commentAllTestMethods(String testCode) {
        String[] lines = testCode.split("\n");
        StringBuilder newCode = new StringBuilder();

        boolean inTestMethod = false;
        int braceCount = 0;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            String trimmed = line.trim();

            // 检测测试方法开始
            if (!inTestMethod && (trimmed.startsWith("@Test") || trimmed.contains("@Test"))) {
                inTestMethod = true;
                braceCount = 0;
                newCode.append("    // [兜底] 注释所有测试方法\n");
                newCode.append("    // ").append(line).append("\n");
                continue;
            }

            // 在测试方法内
            if (inTestMethod) {
                for (char c : line.toCharArray()) {
                    if (c == '{') braceCount++;
                    if (c == '}') braceCount--;
                }

                newCode.append("    // ").append(line).append("\n");

                // 方法结束
                if (braceCount == 0 && trimmed.endsWith("}")) {
                    inTestMethod = false;
                }
            } else {
                newCode.append(line).append("\n");
            }
        }

        return newCode.toString();
    }

    /**
     * 从测试代码中提取package语句
     */
    private String extractPackageFromCode(String testCode) {
        if (testCode == null || testCode.isEmpty()) {
            return null;
        }
        String[] lines = testCode.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("package ") && trimmed.endsWith(";")) {
                return trimmed;
            }
        }
        return null;
    }

    /**
     * 完整的验证-修复-再验证流程
     *
     * 流程：
     * 1. 首次验证测试代码
     * 2. 判断是否需要修复（只有致命错误才需要修复）
     * 3. 如需修复，执行目标最优修复策略（支持策略自动升级）
     * 4. 使用动态二次验证确认修复结果
     * 5. 写回修复后的代码
     *
     * 改进点：
     * - 区分致命错误和优化建议
     * - 动态二次验证聚焦于原错误是否修复
     * - 兜底策略只处理影响编译运行的错误
     */
    public boolean runValiFixProcess(String testCode, String outputPath) throws IOException {
        // ===== 第一步：首次验证 =====
        System.out.println("\n========== 首次验证测试代码 ==========");
        ErrorInfo errorInfo = validateTestCode(testCode);

        // 打印验证结果
        System.out.println("验证结果：");
        System.out.println("  错误类型：" + errorInfo.getErrorType());
        System.out.println("  错误行号：" + errorInfo.getErrorLine());
        System.out.println("  错误原因：" + errorInfo.getErrorReason());
        System.out.println("  严重程度：" + errorInfo.getSeverity());

       /* // 打印优化建议（如果有）
        if (errorInfo.getSuggestions() != null && !errorInfo.getSuggestions().isEmpty()) {
            System.out.println("  优化建议：" + String.join("; ", errorInfo.getSuggestions()));
        }*/

        // ===== 第二步：判断是否需要修复 =====
        if (!needsFix(errorInfo)) {
            System.out.println("✓ 无致命错误，无需修复");
            // 即使有优化建议，也不强制修复
            return true;
        }

        System.out.println("✗ 存在致命错误，需要修复");

        // ===== 第三步：执行带策略升级的修复 =====
        System.out.println("\n========== 执行目标最优修复策略 ==========");
        errorInfo.setTestCode(testCode);
        String fixedTestCode = fixWithStrategyUpgrade(errorInfo);

        // ===== 第四步：写回并覆盖原文件 =====
        System.out.println("\n========== 写回修复后的代码 ==========");
        writeTestCodeToFile(fixedTestCode, outputPath);

        // ===== 第五步：最终验证 =====
        System.out.println("\n========== 最终验证修复结果 ==========");
        ErrorInfo finalErrorInfo = validateTestCode(fixedTestCode);

        // 最终判断：只要没有致命错误就算成功
        if (!needsFix(finalErrorInfo)) {
            System.out.println("✓ 最终验证通过，修复成功！");
           /* if (finalErrorInfo.getSuggestions() != null && !finalErrorInfo.getSuggestions().isEmpty()) {
                System.out.println("  剩余优化建议：" + String.join("; ", finalErrorInfo.getSuggestions()));
            }*/
            return true;
        } else {
            System.out.println("✗ 最终验证仍有致命错误：" + finalErrorInfo.getErrorType() + " - " + finalErrorInfo.getErrorReason());
            failedClasses.add(className);
            return false;
        }
    }

    //10.1-提取失败信息
    private String extractErrorPrompt(String validateResult) {
        String pattern = "该测试代码在验证\\[(.*?)\\]时失败，错误位置：\\[(.*?)\\]，错误原因：\\[(.*?)\\]";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(validateResult);
        if (m.find()) {
            String validationItem = m.group(1); // 验证项
            String errorLine = m.group(2);      // 错误行号
            String errorReason = m.group(3);    // 错误原因
            return "在验证 [" + validationItem + "] 时失败，错误位置：第 " + errorLine + " 行，错误原因：" + errorReason;
        } else {
            return "测试代码未通过验证，原因未知，请检查代码。";
        }
    }
    //11.提取测试代码
    private static String extractPureCode(String response) {
        if (response == null || response.isEmpty()) {
            return response;
        }

        String code = response;

        // 尝试多种代码块标记格式
        String[] startTags = {"```java", "```Java", "```JAVA", "```"};
        String endTag = "```";

        for (String startTag : startTags) {
            int start = code.indexOf(startTag);
            if (start != -1) {
                int end = code.lastIndexOf(endTag);
                if (end > start + startTag.length()) {
                    code = code.substring(start + startTag.length(), end).trim();
                    break;
                }
            }
        }

        // 检查提取的代码是否有效（必须包含package或import或class关键字）
        if (!isValidJavaCode(code)) {
            // 尝试从响应中查找有效的Java代码片段
            String extracted = extractJavaCodeFromResponse(response);
            if (extracted != null && isValidJavaCode(extracted)) {
                return extracted;
            }
            // 如果仍然无效，返回空的占位类
            System.err.println("警告：无法从LLM响应中提取有效的Java代码");
            return null;
        }

        return code;
    }

    // 检查是否为有效的Java代码
    private static boolean isValidJavaCode(String code) {
        if (code == null || code.isEmpty()) {
            return false;
        }
        // 有效的Java测试代码应该包含以下关键字之一
        return code.contains("package ") ||
                code.contains("import ") ||
                code.contains("class ") ||
                code.contains("@Test");
    }

    // 尝试从响应中提取Java代码
    private static String extractJavaCodeFromResponse(String response) {
        // 查找package声明作为代码起点
        int packageStart = response.indexOf("package ");
        if (packageStart != -1) {
            // 查找类定义的结束（最后一个}）
            int lastBrace = response.lastIndexOf("}");
            if (lastBrace > packageStart) {
                String extracted = response.substring(packageStart, lastBrace + 1).trim();
                // 验证括号是否匹配
                if (areBracesBalanced(extracted)) {
                    return extracted;
                }
            }
        }

        // 查找import声明作为代码起点
        int importStart = response.indexOf("import ");
        if (importStart != -1) {
            int lastBrace = response.lastIndexOf("}");
            if (lastBrace > importStart) {
                String extracted = response.substring(importStart, lastBrace + 1).trim();
                if (areBracesBalanced(extracted)) {
                    return extracted;
                }
            }
        }

        return null;
    }

    // 检查大括号是否匹配
    private static boolean areBracesBalanced(String code) {
        int count = 0;
        for (char c : code.toCharArray()) {
            if (c == '{') count++;
            else if (c == '}') count--;
            if (count < 0) return false;
        }
        return count == 0;
    }
    //12.2创建测试类2
    public static void createTestClass2(String passTest, Map<String, String> classInfo) {
        try {
            // 构造测试类的路径和文件名
            String packagePath = classInfo.get("包路径");
            if (packagePath == null || packagePath.isEmpty()) {
                throw new IllegalArgumentException("包路径不能为空");
            }
            // 验证包路径是否包含非法字符
            if (packagePath.matches(".*[\\[\\]{}<>].*")) {
                System.err.println("警告：包路径包含非法字符 - " + packagePath);
                packagePath = packagePath.replaceAll("[\\[\\]{}<>]", "");
                System.out.println("已清理包路径为：" + packagePath);
            }
            String basePackagePath = packagePath.replace(".", "/");
            if (!basePackagePath.startsWith("/")) {
                basePackagePath = "/" + basePackagePath;
            }

            // 处理泛型类名：移除泛型参数部分
            String className = classInfo.get("类名");
            if (className.contains("<")) {
                className = className.substring(0, className.indexOf("<")).trim();
            }

            String fileName = "src/test/java" + basePackagePath + "/" + className + "Test.java";
            // 创建目录（如果不存在）
            java.nio.file.Path path = Paths.get("src/test/java" + basePackagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
            // 将测试代码写入文件
            try (FileWriter writer = new FileWriter(fileName)) {
                String pureCode = extractPureCode(passTest);
                // 如果提取失败，生成一个空的占位测试类
                if (pureCode == null || !isValidJavaCode(pureCode)) {
                    System.err.println("警告：无法提取有效代码，生成占位测试类");
                    pureCode = "package " + packagePath + ";\n\n" +
                            "import org.junit.jupiter.api.Test;\n\n" +
                            "public class " + className + "Test {\n" +
                            "    // 代码生成失败，请手动补充测试用例\n" +
                            "}\n";
                }
                writer.write(pureCode);
            }
            System.out.println("测试类创建成功：" + fileName);

        } catch (IOException e) {
            System.err.println("创建测试类时出错：" + e.getMessage());
        }
    }
    //13.关闭客户端连接
    public void closeClient() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }
    //14.日志记录
    public void logTestCodeGenerationInfo(boolean analyzeSuccess,
                                          boolean generateSuccess,
                                          boolean validateSuccess,
                                          boolean fixSuccess,
                                          boolean evaluateSuccess,
                                          boolean overallSuccess,
                                          long analyzeStart,
                                          long analyzeEndTime,
                                          long generateEndTime,
                                          long validateEndTime,
                                          long fixEndTime,
                                          long evaluateEndTime,
                                          int analyzeTokenCount,
                                          int generateTokenCount,
                                          int validateTokenCount,
                                          int fixTokenCount,
                                          int evaluateTokenCount,
                                          String validateFailReason,
                                          int fixAttempt,
                                          String fixFailReason,
                                          String fixSuccessTimes,
                                          String evaluateCoverageData,
                                          String logFilename) {

        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;
        try (FileWriter writer = new FileWriter(logFilename, true)) {
            //1 try (FileWriter writer = new FileWriter("cli_test_errorTest_5250.txt", true)) {
            writer.write("类名：" + className + "\n");
            writer.write("分析阶段：\n");
            writer.write("  耗时：" + (analyzeEndTime - analyzeStart) + " 毫秒\n");
            writer.write("  消耗 token 数量：" + analyzeTokenCount + "\n");
            writer.write("  是否成功：" + analyzeSuccess + "\n");
            writer.write("生成阶段：\n");
            writer.write("  耗时：" + (generateEndTime - analyzeEndTime) + " 毫秒\n");
            writer.write("  消耗 token 数量：" + generateTokenCount + "\n");
            writer.write("  是否成功：" + generateSuccess + "\n");
            writer.write("验证阶段：\n");
            writer.write("  耗时：" + (validateEndTime - generateEndTime) + " 毫秒\n");
            writer.write("  消耗 token 数量：" + validateTokenCount + "\n");
            writer.write("  是否成功：" + validateSuccess + "\n");
            writer.write("  失败原因：" + validateFailReason + "\n");
            writer.write("修复阶段：\n");
            writer.write("  耗时：" + (fixEndTime - validateEndTime) + " 毫秒\n");
            writer.write("  消耗 token 数量：" + fixTokenCount + "\n");
            writer.write("  是否成功：" + fixSuccess + "\n");
            writer.write("  修复尝试次数：" + fixAttempt + "\n");
            writer.write("  修复失败原因：" + fixFailReason + "\n");
            writer.write("  修复成功次数：" + fixSuccessTimes + "\n");
            writer.write("评估阶段：\n");
            writer.write("  耗时：" + (evaluateEndTime - fixEndTime) + " 毫秒\n");
            writer.write("  消耗 token 数量：" + evaluateTokenCount + "\n");
            writer.write("  是否成功：" + evaluateSuccess + "\n");
            writer.write("  覆盖率数据：" + evaluateCoverageData + "\n");
            writer.write("总耗时：" + totalTime + " 毫秒\n");
            writer.write("总消耗 token 数量：" + tokenCount + "\n");
            writer.write("测试代码生成结果：" + overallSuccess + "\n");
            writer.write("失败原因：" + String.join(", ", failedClasses) + "\n");
            writer.write("--------------------------------------------------------\n");
        } catch (IOException e) {
            System.err.println("写入日志文件时出错：" + e.getMessage());
        }
    }

    /**
     * 智能补全缺失的import语句
     * 从编译错误中提取缺失的类，动态查找并添加import
     */
    private String autoFixMissingImports(String testCode, CompileResult compileResult) {
        if (compileResult == null || compileResult.getErrorMessage() == null) {
            return testCode;
        }

        String errorMsg = compileResult.getErrorMessage();

        // 从编译错误中提取缺失的类名
        Set<String> missingClasses = new HashSet<>();

        // 匹配模式1：找不到符号 符号: 类 ClassName
        Pattern pattern1 = Pattern.compile("找不到符号[\\s\\S]*?符号:\\s*类\\s+(\\w+)");
        Matcher matcher1 = pattern1.matcher(errorMsg);
        while (matcher1.find()) {
            missingClasses.add(matcher1.group(1));
        }

        // 匹配模式2：cannot find symbol: class ClassName
        Pattern pattern2 = Pattern.compile("cannot find symbol:\\s*class\\s+(\\w+)");
        Matcher matcher2 = pattern2.matcher(errorMsg);
        while (matcher2.find()) {
            missingClasses.add(matcher2.group(1));
        }

        // 匹配模式3：package ClassName does not exist
        Pattern pattern3 = Pattern.compile("package\\s+(\\w+)\\s+does not exist");
        Matcher matcher3 = pattern3.matcher(errorMsg);
        while (matcher3.find()) {
            missingClasses.add(matcher3.group(1));
        }

        if (missingClasses.isEmpty()) {
            return testCode;
        }

        System.out.println("    [智能补全] 检测到缺失的类: " + String.join(", ", missingClasses));

        // 动态查找这些类的完整包路径
        Map<String, String> resolvedImports = new HashMap<>();
        for (String className : missingClasses) {
            String fullPath = findClassFullPath(className);
            if (fullPath != null) {
                resolvedImports.put(className, fullPath);
            }
        }

        if (resolvedImports.isEmpty()) {
            System.out.println("    [智能补全] 未能解析任何类的包路径");
            return testCode;
        }

        // 检查已有的import
        Set<String> existingImports = new HashSet<>();
        Pattern importPattern = Pattern.compile("import\\s+([\\w.]+);");
        Matcher importMatcher = importPattern.matcher(testCode);
        while (importMatcher.find()) {
            existingImports.add(importMatcher.group(1));
        }

        // 过滤掉已存在的import
        List<String> importsToAdd = new ArrayList<>();
        for (Map.Entry<String, String> entry : resolvedImports.entrySet()) {
            if (!existingImports.contains(entry.getValue())) {
                importsToAdd.add(entry.getValue());
            }
        }

        if (importsToAdd.isEmpty()) {
            return testCode;
        }

        // 在package声明后插入import
        Pattern packagePattern = Pattern.compile("(package\\s+[\\w.]+;\\s*\\n)");
        Matcher packageMatcher = packagePattern.matcher(testCode);

        if (packageMatcher.find()) {
            StringBuilder newImports = new StringBuilder();
            for (String importStr : importsToAdd) {
                newImports.append("import ").append(importStr).append(";\n");
            }

            String result = testCode.substring(0, packageMatcher.end()) +
                    newImports.toString() +
                    testCode.substring(packageMatcher.end());

            System.out.println("    [智能补全] 成功添加 " + importsToAdd.size() + " 个import: " +
                    String.join(", ", importsToAdd));
            return result;
        }

        return testCode;
    }

    /**
     * 查找类的完整包路径
     * 使用反射和类加载器动态查找
     */
    private String findClassFullPath(String className) {
        // 常见包路径列表（按优先级）
        String[] commonPackages = {
                // JUnit 5 核心包
                "org.junit.jupiter.api",
                "org.junit.jupiter.params",
                "org.junit.jupiter.params.provider",
                // Java标准库
                "java.io",
                "java.nio.file",
                "java.net",
                "java.util",
                "java.time",
                "java.lang",
                "java.math",
                "java.text",
                "java.sql",
                "javax.swing",
                "javax.xml"
        };

        // 尝试在常见包中查找
        for (String pkg : commonPackages) {
            String fullClassName = pkg + "." + className;
            try {
                Class.forName(fullClassName);
                return fullClassName;
            } catch (ClassNotFoundException e) {
                // 继续尝试下一个包
            }
        }

        return null;
    }

    //16.获取测试路径运行测试代码
    private static String getTestFilePath(Map<String, String> classInfo) {
        // 项目根目录路径（根据实际情况调整）
        String projectRoot = System.getProperty("user.dir");

        // 构建测试文件路径
        String basePackagePath = classInfo.get("包路径").replace(".", "/");

        // 处理泛型类名：移除泛型参数部分（如 Converter<T, E extends Exception> -> Converter）
        String className = classInfo.get("类名");
        if (className.contains("<")) {
            className = className.substring(0, className.indexOf("<")).trim();
        }

        String testClassName = className + "Test";
        String testFilePath = Paths.get(projectRoot, "src", "test", "java", basePackagePath, testClassName + ".java").toString();

        return testFilePath;
    }
    /**
     * 递归扫描目录下的所有.java文件
     */
    private static void scanJavaFilesRecursively(File dir, List<File> javaFiles) {
        if (dir == null || !dir.isDirectory()) {
            return;
        }

        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                // 递归扫描子目录
                scanJavaFilesRecursively(file, javaFiles);
            } else if (file.isFile() && file.getName().endsWith(".java")) {
                // 添加.java文件
                javaFiles.add(file);
            }
        }
    }

    /**
     * 从源代码目录路径自动推导包路径
     * 例如：D:\project\src\main\java\cli -> cli
     * 例如：D:\project\src\main\java\cli\help -> cli.help
     * 例如：D:\project\src\main\java\csv -> csv
     *
     * @param sourceCodeDir 源代码目录完整路径
     * @return 包路径（如 cli, cli.help, csv）
     */
    private static String derivePackageFromPath(String sourceCodeDir) {
        // 标准化路径分隔符
        String normalizedPath = sourceCodeDir.replace("\\", "/");

        // 查找 src/main/java/ 的位置
        int javaIndex = normalizedPath.indexOf("src/main/java/");
        if (javaIndex == -1) {
            // 如果找不到标准路径，尝试其他常见模式
            javaIndex = normalizedPath.indexOf("src/java/");
            if (javaIndex != -1) {
                String packagePath = normalizedPath.substring(javaIndex + "src/java/".length());
                return packagePath.replace("/", ".");
            }

            // 兜底：使用最后一个目录名作为包名
            String[] parts = normalizedPath.split("/");
            return parts[parts.length - 1];
        }

        // 提取 src/main/java/ 之后的路径部分
        String packagePath = normalizedPath.substring(javaIndex + "src/main/java/".length());

        // 将路径分隔符替换为点号
        return packagePath.replace("/", ".");
    }

    //16.读取待测试类-待实现
    private static String extractClassName(File javaFile) {
        try {
            List<String> lines = Files.readAllLines(javaFile.toPath());
            for (String line : lines) {
                String trimmed = line.trim();
                // 支持 class、interface、enum，以及各种修饰符组合（final、abstract等）
                // 匹配模式：public [final|abstract] [class|interface|enum] ClassName
                if (trimmed.startsWith("public ") &&
                        (trimmed.contains(" class ") || trimmed.contains(" interface ") || trimmed.contains(" enum "))) {
                    // 移除所有修饰符，统一提取类名
                    String normalized = trimmed
                            .replace("public ", "")
                            .replace("final ", "")
                            .replace("abstract ", "")
                            .replace("interface ", "class ")
                            .replace("enum ", "class ")
                            .trim();
                    if (normalized.startsWith("class ")) {
                        String className = normalized.substring(6).split("[\\s\\{<]")[0].trim();
                        if (!className.isEmpty()) {
                            return className;
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + javaFile.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }
    //18.覆盖率
    public String executeMavenCommand(String command) throws IOException {
        // 使用你的Maven安装路径
        String mavenPath = "D:\\TopNew-SpringBoot\\maven\\apache-maven-3.9.1-bin\\apache-maven-3.9.1\\bin\\mvn.cmd";

        // 使用ProcessBuilder替代Runtime.exec
        List<String> commandList = new ArrayList<>();
        commandList.add(mavenPath);

        // 将命令参数拆分为单独的参数
        String[] commandParts = command.split(" ");
        Collections.addAll(commandList, commandParts);

        System.out.println("执行Maven命令: " + String.join(" ", commandList));

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.redirectErrorStream(true); // 合并标准输出和错误输出

        Process process = processBuilder.start();

        // 读取输出
        // Windows系统命令输出使用GBK编码
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), System.getProperty("os.name").toLowerCase().contains("win") ? java.nio.charset.Charset.forName("GBK") : StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append("\n");
        }

        // 等待进程完成并获取返回码
        try {
            int exitCode = process.waitFor();
            System.out.println("Maven命令返回码: " + exitCode);

            if (exitCode != 0) {
                System.err.println("Maven命令执行失败，返回码: " + exitCode);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Maven命令执行被中断: " + e.getMessage());
        }

        return output.toString();
    }
    /**
     * 提取覆盖率数据从 mvn test 结果
     * @param mvnTestResult mvn test 结果
     * @param coverageType 覆盖率类型（例如："Overall Coverage"、"Branch Coverage"、"Line Coverage"）
     * @return 覆盖率数值（百分比）
     */
    private static double extractCoverageFromMvnResult(String mvnTestResult, String coverageType) {
        String pattern = coverageType + ": (\\d+\\.\\d+)%";
        Pattern r = Pattern.compile(pattern);
        Matcher m = r.matcher(mvnTestResult);
        if (m.find()) {
            return Double.parseDouble(m.group(1));
        }
        return 0.0;
    }
    private static List<Map<String, String>> extractCoverageInfo() {
        // 调用 CoverageEvaluator 类中的方法提取覆盖率信息
        CoverageEvaluator coverageEvaluator=new CoverageEvaluator();

        return coverageEvaluator.extractCoverageInfo("target/site/jacoco/index.html");
        //  return CoverageEvaluator.extractCoverageInfo("target/site/jacoco/index.html");
    }

    private static boolean checkCoverage(List<Map<String, String>> coverageInfoList) {
        // 定义覆盖率阈值
        final double INSTRUCTION_THRESHOLD = 80.0;
        final double BRANCH_THRESHOLD = 80.0;
        final double LINE_THRESHOLD = 80.0;
        final double METHOD_THRESHOLD = 80.0;
        final double CLASS_THRESHOLD = 80.0;

        for (Map<String, String> coverageInfo : coverageInfoList) {
            // 解析覆盖率数据
            double instructionCoverage = parseCoverageToDouble(coverageInfo.get("Instruction Coverage"));
            double branchCoverage = parseCoverageToDouble(coverageInfo.get("Branch Coverage"));
            double lineCoverage = parseCoverageToDouble(coverageInfo.get("Line Coverage"));
            double methodCoverage = parseCoverageToDouble(coverageInfo.get("Method Coverage"));
            double classCoverage = parseCoverageToDouble(coverageInfo.get("Class Coverage"));

            // 检查覆盖率是否满足要求
            if (instructionCoverage < INSTRUCTION_THRESHOLD ||
                    branchCoverage < BRANCH_THRESHOLD ||
                    lineCoverage < LINE_THRESHOLD ||
                    methodCoverage < METHOD_THRESHOLD ||
                    classCoverage < CLASS_THRESHOLD) {
                // 如果某个指标未达到要求，可以在这里记录或处理
                System.out.println("Coverage criteria not met for element: " + coverageInfo.get("Element"));
                System.out.println("  Instruction Coverage: " + instructionCoverage + "%");
                System.out.println("  Branch Coverage: " + branchCoverage + "%");
                System.out.println("  Line Coverage: " + lineCoverage + "%");
                System.out.println("  Method Coverage: " + methodCoverage + "%");
                System.out.println("  Class Coverage: " + classCoverage + "%");
                return false;
            }
        }

        return true;
    }

    private static double parseCoverageToDouble(String coverageText) {
        if (coverageText.equals("-")) {
            return 0.0;
        }
        try {
            return Double.parseDouble(coverageText.replace("%", ""));
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    /**
     * 计算总体覆盖率（合并所有子包的覆盖率数据）
     * 使用原始数据（missed + total）进行加权计算，而不是简单平均百分比
     *
     * @param coverageInfoList 包含多个子包的覆盖率数据
     * @return 合并后的总体覆盖率
     */
    private static Map<String, String> calculateTotalCoverage(List<Map<String, String>> coverageInfoList) {
        Map<String, String> totalCoverage = new HashMap<>();

        if (coverageInfoList.isEmpty()) {
            totalCoverage.put("Instruction Coverage", "-");
            totalCoverage.put("Branch Coverage", "-");
            totalCoverage.put("Line Coverage", "-");
            totalCoverage.put("Method Coverage", "-");
            totalCoverage.put("Class Coverage", "-");
            return totalCoverage;
        }

        // 累加原始数据（missed 和 total）
        int totalInstructionMissed = 0, totalInstructionTotal = 0;
        int totalBranchMissed = 0, totalBranchTotal = 0;
        int totalLineMissed = 0, totalLineTotal = 0;
        int totalMethodMissed = 0, totalMethodTotal = 0;
        int totalClassMissed = 0, totalClassTotal = 0;

        for (Map<String, String> coverageInfo : coverageInfoList) {
            // 指令覆盖率：使用 missed 和 total
            totalInstructionMissed += parseIntValue(coverageInfo.get("Instruction Missed"));
            totalInstructionTotal += parseIntValue(coverageInfo.get("Instruction Total"));

            // 分支覆盖率：使用 missed 和 total
            totalBranchMissed += parseIntValue(coverageInfo.get("Branch Missed"));
            totalBranchTotal += parseIntValue(coverageInfo.get("Branch Total"));

            // 行覆盖率：有 missed 和 total
            totalLineMissed += parseIntValue(coverageInfo.get("Line Missed"));
            totalLineTotal += parseIntValue(coverageInfo.get("Line Total"));

            // 方法覆盖率：有 missed 和 total
            totalMethodMissed += parseIntValue(coverageInfo.get("Method Missed"));
            totalMethodTotal += parseIntValue(coverageInfo.get("Method Total"));

            // 类覆盖率：有 missed 和 total
            totalClassMissed += parseIntValue(coverageInfo.get("Class Missed"));
            totalClassTotal += parseIntValue(coverageInfo.get("Class Total"));
        }

        // 计算总体覆盖率
        totalCoverage.put("Instruction Coverage", calculateCoveragePercentage(totalInstructionMissed, totalInstructionTotal));
        totalCoverage.put("Branch Coverage", calculateCoveragePercentage(totalBranchMissed, totalBranchTotal));
        totalCoverage.put("Line Coverage", calculateCoveragePercentage(totalLineMissed, totalLineTotal));
        totalCoverage.put("Method Coverage", calculateCoveragePercentage(totalMethodMissed, totalMethodTotal));
        totalCoverage.put("Class Coverage", calculateCoveragePercentage(totalClassMissed, totalClassTotal));

        return totalCoverage;
    }

    /**
     * 解析整数值（处理逗号分隔符）
     */
    private static int parseIntValue(String value) {
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return 0;
        }
        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 从 covered 和 missed 计算覆盖率百分比
     */
    private static String calculateCoverageFromCovered(int covered, int missed) {
        int total = covered + missed;
        if (total == 0) {
            return "0.0%";
        }
        double coverage = (double) covered / total * 100;
        return String.format("%.1f%%", coverage);
    }

    /**
     * 从 missed 和 total 计算覆盖率百分比
     */
    private static String calculateCoveragePercentage(int missed, int total) {
        if (total == 0) {
            return "0.0%";
        }
        double coverage = (double) (total - missed) / total * 100;
        return String.format("%.1f%%", coverage);
    }


    // 20.添加 Timer 实例
    private final Timer timer = new Timer();



    public static void main(String[] args) throws IOException {
        String apiKey = "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";

        // ========== 唯一需要配置的地方：源代码目录 ==========
        String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\lang3";
        // 如果要测试其他包，只需修改这一行，例如：
        // String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\csv";
        // String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\help";

        String logFilename = "BatchTest_" + new java.text.SimpleDateFormat("MMdd_HHmm").format(new java.util.Date()) + ".txt";

        // 使用批量处理方法，逐个处理指定目录下所有待测试类
        batchProcessAllClasses(apiKey, sourceCodeDir, logFilename);
    }

    /**
     * 批量处理指定目录下所有待测试类
     *
     * 流程（调整后）：
     * 1. 扫描目录下所有.java文件（支持递归子目录）
     * 2. 【阶段1】先对所有待测类进行分析（全部分析完毕）
     * 3. 【阶段2】再对所有待测类进行生成 → 验证 → 修复 → 创建测试类
     * 4. 【阶段3】统一编译所有测试类
     * 5. 【阶段4】运行所有测试并计算覆盖率
     *
     * @param apiKey API密钥
     * @param sourceCodeDir 源代码目录（完整路径，如 D:\project\src\main\java\cli）
     * @param logFilename 日志文件名
     */
    public static void batchProcessAllClasses(String apiKey, String sourceCodeDir, String logFilename) throws IOException {
        System.out.println("\n================================================================================");
        System.out.println("                    批量测试代码生成流程开始");
        System.out.println("================================================================================");
        System.out.println("【配置信息】");
        System.out.println("  源代码目录: " + sourceCodeDir);

        // ========== 自动推导基础包路径（从sourceCodeDir推导） ==========
        // 例如：D:\project\src\main\java\cli -> cli
        // 例如：D:\project\src\main\java\cli\help -> cli.help
        String basePackage = derivePackageFromPath(sourceCodeDir);
        System.out.println("  自动推导基础包: " + basePackage);
        System.out.println("  日志文件: " + logFilename);
        System.out.println("--------------------------------------------------------------------------------");

        // 1. 递归扫描包及其子包下所有.java文件
        File dir = new File(sourceCodeDir);
        if (!dir.exists() || !dir.isDirectory()) {
            System.err.println("错误：源代码目录不存在或不是目录: " + sourceCodeDir);
            return;
        }

        // 递归扫描所有.java文件
        List<File> javaFiles = new ArrayList<>();
        scanJavaFilesRecursively(dir, javaFiles);
        // ========== 新增：定义需要排除的类列表 ==========
        Set<String> excludeClasses = new HashSet<>(Arrays.asList(
                "Uncheck",
                "IOStream",
                "Base64OutputStream",
                "AppendableOutputStream",
                "IOUtilsHelper",
                "AbstractStreamBuilder",
                "Charsets",
                "UnsynchronizedBufferedReader"
        ));

// ========== 新增：过滤掉排除列表中的类 ==========
        List<File> filteredJavaFiles = new ArrayList<>();
        int excludedCount = 0;
        for (File file : javaFiles) {
            String className = extractClassName(file);
            if (className != null && !excludeClasses.contains(className)) {
                filteredJavaFiles.add(file);
            } else if (className != null) {
                System.out.println("  [跳过] " + className + " (工具类，不生成测试)");
                excludedCount++;
            }
        }

// 使用过滤后的文件列表
        javaFiles = filteredJavaFiles;

        System.out.println("\n【过滤结果】共扫描 " + (javaFiles.size() + excludedCount) + " 个文件，排除 " + excludedCount + " 个工具类，剩余 " + javaFiles.size() + " 个待测试类");

        if (javaFiles.isEmpty()) {
            System.err.println("错误：目录下没有找到.java文件: " + sourceCodeDir);
            return;
        }

        System.out.println("\n【扫描结果】找到 " + javaFiles.size() + " 个待测试类:");
        List<String> classNames = new ArrayList<>();
        // 新增：保存文件路径映射 className -> File
        Map<String, File> classFileMap = new HashMap<>();
        List<Map<String, String>> allClassInfoList = new ArrayList<>();
        List<String> successClasses = new ArrayList<>();
        List<String> failedClasses = new ArrayList<>();

        for (File file : javaFiles) {
            String className = extractClassName(file);
            if (className != null && !className.isEmpty()) {
                classNames.add(className);
                classFileMap.put(className, file);

                // 显示类名和相对路径
                String relativePath = file.getAbsolutePath().replace(new File(sourceCodeDir).getAbsolutePath(), "");
                System.out.println("  - " + className + " (" + relativePath + ")");
            }
        }
        System.out.println("--------------------------------------------------------------------------------");

        ValiFixTextCoreDemo5 testCoreClass = new ValiFixTextCoreDemo5(apiKey);
        testCoreClass.timer.start("批量处理");

        // ==================== 阶段1：先对所有待测类进行分析 ====================
        System.out.println("\n================================================================================");
        System.out.println("【阶段1】批量分析所有待测类（先全部分析完毕）");
        System.out.println("================================================================================");

        // 存储所有类的分析结果：className -> analyzeResult
        Map<String, String> allAnalyzeResults = new HashMap<>();
        // 存储所有类的源代码：className -> sourceCode
        Map<String, String> allSourceCodes = new HashMap<>();
        // 存储所有类的classInfo：className -> classInfo
        Map<String, Map<String, String>> allClassInfoMap = new HashMap<>();
        // 分析成功的类
        List<String> analyzeSuccessClasses = new ArrayList<>();
        // 分析失败的类
        List<String> analyzeFailedClasses = new ArrayList<>();

        for (int i = 0; i < classNames.size(); i++) {
            String className = classNames.get(i);

            // 使用保存的文件路径，而不是硬编码构建
            File sourceFile = classFileMap.get(className);
            if (sourceFile == null) {
                System.err.println("    ✗ 未找到类文件: " + className);
                analyzeFailedClasses.add(className);
                continue;
            }

            String sourceCodePath = sourceFile.getAbsolutePath();

            System.out.println("\n  [分析进度] " + (i + 1) + "/" + classNames.size() + " - 分析: " + className);

            testCoreClass.className = className;

            try {
                // 读取源代码
                String sourceCodeFile = testCoreClass.readSourceCode(sourceCodePath);
                if (sourceCodeFile == null || sourceCodeFile.isEmpty()) {
                    System.err.println("    ✗ 无法读取源代码文件: " + sourceCodePath);
                    analyzeFailedClasses.add(className);
                    continue;
                }
                allSourceCodes.put(className, sourceCodeFile);

                // 检查分析结果缓存
                String analysisResultFilePath = "src/A/" + className + "Analysis.txt";
                File analysisResultFile = new File(analysisResultFilePath);

                String analyzePromt;
                if (analysisResultFile.exists()) {
                    System.out.println("    [缓存] 从缓存加载分析结果");
                    analyzePromt = testCoreClass.readSourceCode(analysisResultFilePath);
                } else {
                    System.out.println("    [LLM] 调用LLM分析...");
                    analyzePromt = testCoreClass.analyzeSourceCode(sourceCodeFile);
                    if (analyzePromt != null && !analyzePromt.isEmpty()) {
                        // 确保目录存在并保存分析结果
                        new File("src/A").mkdirs();
                        try (FileWriter writer = new FileWriter(analysisResultFilePath)) {
                            writer.write(analyzePromt);
                        }
                        System.out.println("    分析结果已保存到: " + analysisResultFilePath);
                    }
                }

                if (analyzePromt == null || analyzePromt.isEmpty()) {
                    System.err.println("    ✗ 分析失败：LLM未返回有效结果");
                    analyzeFailedClasses.add(className);
                    continue;
                }

                // 提取类信息
                Map<String, String> classInfo = testCoreClass.extractClassInfo3(analyzePromt);
                if (classInfo == null || classInfo.isEmpty()) {
                    System.err.println("    ✗ 分析失败：无法提取类信息");
                    analyzeFailedClasses.add(className);
                    continue;
                }

                // 保存分析结果
                allAnalyzeResults.put(className, analyzePromt);
                allClassInfoMap.put(className, classInfo);
                analyzeSuccessClasses.add(className);
                System.out.println("    ✓ 分析成功");

            } catch (Exception e) {
                analyzeFailedClasses.add(className);
                System.err.println("    ✗ 分析异常: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 打印分析阶段汇总
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("【阶段1完成】分析汇总");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("  分析成功: " + analyzeSuccessClasses.size() + " 个 - " + String.join(", ", analyzeSuccessClasses));
        System.out.println("  分析失败: " + analyzeFailedClasses.size() + " 个 - " + (analyzeFailedClasses.isEmpty() ? "无" : String.join(", ", analyzeFailedClasses)));
        System.out.println("--------------------------------------------------------------------------------");

        if (analyzeSuccessClasses.isEmpty()) {
            System.out.println("\n没有分析成功的类，跳过后续阶段");
            testCoreClass.timer.end("批量处理");
            testCoreClass.closeClient();
            return;
        }

        // ==================== 阶段2：对所有分析成功的类进行生成-验证-修复 ====================
        System.out.println("\n================================================================================");
        System.out.println("【阶段2】批量生成-验证-修复测试类（基于已完成的分析结果）");
        System.out.println("================================================================================");

        for (int i = 0; i < analyzeSuccessClasses.size(); i++) {
            String className = analyzeSuccessClasses.get(i);

            // 处理泛型类名：移除泛型参数部分用于文件路径
            String classNameForPath = className;
            if (classNameForPath.contains("<")) {
                classNameForPath = classNameForPath.substring(0, classNameForPath.indexOf("<")).trim();
            }

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("【生成进度】" + (i + 1) + "/" + analyzeSuccessClasses.size() + " - 正在处理: " + className);
            System.out.println("--------------------------------------------------------------------------------");

            testCoreClass.className = className;

            try {
                // 获取已分析的结果
                String analyzePromt = allAnalyzeResults.get(className);
                String sourceCodeFile = allSourceCodes.get(className);
                Map<String, String> classInfo = allClassInfoMap.get(className);

                // 使用classInfo中的实际包路径构建源代码路径
                String actualPackage = classInfo.get("包路径");
                if (actualPackage == null || actualPackage.isEmpty()) {
                    actualPackage = basePackage; // 兜底使用推导的基础包路径
                }
                String sourceCodePath = "src/main/java/" + actualPackage.replace(".", "/") + "/" + classNameForPath + ".java";

                // 调用生成-验证-修复流程（使用已有的分析结果）
                Map<String, String> resultClassInfo = processGenerateValidateFix(
                        testCoreClass, sourceCodeFile, analyzePromt, classInfo, basePackage, logFilename);

                if (resultClassInfo != null && !resultClassInfo.isEmpty()) {
                    allClassInfoList.add(resultClassInfo);
                    successClasses.add(className);
                    System.out.println("  ✓ " + className + " 测试类生成成功");
                } else {
                    failedClasses.add(className);
                    System.out.println("  ✗ " + className + " 测试类生成失败");
                }
            } catch (Exception e) {
                failedClasses.add(className);
                System.err.println("  ✗ " + className + " 处理异常: " + e.getMessage());
                e.printStackTrace();
            }
        }

        // 将分析失败的类也加入失败列表
        failedClasses.addAll(analyzeFailedClasses);

        // 打印生成阶段汇总
        System.out.println("\n================================================================================");
        System.out.println("【阶段2完成】测试类生成汇总");
        System.out.println("================================================================================");
        System.out.println("  成功: " + successClasses.size() + " 个 - " + String.join(", ", successClasses));
        System.out.println("  失败: " + failedClasses.size() + " 个 - " + (failedClasses.isEmpty() ? "无" : String.join(", ", failedClasses)));
        System.out.println("--------------------------------------------------------------------------------");

        // 4. 所有测试类生成完成后，统一编译
        if (successClasses.isEmpty()) {
            System.out.println("\n没有成功生成的测试类，跳过编译和运行阶段");
            testCoreClass.timer.end("批量处理");
            testCoreClass.closeClient();
            return;
        }

        System.out.println("\n================================================================================");
        System.out.println("【阶段2】统一编译所有测试类");
        System.out.println("================================================================================");

        TestClassCompiler testClassCompiler = new TestClassCompiler();
        List<String> compiledClasses = new ArrayList<>();
        List<String> compileFailedClasses = new ArrayList<>();

        for (Map<String, String> classInfo : allClassInfoList) {
            // 处理泛型类名：移除泛型参数部分
            String className = classInfo.get("类名");
            if (className.contains("<")) {
                className = className.substring(0, className.indexOf("<")).trim();
            }
            System.out.println("  编译: " + className + "Test...");

            try {
                boolean compileResult = testClassCompiler.compileTestClass(classInfo);
                if (compileResult) {
                    compiledClasses.add(className);
                    System.out.println("    ✓ 编译成功");
                } else {
                    compileFailedClasses.add(className);
                    System.out.println("    ✗ 编译失败");
                }
            } catch (Exception e) {
                compileFailedClasses.add(className);
                System.out.println("    ✗ 编译异常: " + e.getMessage());
            }
        }

        System.out.println("\n【编译汇总】");
        System.out.println("  成功: " + compiledClasses.size() + " 个 - " + String.join(", ", compiledClasses));
        System.out.println("  失败: " + compileFailedClasses.size() + " 个 - " + (compileFailedClasses.isEmpty() ? "无" : String.join(", ", compileFailedClasses)));

        // 5. 统一运行测试
        if (compiledClasses.isEmpty()) {
            System.out.println("\n没有编译成功的测试类，跳过运行阶段");
            testCoreClass.timer.end("批量处理");
            testCoreClass.closeClient();
            return;
        }

        System.out.println("\n================================================================================");
        System.out.println("【阶段3】统一运行测试并计算覆盖率");
        System.out.println("================================================================================");

        // 先清理旧的覆盖率报告，确保结果是最新的
        System.out.println("  清理旧的覆盖率报告...");
        try {
            testCoreClass.executeMavenCommand("clean");
            System.out.println("  ✓ 清理完成");
        } catch (Exception e) {
            System.out.println("  清理失败（继续执行）: " + e.getMessage());
        }

        // 运行mvn test（会运行所有测试类）
        System.out.println("  执行 mvn test...");
        String mvnTestResult = testCoreClass.executeMavenCommand("test");
        System.out.println("  mvn test 执行完成");

        // 打印编译失败的类（这些类没有测试覆盖）
        if (!compileFailedClasses.isEmpty()) {
            System.out.println("\n【警告】以下类的测试编译失败，未被测试覆盖:");
            for (String failedClass : compileFailedClasses) {
                System.out.println("  ✗ " + failedClass);
            }
        }

        // 提取并显示覆盖率
        System.out.println("\n【覆盖率结果】");
        List<Map<String, String>> coverageInfoList = extractCoverageInfo();
        List<Map<String, String>> filteredCoverageInfoList = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, basePackage);

        if (filteredCoverageInfoList != null && !filteredCoverageInfoList.isEmpty()) {
            // 打印包级别覆盖率（分开显示各个子包）
            for (Map<String, String> coverageInfo : filteredCoverageInfoList) {
                String element = coverageInfo.get("Element");
                System.out.println("  " + element + ":");
                System.out.println("    指令覆盖率: " + coverageInfo.get("Instruction Coverage"));
                System.out.println("    分支覆盖率: " + coverageInfo.get("Branch Coverage"));
                System.out.println("    行覆盖率: " + coverageInfo.get("Line Coverage"));
                System.out.println("    方法覆盖率: " + coverageInfo.get("Method Coverage"));
                System.out.println("    类覆盖率: " + coverageInfo.get("Class Coverage"));
            }

            // 计算并显示总体覆盖率（合并所有子包）
            if (filteredCoverageInfoList.size() > 1) {
                System.out.println("\n  " + basePackage + " (总体，包含所有子包):");
                Map<String, String> totalCoverage = calculateTotalCoverage(filteredCoverageInfoList);
                System.out.println("    指令覆盖率: " + totalCoverage.get("Instruction Coverage"));
                System.out.println("    分支覆盖率: " + totalCoverage.get("Branch Coverage"));
                System.out.println("    行覆盖率: " + totalCoverage.get("Line Coverage"));
                System.out.println("    方法覆盖率: " + totalCoverage.get("Method Coverage"));
                System.out.println("    类覆盖率: " + totalCoverage.get("Class Coverage"));
            }

            // 打印每个类的测试状态
            System.out.println("\n【各类测试状态】");
            for (String className : classNames) {
                boolean generated = successClasses.contains(className);
                boolean compiled = compiledClasses.contains(className);
                String status;
                if (compiled) {
                    status = "✓ 已测试";
                } else if (generated) {
                    status = "✗ 编译失败";
                } else {
                    status = "✗ 生成失败";
                }
                System.out.println("  " + className + ": " + status);
            }
        } else {
            System.out.println("  未能获取覆盖率信息，请检查jacoco配置");
        }

        // 计算实际覆盖的类比例
        double actualClassCoverage = (double) compiledClasses.size() / classNames.size() * 100;
        System.out.println("\n【实际测试覆盖】");
        System.out.println("  测试类覆盖率: " + String.format("%.1f", actualClassCoverage) + "% (" + compiledClasses.size() + "/" + classNames.size() + ")");

        boolean coverageSufficient = checkCoverage(filteredCoverageInfoList);
        // 如果有编译失败的类，覆盖率检查应该标记为不完整
        if (!compileFailedClasses.isEmpty()) {
            System.out.println("\n【覆盖率检查】⚠ 部分达标（有 " + compileFailedClasses.size() + " 个类未被测试）");
        } else {
            System.out.println("\n【覆盖率检查】" + (coverageSufficient ? "✓ 达标" : "✗ 未达标"));
        }

        // 6. 最终汇总
        System.out.println("\n================================================================================");
        System.out.println("                    批量处理完成汇总");
        System.out.println("================================================================================");
        System.out.println("  待测试类总数: " + classNames.size());
        System.out.println("  测试类生成成功: " + successClasses.size());
        System.out.println("  测试类编译成功: " + compiledClasses.size());
        System.out.println("  测试类编译失败: " + compileFailedClasses.size() + (compileFailedClasses.isEmpty() ? "" : " - " + String.join(", ", compileFailedClasses)));
        System.out.println("  JaCoCo覆盖率达标: " + coverageSufficient);
        System.out.println("  完整测试覆盖: " + (compileFailedClasses.isEmpty() && coverageSufficient));
        System.out.println("================================================================================");

        testCoreClass.timer.end("批量处理");
        testCoreClass.closeClient();

        // 写入批量处理日志
        try (FileWriter writer = new FileWriter(logFilename, true)) {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            writer.write("\n========== 批量处理日志 [" + timestamp + "] ==========\n");
            writer.write("待测试类总数: " + classNames.size() + "\n");
            writer.write("测试类生成成功: " + successClasses.size() + " - " + String.join(", ", successClasses) + "\n");
            writer.write("测试类生成失败: " + failedClasses.size() + " - " + (failedClasses.isEmpty() ? "无" : String.join(", ", failedClasses)) + "\n");
            writer.write("测试类编译成功: " + compiledClasses.size() + " - " + String.join(", ", compiledClasses) + "\n");
            writer.write("测试类编译失败: " + compileFailedClasses.size() + " - " + (compileFailedClasses.isEmpty() ? "无" : String.join(", ", compileFailedClasses)) + "\n");
            writer.write("JaCoCo覆盖率达标: " + coverageSufficient + "\n");
            writer.write("完整测试覆盖: " + (compileFailedClasses.isEmpty() && coverageSufficient) + "\n");
            writer.write("========================================\n");
        } catch (IOException e) {
            System.err.println("写入批量处理日志时出错: " + e.getMessage());
        }
    }

    /**
     * 处理单个待测试类（分析 → 生成 → 验证 → 修复 → 创建测试类）
     * 不执行编译和运行，只生成测试类文件
     * @param fallbackPackage 兜底包路径（当classInfo中包路径为空时使用）
     * @return 类信息Map（包含包路径、类名等），失败返回null
     */
    public static Map<String, String> processGenerateValidateFix(ValiFixTextCoreDemo5 testCoreClass,
                                                                 String sourceCodeFile, String analyzePromt,
                                                                 Map<String, String> classInfo, String fallbackPackage,
                                                                 String logFilename) throws IOException {
        String className = testCoreClass.className;
        System.out.println("\n【处理类】" + className + "（生成-验证-修复阶段）");

        // ===== 检查测试类是否已存在，存在则跳过 =====
        String testFilePath = getTestFilePath(classInfo);
        File testFile = new File(testFilePath);
        if (testFile.exists() && testFile.length() > 0) {
            System.out.println("    ✓ 测试类已存在，跳过生成-验证-修复阶段: " + testFilePath);
            return classInfo;
        }

        String conversationHistory = "";

        // ===== 阶段1：生成测试代码（分析已在前置阶段完成） =====
        System.out.println("  [1/3] 生成测试代码...");
        String testCode = null;

        System.out.println("    调用LLM生成...");
        testCode = testCoreClass.generateTestCode2(sourceCodeFile, analyzePromt, conversationHistory);

        if (testCode == null || testCode.isEmpty()) {
            System.err.println("    生成失败：LLM未返回有效结果");
            return null;
        }
        System.out.println("    ✓ 生成完成");

        // ===== 阶段2：动态验证 =====
        System.out.println("  [2/3] 验证测试代码...");
        AnalysisInfo analysisInfo = testCoreClass.parseAnalysisResult(analyzePromt);
        ExtendedValidationResult extendedResult = testCoreClass.validateTestCodeWithAnalysis(testCode, analysisInfo);
        ErrorInfo errorInfo = extendedResult.getErrorInfo();

        // 严格判断验证是否通过：必须无致命错误
        boolean validateSuccess = false;
        // 只有当严重程度不是FATAL时，才可能验证通过
        if (errorInfo.getSeverity() != ErrorSeverity.FATAL) {
            // 无致命错误，检查方法覆盖
            if (extendedResult.isAllMethodsCovered() ||
                    (extendedResult.getUncoveredMethods() == null || extendedResult.getUncoveredMethods().isEmpty())) {
                validateSuccess = true;
            }
        }

        // ===== 保存验证结果到文件 =====
        String validateResultFilePath = "src/V/" + className + "ValidateResult.txt";
        new File("src/V").mkdirs();
        String validateResultText = "错误类型: " + errorInfo.getErrorType() +
                ", 错误行: " + errorInfo.getErrorLine() +
                ", 错误原因: " + errorInfo.getErrorReason() +
                ", 严重程度: " + errorInfo.getSeverity() +
                "\n方法覆盖情况: " +
                "\n  已覆盖: " + (extendedResult.getCoveredMethods() != null ? String.join(", ", extendedResult.getCoveredMethods()) : "无") +
                "\n  未覆盖: " + (extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty() ? String.join(", ", extendedResult.getUncoveredMethods()) : "全部覆盖") ;
        //  "\n优化建议: " + (extendedResult.getOptimizeSuggestions() != null && !extendedResult.getOptimizeSuggestions().isEmpty() ? String.join("; ", extendedResult.getOptimizeSuggestions()) : "无");
        try (FileWriter writer = new FileWriter(validateResultFilePath, true)) {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            writer.write("[" + timestamp + "] 验证结果\n" + validateResultText + "\n\n");
            System.out.println("    验证结果已保存到: " + validateResultFilePath);
        } catch (IOException e) {
            System.err.println("    保存验证结果时出错：" + e.getMessage());
        }

        if (validateSuccess) {
            System.out.println("    ✓ LLM验证通过，进行真实编译验证...");

            // ===== 真实编译验证 =====
            CompileResult compileResult = testCoreClass.realCompileValidation(testCode, classInfo);

            if (compileResult.isSuccess()) {
                System.out.println("    ✓ 真实编译验证通过");
                createTestClass2(testCode, classInfo);
                return classInfo;
            } else {
                System.out.println("    ✗ 真实编译失败，进行编译错误修复...");
                // 基于真实编译错误进行修复，最多尝试3次
                String currentCode = testCode;
                for (int compileFixAttempt = 1; compileFixAttempt <= 3; compileFixAttempt++) {
                    System.out.println("    [编译修复] 第 " + compileFixAttempt + " 次尝试...");
                    currentCode = testCoreClass.fixByCompileError(currentCode, compileResult, classInfo, fallbackPackage);

                    // 再次编译验证
                    compileResult = testCoreClass.realCompileValidation(currentCode, classInfo);
                    if (compileResult.isSuccess()) {
                        System.out.println("    ✓ 编译修复成功");
                        createTestClass2(currentCode, classInfo);
                        return classInfo;
                    }
                }
                // 3次修复后仍失败，使用兜底策略：注释掉错误代码
                System.out.println("    ✗ 编译修复失败，启动兜底策略...");

                // 将 CompileResult 转换为 ErrorInfo
                ErrorInfo compileErrorInfo = new ErrorInfo();
                compileErrorInfo.setErrorType("编译错误");
                compileErrorInfo.setErrorReason(compileResult.getErrorMessage());
                compileErrorInfo.setErrorLine(compileResult.getErrorLine());
                compileErrorInfo.setSeverity(ErrorSeverity.FATAL);
                compileErrorInfo.setTestCode(currentCode);

                // 确定修复范围
                FixScope fallbackScope = testCoreClass.determineFixScope(compileErrorInfo);
                String fallbackCode = testCoreClass.commentErrorCode(currentCode, compileErrorInfo, fallbackScope);

                // 验证兜底后的代码是否能编译
                CompileResult fallbackCompileResult = testCoreClass.realCompileValidation(fallbackCode, classInfo);
                if (fallbackCompileResult.isSuccess()) {
                    System.out.println("    ✓ 兜底策略成功，保存注释错误后的代码");
                    createTestClass2(fallbackCode, classInfo);
                } else {
                    System.out.println("    ✗ 兜底策略失败，保存当前代码");
                    createTestClass2(currentCode, classInfo);
                }
                return classInfo;
            }
        }

        System.out.println("    需要修复: " + errorInfo.getErrorType() + " - " + errorInfo.getErrorReason());

        // ===== 阶段3：修复 =====
        System.out.println("  [3/3] 修复测试代码...");
        errorInfo.setTestCode(testCode);

        String fixedCode;
        boolean hasFatalError = errorInfo.getSeverity() == ErrorSeverity.FATAL && errorInfo.getErrorLine() > 0;
        boolean hasUncoveredMethods = extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty();
        //   boolean hasOptimizeSuggestions = extendedResult.getOptimizeSuggestions() != null && !extendedResult.getOptimizeSuggestions().isEmpty();

        String fixType = "";
        if (hasFatalError) {
            fixType = "致命错误修复";
            fixedCode = testCoreClass.fixWithStrategyUpgrade(errorInfo);
            if (fixedCode == null || fixedCode.isEmpty()) {
                fixType += "（修复失败：返回空结果）";
                fixedCode = testCode;
            } else if (hasUncoveredMethods && !fixedCode.contains("修复失败")) {
                fixType += " + 方法覆盖补充";
                extendedResult.getErrorInfo().setTestCode(fixedCode);
                //     String optimizedCode = testCoreClass.optimizeTestCodeWithValidation(fixedCode, extendedResult, analysisInfo, hasOptimizeSuggestions);
                String optimizedCode = testCoreClass.optimizeTestCodeWithValidation(fixedCode, extendedResult, analysisInfo);
                if (optimizedCode != null && !optimizedCode.isEmpty()) {
                    fixedCode = optimizedCode;
                }
            }
        } else if (hasUncoveredMethods) {
            fixType = "方法覆盖补充";
            //   fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo, hasOptimizeSuggestions);
            fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo);
            if (fixedCode == null || fixedCode.isEmpty()) {
                fixType += "（修复失败：返回空结果）";
                fixedCode = testCode;
            }
        } /*else if (hasOptimizeSuggestions) {
            fixType = "优化建议应用";
            fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo, true);
            if (fixedCode == null || fixedCode.isEmpty()) {
                fixType += "（修复失败：返回空结果）";
                fixedCode = testCode;
            }
        }*/ else {
            fixType = "异常：无修复项但进入修复阶段";
            fixedCode = testCode;
            System.err.println("    警告：无修复项但进入了修复阶段，请检查验证逻辑");
        }

        // 最终验证
        ExtendedValidationResult finalResult = testCoreClass.validateTestCodeWithAnalysis(fixedCode, analysisInfo);
        ErrorInfo finalErrorInfo = finalResult.getErrorInfo();
        boolean finalSuccess = !finalResult.needsFixOrOptimize();

        // ===== 保存修复结果到文件 =====
        String fixResultFilePath = "src/F/" + className + "FixResult.txt";
        new File("src/F").mkdirs();
        try (FileWriter writer = new FileWriter(fixResultFilePath, true)) {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            writer.write("[" + timestamp + "] 修复结果\n");
            writer.write("修复类型: " + fixType + "\n");
            writer.write("修复前错误: " + errorInfo.getErrorType() + " - " + errorInfo.getErrorReason() + "\n");
            writer.write("修复后状态: " + (finalSuccess ? "成功" : "仍有问题 - " + finalErrorInfo.getErrorType() + " - " + finalErrorInfo.getErrorReason()) + "\n");
            String uncoveredMethodsStr = "全部覆盖";
            if (!finalResult.isAllMethodsCovered() && finalResult.getUncoveredMethods() != null && !finalResult.getUncoveredMethods().isEmpty()) {
                uncoveredMethodsStr = "未覆盖: " + String.join(", ", finalResult.getUncoveredMethods());
            }
            writer.write("修复后方法覆盖: " + uncoveredMethodsStr + "\n");
            writer.write("修复后代码:\n```java\n" + fixedCode + "\n```\n\n");
            System.out.println("    修复结果已保存到: " + fixResultFilePath);
        } catch (IOException e) {
            System.err.println("    保存修复结果时出错：" + e.getMessage());
        }

        // ===== 保存最终验证结果到文件 =====
        try (FileWriter writer = new FileWriter(validateResultFilePath, true)) {
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            String coveredStr = (finalResult.getCoveredMethods() != null && !finalResult.getCoveredMethods().isEmpty())
                    ? String.join(", ", finalResult.getCoveredMethods()) : "无";
            String uncoveredStr = (finalResult.getUncoveredMethods() != null && !finalResult.getUncoveredMethods().isEmpty())
                    ? String.join(", ", finalResult.getUncoveredMethods()) : "全部覆盖";
            String finalValidateText = "错误类型: " + finalErrorInfo.getErrorType() +
                    ", 错误行: " + finalErrorInfo.getErrorLine() +
                    ", 错误原因: " + finalErrorInfo.getErrorReason() +
                    ", 严重程度: " + finalErrorInfo.getSeverity() +
                    "\n方法覆盖情况: " +
                    "\n  已覆盖: " + coveredStr +
                    "\n  未覆盖: " + uncoveredStr;
            writer.write("[" + timestamp + "] 修复后验证结果\n" + finalValidateText + "\n\n");
        } catch (IOException e) {
            System.err.println("    保存最终验证结果时出错：" + e.getMessage());
        }

        if (finalSuccess) {
            System.out.println("    ✓ LLM修复验证通过，进行真实编译验证...");

            // ===== 真实编译验证 =====
            CompileResult compileResult = testCoreClass.realCompileValidation(fixedCode, classInfo);

            if (compileResult.isSuccess()) {
                System.out.println("    ✓ 真实编译验证通过");

                // ===== 真实运行验证 =====
                System.out.println("    [运行验证] 执行测试运行...");
                RunResult runResult = testCoreClass.realRunValidation(fixedCode, classInfo);

                if (runResult.isSuccess()) {
                    System.out.println("    ✓ 真实运行验证通过");
                    createTestClass2(fixedCode, classInfo);
                } else {
                    System.out.println("    ✗ 运行验证失败，尝试修复运行时错误...");
                    String currentCode = fixedCode;
                    for (int runFixAttempt = 1; runFixAttempt <= 2; runFixAttempt++) {
                        System.out.println("    [运行修复] 第 " + runFixAttempt + " 次尝试...");
                        currentCode = testCoreClass.fixByRunError(currentCode, runResult, classInfo, fallbackPackage);

                        // 重新编译和运行验证
                        CompileResult recompileResult = testCoreClass.realCompileValidation(currentCode, classInfo);
                        if (!recompileResult.isSuccess()) {
                            System.out.println("    ✗ 修复后编译失败，跳过运行验证");
                            break;
                        }

                        runResult = testCoreClass.realRunValidation(currentCode, classInfo);
                        if (runResult.isSuccess()) {
                            System.out.println("    ✓ 运行修复成功");
                            createTestClass2(currentCode, classInfo);
                            return classInfo;
                        }
                    }

                    // 修复失败，启用兜底策略：注释掉出错的测试方法
                    System.out.println("    ⚠ 运行修复失败，启动兜底策略（注释出错的测试方法）...");
                    System.out.println("    运行错误: " + runResult.getErrorMessage());

                    // 将 RunResult 转换为 ErrorInfo
                    ErrorInfo runErrorInfo = new ErrorInfo();
                    runErrorInfo.setErrorType("运行时错误");
                    runErrorInfo.setErrorReason(runResult.getErrorMessage());
                    runErrorInfo.setErrorLine(-1);  // 运行时错误没有具体行号，使用 -1
                    runErrorInfo.setSeverity(ErrorSeverity.FATAL);
                    runErrorInfo.setTestCode(currentCode);

                    // 注释掉出错的测试方法
                    String fallbackCode = testCoreClass.commentErrorTestMethod(currentCode, runErrorInfo);

                    // 验证兜底后的代码是否能编译和运行
                    CompileResult fallbackCompileResult = testCoreClass.realCompileValidation(fallbackCode, classInfo);
                    if (fallbackCompileResult.isSuccess()) {
                        RunResult fallbackRunResult = testCoreClass.realRunValidation(fallbackCode, classInfo);
                        if (fallbackRunResult.isSuccess()) {
                            System.out.println("    ✓ 兜底策略成功，保存注释错误方法后的代码");
                            createTestClass2(fallbackCode, classInfo);
                        } else {
                            System.out.println("    ⚠ 兜底策略后仍有运行错误，保存当前代码");
                            createTestClass2(fallbackCode, classInfo);
                        }
                    } else {
                        System.out.println("    ✗ 兜底策略失败，保存原代码");
                        createTestClass2(currentCode, classInfo);
                    }
                }
            } else {
                System.out.println("    ✗ 真实编译失败，进行编译错误修复...");
                String currentCode = fixedCode;
                for (int compileFixAttempt = 1; compileFixAttempt <= 3; compileFixAttempt++) {
                    System.out.println("    [编译修复] 第 " + compileFixAttempt + " 次尝试...");
                    currentCode = testCoreClass.fixByCompileError(currentCode, compileResult, classInfo, fallbackPackage);

                    compileResult = testCoreClass.realCompileValidation(currentCode, classInfo);
                    if (compileResult.isSuccess()) {
                        System.out.println("    ✓ 编译修复成功");
                        createTestClass2(currentCode, classInfo);
                        return classInfo;
                    }
                }
                // 3次修复后仍失败，使用兜底策略：注释掉错误代码
                System.out.println("    ✗ 编译修复失败，启动兜底策略...");

                // 将 CompileResult 转换为 ErrorInfo
                ErrorInfo compileErrorInfo = new ErrorInfo();
                compileErrorInfo.setErrorType("编译错误");
                compileErrorInfo.setErrorReason(compileResult.getErrorMessage());
                compileErrorInfo.setErrorLine(compileResult.getErrorLine());
                compileErrorInfo.setSeverity(ErrorSeverity.FATAL);
                compileErrorInfo.setTestCode(currentCode);

                // 确定修复范围
                FixScope fallbackScope = testCoreClass.determineFixScope(compileErrorInfo);
                String fallbackCode = testCoreClass.commentErrorCode(currentCode, compileErrorInfo, fallbackScope);

                // 验证兜底后的代码是否能编译
                CompileResult fallbackCompileResult = testCoreClass.realCompileValidation(fallbackCode, classInfo);
                if (fallbackCompileResult.isSuccess()) {
                    System.out.println("    ✓ 兜底策略成功，保存注释错误后的代码");
                    createTestClass2(fallbackCode, classInfo);
                } else {
                    System.out.println("    ✗ 兜底策略失败，保存当前代码");
                    createTestClass2(currentCode, classInfo);
                }
            }
        } else {
            System.out.println("    修复后仍有问题，使用兜底策略");
            FixScope lastScope = testCoreClass.determineFixScope(finalErrorInfo);
            String fallbackCode = testCoreClass.commentErrorCode(fixedCode, finalErrorInfo, lastScope);

            // 兜底后也进行真实编译验证
            System.out.println("    对兜底代码进行真实编译验证...");
            CompileResult compileResult = testCoreClass.realCompileValidation(fallbackCode, classInfo);

            if (!compileResult.isSuccess()) {
                System.out.println("    ✗ 兜底代码编译失败，尝试修复...");
                for (int compileFixAttempt = 1; compileFixAttempt <= 2; compileFixAttempt++) {
                    fallbackCode = testCoreClass.fixByCompileError(fallbackCode, compileResult, classInfo, fallbackPackage);
                    compileResult = testCoreClass.realCompileValidation(fallbackCode, classInfo);
                    if (compileResult.isSuccess()) {
                        System.out.println("    ✓ 兜底代码编译修复成功");
                        break;
                    }
                }
            }

            createTestClass2(fallbackCode, classInfo);

            try (FileWriter writer = new FileWriter(fixResultFilePath, true)) {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                writer.write("[" + timestamp + "] 兜底处理\n");
                writer.write("兜底策略: " + lastScope + "\n");
                writer.write("兜底后代码:\n```java\n" + fallbackCode + "\n```\n\n");
            } catch (IOException e) {
                System.err.println("    保存兜底结果时出错：" + e.getMessage());
            }
        }

        return classInfo;
    }

    /**
     * 处理单个待测试类（分析 → 生成 → 验证 → 修复 → 创建测试类）
     * 不执行编译和运行，只生成测试类文件
     *
     * 注意：此方法保留用于兼容旧的调用方式，新的批量处理流程使用 processGenerateValidateFix
     *
     * @param testCoreClass 测试核心类实例
     * @param sourceCodeDir 源代码目录
     * @param sourceCodePath 源代码路径
     * @param targetPackage 目标包名
     * @param logFilename 日志文件名
     * @return 类信息Map（包含包路径、类名等），失败返回null
     */
    public static Map<String, String> processSingleClass(ValiFixTextCoreDemo5 testCoreClass, String sourceCodeDir,
                                                         String sourceCodePath, String targetPackage, String logFilename) throws IOException {
        String className = testCoreClass.className;
        System.out.println("\n【处理类】" + className);

        // 读取源代码
        String sourceCodeFile = testCoreClass.readSourceCode(sourceCodePath);
        if (sourceCodeFile == null || sourceCodeFile.isEmpty()) {
            System.err.println("  错误：无法读取源代码文件: " + sourceCodePath);
            return null;
        }

        String conversationHistory = "";

        // ===== 阶段1：分析源代码 =====
        System.out.println("  [1/4] 分析源代码...");
        String analysisResultFilePath = "src/A/" + className + "Analysis.txt";
        File analysisResultFile = new File(analysisResultFilePath);

        String analyzePromt;
        Map<String, String> classInfo;

        if (analysisResultFile.exists()) {
            System.out.println("    从缓存加载分析结果");
            analyzePromt = testCoreClass.readSourceCode(analysisResultFilePath);
        } else {
            System.out.println("    调用LLM分析...");
            analyzePromt = testCoreClass.analyzeSourceCode(sourceCodeFile);
            if (analyzePromt != null && !analyzePromt.isEmpty()) {
                // 确保目录存在
                new File("src/A").mkdirs();
                try (FileWriter writer = new FileWriter(analysisResultFilePath)) {
                    writer.write(analyzePromt);
                }
                System.out.println("    分析结果已保存到: " + analysisResultFilePath);
            }
        }

        if (analyzePromt == null || analyzePromt.isEmpty()) {
            System.err.println("    分析失败：LLM未返回有效结果");
            return null;
        }

        classInfo = testCoreClass.extractClassInfo3(analyzePromt);
        if (classInfo == null || classInfo.isEmpty()) {
            System.err.println("    分析失败：无法提取类信息");
            return null;
        }
        System.out.println("    ✓ 分析完成");

        // ===== 阶段2：生成测试代码 =====
        System.out.println("  [2/4] 生成测试代码...");
        String testCode = null;
        String testFilePath = getTestFilePath(classInfo);
        File testFile = new File(testFilePath);

        // 检查测试类是否已存在，存在则跳过后续阶段
        if (testFile.exists() && testFile.length() > 0) {
            System.out.println("    ✓ 测试类已存在，跳过生成-验证-修复阶段: " + testFilePath);
            return classInfo;
        }

        System.out.println("    调用LLM生成...");
        testCode = testCoreClass.generateTestCode2(sourceCodeFile, analyzePromt, conversationHistory);

        if (testCode == null || testCode.isEmpty()) {
            System.err.println("    生成失败：LLM未返回有效结果");
            return null;
        }
        System.out.println("    ✓ 生成完成");

        // ===== 阶段3：动态验证 =====
        System.out.println("  [3/4] 验证测试代码...");
        AnalysisInfo analysisInfo = testCoreClass.parseAnalysisResult(analyzePromt);
        ExtendedValidationResult extendedResult = testCoreClass.validateTestCodeWithAnalysis(testCode, analysisInfo);
        ErrorInfo errorInfo = extendedResult.getErrorInfo();

        // 严格判断验证是否通过：必须无致命错误
        boolean validateSuccess = false;
        // 只有当严重程度不是FATAL时，才可能验证通过
        if (errorInfo.getSeverity() != ErrorSeverity.FATAL) {
            // 无致命错误，检查方法覆盖
            if (extendedResult.isAllMethodsCovered() ||
                    (extendedResult.getUncoveredMethods() == null || extendedResult.getUncoveredMethods().isEmpty())) {
                validateSuccess = true;
            }
        }

        // ===== 保存验证结果到文件 =====
        String validateResultFilePath = "src/V/" + className + "ValidateResult.txt";
        new File("src/V").mkdirs(); // 确保目录存在
        String validateResultText = "错误类型: " + errorInfo.getErrorType() +
                ", 错误行: " + errorInfo.getErrorLine() +
                ", 错误原因: " + errorInfo.getErrorReason() +
                ", 严重程度: " + errorInfo.getSeverity() +
                "\n方法覆盖情况: " +
                "\n  已覆盖: " + (extendedResult.getCoveredMethods() != null ? String.join(", ", extendedResult.getCoveredMethods()) : "无") +
                "\n  未覆盖: " + (extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty() ? String.join(", ", extendedResult.getUncoveredMethods()) : "全部覆盖") ;
        // "\n优化建议: " + (extendedResult.getOptimizeSuggestions() != null && !extendedResult.getOptimizeSuggestions().isEmpty() ? String.join("; ", extendedResult.getOptimizeSuggestions()) : "无");
        try (FileWriter writer = new FileWriter(validateResultFilePath, true)) { // 追加模式
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            writer.write("[" + timestamp + "] 验证结果\n" + validateResultText + "\n\n");
            System.out.println("    验证结果已保存到: " + validateResultFilePath);
        } catch (IOException e) {
            System.err.println("    保存验证结果时出错：" + e.getMessage());
        }

        if (validateSuccess) {
            System.out.println("    ✓ 验证通过");
            // 直接创建测试类
            createTestClass2(testCode, classInfo);
            return classInfo;
        }

        System.out.println("    需要修复: " + errorInfo.getErrorType() + " - " + errorInfo.getErrorReason());

        // ===== 阶段4：修复 =====
        System.out.println("  [4/4] 修复测试代码...");
        errorInfo.setTestCode(testCode);

        String fixedCode;
        boolean hasFatalError = errorInfo.getSeverity() == ErrorSeverity.FATAL && errorInfo.getErrorLine() > 0;
        boolean hasUncoveredMethods = extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty();
        //  boolean hasOptimizeSuggestions = extendedResult.getOptimizeSuggestions() != null && !extendedResult.getOptimizeSuggestions().isEmpty();

        // 记录修复类型
        String fixType = "";
        if (hasFatalError) {
            fixType = "致命错误修复";
            fixedCode = testCoreClass.fixWithStrategyUpgrade(errorInfo);
            // 检查修复是否返回有效结果
            if (fixedCode == null || fixedCode.isEmpty()) {
                fixType += "（修复失败：返回空结果）";
                fixedCode = testCode; // 修复失败时保留原代码，后续兜底处理
            } else if (hasUncoveredMethods && !fixedCode.contains("修复失败")) {
                fixType += " + 方法覆盖补充";
                extendedResult.getErrorInfo().setTestCode(fixedCode);
                //     String optimizedCode = testCoreClass.optimizeTestCodeWithValidation(fixedCode, extendedResult, analysisInfo, hasOptimizeSuggestions);
                String optimizedCode = testCoreClass.optimizeTestCodeWithValidation(fixedCode, extendedResult, analysisInfo);
                if (optimizedCode != null && !optimizedCode.isEmpty()) {
                    fixedCode = optimizedCode;
                }
            }
        } else if (hasUncoveredMethods) {
            fixType = "方法覆盖补充";
            //   fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo, hasOptimizeSuggestions);
            fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo);
            if (fixedCode == null || fixedCode.isEmpty()) {
                fixType += "（修复失败：返回空结果）";
                fixedCode = testCode;
            }
        } /*else if (hasOptimizeSuggestions) {
            fixType = "优化建议应用";
            fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo, true);
            if (fixedCode == null || fixedCode.isEmpty()) {
                fixType += "（修复失败：返回空结果）";
                fixedCode = testCode;
            }
        } */else {
            // 不应该到达这里，记录异常情况
            fixType = "异常：无修复项但进入修复阶段";
            fixedCode = testCode;
            System.err.println("    警告：无修复项但进入了修复阶段，请检查验证逻辑");
        }

        // 最终验证
        ExtendedValidationResult finalResult = testCoreClass.validateTestCodeWithAnalysis(fixedCode, analysisInfo);
        ErrorInfo finalErrorInfo = finalResult.getErrorInfo();
        boolean finalSuccess = !finalResult.needsFixOrOptimize();

        // ===== 保存修复结果到文件 =====
        String fixResultFilePath = "src/F/" + className + "FixResult.txt";
        new File("src/F").mkdirs(); // 确保目录存在
        try (FileWriter writer = new FileWriter(fixResultFilePath, true)) { // 追加模式
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            writer.write("[" + timestamp + "] 修复结果\n");
            writer.write("修复类型: " + fixType + "\n");
            writer.write("修复前错误: " + errorInfo.getErrorType() + " - " + errorInfo.getErrorReason() + "\n");
            writer.write("修复后状态: " + (finalSuccess ? "成功" : "仍有问题 - " + finalErrorInfo.getErrorType() + " - " + finalErrorInfo.getErrorReason()) + "\n");
            // 安全获取未覆盖方法列表
            String uncoveredMethodsStr = "全部覆盖";
            if (!finalResult.isAllMethodsCovered() && finalResult.getUncoveredMethods() != null && !finalResult.getUncoveredMethods().isEmpty()) {
                uncoveredMethodsStr = "未覆盖: " + String.join(", ", finalResult.getUncoveredMethods());
            }
            writer.write("修复后方法覆盖: " + uncoveredMethodsStr + "\n");
            writer.write("修复后代码:\n```java\n" + fixedCode + "\n```\n\n");
            System.out.println("    修复结果已保存到: " + fixResultFilePath);
        } catch (IOException e) {
            System.err.println("    保存修复结果时出错：" + e.getMessage());
        }

        // ===== 保存最终验证结果到文件 =====
        try (FileWriter writer = new FileWriter(validateResultFilePath, true)) { // 追加模式
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            // 安全获取覆盖方法列表
            String coveredStr = (finalResult.getCoveredMethods() != null && !finalResult.getCoveredMethods().isEmpty())
                    ? String.join(", ", finalResult.getCoveredMethods()) : "无";
            String uncoveredStr = (finalResult.getUncoveredMethods() != null && !finalResult.getUncoveredMethods().isEmpty())
                    ? String.join(", ", finalResult.getUncoveredMethods()) : "全部覆盖";
            String finalValidateText = "错误类型: " + finalErrorInfo.getErrorType() +
                    ", 错误行: " + finalErrorInfo.getErrorLine() +
                    ", 错误原因: " + finalErrorInfo.getErrorReason() +
                    ", 严重程度: " + finalErrorInfo.getSeverity() +
                    "\n方法覆盖情况: " +
                    "\n  已覆盖: " + coveredStr +
                    "\n  未覆盖: " + uncoveredStr;
            writer.write("[" + timestamp + "] 修复后验证结果\n" + finalValidateText + "\n\n");
        } catch (IOException e) {
            System.err.println("    保存最终验证结果时出错：" + e.getMessage());
        }

        if (finalSuccess) {
            System.out.println("    ✓ 修复成功");
            createTestClass2(fixedCode, classInfo);
        } else {
            System.out.println("    修复后仍有问题，使用兜底策略");
            // 兜底：注释错误代码
            FixScope lastScope = testCoreClass.determineFixScope(finalErrorInfo);
            String fallbackCode = testCoreClass.commentErrorCode(fixedCode, finalErrorInfo, lastScope);
            createTestClass2(fallbackCode, classInfo);

            // 记录兜底处理到修复结果文件
            try (FileWriter writer = new FileWriter(fixResultFilePath, true)) {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                writer.write("[" + timestamp + "] 兜底处理\n");
                writer.write("兜底策略: " + lastScope + "\n");
                writer.write("兜底后代码:\n```java\n" + fallbackCode + "\n```\n\n");
            } catch (IOException e) {
                System.err.println("    保存兜底结果时出错：" + e.getMessage());
            }
        }

        return classInfo;
    }


    public static void mainM(String apiKey, ValiFixTextCoreDemo5 testCoreClass, String sourceCodeDir, String sourceCodePath, String targetPackage, String logFilename) throws IOException {
        System.out.println("\n================================================================================");
        System.out.println("                    测试代码生成与验证修复流程开始（含动态验证）");
        System.out.println("================================================================================");
        System.out.println("【配置信息】");
        System.out.println("  源代码目录: " + sourceCodeDir);
        System.out.println("  源代码路径: " + sourceCodePath);
        System.out.println("  目标包名: " + targetPackage);
        System.out.println("  当前类名: " + testCoreClass.className);
        System.out.println("  日志文件: " + logFilename);
        System.out.println("--------------------------------------------------------------------------------");

        List<String> classNames = new ArrayList<>();
        File dir = new File(sourceCodeDir);
        if (dir.exists() && dir.isDirectory()) {
            File[] files = dir.listFiles((d, name) -> name.endsWith(".java"));
            if (files != null) {
                for (File file : files) {
                    // 提取类名
                    String className = extractClassName(file);
                    if (className != null && !className.isEmpty()) {
                        classNames.add(className);
                    }
                }
            }
        }
        String sourceCodeFile = testCoreClass.readSourceCode(sourceCodePath);
        String conversationHistory = ""; // 初始化对话历史
        boolean analyzeSuccess = false;
        boolean generateSuccess = false;
        boolean validateSuccess = false;
        boolean fixSuccess = false;
        boolean evaluateSuccess = false;
        boolean overallSuccess = false;
        long analyzeEndTime = 0;
        long generateEndTime = 0;
        long validateEndTime = 0;
        long fixEndTime = 0;
        long evaluateEndTime = 0;
        int analyzeTokenCount = 0;
        int generateTokenCount = 0;
        int validateTokenCount = 0;
        int fixTokenCount = 0;
        int evaluateTokenCount = 0;
        String validateFailReason = "";
        int fixAttempt = 0;
        String fixFailReason = "";
        String fixSuccessTimes = "0";
        String evaluateCoverageData = "";
        long startTime = System.currentTimeMillis();
        long analyzeStart = System.currentTimeMillis();
        // 初始化 Timer
        testCoreClass.timer.start("套件");

        // ==================== 阶段1：分析源代码 ====================
        System.out.println("\n================================================================================");
        System.out.println("【阶段1】源代码分析");
        System.out.println("================================================================================");

        // 检查分析结果文件是否存在
        String analysisResultFilePath = "src/A/" + testCoreClass.className + "Analysis.txt";
        File analysisResultFile = new File(analysisResultFilePath);

        String analyzePromt;
        Map<String, String> classInfo;
        if (analysisResultFile.exists()) {
            System.out.println("  [缓存] 分析结果已存在，从缓存加载: " + analysisResultFilePath);
            // 从文件中读取分析结果
            analyzePromt = testCoreClass.readSourceCode(analysisResultFilePath);
            if (analyzePromt == null || analyzePromt.isEmpty()) {
                System.err.println("  [错误] 读取分析结果文件时出错，内容为空：" + analysisResultFilePath);
                return;
            }
            classInfo = testCoreClass.extractClassInfo3(analyzePromt);

            if (classInfo == null || classInfo.isEmpty()) {
                System.err.println("  [错误] 提取类信息失败，分析结果无效：" + analyzePromt);
                return;
            }
            analyzeSuccess = true;
            analyzeEndTime = System.currentTimeMillis();
            System.out.println("  [成功] 分析结果加载完成");
        } else {
            System.out.println("  [执行] 调用LLM分析源代码...");
            // 分析开始
            testCoreClass.timer.start("分析");
            analyzePromt = testCoreClass.analyzeSourceCode(sourceCodeFile);
            if (analyzePromt == null || analyzePromt.isEmpty()) {
                System.out.println("  [失败] 分析阶段失败：LLM 未返回有效结果");
                return;
            }
            // 从分析结果中提取类信息
            classInfo = testCoreClass.extractClassInfo3(analyzePromt);
            if (classInfo == null || classInfo.isEmpty()) {
                System.err.println("  [错误] 提取类信息失败，分析结果无效：" + analyzePromt);
                return;
            }
            analyzeSuccess = true;
            analyzeEndTime = System.currentTimeMillis();

            // 保存分析结果到文件
            try (FileWriter writer = new FileWriter(analysisResultFilePath)) {
                writer.write(analyzePromt);
                System.out.println("  [保存] 分析结果已保存到: " + analysisResultFilePath);
            } catch (IOException e) {
                System.err.println("  [警告] 保存分析结果时出错：" + e.getMessage());
            }
            testCoreClass.timer.end("分析");
        }
        System.out.println("  [信息] 包路径: " + classInfo.get("包路径"));
        System.out.println("  [信息] 类名: " + classInfo.get("类名"));
        System.out.println("  [耗时] " + (analyzeEndTime - analyzeStart) + " ms");

        // ==================== 阶段2：生成测试代码 ====================
        System.out.println("\n================================================================================");
        System.out.println("【阶段2】测试代码生成");
        System.out.println("================================================================================");

        String testCode = null;
        String testFilePath = getTestFilePath(classInfo);
        File testFile = new File(testFilePath);
        if (testFile.exists()) {
            System.out.println("  [缓存] 测试类已存在，从文件加载: " + testFilePath);
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(testFile), StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                // 逐行读取文件内容，保留原始换行符
                while ((line = br.readLine()) != null) {
                    sb.append(line).append(System.lineSeparator());
                }
                // 去除末尾多余的换行符，保证内容和原文件一致
                testCode = sb.toString().trim();
                generateSuccess = true;
                System.out.println("  [成功] 测试代码加载完成，共 " + testCode.split("\n").length + " 行");
            } catch (IOException e) {
                // 异常处理：打印错误信息，标记整体失败
                System.err.println("  [错误] 读取已存在的测试类文件失败：" + testFilePath);
                e.printStackTrace();
                overallSuccess = false; // 读取失败则标记为未成功
            }
            overallSuccess = true; // 标记整体成功，因为测试类已存在
        } else {
            System.out.println("  [执行] 调用LLM生成测试代码...");
            //生成开始
            testCoreClass.timer.start("生成");
            if (sourceCodeFile == null || sourceCodeFile.isEmpty()) {
                System.err.println("  [错误] 源代码文件内容为空，无法生成测试代码");
                return;
            }
            testCode = testCoreClass.generateTestCode2(sourceCodeFile, analyzePromt, conversationHistory);
            generateSuccess = true;
            generateEndTime = System.currentTimeMillis();
            generateTokenCount = testCoreClass.tokenCount - analyzeTokenCount;
            if (testCode == null || testCode.isEmpty()) {
                System.out.println("生成阶段失败：LLM 未返回有效结果");
                return;
            }
            //生成结束
            testCoreClass.timer.end("生成");
        }

        // ==================== 阶段2.5：解析分析结果为结构化对象（用于动态验证） ====================
        AnalysisInfo analysisInfo = testCoreClass.parseAnalysisResult(analyzePromt);
        System.out.println("\n【分析结果结构化】");
        System.out.println("  待测方法数: " + analysisInfo.getMethods().size());
        for (MethodInfo method : analysisInfo.getMethods()) {
            System.out.println("    - " + method.getMethodName() + " -> " + method.getReturnType());
        }

        //验证开始
        testCoreClass.timer.start("验证");

        // ==================== 阶段3：动态验证（结合分析结果检查方法覆盖） ====================
        System.out.println("\n================================================================================");
        System.out.println("【阶段3】测试代码动态验证（含方法覆盖检查）");
        System.out.println("================================================================================");

        // 使用动态验证（结合分析结果）
        ExtendedValidationResult extendedResult = testCoreClass.validateTestCodeWithAnalysis(testCode, analysisInfo);
        ErrorInfo errorInfo = extendedResult.getErrorInfo();

        // 判断验证是否通过：无致命错误 且 所有方法已覆盖
        validateSuccess = false;
        // 只有当严重程度不是FATAL时，才可能验证通过
        if (errorInfo.getSeverity() != ErrorSeverity.FATAL) {
            // 无致命错误，检查方法覆盖
            if (extendedResult.isAllMethodsCovered() ||
                    (extendedResult.getUncoveredMethods() == null || extendedResult.getUncoveredMethods().isEmpty())) {
                validateSuccess = true;
            }
        }

        // 如果验证结果中没有具体错误且方法全覆盖，认为通过
        if (errorInfo.getErrorLine() == -1 && extendedResult.isAllMethodsCovered()) {
            validateSuccess = true;
        }
        validateEndTime = System.currentTimeMillis();

        // 保存验证结果到文件
        String validateResultFilePath = "src/V/" + testCoreClass.className + "ValidateResult.txt";
        String validateResultText = "错误类型: " + errorInfo.getErrorType() +
                ", 错误行: " + errorInfo.getErrorLine() +
                ", 错误原因: " + errorInfo.getErrorReason() +
                "\n方法覆盖情况: " +
                "\n  已覆盖: " + (extendedResult.getCoveredMethods() != null ? String.join(", ", extendedResult.getCoveredMethods()) : "无") +
                "\n  未覆盖: " + (extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty() ? String.join(", ", extendedResult.getUncoveredMethods()) : "全部覆盖") ;
        //    "\n优化建议: " + (extendedResult.getOptimizeSuggestions() != null ? String.join("; ", extendedResult.getOptimizeSuggestions()) : "无");
        try (FileWriter writer = new FileWriter(validateResultFilePath, true)) { // 追加模式
            String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            writer.write("[" + timestamp + "]\n" + validateResultText + "\n\n");
        } catch (IOException e) {
            System.err.println("保存验证结果时出错：" + e.getMessage());
        }

        validateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount;
        if (errorInfo == null) {
            System.out.println("验证阶段失败：LLM 未返回有效结果");
            return;
        }
        //验证结束
        testCoreClass.timer.end("验证");

        // ==================== 打印动态验证结果 ====================
        System.out.println("  [验证状态] " + (validateSuccess ? "✓ 通过" : "✗ 未通过"));
        System.out.println("  [错误类型] " + errorInfo.getErrorType());
        System.out.println("  [错误行号] " + (errorInfo.getErrorLine() == -1 ? "无" : errorInfo.getErrorLine()));
        System.out.println("  [错误原因] " + errorInfo.getErrorReason());
        System.out.println("  [严重程度] " + errorInfo.getSeverity());

        // 打印方法覆盖情况（动态验证新增）
        System.out.println("  [方法覆盖]");
        if (extendedResult.getCoveredMethods() != null && !extendedResult.getCoveredMethods().isEmpty()) {
            System.out.println("    已覆盖: " + String.join(", ", extendedResult.getCoveredMethods()));
        }
        if (extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty()) {
            System.out.println("    未覆盖: " + String.join(", ", extendedResult.getUncoveredMethods()));
        } else {
            System.out.println("    ✓ 所有方法已覆盖");
        }

        /*// 打印优化建议
        if (extendedResult.getOptimizeSuggestions() != null && !extendedResult.getOptimizeSuggestions().isEmpty()) {
            System.out.println("  [优化建议]");
            for (String suggestion : extendedResult.getOptimizeSuggestions()) {
                System.out.println("    - " + suggestion);
            }
        }*/
        System.out.println("  [耗时] " + (validateEndTime - generateEndTime) + " ms");
        System.out.println("--------------------------------------------------------------------------------");

        if (!validateSuccess) {
            validateFailReason = validateResultText;
        }

        if (validateSuccess) {
            //创建测试类
            //  createTestClass(testCode);
            System.out.println("测试类创建开始：");
            createTestClass2(testCode, classInfo);
            overallSuccess = true;
            try {
                Thread.sleep(5000); // 等待 5 秒
            } catch (InterruptedException e) {
                System.err.println("等待过程中被中断：" + e.getMessage());
            }
            System.out.println("测试类编译开始：");
            TestClassCompiler testClassCompiler = new TestClassCompiler();
            boolean compileResult = testClassCompiler.compileTestClass(classInfo);
            // 编译测试类
            if (compileResult) {
                System.out.println("测试类编译成功！");
                //编译成功，尝试运行测试代码
                // 运行测试
                try {
                    System.out.println("测试类运行开始：");
                    TestClassRunner runner = new TestClassRunner();

                    // 处理泛型类名：移除泛型参数部分
                    String className = classInfo.get("类名");
                    if (className.contains("<")) {
                        className = className.substring(0, className.indexOf("<")).trim();
                    }

                    //获取测试代码位置
                    testFilePath = Paths.get(
                            "D:\\fx\\jafx\\chatunitest-core",
                            "src", "test", "java",
                            classInfo.get("包路径").replace(".", File.separator),
                            className + "Test.java"
                    ).toString();
                    String testcoderunner = runner.readFileContent(testFilePath);
                    boolean testResult = runner.runTests(testcoderunner, classInfo);
                    System.out.println(runner.getDetailedResults());
                    String DetailedResults = runner.getDetailedResults();
                    if (testResult) {
                        System.out.println("测试类覆盖率计算开始：");
                        String mvnTestResult1 = testCoreClass.executeMavenCommand("test");
                        System.out.println("mvn test 结果1：\n" + mvnTestResult1);
                        //规则修复
                        // 把文本落盘到文件
                        // Path mvnlogFile = Paths.get("target/mvn-guideFixTest.log");

                        // 处理泛型类名：移除泛型参数部分（className已在上面定义）
                        Path mvnlogFile = Paths.get("target", "mvn-" + className + "Test.log");
                        Files.write(mvnlogFile, DetailedResults.getBytes(StandardCharsets.UTF_8));
                        GuideFixHealer.fixSimpleFailures(mvnlogFile.toString());
                        String mvnTestResult2 = testCoreClass.executeMavenCommand("test");
                        System.out.println("mvn test 结果2：\n" + mvnTestResult2);
                        double instructionCoverage = extractCoverageFromMvnResult(mvnTestResult2, "Overall Coverage");
                        System.out.println("mvn test结果展示：" + instructionCoverage);
                        // 提取覆盖率数据（假设 mvn test 输出中包含覆盖率信息）
                        List<Map<String, String>> coverageInfoList = extractCoverageInfo();
                        //String targetPackage = "cli";
                        //1 String targetPackage = "codeing";
                        List<Map<String, String>> filteredCoverageInfoList = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, targetPackage);

                        // 检查覆盖率
                        boolean coverageSufficient = checkCoverage(filteredCoverageInfoList);
                        System.out.println("Coverage sufficient: " + coverageSufficient);
                    } else {
                        System.out.println("测试未通过：");
                        System.out.println(runner.getDetailedResults());
                    }
                } catch (Exception e) {
                    System.out.println("运行测试时出错：" + e.getMessage());
                    e.printStackTrace();
                }


            } else {
                System.out.println("测试类编译失败！");
            }
        } else {
            // ===== 阶段4：动态优化修复（结合分析结果和验证结果） =====
            System.out.println("\n================================================================================");
            System.out.println("【阶段4】动态优化修复（结合分析结果）");
            System.out.println("================================================================================");

            errorInfo.setTestCode(testCode);

            //修复开始
            testCoreClass.timer.start("修复");

            // 判断修复类型
            boolean hasFatalError = errorInfo.getSeverity() == ErrorSeverity.FATAL && errorInfo.getErrorLine() > 0;
            boolean hasUncoveredMethods = extendedResult.getUncoveredMethods() != null && !extendedResult.getUncoveredMethods().isEmpty();
            //  boolean hasOptimizeSuggestions = extendedResult.getOptimizeSuggestions() != null && !extendedResult.getOptimizeSuggestions().isEmpty();

            System.out.println("  [修复需求分析]");
            System.out.println("    致命错误: " + (hasFatalError ? "是 - " + errorInfo.getErrorType() : "否"));
            System.out.println("    未覆盖方法: " + (hasUncoveredMethods ? "是 - " + String.join(", ", extendedResult.getUncoveredMethods()) : "否"));
            //   System.out.println("    优化建议: " + (hasOptimizeSuggestions ? "是 - " + extendedResult.getOptimizeSuggestions().size() + "条" : "否"));

            String fixedCode;

            if (hasFatalError) {
                // 有致命错误：先用原有策略修复致命错误
                System.out.println("\n  [步骤1] 修复致命错误（使用目标最优修复策略）");
                fixedCode = testCoreClass.fixWithStrategyUpgrade(errorInfo);

                // 修复致命错误后，如果还有未覆盖方法，继续优化
                if (hasUncoveredMethods && !fixedCode.contains("修复失败")) {
                    System.out.println("\n  [步骤2] 补充未覆盖方法的测试用例");
                    // 更新extendedResult的testCode
                    extendedResult.getErrorInfo().setTestCode(fixedCode);
                    //  fixedCode = testCoreClass.optimizeTestCodeWithValidation(fixedCode, extendedResult, analysisInfo, hasOptimizeSuggestions);
                }
            } /*else if (hasUncoveredMethods) {
                // 无致命错误但有未覆盖方法：直接优化补充
                System.out.println("\n  [步骤1] 补充未覆盖方法的测试用例");
                fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo, hasOptimizeSuggestions);
            } else if (hasOptimizeSuggestions) {
                // 只有优化建议：可选优化
                System.out.println("\n  [步骤1] 应用优化建议");
                fixedCode = testCoreClass.optimizeTestCodeWithValidation(testCode, extendedResult, analysisInfo, true);
            }*/ else {
                // 不应该到这里，但作为兜底
                fixedCode = testCode;
            }

            fixEndTime = System.currentTimeMillis();
            fixTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount;

            // 保存修复结果到文件
            String fixResultFilePath = "src/F/" + testCoreClass.className + "FixResult.txt";

            try (FileWriter writer = new FileWriter(fixResultFilePath, true)) {
                String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
                writer.write("[" + timestamp + "]\n");
                //   writer.write("修复类型: " + (hasFatalError ? "致命错误修复" : "") + (hasUncoveredMethods ? " + 方法覆盖补充" : "") + (hasOptimizeSuggestions ? " + 优化建议应用" : "") + "\n");
                writer.write("修复类型: " + (hasFatalError ? "致命错误修复" : "") + (hasUncoveredMethods ? " + 方法覆盖补充" : "") + (" + 优化建议应用") + "\n");
                writer.write("修复结果：\n" + fixedCode + "\n\n");
            } catch (IOException e) {
                System.err.println("保存修复结果时出错：" + e.getMessage());
            }

            // 最终验证修复后的代码（使用动态验证）
            System.out.println("\n  [最终验证] 验证修复后的代码...");
            ExtendedValidationResult finalResult = testCoreClass.validateTestCodeWithAnalysis(fixedCode, analysisInfo);
            ErrorInfo finalErrorInfo = finalResult.getErrorInfo();

            // 判断最终是否成功：无致命错误 且 所有方法已覆盖
            boolean finalSuccess = !finalResult.needsFixOrOptimize();

            if (finalSuccess) {
                // 修复成功
                fixSuccess = true;
                System.out.println("  ✓ 修复成功！");
                System.out.println("    - 无致命错误");
                System.out.println("    - 所有方法已覆盖: " + (finalResult.getCoveredMethods() != null ? String.join(", ", finalResult.getCoveredMethods()) : "是"));
              /*  if (finalResult.getOptimizeSuggestions() != null && !finalResult.getOptimizeSuggestions().isEmpty()) {
           //         System.out.println("    - 剩余优化建议: " + String.join("; ", finalResult.getOptimizeSuggestions()));
                }*/

                // 创建测试类
                testCoreClass.createTestClass2(fixedCode, classInfo);
                overallSuccess = true;

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    System.err.println("等待过程中被中断：" + e.getMessage());
                }

                // 编译测试类
                TestClassCompiler testClassCompiler = new TestClassCompiler();
                boolean compileResult = testClassCompiler.compileTestClass(classInfo);

                if (compileResult) {
                    System.out.println("测试类编译成功！");
                    try {
                        TestClassRunner runner = new TestClassRunner();
                        testFilePath = Paths.get(
                                "D:\\fx\\jafx\\chatunitest-core",
                                "src", "test", "java",
                                classInfo.get("包路径").replace(".", File.separator),
                                classInfo.get("类名") + "Test.java"
                        ).toString();
                        String testcoderunner2 = runner.readFileContent(testFilePath);
                        boolean testResult = runner.runTests(testcoderunner2, classInfo);
                        System.out.println(runner.getDetailedResults());
                        String DetailedResults = runner.getDetailedResults();

                        if (testResult) {
                            System.out.println("所有测试通过！");
                            String mvnTestResult1 = testCoreClass.executeMavenCommand("test");
                            Path mvnlogFile = Paths.get("target", "mvn-" + classInfo.get("类名") + "Test.log");
                            Files.write(mvnlogFile, DetailedResults.getBytes(StandardCharsets.UTF_8));
                            GuideFixHealer.fixSimpleFailures(mvnlogFile.toString());
                            String mvnTestResult2 = testCoreClass.executeMavenCommand("test");
                            System.out.println("mvn test 结果：\n" + mvnTestResult2);

                            List<Map<String, String>> coverageInfoList = extractCoverageInfo();
                            List<Map<String, String>> filteredCoverageInfoList = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, targetPackage);
                            boolean coverageSufficient = checkCoverage(filteredCoverageInfoList);
                            System.out.println("Coverage sufficient: " + coverageSufficient);
                        } else {
                            System.out.println("测试未通过：");
                            System.out.println(runner.getDetailedResults());
                        }
                    } catch (Exception e) {
                        System.out.println("运行测试时出错：" + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("测试类编译失败！");
                }

                fixSuccessTimes = "1";
            } else {
                // 修复失败
                System.out.println("  ✗ 修复失败");
                System.out.println("    - 错误: " + finalErrorInfo.getErrorReason());
                if (finalResult.getUncoveredMethods() != null && !finalResult.getUncoveredMethods().isEmpty()) {
                    System.out.println("    - 仍未覆盖的方法: " + String.join(", ", finalResult.getUncoveredMethods()));
                }

                fixFailReason = "错误: " + finalErrorInfo.getErrorReason() +
                        (finalResult.getUncoveredMethods() != null && !finalResult.getUncoveredMethods().isEmpty()
                                ? "; 未覆盖方法: " + String.join(", ", finalResult.getUncoveredMethods()) : "");
                testCoreClass.failedClasses.add(testCoreClass.className);

                // 兜底：创建带注释的测试类保证编译通过
                FixScope lastScope = testCoreClass.determineFixScope(finalErrorInfo);
                String fallbackCode = testCoreClass.commentErrorCode(fixedCode, finalErrorInfo, lastScope);
                testCoreClass.createTestClass2(fallbackCode, classInfo);
            }

            //修复结束
            testCoreClass.timer.end("修复");
        }
        // 记录测试代码生成过程的信息
        testCoreClass.logTestCodeGenerationInfo(analyzeSuccess, generateSuccess, validateSuccess,
                fixSuccess, evaluateSuccess, overallSuccess,analyzeStart,analyzeEndTime, generateEndTime,
                validateEndTime, fixEndTime, evaluateEndTime, analyzeTokenCount, generateTokenCount,
                validateTokenCount, fixTokenCount, evaluateTokenCount, validateFailReason,
                fixAttempt, fixFailReason, fixSuccessTimes, evaluateCoverageData,logFilename);
        testCoreClass.closeClient();
        testCoreClass.timer.end("套件");
    }
}



//对比实验：1.ChatUniTest、HITS、Evosuite、SymPrompt、TestPilot、TestSpark？
//对比数据集：Cli、Csv