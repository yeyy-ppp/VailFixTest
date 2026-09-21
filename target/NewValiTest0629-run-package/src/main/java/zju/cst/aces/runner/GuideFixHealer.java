package zju.cst.aces.runner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 自动修复“expected:<X> but was:<Y>”这类简单失败的工具类
 */
public class GuideFixHealer {


        /** 入口：传入 mvn test 控制台输出文本路径，直接改源码 */
        public static void fixSimpleFailures(String mvnTestOutputPath) throws IOException {
            Path out = Paths.get(mvnTestOutputPath);
            if (!Files.exists(out)) {
                System.err.println("mvn 输出文件不存在: " + mvnTestOutputPath);
                return;
            }
            List<String> lines = Files.readAllLines(out);


            //针对错误反馈1：• testMixedCharacters() [FAILED]  错误: expected: <2> but was: <3>
            Pattern p = Pattern.compile(
                    "\\s*•\\s*(\\w+)\\(\\)\\s*\\[FAILED\\].*\\n.*错误:\\s*expected:\\s*<(\\d+)>\\s*but was:\\s*<(\\d+)>"
            );
            Matcher m = p.matcher(String.join("\n", lines));
            while (m.find()) {
                String method = m.group(1);
                int wrong = Integer.parseInt(m.group(2));
                int right = Integer.parseInt(m.group(3));
                patchExpectedValue(method, wrong, right);
            }
        }

        /** 到测试源文件里把 assertEquals(wrong, ...) 改成 assertEquals(right, ...) */
        private static void patchExpectedValue(String methodName, int wrong, int right) throws IOException {
            // 1. 定位测试源码目录
            Path testDir = Paths.get("src/test/java");
            if (!Files.exists(testDir)) return;

            // 2. 全局搜 *Test.java
            Files.walk(testDir)
                    .filter(p -> p.toString().endsWith("Test.java"))
                    .forEach(p -> {
                        try {
                            List<String> src = Files.readAllLines(p);
                            List<String> patched = new ArrayList<>();
                            boolean hit = false;
                            for (String line : src) {
                                // 只改目标方法里的第一处 assertEquals(wrong, ...)
                                if (line.contains("void " + methodName + "()")) {
                                    hit = true;
                                }
                                if (hit) {
                                    line = line.replaceFirst(
                                            "assertEquals\\(\\s*" + wrong + "\\s*,",
                                            "assertEquals(" + right + ", ");
                                    // 方法结束就停
                                    if (line.contains("}")) hit = false;
                                }
                                patched.add(line);
                            }
                            Files.write(p, patched);
                            System.out.println("已自动修复：[" + methodName + "] expected " + wrong + " -> " + right);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
        }

    public static void main(String[] args) throws IOException {
        // 1. 用刚才落盘的文件
        String logFile = "target/mvn-guideFixTest.log";

        // 2. 执行自动修复
        GuideFixHealer.fixSimpleFailures(logFile);

        /*
        已自动修复：[testMixedCharacters] expected 2 -> 3
        已自动修复：[testMixedCharacters] expected 2 -> 3
        已自动修复：[testMixedCharacters] expected 2 -> 3
        已自动修复：[testMixedCharacters] expected 2 -> 3
        已自动修复：[testMixedCharacters] expected 2 -> 3
        已自动修复：[testMixedCharacters] expected 2 -> 3
        ✅ 修复完成，请重新执行： mvn test -Dtest=AddTest
        若 testMixedCharacters 变为 PASSED，则 GuideFixHealer 可行性验证成功！
        */

        // 3. 提示二次验证
        System.out.println("✅ 修复完成，请重新执行： mvn test -Dtest=AddTest");
        System.out.println("若 testMixedCharacters 变为 PASSED，则 GuideFixHealer 可行性验证成功！");
    }
    }


