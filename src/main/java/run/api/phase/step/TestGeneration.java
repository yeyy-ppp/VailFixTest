package zju.cst.aces.api.phase.step;

import zju.cst.aces.api.config.Config;
import zju.cst.aces.api.impl.ChatGenerator;
import zju.cst.aces.api.impl.PromptConstructorImpl;
import zju.cst.aces.api.impl.RepairImpl;
import zju.cst.aces.api.impl.obfuscator.Obfuscator;
import zju.cst.aces.dto.*;
import zju.cst.aces.prompt.PromptGenerator;
import zju.cst.aces.runner.solution_runner.MethodRunner;
import zju.cst.aces.util.CodeExtractor;

import java.util.ArrayList;
import java.util.List;

public class TestGeneration {
    protected final Config config;
    protected PromptGenerator promptGenerator;
    protected MethodInfo methodInfo;

    public TestGeneration(Config config) {
        this.config = config;
    }

    public void setUp(PromptInfo promptInfo) {
        this.promptGenerator = new PromptGenerator(config);
        this.methodInfo = promptInfo.getMethodInfo();
    }



    //测试代码生成流程
    public void execute(PromptConstructorImpl pc) {

        //获取提示信息
        PromptInfo promptInfo = pc.getPromptInfo();
        if (promptGenerator == null) {
            setUp(promptInfo);
        }

        //记录当前轮次信息
        assert (promptInfo.getRound() != null);

        int rounds = promptInfo.getRound();
        promptInfo.addRecord(new RoundRecord(rounds));
        RoundRecord record = promptInfo.getRecords().get(rounds);
        record.setAttempt(promptInfo.getTestNum());

        //记录日志
        if (rounds == 0) {
            config.getLogger().info("Generating test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
        } else {
            config.getLogger().info("Fixing test for method < " + methodInfo.methodName + " > round " + rounds + " ...");
        }

        //生成提示并获取测试代码
        List<ChatMessage> prompt;
        String code;
        if (config.isEnableObfuscate()) {
            //代码混淆处理
            Obfuscator obfuscator = new Obfuscator(config);
            PromptInfo obfuscatedPromptInfo = new PromptInfo(promptInfo);
            obfuscator.obfuscatePromptInfo(obfuscatedPromptInfo);
            prompt = promptGenerator.generateMessages(obfuscatedPromptInfo, config.getPhaseType());
            code = generateTest(prompt, record);
            if (!record.isHasCode()) {
                promptInfo.setUnitTest("");
                return;
            }
            code = obfuscator.deobfuscateJava(code);
        } else {
            prompt = promptGenerator.generateMessages(promptInfo, config.getPhaseType());
            code = generateTest(prompt, record);
            if (!record.isHasCode()) {
                promptInfo.setUnitTest("");
                return;
            }
        }

        //包装测试方法
        if (CodeExtractor.isTestMethod(code)) {
            TestSkeleton skeleton = new TestSkeleton(promptInfo); // test skeleton to wrap a test method
            code = skeleton.build(code);
        } else {
            //规则修复
            RepairImpl repair = new RepairImpl(config, pc);
            code = repair.ruleBasedRepair(code);
        }   //代码混淆
          promptInfo.setUnitTest(code);   //验证通过
          record.setCode(code);

    }

        public String generateTest(List<ChatMessage> prompt, RoundRecord record) {

            if (MethodRunner.isExceedMaxTokens(config.getMaxPromptTokens(), prompt)) {
                config.getLogger().error("Exceed max prompt tokens: " + methodInfo.methodName + " Skipped.");
                record.setPromptToken(-1);
                record.setHasCode(false);
                return "";
            }
            config.getLogger().debug("[Prompt]:\n" + prompt);

            //调用LLM生成测试代码
            ChatResponse response = ChatGenerator.chat(config, prompt);
            String content = ChatGenerator.getContentByResponse(response);
            config.getLogger().debug("[Response]:\n" + content);
            String code = ChatGenerator.extractCodeByContent(content);


            if (code.isEmpty()) {
                config.getLogger().info("Test for method < " + methodInfo.methodName + " > extract code failed");
                record.setHasCode(false);
                return "";
            }
            record.setHasCode(true);


            int rounds = 0;
            boolean validationStatus = false;
            ValidationRecord validationRecord = new ValidationRecord();

            // 新增：验证生成的测试代码

                // 验证生成的测试代码
                validationStatus = validateTestCode(code, validationRecord); // 将 validationRecord 传递给 validateTestCode 方法
                if (validationStatus) {
                    record.setPromptToken(response.getUsage().getPromptTokens());
                    record.setResponseToken(response.getUsage().getCompletionTokens());
                    record.setPrompt(prompt);
                    record.setResponse(content);
                    config.getLogger().info("Test case for method < " + methodInfo.methodName + " > passed LLM validation.");
                    record.setHasCode(true);
                    return code;
                } else {
                    // 验证失败
                    String validationMessage = extractFailureReason(validationRecord.getValidationResponse());
                    if (validationMessage.isEmpty()) {
                        validationMessage = "Validation failed, but no specific reason provided.";
                    }
                    config.getLogger().info("Generated test code for method < " + methodInfo.methodName + " > is invalid. " + validationMessage);
                        // 触发修复流程，将失败原因和测试代码交给 LLM 修改
                    triggerRepairProcess(prompt,code, validationMessage, rounds, validationRecord);
                        // 再次生成测试代码
                        // 获取修复后的代码
                    code = validationRecord.getRepairedCode();
                 // record.setHasCode(false);
                    }

            //记录token使用情况和结果
            record.setPromptToken(response.getUsage().getPromptTokens());
            record.setResponseToken(response.getUsage().getCompletionTokens());
            record.setPrompt(prompt);
            record.setResponse(content);
            return code;
        }

        // 新增：验证测试代码的方法 具体验证实现部分
    // 调用 LLM 验证测试代码的方法 使用LLM作为审查器，验证测试代码是否符合要求//1.构建包含验证标准的提示//2.调用LLM评估测试代//3.解析LLM回复中的验证结果标识
        // 构建验证提示信息 这里的验证提示信息是比较简单的
        // 事实上生成的测试代码还应该可能包含其他错误类型，比如导入错误、版本错误、语法错误、逻辑错误
        // 以及常见编译错误、常见运行错误等等 归纳总结该如何设计这个验证提示模板
        private boolean validateTestCode(String code, ValidationRecord validationRecord) {
            List<ChatMessage> validationPrompt = new ArrayList<>();
            String validationMessage = "你是一个测试代码审查器，请你检查以下测试代码是否符合以下要求：\n"
                    + "1. **JUnit 5 格式**：测试类是否使用了 `@ExtendWith(MockitoExtension.class)` 或 `@RunWith(MockitoJUnitRunner.class)` 注解；每个测试方法是否使用了 `@Test` 注解。\n"
                    + "2. **导入语句**：是否正确导入了所有必要的类，例如 `org.junit.jupiter.api.*`, `org.mockito.*` 等。\n"
                    + "3. **Mockito 使用**：是否正确使用了 `@Mock` 和 `@InjectMocks` 注解来创建 mock 对象和注入依赖；是否在 `setUp` 方法中初始化了 mocks。\n"
                    + "4. **测试方法逻辑**：是否正确调用了目标函数并进行了断言；是否覆盖了所有关键路径，包括正常情况、边界条件和异常情况。\n"
                    + "5. **语法错误**：是否存在任何语法错误或拼写错误。\n"
                    + "6. **编译错误**：是否存在任何可能导致编译失败的问题。\n"
                    + "7. **运行时错误**：是否存在任何可能导致运行时异常的问题。\n"
                    + "请逐项给出你的判断，并指出是否“通过验证”或“不通过验证”。若不通过，请详细说明理由，并按照以下格式标记出具体的错误位置和原因：\n"
                    + "```\n"
                    + "该测试代码在验证[验证项]时失败，错误位置：[行号]，错误原因：[错误原因]\n"
                    + "```\n"
                    + "附带的测试代码如下：\n\n" + code;
            validationPrompt.add(new ChatMessage(ChatMessage.Role.USER.getValue(), validationMessage));
            // 调用 LLM 进行验证
            ChatResponse validationResponse = ChatGenerator.chat(config, validationPrompt);
            String validationContent = ChatGenerator.getContentByResponse(validationResponse);
            validationRecord.setValidationResponse(validationContent);
            boolean result = validationContent.contains("✅ 通过验证");
            return result;
        }


    // 获取验证失败的信息，然后将这个失败信息+生成的测试代码交给LLM，让LLM修改代码
    // 从验证结果中提取失败原因
    private String extractFailureReason(String validationResponse) {
        if (validationResponse == null || validationResponse.isEmpty()) {
            return "Validation failed, but no specific reason provided.";
        }

        // 尝试按预定义格式提取失败原因
        List<String> failureReasons = new ArrayList<>();
        while (true) {
            int startIdx = validationResponse.indexOf("该测试代码在验证");
            if (startIdx == -1) {
                break;
            }
            int endIdx = validationResponse.indexOf("\n", startIdx);
            if (endIdx == -1) {
                endIdx = validationResponse.length();
            }
            String reason = validationResponse.substring(startIdx, endIdx).trim();
            failureReasons.add(reason);
            validationResponse = validationResponse.substring(endIdx);
        }

        if (!failureReasons.isEmpty()) {
            // 将所有提取到的失败原因汇总
            StringBuilder reasonsSummary = new StringBuilder();
            for (String reason : failureReasons) {
                reasonsSummary.append(reason).append("\n");
            }
            return reasonsSummary.toString().trim();
        } else {
            // 如果没有提取到预定义格式的失败原因，尝试提取其他可能的错误信息
            int reasonStart = validationResponse.indexOf("理由：");
            if (reasonStart != -1) {
                String reason = validationResponse.substring(reasonStart + 3).trim();
                int endPos = reason.indexOf("\n");
                if (endPos != -1) {
                    reason = reason.substring(0, endPos);
                }
                return reason;
            } else {
                return "Validation failed, but no specific reason detected.";
            }
        }
    }

    // 触发修复流程
    private void triggerRepairProcess(List<ChatMessage> originalPrompt,String code, String validationMessage, int rounds, ValidationRecord validationRecord) {
        if (rounds >= 3) {
            // 3 轮后仍然失败，标记失败，不保存生成的测试代码
            config.getLogger().info("Failed to generate valid test code for method < " + methodInfo.methodName + " > after 3 rounds.");

            return ;
        }

        // 构造修复 Prompt
        List<ChatMessage> repairPrompt = new ArrayList<>(originalPrompt);
        String repairMessage = "请基于以下反馈信息，修复下面的测试代码，使其满足以下要求：\n"
                + "【错误说明】\n"
                + validationMessage + "\n"  // 使用提取的失败原因作为错误说明
                + "【待修复测试代码】\n"
                + "```java\n" + code + "\n```\n"  // 添加代码块标记，提高可读性
                + "【上下文信息（函数、类名）】\n"
                + "目标函数类名：" + methodInfo.className + "\n"
                + "目标函数名：" + methodInfo.methodName + "\n"
                + "请按照以下要求进行修复：\n"
                + "- 修复所有标记的错误\n"
                + "- 确保代码符合 JUnit 5 测试规范\n"
                + "- 确保测试用例覆盖正常情况、边界条件和异常情况\n";
        repairPrompt.add(new ChatMessage(ChatMessage.Role.USER.getValue(), repairMessage));

        // 调用 LLM 生成修复后的代码
        ChatResponse repairResponse = ChatGenerator.chat(config, repairPrompt);
        String repairContent = ChatGenerator.getContentByResponse(repairResponse);
        String repairedCode = ChatGenerator.extractCodeByContent(repairContent);

        if (repairedCode.isEmpty()) {
            // 修复失败，标记失败
            config.getLogger().info("Failed to repair test code for method < " + methodInfo.methodName + " >.");
            return;
        }

        // 验证修复后的代码
        boolean repairedValidationStatus = validateTestCode(repairedCode, validationRecord);
        if (repairedValidationStatus) {
            // 验证通过，记录日志
            config.getLogger().info("Repaired test case for method < " + methodInfo.methodName + " > passed validation.");
            validationRecord.setRepairedCode(repairedCode);
        } else {
            // 验证未通过，继续修复
            String newValidationMessage = extractFailureReason(validationRecord.getValidationResponse());
            triggerRepairProcess(originalPrompt,repairedCode, newValidationMessage, rounds + 1, validationRecord);
        }
    }
}
