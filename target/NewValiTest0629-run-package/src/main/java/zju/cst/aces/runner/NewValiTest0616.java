package zju.cst.aces.runner;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Evaluates the test files generated under src/test/java without changing those
 * tests or ValiFixTextCoreDemo5.
 */
public class NewValiTest0616 {
    private static final Path DEFAULT_TEST_DIR = Paths.get("src", "test", "java");
    private static final Path DEFAULT_SOURCE_DIR = Paths.get("src", "main", "java");
    private static final Path DEFAULT_REPORT = Paths.get("target", "new-vali-test-0616-report.txt");
    private static final Path SUREFIRE_DIR = Paths.get("target", "surefire-reports");
    private static final Path PIT_DIR = Paths.get("target", "pit-reports");
    private static final Path DEFAULT_FIX_RESULT_DIR = Paths.get("src", "F");
    private static final Path DEFAULT_AFTER_JACOCO_CSV = Paths.get("target", "site", "jacoco", "jacoco.csv");
    private static final Path DEFAULT_BENCHMARK_DATA = Paths.get("docs", "benchmark-comparison.csv");
    private static final Path DEFAULT_REPAIR_IMPACT_WORK_DIR = Paths.get("target", "vali-repair-impact");
    private static final long DEFAULT_COMMAND_TIMEOUT_MINUTES = 30L;

    private static final Set<String> TEST_ANNOTATIONS = new HashSet<String>(Arrays.asList(
            "Test", "ParameterizedTest", "RepeatedTest", "TestFactory", "TestTemplate"
    ));

    private static final Set<String> ASSERTION_METHODS = new HashSet<String>(Arrays.asList(
            "assertArrayEquals", "assertEquals", "assertFalse", "assertIterableEquals",
            "assertLinesMatch", "assertNotEquals", "assertNotNull", "assertNotSame",
            "assertNull", "assertSame", "assertThrows", "assertThrowsExactly",
            "assertTimeout", "assertTimeoutPreemptively", "assertTrue", "assertAll",
            "assertDoesNotThrow", "fail"
    ));

