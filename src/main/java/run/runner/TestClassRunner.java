package zju.cst.aces.runner;

import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.*;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

public class TestClassRunner {
    private final Launcher launcher;
    private final List<TestResult> testResults = new ArrayList<>();
    private long startTime;
    private long endTime;
    private final Path tempDir; // 临时目录用于编译测试代码

    public TestClassRunner() throws IOException {
        // 创建临时目录
        tempDir = Files.createTempDirectory("test-classes");

        // 配置 Launcher，添加测试执行监听器
        LauncherConfig config = LauncherConfig.builder()
                .addTestExecutionListeners(new TestListener())
                .build();
        this.launcher = LauncherFactory.create(config);
    }

    public void runTests(Class<?> testClass) {
        startTime = System.currentTimeMillis();

        // 创建测试请求，选择要执行的测试类
        LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                .selectors(selectClass(testClass))
                .build();

        // 执行测试
        launcher.execute(request);

        endTime = System.currentTimeMillis();
    }


    public boolean runTests(String testCode, Map<String, String> classInfo) {
        try {
            // 从classInfo中获取类信息
            String packageName = classInfo.get("包路径");
            String className = classInfo.get("类名");
            String testClassName = className + "Test";
            String fullTestClassName = packageName + "." + testClassName;
            String testFilePath = getTestClassPath(classInfo);

            // 编译测试代码
            Class<?> testClass = compileTestCode(testCode, fullTestClassName);
            try {
                // 读取文件内容并打印到控制台
                String content = new String(Files.readAllBytes(Paths.get(testFilePath)));
                System.out.println("生成的测试类文件内容：");
                System.out.println(content+"\n 内容结束！");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("无法读取生成的测试类文件： " + e.getMessage());
            }

            // 执行测试
            runTests(testClass);

            // 检查是否所有测试都通过
            long failedCount = testResults.stream()
                    .filter(r -> r.status == TestStatus.FAILED)
                    .count();
            return failedCount == 0;
        } catch (Exception e) {
            e.printStackTrace();
            testResults.add(new TestResult(classInfo.get("类名") + "Test", TestStatus.FAILED, e));
            return false;
        }
    }

    public static String getTestClassPath(Map<String, String> classInfo) {
        String packagePath = classInfo.get("包路径");
        String className = classInfo.get("类名");
        String testClassName = className + "Test";

        // 使用项目根目录作为基准
        Path projectRoot = Paths.get(System.getProperty("user.dir")).toAbsolutePath();

        return projectRoot.resolve(
                Paths.get("src", "test", "java",
                        packagePath.replace(".", "/"),
                        testClassName + ".java")
        ).toString();
    }

    private Class<?> compileTestCode(String testCode, String className) throws Exception {
        // 创建Java文件对象
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // 创建内存中的Java源文件
        JavaFileObject fileObject = new JavaSourceFromString(className, testCode);

        // 设置编译选项
        List<String> options = Arrays.asList(
                //"--release", "11",  // 指定Java版本为11
                "-d", tempDir.toString(),
                "-classpath", getClasspath()
        );

        // 执行编译
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, null, diagnostics, options, null, Arrays.asList(fileObject));
        System.out.println("运行时编译开始");
        boolean success = task.call();

