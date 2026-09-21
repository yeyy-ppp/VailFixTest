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
import zju.cst.aces.runner.CoverageEvaluator;

//核心代码
public class TestCore {
    private static final String API_URL="https://openrouter.ai/api/v1/chat/completions";
    //private static final String MODEL= "deepseek/deepseek-r1:free";
    //private static final String MODEL= "openai/gpt-4o-mini";
    private static final String MODEL= "deepseek/deepseek-r1";
    //private static final String API_URL = "https://api.gptsapi.net/v1/chat/completions";
    //private static final String MODEL = "gpt-4o-mini";
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
    private String className; // 当前处理的类名
    // 分析提示
    private static final String analyzeCodePromt=
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
                    "```\n";
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


    private static final String generatePrompt2=
            "根据以下待测试类的信息生成测试代码：\n" +
                    "包路径：{packagePath}\n" +
                    "类名：{className}\n" +
                    "方法：{methodName}\n" +
                    "返回类型：{returnType}\n" +
                    "功能描述：{functionDescription}\n" +
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
                    "除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等），以下是待验证的测试代码：\n";
    //评估提示
    private static final String evaluatePrompt =
            "你是一位专业的测试覆盖率评估专家，请基于以下要求分析测试代码质量：\n\n" +
                    "1. 【指令覆盖率】：测试代码执行的字节码指令占总指令的百分比。\n" +
                    "2. 【分支覆盖率】：覆盖的条件语句分支占总分支的百分比。如无分支，返回【分支覆盖率】：【-】。\n" +
                    "3. 【复杂度】：被测方法的圈复杂度（Cyclomatic Complexity）。\n" +
                    "4. 【行覆盖率】：被执行源代码行占总代码行的百分比。\n" +
                    "5. 【方法覆盖率】：被测试的方法占总方法数量的百分比。\n" +
                    "6. 【类覆盖率】：被测试的类占项目总类数量的百分比。\n\n" +
                    "请严格按照以下格式输出，不包含任何解释性说明，除了给定的格式要求不要返回其他内容（包括注释、解释、思考过程等）：\n" +
                    "【指令覆盖率】：【XX%】\n" +
                    "【分支覆盖率】：【XX%】\n" +
                    "【复杂度】：【X】\n" +
                    "【行覆盖率】：【XX%】\n" +
                    "【方法覆盖率】：【XX%】\n" +
                    "【类覆盖率】：【XX%】\n\n" +
                    "以下是待评估的测试代码：\n";


    //优化提示
    private static final String optimizePrompt =
            "你是一位高级Java测试工程师，任务是优化当前测试代码以提高覆盖率。\n\n" +
                    "请根据以下要求对测试代码进行优化：\n" +
                    "1. 分析当前测试结果，识别未覆盖的分支和代码行。\n" +
                    "2. 添加新的测试用例以补足缺失路径。\n" +
                    "3. 改进现有用例以提升测试效率。\n" +
                    "4. 保证优化后的测试代码可无错误编译并通过测试。\n\n" +
                    "以下为最新测试结果和原始测试代码，请输出优化后的完整测试代码，不需要任何注释或说明：\n" +
                    "[TEST RESULTS]\n";

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






