package zju.cst.aces.runner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 基于复杂度报告的测试运行器
 * 
 * 职责：
 * 1. 读取复杂度报告
 * 2. 判断是否需要切分
 * 3. 如需切分，生成切片信息并保存到 src/H/slices/
 * 4. 提供查询接口供 ValiFixTextCoreDemo3 调用
 * 
 * 使用方式（在 ValiFixTextCoreDemo3 中调用）：
 *   ReportBasedTestRunner runner = new ReportBasedTestRunner();
 *   runner.loadReport("src/main/java/M/cli/cli_report.json");
 *   
 *   if (runner.needsSlice("DefaultParser")) {
 *       List<SliceInfo> slices = runner.getSlicesForClass("DefaultParser");
 *       // 为每个切片生成测试类
 *   } else {
 *       // 普通流程，生成单个测试类
 *   }
 */
public class ReportBasedTestRunner {

    private final Gson gson = new Gson();
    private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();

    // 策略常量
    public static final String STRATEGY_SIMPLE = "simple";
    public static final String STRATEGY_STANDARD = "standard";
    public static final String STRATEGY_DETAILED = "detailed";

    // 切片阈值：复杂度 >= 此值时使用切片策略
    public static final int SLICE_THRESHOLD = 5;

    // 切片信息存储目录
    public static final String SLICE_DIR = "src/H/slices";

    // 已加载的类复杂度信息
    private Map<String, ClassComplexityInfo> loadedClasses = new LinkedHashMap<>();

    // ==================== 数据结构 ====================

    /**
     * 方法复杂度信息
     */
    public static class MethodComplexityInfo {
        public String className;
        public String methodName;
        public int complexity;
        public int lineCount;
        public int nestingDepth;
        public int loopCount;
        public int branchCount;
        public int tryCatchCount;
        public String strategy;

        /** 是否需要切片（复杂度 >= 阈值） */
        public boolean needsSlice() {
            return complexity >= SLICE_THRESHOLD;
        }

        @Override
        public String toString() {
            return String.format("%s.%s [complexity=%d, strategy=%s]",
                    className, methodName, complexity, strategy);
        }
    }

    /**
     * 类复杂度信息
     */
    public static class ClassComplexityInfo {
        public String className;
        public String packagePath;
        public List<MethodComplexityInfo> methods = new ArrayList<>();
        public String overallStrategy;

        /** 是否包含需要切片的方法（复杂度 >= 阈值） */
        public boolean hasMethodsNeedingSlice() {
            return methods.stream().anyMatch(m -> m.complexity >= SLICE_THRESHOLD);
        }

        /** 获取需要切片的方法（复杂度 >= 阈值） */
        public List<MethodComplexityInfo> getMethodsNeedingSlice() {
            return methods.stream()
                    .filter(m -> m.complexity >= SLICE_THRESHOLD)
                    .collect(Collectors.toList());
        }

        /** 获取不需要切片的方法（复杂度 < 阈值） */
        public List<MethodComplexityInfo> getMethodsNotNeedingSlice() {
            return methods.stream()
                    .filter(m -> m.complexity < SLICE_THRESHOLD)
                    .collect(Collectors.toList());
        }

        /** 是否包含 detailed 方法（保留原方法兼容） */
        public boolean hasDetailedMethods() {
            return methods.stream().anyMatch(m -> STRATEGY_DETAILED.equals(m.strategy));
        }

        /** 获取所有 detailed 方法（保留原方法兼容） */
        public List<MethodComplexityInfo> getDetailedMethods() {
            return methods.stream()
                    .filter(m -> STRATEGY_DETAILED.equals(m.strategy))
                    .collect(Collectors.toList());
        }

        /** 获取所有非 detailed 方法（保留原方法兼容） */
        public List<MethodComplexityInfo> getNonDetailedMethods() {
            return methods.stream()
                    .filter(m -> !STRATEGY_DETAILED.equals(m.strategy))
                    .collect(Collectors.toList());
        }
    }

