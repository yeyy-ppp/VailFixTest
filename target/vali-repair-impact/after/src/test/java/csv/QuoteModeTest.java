package csv;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class QuoteModeTest {

    @Test
    void testValues() {
        QuoteMode[] values = QuoteMode.values();
        assertEquals(5, values.length);
        assertEquals(QuoteMode.ALL, values[0]);
        assertEquals(QuoteMode.ALL_NON_NULL, values[1]);
        assertEquals(QuoteMode.MINIMAL, values[2]);
        assertEquals(QuoteMode.NON_NUMERIC, values[3]);
        assertEquals(QuoteMode.NONE, values[4]);
    }

    @Test
    void testValueOfValidNames() {
        assertEquals(QuoteMode.ALL, QuoteMode.valueOf("ALL"));
        assertEquals(QuoteMode.ALL_NON_NULL, QuoteMode.valueOf("ALL_NON_NULL"));
        assertEquals(QuoteMode.MINIMAL, QuoteMode.valueOf("MINIMAL"));
        assertEquals(QuoteMode.NON_NUMERIC, QuoteMode.valueOf("NON_NUMERIC"));
        assertEquals(QuoteMode.NONE, QuoteMode.valueOf("NONE"));
    }

    @Test
    void testValueOfInvalidName() {
        assertThrows(IllegalArgumentException.class, () -> {
            QuoteMode.valueOf("INVALID_ENUM_NAME");
        });
    }

    @Test
    void testValueOfNullName() {
        assertThrows(NullPointerException.class, () -> {
            QuoteMode.valueOf(null);
        });
    }

    @Test
    void testValueOfCaseSensitivity() {
        assertThrows(IllegalArgumentException.class, () -> {
            QuoteMode.valueOf("all");
        });
    }
}