    public TestCore(String apiKey) {
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


    private String sendRequestAndGetResponse0(String content, String prompt) throws IOException {
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

        /* ---------- 2. 循环提取每组方法 ---------- */
        // 正则：序号、方法名、返回类型、功能描述
        // 允许每组之间有空行，允许字段值里出现换行（?s 开启 DOTALL）
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
        /*String methodName=classInfo.get("方法名");
        String returnType=classInfo.get("返回类型");
        String functionDescription=classInfo.get("功能描述");*/
/*

        // 构建生成测试代码的提示
        String newGprompt = generatePrompt
                .replace("{packagePath}", packagePath)
                .replace("{className}", className)
                .replace("{methodName}", methodName)
                .replace("{returnType}", returnType)
                .replace("{functionDescription}", functionDescription);*/
        String testCode = sendRequestAndGetResponse3(sourceCodeFile, newGprompt, conversationHistory);
        System.out.println("生成测试代码：\n" + testCode + "over\n");
        return testCode;
    }

    //8.LLM验证测试代码
    public String validateTestCode(String testCode) throws IOException {
        String validateResult=sendRequestAndGetResponse0(testCode, validatePrompt);
        System.out.println("validateTestCode测试代码验证结果：\n"+validateResult + "over\n");
        return validateResult;
    }

    //9.LLM评估测试代码
    public String evaluateTestCode(String testCode) throws IOException {
        String evaluateResult=sendRequestAndGetResponse0(testCode, evaluatePrompt);
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


    /*//15.编译器执行验证代码
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
    }*/

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


    //19.优化
    /**
     * 优化测试代码
     * @param testCode 测试代码
     * @param testResults 测试结果
     * @return 优化后的测试代码
     * @throws IOException 如果生成过程中出现I/O错误
     */
  /*  public String optimizeTestCode(String testCode, String testResults) throws IOException {
        // 构建优化提示
        String prompt = optimizePrompt.replace("[TEST RESULTS]", testResults);
        String optimizeResult = sendRequestAndGetResponse0(testCode, prompt);
        System.out.println("优化后的测试代码：\n" + optimizeResult + "over\n");
        return optimizeResult;
    }*/
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

    /**
     * 检查覆盖率是否满足要求
     * @param instructionCoverage 整体代码覆盖率
     * @param branchCoverage 分支覆盖率
     * @param lineCoverage 行覆盖率
     * @return 是否满足要求
     */
    private static boolean isCoverageSufficient(double instructionCoverage, double branchCoverage, double lineCoverage) {
        // 根据需求设置覆盖率阈值
        final double INSTRUCTION_COVERAGE_THRESHOLD = 50.0;
        final double BRANCH_COVERAGE_THRESHOLD = 50.0;
        final double LINE_COVERAGE_THRESHOLD = 50.0;

        return instructionCoverage >= INSTRUCTION_COVERAGE_THRESHOLD &&
                branchCoverage >= BRANCH_COVERAGE_THRESHOLD &&
                lineCoverage >= LINE_COVERAGE_THRESHOLD;
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


    //21.更新测试代码
    /**
     * 更新现有测试类的内容
     * @param optimizedTestCode 优化后的测试代码（由LLM直接返回）
     * @param classInfo 类信息
     */
    public static void updateTestClass(String optimizedTestCode, Map<String, String> classInfo) {
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
            String testClassName = className + "Test";
            String testClassPath = "src/test/java" + basePackagePath + "/" + testClassName + ".java";

            // 检查文件是否存在
            File testFile = new File(testClassPath);

            // 创建目录（如果不存在）
            java.nio.file.Path directory = Paths.get(testFile.getParent());
            if (!Files.exists(directory)) {
                Files.createDirectories(directory);
            }

            // 提取纯代码内容（去除可能的标记）
            String pureCode = extractPureCode(optimizedTestCode);

            // 写入优化后的测试代码
            try (FileWriter writer = new FileWriter(testFile)) {
                writer.write(pureCode);
            }

            System.out.println("测试类更新成功：" + testClassPath);
        } catch (IOException e) {
            System.err.println("更新测试类时出错：" + e.getMessage());
        }
    }


    //规则修复


    public static void main(String[] args) throws IOException {
    //String apiKey = "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";
    //0526 String apiKey = "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";
    //gpt String apiKey = "sk-o7C539636d3cb6ec90bc084880c104d25926fd8af0bilWd8";
    //free2 String apiKey = "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";
    //DeepSeek: R1 R1_Cost1020
        String apiKey = "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";

        //gpt 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
    String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\codeing";
    String sourceCodePath = "src/main/java/codeing/Add.java";
    String targetPackage = "codeing";
    TestCore testCoreClass = new TestCore(apiKey);
    testCoreClass.className = "Add"; // 设置当前处理的类名
    String logFilename="Add_1020_MutiTestAddGuideDemo_1.txt";
    mainM(apiKey,testCoreClass,sourceCodeDir,sourceCodePath,targetPackage,logFilename);
    }

    public static void mainF(String[] args) throws IOException {
        String apiKey = "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";
        String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\help";
        String testClassDir = "D:\\fx\\jafx\\chatunitest-core\\src\\test\\java\\help";
        String targetPackage = "help";
        String logFilenamePrefix = "cli_Help_test_errorTest_deepseek_";

        TestCore testCoreClass = new TestCore(apiKey);
        testCoreClass.className = ""; // 初始化为空

        // 遍历 cli 目录下的所有 .java 文件
        File cliDir = new File(sourceCodeDir);
        if (cliDir.exists() && cliDir.isDirectory()) {
            File[] javaFiles = cliDir.listFiles((dir, name) -> name.endsWith(".java") && !name.endsWith("Test.java"));

            if (javaFiles != null) {
                for (File javaFile : javaFiles) {
                    String className = javaFile.getName().replaceAll(".java", "");
                    String sourceCodePath = "src/main/java/help/" + className + ".java";
                    String logFilename = logFilenamePrefix + className + ".txt";

                    // 检查测试类是否已经存在
                    String testClassName = className + "Test";
                    File testClassFile = new File(testClassDir, testClassName + ".java");

                    if (!testClassFile.exists()) {
                        // 如果测试类不存在，执行 mainM 函数
                        testCoreClass.className = className;
                        System.out.println("当前正在为"+testCoreClass.className+" : "+className+"生成测试代码！\n");
                        mainM(apiKey, testCoreClass, sourceCodeDir, sourceCodePath, targetPackage, logFilename);
                        System.out.println("测试类不存在！"+className);
                    }else {
                        System.out.println("测试类已存在：" + testClassName);
                    }
                }
            }
        }
    }

    public static void mainM(String apiKey,TestCore testCoreClass,String sourceCodeDir,String sourceCodePath,String targetPackage,String logFilename) throws IOException {
    //1 public static void main(String[] args) throws IOException {
        //1 String apiKey = "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";
        //读取待测试列表
        //String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\cli";
        //1 String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\codeing";
        List<String> classNames = new ArrayList<>();


        //输出当前待测试类名
        //System.out.println("当前正在为"+classNames+"生成测试代码！\n");

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

          //String sourceCodePath = "src/main/java/cli/AlreadySelectedException.java";
        //1 String sourceCodePath = "src/main/java/codeing/Example.java";
        //TestCore testCoreClass = new TestCore(apiKey);
        //testCoreClass.className = "AlreadySelectedException"; // 设置当前处理的类名
        //1 testCoreClass.className = "Example"; // 设置当前处理的类名

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
            try {
                Thread.sleep(5000); // 等待 5 秒
            } catch (InterruptedException e) {
                System.err.println("等待过程中被中断：" + e.getMessage());
            }
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
                        //暂时不优化
                      /*  coverageSufficient=true;
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
                        }*/

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
                    //String fixResult = testCoreClass.fixTestCode(validateResult, testCode);
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
                                    //暂时不优化
                                    /*coverageSufficient=true;
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
                                    }*/

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
