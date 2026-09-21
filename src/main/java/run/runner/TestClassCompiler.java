package zju.cst.aces.runner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;


//编译器
public class TestClassCompiler {

    // 假设 Java 11 的 javac 路径
    // 假设 Java 11 的 javac 路径
    private static final String JAVAC_11_PATH = "D:\\defects\\strawperl\\jdk11\\bin\\javac.exe";

    //14.编译待测试代码
    // 编译待测试类
    public boolean compileTargetClass(Map<String, String> classInfo) {
        try {
            // 构造测试类的路径
            String basePackagePath = classInfo.get("包路径").replace(".", "/");
            String className = classInfo.get("类名");
            String targetClassPath = "src/main/java/" + basePackagePath + "/" + className + ".java";

            // 获取项目的类路径
            List<String> classpathElements = new ArrayList<>();
            // 假设项目的类路径包含 target/classes
            classpathElements.add("target/classes");

            // 拼接 classpath（注意操作系统分隔符）
            String classpath = String.join(System.getProperty("os.name").toLowerCase().contains("win") ? ";" : ":", classpathElements);

            // 设置编译选项
            List<String> options = new ArrayList<>();
            options.add("-classpath");
            options.add(classpath);
            options.add("-d");
            options.add("target/target-classes");
            options.add("-source");
            options.add("11");
            options.add("-target");
            options.add("11");

            // 创建 ProcessBuilder 来执行 Java 11 的 javac 命令
            List<String> command = new ArrayList<>();
            command.add(JAVAC_11_PATH);
            command.addAll(options);
            command.add(targetClassPath);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // 获取编译结果
            int exitCode = process.waitFor();

            // 输出编译结果
            if (exitCode == 0) {
                System.out.println("待测试类编译成功！");
                return true;
            } else {
                System.out.println("待测试类编译失败！");
                java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A");
                if (s.hasNext()) {
                    System.out.println("错误信息: " + s.next());
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("待测试类编译失败！发生异常: " + e.getMessage());
            return false;
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
            String testClassPath = "src/test/java/" + basePackagePath + "/" + testClassName + ".java";



            // 获取项目的类路径
            List<String> classpathElements = new ArrayList<>();
            // 假设项目的类路径包含 target/classes 和 target/test-classes
            classpathElements.add("target/classes");
            classpathElements.add("target/test-classes");

            // 添加 JUnit 依赖的类路径
            // 假设 JUnit JAR 文件在 lib 目录下
            classpathElements.add("lib/junit-jupiter-api-5.10.2.jar");
            classpathElements.add("lib/junit-jupiter-engine-5.10.2.jar");
            classpathElements.add("lib/junit-jupiter-params-5.10.2.jar");  // 添加参数化测试支持
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
            options.add("-source");
            options.add("11");
            options.add("-target");
            options.add("11");
            options.add("-encoding");
            options.add("UTF-8");  // 添加UTF-8编码支持，解决中文字符编译问题

            // 创建 ProcessBuilder 来执行 Java 11 的 javac 命令
            List<String> command = new ArrayList<>();
            command.add(JAVAC_11_PATH);
            command.addAll(options);
            command.add(testClassPath);

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            Process process = processBuilder.start();

            // 获取编译结果
            int exitCode = process.waitFor();

            // 输出编译结果
            if (exitCode == 0) {
                System.out.println("测试类编译成功！");
                return true;
            } else {
                System.out.println("测试类编译失败！");
                java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A");
                if (s.hasNext()) {
                    System.out.println("错误信息: " + s.next());
                }
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("测试类编译失败！发生异常: " + e.getMessage());
            return false;
        }
    }

    public static void main(String[] args) {
        TestClassCompiler compiler = new TestClassCompiler();

        // 配置类信息
        Map<String, String> classInfo = new HashMap<>();
        classInfo.put("包路径", "cli");
        classInfo.put("类名", "AlreadySelectedException");

        // 编译待测试类
        boolean success = compiler.compileTargetClass(classInfo);
        if (success) {
            System.out.println("编译成功！");
        } else {
            System.out.println("编译失败！");
        }

        // 编译测试类
        success = compiler.compileTestClass(classInfo);
        if (success) {
            System.out.println("测试类编译成功！");
        } else {
            System.out.println("测试类编译失败！");
        }
    }

}
