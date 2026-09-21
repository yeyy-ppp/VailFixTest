package zju.cst.aces.runner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.core.LauncherConfig;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;

import javax.tools.*;
import java.io.*;
import java.net.URI;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatUniTestMain {

    public static void main(String[] args) {
    }
}

//0524 main函数版本
/*  public static void main(String[] args) throws IOException {
    String apiKey = "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";
    //读取待测试列表
    String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\codeing";
    List<String> classNames = new ArrayList<>();

    // 遍历指定目录下的所有Java文件-待实现
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
    String sourceCodePath = "src/main/java/codeing/Example.java";
    TestCore testCoreClass = new TestCore(apiKey);
    testCoreClass.className = "Example"; // 设置当前处理的类名
    String sourceCodeFile=testCoreClass.readSourceCode(sourceCodePath);
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
    System.out.println("起始时间："+startTime+"\n");

    long analyzeStart = System.currentTimeMillis();

    // 初始化 Timer
    testCoreClass.timer.start("套件");
    System.out.println("分析起始时间："+analyzeStart+"\n");
    //分析开始
    testCoreClass.timer.start("分析");
    String analyzePromt=testCoreClass.analyzeSourceCode(sourceCodeFile);

    // 从分析结果中提取类信息
    Map<String, String> classInfo = testCoreClass.extractClassInfo(analyzePromt);
    analyzeSuccess = true;
    analyzeEndTime = System.currentTimeMillis();
    System.out.println("分析结束时间："+analyzeEndTime+"\n");
    System.out.println("分析总耗时："+(analyzeEndTime-analyzeStart));
    analyzeTokenCount = testCoreClass.tokenCount;
    if (analyzePromt == null || analyzePromt.isEmpty()) {
        System.out.println("分析阶段失败：LLM 未返回有效结果");
        return;
    }
    //分析结束
    testCoreClass.timer.end("分析");
    //生成
    //  String testCode=testCoreClass.generateTestCode(sourceCodeFile,analyzePromt);
    //生成开始
    testCoreClass.timer.start("生成");
    String testCode=testCoreClass.generateTestCode2(sourceCodeFile,analyzePromt,conversationHistory);

    generateSuccess = true;
    generateEndTime = System.currentTimeMillis();
    generateTokenCount = testCoreClass.tokenCount - analyzeTokenCount;
    if (testCode == null || testCode.isEmpty()) {
        System.out.println("生成阶段失败：LLM 未返回有效结果");
        return;
    }
    //生成结束
    testCoreClass.timer.end("生成");
    //验证开始
    testCoreClass.timer.start("验证");
    String validateResult=testCoreClass.validateTestCode(testCode);

    validateSuccess = validateResult.contains("全部验证通过");
    validateEndTime = System.currentTimeMillis();
    validateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount;
    if (validateResult == null || validateResult.isEmpty()) {
        System.out.println("验证阶段失败：LLM 未返回有效结果");
        return;
    }
    //验证结束
    testCoreClass.timer.end("验证");
    if (!validateSuccess) {
        validateFailReason = validateResult;
    }
    if (validateSuccess) {
        //评估开始
        testCoreClass.timer.start("评估");
        //评估

        String evaluateResult=testCoreClass.evaluateTestCode(testCode);

        evaluateSuccess = true;
        evaluateEndTime = System.currentTimeMillis();
        evaluateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount;
        evaluateCoverageData = evaluateResult;
        if (evaluateResult == null || evaluateResult.isEmpty()) {
            System.out.println("评估阶段失败：LLM 未返回有效结果");
            return;
        }
        //评估结束
        testCoreClass.timer.end("评估");
        //如果覆盖率未达到期望 优化测试代码

        //创建测试类
        //  createTestClass(testCode);
        System.out.println("测试类创建开始：");
        createTestClass2(testCode, classInfo);
        overallSuccess = true;

        System.out.println("测试类编译开始：");
        TestClassCompiler testClassCompiler=new TestClassCompiler();
        boolean compileResult=testClassCompiler.compileTestClass(classInfo);
        // 编译测试类
        //  boolean compileResult = testCoreClass.compileTestClass(classInfo);
        if (compileResult) {
            System.out.println("测试类编译成功！");
            //编译成功，尝试运行测试代码

            // 运行测试
            try {
                System.out.println("测试类运行开始：");
                TestClassRunner runner = new TestClassRunner();
                //获取测试代码位置
                String testFilePath = Paths.get(
                        "D:\\fx\\jafx\\chatunitest-core",
                        "src", "test", "java",
                        classInfo.get("包路径").replace(".", File.separator),
                        classInfo.get("类名") + "Test.java"
                ).toString();

                String testcoderunner=runner.readFileContent(testFilePath);
                boolean testResult = runner.runTests(testcoderunner,classInfo);
                System.out.println(runner.getDetailedResults());
                if (testResult) {
                    System.out.println("测试类覆盖率计算开始：");
                    String mvnTestResult = testCoreClass.executeMavenCommand("test");
                    System.out.println("mvn test 结果：\n" + mvnTestResult);
                    double instructionCoverage = extractCoverageFromMvnResult(mvnTestResult, "Overall Coverage");
                    System.out.println("mvn test结果展示：" + instructionCoverage);

                    // 提取覆盖率数据（假设 mvn test 输出中包含覆盖率信息）
                    List<Map<String, String>> coverageInfoList = extractCoverageInfo();
                    String targetPackage = "codeing";
                    List<Map<String, String>> filteredCoverageInfoList = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, targetPackage);

                    // 检查覆盖率
                    boolean coverageSufficient = checkCoverage(filteredCoverageInfoList);
                    System.out.println("Coverage sufficient: " + coverageSufficient);
                    //暂时不优化
                    coverageSufficient=true;
                    if (!coverageSufficient) {
                        // 覆盖率不足，优化测试代码
                        System.out.println("测试类优化开始：覆盖率不足，优化测试代码...");
                        String optimizedTestCode = testCoreClass.optimizeTestCode(testCode, mvnTestResult);
                        // 验证优化后的测试代码
                        String validate2Result = testCoreClass.validateTestCode(optimizedTestCode);
                        boolean validate2Success = validate2Result.contains("全部验证通过");
                        if (validate2Success) {
                            // 更新测试类内容
                            updateTestClass(optimizedTestCode, classInfo);
                            // 重新编译测试类
                            TestClassCompiler testClass2Compiler = new TestClassCompiler();
                            boolean compile2Result = testClass2Compiler.compileTestClass(classInfo);
                            if (compile2Result) {
                                // 重新运行测试
                                TestClassRunner optimizedRunner = new TestClassRunner();
                                //获取测试代码位置
                                String testFilePath2 = Paths.get(
                                        "D:\\fx\\jafx\\chatunitest-core",
                                        "src", "test", "java",
                                        classInfo.get("包路径").replace(".", File.separator),
                                        classInfo.get("类名") + "Test.java"
                                ).toString();
                                System.out.println("尝试读取文件: " + testFilePath); // 添加调试输出
                                String testcoderunner2=runner.readFileContent(testFilePath2);
                                boolean optimizedTestResult = optimizedRunner.runTests(testcoderunner2, classInfo);
                                System.out.println(optimizedRunner.getDetailedResults());
                                if (optimizedTestResult) {
                                    System.out.println("优化后的测试代码运行成功！");
                                    // 重新执行 mvn test
                                    String newMvnTestResult = testCoreClass.executeMavenCommand("test");
                                    System.out.println("新的 mvn test 结果：\n" + newMvnTestResult);
                                } else {
                                    System.out.println("优化后的测试代码运行失败！");
                                }
                            } else {
                                System.out.println("优化后的测试类编译失败！");
                            }
                        } else {
                            System.out.println("优化后的测试代码验证失败！");
                        }
                    } else {
                        System.out.println("覆盖率满足要求！");
                    }

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
    }
    else
    {
        //修复开始
        testCoreClass.timer.start("修复");
        fixAttempt = 1;
        while (fixAttempt <= 3) {
            //修复
            String fixResult = testCoreClass.fixTestCode(validateResult, testCode);
            fixSuccess = !fixResult.equals("null");
            fixEndTime = System.currentTimeMillis();
            fixTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount;
            if (fixResult == null || fixResult.isEmpty()) {
                System.out.println("修复阶段失败：LLM 未返回有效结果");
                break;
            }
            if (fixSuccess) {
                String evaluateResult = testCoreClass.evaluateTestCode(testCode);
                evaluateSuccess = true;
                evaluateEndTime = System.currentTimeMillis();
                evaluateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount - fixTokenCount;
                if (evaluateResult == null || evaluateResult.isEmpty()) {
                    System.out.println("评估阶段失败：LLM 未返回有效结果");
                    break;
                }
                // 假设 evaluateResult 包含覆盖率数据，如 "整体代码覆盖率：80%，分支覆盖率：70%，行覆盖率：85%"
                evaluateCoverageData = evaluateResult;
                //   testCoreClass.createTestClass(testCode);
                testCoreClass.createTestClass2(testCode, classInfo);;
                overallSuccess = true;

                TestClassCompiler testClassCompiler=new TestClassCompiler();
                boolean compileResult=testClassCompiler.compileTestClass(classInfo);
                // 编译测试类
                //  boolean compileResult = testCoreClass.compileTestClass(classInfo);
                if (compileResult) {
                    System.out.println("测试类编译成功！");
                    //编译成功，尝试运行测试代码

                    // 运行测试
                    try {
                        System.out.println("运行开始：");
                        TestClassRunner runner = new TestClassRunner();
                        //获取测试代码位置
                        String testFilePath = Paths.get(
                                "D:\\fx\\jafx\\chatunitest-core",
                                "src", "test", "java",
                                classInfo.get("包路径").replace(".", File.separator),
                                classInfo.get("类名") + "Test.java"
                        ).toString();
                        System.out.println("尝试读取文件: " + testFilePath); // 添加调试输出
                        String testcoderunner2=runner.readFileContent(testFilePath);
                        boolean testResult = runner.runTests(testcoderunner2,classInfo);
                        System.out.println("运行结束,输出结果：");
                        System.out.println(runner.getDetailedResults());
                        if (testResult) {
                            System.out.println("所有测试通过！");
                            String mvnTestResult = testCoreClass.executeMavenCommand("test");
                            System.out.println("mvn test 结果：\n" + mvnTestResult);
                            // 提取覆盖率数据（假设 mvn test 输出中包含覆盖率信息）
                            List<Map<String, String>> coverageInfoList = extractCoverageInfo();
                            String targetPackage = "codeing";
                            List<Map<String, String>> filteredCoverageInfoList = CoverageEvaluator.filterCoverageInfoByPackage(coverageInfoList, targetPackage);

                            // 检查覆盖率
                            boolean coverageSufficient = checkCoverage(filteredCoverageInfoList);
                            System.out.println("Coverage sufficient: " + coverageSufficient);
                            //暂时不优化
                            coverageSufficient=true;
                            if (!coverageSufficient) {
                                // 覆盖率不足，优化测试代码
                                System.out.println("覆盖率不足，优化测试代码...");
                                String optimizedTestCode = testCoreClass.optimizeTestCode(testCode, mvnTestResult);
                                // 验证优化后的测试代码
                                String validate2Result = testCoreClass.validateTestCode(optimizedTestCode);
                                boolean validate2Success = validate2Result.contains("全部验证通过");
                                if (validate2Success) {
                                    // 更新测试类
                                    updateTestClass(optimizedTestCode, classInfo);
                                    // 重新编译测试类
                                    TestClassCompiler testClass2Compiler = new TestClassCompiler();
                                    boolean compile2Result = testClass2Compiler.compileTestClass(classInfo);
                                    if (compile2Result) {
                                        // 重新运行测试
                                        TestClassRunner optimizedRunner = new TestClassRunner();
                                        boolean optimizedTestResult = optimizedRunner.runTests(optimizedTestCode, classInfo);
                                        System.out.println(optimizedRunner.getDetailedResults());
                                        if (optimizedTestResult) {
                                            System.out.println("优化后的测试代码运行成功！");
                                            // 重新执行 mvn test
                                            String newMvnTestResult = testCoreClass.executeMavenCommand("test");
                                            System.out.println("新的 mvn test 结果：\n" + newMvnTestResult);
                                        } else {
                                            System.out.println("优化后的测试代码运行失败！");
                                        }
                                    } else {
                                        System.out.println("优化后的测试类编译失败！");
                                    }
                                } else {
                                    System.out.println("优化后的测试代码验证失败！");
                                }
                            } else {
                                System.out.println("覆盖率满足要求！");
                            }

                        } else {
                            System.out.println("测试未通过：");
                            System.out.println(runner.getDetailedResults());
                        }
                    } catch (Exception e) {
                        System.out.println("运行测试时出错：" + e.getMessage());
                        e.printStackTrace();
                    }
                }

                fixSuccessTimes = String.valueOf(fixAttempt);
                break;
            } else {
                System.out.println("第 " + fixAttempt + " 次修复失败：" + fixResult + "over\n");
                fixFailReason += "第 " + fixAttempt + " 次修复失败，原因是：" + fixResult + "\n";
                testCoreClass.failedClasses.add(testCoreClass.className);
                fixAttempt++;
            }
        }
        //修复结束
        testCoreClass.timer.end("修复");
    }
    // 记录测试代码生成过程的信息
    testCoreClass.logTestCodeGenerationInfo(analyzeSuccess, generateSuccess, validateSuccess,
            fixSuccess, evaluateSuccess, overallSuccess,analyzeStart,analyzeEndTime, generateEndTime,
            validateEndTime, fixEndTime, evaluateEndTime, analyzeTokenCount, generateTokenCount,
            validateTokenCount, fixTokenCount, evaluateTokenCount, validateFailReason,
            fixAttempt, fixFailReason, fixSuccessTimes, evaluateCoverageData);
    testCoreClass.closeClient();
    testCoreClass.timer.end("套件");

}


/*package zju.cst.aces.runner;

        import com.google.gson.Gson;
        import com.google.gson.GsonBuilder;
        import okhttp3.*;
        import zju.cst.aces.api.config.Config;
        import zju.cst.aces.api.phase.step.TestGeneration;

        import java.io.IOException;
        import java.util.ArrayList;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;

// 主类，用于生成测试代码
public class ChatUnitestCore {
    //private static final String API_URL = "https://api.gptsapi.net/v1/chat/completions";
    // private static final String MODEL = "gpt-4o-mini";
    private static final String API_URL = "https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL = "deepseek/deepseek-r1:free";
    private final String apiKey; //="sk-o7C539636d3cb6ec90bc084880c104d25926fd8af0bilWd8";
    private final OkHttpClient client;
    private final Gson gson;



    public ChatUnitestCore(String apiKey) {
        this.apiKey = apiKey;
        this.client = new OkHttpClient();
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    }

    // 生成测试代码的主要方法
    public String generateTestCode(String sourceCode) throws IOException {
        // 构建请求体
        RequestBody requestBodyG = buildRequestBody(sourceCode);
        // 构建请求
        Request requestG = buildRequest(requestBodyG);
        // 发送请求并获取响应
        try (Response responseG = client.newCall(requestG).execute()) {
            if (responseG.isSuccessful() && responseG.body() != null) {
                String responseBody = responseG.body().string();
                return extractTestCode(responseBody);
            } else {
                throw new IOException("Request failed: " + responseG);
            }
        }

    }


    // 构建请求体
    private RequestBody buildRequestBody(String sourceCode) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", "You are a senior tester in Java projects, your task is writting tests for a specific focal method in a focal class with JUnit5 and Mockito framework (A focal method means a method under test).\n" +
                "I will provide the following information of the focal method:\n" +
                "1. Required dependencies to import.\n" +
                "2. The focal class signature.\n" +
                "3. Source code of the focal method.\n" +
                "4. Signatures of other methods and fields in the class.\n" +
                "I will provide following brief information if the focal method has dependencies:\n" +
                "1. Signatures of dependent classes.\n" +
                "2. Signatures of dependent methods and fields in the dependent classes.\n" +
                "You need to create a complete unit test using JUnit 5, ensuring to cover all branches. Compile without errors, and use reflection to invoke private methods or fields if needed.\n" +
                "Please implement the test corresponding to the path according to the execution path information provided in annotations. No additional explanations required."+ sourceCode);
        messages.add(message);

        Map<String, Object> requestBodyMap = new HashMap<>();
        requestBodyMap.put("model", MODEL);
        requestBodyMap.put("messages", messages);

        return RequestBody.create(gson.toJson(requestBodyMap), MediaType.get("application/json; charset=utf-8"));
    }

    // 构建请求
    private Request buildRequest(RequestBody requestBody) {
        return new Request.Builder()
                .url(API_URL)
                .post(requestBody)
                .addHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    // 从响应中提取测试代码
    private String extractTestCode(String responseBody) {
        Map<String, Object> responseMap = gson.fromJson(responseBody, HashMap.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> firstChoice = choices.get(0);
            Map<String, String> message = (Map<String, String>) firstChoice.get("message");
            return message != null ? message.get("content") : "";
        }
        return "";
    }


    public static void main(String[] args) {
        //api String apiKey = "sk-o7C539636d3cb6ec90bc084880c104d25926fd8af0bilWd8";
        String apiKey = "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";
        ChatUnitestCore chatUnitestCore = new ChatUnitestCore(apiKey);
        String sourceCode = "public class Example {\n    public int add(int a, int b) {\n        return a + b;\n    }\n}";
        try {
            String testCode = chatUnitestCore.generateTestCode(sourceCode);
            System.out.println("Generated Test Code:\n" + testCode);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}*/



