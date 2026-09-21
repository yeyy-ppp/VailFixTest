package cli.help;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Collection;

class HelpAppendableTest {
    private TestableHelpAppendable helpAppendable;

    @BeforeEach
    void setUp() {
        helpAppendable = new TestableHelpAppendable();
    }

    @Test
    void testAppendFormat() throws IOException {
        helpAppendable.appendFormat("Test %s", "value");
        assertEquals("Test value", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendFormatWithEmpty() throws IOException {
        helpAppendable.appendFormat("");
        assertEquals("", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendHeader() throws IOException {
        helpAppendable.appendHeader(1, "Header1");
        assertEquals("HEADER:1(Header1)", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendHeaderWithEmptyText() throws IOException {
        helpAppendable.appendHeader(2, "");
        assertEquals("HEADER:2()", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendListOrdered() throws IOException {
        Collection<CharSequence> items = Arrays.asList("Item1", "Item2");
        helpAppendable.appendList(true, items);
        assertEquals("ORDERED_LIST[Item1, Item2]", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendListUnordered() throws IOException {
        Collection<CharSequence> items = Arrays.asList("Point1", "Point2");
        helpAppendable.appendList(false, items);
        assertEquals("UNORDERED_LIST[Point1, Point2]", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendListEmpty() throws IOException {
        Collection<CharSequence> items = Arrays.asList();
        helpAppendable.appendList(true, items);
        assertEquals("ORDERED_LIST[]", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendParagraph() throws IOException {
        helpAppendable.appendParagraph("Sample paragraph");
        assertEquals("PARAGRAPH(Sample paragraph)", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendParagraphFormat() throws IOException {
        helpAppendable.appendParagraphFormat("Formatted %s", "text");
        assertEquals("PARAGRAPH(Formatted text)", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendTable() throws IOException {
        TableDefinition table = TableDefinition.from(
            null,
            null,
            Arrays.asList("H1", "H2"),
            Collections.singletonList(Arrays.asList("R1C1", "R1C2"))
        );
        helpAppendable.appendTable(table);
        assertEquals("TABLE([H1, H2], [[R1C1, R1C2]])", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendTableEmpty() throws IOException {
        TableDefinition table = TableDefinition.from(
            null,
            null,
            Collections.emptyList(),
            Collections.emptyList()
        );
        helpAppendable.appendTable(table);
        assertEquals("TABLE([], [])", helpAppendable.getContent().toString());
    }

    @Test
    void testAppendTitle() throws IOException {
        helpAppendable.appendTitle("Document Title");
        assertEquals("TITLE(Document Title)", helpAppendable.getContent().toString());
    }

    private static class TestableHelpAppendable implements HelpAppendable {
        private final StringBuilder content = new StringBuilder();

        @Override
        public HelpAppendable append(CharSequence csq) throws IOException {
            content.append(csq);
            return this;
        }

        @Override
        public HelpAppendable append(CharSequence csq, int start, int end) {
            content.append(csq, start, end);
            return this;
        }

        @Override
        public HelpAppendable append(char c) {
            content.append(c);
            return this;
        }

        @Override
        public void appendHeader(int level, CharSequence text) throws IOException {
            content.append("HEADER:").append(level).append("(").append(text).append(")");
        }

        @Override
        public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {
            content.append(ordered ? "ORDERED_LIST" : "UNORDERED_LIST")
                   .append(list.toString());
        }

        @Override
        public void appendParagraph(CharSequence paragraph) throws IOException {
            content.append("PARAGRAPH(").append(paragraph).append(")");
        }

        @Override
        public void appendTable(TableDefinition table) throws IOException {
            content.append("TABLE(")
                   .append(table.headers()).append(", ")
                   .append(table.rows())
                   .append(")");
        }

        @Override
        public void appendTitle(CharSequence title) throws IOException {
            content.append("TITLE(").append(title).append(")");
        }

        public StringBuilder getContent() {
            return content;
        }
    }
}