    /**
     * 切片信息 - 描述一个测试切片
     */
    public static class SliceInfo {
        public int sliceIndex;                    // 切片序号
        public String sliceName;                  // 切片名称（用于生成测试类名后缀）
        public String testClassName;              // 完整测试类名
        public List<String> targetMethods;        // 该切片要测试的方法列表
        public String focusDescription;           // 测试重点描述（可作为提示词补充）

        public SliceInfo(int sliceIndex, String sliceName) {
            this.sliceIndex = sliceIndex;
            this.sliceName = sliceName;
            this.targetMethods = new ArrayList<>();
        }
    }

    // ==================== 报告读取 ====================

    /**
     * 加载单个报告文件
     * @param reportPath 报告文件路径
     * @return 加载的类数量
     */
    public int loadReport(String reportPath) throws IOException {
        return loadReport(Paths.get(reportPath));
    }

    /**
     * 加载单个报告文件
     */
    public int loadReport(Path reportPath) throws IOException {
        if (!Files.exists(reportPath)) {
            System.err.println("报告文件不存在: " + reportPath);
            return 0;
        }

        String content = new String(Files.readAllBytes(reportPath), StandardCharsets.UTF_8);
        JsonObject root = gson.fromJson(content, JsonObject.class);

        int count = 0;
        for (String className : root.keySet()) {
            ClassComplexityInfo classInfo = parseClassInfo(className, root.getAsJsonObject(className));
            loadedClasses.put(className, classInfo);
            count++;
        }

        System.out.println("已加载报告: " + reportPath + " (" + count + " 个类)");
        return count;
    }

    /**
     * 加载目录下所有报告文件
     * @param reportBaseDir 报告目录
     * @return 加载的类总数
     */
    public int loadAllReports(String reportBaseDir) throws IOException {
        return loadAllReports(Paths.get(reportBaseDir));
    }

    /**
     * 加载目录下所有报告文件
     */
    public int loadAllReports(Path reportBaseDir) throws IOException {
        int totalCount = 0;

        try (Stream<Path> paths = Files.walk(reportBaseDir)) {
            List<Path> reportFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith("_report.json"))
                    .collect(Collectors.toList());

            for (Path reportFile : reportFiles) {
                int count = loadReport(reportFile);

                // 设置包路径
                Path relativePath = reportBaseDir.relativize(reportFile.getParent());
                String packagePath = relativePath.toString().replace("\\", "/");
                
                // 更新刚加载的类的包路径
                String content = new String(Files.readAllBytes(reportFile), StandardCharsets.UTF_8);
                JsonObject root = gson.fromJson(content, JsonObject.class);
                for (String className : root.keySet()) {
                    if (loadedClasses.containsKey(className)) {
                        loadedClasses.get(className).packagePath = packagePath;
                    }
                }

                totalCount += count;
            }
        }

