package zju.cst.aces.runner;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.StandardCopyOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 处理TryCommons下面数据
 * 功能：清洗、去重、去噪、过滤、标准化、代码分块
 */
public class Cleaning {

    // Java 8 兼容的文件读取方法
    private static String readFileContent(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    // Java 8 兼容的文件写入方法
    private static void writeFileContent(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }

    // 用于去重的哈希集合
    private final Set<String> contentHashes = new HashSet<>();
    
    // 单行注释模式
    private static final Pattern SINGLE_LINE_COMMENT = Pattern.compile("//.*");
    
    // 多行注释模式
    private static final Pattern MULTI_LINE_COMMENT = Pattern.compile("/\\*[\\s\\S]*?\\*/");
    
    // 空白行模式
    private static final Pattern BLANK_LINE = Pattern.compile("^\\s*$", Pattern.MULTILINE);
    
    // 连续空白行模式
    private static final Pattern MULTIPLE_BLANK_LINES = Pattern.compile("(\\r?\\n){3,}");

    /**
     * 清洗单个文件内容
     * @param content 原始内容
     * @return 清洗后的内容
     */
    public String cleanContent(String content) {
        if (content == null || content.isEmpty()) {
            return content;
        }
        
        String result = content;
        
        // 1. 删除多行注释
        result = removeMultiLineComments(result);
        
        // 2. 删除单行注释
        result = removeSingleLineComments(result);
        
        // 3. 删除空白行
        result = removeBlankLines(result);
        
        // 4. 标准化空白字符
        result = normalizeWhitespace(result);
        
        return result;
    }

    /**
     * 删除单行注释 (//)
     */
    public String removeSingleLineComments(String content) {
        if (content == null) return null;
        
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean inChar = false;
        char prevChar = 0;
        
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            StringBuilder cleanLine = new StringBuilder();
            inString = false;
            inChar = false;
            prevChar = 0;
            
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                
                // 处理字符串内的情况
                if (c == '"' && prevChar != '\\' && !inChar) {
                    inString = !inString;
                }
                if (c == '\'' && prevChar != '\\' && !inString) {
                    inChar = !inChar;
                }
                
                // 检测单行注释
                if (!inString && !inChar && c == '/' && j + 1 < line.length() && line.charAt(j + 1) == '/') {
                    break; // 跳过剩余部分
                }
                
                cleanLine.append(c);
                prevChar = c;
            }
            
            result.append(cleanLine);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }

    /**
     * 删除多行注释
     */
    public String removeMultiLineComments(String content) {
        if (content == null) return null;
        return MULTI_LINE_COMMENT.matcher(content).replaceAll("");
    }