/*      package zju.cst.aces.runner;         0520版本

        import com.google.gson.Gson;
        import com.google.gson.GsonBuilder;
        import okhttp3.*;

        import javax.tools.*;
        import java.io.*;
        import java.nio.file.Files;
        import java.nio.file.Paths;
        import java.util.*;
        import java.util.regex.Matcher;
        import java.util.regex.Pattern;


//核心代码
public class TestCore {
    private static final String API_URL="https://openrouter.ai/api/v1/chat/completions";
    private static final String MODEL= "deepseek/deepseek-r1:free";
    private final String apiKey;
    private final OkHttpClient client;
    private final Gson gson;
    private long startTime=System.currentTimeMillis();  // 测试代码生成开始时间
    private long endTime;  // 测试代码生成结束时间
    private String sourceCodeFile;  //待测试源代码
    private String sourceCodePath;  //待测试源代码路径
    private long totalTime;  // 测试代码生成总耗时
    private int tokenCount;  // API 调用消耗的 token 数量
    private List<String> failedClasses;  // 记录修复失败的待测试类
    private String className; // 当前处理的类名
    // 分析提示
    private static final String analyzeCodePromt="你是一个Java程序分析专家,请严格根据以下要求从专业角度分析该Java代码，要求如下：\n" +
            "1. **包路径**：识别类所在的包路径（例如 `package zju.cst.aces.runner`）。\n" +
            "2. **类名**：类的名称。\n" +
            "3. **方法名**：每个方法的名称。\n" +
            "4. **返回类型**：每个方法的返回类型。\n" +
            "5. **实现功能**：每个方法实现的具体功能。\n" +
            "请按照以下格式返回结果：\n" +
            "```\n" +
            "包路径：[包路径]\n" +
            "类名：[类名]\n" +
            "方法名：[方法名]\n" +
            "返回类型：[返回类型]\n" +
            "功能描述：[功能描述]\n" +
            "```\n";
    //生成提示
    private static final String generatePrompt="\"根据以下待测试类的信息生成测试代码：\"+\n" +
            "\"包路径：{packagePath}\"+\n"+
            "\"类名：{className}\"+\n"+
            "\"方法：{methodName}\"+\n"+
            "\"返回类型：{returnType}\"+\n"+
            "\"功能描述：{functionDescription}\"+\n"+
            "\"以上是待测试类的一些基本信息,\"+\n"+
            "\"你是一位资深的Java项目测试人员，你的任务是为特定的焦点类中的焦点方法编写测试用例（焦点方法即待测试的方法）。\"+\n" +
            "\"你需要使用JUnit5测试框架编写完整的单元测试，确保覆盖所有分支。测试代码应能够无错误地编译，如果需要，可以使用反射来调用私有方法或字段。\"+\n" +
            "\"请根据注解中提供的执行路径信息，实现对应的测试。\"+\n" +
            "\"生成的测试类或测试方法名称应为源代码类名或方法名后面加Test，如果有相同名称则再加数字后缀。\"+\n" +
            "\"例如，如果源代码类名为Example，则测试类名为ExampleTest；如果有冲突，则命名为ExampleTest1、ExampleTest2等。" +
            "\"生成的测试代码应该包含正确的包路径，导入正确的焦点类或焦点方法" +
            "\"不需要返回思考过程等额外信息，附带的待测试源代码如下：\n";
    //验证提示
    private static final String validatePrompt="你是一个测试代码审查器，请你检查以下测试代码是否符合以下要求：\n"
            + "1. **导入语句**：是否正确导入了所有必要的类，例如 `org.junit.jupiter.api.*`, `org.mockito.*` 等。\n"
            + "2. **测试方法逻辑**：是否覆盖了所有关键路径，包括正常情况、边界条件和异常情况。\n"
            + "3. **语法错误**：是否存在任何语法错误或拼写错误。\n"
            + "4. **编译错误**：是否存在任何可能导致编译失败的问题。\n"
            + "5. **运行时错误**：是否存在任何可能导致运行时异常的问题。\n"
            + "请逐项给出你的判断，并指出是否“通过验证”或“不通过验证”。"
            + "若全部验证通过,请返回全部验证通过。"
            + "若不通过，请详细说明理由，并按照以下格式标记出具体的错误位置和原因：\n"
            + "```\n"
            + "该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[错误原因]\n"
            + "```\n"
            + "不需要返回思考过程等额外信息，附带的测试代码如下：\n";
    //评估提示
    private static final String evaluatePrompt = "你是一位专业的测试代码评估专家，专门负责精准评估测试代码的质量。请根据以下详细要求，对提供的测试代码进行全面的代码覆盖率分析：\n" +
            "- **整体代码覆盖率**：计算测试代码覆盖被测试代码中语句的比例，以百分比形式精确呈现，格式为【整体代码覆盖率】：【XX%】。\n" +
            "- **分支覆盖率**：分析测试代码对被测试代码中所有分支（如 `if-else`、`switch` 等条件语句）的覆盖情况，给出分支被执行的比例，格式为【分支覆盖率】：【XX%】。\n" +
            "- **行覆盖率**：确定测试代码执行到被测试代码中每一行的比例，以百分比形式展示，格式为【行覆盖率】：【XX%】。\n" +
            "- **语句覆盖率**：衡量在测试中执行了多少代码语句，以百分比形式展示，格式为【语句覆盖率】：【XX%】。\n"+
            "- **路径覆盖率**：衡量在测试中是否覆盖了源代码的所有可能路径,，以百分比形式展示，格式为【路径覆盖率】：【XX%】。\n"+
            "\n" +
            "请严格按照上述格式输出结果，不包含任何额外的解释、注释或思考过程。测试代码如下：";

    //修复提示
    private static final String fixPrompt = "以上是测试代码的错误信息,你是一个高级测试代码修复师，根据以下信息对测试代码进行修复：\n" +
            "请根据错误信息提示修复测试代码，确保修复后的代码满足所有验证要求，包括导入语句、测试方法逻辑、语法正确性、编译和运行时无错误。。" +
            "如果修复失败就直接返回【修复失败】四个字以及修复失败的原因，仅返回错误；否则返回【修复成功】四个字以及修复成功的测试代码，仅返回代码"+
            "不需要额外的解释、注释和思考过程。"+
            "测试代码如下：";

    public TestCore(String apiKey) {
        this.apiKey=apiKey;
        this.client = new OkHttpClient();
        this.gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        this.failedClasses = new ArrayList<>(); // 初始化 failedClasses 列表
    }

    //1.读取源代码
    private static String readSourceCode(String sourceCodePath) throws IOException {
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
    private RequestBody buildRequestBody(String code,String prompt) {
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
        String analyzePromt=sendRequestAndGetResponse(sourceCodeFile, analyzeCodePromt);
        System.out.println("解析提示内容：\n"+analyzePromt + "over\n");
        return analyzePromt;
    }

    //6.1 解析提取源代码信息构建测试类
    public Map<String, String> extractClassInfo(String analyzeResult) {
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
        } else {
            System.err.println("未能从分析结果中提取包路径和类名");
        }
        return classInfo;
    }

    //7.LLM生成测试代码
    public String generateTestCode(String sourceCodeFile,String analyzePromt) throws IOException {
        String testCode=sendRequestAndGetResponse(sourceCodeFile, analyzePromt+generatePrompt);
        System.out.println("生成测试代码：\n"+testCode + "over\n");
        return testCode;
    }

    //7.2 LLM生成测试代码，缓存中获取代码
    public String generateTestCode2(String sourceCodeFile, String analyzePromt, String conversationHistory) throws IOException {
        // 调用 sendRequestAndGetResponse 方法发送请求并获取测试代码
        // 提取分析结果中的信息
        Map<String, String> classInfo = extractClassInfo(analyzePromt);
        String packagePath = classInfo.get("包路径");
        String className = classInfo.get("类名");
        String methodName=classInfo.get("方法名");
        String returnType=classInfo.get("返回类型");
        String functionDescription=classInfo.get("功能描述");

        // 检查提取的信息是否完整
        if (packagePath == null || className == null || methodName == null || returnType == null || functionDescription == null) {
            System.err.println("提取的信息不完整，无法生成测试代码。");
            return null;
        }
        // 构建生成测试代码的提示
        String newGprompt = generatePrompt
                .replace("{packagePath}", packagePath)
                .replace("{className}", className)
                .replace("{methodName}", methodName)
                .replace("{returnType}", returnType)
                .replace("{functionDescription}", functionDescription);
        String testCode = sendRequestAndGetResponse2(sourceCodeFile, newGprompt, conversationHistory);
        System.out.println("生成测试代码：\n" + testCode + "over\n");
        return testCode;
    }

    //8.LLM验证测试代码
    public String validateTestCode(String testCode) throws IOException {
        String validateResult=sendRequestAndGetResponse(testCode, validatePrompt);
        System.out.println("测试代码验证结果：\n"+validateResult + "over\n");
        return validateResult;
    }

    //9.LLM评估测试代码
    public String evaluateTestCode(String testCode) throws IOException {
        String evaluateResult=sendRequestAndGetResponse(testCode, evaluatePrompt);
        System.out.println("测试代码评估结果：\n"+evaluateResult + "over\n");
        return evaluateResult;
    }

    //10.LLM修复测试代码
    public String fixTestCode(String errorPrompt, String testCode) throws IOException {
        int fixNumber = 1;
        boolean fixSuccess = false;
        boolean validateSuccess = false;

        while (!validateSuccess && fixNumber <= 3) {
            // 构建修复提示
            String fixRequestPrompt = errorPrompt + fixPrompt;
            String fixResult = sendRequestAndGetResponse(testCode, fixRequestPrompt);
            System.out.println("第 " + fixNumber + " 次修复结果：\n" + fixResult + "over\n");

            // 检查修复是否成功
            if (fixResult.contains("修复成功")) {
                // 提取修复后的测试代码
                testCode = fixResult.replace("修复成功", "").trim();
                fixSuccess = true;
                String validateResult = validateTestCode(testCode);
                if (validateResult.contains("全部验证通过")) {
                    validateSuccess = true;
                    System.out.println("修复后的代码验证成功！");
                } else {
                    System.out.println("修复后的代码验证失败！");
                    errorPrompt = validateResult;
                    fixNumber++;
                }
            } else {
                // 更新错误提示，包含修复失败的原因
                errorPrompt += "\n修复尝试 " + fixNumber + " 失败，原因是：\n" + fixResult;
                fixNumber++;
            }
        }

        if (validateSuccess) {
            System.out.println("修复和验证成功！");
        } else {
            System.out.println("经过 " + fixNumber + " 次修复尝试，仍未成功。");
            testCode="null";
        }

        return testCode;
    }

    //11.提取测试代码
    private static String extractPureCode(String response) {
        String startTag = "```java";
        String endTag = "```";

        int start = response.indexOf(startTag);
        int end = response.lastIndexOf(endTag);

        if (start != -1 && end != -1 && end > start) {
            // 跳过起始标记并提取代码
            return response.substring(start + startTag.length(), end).trim();
        }
        // 如果未找到标记，返回原始内容
        return response;
    }

    //12.创建测试类
    public static void createTestClass(String passTest) {
        try {
            // 定义测试类文件的路径和名称
            String fileName = "src/main/java/zju/cst/aces/runner/ExampleTest.java";
            // 将测试代码写入文件
            try (FileWriter writer = new FileWriter(fileName)) {
                String pureCode = extractPureCode(passTest);
                writer.write(pureCode);
            }
            System.out.println("测试类创建成功：" + fileName);
        } catch (IOException e) {
            System.err.println("创建测试类时出错：" + e.getMessage());
        }
    }
    //12.2创建测试类2
    public static void createTestClass2(String passTest, Map<String, String> classInfo) {
        try {
            // 构造测试类的路径和文件名
            String packagePath = classInfo.get("包路径");
            if (packagePath == null || packagePath.isEmpty()) {
                throw new IllegalArgumentException("包路径不能为空");
            }
            String basePackagePath = packagePath.replace(".", "/");
            if (!basePackagePath.startsWith("/")) {
                basePackagePath = "/" + basePackagePath;
            }
            String className = classInfo.get("类名");
            String fileName = "src/test/java" + basePackagePath + "/" + className + "Test.java";

            // 创建目录（如果不存在）
            java.nio.file.Path path = Paths.get("src/test/java" + basePackagePath);
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }

            // 将测试代码写入文件
            try (FileWriter writer = new FileWriter(fileName)) {
                String pureCode = extractPureCode(passTest);
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
                                          String evaluateCoverageData) {

        endTime = System.currentTimeMillis();
        totalTime = endTime - startTime;

        try (FileWriter writer = new FileWriter("Example_test_code_generation_log5201.txt", true)) {
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
            writer.write("测试代码生成成功：" + overallSuccess + "\n");
            writer.write("失败原因：" + String.join(", ", failedClasses) + "\n");
            writer.write("--------------------------------------------------------\n");
        } catch (IOException e) {
            System.err.println("写入日志文件时出错：" + e.getMessage());
        }
    }


     //15.编译器执行验证代码
    //测试代码类生成成功，尝试编译测试代码
    public boolean compileTestClass(Map<String, String> classInfo) {

        try {
        // 构造测试类的路径
        String basePackagePath = classInfo.get("包路径").replace(".", "/");
        String className = classInfo.get("类名");
        String testClassName = className + "Test";
        String testClassPath = "src/test/java" + basePackagePath + "/" + testClassName + ".java";

        // 获取项目的类路径
        List<String> classpathElements = new ArrayList<>();
        // 假设项目的类路径包含 target/classes 和 target/test-classes
        classpathElements.add("target/classes");
        classpathElements.add("target/test-classes");


        // 添加 JUnit 依赖的类路径
        // 假设 JUnit JAR 文件在 lib 目录下
            classpathElements.add("lib/junit-jupiter-api-5.10.2.jar");
            classpathElements.add("lib/junit-jupiter-engine-5.10.2.jar");
            classpathElements.add("lib/apiguardian-api-1.1.2.jar"); // JUnit 的依赖
            classpathElements.add("lib/opentest4j-1.3.0.jar");       // JUnit 的依赖
// 拼接 classpath（注意操作系统分隔符）
            String classpath = String.join(System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":", classpathElements);
        // 设置编译选项
        List<String> options = new ArrayList<>();
        options.add("-classpath");
        options.add(classpath);
        options.add("-d");
        options.add("target/test-classes");

        // 创建编译器和文件管理器
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null);

        JavaFileObject testClassFile = fileManager.getJavaFileObjectsFromStrings(Collections.singletonList(testClassPath)).iterator().next();

        // 创建编译任务
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, Arrays.asList(testClassFile));

        // 执行编译任务
        boolean result = task.call();

        // 输出编译结果
        if (result) {
            System.out.println("测试类编译成功！");
        } else {
            System.out.println("测试类编译失败！");
            diagnostics.getDiagnostics().forEach(diagnostic -> {
                System.out.println("错误: " + diagnostic.getMessage(null) +
                        " 在行 " + diagnostic.getLineNumber() +
                        " 列 " + diagnostic.getColumnNumber());
            });
        }

        // 关闭文件管理器
        fileManager.close();

        return result;
    } catch (Exception e) {
        e.printStackTrace();
        System.out.println("测试类编译失败！发生异常: " + e.getMessage());
        return false;
    }
    }/*

    //16.读取待测试类-待实现
    private static String extractClassName(File javaFile) {
        try {
            List<String> lines = Files.readAllLines(javaFile.toPath());
            for (String line : lines) {
                if (line.contains("public class")) {
                    String className = line.split("public class ")[1].split("\\s+")[0].replaceAll("\\{", "").trim();
                    return className;
                }
            }
        } catch (IOException e) {
            System.err.println("读取文件时出错: " + javaFile.getAbsolutePath());
            e.printStackTrace();
        }
        return null;
    }

    // 添加 Timer 实例
    private final Timer timer = new Timer();

    public static void main(String[] args) throws IOException {
        String apiKey = "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";
        //读取待测试列表
        String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\codeing";
        List<String> classNames = new ArrayList<>();

        // 遍历指定目录下的所有Java文件-待实现
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




        String sourceCodePath = "src/main/java/codeing/Example.java";
        TestCore testCoreClass = new TestCore(apiKey);
        testCoreClass.className = "Example"; // 设置当前处理的类名
        String sourceCodeFile=testCoreClass.readSourceCode(sourceCodePath);
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
        System.out.println("起始时间："+startTime+"\n");

        long analyzeStart = System.currentTimeMillis();

        // 初始化 Timer
        testCoreClass.timer.start("整体");
        System.out.println("分析起始时间："+analyzeStart+"\n");
        //分析开始
        testCoreClass.timer.start("分析");
        String analyzePromt=testCoreClass.analyzeSourceCode(sourceCodeFile);

        // 从分析结果中提取类信息
        Map<String, String> classInfo = testCoreClass.extractClassInfo(analyzePromt);
        analyzeSuccess = true;
        analyzeEndTime = System.currentTimeMillis();
        System.out.println("分析结束时间："+analyzeEndTime+"\n");
        System.out.println("分析总耗时："+(analyzeEndTime-analyzeStart));
        analyzeTokenCount = testCoreClass.tokenCount;
        if (analyzePromt == null || analyzePromt.isEmpty()) {
            System.out.println("分析阶段失败：LLM 未返回有效结果");
            return;
        }
        //分析结束
        testCoreClass.timer.end("分析");
        //生成
        //  String testCode=testCoreClass.generateTestCode(sourceCodeFile,analyzePromt);
        //生成开始
        testCoreClass.timer.start("生成");
        String testCode=testCoreClass.generateTestCode2(sourceCodeFile,analyzePromt,conversationHistory);

        generateSuccess = true;
        generateEndTime = System.currentTimeMillis();
        generateTokenCount = testCoreClass.tokenCount - analyzeTokenCount;
        if (testCode == null || testCode.isEmpty()) {
            System.out.println("生成阶段失败：LLM 未返回有效结果");
            return;
        }
        //生成结束
        testCoreClass.timer.end("生成");
        //验证开始
        testCoreClass.timer.start("验证");
        String validateResult=testCoreClass.validateTestCode(testCode);

        validateSuccess = validateResult.contains("全部验证通过");
        validateEndTime = System.currentTimeMillis();
        validateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount;
        if (validateResult == null || validateResult.isEmpty()) {
            System.out.println("验证阶段失败：LLM 未返回有效结果");
            return;
        }
        //验证结束
        testCoreClass.timer.end("验证");
        if (!validateSuccess) {
            validateFailReason = validateResult;
        }
        if (validateSuccess) {
            //评估开始
            testCoreClass.timer.start("评估");
            //评估

            String evaluateResult=testCoreClass.evaluateTestCode(testCode);

            evaluateSuccess = true;
            evaluateEndTime = System.currentTimeMillis();
            evaluateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount;
            evaluateCoverageData = evaluateResult;
            if (evaluateResult == null || evaluateResult.isEmpty()) {
                System.out.println("评估阶段失败：LLM 未返回有效结果");
                return;
            }
            //评估结束
            testCoreClass.timer.end("评估");
            //如果覆盖率未达到期望 优化测试代码

            //创建测试类
            //  createTestClass(testCode);
            createTestClass2(testCode, classInfo);
            overallSuccess = true;

            TestClassCompiler testClassCompiler=new TestClassCompiler();
            boolean compileResult=testClassCompiler.compileTestClass(classInfo);
            // 编译测试类
            //  boolean compileResult = testCoreClass.compileTestClass(classInfo);
            if (compileResult) {
                System.out.println("测试类编译成功！");
            } else {
                System.out.println("测试类编译失败！");
            }
        }
        else
        {
            //修复开始
            testCoreClass.timer.start("修复");
            fixAttempt = 1;
            while (fixAttempt <= 3) {
                //修复

                String fixResult = testCoreClass.fixTestCode(validateResult, testCode);
                fixSuccess = !fixResult.equals("null");
                fixEndTime = System.currentTimeMillis();
                fixTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount;
                if (fixResult == null || fixResult.isEmpty()) {
                    System.out.println("修复阶段失败：LLM 未返回有效结果");
                    break;
                }
                if (fixSuccess) {
                    String evaluateResult = testCoreClass.evaluateTestCode(testCode);
                    evaluateSuccess = true;
                    evaluateEndTime = System.currentTimeMillis();
                    evaluateTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount - fixTokenCount;
                    if (evaluateResult == null || evaluateResult.isEmpty()) {
                        System.out.println("评估阶段失败：LLM 未返回有效结果");
                        break;
                    }
                    // 假设 evaluateResult 包含覆盖率数据，如 "整体代码覆盖率：80%，分支覆盖率：70%，行覆盖率：85%"
                    evaluateCoverageData = evaluateResult;
                    //   testCoreClass.createTestClass(testCode);
                    testCoreClass.createTestClass2(testCode, classInfo);;
                    overallSuccess = true;
                    fixSuccessTimes = String.valueOf(fixAttempt);
                    break;
                } else {
                    System.out.println("第 " + fixAttempt + " 次修复失败：" + fixResult + "over\n");
                    fixFailReason += "第 " + fixAttempt + " 次修复失败，原因是：" + fixResult + "\n";
                    testCoreClass.failedClasses.add(testCoreClass.className);
                    fixAttempt++;
                }
            }
            //修复结束
            testCoreClass.timer.end("修复");
        }
        // 记录测试代码生成过程的信息
        testCoreClass.logTestCodeGenerationInfo(analyzeSuccess, generateSuccess, validateSuccess,
                fixSuccess, evaluateSuccess, overallSuccess,analyzeStart,analyzeEndTime, generateEndTime,
                validateEndTime, fixEndTime, evaluateEndTime, analyzeTokenCount, generateTokenCount,
                validateTokenCount, fixTokenCount, evaluateTokenCount, validateFailReason,
                fixAttempt, fixFailReason, fixSuccessTimes, evaluateCoverageData);
        testCoreClass.closeClient();


    }
}*/