        System.out.println("共加载 " + totalCount + " 个类的复杂度信息");
        return totalCount;
    }

    /**
     * 解析单个类的复杂度信息
     */
    private ClassComplexityInfo parseClassInfo(String className, JsonObject classObj) {
        ClassComplexityInfo classInfo = new ClassComplexityInfo();
        classInfo.className = className;

        int maxComplexity = 0;

        for (String methodName : classObj.keySet()) {
            JsonObject methodObj = classObj.getAsJsonObject(methodName);

            MethodComplexityInfo methodInfo = new MethodComplexityInfo();
            methodInfo.className = className;
            methodInfo.methodName = methodName;
            methodInfo.complexity = methodObj.get("complexity").getAsInt();
            methodInfo.lineCount = methodObj.get("lineCount").getAsInt();
            methodInfo.nestingDepth = methodObj.get("nestingDepth").getAsInt();
            methodInfo.loopCount = methodObj.get("loopCount").getAsInt();
            methodInfo.branchCount = methodObj.get("branchCount").getAsInt();
            methodInfo.tryCatchCount = methodObj.get("tryCatchCount").getAsInt();
            methodInfo.strategy = methodObj.get("strategy").getAsString();

            classInfo.methods.add(methodInfo);

            if (methodInfo.complexity > maxComplexity) {
                maxComplexity = methodInfo.complexity;
                classInfo.overallStrategy = methodInfo.strategy;
            }
        }

        return classInfo;
    }

    // ==================== 切分判断 ====================

    /**
     * 判断指定类是否需要切分（复杂度 >= 5 的方法需要切分）
     * @param className 类名
     * @return true 需要切分，false 不需要
     */
    public boolean needsSlice(String className) {
        ClassComplexityInfo classInfo = loadedClasses.get(className);
        if (classInfo == null) {
            return false;
        }
        return classInfo.hasMethodsNeedingSlice();
    }

    /**
     * 获取指定类的复杂度信息
     */
    public ClassComplexityInfo getClassInfo(String className) {
        return loadedClasses.get(className);
    }

    /**
     * 获取所有已加载的类名
     */
    public Set<String> getAllClassNames() {
        return loadedClasses.keySet();
    }

    /**
     * 获取需要切分的类列表（包含复杂度 >= 5 方法的类）
     */
    public List<String> getClassesNeedingSlice() {
        return loadedClasses.entrySet().stream()
                .filter(e -> e.getValue().hasMethodsNeedingSlice())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 获取不需要切分的类列表
     */
    public List<String> getClassesNotNeedingSlice() {
        return loadedClasses.entrySet().stream()
                .filter(e -> !e.getValue().hasMethodsNeedingSlice())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // ==================== 切片生成 ====================

    /**
     * 为指定类生成切片信息
     * @param className 类名
     * @return 切片列表，如果不需要切分返回 null
     */
    public List<SliceInfo> generateSlices(String className) {
        ClassComplexityInfo classInfo = loadedClasses.get(className);
        if (classInfo == null || !classInfo.hasMethodsNeedingSlice()) {
            return null;
        }

        List<SliceInfo> slices = new ArrayList<>();
        int sliceIndex = 1;

        // 1. 每个复杂度 >= 5 的方法单独一个切片
        for (MethodComplexityInfo method : classInfo.getMethodsNeedingSlice()) {
            SliceInfo slice = new SliceInfo(sliceIndex, "Slice" + sliceIndex);
            slice.testClassName = className + "Test_Slice" + sliceIndex;
            slice.targetMethods.add(method.methodName);
            slice.focusDescription = buildFocusDescription(method);
            slices.add(slice);
            sliceIndex++;
        }

        // 2. 所有复杂度 < 5 的方法合并为一个切片
        List<MethodComplexityInfo> simpleMethods = classInfo.getMethodsNotNeedingSlice();
        if (!simpleMethods.isEmpty()) {
            SliceInfo simpleSlice = new SliceInfo(sliceIndex, "SliceSimple");
            simpleSlice.testClassName = className + "Test_SliceSimple";
            simpleSlice.focusDescription = "测试所有简单复杂度方法（complexity < " + SLICE_THRESHOLD + "）";
            for (MethodComplexityInfo method : simpleMethods) {
                simpleSlice.targetMethods.add(method.methodName);
            }
            slices.add(simpleSlice);
        }

        return slices;
    }

    /**
     * 为指定类生成切片并保存到文件
     * @param className 类名
     * @return 切片文件路径，如果不需要切分返回 null
     */
    public String generateAndSaveSlices(String className) throws IOException {
        List<SliceInfo> slices = generateSlices(className);
        if (slices == null || slices.isEmpty()) {
            return null;
        }

        ClassComplexityInfo classInfo = loadedClasses.get(className);
        return saveSlices(className, classInfo.packagePath, slices);
    }

    /**
     * 保存切片信息到文件
     */
    private String saveSlices(String className, String packagePath, List<SliceInfo> slices) throws IOException {
        Path sliceDir = Paths.get(SLICE_DIR);
        Files.createDirectories(sliceDir);

        Path sliceFile = sliceDir.resolve(className + "_slices.json");

        JsonObject root = new JsonObject();
        root.addProperty("className", className);
        root.addProperty("packagePath", packagePath != null ? packagePath : "");
        root.addProperty("totalSlices", slices.size());

        JsonArray slicesArray = new JsonArray();
        for (SliceInfo slice : slices) {
            JsonObject sliceObj = new JsonObject();
            sliceObj.addProperty("sliceIndex", slice.sliceIndex);
            sliceObj.addProperty("sliceName", slice.sliceName);
            sliceObj.addProperty("testClassName", slice.testClassName);
            sliceObj.addProperty("focusDescription", slice.focusDescription);

            JsonArray methodsArray = new JsonArray();
            for (String method : slice.targetMethods) {
                methodsArray.add(method);
            }
            sliceObj.add("targetMethods", methodsArray);

            slicesArray.add(sliceObj);
        }
        root.add("slices", slicesArray);

        String json = prettyGson.toJson(root);
        Files.write(sliceFile, json.getBytes(StandardCharsets.UTF_8));

        System.out.println("切片信息已保存: " + sliceFile);
        return sliceFile.toString();
    }

    /**
     * 构建测试重点描述
     */
    private String buildFocusDescription(MethodComplexityInfo method) {
        StringBuilder sb = new StringBuilder();
        sb.append("测试方法 ").append(method.methodName);
        sb.append(" (complexity=").append(method.complexity).append(")");

        List<String> focuses = new ArrayList<>();
        if (method.branchCount > 0) {
            focuses.add("覆盖" + method.branchCount + "个分支路径");
        }
        if (method.loopCount > 0) {
            focuses.add("测试" + method.loopCount + "个循环边界");
        }
        if (method.tryCatchCount > 0) {
            focuses.add("覆盖" + method.tryCatchCount + "个异常处理");
        }

        if (!focuses.isEmpty()) {
            sb.append("，").append(String.join("、", focuses));
        }

        return sb.toString();
    }

    // ==================== 切片读取 ====================

    /**
     * 从文件读取切片信息
     * @param className 类名
     * @return 切片列表，文件不存在返回 null
     */
    public List<SliceInfo> loadSlices(String className) throws IOException {
        Path sliceFile = Paths.get(SLICE_DIR, className + "_slices.json");
        if (!Files.exists(sliceFile)) {
            return null;
        }

        String content = new String(Files.readAllBytes(sliceFile), StandardCharsets.UTF_8);
        JsonObject root = gson.fromJson(content, JsonObject.class);

        List<SliceInfo> slices = new ArrayList<>();
        JsonArray slicesArray = root.getAsJsonArray("slices");

        for (int i = 0; i < slicesArray.size(); i++) {
            JsonObject sliceObj = slicesArray.get(i).getAsJsonObject();

            SliceInfo slice = new SliceInfo(
                    sliceObj.get("sliceIndex").getAsInt(),
                    sliceObj.get("sliceName").getAsString()
            );
            slice.testClassName = sliceObj.get("testClassName").getAsString();
            slice.focusDescription = sliceObj.get("focusDescription").getAsString();

            JsonArray methodsArray = sliceObj.getAsJsonArray("targetMethods");
            for (int j = 0; j < methodsArray.size(); j++) {
                slice.targetMethods.add(methodsArray.get(j).getAsString());
            }

            slices.add(slice);
        }

        return slices;
    }

    /**
     * 检查切片文件是否存在
     */
    public static boolean hasSliceFile(String className) {
        return Files.exists(Paths.get(SLICE_DIR, className + "_slices.json"));
    }

    // ==================== 统计信息 ====================

    /**
     * 打印统计信息
     */
    public void printStatistics() {
        System.out.println("\n【复杂度报告统计】");
        System.out.println("  总类数: " + loadedClasses.size());

        List<String> needSlice = getClassesNeedingSlice();
        List<String> noSlice = getClassesNotNeedingSlice();

        System.out.println("  需要切分: " + needSlice.size() + " 个");
        for (String className : needSlice) {
            ClassComplexityInfo info = loadedClasses.get(className);
            System.out.println("    - " + className + " (" + info.getDetailedMethods().size() + " 个高复杂度方法)");
        }

        System.out.println("  普通处理: " + noSlice.size() + " 个");
    }

    // ==================== 主方法（测试用） ====================

    public static void main(String[] args) throws IOException {
        ReportBasedTestRunner runner = new ReportBasedTestRunner();

        // 加载报告
        runner.loadAllReports("src/main/java/M/cli");

        // 打印统计
        runner.printStatistics();

        // 为需要切分的类生成切片
        for (String className : runner.getClassesNeedingSlice()) {
            runner.generateAndSaveSlices(className);
        }
    }
}
