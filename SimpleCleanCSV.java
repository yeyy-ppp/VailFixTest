import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

/**
 * 简单的
 * 包清洗工具
 * 完全独立，不依赖任何其他类
 * 
 * 编译: javac -encoding UTF-8 SimpleCleanCSV.java
 * 运行: java SimpleCleanCSV
 */
public class SimpleCleanCSV {

    public static void main(String[] args) {
        System.out.println("=== 清洗 csv 包中的 Java 文件 ===\n");
        
        String targetDir = "src/main/java/lang3/text";
        if (args.length > 0) {
            targetDir = args[0];
        }
        
        Path dir = Paths.get(targetDir);
        
        if (!Files.exists(dir)) {
            System.err.println("错误: 目录不存在: " + dir);
            return;
        }
        
        int processedCount = 0;
        int modifiedCount = 0;
        Set<String> missingImports = new TreeSet<>();
        
        try {
            File[] files = dir.toFile().listFiles((d, name) -> name.endsWith(".java"));
            
            if (files == null || files.length == 0) {
                System.out.println("未找到Java文件");
                return;
            }
            
            System.out.println("找到 " + files.length + " 个Java文件\n");
            
            for (File file : files) {
                try {
                    System.out.print("处理: " + file.getName() + " ... ");
                    
                    String content = readFile(file.toPath());
                    String original = content;
                    
                    // 1. 替换包路径
                    content = replacePackagePath(content);
                    
                    // 2. 清洗步骤
                    content = removeMultiLineComments(content);
                    content = removeSingleLineComments(content);
                    content = removeBlankLines(content);
                    content = normalizeWhitespace(content);
                    
                    // 3. 检查缺失的导包
                    Set<String> missing = checkMissingImports(content);
                    missingImports.addAll(missing);
                    
                    if (!content.equals(original)) {
                        writeFile(file.toPath(), content);
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
            
            if (!missingImports.isEmpty()) {
                System.out.println("\n=== 可能缺失的导包 ===");
                for (String imp : missingImports) {
                    System.out.println("  - " + imp);
                }
            }
            
        } catch (Exception e) {
            System.err.println("处理失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 替换包路径: org.apache.commons.csv -> csv
     */
    private static String replacePackagePath(String content) {
        // 替换包声明
        content = content.replaceAll("package\\s+org\\.apache\\.commons\\.lang3\\s*;", "package lang3;");
        
        // 替换import语句
        content = content.replaceAll("import\\s+org\\.apache\\.commons\\.lang3\\.", "import lang3.");
        content = content.replaceAll("import\\s+static\\s+org\\.apache\\.commons\\.lang3\\.", "import static lang3.");
        
        // 替换代码中的完整类名引用
        content = content.replaceAll("org\\.apache\\.commons\\.lang3\\.", "lang3.");
        
        return content;
    }
    
    /**
     * 检查可能缺失的导包
     */
    private static Set<String> checkMissingImports(String content) {
        Set<String> missing = new TreeSet<>();
        
        // 检查是否使用了commons-io的类但没有导入
        if (content.contains("IOUtils") && !content.contains("import") && !content.contains("java.io")) {
            missing.add("org.apache.commons.io.IOUtils");
        }
        if (content.contains("UnsynchronizedBufferedReader")) {
            missing.add("org.apache.commons.io.input.UnsynchronizedBufferedReader (建议改用 java.io.BufferedReader)");
        }
        if (content.contains("AppendableOutputStream")) {
            missing.add("org.apache.commons.io.output.AppendableOutputStream");
        }
        if (content.contains("IOStream")) {
            missing.add("org.apache.commons.io.function.IOStream (建议改用 Stream)");
        }
        if (content.contains("Charsets.toCharset")) {
            missing.add("org.apache.commons.io.Charsets (建议改用 Charset.forName)");
        }
        if (content.contains("AbstractStreamBuilder")) {
            missing.add("org.apache.commons.io.build.AbstractStreamBuilder (建议简化Builder)");
        }
        if (content.contains("Uncheck.get")) {
            missing.add("org.apache.commons.io.function.Uncheck (建议改用 try-catch)");
        }
        
        return missing;
    }
    
    private static String readFile(Path path) throws IOException {
        byte[] bytes = Files.readAllBytes(path);
        return new String(bytes, StandardCharsets.UTF_8);
    }
    
    private static void writeFile(Path path, String content) throws IOException {
        Files.write(path, content.getBytes(StandardCharsets.UTF_8));
    }
    
    private static String removeMultiLineComments(String content) {
        return content.replaceAll("/\\*[\\s\\S]*?\\*/", "");
    }
    
    private static String removeSingleLineComments(String content) {
        StringBuilder result = new StringBuilder();
        String[] lines = content.split("\n", -1);
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            StringBuilder cleanLine = new StringBuilder();
            boolean inString = false;
            boolean inChar = false;
            char prevChar = 0;
            
            for (int j = 0; j < line.length(); j++) {
                char c = line.charAt(j);
                
                if (c == '"' && prevChar != '\\' && !inChar) {
                    inString = !inString;
                }
                if (c == '\'' && prevChar != '\\' && !inString) {
                    inChar = !inChar;
                }
                
                if (!inString && !inChar && c == '/' && j + 1 < line.length() && line.charAt(j + 1) == '/') {
                    break;
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
    
    private static String removeBlankLines(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                result.append(line).append("\n");
            }
        }
        
        if (result.length() > 0 && result.charAt(result.length() - 1) == '\n') {
            result.setLength(result.length() - 1);
        }
        
        return result.toString();
    }
    
    private static String normalizeWhitespace(String content) {
        String[] lines = content.split("\n", -1);
        StringBuilder result = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replaceAll("\\s+$", "");
            result.append(line);
            if (i < lines.length - 1) {
                result.append("\n");
            }
        }
        
        return result.toString();
    }
}
