package cli.help;

import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

class FilterHelpAppendableTest {

    @Test
    void constructorInitializesOutput() {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        assertSame(output, appendable.output);
    }

    @Test
    void appendChar() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append('a');
        assertEquals("a", output.toString());
    }

    @Test
    void appendNullCharSequence() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append(null);
        assertEquals("null", output.toString());
    }

    @Test
    void appendEmptyCharSequence() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append("");
        assertEquals("", output.toString());
    }

    @Test
    void appendNonEmptyCharSequence() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append("test");
        assertEquals("test", output.toString());
    }

    @Test
    void appendSubsequenceFullRange() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append("hello", 0, 5);
        assertEquals("hello", output.toString());
    }

    @Test
    void appendSubsequencePartialRange() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append("world", 1, 4);
        assertEquals("orl", output.toString());
    }

    @Test
    void appendSubsequenceEmptyRange() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append("test", 2, 2);
        assertEquals("", output.toString());
    }

    @Test
    void appendNullSubsequence() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append(null, 0, 4);
        assertEquals("null", output.toString());
    }

    @Test
    void appendNullSubsequencePartial() throws IOException {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        appendable.append(null, 1, 3);
        assertEquals("ul", output.toString());
    }

    @Test
    void appendSubsequenceNegativeStartThrows() {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        assertThrows(IndexOutOfBoundsException.class, () -> 
            appendable.append("test", -1, 2));
    }

    @Test
    void appendSubsequenceStartAfterEndThrows() {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        assertThrows(IndexOutOfBoundsException.class, () -> 
            appendable.append("test", 3, 2));
    }

    @Test
    void appendSubsequenceEndExceedsLengthThrows() {
        StringBuilder output = new StringBuilder();
        FilterHelpAppendable appendable = new FilterHelpAppendable(output) {
            @Override
            public void appendHeader(int level, CharSequence text) throws IOException {}
            @Override
            public void appendList(boolean ordered, Collection<CharSequence> list) throws IOException {}
            @Override
            public void appendParagraph(CharSequence paragraph) throws IOException {}
            @Override
            public void appendTable(TableDefinition table) throws IOException {}
            @Override
            public void appendTitle(CharSequence title) throws IOException {}
        };
        assertThrows(IndexOutOfBoundsException.class, () -> 
            appendable.append("test", 1, 5));
    }
}