package cli.help;

import java.io.IOException;
import java.io.StringWriter;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

class TextHelpAppendableTest {
    private TextHelpAppendable appendable;
    private StringWriter outputWriter;

    @BeforeEach
    void setup() {
        outputWriter = new StringWriter();
        appendable = new TextHelpAppendable(outputWriter);
    }

    @Test
    void testIndexOfWrap_WidthLessThan1() {
        assertThrows(IllegalArgumentException.class, () -> {
            TextHelpAppendable.indexOfWrap("text", 0, 0);
        });
    }

    @Test
    void testIndexOfWrap_FindsBreakChar() {
        CharSequence text = "a\nb";
        int result = TextHelpAppendable.indexOfWrap(text, 10, 0);
        assertEquals(1, result);
    }

    @Test
    void testIndexOfWrap_ExceedsLength() {
        CharSequence text = "abc";
        int result = TextHelpAppendable.indexOfWrap(text, 5, 0);
        assertEquals(3, result);
    }

    @Test
    void testIndexOfWrap_FindsWhitespace() {
        CharSequence text = "abc def";
        int result = TextHelpAppendable.indexOfWrap(text, 4, 0);
        assertEquals(3, result);
    }

    @Test
    void testIndexOfWrap_NoBreakOrSpace() {
        CharSequence text = "abcdef";
        int result = TextHelpAppendable.indexOfWrap(text, 3, 0);
        assertEquals(2, result);
    }

    @Test
    void testSystemOut() {
        TextHelpAppendable instance = TextHelpAppendable.systemOut();
        assertNotNull(instance);
    }

    @Test
    void testConstructor() {
        assertNotNull(appendable);
    }

   /* @Test
    void testAdjustTableFormat_ColumnWidths() {
        List<TextStyle> styles = Arrays.asList(
            TextStyle.builder().setMaxWidth(10).build(),
            TextStyle.builder().setMaxWidth(TextStyle.UNSET_MAX_WIDTH).build()
        );
        List<String> headers = Arrays.asList("header1", "longheader");
        List<List<String>> rows = Arrays.asList(
            Arrays.asList("cell", "longcellcontent")
        );
        TableDefinition table = TableDefinition.from("caption", styles, headers, rows);

        TableDefinition result = appendable.adjustTableFormat(table);
        assertEquals(8, result.columnTextStyles().get(0).getMaxWidth());
        assertEquals(13, result.columnTextStyles().get(1).getMaxWidth());
    }*/

   /* @Test
    void testAdjustTableFormat_ScalableColumns() {
        List<TextStyle> styles = Arrays.asList(
            TextStyle.builder().setMaxWidth(20).setScalable(true).build(),
            TextStyle.builder().setMaxWidth(20).setScalable(true).build()
        );
        List<String> headers = Arrays.asList("h1", "h2");
        List<List<String>> rows = Arrays.asList(
            Arrays.asList("cell1", "cell2")
        );
        TableDefinition table = TableDefinition.from("caption", styles, headers, rows);
        appendable.setMaxWidth(30);

        TableDefinition result = appendable.adjustTableFormat(table);
        assertTrue(result.columnTextStyles().get(0).getMaxWidth() < 20);
    }*/

    @Test
    void testAppendHeader_Level1() throws IOException {
        appendable.appendHeader(1, "Title");
        String output = outputWriter.toString();
        assertTrue(output.contains("Title"));
        assertTrue(output.contains("====="));
    }

    @Test
    void testAppendHeader_InvalidLevel() {
        assertThrows(IllegalArgumentException.class, () -> {
            appendable.appendHeader(0, "Title");
        });
    }

    @Test
    void testAppendList_Ordered() throws IOException {
        List<CharSequence> list = Arrays.asList("Item1", "Item2");
        appendable.appendList(true, list);
        String output = outputWriter.toString();
        assertTrue(output.contains("1. Item1"));
        assertTrue(output.contains("2. Item2"));
    }

