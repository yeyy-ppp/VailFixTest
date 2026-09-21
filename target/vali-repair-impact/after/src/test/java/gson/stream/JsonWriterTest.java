package gson.stream;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.io.StringWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import gson.FormattingStyle;
import gson.Strictness;

class JsonWriterTest {

    @Test
    void testConstructor() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        assertNotNull(writer);
    }

    @Test
    void testSetIndent() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setIndent("  ");
        assertTrue(sw.toString().isEmpty());
    }

    @Test
    void testSetFormattingStyle() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setFormattingStyle(FormattingStyle.COMPACT);
        assertEquals(FormattingStyle.COMPACT, writer.getFormattingStyle());
    }

    @Test
    void testSetHtmlSafe() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setHtmlSafe(true);
        assertTrue(writer.isHtmlSafe());
    }

    @Test
    void testSetSerializeNulls() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setSerializeNulls(true);
        assertTrue(writer.getSerializeNulls());
    }

    @Test
    void testBeginArray() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray();
        writer.endArray();
        assertEquals("[]", sw.toString());
    }

    @Test
    void testEndArray() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().endArray();
        assertEquals("[]", sw.toString());
    }

    @Test
    void testBeginObject() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject();
        writer.endObject();
        assertEquals("{}", sw.toString());
    }

    @Test
    void testEndObject() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject().endObject();
        assertEquals("{}", sw.toString());
    }

    @Test
    void testName() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject();
        writer.name("key");
        writer.value("value");
        writer.endObject();
        assertEquals("{\"key\":\"value\"}", sw.toString());
    }

    @Test
    void testValueString() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value("test").endArray();
        assertEquals("[\"test\"]", sw.toString());
    }

    @Test
    void testValueBoolean() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value(true).value(false).endArray();
        assertEquals("[true,false]", sw.toString());
    }

    @Test
    void testValueBooleanObj() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value(Boolean.TRUE).value(Boolean.FALSE).endArray();
        assertEquals("[true,false]", sw.toString());
    }

    @Test
    void testValueFloat() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value(1.5f).endArray();
        assertEquals("[1.5]", sw.toString());
    }

    @Test
    void testValueDouble() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value(2.5).endArray();
        assertEquals("[2.5]", sw.toString());
    }

    @Test
    void testValueLong() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value(100L).endArray();
        assertEquals("[100]", sw.toString());
    }

    @Test
    void testValueNumber() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().value(123).endArray();
        assertEquals("[123]", sw.toString());
    }

    @Test
    void testNullValue() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().nullValue().endArray();
        assertEquals("[null]", sw.toString());
    }

    @Test
    void testJsonValue() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().jsonValue("[1,2]").endArray();
        assertEquals("[[1,2]]", sw.toString());
    }

    @Test
    void testClose() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().endArray();
        writer.close();
        assertThrows(IllegalStateException.class, () -> writer.beginArray());
    }

    @Test
    void testAlwaysCreatesValidJsonNumber() throws Exception {
        Method method = JsonWriter.class.getDeclaredMethod("alwaysCreatesValidJsonNumber", Class.class);
        method.setAccessible(true);
        assertTrue((Boolean) method.invoke(null, Integer.class));
        assertFalse((Boolean) method.invoke(null, Double.class));
    }

    @Test
    void testString() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        Method method = JsonWriter.class.getDeclaredMethod("string", String.class);
        method.setAccessible(true);
        method.invoke(writer, "test");
        assertEquals("\"test\"", sw.toString());
    }

    @Test
    void testSetLenient() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setLenient(true);
        assertTrue(writer.isLenient());
    }

    @Test
    void testSetStrictness() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setStrictness(Strictness.LENIENT);
        assertEquals(Strictness.LENIENT, writer.getStrictness());
    }

    @Test
    void testFlush() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray().endArray();
        writer.flush();
        assertEquals("[]", sw.toString());
    }

    @Test
    void testPush() throws Exception {
        JsonWriter writer = new JsonWriter(new StringWriter());
        Method push = JsonWriter.class.getDeclaredMethod("push", int.class);
        push.setAccessible(true);
        push.invoke(writer, 1);

        Method peek = JsonWriter.class.getDeclaredMethod("peek");
        peek.setAccessible(true);
        assertEquals(1, peek.invoke(writer));
    }

    @Test
    void testPeek() throws Exception {
        JsonWriter writer = new JsonWriter(new StringWriter());
        Field stackField = JsonWriter.class.getDeclaredField("stack");
        stackField.setAccessible(true);
        int[] stack = (int[]) stackField.get(writer);
        stack[0] = 5;

        Method peek = JsonWriter.class.getDeclaredMethod("peek");
        peek.setAccessible(true);
        assertEquals(5, peek.invoke(writer));
    }

    @Test
    void testReplaceTop() throws Exception {
        JsonWriter writer = new JsonWriter(new StringWriter());
        Method push = JsonWriter.class.getDeclaredMethod("push", int.class);
        push.setAccessible(true);
        push.invoke(writer, 1);

        Method replaceTop = JsonWriter.class.getDeclaredMethod("replaceTop", int.class);
        replaceTop.setAccessible(true);
        replaceTop.invoke(writer, 2);

        Method peek = JsonWriter.class.getDeclaredMethod("peek");
        peek.setAccessible(true);
        assertEquals(2, peek.invoke(writer));
    }

    @Test
    void testOpenScope() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        Method openScope = JsonWriter.class.getDeclaredMethod("openScope", int.class, char.class);
        openScope.setAccessible(true);
        openScope.invoke(writer, 1, '[');
        assertEquals("[", sw.toString());
    }

    @Test
    void testCloseScope() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        Method openScope = JsonWriter.class.getDeclaredMethod("openScope", int.class, char.class);
        openScope.setAccessible(true);
        openScope.invoke(writer, 1, '[');

        Method closeScope = JsonWriter.class.getDeclaredMethod("closeScope", int.class, int.class, char.class);
        closeScope.setAccessible(true);
        closeScope.invoke(writer, 1, 2, ']');
        assertEquals("[]", sw.toString());
    }

    @Test
    void testNewline() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.setIndent("  ");
        writer.beginObject();

        Method newline = JsonWriter.class.getDeclaredMethod("newline");
        newline.setAccessible(true);
        newline.invoke(writer);

        writer.endObject();
        assertTrue(sw.toString().contains("\n"));
    }

    @Test
    void testBeforeName() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject();

        Method beforeName = JsonWriter.class.getDeclaredMethod("beforeName");
        beforeName.setAccessible(true);
        beforeName.invoke(writer);

        writer.name("key").value("value");
        writer.endObject();
        assertEquals("{\"key\":\"value\"}", sw.toString());
    }

    @Test
    void testBeforeValue() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray();

        Method beforeValue = JsonWriter.class.getDeclaredMethod("beforeValue");
        beforeValue.setAccessible(true);
        beforeValue.invoke(writer);

        writer.value("test");
        writer.endArray();
        assertEquals("[\"test\"]", sw.toString());
    }

    @Test
    void testNumericValues() throws IOException {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginArray()
            .value(1L)
            .value(2.5)
            .value(3.14)
            .endArray();
        assertEquals("[1,2.5,3.14]", sw.toString());
    }

    @Test
    void testWriteDeferredName() throws Exception {
        StringWriter sw = new StringWriter();
        JsonWriter writer = new JsonWriter(sw);
        writer.beginObject();
        writer.name("key");

        Method writeDeferredName = JsonWriter.class.getDeclaredMethod("writeDeferredName");
        writeDeferredName.setAccessible(true);
        writeDeferredName.invoke(writer);

        writer.value("value");
        writer.endObject();
        assertEquals("{\"key\":\"value\"}", sw.toString());
    }
}
