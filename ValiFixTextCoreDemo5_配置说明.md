# ValiFixTextCoreDemo5 配置说明 - 排除新建工具类

## 问题描述
csv包中新建了一些工具类用于解决编译依赖问题，这些类不属于原始csv包的核心功能，需要在测试生成和覆盖率统计时排除。

## 需要排除的类列表
```
Uncheck.java
IOStream.java
Base64OutputStream.java
AppendableOutputStream.java
IOUtilsHelper.java
AbstractStreamBuilder.java
Charsets.java
UnsynchronizedBufferedReader.java
```

## 解决方案

### 方案1：在main方法中添加过滤逻辑（推荐）

在 `ValiFixTextCoreDemo5.java` 的 `batchProcessAllClasses` 方法中，找到扫描Java文件的部分，添加过滤逻辑：

```java
public static void batchProcessAllClasses(String apiKey, String sourceCodeDir, String logFilename) throws IOException {
    // ... 现有代码 ...
    
    // ========== 新增：定义需要排除的类列表 ==========
    Set<String> excludeClasses = new HashSet<>(Arrays.asList(
        "Uncheck",
        "IOStream",
        "Base64OutputStream",
        "AppendableOutputStream",
        "IOUtilsHelper",
        "AbstractStreamBuilder",
        "Charsets",
        "UnsynchronizedBufferedReader"
    ));
    
    // 递归扫描所有.java文件
    List<File> javaFiles = new ArrayList<>();
    scanJavaFilesRecursively(dir, javaFiles);
    
    // ========== 新增：过滤掉排除列表中的类 ==========
    List<File> filteredJavaFiles = new ArrayList<>();
    for (File file : javaFiles) {
        String className = extractClassName(file);
        if (className != null && !excludeClasses.contains(className)) {
            filteredJavaFiles.add(file);
        } else if (className != null) {
            System.out.println("  [跳过] " + className + " (工具类，不生成测试)");
        }
    }
    
    // 使用过滤后的文件列表
    javaFiles = filteredJavaFiles;
    
    if (javaFiles.isEmpty()) {
        System.err.println("错误：目录下没有找到需要测试的.java文件: " + sourceCodeDir);
        return;
    }
    
    // ... 后续代码保持不变 ...
}
```

### 方案2：从配置文件读取排除列表

如果希望更灵活地配置，可以从外部文件读取排除列表：

```java
/**
 * 从配置文件读取需要排除的类列表
 */
private static Set<String> loadExcludeClasses(String configFile) {
    Set<String> excludeClasses = new HashSet<>();
    try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
        String line;
        while ((line = br.readLine()) != null) {
            line = line.trim();
            // 跳过注释和空行
            if (!line.isEmpty() && !line.startsWith("#")) {
                // 移除.java后缀
                if (line.endsWith(".java")) {
                    line = line.substring(0, line.length() - 5);
                }
                excludeClasses.add(line);
            }
        }
    } catch (IOException e) {
        System.err.println("警告：无法读取排除配置文件: " + configFile);
    }
    return excludeClasses;
}

// 在main方法中使用：
public static void main(String[] args) throws IOException {
    String apiKey = "your-api-key";
    String sourceCodeDir = "D:\\fx\\jafx\\chatunitest-core\\src\\main\\java\\csv";
    String logFilename = "BatchTest_" + new java.text.SimpleDateFormat("MMdd_HHmm").format(new java.util.Date()) + ".txt";
    
    // 读取排除列表
    Set<String> excludeClasses = loadExcludeClasses("csv-exclude-classes.txt");
    
    // 传递给批量处理方法
    batchProcessAllClassesWithExclusion(apiKey, sourceCodeDir, logFilename, excludeClasses);
}
```

## JaCoCo覆盖率配置

为了在JaCoCo覆盖率统计中也排除这些类，需要修改 `pom.xml` 中的JaCoCo配置：

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.8</version>
    <configuration>
        <excludes>
            <!-- 排除新建的工具类 -->
            <exclude>csv/Uncheck.class</exclude>
            <exclude>csv/IOStream.class</exclude>
            <exclude>csv/IOStream$*.class</exclude>
            <exclude>csv/Base64OutputStream.class</exclude>
            <exclude>csv/AppendableOutputStream.class</exclude>
            <exclude>csv/IOUtilsHelper.class</exclude>
            <exclude>csv/IOUtilsHelper$*.class</exclude>
            <exclude>csv/AbstractStreamBuilder.class</exclude>
            <exclude>csv/Charsets.class</exclude>
            <exclude>csv/UnsynchronizedBufferedReader.class</exclude>
        </excludes>
    </configuration>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

## 快速实施步骤

1. **修改ValiFixTextCoreDemo5.java**：
   - 在 `batchProcessAllClasses` 方法开头添加排除列表
   - 在扫描文件后添加过滤逻辑

2. **修改pom.xml**：
   - 在JaCoCo插件配置中添加excludes节点

3. **验证配置**：
   - 运行main方法，确认控制台输出显示跳过了工具类
   - 运行测试并生成覆盖率报告，确认工具类未被统计

## 示例输出

配置成功后，运行时应该看到类似输出：

```
【扫描结果】找到 18 个文件:
  [跳过] Uncheck (工具类，不生成测试)
  [跳过] IOStream (工具类，不生成测试)
  [跳过] Base64OutputStream (工具类，不生成测试)
  [跳过] AppendableOutputStream (工具类，不生成测试)
  [跳过] IOUtilsHelper (工具类，不生成测试)
  [跳过] AbstractStreamBuilder (工具类，不生成测试)
  [跳过] Charsets (工具类，不生成测试)
  [跳过] UnsynchronizedBufferedReader (工具类，不生成测试)
  - CSVFormat (CSVFormat.java)
  - CSVParser (CSVParser.java)
  - CSVPrinter (CSVPrinter.java)
  ... (其他原始csv类)

【扫描结果】找到 10 个待测试类 (已排除8个工具类)
```

## 注意事项

1. 排除列表中的类名不包含`.java`后缀
2. 如果后续添加了新的工具类，记得更新排除列表
3. JaCoCo的exclude使用的是编译后的`.class`文件路径
4. 内部类需要使用`$`符号匹配（如`IOStream$*.class`）
