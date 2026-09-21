package zju.cst.aces.runner;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AnalyzeResultStorage {
    // 基础存储目录（target下）
    private static final String BASE_STORAGE_DIR = "target";
    // 已处理的类集合（避免循环依赖）
    private Set<String> processedClasses = new HashSet<>();
    // 当前分析的根目录（如cli）
    private String currentRootDir;
    // 测试核心类实例（用于调用LLM分析）
    private TestCoreDemo2 testCoreClass;

    public AnalyzeResultStorage(String rootDir, TestCoreDemo2 testCore) {
        this.currentRootDir = rootDir;
        this.testCoreClass = testCore;
    }

    /**
     * 启动分析并存储指定类及其依赖
     * @param className 初始类名（如AlreadySelectedException）
     * @param sourceCodeDir 源代码目录
     * @throws IOException IO异常
     */
    public void startAnalyzeAndStore(String className, String sourceCodeDir) throws IOException {
        // 构建根存储目录（target/analyzeStart-test-cli）
        String rootStorageDir = Paths.get(BASE_STORAGE_DIR, "analyzeStart-test-" + currentRootDir).toString();
        File rootDir = new File(rootStorageDir);
        if (!rootDir.exists()) {
            rootDir.mkdirs();
        }

        // 递归分析类及其依赖
        analyzeAndStoreRecursive(className, sourceCodeDir, rootStorageDir);
    }

    // 辅助方法：拆分源代码为多个块（按方法或固定长度）
    private List<String> splitSourceCode(String sourceCode) {
        List<String> chunks = new ArrayList<>();
        int maxChunkLength = 5000; // 每块最大字符数（根据LLM输入限制调整）
        int length = sourceCode.length();

        for (int i = 0; i < length; i += maxChunkLength) {
            int end = Math.min(i + maxChunkLength, length);
            // 尽量按方法边界拆分（简单处理：找最近的"}"作为分割点）
            if (end < length) {
                int lastBrace = sourceCode.lastIndexOf("}", end);
                if (lastBrace > i) {
                    end = lastBrace + 1; // 确保方法完整
                }
            }
            chunks.add(sourceCode.substring(i, end));
        }
        return chunks;
    }

    /**
     * 递归分析类及其依赖/继承类并存储结果
     */
    private void analyzeAndStoreRecursive(String className, String sourceCodeDir, String rootStorageDir) throws IOException {
        // 避免重复处理
        if (processedClasses.contains(className)) {
            return;
        }
        processedClasses.add(className);

        // 1. 检查当前类是否存在于源代码目录
        String classFilePath = findClassInDir(className, sourceCodeDir);
        if (classFilePath == null) {
            System.out.println("类[" + className + "]不在当前目录[" + sourceCodeDir + "]下，跳过包依赖处理");
            return;
        }

        // 2. 检查是否已存在分析结果，存在则跳过分析（但仍会用于LLM）
        String resultFilePath = Paths.get(rootStorageDir, className + ".analyze").toString();
        File resultFile = new File(resultFilePath);
        if (resultFile.exists()) {
            System.out.println("类[" + className + "]分析结果已存在，跳过分析");
        } else {
            // 3. 调用LLM分析当前类
            System.out.println("开始分析类[" + className + "]...");
            String sourceCode = testCoreClass.readSourceCode(classFilePath);


            // 拆分长代码（如按方法拆分，每块不超过5000字符）
            List<String> codeChunks = splitSourceCode(sourceCode);
            StringBuilder fullAnalyzeResult = new StringBuilder();

            for (String chunk : codeChunks) {
                String chunkResult = testCoreClass.analyzeSourceCode(chunk); // 调用LLM分析单块
                if (chunkResult != null) {
                    fullAnalyzeResult.append(chunkResult).append("\n"); // 合并结果
                }
            }

            String analyzeResult = fullAnalyzeResult.toString();

           // String analyzeResult = testCoreClass.analyzeSourceCode(sourceCode);
            // 处理分析结果：删除```和```over标识符
            analyzeResult = analyzeResult.replace("```", "").replace("```over", "");
            // 删除空白行（匹配空行或仅含空白字符的行）
            analyzeResult = analyzeResult.replaceAll("(?m)^\\s*$\\n?", "");
            if (analyzeResult == null || analyzeResult.isEmpty()) {
                System.out.println("类[" + className + "]分析失败，LLM未返回有效结果");
                return;
            }

            // 4. 存储分析结果到target目录
            Files.write(Paths.get(resultFilePath), analyzeResult.getBytes());
            System.out.println("类[" + className + "]分析结果已存储至：" + resultFilePath);
        }

        // 5. 提取当前类的依赖类和继承类（通过LLM分析结果）
        String analyzeContent = new String(Files.readAllBytes(Paths.get(resultFilePath)), StandardCharsets.UTF_8);
        ClassDependencyInfo dependencyInfo = extractDependencyInfo(analyzeContent, className);

        // 6. 递归处理依赖类（如OptionGroup、Option）
        for (String dependency : dependencyInfo.getDependencies()) {
            System.out.println("处理类[" + className + "]的依赖类：" + dependency);
            analyzeAndStoreRecursive(dependency, sourceCodeDir, rootStorageDir);
        }

        // 7. 处理继承类（如extends ParseException）
        if (dependencyInfo.getSuperClass() != null && !dependencyInfo.getSuperClass().isEmpty()) {
            String superClass = dependencyInfo.getSuperClass();
            System.out.println("处理类[" + className + "]的继承类：" + superClass);
            analyzeAndStoreRecursive(superClass, sourceCodeDir, rootStorageDir);
        }
    }

    /**
     * 在指定目录中查找类文件
     * @param className 类名
     * @param dir 目录路径
     * @return 类文件路径，不存在则返回null
     */
    private String findClassInDir(String className, String dir) {
        File directory = new File(dir);
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles((d, name) -> name.equals(className + ".java"));
        if (files != null && files.length > 0) {
            return files[0].getAbsolutePath();
        }
        return null;
    }


    /**
     * 从LLM分析结果中提取依赖信息（简化实现，实际需根据LLM输出格式解析）
     * @param analyzeContent LLM分析结果
     * @param className 类名
     * @return 依赖信息对象
     */
    private ClassDependencyInfo extractDependencyInfo(String analyzeContent, String className) {
        ClassDependencyInfo info = new ClassDependencyInfo();
        List<String> dependencies = new ArrayList<>();
        String superClass = "";

        // 1. 提取父类（匹配"父类：XXX"格式）
        String[] superLines = analyzeContent.split("父类：");
        if (superLines.length > 1) {
            superClass = superLines[1].trim().split("\n")[0]; // 取冒号后第一行内容
            superClass = superClass.replaceAll("[^a-zA-Z0-9]", ""); // 过滤非字母数字字符
        }

        // 2. 提取依赖类（匹配"依赖类：XXX,XXX"格式）
        String[] depLines = analyzeContent.split("依赖类：");
        if (depLines.length > 1) {
            String depPart = depLines[1].trim().split("\n")[0]; // 取冒号后第一行内容
            String[] depClasses = depPart.split(",");
            for (String dep : depClasses) {
                dep = dep.trim().replaceAll("[^a-zA-Z0-9]", ""); // 过滤特殊字符
                if (!dep.isEmpty()) {
                    dependencies.add(dep);
                }
            }
        }

        info.setDependencies(dependencies);
        info.setSuperClass(superClass);
        return info;
    }

    /**
     * 依赖信息内部类
     */
    private static class ClassDependencyInfo {
        private List<String> dependencies; // 依赖类列表
        private String superClass; // 父类

        public List<String> getDependencies() {
            return dependencies;
        }

        public void setDependencies(List<String> dependencies) {
            this.dependencies = dependencies;
        }

        public String getSuperClass() {
            return superClass;
        }

        public void setSuperClass(String superClass) {
            this.superClass = superClass;
        }
    }

    // 测试调用示例
    public static void main(String[] args) throws IOException {
        // 初始化参数
        String apiKey = "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";
        String sourceCodeDir = "src/main/java/cli"; // 目标目录（如cli）
        String rootDir = "cli"; // 当前分析的目录名
        String targetClass = "BasicParser"; // 初始分析类

        // 初始化测试核心类
        TestCoreDemo2 testCore = new TestCoreDemo2(apiKey);
        // 创建存储处理器
        AnalyzeResultStorage storage = new AnalyzeResultStorage(rootDir, testCore);
        // 启动分析和存储
        storage.startAnalyzeAndStore(targetClass, sourceCodeDir);
    }
}

