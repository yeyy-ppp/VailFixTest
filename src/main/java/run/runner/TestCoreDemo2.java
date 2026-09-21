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

//核心代码
public class TestCoreDemo2 {
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
    // 分析提示
   /* private static final String analyzeCodePromt=
            "你是一位Java程序分析专家，请根据以下要求从专业角度分析给定的Java代码，除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等）：\n" +
                    "1. **包路径**：识别类的完整包路径。\n" +
                    "2. **类名**：类的名称。\n" +
                    "3. **序号**：类中包含多方法名时，自动为方法编号（从1开始）。\n"+
                    "4. **方法名**：列出每个方法的名称。\n" +
                    "5. **返回类型**：列出每个方法的返回类型。\n" +
                    "6. **功能描述**：简洁清晰地描述每个方法的核心功能。\n" +
                    "请严格按照以下格式输出：\n" +
                    "```\n" +
                    "包路径：[包路径]\n" +
                    "类名：[类名]\n" +
                    "序号：[序号]\n"+
                    "方法名：[方法名]\n" +
                    "返回类型：[返回类型]\n" +
                    "功能描述：[功能描述]\n" +
                    "```\n";*/
    // 分析提示（优化后：类级信息仅输出一次，方法信息按序号列出）
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
                    "5. 正确设置包路径与包导入；禁止使用 `org.junit.jupiter.params` 包内容。\n\n" +
                    "请直接输出完整的测试代码，不包含任何注释、解释或思考过程。以下为待测试的源代码：\n";

    //验证提示
    private static final String validatePrompt=
            "你是测试代码审查专家，请检查以下测试代码是否符合如下要求：\n" +
                    "1. **导入语句**：检查是否导入了正确的包。其中待测试类由系统导入，不需要手动导入待测试类。\n" +
                    "2. **测试逻辑完整性**：检查正常路径、边界情况和异常处理逻辑覆盖情况。\n" +
                    "3. **语法正确性**：检查是否存在语法或拼写错误。\n" +
                    "4. **编译正确性**：检查是否存在可能导致编译失败的问题。\n" +
                    "5. **运行时健壮性**：检查是否存在可能导致运行时错误的问题。\n" +
                    "6. **数值正确性**：假设被测方法已给出实现，手工执行一遍测试用例，若expected值与实际结果不符，视为运行时错误，按格式反馈。\n" +
                    "请逐项审查，并判断是否“通过验证”或“不通过验证”。格式如下：\n" +
                    "- 若全部验证通过，请返回：【全部验证通过】。\n" +
                    "- 若验证未通过，请使用如下格式反馈问题：\n" +
                    "```\n" +
                    "该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[详细描述]\n" +
                    "```\n" +
                    "请严格按照行号注释报告错误位置，例如：错误位置：[11]。除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等）。\n";

    //修复提示
    private static final String fixPrompt =
            "你是一名专业的java测试代码修复工程师，请仅对下面代码中【错误位置】所示的片段进行最小化修改，使其通过验证\n" +
                    " 除【修改后片段】外不要返回任何解释、注释、行号或其他内容。\n "+
                    "错误信息：{errorPrompt}\n"+
                    "返回格式：\n" +
                    "- 若修复失败，返回：【修复失败】，并附原因，格式：该测试代码在修复[修复项]时失败，错误位置：[行号]，错误原因：[详细描述]\\n\"。\n" +
                    "- 若修复成功，返回：【修复成功】，和修改后的测试代码。\n" +
                    "不需要返回解释、注释、思考过程、分析思路等内容，除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等）。" +
                    "如果第三次修复还是失败，就将错误的代码位置或整个测试方法删除或注释掉，以帮助成功构建编译测试类。";


    //截取错误片段-原地修复测试代码


