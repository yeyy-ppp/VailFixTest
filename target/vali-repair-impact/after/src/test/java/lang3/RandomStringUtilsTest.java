package lang3;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RandomStringUtilsTest {

    @Test
    void testInsecure_ReturnsNonNull() {
        RandomStringUtils utils = RandomStringUtils.insecure();
        assertNotNull(utils);
    }

    @Test
    void testSecure_ReturnsNonNull() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertNotNull(utils);
    }

    @Test
    void testSecureStrong_ReturnsNonNull() {
        RandomStringUtils utils = RandomStringUtils.secureStrong();
        assertNotNull(utils);
    }

    @Test
    void testNext_CountZero_ReturnsEmptyString() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertEquals("", utils.next(0));
    }

    @Test
    void testNext_NegativeCount_ThrowsException() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> utils.next(-1));
    }

    @Test
    void testNext_ValidCount_ReturnsCorrectLength() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertEquals(5, utils.next(5).length());
    }

    @Test
    void testNext_WithLettersOnly_ReturnsAlphabetic() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.next(10, true, false);
        assertTrue(result.chars().allMatch(Character::isLetter));
    }

    @Test
    void testNext_WithNumbersOnly_ReturnsNumeric() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.next(10, false, true);
        assertTrue(result.chars().allMatch(Character::isDigit));
    }

    @Test
    void testNext_WithCharsArray_ReturnsCharsFromArray() {
        RandomStringUtils utils = RandomStringUtils.secure();
        char[] chars = {'a', 'b', 'c'};
        String result = utils.next(10, chars);
        assertTrue(result.chars().allMatch(c -> c == 'a' || c == 'b' || c == 'c'));
    }

    @Test
    void testNext_EmptyCharsArray_ThrowsException() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> utils.next(5, new char[0]));
    }

    @Test
    void testNextAlphabetic_ValidCount_ReturnsLetters() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.nextAlphabetic(10);
        assertTrue(result.chars().allMatch(Character::isLetter));
    }

    @Test
    void testNextNumeric_ValidCount_ReturnsDigits() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.nextNumeric(10);
        assertTrue(result.chars().allMatch(Character::isDigit));
    }

    @Test
    void testNextAlphanumeric_ValidCount_ReturnsAlnum() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.nextAlphanumeric(10);
        assertTrue(result.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void testNextAscii_ValidCount_ReturnsAsciiChars() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.nextAscii(10);
        assertTrue(result.chars().allMatch(c -> c >= 32 && c <= 126));
    }

    @Test
    void testNextPrint_ValidCount_ReturnsPrintable() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.nextPrint(10);
        assertTrue(result.chars().allMatch(c -> c >= 32 && c <= 126));
    }

    @Test
    void testNextGraph_ValidCount_ReturnsGraphChars() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.nextGraph(10);
        assertTrue(result.chars().allMatch(c -> c >= 33 && c <= 126));
    }

    @Test
    void testNext_CharRangeEndLessThanStart_ThrowsException() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> utils.next(5, 100, 50, false, false));
    }

    @Test
    void testNext_CharRangeNegativeValues_ThrowsException() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> utils.next(5, -1, 10, false, false));
    }

    @Test
    void testNext_RangeWithLettersAndNoLetters_ThrowsException() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> 
            utils.next(5, 10, 20, true, false, null));
    }

    @Test
    void testNext_RangeWithDigitsAndNoDigits_ThrowsException() {
        RandomStringUtils utils = RandomStringUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> 
            utils.next(5, 10, 20, false, true, null));
    }

    @Test
    void testToString_ReturnsValidRepresentation() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String str = utils.toString();
        assertTrue(str.contains("RandomStringUtils"));
        assertTrue(str.contains("random="));
    }

    @Test
    void testRandom_ZeroCount_ReturnsEmpty() {
        String result = RandomStringUtils.random(0, 0, 0, false, false, null, new Random());
        assertEquals("", result);
    }

    @Test
    void testRandom_NegativeCount_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            RandomStringUtils.random(-1, 0, 0, false, false, null, new Random()));
    }

    @Test
    void testRandom_CharsArrayEmpty_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> 
            RandomStringUtils.random(5, 0, 0, false, false, new char[0], new Random()));
    }

   /* @Test
    void testRandom_EndExceedsMaxCodePoint_AdjustsEndValue(@Mock Random random) {
        when(random.nextInt(Character.MAX_CODE_POINT)).thenReturn(100);
        String result = RandomStringUtils.random(1, 0, Character.MAX_CODE_POINT + 10, 
                                               false, false, null, random);
        assertEquals(1, result.length());
    }*/

    @Test
    void testRandom_CharRangeStartEndZero_SetsDefaultRange() {
        String result = RandomStringUtils.random(1, 0, 0, false, false, null, new Random());
        assertFalse(result.isEmpty());
    }

    /*@Test
    void testRandom_AlphanumericalCacheUsed(@Mock Random random) {
        when(random.nextInt(62)).thenReturn(0);
        String result = RandomStringUtils.random(5, 0, 0, true, true, null, random);
        assertEquals("aaaaa", result);
    }*/

    /*@Test
    void testRandom_UnicodeSurrogate_SkipsAndRetries(@Mock Random random) {
        when(random.nextInt(0xD800 - 32)).thenReturn(0xD7FF - 32);
        String result = RandomStringUtils.random(1, 32, 0xD800, false, false, null, random);
        assertFalse(result.isEmpty());
    }*/

