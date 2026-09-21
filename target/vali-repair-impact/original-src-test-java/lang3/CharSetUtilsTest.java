package lang3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class CharSetUtilsTest {

    @Test
    void testContainsAny() {
        assertFalse(CharSetUtils.containsAny(null, "a"));
        assertFalse(CharSetUtils.containsAny("", "a"));
        assertFalse(CharSetUtils.containsAny("hello"));
        assertFalse(CharSetUtils.containsAny("hello", "xyz"));
        assertTrue(CharSetUtils.containsAny("hello", "l"));
        assertTrue(CharSetUtils.containsAny("world", "d", "x"));
    }

     @Test
     void testCount() {
         assertEquals(0, CharSetUtils.count(null, "a"));
         assertEquals(0, CharSetUtils.count("", "a"));
        assertEquals(0, CharSetUtils.count("hello", "xyz"));
         assertEquals(2, CharSetUtils.count("hello", "l"));
        assertEquals(5, CharSetUtils.count("banana", "na"));
         assertEquals(3, CharSetUtils.count("apples", "a", "p"));
    }

    @Test
    void testDelete() {
        assertNull(CharSetUtils.delete(null, "a"));
        assertEquals("", CharSetUtils.delete("", "a"));
        assertEquals("hello", CharSetUtils.delete("hello", "xyz"));
        assertEquals("heo", CharSetUtils.delete("hello", "l"));
        assertEquals("bnn", CharSetUtils.delete("banana", "a"));
        assertEquals("le", CharSetUtils.delete("apple", "a", "p"));
    }

    @Test
    void testIsEmpty() {
        assertNull(CharSetUtils.keep(null, "a"));
        assertEquals("", CharSetUtils.keep("", "a"));
    }

    @Test
    void testKeep() {
        assertNull(CharSetUtils.keep(null, "a"));
        assertEquals("", CharSetUtils.keep("", "a"));
        assertEquals("", CharSetUtils.keep("hello", "xyz"));
        assertEquals("ll", CharSetUtils.keep("hello", "l"));
        assertEquals("aaa", CharSetUtils.keep("banana", "a"));
        assertEquals("pp", CharSetUtils.keep("apple", "p"));
    }

    @Test
    void testSqueeze() {
        assertNull(CharSetUtils.squeeze(null, "a"));
        assertEquals("", CharSetUtils.squeeze("", "a"));
        assertEquals("aabbccdd", CharSetUtils.squeeze("aabbccdd", "xy"));
        assertEquals("helo", CharSetUtils.squeeze("hello", "l"));
        assertEquals("banana", CharSetUtils.squeeze("baananaa", "a"));
        assertEquals("squeze", CharSetUtils.squeeze("squeeze", "e"));
        assertEquals("aabbccdd", CharSetUtils.squeeze("aabbccdd", "x"));
        assertEquals("abccc", CharSetUtils.squeeze("aaabbbccc", "ab"));
    }

    @Test
    void testConstructor() {
        new CharSetUtils();
    }
}
