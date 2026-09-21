package lang3.stream;

import java.util.stream.Stream;
import java.util.function.Function;
import java.util.stream.Collector;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class LangCollectorsTest {

    @Test
    void testLangCollectorsConstructor() throws Exception {
        Constructor<LangCollectors> constructor = LangCollectors.class.getDeclaredConstructor();
        Assertions.assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    void testJoining() {
        Collector<Object, ?, String> collector = LangCollectors.joining();
        Assertions.assertEquals("", Stream.empty().collect(collector));
        Assertions.assertEquals("a", Stream.of("a").collect(collector));
        Assertions.assertEquals("ab", Stream.of("a", "b").collect(collector));
        Assertions.assertEquals("123", Stream.of(1, 2, 3).collect(collector));
    }

    @Test
    void testJoiningWithDelimiter() {
        Collector<Object, ?, String> collector = LangCollectors.joining(",");
        Assertions.assertEquals("", Stream.empty().collect(collector));
        Assertions.assertEquals("a", Stream.of("a").collect(collector));
        Assertions.assertEquals("a,b", Stream.of("a", "b").collect(collector));
        Assertions.assertEquals("1,2,3", Stream.of(1, 2, 3).collect(collector));
    }

    @Test
    void testJoiningWithDelimiterPrefixSuffix() {
        Collector<Object, ?, String> collector = LangCollectors.joining(",", "[", "]");
        Assertions.assertEquals("[]", Stream.empty().collect(collector));
        Assertions.assertEquals("[a]", Stream.of("a").collect(collector));
        Assertions.assertEquals("[a,b]", Stream.of("a", "b").collect(collector));
        Assertions.assertEquals("[1,2,3]", Stream.of(1, 2, 3).collect(collector));
    }

    @Test
    void testJoiningWithDelimiterPrefixSuffixAndFunction() {
        Function<Object, String> toUpperCase = obj -> obj.toString().toUpperCase();
        Collector<Object, ?, String> collector = LangCollectors.joining(",", "[", "]", toUpperCase);
        Assertions.assertEquals("[]", Stream.empty().collect(collector));
        Assertions.assertEquals("[A]", Stream.of("a").collect(collector));
        Assertions.assertEquals("[A,B]", Stream.of("a", "b").collect(collector));
        Assertions.assertEquals("[1,2,3]", Stream.of(1, 2, 3).collect(collector));
    }

    @Test
    void testJoiningWithNullElements() {
        Collector<Object, ?, String> collector1 = LangCollectors.joining();
        Assertions.assertEquals("null", Stream.of((Object)null).collect(collector1));

        Collector<Object, ?, String> collector2 = LangCollectors.joining(",");
        Assertions.assertEquals("null", Stream.of((Object)null).collect(collector2));
        Assertions.assertEquals("a,null,c", Stream.of("a", null, "c").collect(collector2));

        Collector<Object, ?, String> collector3 = LangCollectors.joining(",", "[", "]");
        Assertions.assertEquals("[a,null,c]", Stream.of("a", null, "c").collect(collector3));

        Function<Object, String> toString = obj -> obj == null ? "NULL" : obj.toString();
        Collector<Object, ?, String> collector4 = LangCollectors.joining(",", "[", "]", toString);
        Assertions.assertEquals("[a,NULL,c]", Stream.of("a", null, "c").collect(collector4));
    }

    @Test
    void testJoiningWithEmptyDelimiter() {
        Collector<Object, ?, String> collector = LangCollectors.joining("");
        Assertions.assertEquals("", Stream.empty().collect(collector));
        Assertions.assertEquals("a", Stream.of("a").collect(collector));
        Assertions.assertEquals("ab", Stream.of("a", "b").collect(collector));
    }

    @Test
    void testJoiningWithLongPrefixSuffix() {
        Collector<Object, ?, String> collector = LangCollectors.joining(",", "PREFIX-", "-SUFFIX");
        Assertions.assertEquals("PREFIX--SUFFIX", Stream.empty().collect(collector));
        Assertions.assertEquals("PREFIX-a-SUFFIX", Stream.of("a").collect(collector));
        Assertions.assertEquals("PREFIX-a,b-SUFFIX", Stream.of("a", "b").collect(collector));
    }

    @Test
    void testJoiningWithCustomFunction() {
        Function<Object, String> stringLength = obj -> String.valueOf(obj.toString().length());
        Collector<Object, ?, String> collector = LangCollectors.joining(",", "[", "]", stringLength);
        Assertions.assertEquals("[]", Stream.empty().collect(collector));
        Assertions.assertEquals("[1]", Stream.of("a").collect(collector));
        Assertions.assertEquals("[1,3]", Stream.of("a", "abc").collect(collector));
    }

}