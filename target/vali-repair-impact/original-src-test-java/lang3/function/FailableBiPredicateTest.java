package lang3.function;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("unchecked")
public class FailableBiPredicateTest {

    private static final Object OBJ1 = new Object();
    private static final Object OBJ2 = new Object();

    @Test
    void testTrueConstant() throws Throwable {
        assertTrue(FailableBiPredicate.TRUE.test(OBJ1, OBJ2));
    }

    @Test
    void testFalseConstant() throws Throwable {
        assertFalse(FailableBiPredicate.FALSE.test(OBJ1, OBJ2));
    }

    @Test
    void testAndBothTrue() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.and(p2);
        assertTrue(result.test(OBJ1, OBJ2));
    }

    @Test
    void testAndFirstTrueSecondFalse() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.and(p2);
        assertFalse(result.test(OBJ1, OBJ2));
    }

    @Test
    void testAndFirstFalseSecondTrue() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.and(p2);
        assertFalse(result.test(OBJ1, OBJ2));
    }

    @Test
    void testAndBothFalse() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.and(p2);
        assertFalse(result.test(OBJ1, OBJ2));
    }

    @Test
    void testAndNullOther() {
        FailableBiPredicate<Object, Object, RuntimeException> p = (t, u) -> true;
        assertThrows(NullPointerException.class, () -> p.and(null));
    }

    @Test
    void testOrBothTrue() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.or(p2);
        assertTrue(result.test(OBJ1, OBJ2));
    }

    @Test
    void testOrFirstTrueSecondFalse() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.or(p2);
        assertTrue(result.test(OBJ1, OBJ2));
    }

    @Test
    void testOrFirstFalseSecondTrue() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> true;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.or(p2);
        assertTrue(result.test(OBJ1, OBJ2));
    }

    @Test
    void testOrBothFalse() throws Exception {
        FailableBiPredicate<Object, Object, RuntimeException> p1 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> p2 = (t, u) -> false;
        FailableBiPredicate<Object, Object, RuntimeException> result = p1.or(p2);
        assertFalse(result.test(OBJ1, OBJ2));
    }

    @Test
    void testOrNullOther() {
        FailableBiPredicate<Object, Object, RuntimeException> p = (t, u) -> true;
        assertThrows(NullPointerException.class, () -> p.or(null));
    }

    @Test
    void testCustomPredicateTrue() throws Exception {
        FailableBiPredicate<String, Integer, RuntimeException> predicate = (s, i) -> s != null && i > 0;
        assertTrue(predicate.test("test", 10));
    }

    @Test
    void testCustomPredicateFalse() throws Exception {
        FailableBiPredicate<String, Integer, RuntimeException> predicate = (s, i) -> s != null && i > 0;
        assertFalse(predicate.test(null, 5));
    }
}