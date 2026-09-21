package lang3;
import org.apache.maven.surefire.shared.lang3.CharUtils;
import org.apache.maven.surefire.shared.lang3.StringUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
public class RuntimeEnvironment {
    private static boolean fileExists(final String path) {
        return Files.exists(Paths.get(path));
    }
    public static Boolean inContainer() {
        return inContainer(StringUtils.EMPTY);
    }
    static boolean inContainer(final String dirPrefix) {
        final String value = readFile(dirPrefix + "/proc/1/environ", "container");
        if (value != null) {
            return !value.isEmpty();
        }
        return fileExists(dirPrefix + "/.dockerenv") || fileExists(dirPrefix + "/run/.containerenv");
    }
    private static String readFile(final String envVarFile, final String key) {
        try {
            final byte[] bytes = Files.readAllBytes(Paths.get(envVarFile));
            final String content = new String(bytes, Charset.defaultCharset());
            final String[] lines = content.split(String.valueOf(CharUtils.NUL));
            final String prefix = key + "=";
            return Arrays.stream(lines)
                    .filter(line -> line.startsWith(prefix))
                    .map(line -> line.split("=", 2))
                    .map(keyValue -> keyValue[1])
                    .findFirst()
                    .orElse(null);
        } catch (final IOException e) {
            return null;
        }
    }
    @Deprecated
    public RuntimeEnvironment() {
    }
}