//0521运行器版本
/*package zju.cst.aces.runner;

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

    *//**
     * 执行指定类中的所有测试方法
     * @param testClass 要执行的测试类
     *//*
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

    *//**
     * 从测试代码字符串运行测试
     * @param testCode 测试代码字符串
     * @param classInfo 包含类信息的Map，应包含"包路径"和"类名"
     * @return 测试是否全部通过
     *//*
    public boolean runTests(String testCode, Map<String, String> classInfo) {
        try {
            // 从classInfo中获取类信息
            String packageName = classInfo.get("包路径");
            String className = classInfo.get("类名");
            String testClassName = className + "Test";
            String fullTestClassName = packageName + "." + testClassName;

            // 编译测试代码
            Class<?> testClass = compileTestCode(testCode, fullTestClassName);

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

    *//**
     * 编译测试代码字符串并返回Class对象
     *//*
    private Class<?> compileTestCode(String testCode, String className) throws Exception {
        // 创建Java文件对象
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

        // 创建内存中的Java源文件
        JavaFileObject fileObject = new JavaSourceFromString(className, testCode);

        // 设置编译选项
        List<String> options = Arrays.asList(
                //      "--release", "11",  // 指定Java版本为11
                "-d", tempDir.toString(),
                "-classpath", getClasspath()
        );

        // 执行编译
        JavaCompiler.CompilationTask task = compiler.getTask(
                null, null, diagnostics, options, null, Arrays.asList(fileObject));

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

    *//**
     * 获取类路径（包含JUnit依赖）
     *//*
    private String getClasspath() {
        // 这里需要根据实际情况配置JUnit和项目依赖的路径
        return System.getProperty("java.class.path") +
                File.pathSeparator + "lib/junit-jupiter-api-5.10.2.jar" +
                File.pathSeparator + "lib/junit-jupiter-engine-5.10.2.jar" +
                File.pathSeparator + "lib/apiguardian-api-1.1.2.jar" +
                File.pathSeparator + "lib/opentest4j-1.3.0.jar";
    }

    *//**
     * 输出测试结果摘要
     *//*
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

    *//**
     * 输出详细的测试结果
     *//*
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

    *//**
     * 获取测试执行时间（毫秒）
     *//*
    public long getExecutionTime() {
        return endTime - startTime;
    }

    *//**
     * 获取所有测试结果
     *//*
    public List<TestResult> getResults() {
        return testResults;
    }

    *//**
     * 测试结果状态枚举
     *//*
    public enum TestStatus {
        PASSED, FAILED, SKIPPED
    }

    *//**
     * 测试结果数据结构
     *//*
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

    *//**
     * JUnit 测试执行监听器
     *//*
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

    *//**
     * 用于表示内存中Java源文件的辅助类
     *//*
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
            String testFilePath = "D:\\fx\\jafx\\chatunitest-core\\src\\test\\java\\codeing\\ExampleTest.java";

            // 从文件读取测试代码
            String testCode = readFileContent(testFilePath);

            // 配置类信息
            Map<String, String> classInfo = new HashMap<>();
            classInfo.put("包路径", "codeing");  // 包路径应与测试文件的package声明一致
            classInfo.put("类名", "Example");    // 类名应与测试文件名前缀一致（不包含Test后缀）

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

    *//**
     * 从文件读取内容
     *//*
    private static String readFileContent(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }
}*/
