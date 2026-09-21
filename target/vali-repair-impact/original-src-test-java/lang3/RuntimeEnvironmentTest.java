package lang3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import static org.junit.jupiter.api.Assertions.*;

class RuntimeEnvironmentTest {

    @TempDir
    private Path tempDir;

    @Test
    void testInContainer_WithEnvironFileContainingContainer() throws IOException {
        Path procDir = tempDir.resolve("proc/1");
        Files.createDirectories(procDir);
        Path environFile = procDir.resolve("environ");
        Files.write(environFile, "container=docker\0other=value".getBytes());
        
        assertTrue(RuntimeEnvironment.inContainer(tempDir.toString()));
    }

    @Test
    void testInContainer_WithDockerenvFile() throws IOException {
        Files.createFile(tempDir.resolve(".dockerenv"));
        assertTrue(RuntimeEnvironment.inContainer(tempDir.toString()));
    }

    @Test
    void testInContainer_WithContainerenvFile() throws IOException {
        Path runDir = tempDir.resolve("run");
        Files.createDirectories(runDir);
        Files.createFile(runDir.resolve(".containerenv"));
        assertTrue(RuntimeEnvironment.inContainer(tempDir.toString()));
    }

    @Test
    void testInContainer_WithEnvironFileButNoContainerKey() throws IOException {
        Path procDir = tempDir.resolve("proc/1");
        Files.createDirectories(procDir);
        Path environFile = procDir.resolve("environ");
        Files.write(environFile, "key=value\0another=entry".getBytes());
        
        assertFalse(RuntimeEnvironment.inContainer(tempDir.toString()));
    }

    @Test
    void testInContainer_WithEnvironFileIOException() {
        Path invalidPath = tempDir.resolve("invalid");
        assertFalse(RuntimeEnvironment.inContainer(invalidPath.toString()));
    }

    @Test
    void testInContainer_NoFilesExist() {
        assertFalse(RuntimeEnvironment.inContainer(tempDir.toString()));
    }

    @Test
    void testInContainer_EnvironFileEmptyContainerValue() throws IOException {
        Path procDir = tempDir.resolve("proc/1");
        Files.createDirectories(procDir);
        Path environFile = procDir.resolve("environ");
        Files.write(environFile, "container=\0".getBytes());
        
        assertFalse(RuntimeEnvironment.inContainer(tempDir.toString()));
    }

    @Test
    void testInContainer_EmptyDirPrefix() {
        Boolean result = RuntimeEnvironment.inContainer();
        assertNotNull(result);
    }

    @Test
    void testConstructor() {
        new RuntimeEnvironment();
    }
}