/*    @Test
    void testRandom_RequiresTwoChars_HandlesCountAdjustment(@Mock Random random) {
        when(random.nextInt(0x10FFFF - 0x10000 + 1)).thenReturn(0x10000);
        String result = RandomStringUtils.random(1, 0x10000, 0x10001, false, false, null, random);
        assertEquals("\uD800\uDC00", result);
    }*/

    @Test
    void testNext_WithString_ReturnsFromChars() {
        RandomStringUtils utils = RandomStringUtils.secure();
        String result = utils.next(5, "abc");
        assertTrue(result.chars().allMatch(c -> "abc".indexOf(c) != -1));
    }

    @Test
    void testRandomAlphabetic_IntCount() {
        String result = RandomStringUtils.randomAlphabetic(5);
        assertEquals(5, result.length());
        assertTrue(result.chars().allMatch(Character::isLetter));
    }

    @Test
    void testRandomAlphabetic_IntRange() {
        String result = RandomStringUtils.randomAlphabetic(3, 8);
        assertTrue(result.length() >= 3 && result.length() < 8);
        assertTrue(result.chars().allMatch(Character::isLetter));
    }

    @Test
    void testRandomAlphanumeric_IntCount() {
        String result = RandomStringUtils.randomAlphanumeric(5);
        assertEquals(5, result.length());
        assertTrue(result.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void testRandomAlphanumeric_IntRange() {
        String result = RandomStringUtils.randomAlphanumeric(3, 8);
        assertTrue(result.length() >= 3 && result.length() < 8);
        assertTrue(result.chars().allMatch(Character::isLetterOrDigit));
    }

    @Test
    void testRandomAscii_IntCount() {
        String result = RandomStringUtils.randomAscii(5);
        assertEquals(5, result.length());
        assertTrue(result.chars().allMatch(c -> c >= 32 && c <= 126));
    }

    @Test
    void testRandomAscii_IntRange() {
        String result = RandomStringUtils.randomAscii(3, 8);
        assertTrue(result.length() >= 3 && result.length() < 8);
        assertTrue(result.chars().allMatch(c -> c >= 32 && c <= 126));
    }

    @Test
    void testRandomGraph_IntCount() {
        String result = RandomStringUtils.randomGraph(5);
        assertEquals(5, result.length());
        assertTrue(result.chars().allMatch(c -> c >= 33 && c <= 126));
    }

    @Test
    void testRandomGraph_IntRange() {
        String result = RandomStringUtils.randomGraph(3, 8);
        assertTrue(result.length() >= 3 && result.length() < 8);
        assertTrue(result.chars().allMatch(c -> c >= 33 && c <= 126));
    }

    @Test
    void testRandomNumeric_IntCount() {
        String result = RandomStringUtils.randomNumeric(5);
        assertEquals(5, result.length());
        assertTrue(result.chars().allMatch(Character::isDigit));
    }

    @Test
    void testRandomNumeric_IntRange() {
        String result = RandomStringUtils.randomNumeric(3, 8);
        assertTrue(result.length() >= 3 && result.length() < 8);
        assertTrue(result.chars().allMatch(Character::isDigit));
    }

    @Test
    void testRandomPrint_IntCount() {
        String result = RandomStringUtils.randomPrint(5);
        assertEquals(5, result.length());
        assertTrue(result.chars().allMatch(c -> c >= 32 && c <= 126));
    }

    @Test
    void testRandomPrint_IntRange() {
        String result = RandomStringUtils.randomPrint(3, 8);
        assertTrue(result.length() >= 3 && result.length() < 8);
        assertTrue(result.chars().allMatch(c -> c >= 32 && c <= 126));
    }

    @Test
    void testRandomStringUtils_Constructor() {
        RandomStringUtils utils = new RandomStringUtils();
        assertNotNull(utils);
    }
}