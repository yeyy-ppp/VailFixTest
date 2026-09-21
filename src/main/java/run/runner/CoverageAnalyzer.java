package zju.cst.aces.runner;

import org.jacoco.core.analysis.*;
import org.jacoco.core.data.*;
import org.jacoco.core.instr.Instrumenter;
import org.jacoco.core.runtime.LoggerRuntime;
import org.jacoco.core.tools.ExecFileLoader;
import org.objectweb.asm.ClassReader;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class CoverageAnalyzer {

    /**
     * 插桩原始类，并输出到目标目录
     */
    public static void instrumentClasses(String originalClassDir, String outputDir) throws IOException {
        LoggerRuntime runtime = new LoggerRuntime();
        Instrumenter instrumenter = new Instrumenter(runtime);

        Path sourcePath = Paths.get(originalClassDir);
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);

        Files.walk(sourcePath)
                .filter(path -> path.toString().endsWith(".class"))
                .forEach(path -> {
                    try {
                        Path relative = sourcePath.relativize(path);
                        byte[] original = Files.readAllBytes(path);
                        byte[] instrumented = instrumenter.instrument(original, path.toString());

                        Path outPath = outputPath.resolve(relative);
                        Files.createDirectories(outPath.getParent());
                        Files.write(outPath, instrumented);
                    } catch (Exception e) {
                        throw new RuntimeException("插桩失败：" + path, e);
                    }
                });

        System.out.println("✅ 插桩完成，输出目录: " + outputPath);
    }

    /**
     * 分析执行后的覆盖率（需要 exec 数据 + 原始 class）
     */
    public static void analyzeCoverage(String originalClassDir, String execFilePath) throws IOException {
        ExecFileLoader loader = new ExecFileLoader();
        loader.load(new File(execFilePath));

        CoverageBuilder builder = new CoverageBuilder();
        Analyzer analyzer = new Analyzer(loader.getExecutionDataStore(), builder);

        analyzer.analyzeAll(new File(originalClassDir));

        printSummary(builder.getClasses());
    }

    /**
     * 打印覆盖率汇总
     */
    private static void printSummary(Collection<IClassCoverage> classes) {
        double totalInstructions = 0, coveredInstructions = 0;
        double totalBranches = 0, coveredBranches = 0;
        double totalLines = 0, coveredLines = 0;
        double totalMethods = 0, coveredMethods = 0;

        System.out.println("📊 覆盖率报告：");

        for (IClassCoverage cc : classes) {
            System.out.println("  • 类名：" + cc.getName().replace("/", "."));
            System.out.printf("    - 指令覆盖率:  %.2f%%%n", percent(cc.getInstructionCounter()));
            System.out.printf("    - 分支覆盖率:  %.2f%%%n", percent(cc.getBranchCounter()));
            System.out.printf("    - 行覆盖率:    %.2f%%%n", percent(cc.getLineCounter()));
            System.out.printf("    - 方法覆盖率:  %.2f%%%n", percent(cc.getMethodCounter()));
            System.out.printf("    - 类覆盖率:    %.2f%%%n", percent(cc.getClassCounter()));

            totalInstructions += cc.getInstructionCounter().getTotalCount();
            coveredInstructions += cc.getInstructionCounter().getCoveredCount();
            totalBranches += cc.getBranchCounter().getTotalCount();
            coveredBranches += cc.getBranchCounter().getCoveredCount();
            totalLines += cc.getLineCounter().getTotalCount();
            coveredLines += cc.getLineCounter().getCoveredCount();
            totalMethods += cc.getMethodCounter().getTotalCount();
            coveredMethods += cc.getMethodCounter().getCoveredCount();
        }

        System.out.println();
        System.out.printf("✅ 整体代码覆盖率:  %.2f%%%n", percent(coveredInstructions, totalInstructions));
        System.out.printf("✅ 分支覆盖率:      %.2f%%%n", percent(coveredBranches, totalBranches));
        System.out.printf("✅ 行覆盖率:        %.2f%%%n", percent(coveredLines, totalLines));
        System.out.printf("✅ 方法覆盖率:      %.2f%%%n", percent(coveredMethods, totalMethods));
        System.out.printf("✅ 类覆盖率:        %.2f%%%n", percent(classes.size(), classes.size()));
    }

    private static double percent(ICounter counter) {
        return percent(counter.getCoveredCount(), counter.getTotalCount());
    }

    private static double percent(double covered, double total) {
        return total == 0 ? 0.0 : (covered / total) * 100;
    }
}
