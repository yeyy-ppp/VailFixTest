package lang3;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.util.Random;
import java.security.SecureRandom;
import java.util.Arrays;
import lang3.exception.UncheckedException;

class RandomUtilsTest {

    @Test
    void testInsecure() {
        RandomUtils instance1 = RandomUtils.insecure();
        RandomUtils instance2 = RandomUtils.insecure();
        assertNotNull(instance1);
        assertSame(instance1, instance2);
        assertTrue(instance1.random() instanceof Random);
    }

    @Test
    void testSecure() {
        RandomUtils instance1 = RandomUtils.secure();
        RandomUtils instance2 = RandomUtils.secure();
        assertNotNull(instance1);
        assertSame(instance1, instance2);
        assertTrue(instance1.random() instanceof SecureRandom);
    }

    @Test
    void testSecureStrong() {
        RandomUtils instance1 = RandomUtils.secureStrong();
        RandomUtils instance2 = RandomUtils.secureStrong();
        assertNotNull(instance1);
        assertSame(instance1, instance2);
        assertTrue(instance1.random() instanceof SecureRandom);
    }

    @Test
    void testNextBoolean() {
        boolean result = RandomUtils.nextBoolean();
        assertTrue(result || !result);
    }

    @Test
    void testNextBytes() {
        byte[] bytes = RandomUtils.nextBytes(5);
        assertEquals(5, bytes.length);
    }

    @Test
    void testNextDouble() {
        double result = RandomUtils.nextDouble();
        assertNotNull(Double.valueOf(result));
    }

    @Test
    void testNextDoubleRange() {
        double result = RandomUtils.nextDouble(1.5, 3.5);
        assertTrue(result >= 1.5 && result < 3.5);
    }

    @Test
    void testNextFloat() {
        float result = RandomUtils.nextFloat();
        assertNotNull(Float.valueOf(result));
    }

    @Test
    void testNextFloatRange() {
        float result = RandomUtils.nextFloat(1.5f, 3.5f);
        assertTrue(result >= 1.5f && result < 3.5f);
    }

    @Test
    void testNextInt() {
        int result = RandomUtils.nextInt();
        assertNotNull(Integer.valueOf(result));
    }

    @Test
    void testNextIntRange() {
        int result = RandomUtils.nextInt(10, 20);
        assertTrue(result >= 10 && result < 20);
    }

    @Test
    void testNextLong() {
        long result = RandomUtils.nextLong();
        assertNotNull(Long.valueOf(result));
    }

    @Test
    void testNextLongRange() {
        long result = RandomUtils.nextLong(10L, 20L);
        assertTrue(result >= 10 && result < 20);
    }

    @Test
    void testRandomBoolean() {
        RandomUtils instance = RandomUtils.secure();
        boolean result = instance.randomBoolean();
        assertTrue(result || !result);
    }

    @Test
    void testRandomBytesZero() {
        RandomUtils instance = RandomUtils.secure();
        byte[] bytes = instance.randomBytes(0);
        assertEquals(0, bytes.length);
    }

    @Test
    void testRandomBytesPositive() {
        RandomUtils instance = RandomUtils.secure();
        byte[] bytes = instance.randomBytes(3);
        assertEquals(3, bytes.length);
    }

    @Test
    void testRandomBytesNegative() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomBytes(-1));
    }

    @Test
    void testRandomDouble() {
        RandomUtils instance = RandomUtils.secure();
        double result = instance.randomDouble();
        assertNotNull(Double.valueOf(result));
    }

    @Test
    void testRandomDoubleRangeEqual() {
        RandomUtils instance = RandomUtils.secure();
        double result = instance.randomDouble(5.0, 5.0);
        assertEquals(5.0, result);
    }

    @Test
    void testRandomDoubleRangeNormal() {
        RandomUtils instance = RandomUtils.secure();
        double result = instance.randomDouble(1.0, 2.0);
        assertTrue(result >= 1.0 && result < 2.0);
    }

    @Test
    void testRandomDoubleInvalidRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomDouble(3.0, 1.0));
    }

    @Test
    void testRandomDoubleNegativeRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomDouble(-1.0, 2.0));
    }

    @Test
    void testRandomFloat() {
        RandomUtils instance = RandomUtils.secure();
        float result = instance.randomFloat();
        assertNotNull(Float.valueOf(result));
    }

    @Test
    void testRandomFloatRangeEqual() {
        RandomUtils instance = RandomUtils.secure();
        float result = instance.randomFloat(5.0f, 5.0f);
        assertEquals(5.0f, result);
    }

    @Test
    void testRandomFloatRangeNormal() {
        RandomUtils instance = RandomUtils.secure();
        float result = instance.randomFloat(1.0f, 2.0f);
        assertTrue(result >= 1.0f && result < 2.0f);
    }

    @Test
    void testRandomFloatInvalidRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomFloat(3.0f, 1.0f));
    }

    @Test
    void testRandomFloatNegativeRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomFloat(-1.0f, 2.0f));
    }

    @Test
    void testRandomInt() {
        RandomUtils instance = RandomUtils.secure();
        int result = instance.randomInt();
        assertNotNull(Integer.valueOf(result));
    }

    @Test
    void testRandomIntRangeEqual() {
        RandomUtils instance = RandomUtils.secure();
        int result = instance.randomInt(5, 5);
        assertEquals(5, result);
    }

    @Test
    void testRandomIntRangeNormal() {
        RandomUtils instance = RandomUtils.secure();
        int result = instance.randomInt(10, 20);
        assertTrue(result >= 10 && result < 20);
    }

    @Test
    void testRandomIntInvalidRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomInt(20, 10));
    }

    @Test
    void testRandomIntNegativeRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomInt(-5, 10));
    }

    @Test
    void testRandomLong() {
        RandomUtils instance = RandomUtils.secure();
        long result = instance.randomLong();
        assertNotNull(Long.valueOf(result));
    }

    @Test
    void testRandomLongRangeEqual() {
        RandomUtils instance = RandomUtils.secure();
        long result = instance.randomLong(5L, 5L);
        assertEquals(5L, result);
    }

    @Test
    void testRandomLongRangeNormal() {
        RandomUtils instance = RandomUtils.secure();
        long result = instance.randomLong(10L, 20L);
        assertTrue(result >= 10 && result < 20);
    }

    @Test
    void testRandomLongInvalidRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomLong(20L, 10L));
    }

    @Test
    void testRandomLongNegativeRange() {
        RandomUtils instance = RandomUtils.secure();
        assertThrows(IllegalArgumentException.class, () -> instance.randomLong(-5L, 10L));
    }

    @Test
    void testToString() {
        RandomUtils instance = RandomUtils.secure();
        String result = instance.toString();
        assertTrue(result.startsWith("RandomUtils [random="));
    }

    @Test
    void testConstructorNoArg() {
        RandomUtils instance = new RandomUtils();
        assertNotNull(instance);
        assertTrue(instance.random() instanceof SecureRandom);
    }
}