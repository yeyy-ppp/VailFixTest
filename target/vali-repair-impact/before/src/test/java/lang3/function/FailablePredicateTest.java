package lang3.function;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class FailablePredicateTest {

    @Test
    void testTrueConstant() throws Throwable {
        FailablePredicate<Object, Throwable> predicate = (FailablePredicate<Object, Throwable>) FailablePredicate.TRUE;
        assertTrue(predicate.test("any"));
    }

    @Test
    void testFalseConstant() throws Throwable {
        FailablePredicate<Object, Throwable> predicate = (FailablePredicate<Object, Throwable>) FailablePredicate.FALSE;
        assertFalse(predicate.test("any"));
    }

    @Test
    void testCustomPredicate() {
        FailablePredicate<Integer, RuntimeException> isEven = num -> num % 2 == 0;
        assertTrue(isEven.test(4));
        assertFalse(isEven.test(5));
    }

    @Test
    void testAndBothTrue() throws Exception {
        FailablePredicate<String, Exception> truePred = s -> true;
        FailablePredicate<String, Exception> combined = truePred.and(truePred);
        assertTrue(combined.test("test"));
    }

    @Test
    void testAndFirstFalse() throws Exception {
        FailablePredicate<Integer, Exception> falsePred = n -> false;
        FailablePredicate<Integer, Exception> truePred = n -> true;
        FailablePredicate<Integer, Exception> combined = falsePred.and(truePred);
        assertFalse(combined.test(10));
    }

    @Test
    void testAndSecondFalse() throws Exception {
        FailablePredicate<Integer, Exception> truePred = n -> true;
        FailablePredicate<Integer, Exception> falsePred = n -> false;
        FailablePredicate<Integer, Exception> combined = truePred.and(falsePred);
        assertFalse(combined.test(10));
    }

    @Test
    void testAndBothFalse() throws Throwable {
        FailablePredicate<Object, Throwable> falsePred = (FailablePredicate<Object, Throwable>) FailablePredicate.FALSE;
        FailablePredicate<Object, Throwable> combined = falsePred.and(falsePred);
        assertFalse(combined.test("test"));
    }

    @Test
    void testAndWithComplexLogic() throws Exception {
        FailablePredicate<Integer, Exception> greaterThan5 = n -> n > 5;
        FailablePredicate<Integer, Exception> lessThan10 = n -> n < 10;
        FailablePredicate<Integer, Exception> combined = greaterThan5.and(lessThan10);
        
        assertTrue(combined.test(7));
        assertFalse(combined.test(3));
        assertFalse(combined.test(12));
    }

    @Test
    void testAndNullOther() {
        FailablePredicate<String, Exception> pred = s -> true;
        assertThrows(NullPointerException.class, () -> pred.and(null));
    }

    @Test
    void testOrBothTrue() throws Exception {
        FailablePredicate<String, Exception> truePred = s -> true;
        FailablePredicate<String, Exception> combined = truePred.or(truePred);
        assertTrue(combined.test("test"));
    }

    @Test
    void testOrFirstTrue() throws Exception {
        FailablePredicate<Integer, Exception> truePred = n -> true;
        FailablePredicate<Integer, Exception> falsePred = n -> false;
        FailablePredicate<Integer, Exception> combined = truePred.or(falsePred);
        assertTrue(combined.test(10));
    }

    @Test
    void testOrSecondTrue() throws Exception {
        FailablePredicate<Integer, Exception> falsePred = n -> false;
        FailablePredicate<Integer, Exception> truePred = n -> true;
        FailablePredicate<Integer, Exception> combined = falsePred.or(truePred);
        assertTrue(combined.test(10));
    }

    @Test
    void testOrBothFalse() throws Throwable {
        FailablePredicate<Object, Throwable> falsePred = (FailablePredicate<Object, Throwable>) FailablePredicate.FALSE;
        FailablePredicate<Object, Throwable> combined = falsePred.or(falsePred);
        assertFalse(combined.test("test"));
    }

    @Test
    void testOrWithComplexLogic() throws Exception {
        FailablePredicate<Integer, Exception> lessThan3 = n -> n < 3;
        FailablePredicate<Integer, Exception> greaterThan7 = n -> n > 7;
        FailablePredicate<Integer, Exception> combined = lessThan3.or(greaterThan7);
        
        assertTrue(combined.test(2));
        assertTrue(combined.test(8));
        assertFalse(combined.test(5));
    }

    @Test
    void testOrNullOther() {
        FailablePredicate<String, Exception> pred = s -> true;
        assertThrows(NullPointerException.class, () -> pred.or(null));
    }
}