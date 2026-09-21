package zju.cst.aces.runner;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class NewValiTest0629 {
    private static final Path DEFAULT_OUTPUT_ROOT = Paths.get("target", "new-vali-test-0629");
    private static final Path FALLBACK_DATA_ROOT = Paths.get("D:\\2025_7_8\\TestProModify\\data0629");
    private static final List<ProjectData> DEFAULT_PROJECTS = Arrays.asList(
            new ProjectData("Commons-codec", "codec"),
            new ProjectData("JDom2", "jdom2"),
            new ProjectData("Datafaker", "datafaker"),
            new ProjectData("Chart", "chart")
    );

    public static void main(String[] args) throws Exception {
        Config config = Config.fromArgs(args);
        if (!config.inProjectWorker && !sameDirectory(Paths.get(System.getProperty("user.dir")), config.projectRoot)) {
            CommandResult worker = rerunInProjectRoot(config, args);
            System.out.print(worker.output);
            if (worker.exitCode != 0) {
                throw new IllegalStateException("NewValiTest0629 worker failed with exit code " + worker.exitCode);
            }
            return;
        }

        NewValiTest0629 runner = new NewValiTest0629();
        RunSummary summary = runner.run(config);
        System.out.println(summary.toText());
    }

    RunSummary run(Config config) throws Exception {
        Files.createDirectories(config.logsDir);
        Files.createDirectories(config.testsDir);
        Files.createDirectories(config.coverageDir);

        RunSummary summary = new RunSummary();
        summary.startedAt = new Date();
        summary.outputRoot = config.outputRoot.toAbsolutePath().normalize();
        summary.dataRoot = config.dataRoot.toAbsolutePath().normalize();
        summary.logsDir = config.logsDir.toAbsolutePath().normalize();
        summary.testsDir = config.testsDir.toAbsolutePath().normalize();
        summary.coverageDir = config.coverageDir.toAbsolutePath().normalize();

        for (ProjectData project : config.projects) {
            ProjectResult result = runProject(config, project);
            summary.results.add(result);
        }

        summary.finishedAt = new Date();
        Path summaryFile = config.outputRoot.resolve("NewValiTest0629-summary.txt");
        writeText(summaryFile, summary.toText());
        summary.summaryFile = summaryFile.toAbsolutePath().normalize();
        return summary;
    }

    private ProjectResult runProject(Config config, ProjectData project) throws Exception {
        ProjectResult result = new ProjectResult();
        result.projectName = project.name;
        result.packageName = project.packageName;
        result.dataSourceDir = config.dataRoot.resolve(project.packageName).toAbsolutePath().normalize();
        result.sourceDir = config.projectRoot.resolve("src").resolve("main").resolve("java")
                .resolve(project.packageName.replace('.', File.separatorChar))
                .toAbsolutePath().normalize();

        Path projectLogDir = config.logsDir.resolve(project.safeName());
        Path projectTestDir = config.testsDir.resolve(project.safeName());
        Path projectCoverageDir = config.coverageDir.resolve(project.safeName());
        Files.createDirectories(projectLogDir);
        Files.createDirectories(projectTestDir);
        Files.createDirectories(projectCoverageDir);

        result.logDir = projectLogDir.toAbsolutePath().normalize();
        result.testCodeDir = projectTestDir.toAbsolutePath().normalize();
        result.coverageDir = projectCoverageDir.toAbsolutePath().normalize();

        String timestamp = new SimpleDateFormat("MMdd_HHmmss").format(new Date());
        Path logFile = projectLogDir.resolve(project.safeName() + "_ValiFixTest_" + timestamp + ".txt");
        result.mainLogFile = logFile.toAbsolutePath().normalize();

        if (!Files.isDirectory(result.dataSourceDir)) {
            result.success = false;
            result.message = "source directory is missing";
            writeText(logFile, "Dataset source directory does not exist: " + result.dataSourceDir + System.lineSeparator());
            result.logFiles = countFiles(projectLogDir, null);
            return result;
        }

        if (config.checkOnly) {
            result.success = true;
            result.message = "check only; dataset directory is available";
            writeText(logFile, "Dataset source directory is available: " + result.dataSourceDir + System.lineSeparator());
            result.logFiles = countFiles(projectLogDir, null);
            return result;
        }

        long projectStartMillis = System.currentTimeMillis();
        try {
            syncProjectSource(result.dataSourceDir, result.sourceDir);
            ValiFixTextCoreDemo5.ValiFixTest(result.sourceDir.toString(), logFile.toString());
            result.success = true;
            result.message = "ValiFixTest finished";
        } catch (Exception ex) {
            result.success = false;
            result.message = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            appendText(logFile, System.lineSeparator() + "ValiFixTest failed: " + result.message + System.lineSeparator());
        }

        collectProjectArtifacts(config, project, projectLogDir, projectTestDir, projectCoverageDir, result, projectStartMillis);
        return result;
    }

    private void collectProjectArtifacts(Config config, ProjectData project, Path projectLogDir,
                                         Path projectTestDir, Path projectCoverageDir, ProjectResult result)
            throws IOException, InterruptedException {
        collectProjectArtifacts(config, project, projectLogDir, projectTestDir, projectCoverageDir, result, 0L);
    }

    private void collectProjectArtifacts(Config config, ProjectData project, Path projectLogDir,
                                         Path projectTestDir, Path projectCoverageDir, ProjectResult result,
                                         long projectStartMillis)
            throws IOException, InterruptedException {
        copyProjectTests(config.projectRoot, project.packageName, projectTestDir);
        result.generatedTestFiles = countFiles(projectTestDir, ".java");
        copyGeneratedLogs(config.projectRoot, projectLogDir, projectStartMillis);

        Path surefireDir = config.projectRoot.resolve("target").resolve("surefire-reports");
        Path surefireTarget = projectLogDir.resolve("surefire-reports");
        copyDirectoryIfExists(surefireDir, surefireTarget);

        if (config.runCoverage) {
            CommandResult coverageCommand = runMaven(config, Arrays.asList("-Dtest=" + project.packageName + ".*Test", "test", "jacoco:report"));
            result.coverageCommandExitCode = coverageCommand.exitCode;
            result.coverageCommandLog = projectLogDir.resolve("maven-coverage.log").toAbsolutePath().normalize();
            writeText(result.coverageCommandLog, coverageCommand.output);
        }

        Path jacocoDir = config.projectRoot.resolve("target").resolve("site").resolve("jacoco");
        Path jacocoCsv = jacocoDir.resolve("jacoco.csv");
        if (Files.exists(jacocoDir)) {
            copyDirectoryIfExists(jacocoDir, projectCoverageDir.resolve("jacoco"));
        }
        if (Files.isRegularFile(jacocoCsv)) {
            Files.copy(jacocoCsv, projectCoverageDir.resolve(project.safeName() + "_jacoco.csv"),
                    StandardCopyOption.REPLACE_EXISTING);
            result.coverageCsv = projectCoverageDir.resolve(project.safeName() + "_jacoco.csv").toAbsolutePath().normalize();
        }

        result.coverageFiles = countFiles(projectCoverageDir, null);
        result.logFiles = countFiles(projectLogDir, null);
    }

    private static void copyProjectTests(Path projectRoot, String packageName, Path targetDir) throws IOException {
        Path source = projectRoot.resolve("src").resolve("test").resolve("java")
                .resolve(packageName.replace('.', File.separatorChar));
        copyDirectoryIfExists(source, targetDir);
    }

    private static void copyGeneratedLogs(Path projectRoot, Path projectLogDir, long notBeforeMillis) throws IOException {
        copyRecentDirectory(projectRoot.resolve("src").resolve("A"), projectLogDir.resolve("analysis"), notBeforeMillis);
        copyRecentDirectory(projectRoot.resolve("src").resolve("V"), projectLogDir.resolve("validation"), notBeforeMillis);
        copyRecentDirectory(projectRoot.resolve("src").resolve("F"), projectLogDir.resolve("fix"), notBeforeMillis);
    }

    private static void syncProjectSource(Path dataSourceDir, Path projectSourceDir) throws IOException {
        if (Files.isDirectory(projectSourceDir)) {
            deleteDirectory(projectSourceDir);
        }
        copyDirectoryIfExists(dataSourceDir, projectSourceDir);
    }

    private static CommandResult runMaven(Config config, List<String> arguments) throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(config.mavenCommand());
        command.addAll(arguments);

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(config.projectRoot.toFile());
        builder.redirectErrorStream(true);

        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), isWindows() ? Charset.forName("GBK") : StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }

        boolean completed = process.waitFor(config.mavenTimeoutMinutes, TimeUnit.MINUTES);
        CommandResult result = new CommandResult();
        result.command = join(command, " ");
        result.output = output.toString();
        if (completed) {
            result.exitCode = process.exitValue();
        } else {
            process.destroyForcibly();
            result.exitCode = -1;
            result.output += System.lineSeparator() + "Command timed out." + System.lineSeparator();
        }
        return result;
    }

    private static CommandResult rerunInProjectRoot(Config config, String[] originalArgs)
            throws IOException, InterruptedException {
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable());
        command.add("-cp");
        command.add(absoluteClasspath());
        command.add(NewValiTest0629.class.getName());
        command.add("--in-project-worker");
        command.add("--project-root=" + config.projectRoot.toAbsolutePath().normalize());
        for (String arg : originalArgs) {
            if (!"--in-project-worker".equals(arg) && !arg.startsWith("--project-root=")) {
                command.add(arg);
            }
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        builder.directory(config.projectRoot.toFile());
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(
                process.getInputStream(), isWindows() ? Charset.forName("GBK") : StandardCharsets.UTF_8));
        StringBuilder output = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            output.append(line).append(System.lineSeparator());
        }
        int exitCode = process.waitFor();
        CommandResult result = new CommandResult();
        result.command = join(command, " ");
        result.output = output.toString();
        result.exitCode = exitCode;
        return result;
    }

    private static String javaExecutable() {
        Path javaHome = Paths.get(System.getProperty("java.home"), "bin", isWindows() ? "java.exe" : "java");
        if (Files.exists(javaHome)) {
            return javaHome.toString();
        }
        return isWindows() ? "java.exe" : "java";
    }

    private static String absoluteClasspath() {
        String[] entries = System.getProperty("java.class.path", "").split(PatternQuote.pathSeparator());
        List<String> absoluteEntries = new ArrayList<String>();
        Path cwd = Paths.get(System.getProperty("user.dir"));
        for (String entry : entries) {
            if (entry == null || entry.trim().isEmpty()) {
                continue;
            }
            Path path = Paths.get(entry);
            if (!path.isAbsolute()) {
                path = cwd.resolve(path);
            }
            absoluteEntries.add(path.normalize().toString());
        }
        return join(absoluteEntries, File.pathSeparator);
    }

    private static void copyDirectoryIfExists(final Path source, final Path target) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Files.createDirectories(target);
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void copyRecentDirectory(final Path source, final Path target, final long notBeforeMillis) throws IOException {
        if (!Files.isDirectory(source)) {
            return;
        }
        Files.createDirectories(target);
        Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                if (notBeforeMillis <= 0L || Files.getLastModifiedTime(file).toMillis() >= notBeforeMillis - 1000L) {
                    Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                }
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(final Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return;
        }
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path directory, IOException exc) throws IOException {
                Files.delete(directory);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static int countFiles(Path dir, final String suffix) throws IOException {
        if (!Files.isDirectory(dir)) {
            return 0;
        }
        final int[] count = new int[]{0};
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                if (suffix == null || file.getFileName().toString().endsWith(suffix)) {
                    count[0]++;
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return count[0];
    }

    private static void writeText(Path file, String text) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, text.getBytes(StandardCharsets.UTF_8));
    }

    private static void appendText(Path file, String text) throws IOException {
        Path parent = file.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(file, text.getBytes(StandardCharsets.UTF_8),
                Files.exists(file) ? java.nio.file.StandardOpenOption.APPEND : java.nio.file.StandardOpenOption.CREATE);
    }

    private static String join(List<String> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(delimiter);
            }
            builder.append(values.get(i));
        }
        return builder.toString();
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    private static boolean sameDirectory(Path left, Path right) throws IOException {
        Path leftReal = Files.exists(left) ? left.toRealPath() : left.toAbsolutePath().normalize();
        Path rightReal = Files.exists(right) ? right.toRealPath() : right.toAbsolutePath().normalize();
        return leftReal.equals(rightReal);
    }

    static class Config {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path dataRoot = null;
        Path outputRoot = DEFAULT_OUTPUT_ROOT;
        Path logsDir = DEFAULT_OUTPUT_ROOT.resolve("logs");
        Path testsDir = DEFAULT_OUTPUT_ROOT.resolve("test-code");
        Path coverageDir = DEFAULT_OUTPUT_ROOT.resolve("coverage-reports");
        String mavenExecutable;
        boolean runCoverage = true;
        boolean inProjectWorker;
        boolean checkOnly;
        long mavenTimeoutMinutes = 30L;
        List<ProjectData> projects = new ArrayList<ProjectData>(DEFAULT_PROJECTS);

        static Config fromArgs(String[] args) {
            Config config = new Config();
            for (String arg : args) {
                if (arg.startsWith("--project-root=")) {
                    config.projectRoot = Paths.get(arg.substring("--project-root=".length()));
                } else if (arg.startsWith("--data-root=")) {
                    config.dataRoot = Paths.get(arg.substring("--data-root=".length()));
                } else if (arg.startsWith("--output-root=")) {
                    config.outputRoot = Paths.get(arg.substring("--output-root=".length()));
                    config.logsDir = config.outputRoot.resolve("logs");
                    config.testsDir = config.outputRoot.resolve("test-code");
                    config.coverageDir = config.outputRoot.resolve("coverage-reports");
                } else if (arg.startsWith("--logs-dir=")) {
                    config.logsDir = Paths.get(arg.substring("--logs-dir=".length()));
                } else if (arg.startsWith("--tests-dir=")) {
                    config.testsDir = Paths.get(arg.substring("--tests-dir=".length()));
                } else if (arg.startsWith("--coverage-dir=")) {
                    config.coverageDir = Paths.get(arg.substring("--coverage-dir=".length()));
                } else if (arg.startsWith("--mvn=")) {
                    config.mavenExecutable = arg.substring("--mvn=".length());
                } else if (arg.startsWith("--maven-timeout-minutes=")) {
                    config.mavenTimeoutMinutes = Long.parseLong(arg.substring("--maven-timeout-minutes=".length()));
                } else if ("--skip-coverage-run".equals(arg)) {
                    config.runCoverage = false;
                } else if ("--in-project-worker".equals(arg)) {
                    config.inProjectWorker = true;
                } else if ("--check-only".equals(arg)) {
                    config.checkOnly = true;
                } else if (arg.startsWith("--projects=")) {
                    config.projects = parseProjects(arg.substring("--projects=".length()));
                }
            }
            config.projectRoot = config.projectRoot.toAbsolutePath().normalize();
            if (config.dataRoot == null) {
                Path packagedDataRoot = config.projectRoot.resolve("data0629");
                config.dataRoot = Files.isDirectory(packagedDataRoot) ? packagedDataRoot : FALLBACK_DATA_ROOT;
            }
            config.dataRoot = config.dataRoot.toAbsolutePath().normalize();
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

        private static List<ProjectData> parseProjects(String value) {
            Map<String, String> aliases = new LinkedHashMap<String, String>();
            for (ProjectData project : DEFAULT_PROJECTS) {
                aliases.put(project.name.toLowerCase(Locale.ROOT), project.packageName);
            }

            List<ProjectData> result = new ArrayList<ProjectData>();
            for (String item : value.split(",")) {
                String trimmed = item.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                String[] pair = trimmed.split(":", 2);
                String name = pair[0].trim();
                String packageName = pair.length > 1 ? pair[1].trim() : aliases.get(name.toLowerCase(Locale.ROOT));
                if (packageName == null || packageName.isEmpty()) {
                    packageName = name;
                }
                result.add(new ProjectData(name, packageName));
            }
            return result;
        }
    }

    static class ProjectData {
        final String name;
        final String packageName;

        ProjectData(String name, String packageName) {
            this.name = name;
            this.packageName = packageName;
        }

        String safeName() {
            return name.replaceAll("[^A-Za-z0-9_.-]", "_");
        }
    }

    static class RunSummary {
        Date startedAt;
        Date finishedAt;
        Path outputRoot;
        Path dataRoot;
        Path logsDir;
        Path testsDir;
        Path coverageDir;
        Path summaryFile;
        List<ProjectResult> results = new ArrayList<ProjectResult>();

        String toText() {
            StringBuilder text = new StringBuilder();
            text.append("NewValiTest0629 summary").append(System.lineSeparator());
            text.append("Started at: ").append(format(startedAt)).append(System.lineSeparator());
            text.append("Finished at: ").append(format(finishedAt)).append(System.lineSeparator());
            text.append("Output root: ").append(outputRoot).append(System.lineSeparator());
            text.append("Data root: ").append(dataRoot).append(System.lineSeparator());
            text.append("Logs directory: ").append(logsDir).append(System.lineSeparator());
            text.append("Test code directory: ").append(testsDir).append(System.lineSeparator());
            text.append("Coverage directory: ").append(coverageDir).append(System.lineSeparator());
            if (summaryFile != null) {
                text.append("Summary file: ").append(summaryFile).append(System.lineSeparator());
            }
            text.append(System.lineSeparator());

            for (ProjectResult result : results) {
                text.append("[").append(result.projectName).append("]").append(System.lineSeparator());
                text.append("Package: ").append(result.packageName).append(System.lineSeparator());
                text.append("Dataset directory: ").append(result.dataSourceDir).append(System.lineSeparator());
                text.append("Source directory: ").append(result.sourceDir).append(System.lineSeparator());
                text.append("Status: ").append(result.success ? "SUCCESS" : "FAILED").append(" - ")
                        .append(result.message).append(System.lineSeparator());
                text.append("Main log: ").append(result.mainLogFile).append(System.lineSeparator());
                text.append("Log files: ").append(result.logFiles).append(System.lineSeparator());
                text.append("Generated test files copied: ").append(result.generatedTestFiles)
                        .append(" -> ").append(result.testCodeDir).append(System.lineSeparator());
                text.append("Coverage files copied: ").append(result.coverageFiles)
                        .append(" -> ").append(result.coverageDir).append(System.lineSeparator());
                if (result.coverageCsv != null) {
                    text.append("Coverage CSV: ").append(result.coverageCsv).append(System.lineSeparator());
                }
                if (result.coverageCommandLog != null) {
                    text.append("Coverage command exit code: ").append(result.coverageCommandExitCode)
                            .append(", log: ").append(result.coverageCommandLog).append(System.lineSeparator());
                }
                text.append(System.lineSeparator());
            }
            return text.toString();
        }

        private static String format(Date date) {
            if (date == null) {
                return "N/A";
            }
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
        }
    }

    static class ProjectResult {
        String projectName;
        String packageName;
        boolean success;
        String message;
        Path sourceDir;
        Path dataSourceDir;
        Path logDir;
        Path testCodeDir;
        Path coverageDir;
        Path mainLogFile;
        Path coverageCommandLog;
        Path coverageCsv;
        int coverageCommandExitCode;
        int generatedTestFiles;
        int coverageFiles;
        int logFiles;
    }

    static class CommandResult {
        String command;
        String output;
        int exitCode;
    }

    static class PatternQuote {
        static String pathSeparator() {
            return java.util.regex.Pattern.quote(File.pathSeparator);
        }
    }
}
