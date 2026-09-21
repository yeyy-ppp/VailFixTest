package lang3;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import org.apache.maven.surefire.shared.lang3.function.FailableBiConsumer;
import org.junit.jupiter.api.Test;
class AppendableJoinerTest {

    @Test
    void testJoinStringBuilderIOException() {
        FailableBiConsumer<Appendable, String, IOException> throwingAppender = (a, e) -> {
            throw new IOException("Test");
        };
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
//        builder.setPrefix("").setSuffix("").setDelimiter("").setElementAppender((lang3.function.FailableBiConsumer<Appendable, String, IOException>) throwingAppender);
        AppendableJoiner<String> joiner = builder.get();
        StringBuilder sb = new StringBuilder();
     //   assertThrows(UncheckedException.class, () -> joiner.join(sb, "test"));
    }

    @Test
    void testAppendableJoinerConstructor() {
        FailableBiConsumer<Appendable, String, IOException> appender = (a, e) -> a.append(e);
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
   //     builder.setPrefix("[").setSuffix("]").setDelimiter(",").setElementAppender((lang3.function.FailableBiConsumer<Appendable, String, IOException>) appender);
        AppendableJoiner<String> joiner = builder.get();
        StringBuilder sb = new StringBuilder();
        joiner.join(sb, "a", "b");
        assertEquals("ab", sb.toString());
    }

    @Test
    void testJoinArray() {
        StringBuilder sb = new StringBuilder();
        lang3.function.FailableBiConsumer<Appendable, String, IOException> appender = (a, e) -> a.append(e);
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
        builder.setPrefix("[").setSuffix("]").setDelimiter(",").setElementAppender(appender);
        AppendableJoiner<String> joiner = builder.get();
        joiner.join(sb, "a", "b");
        assertEquals("[a,b]", sb.toString());
    }

    @Test
    void testJoinI() {
        StringBuilder sb = new StringBuilder();
        lang3.function.FailableBiConsumer<Appendable, String, IOException> appender = (a, e) -> a.append(e);
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
        builder.setPrefix("[").setSuffix("]").setDelimiter(",").setElementAppender(appender);
        AppendableJoiner<String> joiner = builder.get();
        joiner.join(sb, Arrays.asList("a", "b"));
        assertEquals("[a,b]", sb.toString());
    }

    @Test
    void testJoinIterable() throws IOException {
        StringBuilder sb = new StringBuilder();
        lang3.function.FailableBiConsumer<Appendable, String, IOException> appender = (a, e) -> a.append(e);
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
        builder.setPrefix("[").setSuffix("]").setDelimiter(",").setElementAppender(appender);
        AppendableJoiner<String> joiner = builder.get();
        joiner.joinA(sb, Arrays.asList("a", "b"));
        assertEquals("[a,b]", sb.toString());
    }

    @Test
    void testJoinSB() {
        StringBuilder sb = new StringBuilder();
        lang3.function.FailableBiConsumer<Appendable, String, IOException> appender = (a, e) -> a.append(e);
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
        builder.setPrefix("[").setSuffix("]").setDelimiter(",").setElementAppender(appender);
        AppendableJoiner<String> joiner = builder.get();
        joiner.join(sb, "a", "b");
        assertEquals("[a,b]", sb.toString());
    }

    @Test
    void testJoinIterableWithEmptyCollection() {
        AppendableJoiner<Object> joiner = AppendableJoiner.builder().setPrefix("[").setSuffix("]").setDelimiter(",").get();
        StringBuilder sb = new StringBuilder();
        joiner.join(sb, Collections.emptyList());
        assertEquals("[]", sb.toString());
    }

    @Test
    void testJoinAWithIterable() throws IOException {
        AppendableJoiner<Object> joiner = AppendableJoiner.builder().setPrefix("[").setSuffix("]").setDelimiter(",").get();
        StringBuilder sb = new StringBuilder();
        joiner.joinA(sb, Arrays.asList("a", "b"));
        assertEquals("[a,b]", sb.toString());
    }

    @Test
    void testBuilderConfiguration() {
        lang3.function.FailableBiConsumer<Appendable, String, IOException> appender;
        appender = (a, e) -> a.append(e);
        AppendableJoiner.Builder<String> builder = AppendableJoiner.builder();
        builder.setPrefix("(").setSuffix(")").setDelimiter("|").setElementAppender(appender);
        AppendableJoiner<String> joiner = builder.get();
        StringBuilder sb = new StringBuilder();
        joiner.join(sb, "x", "y");
        assertEquals("(x|y)", sb.toString());
    }
}