    @Test
    void testAppendList_Unordered() throws IOException {
        List<CharSequence> list = Arrays.asList("Item1", "Item2");
        appendable.appendList(false, list);
        String output = outputWriter.toString();
        assertTrue(output.contains("* Item1"));
        assertTrue(output.contains("* Item2"));
    }

    @Test
    void testAppendParagraph() throws IOException {
        appendable.appendParagraph("Test paragraph");
        String output = outputWriter.toString();
        assertTrue(output.contains("Test paragraph"));
    }

    /*@Test
    void testAppendTable() throws IOException {
        List<TextStyle> styles = Arrays.asList(
            TextStyle.builder().setMaxWidth(10).build(),
            TextStyle.builder().setMaxWidth(10).build()
        );
        List<String> headers = Arrays.asList("H1", "H2");
        List<List<String>> rows = Arrays.asList(
            Arrays.asList("R1C1", "R1C2")
        );
        TableDefinition table = TableDefinition.from("Caption", styles, headers, rows);
        
        appendable.appendTable(table);
        String output = outputWriter.toString();
        assertTrue(output.contains("H1"));
        assertTrue(output.contains("R1C1"));
    }*/

    @Test
    void testAppendTitle() throws IOException {
        appendable.appendTitle("Document Title");
        String output = outputWriter.toString();
        assertTrue(output.contains("Document Title"));
        assertTrue(output.contains("#############"));
    }

    @Test
    void testGetSetIndent() {
        appendable.setIndent(5);
        assertEquals(5, appendable.getIndent());
    }

    @Test
    void testGetSetLeftPad() {
        appendable.setLeftPad(2);
        assertEquals(2, appendable.getLeftPad());
    }

    @Test
    void testGetSetMaxWidth() {
        appendable.setMaxWidth(80);
        assertEquals(80, appendable.getMaxWidth());
    }

    @Test
    void testMakeColumnQueue_SingleLine() throws IOException {
        Queue<String> queue = appendable.makeColumnQueue("Short text", TextStyle.DEFAULT);
        assertEquals(1, queue.size());
    }

    @Test
    void testMakeColumnQueue_MultiLine() throws IOException {
        String longText = "This is a very long text that should wrap multiple times when printed";
        appendable.setMaxWidth(20);
        Queue<String> queue = appendable.makeColumnQueue(longText, appendable.getTextStyleBuilder().get());
        assertTrue(queue.size() > 1);
    }

    @Test
    void testMakeColumnQueues() throws IOException {
        List<String> data = Arrays.asList("Col1", "Col2");
        List<TextStyle> styles = Arrays.asList(TextStyle.DEFAULT, TextStyle.DEFAULT);
        List<Queue<String>> queues = appendable.makeColumnQueues(data, styles);
        assertEquals(2, queues.size());
    }

    @Test
    void testPrintWrapped_DefaultStyle() throws IOException {
        appendable.printWrapped("Wrapped text");
        String output = outputWriter.toString();
        assertTrue(output.contains("Wrapped text"));
    }

 /*   @Test
    void testPrintWrapped_CustomStyle() throws IOException {
        TextStyle style = TextStyle.builder().setLeftPad(3).build();
        appendable.printWrapped("Wrapped text", style);
        String output = outputWriter.toString();
        assertTrue(output.contains("   Wrapped text"));
    }*/

    @Test
    void testWriteColumnQueues() throws IOException {
        Queue<String> q1 = new LinkedList<>(Arrays.asList("Line1", "Line2"));
        Queue<String> q2 = new LinkedList<>(Collections.singletonList("Col2"));
        List<Queue<String>> queues = Arrays.asList(q1, q2);
        List<TextStyle> styles = Arrays.asList(TextStyle.DEFAULT, TextStyle.DEFAULT);
        
        appendable.writeColumnQueues(queues, styles);
        String output = outputWriter.toString();
        assertTrue(output.contains("Line1"));
        assertTrue(output.contains("Line2"));
        assertTrue(output.contains("Col2"));
    }
}