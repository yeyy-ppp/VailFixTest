package lang3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CharSetTest {

    @Test
    void testGetInstance_Null() {
        CharSet result = CharSet.getInstance((String[]) null);
        assertSame(CharSet.EMPTY, result);
    }

    @Test
    void testGetInstance_SingleEmpty() {
        CharSet result = CharSet.getInstance("");
        assertSame(CharSet.EMPTY, result);
    }

    @Test
    void testGetInstance_SingleCommon() {
        CharSet result = CharSet.getInstance("a-zA-Z");
        assertSame(CharSet.ASCII_ALPHA, result);
    }

    @Test
    void testGetInstance_SingleNew() {
        CharSet result = CharSet.getInstance("abc");
        assertNotSame(CharSet.EMPTY, result);
    }

    @Test
    void testGetInstance_Multiple() {
        CharSet result = CharSet.getInstance("a", "b");
        assertNotNull(result);
    }

/*    @Test
    void testConstructor_Null() {
        CharSet charSet = new CharSet((String[]) null);
        assertFalse(charSet.contains('a'));
    }*/

    @Test
    void testConstructor_Empty() {
        CharSet charSet = new CharSet("");
        assertFalse(charSet.contains('a'));
    }

    @Test
    void testConstructor_Single() {
        CharSet charSet = new CharSet("a");
        assertTrue(charSet.contains('a'));
    }

    @Test
    void testConstructor_Multiple() {
        CharSet charSet = new CharSet("a", "b");
        assertTrue(charSet.contains('a'));
        assertTrue(charSet.contains('b'));
    }

    @Test
    void testAdd_Null() throws Exception {
        CharSet charSet = new CharSet();
        Field setField = CharSet.class.getDeclaredField("set");
        setField.setAccessible(true);
        Set<?> originalSet = (Set<?>) setField.get(charSet);

        charSet.add(null);
        Set<?> newSet = (Set<?>) setField.get(charSet);
        assertEquals(originalSet.size(), newSet.size());
    }

    @Test
    void testAdd_SingleChar() {
        CharSet charSet = new CharSet();
        charSet.add("a");
        assertTrue(charSet.contains('a'));
        assertFalse(charSet.contains('b'));
    }

    @Test
    void testAdd_Range() {
        CharSet charSet = new CharSet();
        charSet.add("a-c");
        assertTrue(charSet.contains('a'));
        assertTrue(charSet.contains('b'));
        assertTrue(charSet.contains('c'));
        assertFalse(charSet.contains('d'));
    }

    @Test
    void testAdd_NegatedSingle() {
        CharSet charSet = new CharSet();
        charSet.add("^d");
        assertTrue(charSet.contains('a'));
        assertFalse(charSet.contains('d'));
    }

    @Test
    void testAdd_NegatedRange() {
        CharSet charSet = new CharSet();
        charSet.add("^d-f");
        assertTrue(charSet.contains('a'));
        assertFalse(charSet.contains('d'));
        assertFalse(charSet.contains('e'));
        assertFalse(charSet.contains('f'));
    }

    @Test
    void testAdd_ComplexPattern() {
        CharSet charSet = new CharSet();
        charSet.add("a^b-c^d-e");
        assertTrue(charSet.contains('a'));
        assertTrue(charSet.contains('b'));
        assertTrue(charSet.contains('c'));
        assertTrue(charSet.contains('d'));
        assertTrue(charSet.contains('e'));
    }

    @Test
    void testContains_Empty() {
        CharSet charSet = CharSet.EMPTY;
        assertFalse(charSet.contains('a'));
    }

    @ParameterizedTest
    @ValueSource(chars = {'a', 'z', 'A', 'Z'})
    void testContains_AlphaRange(char ch) {
        assertTrue(CharSet.ASCII_ALPHA.contains(ch));
    }

    @ParameterizedTest
    @ValueSource(chars = {'0', '5', '9'})
    void testContains_NumericRange(char ch) {
        assertTrue(CharSet.ASCII_NUMERIC.contains(ch));
    }

    @Test
    void testContains_OutsideRange() {
        assertFalse(CharSet.ASCII_ALPHA.contains('@'));
    }

    @Test
    void testEquals_SameInstance() {
        assertTrue(CharSet.ASCII_ALPHA.equals(CharSet.ASCII_ALPHA));
    }

    @Test
    void testEquals_EqualSet() {
        CharSet set1 = new CharSet("a");
        CharSet set2 = new CharSet("a");
        assertTrue(set1.equals(set2));
    }

    @Test
    void testEquals_DifferentSet() {
        CharSet set1 = new CharSet("a");
        CharSet set2 = new CharSet("b");
        assertFalse(set1.equals(set2));
    }

    @Test
    void testEquals_DifferentType() {
        assertFalse(CharSet.ASCII_ALPHA.equals("string"));
    }

    @Test
    void testEquals_Null() {
        assertFalse(CharSet.ASCII_ALPHA.equals(null));
    }

    @Test
    void testGetCharRanges_Empty() {
        CharSet charSet = CharSet.EMPTY;
        CharRange[] ranges = charSet.getCharRanges();
        assertEquals(0, ranges.length);
    }

    @Test
    void testGetCharRanges_Single() {
        CharSet charSet = new CharSet("a");
        CharRange[] ranges = charSet.getCharRanges();
        assertEquals(1, ranges.length);
    }

    @Test
    void testGetCharRanges_Multiple() {
        CharSet charSet = new CharSet("a", "b-c");
        CharRange[] ranges = charSet.getCharRanges();
        assertEquals(2, ranges.length);
    }

    @Test
    void testHashCode_SameSet() {
        CharSet set1 = new CharSet("a");
        CharSet set2 = new CharSet("a");
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    void testHashCode_DifferentSet() {
        CharSet set1 = new CharSet("a");
        CharSet set2 = new CharSet("b");
        assertNotEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    void testToString_Empty() {
        assertEquals("[]", CharSet.EMPTY.toString());
    }

    @Test
    void testToString_NonEmpty() {
        CharSet charSet = new CharSet("a");
        String result = charSet.toString();
        assertTrue(result.contains("a"));
    }
}