package gson;

import gson.stream.JsonReader;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.io.StringReader;
import static org.junit.jupiter.api.Assertions.*;

class ToNumberStrategyTest {

    @Test
    void testReadNumber_validInteger() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextLong();
            }
        };
        JsonReader reader = new JsonReader(new StringReader("12345"));
        Number result = strategy.readNumber(reader);
        assertEquals(12345L, result);
    }

    @Test
    void testReadNumber_validDouble() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextDouble();
            }
        };
        JsonReader reader = new JsonReader(new StringReader("123.45"));
        Number result = strategy.readNumber(reader);
        assertEquals(123.45, result.doubleValue(), 0.001);
    }

    @Test
    void testReadNumber_boundaryMinInteger() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextLong();
            }
        };
        JsonReader reader = new JsonReader(new StringReader(Long.toString(Long.MIN_VALUE)));
        Number result = strategy.readNumber(reader);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    void testReadNumber_boundaryMaxInteger() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextLong();
            }
        };
        JsonReader reader = new JsonReader(new StringReader(Long.toString(Long.MAX_VALUE)));
        Number result = strategy.readNumber(reader);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    void testReadNumber_boundaryMinDouble() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextDouble();
            }
        };
        JsonReader reader = new JsonReader(new StringReader(Double.toString(Double.MIN_VALUE)));
        Number result = strategy.readNumber(reader);
        assertEquals(Double.MIN_VALUE, result.doubleValue(), 0.0);
    }

    @Test
    void testReadNumber_boundaryMaxDouble() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextDouble();
            }
        };
        JsonReader reader = new JsonReader(new StringReader(Double.toString(Double.MAX_VALUE)));
        Number result = strategy.readNumber(reader);
        assertEquals(Double.MAX_VALUE, result.doubleValue(), 0.0);
    }

    @Test
    void testReadNumber_invalidNumberFormat() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextDouble();
            }
        };
        JsonReader reader = new JsonReader(new StringReader("invalid"));
        assertThrows(IOException.class, () -> strategy.readNumber(reader));
    }

    @Test
    void testReadNumber_emptyNumber() throws IOException {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextDouble();
            }
        };
        JsonReader reader = new JsonReader(new StringReader(""));
        assertThrows(IOException.class, () -> strategy.readNumber(reader));
    }

    @Test
    void testReadNumber_nullReader() {
        ToNumberStrategy strategy = new ToNumberStrategy() {
            @Override
            public Number readNumber(JsonReader in) throws IOException {
                return in.nextDouble();
            }
        };
        assertThrows(NullPointerException.class, () -> strategy.readNumber(null));
    }
}