    private static final Set<String> STRONG_ASSERTIONS = new HashSet<String>(Arrays.asList(
            "assertArrayEquals", "assertEquals", "assertIterableEquals", "assertLinesMatch",
            "assertNotEquals", "assertSame", "assertNotSame", "assertThrows",
            "assertThrowsExactly", "assertTimeout", "assertTimeoutPreemptively"
    ));

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        NewValiTest0616 validator = new NewValiTest0616();
        EvaluationResult result = validator.evaluate(config);
        String report = result.toReport();
        validator.writeReport(config.reportPath, report);
        System.out.println(report);
    }

    public EvaluationResult evaluate(Config config) throws Exception {
        EvaluationResult result = new EvaluationResult();
        result.config = config;
        result.startedAt = new Date();

        result.sourceMetrics = analyzeTestSources(config.testDir);
        result.beforeSourceMetrics = Files.isDirectory(config.beforeTestDir) ? analyzeTestSources(config.beforeTestDir) : null;
        result.surefireSummaryBeforeRun = readSurefireSummary(SUREFIRE_DIR);

        if (config.runTests) {
            result.testRuns = runTestsRepeatedly(config);
            result.flakySummary = calculateFlakySummary(result.testRuns);
            if (!result.testRuns.isEmpty()) {
                result.executionTimeMillis = result.testRuns.get(result.testRuns.size() - 1).wallTimeMillis;
            }
        } else {
            result.flakySummary = FlakySummary.notMeasured("Use --run-tests --flaky-runs=N to repeat the generated tests.");
            if (result.surefireSummaryBeforeRun.available) {
                result.executionTimeMillis = result.surefireSummaryBeforeRun.timeMillis;
            }
        }

        if (config.runPit) {
            result.pitCommandResult = runPit(config);
        }
        result.mutationSummary = readMutationSummary(PIT_DIR);
        result.coverageAfterRepair = readCoverageSummary(config.afterCoverageCsv);
        result.coverageBeforeRepair = readCoverageSummary(config.beforeCoverageCsv);
        result.repairCost = calculateRepairCost(result.sourceMetrics, result.surefireSummaryBeforeRun);
        result.minimalRepairImpact = calculateMinimalRepairImpact(config, result);
        if (config.measureRepairImpact) {
            measureMinimalRepairImpact(config, result.minimalRepairImpact);
        }
        result.benchmarkComparison = readBenchmarkComparison(config.benchmarkDataPath);
        result.defectDetection = calculateDefectDetection(result);
        result.finishedAt = new Date();
        return result;
    }

    private SourceMetrics analyzeTestSources(Path testDir) throws IOException {
        SourceMetrics metrics = new SourceMetrics();
        metrics.testDir = testDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(testDir)) {
            metrics.notes.add("Test directory does not exist: " + metrics.testDir);
            return metrics;
        }

        List<Path> files = findFiles(testDir, "Test.java");
        Collections.sort(files);
        metrics.testFileCount = files.size();

        for (Path file : files) {
            FileMetrics fileMetrics = analyzeSingleTestFile(file);
            metrics.files.add(fileMetrics);
            metrics.testMethodCount += fileMetrics.testMethodCount;
            metrics.assertionCount += fileMetrics.assertionCount;
            metrics.meaningfulAssertionCount += fileMetrics.meaningfulAssertionCount;
            metrics.weakAssertionCount += fileMetrics.weakAssertionCount;
            metrics.exceptionAssertionCount += fileMetrics.exceptionAssertionCount;
            metrics.nonCommentCodeLines += fileMetrics.nonCommentCodeLines;
            metrics.parseErrorCount += fileMetrics.parseError == null ? 0 : 1;
        }

        metrics.assertionsPerTestMethod = safeDivide(metrics.assertionCount, metrics.testMethodCount);
        metrics.meaningfulAssertionsPerTestMethod = safeDivide(metrics.meaningfulAssertionCount, metrics.testMethodCount);
        metrics.assertionsPerHundredLines = safeDivide(metrics.assertionCount * 100.0, metrics.nonCommentCodeLines);
        metrics.meaningfulAssertionRatio = safeDivide(metrics.meaningfulAssertionCount, metrics.assertionCount);
        return metrics;
    }

    private FileMetrics analyzeSingleTestFile(Path file) {
        FileMetrics metrics = new FileMetrics();
        metrics.path = file.toAbsolutePath().normalize();
        try {
            String code = readFile(file);
            metrics.nonCommentCodeLines = countNonCommentCodeLines(code);
            CompilationUnit unit = StaticJavaParser.parse(file);
            metrics.packageName = unit.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");

            List<MethodDeclaration> methods = unit.findAll(MethodDeclaration.class);
            for (MethodDeclaration method : methods) {
                if (isTestMethod(method)) {
                    metrics.testMethodCount++;
                }
            }

            List<MethodCallExpr> calls = unit.findAll(MethodCallExpr.class);
            for (MethodCallExpr call : calls) {
                String name = call.getNameAsString();
                if (ASSERTION_METHODS.contains(name)) {
                    metrics.assertionCount++;
                    if (isMeaningfulAssertion(call)) {
                        metrics.meaningfulAssertionCount++;
                    } else {
                        metrics.weakAssertionCount++;
                    }
                    if ("assertThrows".equals(name) || "assertThrowsExactly".equals(name)) {
                        metrics.exceptionAssertionCount++;
                    }
                }
            }
        } catch (Exception ex) {
            metrics.parseError = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            try {
                String code = readFile(file);
                metrics.nonCommentCodeLines = countNonCommentCodeLines(code);
                metrics.assertionCount = countByRegex(code, "\\bassert[A-Za-z0-9_]*\\s*\\(");
                metrics.meaningfulAssertionCount = metrics.assertionCount;
            } catch (IOException ignored) {
                metrics.parseError = metrics.parseError + "; also failed to read file";
            }
        }
        return metrics;
    }

    private boolean isTestMethod(MethodDeclaration method) {
        for (AnnotationExpr annotation : method.getAnnotations()) {
            String name = annotation.getNameAsString();
            if (TEST_ANNOTATIONS.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private boolean isMeaningfulAssertion(MethodCallExpr call) {
        String name = call.getNameAsString();
        if (STRONG_ASSERTIONS.contains(name)) {
            return hasNonTrivialArguments(call);
        }
        if ("assertAll".equals(name)) {
            return !call.getArguments().isEmpty();
        }
        if ("assertDoesNotThrow".equals(name) || "fail".equals(name)) {
            return false;
        }
        if ("assertTrue".equals(name) || "assertFalse".equals(name)) {
            return isNonConstantBooleanAssertion(call);
        }
        if ("assertNull".equals(name) || "assertNotNull".equals(name)) {
            return hasNonTrivialArguments(call) && hasMessageOrNonNameArgument(call);
        }
        return hasNonTrivialArguments(call);
    }

    private boolean hasNonTrivialArguments(MethodCallExpr call) {
        for (Expression argument : call.getArguments()) {
            String text = argument.toString().trim();
            if (!"true".equals(text) && !"false".equals(text) && !"null".equals(text) && !text.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private boolean isNonConstantBooleanAssertion(MethodCallExpr call) {
        if (call.getArguments().isEmpty()) {
            return false;
        }
        Expression first = call.getArgument(0);
        String text = first.toString().trim();
        if ("true".equals(text) || "false".equals(text)) {
            return false;
        }
        if (first.isNameExpr()) {
            return call.getArguments().size() > 1;
        }
        return true;
    }

    private boolean hasMessageOrNonNameArgument(MethodCallExpr call) {
        if (call.getArguments().size() > 1) {
            for (int i = 1; i < call.getArguments().size(); i++) {
                if (call.getArgument(i) instanceof StringLiteralExpr) {
                    return true;
                }
            }
        }
        if (call.getArguments().isEmpty()) {
            return false;
        }
        Expression first = call.getArgument(0);
        return !first.isNameExpr();
    }

    private List<TestRunResult> runTestsRepeatedly(Config config) throws IOException, InterruptedException {
        List<TestRunResult> runs = new ArrayList<TestRunResult>();
        int count = Math.max(1, config.flakyRuns);
        for (int i = 1; i <= count; i++) {
            long start = System.nanoTime();
            CommandResult command = runCommand(config.mavenCommand(), Arrays.asList("test"), DEFAULT_COMMAND_TIMEOUT_MINUTES);
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            TestRunResult run = new TestRunResult();
            run.index = i;
            run.commandResult = command;
            run.wallTimeMillis = elapsed;
            run.surefireSummary = readSurefireSummary(SUREFIRE_DIR);
            runs.add(run);
        }
        return runs;
    }

    private CommandResult runPit(Config config) throws IOException, InterruptedException {
        List<String> args = new ArrayList<String>();
        args.add("org.pitest:pitest-maven:mutationCoverage");
        args.add("-DskipTests=false");
        if (!config.pitTargetClasses.isEmpty()) {
            args.add("-DtargetClasses=" + join(config.pitTargetClasses, ","));
        }
        if (!config.pitTargetTests.isEmpty()) {
            args.add("-DtargetTests=" + join(config.pitTargetTests, ","));
        }
        return runCommand(config.mavenCommand(), args, Math.max(DEFAULT_COMMAND_TIMEOUT_MINUTES, config.pitTimeoutMinutes));
    }

    private CommandResult runCommand(String executable, List<String> arguments, long timeoutMinutes)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(executable);
        command.addAll(arguments);
        ProcessBuilder builder = new ProcessBuilder(command);
        builder.redirectErrorStream(true);
        builder.directory(new File(".").getAbsoluteFile());
        long start = System.nanoTime();
        Process process = builder.start();
        List<String> output = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), isWindows() ? Charset.forName("GBK") : StandardCharsets.UTF_8));
        String line;
        while ((line = reader.readLine()) != null) {
            output.add(line);
        }
        boolean completed = process.waitFor(timeoutMinutes, TimeUnit.MINUTES);
        CommandResult result = new CommandResult();
        result.command = join(command, " ");
        result.output = output;
        result.wallTimeMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        if (completed) {
            result.exitCode = process.exitValue();
        } else {
            process.destroyForcibly();
            result.exitCode = -1;
            result.timedOut = true;
        }
        return result;
    }

    private SurefireSummary readSurefireSummary(Path surefireDir) {
        SurefireSummary summary = new SurefireSummary();
        summary.directory = surefireDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(surefireDir)) {
            summary.available = false;
            summary.note = "Surefire report directory is absent.";
            return summary;
        }
        try {
            List<Path> xmlFiles = findFiles(surefireDir, ".xml");
            for (Path xml : xmlFiles) {
                if (!xml.getFileName().toString().startsWith("TEST-")) {
                    continue;
                }
                parseSurefireXml(xml, summary);
            }
            summary.available = summary.testCases > 0 || summary.testSuites > 0;
            if (!summary.available) {
                summary.note = "No TEST-*.xml files were found.";
            }
        } catch (Exception ex) {
            summary.available = false;
            summary.note = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        return summary;
    }

    private void parseSurefireXml(Path xml, SurefireSummary summary) throws Exception {
        Document document = parseXml(xml);
        Element root = document.getDocumentElement();
        String rootName = root.getTagName();
        if ("testsuite".equals(rootName)) {
            addSurefireSuite(root, summary);
        } else if ("testsuites".equals(rootName)) {
            NodeList suites = root.getElementsByTagName("testsuite");
            for (int i = 0; i < suites.getLength(); i++) {
                addSurefireSuite((Element) suites.item(i), summary);
            }
        }
    }

    private void addSurefireSuite(Element suite, SurefireSummary summary) {
        summary.testSuites++;
        summary.tests += intAttr(suite, "tests");
        summary.failures += intAttr(suite, "failures");
        summary.errors += intAttr(suite, "errors");
        summary.skipped += intAttr(suite, "skipped");
        summary.timeMillis += Math.round(doubleAttr(suite, "time") * 1000.0);

        NodeList cases = suite.getElementsByTagName("testcase");
        for (int i = 0; i < cases.getLength(); i++) {
            Element testCase = (Element) cases.item(i);
            summary.testCases++;
            TestCaseStatus status = new TestCaseStatus();
            status.className = testCase.getAttribute("classname");
            status.methodName = testCase.getAttribute("name");
            status.timeMillis = Math.round(doubleAttr(testCase, "time") * 1000.0);
            status.failed = hasChild(testCase, "failure") || hasChild(testCase, "error");
            status.skipped = hasChild(testCase, "skipped");
            summary.testCaseStatuses.put(status.id(), status);
        }
    }

    private FlakySummary calculateFlakySummary(List<TestRunResult> runs) {
        if (runs == null || runs.size() < 2) {
            return FlakySummary.notMeasured("At least two runs are required to calculate flaky ratio.");
        }

        Map<String, List<Boolean>> outcomes = new LinkedHashMap<String, List<Boolean>>();
        for (TestRunResult run : runs) {
            for (Map.Entry<String, TestCaseStatus> entry : run.surefireSummary.testCaseStatuses.entrySet()) {
                List<Boolean> values = outcomes.get(entry.getKey());
                if (values == null) {
                    values = new ArrayList<Boolean>();
                    outcomes.put(entry.getKey(), values);
                }
                values.add(!entry.getValue().failed && !entry.getValue().skipped);
            }
        }

        int flaky = 0;
        for (List<Boolean> values : outcomes.values()) {
            boolean sawPass = false;
            boolean sawFail = false;
            for (Boolean value : values) {
                if (Boolean.TRUE.equals(value)) {
                    sawPass = true;
                } else {
                    sawFail = true;
                }
            }
            if (sawPass && sawFail) {
                flaky++;
            }
        }

        FlakySummary summary = new FlakySummary();
        summary.available = true;
        summary.runCount = runs.size();
        summary.observedTestCases = outcomes.size();
        summary.flakyTestCases = flaky;
        summary.flakyRatio = safeDivide(flaky, outcomes.size());
        return summary;
    }

    private MutationSummary readMutationSummary(Path pitDir) {
        MutationSummary summary = new MutationSummary();
        summary.pitDir = pitDir.toAbsolutePath().normalize();
        if (!Files.isDirectory(pitDir)) {
            summary.available = false;
            summary.note = "PIT report directory is absent. Use --run-pit to generate it.";
            return summary;
        }
        try {
            List<Path> mutationFiles = findFiles(pitDir, "mutations.xml");
            if (mutationFiles.isEmpty()) {
                summary.available = false;
                summary.note = "No mutations.xml file was found under PIT reports.";
                return summary;
            }
            Collections.sort(mutationFiles, new Comparator<Path>() {
                public int compare(Path a, Path b) {
                    try {
                        return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                    } catch (IOException e) {
                        return 0;
                    }
                }
            });
            summary.reportFile = mutationFiles.get(0).toAbsolutePath().normalize();
            parseMutationsXml(summary.reportFile, summary);
            summary.available = summary.totalMutants > 0;
            if (!summary.available) {
                summary.note = "PIT report exists, but no mutation entries were read.";
            }
        } catch (Exception ex) {
            summary.available = false;
            summary.note = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        return summary;
    }

    private void parseMutationsXml(Path xml, MutationSummary summary) throws Exception {
        Document document = parseXml(xml);
        NodeList mutations = document.getElementsByTagName("mutation");
        for (int i = 0; i < mutations.getLength(); i++) {
            Element mutation = (Element) mutations.item(i);
            summary.totalMutants++;
            String status = mutation.getAttribute("status");
            boolean detected = Boolean.parseBoolean(mutation.getAttribute("detected"));
            if (detected || "KILLED".equals(status) || "TIMED_OUT".equals(status) || "MEMORY_ERROR".equals(status)) {
                summary.detectedMutants++;
            }
            if ("KILLED".equals(status)) {
                summary.killedMutants++;
            } else if ("SURVIVED".equals(status)) {
                summary.survivedMutants++;
            } else if ("NO_COVERAGE".equals(status)) {
                summary.noCoverageMutants++;
            } else {
                Integer count = summary.otherStatuses.get(status);
                summary.otherStatuses.put(status, count == null ? 1 : count + 1);
            }
        }
        summary.mutationScore = safeDivide(summary.detectedMutants, summary.totalMutants);
        summary.killScore = safeDivide(summary.killedMutants, summary.totalMutants);
    }

    private CoverageSummary readCoverageSummary(Path jacocoCsv) {
        CoverageSummary summary = new CoverageSummary();
        summary.csvFile = jacocoCsv == null ? null : jacocoCsv.toAbsolutePath().normalize();
        if (jacocoCsv == null || !Files.isRegularFile(jacocoCsv)) {
            summary.available = false;
            summary.note = "JaCoCo CSV not found. Run mvn test or pass --after-coverage-csv/--before-coverage-csv.";
            return summary;
        }
        try {
            List<String> lines = Files.readAllLines(jacocoCsv, StandardCharsets.UTF_8);
            for (int i = 1; i < lines.size(); i++) {
                List<String> cells = parseCsvLine(lines.get(i));
                if (cells.size() < 13) {
                    continue;
                }
                summary.instructionMissed += parseLong(cells.get(3));
                summary.instructionCovered += parseLong(cells.get(4));
                summary.branchMissed += parseLong(cells.get(5));
                summary.branchCovered += parseLong(cells.get(6));
                summary.lineMissed += parseLong(cells.get(7));
                summary.lineCovered += parseLong(cells.get(8));
                summary.methodMissed += parseLong(cells.get(11));
                summary.methodCovered += parseLong(cells.get(12));
            }
            summary.available = true;
            summary.instructionCoverage = ratio(summary.instructionCovered, summary.instructionMissed + summary.instructionCovered);
            summary.branchCoverage = ratio(summary.branchCovered, summary.branchMissed + summary.branchCovered);
            summary.lineCoverage = ratio(summary.lineCovered, summary.lineMissed + summary.lineCovered);
            summary.methodCoverage = ratio(summary.methodCovered, summary.methodMissed + summary.methodCovered);
        } catch (Exception ex) {
            summary.available = false;
            summary.note = ex.getClass().getSimpleName() + ": " + ex.getMessage();
        }
        return summary;
    }

    private MinimalRepairImpact calculateMinimalRepairImpact(Config config, EvaluationResult result) throws IOException {
        MinimalRepairImpact impact = new MinimalRepairImpact();
        impact.fixResultDir = config.fixResultDir.toAbsolutePath().normalize();
        impact.beforeTestDir = config.beforeTestDir.toAbsolutePath().normalize();
        impact.afterTestDir = config.testDir.toAbsolutePath().normalize();

        if (result.beforeSourceMetrics != null) {
            impact.beforeMethods = result.beforeSourceMetrics.testMethodCount;
            impact.beforeLines = result.beforeSourceMetrics.nonCommentCodeLines;
            impact.beforeAssertions = result.beforeSourceMetrics.assertionCount;
            impact.afterMethods = result.sourceMetrics.testMethodCount;
            impact.afterLines = result.sourceMetrics.nonCommentCodeLines;
            impact.afterAssertions = result.sourceMetrics.assertionCount;
            impact.deletedOrCommentedMethods = Math.max(0, impact.beforeMethods - impact.afterMethods);
            impact.deletedOrCommentedLines = Math.max(0, impact.beforeLines - impact.afterLines);
            impact.deletedOrCommentedAssertions = Math.max(0, impact.beforeAssertions - impact.afterAssertions);
        }

        impact.coverageBefore = result.coverageBeforeRepair;
        impact.coverageAfter = result.coverageAfterRepair;
        SurefireSummary beforeSurefire = readSurefireSummary(config.beforeSurefireDir);
        impact.hasBeforePassRate = beforeSurefire.available && beforeSurefire.tests > 0;
        impact.passRateBefore = impact.hasBeforePassRate
                ? ratio(beforeSurefire.tests - beforeSurefire.failures - beforeSurefire.errors, beforeSurefire.tests)
                : 0.0;
        impact.passRateAfter = result.surefireSummaryBeforeRun.available
                ? ratio(result.surefireSummaryBeforeRun.tests - result.surefireSummaryBeforeRun.failures - result.surefireSummaryBeforeRun.errors,
                result.surefireSummaryBeforeRun.tests)
                : 0.0;
        impact.hasAfterPassRate = result.surefireSummaryBeforeRun.available;
        impact.beforeCoverageCsv = config.beforeCoverageCsv.toAbsolutePath().normalize();
        impact.afterCoverageCsv = config.afterCoverageCsv.toAbsolutePath().normalize();
        impact.beforeSurefireDir = config.beforeSurefireDir.toAbsolutePath().normalize();
        impact.afterSurefireDir = SUREFIRE_DIR.toAbsolutePath().normalize();

        if (!Files.isDirectory(config.fixResultDir)) {
            impact.note = "Fix result directory not found. Pass --fix-result-dir=... if ValiFixTextCoreDemo5 writes logs elsewhere.";
            return impact;
        }

        List<Path> fixLogs = findFiles(config.fixResultDir, "FixResult.txt");
        impact.fixResultFileCount = fixLogs.size();
        for (Path fixLog : fixLogs) {
            FixLogImpact logImpact = analyzeFixLog(fixLog);
            if (logImpact.hasFallback) {
                impact.fallbackFileCount++;
                impact.fallbackMethodsCommented += logImpact.methodsCommented;
                impact.fallbackLinesCommented += logImpact.linesCommented;
                impact.fallbackAssertionsCommented += logImpact.assertionsCommented;
                impact.fallbackMethodsDeleted += logImpact.methodsDeleted;
                impact.fallbackLinesDeleted += logImpact.linesDeleted;
                impact.fallbackAssertionsDeleted += logImpact.assertionsDeleted;
                impact.fixLogImpacts.add(logImpact);
            }
        }

        if (impact.deletedOrCommentedMethods == 0 && impact.fallbackMethodsCommented + impact.fallbackMethodsDeleted > 0) {
            impact.deletedOrCommentedMethods = impact.fallbackMethodsCommented + impact.fallbackMethodsDeleted;
            impact.deletedOrCommentedLines = impact.fallbackLinesCommented + impact.fallbackLinesDeleted;
            impact.deletedOrCommentedAssertions = impact.fallbackAssertionsCommented + impact.fallbackAssertionsDeleted;
        }
        return impact;
    }

    private FixLogImpact analyzeFixLog(Path fixLog) throws IOException {
        FixLogImpact impact = new FixLogImpact();
        impact.file = fixLog.toAbsolutePath().normalize();
        String text = readFile(fixLog);
        impact.hasFallback = text.contains("兜底") || text.contains("[兜底]") || text.contains("注释错误") || text.contains("修复失败");
        List<CodeBlock> blocks = extractJavaBlocks(text);
        CodeBlock beforeFallback = null;
        CodeBlock fallback = null;
        for (CodeBlock block : blocks) {
            if (block.label.contains("兜底后代码") || block.label.contains("兜底")) {
                fallback = block;
                break;
            }
            beforeFallback = block;
        }
        if (fallback == null && impact.hasFallback && !blocks.isEmpty()) {
            fallback = blocks.get(blocks.size() - 1);
        }
        if (fallback != null) {
            impact.methodsCommented = countCommentedTestMethods(fallback.code);
            impact.linesCommented = countFallbackCommentedLines(fallback.code);
            impact.assertionsCommented = countCommentedAssertions(fallback.code);
        }
        if (beforeFallback != null && fallback != null) {
            SourceMetrics before = analyzeCodeSnapshot(beforeFallback.code, fixLog);
            SourceMetrics after = analyzeCodeSnapshot(fallback.code, fixLog);
            impact.methodsDeleted = Math.max(0, before.testMethodCount - after.testMethodCount - impact.methodsCommented);
            impact.linesDeleted = Math.max(0, before.nonCommentCodeLines - after.nonCommentCodeLines - impact.linesCommented);
            impact.assertionsDeleted = Math.max(0, before.assertionCount - after.assertionCount - impact.assertionsCommented);
        }
        return impact;
    }

    private void measureMinimalRepairImpact(Config config, MinimalRepairImpact impact) {
        impact.autoMeasurementRequested = true;
        try {
            RepairImpactSnapshots snapshots = buildRepairImpactSnapshots(config, impact);
            if (!snapshots.available) {
                appendImpactNote(impact, snapshots.note);
                return;
            }

            Path originalTestDir = config.testDir.toAbsolutePath().normalize();
            Path backupTestDir = config.repairImpactWorkDir.resolve("original-src-test-java");
            deleteTree(backupTestDir);
            copyTree(originalTestDir, backupTestDir);

            try {
                impact.beforeMeasurement = runRepairImpactSnapshot(config, snapshots.beforeTestDir, backupTestDir, "before");
                impact.afterMeasurement = runRepairImpactSnapshot(config, snapshots.afterTestDir, backupTestDir, "after");
            } finally {
                deleteTree(originalTestDir);
                copyTree(backupTestDir, originalTestDir);
            }

            impact.beforeCoverageCsv = impact.beforeMeasurement.coverage.csvFile;
            impact.afterCoverageCsv = impact.afterMeasurement.coverage.csvFile;
            impact.beforeSurefireDir = impact.beforeMeasurement.surefire.directory;
            impact.afterSurefireDir = impact.afterMeasurement.surefire.directory;
            if (impact.beforeMeasurement.coverage.available) {
                impact.coverageBefore = impact.beforeMeasurement.coverage;
            }
            if (impact.afterMeasurement.coverage.available) {
                impact.coverageAfter = impact.afterMeasurement.coverage;
            }
            if (impact.beforeMeasurement.surefire.available && impact.beforeMeasurement.surefire.tests > 0) {
                impact.hasBeforePassRate = true;
                impact.passRateBefore = passRate(impact.beforeMeasurement.surefire);
            }
            if (impact.afterMeasurement.surefire.available && impact.afterMeasurement.surefire.tests > 0) {
                impact.hasAfterPassRate = true;
                impact.passRateAfter = passRate(impact.afterMeasurement.surefire);
            }
        } catch (Exception ex) {
            appendImpactNote(impact, "Automatic before/after measurement failed: "
                    + ex.getClass().getSimpleName() + ": " + ex.getMessage());
        }
    }

    private RepairImpactSnapshots buildRepairImpactSnapshots(Config config, MinimalRepairImpact impact) throws IOException {
        RepairImpactSnapshots snapshots = new RepairImpactSnapshots();
        snapshots.beforeTestDir = config.repairImpactWorkDir.resolve("before").resolve("src").resolve("test").resolve("java");
        snapshots.afterTestDir = config.repairImpactWorkDir.resolve("after").resolve("src").resolve("test").resolve("java");
        deleteTree(snapshots.beforeTestDir);
        deleteTree(snapshots.afterTestDir);
        Files.createDirectories(snapshots.beforeTestDir);
        Files.createDirectories(snapshots.afterTestDir);
        if (Files.isDirectory(config.testDir)) {
            copyTree(config.testDir, snapshots.beforeTestDir);
            copyTree(config.testDir, snapshots.afterTestDir);
        }

        if (!Files.isDirectory(config.fixResultDir)) {
            snapshots.note = "Cannot measure repair impact automatically because fix result directory is missing: "
                    + config.fixResultDir.toAbsolutePath().normalize();
            return snapshots;
        }

        List<Path> fixLogs = findFiles(config.fixResultDir, "FixResult.txt");
        for (Path fixLog : fixLogs) {
            writeRepairSnapshotPair(fixLog, snapshots);
        }
        impact.snapshotPairCount = snapshots.snapshotPairCount;
        impact.snapshotBeforeFileCount = snapshots.beforeFileCount;
        impact.snapshotAfterFileCount = snapshots.afterFileCount;

        snapshots.available = snapshots.snapshotPairCount > 0;
        if (!snapshots.available) {
            snapshots.note = "No paired repair/fallback code blocks were found in "
                    + config.fixResultDir.toAbsolutePath().normalize()
                    + ". Expected blocks labelled 修复后代码 followed by 兜底后代码.";
        }
        return snapshots;
    }

    private void writeRepairSnapshotPair(Path fixLog, RepairImpactSnapshots snapshots) throws IOException {
        String text = readFile(fixLog);
        List<CodeBlock> blocks = extractJavaBlocks(text);
        CodeBlock before = null;
        int pairIndex = 0;
        for (CodeBlock block : blocks) {
            if (block.label.contains("兜底")) {
                if (before != null) {
                    pairIndex++;
                    boolean wroteBefore = writeCodeBlockAsTestFile(before, snapshots.beforeTestDir, fixLog, pairIndex);
                    boolean wroteAfter = writeCodeBlockAsTestFile(block, snapshots.afterTestDir, fixLog, pairIndex);
                    if (wroteBefore && wroteAfter) {
                        snapshots.snapshotPairCount++;
                        snapshots.beforeFileCount++;
                        snapshots.afterFileCount++;
                    }
                }
                before = null;
            } else {
                before = block;
            }
        }
    }

    private boolean writeCodeBlockAsTestFile(CodeBlock block, Path testRoot, Path fixLog, int pairIndex) throws IOException {
        String code = stripMarkdownFence(block.code);
        JavaClassLocation location = parseJavaClassLocation(code);
        if (!location.available) {
            return false;
        }
        Path packageDir = testRoot;
        if (!location.packageName.isEmpty()) {
            packageDir = packageDir.resolve(location.packageName.replace('.', File.separatorChar));
        }
        Files.createDirectories(packageDir);
        Path target = packageDir.resolve(location.className + ".java");
        BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
        try {
            writer.write(code);
            if (!code.endsWith(System.lineSeparator())) {
                writer.write(System.lineSeparator());
            }
        } finally {
            writer.close();
        }
        return true;
    }

    private JavaClassLocation parseJavaClassLocation(String code) {
        JavaClassLocation location = new JavaClassLocation();
        try {
            CompilationUnit unit = StaticJavaParser.parse(code);
            location.packageName = unit.getPackageDeclaration().map(pd -> pd.getNameAsString()).orElse("");
            if (!unit.getTypes().isEmpty()) {
                TypeDeclaration<?> type = unit.getType(0);
                location.className = type.getNameAsString();
                location.available = true;
            }
        } catch (Exception ignored) {
            Matcher packageMatcher = Pattern.compile("(?m)^\\s*package\\s+([A-Za-z0-9_.]+)\\s*;").matcher(code);
            location.packageName = packageMatcher.find() ? packageMatcher.group(1) : "";
            Matcher classMatcher = Pattern.compile("\\b(?:class|interface|enum)\\s+([A-Za-z0-9_]+)").matcher(code);
            if (classMatcher.find()) {
                location.className = classMatcher.group(1);
                location.available = true;
            }
        }
        return location;
    }

    private RepairImpactMeasurement runRepairImpactSnapshot(Config config, Path snapshotTestDir, Path baselineTestDir, String label)
            throws IOException, InterruptedException {
        RepairImpactMeasurement measurement = new RepairImpactMeasurement();
        Path originalTestDir = config.testDir.toAbsolutePath().normalize();
        Path outputDir = config.repairImpactWorkDir.resolve(label);
        for (int attempt = 1; attempt <= 6; attempt++) {
            deleteTree(originalTestDir);
            copyTree(snapshotTestDir, originalTestDir);

            deleteTree(SUREFIRE_DIR);
            deleteTree(Paths.get("target", "site", "jacoco"));
            deleteTree(Paths.get("target", "test-classes"));
            Files.deleteIfExists(Paths.get("target", "jacoco.exec"));

            measurement.commandResult = runCommand(config.mavenCommand(),
                    Arrays.asList("-fn", "-Dmaven.test.failure.ignore=true", "test", "jacoco:report"),
                    Math.max(DEFAULT_COMMAND_TIMEOUT_MINUTES, config.repairImpactTimeoutMinutes));
            measurement.commandLog = outputDir.resolve("maven-output.log").toAbsolutePath().normalize();
            writeLines(measurement.commandLog, measurement.commandResult.output);

            Path surefireCopy = outputDir.resolve("surefire-reports");
            Path jacocoCopy = outputDir.resolve("jacoco.csv");
            deleteTree(surefireCopy);
            if (Files.isDirectory(SUREFIRE_DIR)) {
                copyTree(SUREFIRE_DIR, surefireCopy);
            }
            Files.deleteIfExists(jacocoCopy);
            if (Files.isRegularFile(DEFAULT_AFTER_JACOCO_CSV)) {
                Files.createDirectories(jacocoCopy.getParent());
                Files.copy(DEFAULT_AFTER_JACOCO_CSV, jacocoCopy, StandardCopyOption.REPLACE_EXISTING);
            }
            measurement.surefire = readSurefireSummary(surefireCopy);
            measurement.coverage = readCoverageSummary(jacocoCopy);
            if (measurement.surefire.available || measurement.coverage.available) {
                return measurement;
            }

            Set<Path> brokenFiles = extractBrokenTestFiles(measurement.commandResult.output, originalTestDir);
            if (brokenFiles.isEmpty()) {
                return measurement;
            }
            for (Path relative : brokenFiles) {
                Path baseline = baselineTestDir.resolve(relative);
                Path snapshot = snapshotTestDir.resolve(relative);
                if (Files.isRegularFile(baseline)) {
                    Files.createDirectories(snapshot.getParent());
                    Files.copy(baseline, snapshot, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    Files.deleteIfExists(snapshot);
                }
                measurement.fallbackSnapshotFiles.add(relative.toString());
            }
        }
        return measurement;
    }

    private Set<Path> extractBrokenTestFiles(List<String> output, Path testRoot) {
        Set<Path> files = new LinkedHashSet<Path>();
        Pattern pattern = Pattern.compile("([A-Za-z]:)?[\\\\/][^:\\r\\n]*src[\\\\/]test[\\\\/]java[\\\\/][^:\\r\\n]*\\.java");
        for (String line : output) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                Path file = Paths.get(matcher.group()).toAbsolutePath().normalize();
                if (file.toString().startsWith(testRoot.toString())) {
                    files.add(testRoot.relativize(file));
                }
            }
        }
        return files;
    }

    private String stripMarkdownFence(String code) {
        String normalized = code == null ? "" : code.trim();
        while (normalized.startsWith("```")) {
            int newline = normalized.indexOf('\n');
            if (newline < 0) {
                return "";
            }
            normalized = normalized.substring(newline + 1).trim();
        }
        while (normalized.endsWith("```")) {
            normalized = normalized.substring(0, normalized.length() - 3).trim();
        }
        return normalized;
    }

    private static void appendImpactNote(MinimalRepairImpact impact, String note) {
        if (note == null || note.trim().isEmpty()) {
            return;
        }
        if (impact.note == null || impact.note.trim().isEmpty()) {
            impact.note = note;
        } else {
            impact.note = impact.note + " " + note;
        }
    }

    private SourceMetrics analyzeCodeSnapshot(String code, Path sourceHint) {
        SourceMetrics metrics = new SourceMetrics();
        metrics.testFileCount = 1;
        metrics.nonCommentCodeLines = countNonCommentCodeLines(code);
        try {
            CompilationUnit unit = StaticJavaParser.parse(code);
            for (MethodDeclaration method : unit.findAll(MethodDeclaration.class)) {
                if (isTestMethod(method)) {
                    metrics.testMethodCount++;
                }
            }
            for (MethodCallExpr call : unit.findAll(MethodCallExpr.class)) {
                if (ASSERTION_METHODS.contains(call.getNameAsString())) {
                    metrics.assertionCount++;
                    if (isMeaningfulAssertion(call)) {
                        metrics.meaningfulAssertionCount++;
                    }
                }
            }
        } catch (Exception ex) {
            metrics.parseErrorCount = 1;
            metrics.notes.add("Cannot parse code block from " + sourceHint + ": " + ex.getMessage());
            metrics.testMethodCount = countByRegex(code, "@Test\\b");
            metrics.assertionCount = countByRegex(code, "\\bassert[A-Za-z0-9_]*\\s*\\(");
        }
        return metrics;
    }

    private List<CodeBlock> extractJavaBlocks(String text) {
        List<CodeBlock> blocks = new ArrayList<CodeBlock>();
        Pattern pattern = Pattern.compile("(?s)([^\\n]*)\\n```java\\s*\\R(.*?)\\R```");
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            CodeBlock block = new CodeBlock();
            block.label = matcher.group(1) == null ? "" : matcher.group(1).trim();
            block.code = matcher.group(2);
            blocks.add(block);
        }
        return blocks;
    }

    private int countCommentedTestMethods(String code) {
        int count = countByRegex(code, "(?m)^\\s*//\\s*(?:\\[兜底\\]|修复失败[^：:]*[:：])?\\s*@Test\\b");
        if (count == 0) {
            count = countByRegex(code, "(?m)^\\s*//\\s*\\[兜底\\]\\s*(?:public\\s+)?void\\s+\\w+\\s*\\(");
        }
        return count;
    }

    private int countFallbackCommentedLines(String code) {
        return countByRegex(code, "(?m)^\\s*//\\s*(?:\\[兜底\\]|修复失败|全类修复失败)");
    }

    private int countCommentedAssertions(String code) {
        return countByRegex(code, "(?m)^\\s*//.*\\bassert[A-Za-z0-9_]*\\s*\\(");
    }

    private double readPassRate(Path surefireDir) {
        SurefireSummary summary = readSurefireSummary(surefireDir);
        if (!summary.available || summary.tests == 0) {
            return 0.0;
        }
        return ratio(summary.tests - summary.failures - summary.errors, summary.tests);
    }

    private BenchmarkComparison readBenchmarkComparison(Path dataPath) throws IOException {
        BenchmarkComparison comparison = BenchmarkComparison.defaultEmpty();
        comparison.dataFile = dataPath == null ? null : dataPath.toAbsolutePath().normalize();
        if (dataPath == null || !Files.isRegularFile(dataPath)) {
            comparison.note = "No local benchmark data file found. Public papers usually do not report the full requested metric set; pass --benchmark-data=CSV to import your experiment table.";
            return comparison;
        }
        List<String> lines = Files.readAllLines(dataPath, StandardCharsets.UTF_8);
        if (lines.isEmpty()) {
            comparison.note = "Benchmark data file is empty.";
            return comparison;
        }
        List<String> header = parseDelimitedLine(lines.get(0));
        for (int i = 1; i < lines.size(); i++) {
            if (lines.get(i).trim().isEmpty()) {
                continue;
            }
            List<String> cells = parseDelimitedLine(lines.get(i));
            BenchmarkRecord record = new BenchmarkRecord();
            for (int c = 0; c < header.size() && c < cells.size(); c++) {
                record.values.put(header.get(c).trim(), cells.get(c).trim());
            }
            if (!record.values.isEmpty()) {
                comparison.records.add(record);
            }
        }
        comparison.available = true;
        comparison.note = "Loaded local benchmark data. Missing cells are preserved as N/A.";
        return comparison;
    }

    private RepairCost calculateRepairCost(SourceMetrics sourceMetrics, SurefireSummary surefire) throws IOException {
        RepairCost repairCost = new RepairCost();
        repairCost.generatedTestFiles = sourceMetrics.testFileCount;
        repairCost.generatedTestMethods = sourceMetrics.testMethodCount;
        repairCost.generatedTestLines = sourceMetrics.nonCommentCodeLines;
        repairCost.parseErrorCount = sourceMetrics.parseErrorCount;
        if (surefire.available) {
            repairCost.currentFailingTests = surefire.failures + surefire.errors;
        }
        repairCost.generationLogSummary = readLatestGenerationLogSummary();
        return repairCost;
    }

    private GenerationLogSummary readLatestGenerationLogSummary() throws IOException {
        GenerationLogSummary summary = new GenerationLogSummary();
        List<Path> logs = new ArrayList<Path>();
        File root = new File(".");
        File[] files = root.listFiles();
        if (files == null) {
            summary.note = "No root files can be listed.";
            return summary;
        }
        for (File file : files) {
            String name = file.getName();
            if (file.isFile() && (name.startsWith("BatchTest_") || name.contains("test") || name.contains("log"))) {
                if (name.endsWith(".txt") || name.endsWith(".log")) {
                    logs.add(file.toPath());
                }
            }
        }
        if (logs.isEmpty()) {
            summary.note = "No generation log file was found.";
            return summary;
        }
        Collections.sort(logs, new Comparator<Path>() {
            public int compare(Path a, Path b) {
                try {
                    return Files.getLastModifiedTime(b).compareTo(Files.getLastModifiedTime(a));
                } catch (IOException e) {
                    return 0;
                }
            }
        });
        summary.available = true;
        summary.logFile = logs.get(0).toAbsolutePath().normalize();
        List<String> lines = Files.readAllLines(logs.get(0), StandardCharsets.UTF_8);
        for (String line : lines) {
            String normalized = line.trim();
            if (containsAny(normalized, "success", "SUCCESS", "chenggong", "鎴愬姛", "成功")) {
                summary.successLikeLines++;
            }
            if (containsAny(normalized, "fail", "FAIL", "failed", "澶辫触", "失败")) {
                summary.failureLikeLines++;
            }
            if (containsAny(normalized, "fix", "repair", "淇", "修复")) {
                Integer firstNumber = firstInteger(normalized);
                if (firstNumber != null) {
                    summary.repairNumberSum += firstNumber;
                    summary.repairNumberLines++;
                }
            }
        }
        return summary;
    }

    private DefectDetection calculateDefectDetection(EvaluationResult result) {
        DefectDetection detection = new DefectDetection();
        if (result.mutationSummary.available) {
            detection.available = true;
            detection.score = result.mutationSummary.mutationScore;
            detection.basis = "PIT detected mutants / total mutants";
            return detection;
        }
        detection.available = false;
        detection.score = result.sourceMetrics.meaningfulAssertionRatio;
        detection.basis = "PIT is unavailable; showing meaningful assertion ratio as a weak proxy.";
        return detection;
    }

    private void writeReport(Path reportPath, String report) throws IOException {
        Path parent = reportPath.toAbsolutePath().normalize().getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
        BufferedWriter writer = Files.newBufferedWriter(reportPath, StandardCharsets.UTF_8);
        try {
            writer.write(report);
        } finally {
            writer.close();
        }
    }

    private static void writeLines(Path file, List<String> lines) throws IOException {
        Path parent = file.toAbsolutePath().normalize().getParent();
        if (parent != null && !Files.isDirectory(parent)) {
            Files.createDirectories(parent);
        }
        BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8);
        try {
            for (String line : lines) {
                writer.write(line == null ? "" : line);
                writer.write(System.lineSeparator());
            }
        } finally {
            writer.close();
        }
    }

    private static Document parseXml(Path xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(xml.toFile());
    }

    private static List<Path> findFiles(Path root, final String suffix) throws IOException {
        final List<Path> files = new ArrayList<Path>();
        if (!Files.exists(root)) {
            return files;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (file.getFileName().toString().endsWith(suffix)) {
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }

    private static void copyTree(final Path source, final Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(dir);
                Files.createDirectories(target.resolve(relative));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Path relative = source.relativize(file);
                Files.copy(file, target.resolve(relative), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteTree(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        Files.walkFileTree(root, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                }
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static String readFile(Path file) throws IOException {
        return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    }

    private static int countNonCommentCodeLines(String code) {
        String withoutBlock = code.replaceAll("(?s)/\\*.*?\\*/", "");
        String[] lines = withoutBlock.split("\\R");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//")) {
                count++;
            }
        }
        return count;
    }

    private static int countByRegex(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private static List<String> parseCsvLine(String line) {
        return parseDelimitedLine(line);
    }

    private static List<String> parseDelimitedLine(String line) {
        char delimiter = line.indexOf('\t') >= 0 ? '\t' : ',';
        List<String> cells = new ArrayList<String>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                cells.add(cell.toString());
                cell.setLength(0);
            } else {
                cell.append(ch);
            }
        }
        cells.add(cell.toString());
        return cells;
    }

    private static long parseLong(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private static double ratio(double numerator, double denominator) {
        if (denominator <= 0.0) {
            return 0.0;
        }
        return numerator / denominator;
    }

    private static double passRate(SurefireSummary summary) {
        if (summary == null || !summary.available || summary.tests == 0) {
            return 0.0;
        }
        return ratio(summary.tests - summary.failures - summary.errors, summary.tests);
    }

    private static boolean hasChild(Element element, String childName) {
        return element.getElementsByTagName(childName).getLength() > 0;
    }

    private static int intAttr(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.trim().isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static double doubleAttr(Element element, String name) {
        String value = element.getAttribute(name);
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private static double safeDivide(double numerator, double denominator) {
        if (denominator == 0.0) {
            return 0.0;
        }
        return numerator / denominator;
    }

    private static String pct(double value) {
        return new DecimalFormat("0.00%").format(value);
    }

    private static String deltaPct(double value) {
        String sign = value >= 0.0 ? "+" : "";
        return sign + pct(value);
    }

    private static String sanitizeFileName(String name) {
        return name == null ? "unknown" : name.replaceAll("[^A-Za-z0-9_\\-.]", "_");
    }

    private static String two(double value) {
        return new DecimalFormat("0.00").format(value);
    }

    private static String millis(long millis) {
        if (millis <= 0L) {
            return "N/A";
        }
        BigDecimal seconds = new BigDecimal(millis).divide(new BigDecimal(1000), 2, RoundingMode.HALF_UP);
        return seconds.toPlainString() + "s";
    }

    private static String join(List<?> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(String.valueOf(values.get(i)));
        }
        return builder.toString();
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static Integer firstInteger(String text) {
        Matcher matcher = Pattern.compile("(\\d+)").matcher(text);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        }
        return null;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    static class Config {
        Path testDir = DEFAULT_TEST_DIR;
        Path sourceDir = DEFAULT_SOURCE_DIR;
        Path reportPath = DEFAULT_REPORT;
        Path fixResultDir = DEFAULT_FIX_RESULT_DIR;
        Path beforeTestDir = Paths.get("target", "vali-before-repair", "src", "test", "java");
        Path beforeCoverageCsv = Paths.get("target", "vali-before-repair", "jacoco.csv");
        Path afterCoverageCsv = DEFAULT_AFTER_JACOCO_CSV;
        Path beforeSurefireDir = Paths.get("target", "vali-before-repair", "surefire-reports");
        Path benchmarkDataPath = DEFAULT_BENCHMARK_DATA;
        Path repairImpactWorkDir = DEFAULT_REPAIR_IMPACT_WORK_DIR;
        boolean runTests;
        boolean runPit;
        boolean measureRepairImpact;
        int flakyRuns = 3;
        long pitTimeoutMinutes = 60L;
        long repairImpactTimeoutMinutes = 30L;
        String mavenExecutable;
        List<String> pitTargetClasses = new ArrayList<String>();
        List<String> pitTargetTests = new ArrayList<String>();

        static Config fromArgs(String[] args) throws IOException {
            Config config = new Config();
            for (String arg : args) {
                if ("--run-tests".equals(arg)) {
                    config.runTests = true;
                } else if ("--run-pit".equals(arg)) {
                    config.runPit = true;
                } else if ("--measure-repair-impact".equals(arg)) {
                    config.measureRepairImpact = true;
                } else if (arg.startsWith("--flaky-runs=")) {
                    config.flakyRuns = Integer.parseInt(arg.substring("--flaky-runs=".length()));
                } else if (arg.startsWith("--test-dir=")) {
                    config.testDir = Paths.get(arg.substring("--test-dir=".length()));
                } else if (arg.startsWith("--source-dir=")) {
                    config.sourceDir = Paths.get(arg.substring("--source-dir=".length()));
                } else if (arg.startsWith("--report=")) {
                    config.reportPath = Paths.get(arg.substring("--report=".length()));
                } else if (arg.startsWith("--fix-result-dir=")) {
                    config.fixResultDir = Paths.get(arg.substring("--fix-result-dir=".length()));
                } else if (arg.startsWith("--before-test-dir=")) {
                    config.beforeTestDir = Paths.get(arg.substring("--before-test-dir=".length()));
                } else if (arg.startsWith("--before-coverage-csv=")) {
                    config.beforeCoverageCsv = Paths.get(arg.substring("--before-coverage-csv=".length()));
                } else if (arg.startsWith("--after-coverage-csv=")) {
                    config.afterCoverageCsv = Paths.get(arg.substring("--after-coverage-csv=".length()));
                } else if (arg.startsWith("--before-surefire-dir=")) {
                    config.beforeSurefireDir = Paths.get(arg.substring("--before-surefire-dir=".length()));
                } else if (arg.startsWith("--benchmark-data=")) {
                    config.benchmarkDataPath = Paths.get(arg.substring("--benchmark-data=".length()));
                } else if (arg.startsWith("--repair-impact-work-dir=")) {
                    config.repairImpactWorkDir = Paths.get(arg.substring("--repair-impact-work-dir=".length()));
                } else if (arg.startsWith("--mvn=")) {
                    config.mavenExecutable = arg.substring("--mvn=".length());
                } else if (arg.startsWith("--pit-target-classes=")) {
                    config.pitTargetClasses = splitComma(arg.substring("--pit-target-classes=".length()));
                } else if (arg.startsWith("--pit-target-tests=")) {
                    config.pitTargetTests = splitComma(arg.substring("--pit-target-tests=".length()));
                } else if (arg.startsWith("--pit-timeout-minutes=")) {
                    config.pitTimeoutMinutes = Long.parseLong(arg.substring("--pit-timeout-minutes=".length()));
                } else if (arg.startsWith("--repair-impact-timeout-minutes=")) {
                    config.repairImpactTimeoutMinutes = Long.parseLong(arg.substring("--repair-impact-timeout-minutes=".length()));
                }
            }
            if (config.pitTargetClasses.isEmpty()) {
                config.pitTargetClasses = derivePitTargetClasses(config.testDir);
            }
            if (config.pitTargetTests.isEmpty()) {
                config.pitTargetTests.add("*Test");
            }
            return config;
        }

        String mavenCommand() {
            if (mavenExecutable != null && !mavenExecutable.trim().isEmpty()) {
                return mavenExecutable;
            }
            String hardcoded = "D:\\TopNew-SpringBoot\\maven\\apache-maven-3.9.1-bin\\apache-maven-3.9.1\\bin\\mvn.cmd";
            if (isWindows() && Files.exists(Paths.get(hardcoded))) {
                return hardcoded;
            }
            return isWindows() ? "mvn.cmd" : "mvn";
        }

        private static List<String> splitComma(String value) {
            List<String> result = new ArrayList<String>();
            for (String item : value.split(",")) {
                String trimmed = item.trim();
                if (!trimmed.isEmpty()) {
                    result.add(trimmed);
                }
            }
            return result;
        }

        private static List<String> derivePitTargetClasses(Path testDir) throws IOException {
            Set<String> packages = new LinkedHashSet<String>();
            if (!Files.isDirectory(testDir)) {
                return new ArrayList<String>();
            }
            List<Path> files = findFiles(testDir, "Test.java");
            for (Path file : files) {
                try {
                    CompilationUnit unit = StaticJavaParser.parse(file);
                    if (unit.getPackageDeclaration().isPresent()) {
                        Name name = unit.getPackageDeclaration().get().getName();
                        String packageName = name.asString();
                        if (!packageName.startsWith("zju.")) {
                            packages.add(packageName + ".*");
                        }
                    }
                } catch (Exception ignored) {
                    Path relative = testDir.relativize(file).getParent();
                    if (relative != null) {
                        String packageName = relative.toString().replace(File.separatorChar, '.');
                        if (!packageName.startsWith("zju.")) {
                            packages.add(packageName + ".*");
                        }
                    }
                }
            }
            return new ArrayList<String>(packages);
        }
    }

    static class EvaluationResult {
        Config config;
        Date startedAt;
        Date finishedAt;
        SourceMetrics sourceMetrics;
        SourceMetrics beforeSourceMetrics;
        SurefireSummary surefireSummaryBeforeRun;
        List<TestRunResult> testRuns = new ArrayList<TestRunResult>();
        FlakySummary flakySummary;
        MutationSummary mutationSummary;
        CommandResult pitCommandResult;
        CoverageSummary coverageBeforeRepair;
        CoverageSummary coverageAfterRepair;
        long executionTimeMillis;
        RepairCost repairCost;
        MinimalRepairImpact minimalRepairImpact;
        BenchmarkComparison benchmarkComparison;
        DefectDetection defectDetection;

        String toReport() {
            StringBuilder report = new StringBuilder();
            report.append("NewValiTest0616 validation report").append(System.lineSeparator());
            report.append("Generated at: ").append(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(finishedAt)).append(System.lineSeparator());
            report.append("Test directory: ").append(config.testDir.toAbsolutePath().normalize()).append(System.lineSeparator());
            report.append("Report file: ").append(config.reportPath.toAbsolutePath().normalize()).append(System.lineSeparator());
            report.append(System.lineSeparator());

            report.append("[Source quality]").append(System.lineSeparator());
            report.append("Test files: ").append(sourceMetrics.testFileCount).append(System.lineSeparator());
            report.append("Test methods: ").append(sourceMetrics.testMethodCount).append(System.lineSeparator());
            report.append("Assertions: ").append(sourceMetrics.assertionCount).append(System.lineSeparator());
            report.append("Meaningful assertions: ").append(sourceMetrics.meaningfulAssertionCount).append(System.lineSeparator());
            report.append("Weak assertions: ").append(sourceMetrics.weakAssertionCount).append(System.lineSeparator());
            report.append("Exception assertions: ").append(sourceMetrics.exceptionAssertionCount).append(System.lineSeparator());
            report.append("Assertion density: ").append(two(sourceMetrics.assertionsPerTestMethod)).append(" assertions/test method, ")
                    .append(two(sourceMetrics.assertionsPerHundredLines)).append(" assertions/100 code lines").append(System.lineSeparator());
            report.append("Meaningful assertion ratio: ").append(pct(sourceMetrics.meaningfulAssertionRatio)).append(System.lineSeparator());
            report.append("Parse errors: ").append(sourceMetrics.parseErrorCount).append(System.lineSeparator());
            report.append(System.lineSeparator());

            report.append("[Mutation score]").append(System.lineSeparator());
            if (mutationSummary.available) {
                report.append("PIT report: ").append(mutationSummary.reportFile).append(System.lineSeparator());
                report.append("Total mutants: ").append(mutationSummary.totalMutants).append(System.lineSeparator());
                report.append("Detected mutants: ").append(mutationSummary.detectedMutants).append(System.lineSeparator());
                report.append("Killed mutants: ").append(mutationSummary.killedMutants).append(System.lineSeparator());
                report.append("Survived mutants: ").append(mutationSummary.survivedMutants).append(System.lineSeparator());
                report.append("No coverage mutants: ").append(mutationSummary.noCoverageMutants).append(System.lineSeparator());
                report.append("Mutation score: ").append(pct(mutationSummary.mutationScore)).append(System.lineSeparator());
                report.append("Kill score: ").append(pct(mutationSummary.killScore)).append(System.lineSeparator());
            } else {
                report.append("N/A: ").append(mutationSummary.note).append(System.lineSeparator());
            }
            if (pitCommandResult != null) {
                report.append("PIT command exit code: ").append(pitCommandResult.exitCode)
                        .append(", time: ").append(millis(pitCommandResult.wallTimeMillis)).append(System.lineSeparator());
            }
            report.append(System.lineSeparator());

            report.append("[Defect detection]").append(System.lineSeparator());
            report.append("Score: ").append(pct(defectDetection.score)).append(System.lineSeparator());
            report.append("Basis: ").append(defectDetection.basis).append(System.lineSeparator());
            report.append(System.lineSeparator());

            report.append("[Flaky tests]").append(System.lineSeparator());
            if (flakySummary.available) {
                report.append("Runs: ").append(flakySummary.runCount).append(System.lineSeparator());
                report.append("Observed test cases: ").append(flakySummary.observedTestCases).append(System.lineSeparator());
                report.append("Flaky test cases: ").append(flakySummary.flakyTestCases).append(System.lineSeparator());
                report.append("Flaky ratio: ").append(pct(flakySummary.flakyRatio)).append(System.lineSeparator());
            } else {
                report.append("N/A: ").append(flakySummary.note).append(System.lineSeparator());
            }
            report.append(System.lineSeparator());

            report.append("[Execution time]").append(System.lineSeparator());
            report.append("Measured execution time: ").append(millis(executionTimeMillis)).append(System.lineSeparator());
            if (surefireSummaryBeforeRun.available) {
                report.append("Existing Surefire tests: ").append(surefireSummaryBeforeRun.tests)
                        .append(", failures: ").append(surefireSummaryBeforeRun.failures)
                        .append(", errors: ").append(surefireSummaryBeforeRun.errors)
                        .append(", skipped: ").append(surefireSummaryBeforeRun.skipped)
                        .append(", report time: ").append(millis(surefireSummaryBeforeRun.timeMillis))
                        .append(System.lineSeparator());
            } else {
                report.append("Existing Surefire summary: ").append(surefireSummaryBeforeRun.note).append(System.lineSeparator());
            }
            for (TestRunResult run : testRuns) {
                report.append("Run ").append(run.index).append(": exit=").append(run.commandResult.exitCode)
                        .append(", wall=").append(millis(run.wallTimeMillis))
                        .append(", tests=").append(run.surefireSummary.tests)
                        .append(", failures=").append(run.surefireSummary.failures)
                        .append(", errors=").append(run.surefireSummary.errors)
                        .append(System.lineSeparator());
            }
            report.append(System.lineSeparator());

            report.append("[Repair cost]").append(System.lineSeparator());
            report.append("Generated test files: ").append(repairCost.generatedTestFiles).append(System.lineSeparator());
            report.append("Generated test methods: ").append(repairCost.generatedTestMethods).append(System.lineSeparator());
            report.append("Generated non-comment lines: ").append(repairCost.generatedTestLines).append(System.lineSeparator());
            report.append("Current failing tests from Surefire: ").append(repairCost.currentFailingTests).append(System.lineSeparator());
            report.append("Test source parse errors: ").append(repairCost.parseErrorCount).append(System.lineSeparator());
            if (repairCost.generationLogSummary.available) {
                report.append("Latest generation log: ").append(repairCost.generationLogSummary.logFile).append(System.lineSeparator());
                report.append("Log success-like lines: ").append(repairCost.generationLogSummary.successLikeLines).append(System.lineSeparator());
                report.append("Log failure-like lines: ").append(repairCost.generationLogSummary.failureLikeLines).append(System.lineSeparator());
                report.append("Log repair-number lines: ").append(repairCost.generationLogSummary.repairNumberLines).append(System.lineSeparator());
                report.append("Log repair-number sum: ").append(repairCost.generationLogSummary.repairNumberSum).append(System.lineSeparator());
            } else {
                report.append("Generation log: ").append(repairCost.generationLogSummary.note).append(System.lineSeparator());
            }
            report.append(System.lineSeparator());

            report.append("[Minimal target repair impact]").append(System.lineSeparator());
            report.append("Fix result files: ").append(minimalRepairImpact.fixResultFileCount).append(System.lineSeparator());
            report.append("Files with fallback/comment strategy: ").append(minimalRepairImpact.fallbackFileCount).append(System.lineSeparator());
            report.append("Commented test methods: ").append(minimalRepairImpact.fallbackMethodsCommented).append(System.lineSeparator());
            report.append("Commented code lines: ").append(minimalRepairImpact.fallbackLinesCommented).append(System.lineSeparator());
            report.append("Commented assertions: ").append(minimalRepairImpact.fallbackAssertionsCommented).append(System.lineSeparator());
            report.append("Deleted test methods estimate: ").append(minimalRepairImpact.fallbackMethodsDeleted).append(System.lineSeparator());
            report.append("Deleted code lines estimate: ").append(minimalRepairImpact.fallbackLinesDeleted).append(System.lineSeparator());
            report.append("Deleted assertions estimate: ").append(minimalRepairImpact.fallbackAssertionsDeleted).append(System.lineSeparator());
            report.append("Total removed/commented methods: ").append(minimalRepairImpact.deletedOrCommentedMethods).append(System.lineSeparator());
            report.append("Total removed/commented lines: ").append(minimalRepairImpact.deletedOrCommentedLines).append(System.lineSeparator());
            report.append("Total removed/commented assertions: ").append(minimalRepairImpact.deletedOrCommentedAssertions).append(System.lineSeparator());
            if (minimalRepairImpact.autoMeasurementRequested) {
                report.append("Automatic measurement: enabled").append(System.lineSeparator());
                report.append("Snapshot pairs measured: ").append(minimalRepairImpact.snapshotPairCount).append(System.lineSeparator());
                report.append("Snapshot before files: ").append(minimalRepairImpact.snapshotBeforeFileCount).append(System.lineSeparator());
                report.append("Snapshot after files: ").append(minimalRepairImpact.snapshotAfterFileCount).append(System.lineSeparator());
                if (minimalRepairImpact.beforeMeasurement != null && minimalRepairImpact.beforeMeasurement.commandResult != null) {
                    report.append("Before measurement command exit code: ")
                            .append(minimalRepairImpact.beforeMeasurement.commandResult.exitCode)
                            .append(", time: ").append(millis(minimalRepairImpact.beforeMeasurement.commandResult.wallTimeMillis))
                            .append(System.lineSeparator());
                    if (minimalRepairImpact.beforeMeasurement.surefire.available) {
                        report.append("Before measurement tests: ")
                                .append(minimalRepairImpact.beforeMeasurement.surefire.tests)
                                .append(", failures: ").append(minimalRepairImpact.beforeMeasurement.surefire.failures)
                                .append(", errors: ").append(minimalRepairImpact.beforeMeasurement.surefire.errors)
                                .append(", skipped: ").append(minimalRepairImpact.beforeMeasurement.surefire.skipped)
                                .append(System.lineSeparator());
                    }
                    if (minimalRepairImpact.beforeMeasurement.commandLog != null) {
                        report.append("Before measurement log: ")
                                .append(minimalRepairImpact.beforeMeasurement.commandLog)
                                .append(System.lineSeparator());
                    }
                    if (!minimalRepairImpact.beforeMeasurement.fallbackSnapshotFiles.isEmpty()) {
                        report.append("Before snapshot files restored to current version: ")
                                .append(minimalRepairImpact.beforeMeasurement.fallbackSnapshotFiles.size())
                                .append(System.lineSeparator());
                    }
                }
                if (minimalRepairImpact.afterMeasurement != null && minimalRepairImpact.afterMeasurement.commandResult != null) {
                    report.append("After measurement command exit code: ")
                            .append(minimalRepairImpact.afterMeasurement.commandResult.exitCode)
                            .append(", time: ").append(millis(minimalRepairImpact.afterMeasurement.commandResult.wallTimeMillis))
                            .append(System.lineSeparator());
                    if (minimalRepairImpact.afterMeasurement.surefire.available) {
                        report.append("After measurement tests: ")
                                .append(minimalRepairImpact.afterMeasurement.surefire.tests)
                                .append(", failures: ").append(minimalRepairImpact.afterMeasurement.surefire.failures)
                                .append(", errors: ").append(minimalRepairImpact.afterMeasurement.surefire.errors)
                                .append(", skipped: ").append(minimalRepairImpact.afterMeasurement.surefire.skipped)
                                .append(System.lineSeparator());
                    }
                    if (minimalRepairImpact.afterMeasurement.commandLog != null) {
                        report.append("After measurement log: ")
                                .append(minimalRepairImpact.afterMeasurement.commandLog)
                                .append(System.lineSeparator());
                    }
                    if (!minimalRepairImpact.afterMeasurement.fallbackSnapshotFiles.isEmpty()) {
                        report.append("After snapshot files restored to current version: ")
                                .append(minimalRepairImpact.afterMeasurement.fallbackSnapshotFiles.size())
                                .append(System.lineSeparator());
                    }
                }
            } else {
                report.append("Automatic measurement: disabled; use --measure-repair-impact to generate before/after data from fix logs.").append(System.lineSeparator());
            }
            if (minimalRepairImpact.coverageBefore != null && minimalRepairImpact.coverageBefore.available) {
                report.append("Coverage before repair: line=").append(pct(minimalRepairImpact.coverageBefore.lineCoverage))
                        .append(", branch=").append(pct(minimalRepairImpact.coverageBefore.branchCoverage))
                        .append(", method=").append(pct(minimalRepairImpact.coverageBefore.methodCoverage)).append(System.lineSeparator());
                if (minimalRepairImpact.beforeCoverageCsv != null) {
                    report.append("Coverage before source: ").append(minimalRepairImpact.beforeCoverageCsv).append(System.lineSeparator());
                }
            } else {
                report.append("Coverage before repair: N/A");
                if (minimalRepairImpact.beforeCoverageCsv != null) {
                    report.append(" (missing ").append(minimalRepairImpact.beforeCoverageCsv).append(")");
                }
                report.append(System.lineSeparator());
            }
            if (minimalRepairImpact.coverageAfter != null && minimalRepairImpact.coverageAfter.available) {
                report.append("Coverage after repair: line=").append(pct(minimalRepairImpact.coverageAfter.lineCoverage))
                        .append(", branch=").append(pct(minimalRepairImpact.coverageAfter.branchCoverage))
                        .append(", method=").append(pct(minimalRepairImpact.coverageAfter.methodCoverage)).append(System.lineSeparator());
                if (minimalRepairImpact.afterCoverageCsv != null) {
                    report.append("Coverage after source: ").append(minimalRepairImpact.afterCoverageCsv).append(System.lineSeparator());
                }
            } else {
                report.append("Coverage after repair: N/A");
                if (minimalRepairImpact.afterCoverageCsv != null) {
                    report.append(" (missing ").append(minimalRepairImpact.afterCoverageCsv).append(")");
                }
                report.append(System.lineSeparator());
            }
            if (minimalRepairImpact.coverageBefore != null && minimalRepairImpact.coverageBefore.available
                    && minimalRepairImpact.coverageAfter != null && minimalRepairImpact.coverageAfter.available) {
                report.append("Coverage change: line=")
                        .append(pct(minimalRepairImpact.coverageBefore.lineCoverage)).append(" -> ")
                        .append(pct(minimalRepairImpact.coverageAfter.lineCoverage)).append(" (")
                        .append(deltaPct(minimalRepairImpact.coverageAfter.lineCoverage - minimalRepairImpact.coverageBefore.lineCoverage)).append(")")
                        .append(", branch=")
                        .append(pct(minimalRepairImpact.coverageBefore.branchCoverage)).append(" -> ")
                        .append(pct(minimalRepairImpact.coverageAfter.branchCoverage)).append(" (")
                        .append(deltaPct(minimalRepairImpact.coverageAfter.branchCoverage - minimalRepairImpact.coverageBefore.branchCoverage)).append(")")
                        .append(", method=")
                        .append(pct(minimalRepairImpact.coverageBefore.methodCoverage)).append(" -> ")
                        .append(pct(minimalRepairImpact.coverageAfter.methodCoverage)).append(" (")
                        .append(deltaPct(minimalRepairImpact.coverageAfter.methodCoverage - minimalRepairImpact.coverageBefore.methodCoverage)).append(")")
                        .append(System.lineSeparator());
            } else {
                report.append("Coverage change: N/A because before or after coverage data is unavailable.").append(System.lineSeparator());
            }
            report.append("Pass rate before repair: ")
                    .append(minimalRepairImpact.hasBeforePassRate ? pct(minimalRepairImpact.passRateBefore) : "N/A")
                    .append(System.lineSeparator());
            if (minimalRepairImpact.beforeSurefireDir != null) {
                report.append("Pass rate before source: ").append(minimalRepairImpact.beforeSurefireDir).append(System.lineSeparator());
            }
            report.append("Pass rate after repair: ")
                    .append(minimalRepairImpact.hasAfterPassRate ? pct(minimalRepairImpact.passRateAfter) : "N/A")
                    .append(System.lineSeparator());
            if (minimalRepairImpact.afterSurefireDir != null) {
                report.append("Pass rate after source: ").append(minimalRepairImpact.afterSurefireDir).append(System.lineSeparator());
            }
            if (minimalRepairImpact.hasBeforePassRate && minimalRepairImpact.hasAfterPassRate) {
                report.append("Pass rate change: ")
                        .append(pct(minimalRepairImpact.passRateBefore)).append(" -> ")
                        .append(pct(minimalRepairImpact.passRateAfter)).append(" (")
                        .append(deltaPct(minimalRepairImpact.passRateAfter - minimalRepairImpact.passRateBefore)).append(")")
                        .append(System.lineSeparator());
            } else {
                report.append("Pass rate change: N/A because before or after Surefire data is unavailable.").append(System.lineSeparator());
            }
            if (minimalRepairImpact.note != null) {
                report.append("Note: ").append(minimalRepairImpact.note).append(System.lineSeparator());
            }
            report.append(System.lineSeparator());

            report.append("[External tool benchmark data]").append(System.lineSeparator());
            report.append("Expected tools: ChatUniTest[8], BRMiner[28], TestSpark[29], EvoSuite[3], A3Test[10], AthenaTest[30], SymPrompt[31], ChatTester[7], HITS[21]").append(System.lineSeparator());
            report.append("Expected projects: Commons-Cli[24], Commons-Csv[25], Commons-Lang[26], Gson[27]").append(System.lineSeparator());
            if (benchmarkComparison.available) {
                report.append("Benchmark data file: ").append(benchmarkComparison.dataFile).append(System.lineSeparator());
                report.append("Rows loaded: ").append(benchmarkComparison.records.size()).append(System.lineSeparator());
                for (BenchmarkRecord record : benchmarkComparison.records) {
                    report.append("  ").append(record.values).append(System.lineSeparator());
                }
            } else {
                report.append("N/A: ").append(benchmarkComparison.note).append(System.lineSeparator());
            }
            report.append(System.lineSeparator());

            report.append("[Tool usage]").append(System.lineSeparator());
            report.append("PIT mutation score: mvn exec:java \"-Dexec.mainClass=zju.cst.aces.runner.NewValiTest0616\" \"-Dexec.args=--run-pit --pit-target-classes=cli.*,csv.*,gson.*,lang3.*\"").append(System.lineSeparator());
            report.append("Flaky ratio and execution time: mvn exec:java \"-Dexec.mainClass=zju.cst.aces.runner.NewValiTest0616\" \"-Dexec.args=--run-tests --flaky-runs=3\"").append(System.lineSeparator());
            report.append("Before/after repair impact: use --measure-repair-impact to build snapshots from src/F logs, or provide --before-test-dir, --before-coverage-csv and --before-surefire-dir when historical snapshots exist.").append(System.lineSeparator());
            report.append("External tools comparison: create a CSV/TSV with columns tool,project,mutationScore,assertionDensity,meaningfulAssertions,defectDetection,flakyRatio,executionTime,repairCost,source and pass --benchmark-data=path.").append(System.lineSeparator());
            report.append(System.lineSeparator());

            report.append("Usage hints:").append(System.lineSeparator());
            report.append("  mvn exec:java \"-Dexec.mainClass=zju.cst.aces.runner.NewValiTest0616\"").append(System.lineSeparator());
            report.append("  mvn exec:java \"-Dexec.mainClass=zju.cst.aces.runner.NewValiTest0616\" \"-Dexec.args=--run-tests --flaky-runs=3\"").append(System.lineSeparator());
            report.append("  mvn exec:java \"-Dexec.mainClass=zju.cst.aces.runner.NewValiTest0616\" \"-Dexec.args=--run-pit --pit-target-classes=cli.*,csv.*,gson.*,lang3.*\"").append(System.lineSeparator());
            report.append("  mvn exec:java \"-Dexec.mainClass=zju.cst.aces.runner.NewValiTest0616\" \"-Dexec.args=--measure-repair-impact\"").append(System.lineSeparator());
            return report.toString();
        }
    }

    static class SourceMetrics {
        Path testDir;
        int testFileCount;
        int testMethodCount;
        int assertionCount;
        int meaningfulAssertionCount;
        int weakAssertionCount;
        int exceptionAssertionCount;
        int nonCommentCodeLines;
        int parseErrorCount;
        double assertionsPerTestMethod;
        double meaningfulAssertionsPerTestMethod;
        double assertionsPerHundredLines;
        double meaningfulAssertionRatio;
        List<FileMetrics> files = new ArrayList<FileMetrics>();
        List<String> notes = new ArrayList<String>();
    }

    static class FileMetrics {
        Path path;
        String packageName;
        int testMethodCount;
        int assertionCount;
        int meaningfulAssertionCount;
        int weakAssertionCount;
        int exceptionAssertionCount;
        int nonCommentCodeLines;
        String parseError;
    }

    static class MutationSummary {
        boolean available;
        Path pitDir;
        Path reportFile;
        int totalMutants;
        int detectedMutants;
        int killedMutants;
        int survivedMutants;
        int noCoverageMutants;
        double mutationScore;
        double killScore;
        String note;
        Map<String, Integer> otherStatuses = new LinkedHashMap<String, Integer>();
    }

    static class CoverageSummary {
        boolean available;
        Path csvFile;
        long instructionMissed;
        long instructionCovered;
        long branchMissed;
        long branchCovered;
        long lineMissed;
        long lineCovered;
        long methodMissed;
        long methodCovered;
        double instructionCoverage;
        double branchCoverage;
        double lineCoverage;
        double methodCoverage;
        String note;
    }

    static class MinimalRepairImpact {
        Path fixResultDir;
        Path beforeTestDir;
        Path afterTestDir;
        int fixResultFileCount;
        int fallbackFileCount;
        int fallbackMethodsCommented;
        int fallbackLinesCommented;
        int fallbackAssertionsCommented;
        int fallbackMethodsDeleted;
        int fallbackLinesDeleted;
        int fallbackAssertionsDeleted;
        int deletedOrCommentedMethods;
        int deletedOrCommentedLines;
        int deletedOrCommentedAssertions;
        int beforeMethods;
        int afterMethods;
        int beforeLines;
        int afterLines;
        int beforeAssertions;
        int afterAssertions;
        CoverageSummary coverageBefore;
        CoverageSummary coverageAfter;
        Path beforeCoverageCsv;
        Path afterCoverageCsv;
        Path beforeSurefireDir;
        Path afterSurefireDir;
        boolean hasBeforePassRate;
        boolean hasAfterPassRate;
        double passRateBefore;
        double passRateAfter;
        boolean autoMeasurementRequested;
        int snapshotPairCount;
        int snapshotBeforeFileCount;
        int snapshotAfterFileCount;
        RepairImpactMeasurement beforeMeasurement;
        RepairImpactMeasurement afterMeasurement;
        String note;
        List<FixLogImpact> fixLogImpacts = new ArrayList<FixLogImpact>();
    }

    static class FixLogImpact {
        Path file;
        boolean hasFallback;
        int methodsCommented;
        int linesCommented;
        int assertionsCommented;
        int methodsDeleted;
        int linesDeleted;
        int assertionsDeleted;
    }

    static class CodeBlock {
        String label;
        String code;
    }

    static class JavaClassLocation {
        boolean available;
        String packageName = "";
        String className = "";
    }

    static class RepairImpactSnapshots {
        boolean available;
        Path beforeTestDir;
        Path afterTestDir;
        int snapshotPairCount;
        int beforeFileCount;
        int afterFileCount;
        String note;
    }

    static class RepairImpactMeasurement {
        CommandResult commandResult;
        Path commandLog;
        CoverageSummary coverage = new CoverageSummary();
        SurefireSummary surefire = new SurefireSummary();
        Set<String> fallbackSnapshotFiles = new LinkedHashSet<String>();
    }

    static class BenchmarkComparison {
        boolean available;
        Path dataFile;
        String note;
        List<BenchmarkRecord> records = new ArrayList<BenchmarkRecord>();

        static BenchmarkComparison defaultEmpty() {
            BenchmarkComparison comparison = new BenchmarkComparison();
            comparison.available = false;
            return comparison;
        }
    }

    static class BenchmarkRecord {
        Map<String, String> values = new LinkedHashMap<String, String>();
    }

    static class SurefireSummary {
        boolean available;
        Path directory;
        int testSuites;
        int tests;
        int testCases;
        int failures;
        int errors;
        int skipped;
        long timeMillis;
        String note;
        Map<String, TestCaseStatus> testCaseStatuses = new LinkedHashMap<String, TestCaseStatus>();
    }

    static class TestCaseStatus {
        String className;
        String methodName;
        boolean failed;
        boolean skipped;
        long timeMillis;

        String id() {
            return className + "#" + methodName;
        }
    }

    static class TestRunResult {
        int index;
        CommandResult commandResult;
        SurefireSummary surefireSummary;
        long wallTimeMillis;
    }

    static class FlakySummary {
        boolean available;
        int runCount;
        int observedTestCases;
        int flakyTestCases;
        double flakyRatio;
        String note;

        static FlakySummary notMeasured(String note) {
            FlakySummary summary = new FlakySummary();
            summary.available = false;
            summary.note = note;
            return summary;
        }
    }

    static class RepairCost {
        int generatedTestFiles;
        int generatedTestMethods;
        int generatedTestLines;
        int currentFailingTests;
        int parseErrorCount;
        GenerationLogSummary generationLogSummary;
    }

    static class GenerationLogSummary {
        boolean available;
        Path logFile;
        int successLikeLines;
        int failureLikeLines;
        int repairNumberLines;
        int repairNumberSum;
        String note;
    }

    static class DefectDetection {
        boolean available;
        double score;
        String basis;
    }

    static class CommandResult {
        String command;
        int exitCode;
        boolean timedOut;
        long wallTimeMillis;
        List<String> output = new ArrayList<String>();
    }
}