        // 检查编译错误
        if (!success) {
            StringBuilder errorMsg = new StringBuilder("编译错误:\n");
            for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
                errorMsg.append(diagnostic.getMessage(null)).append("\n");
            }
            throw new RuntimeException(errorMsg.toString());
        }

        // 使用自定义类加载器加载编译后的类
        URLClassLoader classLoader = URLClassLoader.newInstance(
                new URL[]{tempDir.toUri().toURL()},
                getClass().getClassLoader()
        );

        return classLoader.loadClass(className);
    }


    private String getClasspath() {
        // 这里需要根据实际情况配置JUnit和项目依赖的路径
        return System.getProperty("java.class.path") +
                File.pathSeparator + "lib/junit-jupiter-api-5.10.2.jar" +
                File.pathSeparator + "lib/junit-jupiter-engine-5.10.2.jar" +
                File.pathSeparator + "lib/apiguardian-api-1.1.2.jar" +
                File.pathSeparator + "lib/junit-jupiter-api-5.9.2.jar" +
                File.pathSeparator + "lib/junit-jupiter-engine-5.9.2.jar" +
                File.pathSeparator + "lib/junit-platform-launcher-1.9.2.jar" +
                File.pathSeparator + "lib/opentest4j-1.3.0.jar";
    }

    public String getSummary() {
        long total = testResults.size();
        long passed = testResults.stream().filter(r -> r.status == TestStatus.PASSED).count();
        long failed = testResults.stream().filter(r -> r.status == TestStatus.FAILED).count();
        long skipped = testResults.stream().filter(r -> r.status == TestStatus.SKIPPED).count();

        StringBuilder summary = new StringBuilder();
        summary.append("测试执行结果：\n");
        summary.append("  总测试数: ").append(total).append("\n");
        summary.append("  通过:    ").append(passed).append("\n");
        summary.append("  失败:    ").append(failed).append("\n");
        summary.append("  跳过:    ").append(skipped).append("\n");
        summary.append("  执行时间: ").append(getExecutionTime()).append(" ms\n");

        return summary.toString();
    }


    public String getDetailedResults() {
        StringBuilder details = new StringBuilder();
        details.append(getSummary());
        details.append("\n详细结果：\n");

        for (TestResult result : testResults) {
            // 提取方法名（去除括号）
            String testName = result.testName;
            if (testName.endsWith("()")) {
                testName = testName.substring(0, testName.length() - 2);
            }

            details.append("  • ").append(testName).append("() [").append(result.status).append("]");

            if (result.status == TestStatus.FAILED && result.errorMessage != null) {
                details.append("\n    错误: ").append(result.errorMessage);
            }
            details.append("\n");
        }

        return details.toString();
    }


    public long getExecutionTime() {
        return endTime - startTime;
    }


    public List<TestResult> getResults() {
        return testResults;
    }


    public enum TestStatus {
        PASSED, FAILED, SKIPPED
    }


    public static class TestResult {
        String testName;
        TestStatus status;
        String errorMessage;
        String stackTrace;

        public TestResult(String testName, TestStatus status) {
            this.testName = testName;
            this.status = status;
        }

        public TestResult(String testName, TestStatus status, Throwable throwable) {
            this(testName, status);
            this.errorMessage = throwable.getMessage();

            // 捕获堆栈跟踪信息
            StringWriter sw = new StringWriter();
            throwable.printStackTrace(new PrintWriter(sw));
            this.stackTrace = sw.toString();
        }
    }

    private class TestListener implements TestExecutionListener {
        @Override
        public void executionStarted(TestIdentifier testIdentifier) {
            // 测试开始时的处理
        }

        @Override
        public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
            if (testIdentifier.isTest()) {
                String testName = testIdentifier.getDisplayName();

                switch (result.getStatus()) {
                    case SUCCESSFUL:
                        testResults.add(new TestResult(testName, TestStatus.PASSED));
                        break;
                    case FAILED:
                        result.getThrowable().ifPresent(throwable ->
                                testResults.add(new TestResult(testName, TestStatus.FAILED, throwable))
                        );
                        break;
                    case ABORTED:
                        testResults.add(new TestResult(testName, TestStatus.SKIPPED));
                        break;
                }
            }
        }
    }


    private static class JavaSourceFromString extends SimpleJavaFileObject {
        final String code;

        JavaSourceFromString(String name, String code) {
            super(URI.create("string:///" + name.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.code = code;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return code;
        }
    }



    public static void main(String[] args) {
        try {
            // 测试代码文件路径
            String testFilePath = "D:\\fx\\jafx\\chatunitest-core\\src\\test\\java\\cli\\TypeHandler_Suite.java";

            // 从文件读取测试代码
            String testCode = readFileContent(testFilePath);

            // 配置类信息
            Map<String, String> classInfo = new HashMap<>();
            classInfo.put("包路径", "cli");  // 包路径应与测试文件的package声明一致
            classInfo.put("类名", "AlreadySelectedExceptionTest");    // 类名应与测试文件名前缀一致（不包含Test后缀）

            // 创建并运行测试执行器
            TestClassRunner runner = new TestClassRunner();
            boolean success = runner.runTests(testCode, classInfo);

            // 输出结果
            System.out.println(runner.getDetailedResults());

            if (success) {
                System.out.println("\n所有测试通过！");
            } else {
                System.out.println("\n存在测试失败，请检查详细结果！");
            }



        } catch (Exception e) {
            System.err.println("执行测试时出错：" + e.getMessage());
            e.printStackTrace();
        }
    }


    static String readFileContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}