    public TestCoreDemo2(String apiKey) {
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
            classInfo.put("类名",   headerMatcher.group(2).trim());
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


    //7.LLM生成测试代码 out
    public String generateTestCode(String sourceCodeFile,String analyzePromt) throws IOException {
        String testCode=sendRequestAndGetResponse0(sourceCodeFile, analyzePromt+generatePrompt);
        System.out.println("生成测试代码：\n"+testCode + "over\n");
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
                "\n注意：以下代码已添加行号注释，请以行号注释为准报告错误位置。\n" +
                "```java\n" + numbered + "\n```";
        String validateResult = sendRequestAndGetResponse0("", promptWithNum);
        System.out.println("validateTestCode2测试代码验证结果：\n" + validateResult + "over\n");
        return validateResult;
    }


    //8.LLM验证测试代码
    public String validateTestCode(String testCode) throws IOException {
        String validateResult=sendRequestAndGetResponse0(testCode, validatePrompt);
        System.out.println("validateTestCode测试代码验证结果：\n"+validateResult + "over\n");
        return validateResult;
    }
    //10.LLM修复测试代码
    public String fixTestCode(String errorPrompt, String testCode) throws IOException {
        int fixNumber = 1;
        boolean fixSuccess = false;
        boolean validateSuccess = false;

        while (!validateSuccess && fixNumber <= 3) {
            // 构建修复提示
            String fixRequestPrompt = errorPrompt;
            String fixResult = sendRequestAndGetResponse0(testCode, fixRequestPrompt);
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
                errorPrompt = "\n修复尝试 " + fixNumber + " 失败，原因是：\n" + fixResult;
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
    //16.获取测试路径运行测试代码
    private static String getTestFilePath(Map<String, String> classInfo) {
        // 项目根目录路径（根据实际情况调整）
        String projectRoot = System.getProperty("user.dir");

        // 构建测试文件路径
        String basePackagePath = classInfo.get("包路径").replace(".", "/");
        String testClassName = classInfo.get("类名") + "Test";
        String testFilePath = Paths.get(projectRoot, "src", "test", "java", basePackagePath, testClassName + ".java").toString();

        return testFilePath;
    }
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
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
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


    // 20.添加 Timer 实例
    private final Timer timer = new Timer();



    public static void main(String[] args) throws IOException {
        String apiKey = "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";
        String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\codeing";
        String sourceCodePath = "src/main/java/codeing/AddExample.java";
        String targetPackage = "codeing";
        TestCoreDemo2 testCoreClass = new TestCoreDemo2(apiKey);
        testCoreClass.className = "AddExample"; // 设置当前处理的类名
        String logFilename="AddExampleTest_0105.txt";
        mainM(apiKey,testCoreClass,sourceCodeDir,sourceCodePath,targetPackage,logFilename);
    }



    //增加一个判别测试类是否已存在或以分析的功能，对于已编写好的测试类就不处理，对于已分析过的测试类就不分析
    //以待测试类名＋Analysis 保留每次分析结果在程序中A路径下面 如果没有A就新建A
    //和上面同样逻辑，对于已验证或已修复的测试类的验证结果和修复结果保存，方便下次LLM查看，但是这个有了并不影响再次操作，即再次操作就在原文件下面续写，相当于记录日志
    //所有记录耗时、时间开始、结束的都不要了
    //


    public static void mainM(String apiKey,TestCoreDemo2 testCoreClass,String sourceCodeDir,String sourceCodePath,String targetPackage,String logFilename) throws IOException {
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
        Map<String, String> classInfo = testCoreClass.extractClassInfo3(analyzePromt);
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
            TestClassCompiler testClassCompiler=new TestClassCompiler();
            boolean compileResult=testClassCompiler.compileTestClass(classInfo);
            // 编译测试类
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
                    String DetailedResults=runner.getDetailedResults();
                    if (testResult) {
                        System.out.println("测试类覆盖率计算开始：");
                        String mvnTestResult1 = testCoreClass.executeMavenCommand("test");
                        System.out.println("mvn test 结果1：\n" + mvnTestResult1);
                        //规则修复
                        // 把文本落盘到文件
                        // Path mvnlogFile = Paths.get("target/mvn-guideFixTest.log");
                        Path mvnlogFile = Paths.get("target","mvn-"+ classInfo.get("类名") + "Test.log");
                        Files.write(mvnlogFile,DetailedResults.getBytes(StandardCharsets.UTF_8));
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
        }
        else
        {
            String errorPrompt = testCoreClass.extractErrorPrompt(validateResult);
            // 构建修复提示
            String fixRequestPrompt = fixPrompt.replace("{errorPrompt}", errorPrompt);
            //修复开始
            testCoreClass.timer.start("修复");
            fixAttempt = 1;
            while (fixAttempt <= 3) {
                //修复
                String fixResult = testCoreClass.fixTestCode(fixRequestPrompt, testCode);
                // fixSuccess = !fixResult.equals("null");
                fixEndTime = System.currentTimeMillis();
                fixTokenCount = testCoreClass.tokenCount - analyzeTokenCount - generateTokenCount - validateTokenCount;
                if (fixResult != null && !fixResult.equals("null")) {fixSuccess=true;}
                if (fixResult == null || fixResult.isEmpty()) {
                    System.out.println("修复阶段失败：LLM 未返回有效结果");
                    break;
                }
                if (fixSuccess) {
                    testCode=fixResult;
                    testCoreClass.createTestClass2(testCode, classInfo);;
                    overallSuccess = true;
                    try {
                        Thread.sleep(5000); // 等待 5 秒
                    } catch (InterruptedException e) {
                        System.err.println("等待过程中被中断：" + e.getMessage());
                    }
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
                            String DetailedResults=runner.getDetailedResults();
                            if (testResult) {
                                System.out.println("所有测试通过！");
                                String mvnTestResult1 = testCoreClass.executeMavenCommand("test");
                                //规则修复
                                // 把文本落盘到文件
                                Path mvnlogFile = Paths.get("target","mvn-"+ classInfo.get("类名") + "Test.log");
                                Files.write(mvnlogFile, DetailedResults.getBytes(StandardCharsets.UTF_8));
                                GuideFixHealer.fixSimpleFailures(mvnlogFile.toString());
                                String mvnTestResult2 = testCoreClass.executeMavenCommand("test");
                                System.out.println("mvn test 结果：\n" + mvnTestResult2);
                                System.out.println("mvn test 结果：\n" + mvnTestResult2);
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
                fixAttempt, fixFailReason, fixSuccessTimes, evaluateCoverageData,logFilename);
        testCoreClass.closeClient();
        testCoreClass.timer.end("套件");
    }
}
