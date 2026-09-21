package zju.cst.aces.runner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.*;
import zju.cst.aces.util.CodeExtractor;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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


    //读取源代码
    private static String readSourceCode(String filePath) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    // 分析源代码的主要方法
    public String analyzeSourceCode(String sourceCode) throws IOException {
        String analyzePrompt = "你是一个Java程序分析专家,请根据以下要求从专业角度分析该Java代码，要求如下：\n" +
                "1. **类名**：类的名称。\n" +
                "2. **方法名**：每个方法的名称。\n" +
                "3. **返回类型**：每个方法的返回类型。\n" +
                "4. **实现功能**：每个方法实现的具体功能。\n" +
                "请按照以下格式返回结果：\n" +
                "```\n" +
                "类名：[类名]\n" +
                "方法：[方法名]\n" +
                "返回类型：[返回类型]\n" +
                "功能：[功能描述]\n" +
                "```\n";
        RequestBody requestBodyA = buildRequestBody(sourceCode, analyzePrompt);
        Request requestA = buildRequest(requestBodyA);
        try (Response responseA = client.newCall(requestA).execute()) {
            if (responseA.isSuccessful() && responseA.body() != null) {
                String responseBody = responseA.body().string();
                return extractTestCode(responseBody);    //直接返回分析结果
            } else {
                throw new IOException("分析请求响应失败！" + responseA);
            }
        }
    }


    // 生成测试代码的主要方法+生成提示+分析提示+源代码
    public String generateTestCode(String sourceCode,String analyzePrompt) throws IOException {
        //创建生成提示
        String generatePrompt="You are a senior tester in Java projects, " +
                "your task is writting tests for a specific focal method in a focal class with JUnit5 framework (A focal method means a method under test).\n"+
                "这里提供了待测试类的一些基本信息：" +analyzePrompt+
                "\"You need to create a complete unit test using JUnit 5, ensuring to cover all branches. " +
                "Compile without errors, and use reflection to invoke private methods or fields if needed.\\n\" +\n" +
                "\"Please implement the test corresponding to the path according to the execution path information provided in annotations. No additional explanations required."
                + "不需要返回思考过程等额外信息，附带的待测试源代码如下：\n";
        // 构建请求体
        RequestBody requestBodyG = buildRequestBody(sourceCode,generatePrompt);
        // 构建请求
        Request requestG = buildRequest(requestBodyG);
        // 发送请求并获取响应
        try (Response responseG = client.newCall(requestG).execute()) {
            if (responseG.isSuccessful() && responseG.body() != null) {
                String responseBody = responseG.body().string();
                return extractTestCode(responseBody);                  //提取生成的测试代码
            } else {
                throw new IOException("生成请求响应失败！" + responseG);
            }
        }

    }

    // 验证测试代码的主要方法+测试代码+验证提示
    public String validateTestCode(String testCode) throws IOException {
        String validatePrompt="你是一个测试代码审查器，请你检查以下测试代码是否符合以下要求：\n"
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
        // 构建请求体
        RequestBody requestBodyR = buildRequestBody(testCode,validatePrompt);
        // 构建请求
        Request requestR = buildRequest(requestBodyR);
        // 发送请求并获取响应
        try (Response responseR = client.newCall(requestR).execute()) {
            if (responseR.isSuccessful() && responseR.body() != null) {
                String responseBody = responseR.body().string();
                String result = extractTestCode(responseBody);
                // 检查验证结果
                if (result.contains("全部验证通过")) {
                    System.out.println("2.1验证通过结果:\n" +result +"\n");
                    //验证通过将测试代码生成测试类
                    createTestClass(testCode);
                } else
                    {
                        System.out.println("2.2验证失败结果:\n" +result +"\n");
                       // String errorPrompt=extractValidationErrors(responseBody);
                        System.out.println("2.2.1当前测试代码错误内容:\n"+ result + "等待修复！\n");
                        //修复代码
                        String fixCode=fixTestCode(result,testCode);
                        System.out.println("2.2.2修复后的测试代码:\n"+ fixCode + "修复结束！\n");
                       //再次验证
                    }
                return "2.3验证阶段结束！";
               // return extractTestCode(responseBody);
            } else {
                throw new IOException("验证请求响应失败！" + responseR +"响应失败！\n");
            }
        }

    }


    // 评估测试代码的主要方法
    public String evaluateTestCode(String testCode) throws IOException {
        // 创建评估提示
        String evaluatePrompt = "你是一位专业的测试代码评估专家，专门负责精准评估测试代码的质量。请根据以下详细要求，对提供的测试代码进行全面的代码覆盖率分析：\n" +
                "- **整体代码覆盖率**：计算测试代码覆盖被测试代码中语句的比例，以百分比形式精确呈现，格式为【整体代码覆盖率】：【XX%】。\n" +
                "- **分支覆盖率**：分析测试代码对被测试代码中所有分支（如 `if-else`、`switch` 等条件语句）的覆盖情况，给出分支被执行的比例，格式为【分支覆盖率】：【XX%】。\n" +
                "- **行覆盖率**：确定测试代码执行到被测试代码中每一行的比例，以百分比形式展示，格式为【行覆盖率】：【XX%】。\n" +
                "\n" +
                "请严格按照上述格式输出结果，不包含任何额外的解释、注释或思考过程。测试代码如下：";
        // 构建请求体
        RequestBody requestBodyE = buildRequestBody(testCode, evaluatePrompt);
        // 构建请求
        Request requestE = buildRequest(requestBodyE);

        // 发送请求并获取响应
        try (Response responseE = client.newCall(requestE).execute()) {
            if (responseE.isSuccessful() && responseE.body() != null) {
                String responseBodyE = responseE.body().string();
                return extractTestCode(responseBodyE);
            } else {
                throw new IOException("评估请求响应失败！" + responseE);
            }
        }
    }



    // 修复测试代码的主要方法
    public String fixTestCode(String errorPrompt, String testCode) throws IOException {
        // 创建修复提示
        String fixPrompt = "你是一个高级测试代码修复师，根据以下信息对测试代码进行修复：\n" +
                "错误信息：\n" + errorPrompt + "\n" +
                "请根据错误信息提示修复测试代码，确保修复后的代码满足所有验证要求。" +
                "如果修复失败就直接返回【修复失败】以及修复失败的原因，仅返回错误；否则返回【修复成功】以及修复成功的测试代码，仅返回代码"+
                "不需要额外的解释、注释和思考过程。"+
                "测试代码如下：";
        // 构建请求体
        RequestBody requestBodyF = buildRequestBody(testCode, fixPrompt);
        // 构建请求
        Request requestF = buildRequest(requestBodyF);

        // 发送请求并获取响应
        try (Response responseF = client.newCall(requestF).execute()) {
            if (responseF.isSuccessful() && responseF.body() != null) {
                String responseBodyF = responseF.body().string();
                String fixresultF=extractTestCode(responseBodyF);
                //如果修复成功返回
                if (responseBodyF.contains("修复成功")) {
                    // 如果修复成功
                    System.out.println("修复成功！");
                    createTestClass(fixresultF);
                }else
                    {
                        // 如果修复失败
                        System.out.println("修复失败，原因如下：");
                        System.out.println(extractTestCode(responseBodyF));
                    }
                return extractTestCode(responseBodyF);
                //如果修复失败记录失败原因
            } else {
                throw new IOException("修复请求响应失败！" + responseF);
            }
        }
    }


    // 构建请求体
    private RequestBody buildRequestBody(String sourceCode,String sourcePrompt) {
        List<Map<String, String>> messages = new ArrayList<>();
        Map<String, String> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", sourcePrompt+ sourceCode);
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

    // 从响应中提取内容
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


    //创建测试类
    public static void createTestClass(String passTest) {
        try {
            // 定义测试类文件的路径和名称
            String fileName = "src/main/java/zju/cst/aces/runner/javaExampleTest.java";
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
    // 提取纯净的测试代码，去除 ```java 和 ``` 标记
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

    // 关闭客户端
    public void closeClient() {
        if (client != null) {
            client.dispatcher().executorService().shutdown();
            client.connectionPool().evictAll();
        }
    }

    public static void main(String[] args) throws IOException {
        //api String apiKey = "sk-o7C539636d3cb6ec90bc084880c104d25926fd8af0bilWd8";
        String apiKey = "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";
        ChatUnitestCore chatUnitestCore = new ChatUnitestCore(apiKey);
    //    String sourceCode = "public class Example {\n    public int add(int a, int b) {\n        return a + b;\n    }\n}";
        //待测试代码路径
        String sourceCodePath = "src/main/java/zju/cst/aces/runner/Example.java";
        String sourceCode = chatUnitestCore.readSourceCode(sourceCodePath);
        //分析待测试代码
        String analyzeCode=chatUnitestCore.analyzeSourceCode(sourceCode);
       // System.out.println("1Analysis Test Code :\n" +analyzeCode);
        //生成测试代码
        String testCode = chatUnitestCore.generateTestCode(sourceCode,analyzeCode);
      //  System.out.println("2Generated Test Code:\n" + testCode);
        try {

            if(testCode != null && !testCode.trim().isEmpty()) {
                //验证测试代码
                String validateCode=chatUnitestCore.validateTestCode(testCode);
            }
            else { }

            // 评估测试代码
            String evaluateCode = chatUnitestCore.evaluateTestCode(testCode);

        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            chatUnitestCore.closeClient();
        }
    }
}