    /**
     * 删除空白行
     */
    public String removeBlankLines(String content) {
        if (content == null) return null;
        
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                result.append(line).append("\n");
            }
        }
        
        // 移除末尾多余的换行符
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        
        return result.toString();
    }

    /**
     * 标准化空白字符
     * - 将多个连续空格替换为单个空格
     * - 移除行尾空白
     * - 统一换行符
     */
    public String normalizeWhitespace(String content) {
        if (content == null) return null;
        
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            // 移除行尾空白
            line = line.replaceAll("\\s+$", "");
            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }

    /**
     * 检查内容是否重复（基于哈希）
     * @param content 内容
     * @return true 如果是重复内容
     */
    public boolean isDuplicate(String content) {
        String hash = computeHash(content);
        return !contentHashes.add(hash);
    }

    /**
     * 计算内容的MD5哈希
     */
    private String computeHash(String content) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // 降级使用hashCode
            return String.valueOf(content.hashCode());
        }
    }

    /**
     * 重置去重缓存
     */
    public void resetDuplicateCache() {
        contentHashes.clear();
    }

    /**
     * 过滤无效代码
     * - 过滤过短的代码片段
     * - 过滤只有import语句的文件
     */
    public boolean isValidCode(String content, int minLines) {
        if (content == null || content.trim().isEmpty()) {
            return false;
        }
        
        String[] lines = content.split("\n");
        int codeLines = 0;
        
        for (String line : lines) {
            String trimmed = line.trim();
            // 跳过空行、import、package语句
            if (!trimmed.isEmpty() 
                && !trimmed.startsWith("import ") 
                && !trimmed.startsWith("package ")) {
                codeLines++;
            }
        }
        
        return codeLines >= minLines;
    }

    // ==================== 复杂度分析与报告生成 ====================

    /**
     * 方法信息类 - 包含方法代码和复杂度指标
     */
    public static class MethodInfo {
        public String className;      // 所属类名
        public String methodName;     // 方法名
        public String signature;      // 方法签名
        public String code;           // 方法代码
        public int startLine;         // 起始行
        public int endLine;           // 结束行
        public int lineCount;         // 代码行数
        
        // 复杂度指标
        public int nestingDepth;      // 最大嵌套深度
        public int loopCount;         // 循环数量 (for, while, do)
        public int branchCount;       // 分支数量 (if, switch, case)
        public int tryCatchCount;     // try-catch 数量
        public int complexity;        // 综合复杂度评分
        
        @Override
        public String toString() {
            return String.format("%s.%s [行数:%d, 嵌套:%d, 循环:%d, 分支:%d, try-catch:%d, 复杂度:%d]",
                    className, methodName, lineCount, nestingDepth, loopCount, branchCount, tryCatchCount, complexity);
        }
    }

    /**
     * 分析方法复杂度
     */
    private void analyzeMethodComplexity(MethodInfo info) {
        String code = info.code;
        
        info.nestingDepth = calculateNestingDepth(code);
        info.loopCount = countPattern(code, "\\b(for|while|do)\\s*\\(");
        info.branchCount = countPattern(code, "\\b(if|switch|case)\\s*[\\(:]");
        info.tryCatchCount = countPattern(code, "\\b(try|catch)\\s*[\\{\\(]");
        
        // 综合复杂度评分
        info.complexity = info.nestingDepth * 3 
                        + info.loopCount * 2 
                        + info.branchCount * 2 
                        + info.tryCatchCount * 2
                        + (info.lineCount > 30 ? 2 : 0)
                        + (info.lineCount > 50 ? 3 : 0);
    }

    private int calculateNestingDepth(String code) {
        int maxDepth = 0, currentDepth = 0;
        boolean inString = false;
        char prevChar = 0;
        
        for (char c : code.toCharArray()) {
            if (c == '"' && prevChar != '\\') inString = !inString;
            if (!inString) {
                if (c == '{') { currentDepth++; maxDepth = Math.max(maxDepth, currentDepth); }
                else if (c == '}') currentDepth--;
            }
            prevChar = c;
        }
        return Math.max(0, maxDepth - 1);
    }

    private int countPattern(String code, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(code);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * 按方法分割并分析复杂度
     */
    public List<MethodInfo> splitByMethodsWithComplexity(String content, String className) {
        List<MethodInfo> methods = new ArrayList<>();
        if (content == null || content.isEmpty()) return methods;
        
        Pattern methodPattern = Pattern.compile(
            "((?:public|private|protected|static|final|native|synchronized|abstract|default)\\s+)*" +
            "(?:<[\\w\\s,<>\\[\\]?]+>\\s+)?" +
            "([\\w<>\\[\\],\\s\\?]+)\\s+" +
            "(\\w+)\\s*" +
            "(\\([^)]*\\))" +
            "(?:\\s*throws\\s+[\\w,\\s]+)?" +
            "\\s*\\{"
        );
        
        Matcher matcher = methodPattern.matcher(content);
        
        while (matcher.find()) {
            int start = matcher.start();
            int braceStart = matcher.end() - 1;
            int braceCount = 1;
            int end = braceStart + 1;
            
            while (end < content.length() && braceCount > 0) {
                char c = content.charAt(end);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                end++;
            }
            
            if (braceCount == 0) {
                MethodInfo info = new MethodInfo();
                info.className = className;
                info.methodName = matcher.group(3);
                info.signature = matcher.group(3) + matcher.group(4);
                info.code = content.substring(start, end).trim();
                info.startLine = content.substring(0, start).split("\n").length;
                info.endLine = content.substring(0, end).split("\n").length;
                info.lineCount = info.endLine - info.startLine + 1;
                analyzeMethodComplexity(info);
                methods.add(info);
            }
        }
        return methods;
    }

    /**
     * 分析目录复杂度并按包生成报告
     * @param directory 源代码目录
     * @param outputBaseDir 报告输出基础目录 (如 src/main/java/M)
     */
    public void generateComplexityReport(Path directory, Path outputBaseDir) throws IOException {
        // 按包分组的报告
        Map<String, JsonObject> packageReports = new LinkedHashMap<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            
            for (Path file : javaFiles) {
                String content = readFileContent(file);
                String className = file.getFileName().toString().replace(".java", "");
                
                // 获取相对包路径 (如 cli 或 cli/help)
                Path relativePath = directory.relativize(file.getParent());
                String packagePath = relativePath.toString().replace("\\", "/");
                
                List<MethodInfo> methods = splitByMethodsWithComplexity(content, className);
                
                if (!methods.isEmpty()) {
                    // 获取或创建该包的报告
                    JsonObject packageReport = packageReports.computeIfAbsent(packagePath, k -> new JsonObject());
                    
                    JsonObject classInfo = new JsonObject();
                    for (MethodInfo m : methods) {
                        JsonObject methodInfo = new JsonObject();
                        methodInfo.addProperty("complexity", m.complexity);
                        methodInfo.addProperty("lineCount", m.lineCount);
                        methodInfo.addProperty("nestingDepth", m.nestingDepth);
                        methodInfo.addProperty("loopCount", m.loopCount);
                        methodInfo.addProperty("branchCount", m.branchCount);
                        methodInfo.addProperty("tryCatchCount", m.tryCatchCount);
                        methodInfo.addProperty("strategy", getStrategy(m.complexity));
                        classInfo.add(m.methodName, methodInfo);
                    }
                    packageReport.add(className, classInfo);
                }
            }
        }
        
        // 为每个包生成独立的报告文件
        com.google.gson.Gson prettyGson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
        
        for (Map.Entry<String, JsonObject> entry : packageReports.entrySet()) {
            String packagePath = entry.getKey();
            JsonObject report = entry.getValue();
            
            // 生成报告文件名 (如 cli_report.json, help_report.json)
            String reportName = packagePath.isEmpty() ? "root_report.json" 
                    : packagePath.replace("/", "_").replace("\\", "_") + "_report.json";
            
            // 如果是子包，取最后一个包名
            if (packagePath.contains("/")) {
                String lastPkg = packagePath.substring(packagePath.lastIndexOf("/") + 1);
                reportName = lastPkg + "_report.json";
            } else if (!packagePath.isEmpty()) {
                reportName = packagePath + "_report.json";
            }
            
            // 输出路径: outputBaseDir/包路径/报告名
            Path outputPath = outputBaseDir.resolve(packagePath).resolve(reportName);
            Files.createDirectories(outputPath.getParent());
            
            String jsonOutput = prettyGson.toJson(report);
            writeFileContent(outputPath, jsonOutput);
            
            System.out.println("已生成报告: " + outputPath + " (" + report.size() + " 个类)");
        }
        
        System.out.println("\n复杂度报告生成完成，共 " + packageReports.size() + " 个包");
    }

    /**
     * 根据复杂度返回测试策略
     */
    private String getStrategy(int complexity) {
        if (complexity < 5) return "simple";
        if (complexity <= 10) return "standard";
        return "detailed";
    }

    // ==================== 代码分块（旧方法，已废弃） ====================

    /**
     * 代码分块 - 按方法分割
     * @param content Java源代码
     * @return 方法代码块列表
     * @deprecated 建议使用 splitByMethodsWithComplexity
     */
    @Deprecated
    public List<String> splitByMethods(String content) {
        List<String> methods = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return methods;
        }
        
        // 简单的方法提取（基于大括号匹配）
        Pattern methodPattern = Pattern.compile(
            "((?:public|private|protected|static|final|native|synchronized|abstract|transient)+\\s+)" +
            "(?:<[\\w\\s,<>\\[\\]]+>\\s+)?" +  // 泛型
            "[\\w<>\\[\\]]+\\s+" +              // 返回类型
            "(\\w+)\\s*" +                       // 方法名
            "\\([^)]*\\)" +                      // 参数
            "(?:\\s*throws\\s+[\\w,\\s]+)?" +   // throws
            "\\s*\\{"                            // 方法体开始
        );
        
        Matcher matcher = methodPattern.matcher(content);
        
        while (matcher.find()) {
            int start = matcher.start();
            int braceStart = matcher.end() - 1;
            int braceCount = 1;
            int end = braceStart + 1;
            
            // 匹配大括号
            while (end < content.length() && braceCount > 0) {
                char c = content.charAt(end);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                end++;
            }
            
            if (braceCount == 0) {
                methods.add(content.substring(start, end).trim());
            }
        }
        
        return methods;
    }

    /**
     * 代码分块 - 按类分割
     */
    public List<String> splitByClasses(String content) {
        List<String> classes = new ArrayList<>();
        if (content == null || content.isEmpty()) {
            return classes;
        }
        
        Pattern classPattern = Pattern.compile(
            "((?:public|private|protected|static|final|abstract)?\\s*)" +
            "(class|interface|enum)\\s+" +
            "(\\w+)" +
            "(?:<[^>]+>)?" +
            "(?:\\s+extends\\s+[\\w<>,\\s]+)?" +
            "(?:\\s+implements\\s+[\\w<>,\\s]+)?" +
            "\\s*\\{"
        );
        
        Matcher matcher = classPattern.matcher(content);
        
        while (matcher.find()) {
            int start = matcher.start();
            int braceStart = matcher.end() - 1;
            int braceCount = 1;
            int end = braceStart + 1;
            
            while (end < content.length() && braceCount > 0) {
                char c = content.charAt(end);
                if (c == '{') braceCount++;
                else if (c == '}') braceCount--;
                end++;
            }
            
            if (braceCount == 0) {
                classes.add(content.substring(start, end).trim());
            }
        }
        
        return classes;
    }

    /**
     * 处理目录下的所有Java文件（输出到新目录）
     * @param inputDir 输入目录
     * @param outputDir 输出目录
     */
    public void processDirectory(Path inputDir, Path outputDir) throws IOException {
        if (!Files.exists(inputDir)) {
            throw new IllegalArgumentException("输入目录不存在: " + inputDir);
        }
        
        Files.createDirectories(outputDir);
        resetDuplicateCache();
        
        try (Stream<Path> paths = Files.walk(inputDir)) {
            List<Path> javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());
            
            for (Path file : javaFiles) {
                processFile(file, inputDir, outputDir);
            }
        }
    }

    /**
     * 原地处理目录下的所有Java文件（直接覆盖原文件）
     * @param inputDir 输入目录
     */
    public void processDirectoryInPlace(Path inputDir) throws IOException {
        if (!Files.exists(inputDir)) {
            throw new IllegalArgumentException("输入目录不存在: " + inputDir);
        }
        
        resetDuplicateCache();
        
        try (Stream<Path> paths = Files.walk(inputDir)) {
            List<Path> javaFiles = paths
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());
            
            for (Path file : javaFiles) {
                processFileInPlace(file);
            }
        }
    }

    /**
     * 原地处理单个文件
     */
    private void processFileInPlace(Path file) throws IOException {
        String content = readFileContent(file);
        
        // 清洗内容
        String cleaned = cleanContent(content);
        
        // 只有内容变化才写入
        if (!cleaned.equals(content)) {
            writeFileContent(file, cleaned);
            System.out.println("已清洗: " + file);
        }
    }

    /**
     * 处理单个文件（输出到新目录）
     */
    private void processFile(Path file, Path inputDir, Path outputDir) throws IOException {
        String content = readFileContent(file);
        
        // 清洗内容
        String cleaned = cleanContent(content);
        
        // 检查是否有效
        if (!isValidCode(cleaned, 3)) {
            System.out.println("跳过无效文件: " + file);
            return;
        }
        
        // 检查是否重复
        if (isDuplicate(cleaned)) {
            System.out.println("跳过重复文件: " + file);
            return;
        }
        
        // 计算相对路径并写入输出目录
        Path relativePath = inputDir.relativize(file);
        Path outputFile = outputDir.resolve(relativePath);
        Files.createDirectories(outputFile.getParent());
        writeFileContent(outputFile, cleaned);
        
        System.out.println("已处理: " + file);
    }

    /**
     * 提取代码中的import语句
     */
    public List<String> extractImports(String content) {
        List<String> imports = new ArrayList<>();
        if (content == null) return imports;
        
        Pattern importPattern = Pattern.compile("^\\s*import\\s+([\\w.]+(?:\\.\\*)?);", Pattern.MULTILINE);
        Matcher matcher = importPattern.matcher(content);
        
        while (matcher.find()) {
            imports.add(matcher.group(1));
        }
        
        return imports;
    }

    /**
     * 提取包名
     */
    public String extractPackage(String content) {
        if (content == null) return null;
        
        Pattern packagePattern = Pattern.compile("^\\s*package\\s+([\\w.]+);", Pattern.MULTILINE);
        Matcher matcher = packagePattern.matcher(content);
        
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 去除License头部注释
     */
    public String removeLicenseHeader(String content) {
        if (content == null) return null;
        
        // 匹配文件开头的多行注释（通常是License）
        Pattern licensePattern = Pattern.compile("^\\s*/\\*[\\s\\S]*?\\*/\\s*", Pattern.MULTILINE);
        Matcher matcher = licensePattern.matcher(content);
        
        if (matcher.find() && matcher.start() == 0) {
            return content.substring(matcher.end());
        }
        
        return content;
    }

    // ==================== 依赖分析与自动导入 ====================

    // 常见包名到Maven坐标的映射
    private static final Map<String, String[]> KNOWN_DEPENDENCIES = new HashMap<>();
    
    static {
        // 格式: 包前缀 -> [groupId, artifactId, version]
        KNOWN_DEPENDENCIES.put("org.apache.commons.cli", new String[]{"commons-cli", "commons-cli", "1.6.0"});
        KNOWN_DEPENDENCIES.put("org.apache.commons.lang3", new String[]{"org.apache.commons", "commons-lang3", "3.14.0"});
        KNOWN_DEPENDENCIES.put("org.apache.commons.io", new String[]{"commons-io", "commons-io", "2.15.1"});
        KNOWN_DEPENDENCIES.put("org.apache.commons.collections4", new String[]{"org.apache.commons", "commons-collections4", "4.4"});
        KNOWN_DEPENDENCIES.put("org.apache.commons.text", new String[]{"org.apache.commons", "commons-text", "1.11.0"});
        KNOWN_DEPENDENCIES.put("com.google.guava", new String[]{"com.google.guava", "guava", "33.0.0-jre"});
        KNOWN_DEPENDENCIES.put("org.slf4j", new String[]{"org.slf4j", "slf4j-api", "2.0.9"});
        KNOWN_DEPENDENCIES.put("org.junit.jupiter", new String[]{"org.junit.jupiter", "junit-jupiter", "5.10.1"});
        KNOWN_DEPENDENCIES.put("org.mockito", new String[]{"org.mockito", "mockito-core", "5.8.0"});
        KNOWN_DEPENDENCIES.put("com.fasterxml.jackson", new String[]{"com.fasterxml.jackson.core", "jackson-databind", "2.16.1"});
        KNOWN_DEPENDENCIES.put("org.apache.logging.log4j", new String[]{"org.apache.logging.log4j", "log4j-core", "2.22.1"});
    }

    private final OkHttpClient httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build();
    
    private final Gson gson = new Gson();

    /**
     * 扫描目录下所有Java文件的import语句
     * @param directory 目录路径
     * @return 所有外部import语句集合
     */
    public Set<String> scanAllImports(Path directory) throws IOException {
        Set<String> allImports = new TreeSet<>();
        
        try (Stream<Path> paths = Files.walk(directory)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            
            for (Path file : javaFiles) {
                String content = readFileContent(file);
                List<String> imports = extractAllImports(content);
                allImports.addAll(imports);
            }
        }
        
        return allImports;
    }

    /**
     * 提取所有import语句（包括static import）
     */
    public List<String> extractAllImports(String content) {
        List<String> imports = new ArrayList<>();
        if (content == null) return imports;
        
        // 匹配普通import和static import
        Pattern importPattern = Pattern.compile(
                "^\\s*import\\s+(static\\s+)?([\\w.]+(?:\\.\\*)?);", 
                Pattern.MULTILINE
        );
        Matcher matcher = importPattern.matcher(content);
        
        while (matcher.find()) {
            String importStmt = matcher.group(2);
            // 对于static import，提取类的完整路径
            if (matcher.group(1) != null) {
                // static import: 去掉最后的方法/字段名
                int lastDot = importStmt.lastIndexOf('.');
                if (lastDot > 0) {
                    String className = importStmt.substring(0, lastDot);
                    // 再去掉一层获取包名
                    int classNameDot = className.lastIndexOf('.');
                    if (classNameDot > 0) {
                        imports.add(className.substring(0, classNameDot));
                    }
                }
            } else {
                // 普通import
                int lastDot = importStmt.lastIndexOf('.');
                if (lastDot > 0) {
                    imports.add(importStmt.substring(0, lastDot));
                }
            }
        }
        
        return imports;
    }

    /**
     * 过滤出外部依赖（排除项目内部包和JDK包）
     * @param imports import语句集合
     * @param projectPackages 项目内部包前缀列表
     * @return 外部依赖包集合
     */
    public Set<String> filterExternalDependencies(Set<String> imports, List<String> projectPackages) {
        Set<String> external = new TreeSet<>();
        
        for (String imp : imports) {
            // 跳过JDK内置包
            if (imp.startsWith("java.") || imp.startsWith("javax.") || imp.startsWith("sun.")) {
                continue;
            }
            
            // 跳过项目内部包
            boolean isInternal = false;
            for (String projectPkg : projectPackages) {
                if (imp.startsWith(projectPkg)) {
                    isInternal = true;
                    break;
                }
            }
            
            if (!isInternal) {
                external.add(imp);
            }
        }
        
        return external;
    }

    /**
     * 从Maven Central搜索依赖坐标
     * @param packageName 包名
     * @return Maven依赖信息 [groupId, artifactId, version]，未找到返回null
     */
    public String[] searchMavenDependency(String packageName) {
        // 先检查已知映射
        for (Map.Entry<String, String[]> entry : KNOWN_DEPENDENCIES.entrySet()) {
            if (packageName.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        
        // 通过Maven Central API搜索
        try {
            String searchUrl = "https://search.maven.org/solrsearch/select?q=fc:" + 
                    packageName.replace(".", "/") + "&rows=1&wt=json";
            
            Request request = new Request.Builder()
                    .url(searchUrl)
                    .build();
            
            try (Response response = httpClient.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String json = response.body().string();
                    JsonObject root = gson.fromJson(json, JsonObject.class);
                    JsonObject responseObj = root.getAsJsonObject("response");
                    
                    if (responseObj != null && responseObj.get("numFound").getAsInt() > 0) {
                        JsonArray docs = responseObj.getAsJsonArray("docs");
                        if (docs.size() > 0) {
                            JsonObject doc = docs.get(0).getAsJsonObject();
                            String groupId = doc.get("g").getAsString();
                            String artifactId = doc.get("a").getAsString();
                            String version = doc.get("latestVersion").getAsString();
                            return new String[]{groupId, artifactId, version};
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("搜索Maven依赖失败: " + packageName + " - " + e.getMessage());
        }
        
        return null;
    }

    /**
     * 分析目录并生成需要添加的Maven依赖
     * @param directory 源代码目录
     * @param projectPackages 项目内部包前缀
     * @return 依赖列表，每个元素为 [groupId, artifactId, version]
     */
    public List<String[]> analyzeDependencies(Path directory, List<String> projectPackages) throws IOException {
        Set<String> allImports = scanAllImports(directory);
        Set<String> externalDeps = filterExternalDependencies(allImports, projectPackages);
        
        Map<String, String[]> uniqueDeps = new LinkedHashMap<>();
        
        for (String pkg : externalDeps) {
            String[] dep = searchMavenDependency(pkg);
            if (dep != null) {
                String key = dep[0] + ":" + dep[1];
                if (!uniqueDeps.containsKey(key)) {
                    uniqueDeps.put(key, dep);
                    System.out.println("找到依赖: " + key + ":" + dep[2]);
                }
            } else {
                System.out.println("未找到依赖: " + pkg);
            }
        }
        
        return new ArrayList<>(uniqueDeps.values());
    }

    /**
     * 生成Maven依赖XML片段
     * @param dependencies 依赖列表
     * @return XML字符串
     */
    public String generateDependencyXml(List<String[]> dependencies) {
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- 自动生成的依赖 -->\n");
        
        for (String[] dep : dependencies) {
            sb.append("<dependency>\n");
            sb.append("    <groupId>").append(dep[0]).append("</groupId>\n");
            sb.append("    <artifactId>").append(dep[1]).append("</artifactId>\n");
            sb.append("    <version>").append(dep[2]).append("</version>\n");
            sb.append("</dependency>\n");
        }
        
        return sb.toString();
    }

    /**
     * 自动更新pom.xml添加依赖
     * @param pomPath pom.xml路径
     * @param dependencies 依赖列表
     */
    public void updatePomXml(Path pomPath, List<String[]> dependencies) throws IOException {
        if (!Files.exists(pomPath)) {
            throw new IllegalArgumentException("pom.xml不存在: " + pomPath);
        }
        
        String pomContent = readFileContent(pomPath);
        
        // 读取现有依赖，避免重复添加
        Set<String> existingDeps = new HashSet<>();
        Pattern depPattern = Pattern.compile(
                "<groupId>([^<]+)</groupId>\\s*<artifactId>([^<]+)</artifactId>",
                Pattern.DOTALL
        );
        Matcher matcher = depPattern.matcher(pomContent);
        while (matcher.find()) {
            existingDeps.add(matcher.group(1) + ":" + matcher.group(2));
        }
        
        // 过滤出需要新增的依赖
        List<String[]> newDeps = dependencies.stream()
                .filter(dep -> !existingDeps.contains(dep[0] + ":" + dep[1]))
                .collect(Collectors.toList());
        
        if (newDeps.isEmpty()) {
            System.out.println("没有需要新增的依赖");
            return;
        }
        
        // 生成新依赖XML
        StringBuilder newDepsXml = new StringBuilder();
        for (String[] dep : newDeps) {
            newDepsXml.append("        <dependency>\n");
            newDepsXml.append("            <groupId>").append(dep[0]).append("</groupId>\n");
            newDepsXml.append("            <artifactId>").append(dep[1]).append("</artifactId>\n");
            newDepsXml.append("            <version>").append(dep[2]).append("</version>\n");
            newDepsXml.append("        </dependency>\n");
        }
        
        // 在</dependencies>前插入新依赖
        String updatedPom = pomContent.replace(
                "</dependencies>",
                newDepsXml.toString() + "    </dependencies>"
        );
        
        // 备份原文件
        Path backupPath = pomPath.resolveSibling("pom.xml.backup");
        Files.copy(pomPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
        
        // 写入更新后的pom.xml
        writeFileContent(pomPath, updatedPom);
        
        System.out.println("已添加 " + newDeps.size() + " 个新依赖到pom.xml");
        System.out.println("原文件已备份到: " + backupPath);
    }

    /**
     * 一键分析并导入依赖
     * @param sourceDir 源代码目录
     * @param pomPath pom.xml路径
     * @param projectPackages 项目内部包前缀
     */
    public void analyzeAndImportDependencies(Path sourceDir, Path pomPath, List<String> projectPackages) 
            throws IOException {
        System.out.println("开始分析目录: " + sourceDir);
        
        // 分析依赖
        List<String[]> dependencies = analyzeDependencies(sourceDir, projectPackages);
        
        if (dependencies.isEmpty()) {
            System.out.println("未发现需要导入的外部依赖");
            return;
        }
        
        System.out.println("\n发现 " + dependencies.size() + " 个外部依赖:");
        System.out.println(generateDependencyXml(dependencies));
        
        // 更新pom.xml
        updatePomXml(pomPath, dependencies);
    }

    // ==================== 独立清洗方法（不依赖编译） ====================
    
    /**
     * 独立清洗方法 - 直接处理指定目录的Java文件
     * 即使目录中的代码有语法错误也能正常工作
     * 
     * 使用方法：
     * java -cp "lib/*;target/classes" zju.cst.aces.runner.Cleaning cleanOnly src/main/java/csv
     * 
     * @param targetDir 要清洗的目录路径
     */
    public static void cleanOnly(String targetDir) {
        System.out.println("=== 独立清洗模式 ===");
        System.out.println("目标目录: " + targetDir + "\n");
        
        Path dir = Paths.get(targetDir);
        
        if (!Files.exists(dir)) {
            System.err.println("错误: 目录不存在: " + dir);
            return;
        }
        
        Cleaning cleaning = new Cleaning();
        int processedCount = 0;
        int modifiedCount = 0;
        
        try (Stream<Path> paths = Files.walk(dir)) {
            List<Path> javaFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .collect(Collectors.toList());
            
            System.out.println("找到 " + javaFiles.size() + " 个Java文件\n");
            
            for (Path file : javaFiles) {
                try {
                    System.out.print("处理: " + file.getFileName() + " ... ");
                    
                    String content = readFileContent(file);
                    String original = content;
                    
                    // 执行清洗
                    String cleaned = cleaning.cleanContent(content);
                    
                    // 只有内容变化才写入
                    if (!cleaned.equals(original)) {
                        writeFileContent(file, cleaned);
                        System.out.println("✓ 已清洗");
                        modifiedCount++;
                    } else {
                        System.out.println("- 无需修改");
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    System.out.println("✗ 失败: " + e.getMessage());
                }
            }
            
            System.out.println("\n=== 清洗完成 ===");
            System.out.println("处理文件数: " + processedCount);
            System.out.println("修改文件数: " + modifiedCount);
            
        } catch (IOException e) {
            System.err.println("遍历目录失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ==================== 主方法示例 ====================
    
    public static void main(String[] args) {
        // 支持命令行参数调用独立清洗方法
        if (args.length > 0 && "cleanOnly".equals(args[0])) {
            if (args.length < 2) {
                System.err.println("用法: java Cleaning cleanOnly <目录路径>");
                System.err.println("示例: java Cleaning cleanOnly src/main/java/lang3");
                return;
            }
            cleanOnly(args[1]);
            return;
        }
        Cleaning cleaning = new Cleaning();
        
        // 处理多个目录
        List<Path> inputDirs = Arrays.asList(
              //  Paths.get("src/main/java/help"),
                Paths.get("src/main/java/lang3")
        );
        
        // 1. 原地清洗所有目录
        for (Path inputDir : inputDirs) {
            if (Files.exists(inputDir)) {
                try {
                    System.out.println("开始清洗目录: " + inputDir);
                    cleaning.processDirectoryInPlace(inputDir);
                    System.out.println("清洗处理完成: " + inputDir + "\n");
                } catch (IOException e) {
                    System.err.println("清洗处理失败 " + inputDir + ": " + e.getMessage());
                }
            } else {
                System.out.println("目录不存在，跳过: " + inputDir);
            }
        }
        
        // 2. 生成复杂度报告到 src/main/java/M/ 目录（按包结构）
        Path reportBaseDir = Paths.get("src/main/java/M");
        for (Path inputDir : inputDirs) {
            if (Files.exists(inputDir)) {
                try {
                    System.out.println("生成复杂度报告: " + inputDir);
                    cleaning.generateComplexityReport(inputDir, reportBaseDir);
                    System.out.println("复杂度报告生成完成: " + inputDir + "\n");
                } catch (IOException e) {
                    System.err.println("复杂度报告生成失败 " + inputDir + ": " + e.getMessage());
                }
            }
        }
        
        // 3. 分析并导入依赖
        Path pomPath = Paths.get("pom.xml");
        List<String> projectPackages = Arrays.asList(
             /*   "zju.cst.aces",
                "TryCommons",*/
                "lang3"
        );
        
        for (Path inputDir : inputDirs) {
            if (Files.exists(inputDir)) {
                try {
                    System.out.println("分析依赖: " + inputDir);
                    cleaning.analyzeAndImportDependencies(inputDir, pomPath, projectPackages);
                } catch (IOException e) {
                    System.err.println("依赖导入失败 " + inputDir + ": " + e.getMessage());
                }
            }
        }
        
        System.out.println("\n所有处理完成！请执行 mvn install 安装依赖");
    }
}
