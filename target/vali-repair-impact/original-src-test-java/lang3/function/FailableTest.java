package lang3.function;

import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import lombok.var;
import org.junit.jupiter.api.Test;

public class FailableTest {

    @Test
    void testFailableConstructor() throws Exception {
        var constructor = Failable.class.getDeclaredConstructor();
        assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        assertNotNull(constructor.newInstance());
    }

    @Test
    void testAcceptFailableBiConsumer_Normal() {
        Failable.accept((t, u) -> {}, "a", 1);
    }

    @Test
    void testAcceptFailableBiConsumer_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.accept((t, u) -> { throw new IOException(); }, "a", 1)
        );
    }

    @Test
    void testAcceptFailableConsumer_Normal() {
        Failable.accept(t -> {}, "a");
    }

    @Test
    void testAcceptFailableConsumer_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.accept(t -> { throw new IOException(); }, "a")
        );
    }

    @Test
    void testApplyFailableBiFunction_Normal() {
        assertEquals("a1", Failable.apply((t, u) -> t + u, "a", 1));
    }

    @Test
    void testApplyFailableBiFunction_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.apply((t, u) -> { throw new IOException(); }, "a", 1)
        );
    }

   /* @Test
    void testApplyFailableFunction_Normal() {
        assertEquals(java.util.Optional.of(1), Failable.apply(t -> 1, "a"));
    }*/

    @Test
    void testApplyFailableFunction_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.apply(t -> { throw new IOException(); }, "a")
        );
    }

    @Test
    void testApplyNonNull_Single_NullValue() throws Exception {
        assertNull(Failable.applyNonNull(null, t -> "mapped"));
    }

    @Test
    void testApplyNonNull_Single_NonNullValue() throws Exception {
        assertEquals("mapped", Failable.applyNonNull("a", t -> "mapped"));
    }

    @Test
    void testApplyNonNull_Single_Exception() throws IOException {
        assertThrows(IOException.class, () -> 
            Failable.applyNonNull("a", t -> { throw new IOException(); })
        );
    }

    @Test
    void testApplyNonNull_Two_NullValue() throws Exception {
        assertNull(Failable.applyNonNull(null, t -> "u", u -> "r"));
    }

    @Test
    void testApplyNonNull_Two_NonNullValue() throws Exception {
        assertEquals("r", Failable.applyNonNull("a", t -> "u", u -> "r"));
    }

    @Test
    void testApplyNonNull_Two_FirstMapperException() throws IOException {
        assertThrows(IOException.class, () -> 
            Failable.applyNonNull("a", t -> { throw new IOException(); }, u -> "r")
        );
    }

    @Test
    void testApplyNonNull_Three_NullValue() throws Exception {
        assertNull(Failable.applyNonNull(null, t -> "u", u -> "v", v -> "r"));
    }

    @Test
    void testApplyNonNull_Three_NonNullValue() throws Exception {
        assertEquals("r", Failable.applyNonNull("a", t -> "u", u -> "v", v -> "r"));
    }

    @Test
    void testApplyNonNull_Three_SecondMapperException() throws IOException {
        assertThrows(IOException.class, () -> 
            Failable.applyNonNull("a", t -> "u", u -> { throw new IOException(); }, v -> "r")
        );
    }

    @Test
    void testAsBiConsumer_Normal() {
        BiConsumer<String, Integer> consumer = Failable.asBiConsumer((t, u) -> {});
        consumer.accept("a", 1);
    }

    @Test
    void testAsBiConsumer_Exception() {
        BiConsumer<String, Integer> consumer = Failable.asBiConsumer((t, u) -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> consumer.accept("a", 1));
    }

    @Test
    void testAsBiFunction_Normal() {
        BiFunction<String, Integer, String> function = Failable.asBiFunction((t, u) -> t + u);
        assertEquals("a1", function.apply("a", 1));
    }

    @Test
    void testAsBiFunction_Exception() {
        BiFunction<String, Integer, String> function = Failable.asBiFunction((t, u) -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> function.apply("a", 1));
    }

    @Test
    void testAsBiPredicate_Normal() {
        BiPredicate<String, Integer> predicate = Failable.asBiPredicate((t, u) -> true);
        assertTrue(predicate.test("a", 1));
    }

    @Test
    void testAsBiPredicate_Exception() {
        BiPredicate<String, Integer> predicate = Failable.asBiPredicate((t, u) -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> predicate.test("a", 1));
    }

    @Test
    void testAsCallable_Normal() throws Exception {
        Callable<String> callable = Failable.asCallable(() -> "value");
        assertEquals("value", callable.call());
    }

    @Test
    void testAsCallable_Exception() {
        Callable<String> callable = Failable.asCallable(() -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> callable.call());
    }

    @Test
    void testAsConsumer_Normal() {
        Consumer<String> consumer = Failable.asConsumer(t -> {});
        consumer.accept("a");
    }

    @Test
    void testAsConsumer_Exception() {
        Consumer<String> consumer = Failable.asConsumer(t -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> consumer.accept("a"));
    }

    @Test
    void testAsFunction_Normal() {
        Function<String, Integer> function = Failable.asFunction(t -> 1);
        assertEquals(1, function.apply("a"));
    }

    @Test
    void testAsFunction_Exception() {
        Function<String, Integer> function = Failable.asFunction(t -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> function.apply("a"));
    }

    @Test
    void testAsPredicate_Normal() {
        Predicate<String> predicate = Failable.asPredicate(t -> true);
        assertTrue(predicate.test("a"));
    }

    @Test
    void testAsPredicate_Exception() {
        Predicate<String> predicate = Failable.asPredicate(t -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, () -> predicate.test("a"));
    }

    @Test
    void testAsRunnable_Normal() {
        Runnable runnable = Failable.asRunnable(() -> {});
        runnable.run();
    }

    @Test
    void testAsRunnable_Exception() {
        Runnable runnable = Failable.asRunnable(() -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, runnable::run);
    }

    @Test
    void testAsSupplier_Normal() {
        Supplier<String> supplier = Failable.asSupplier(() -> "value");
        assertEquals("value", supplier.get());
    }

    @Test
    void testAsSupplier_Exception() {
        Supplier<String> supplier = Failable.asSupplier(() -> { throw new IOException(); });
        assertThrows(UncheckedIOException.class, supplier::get);
    }

    @Test
    void testCall_Normal() {
        assertEquals("value", Failable.call(() -> "value"));
    }

    @Test
    void testCall_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.call(() -> { throw new IOException(); })
        );
    }

    @Test
    void testGet_Normal() {
        assertEquals("value", Failable.get(() -> "value"));
    }

    @Test
    void testGet_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.get(() -> { throw new IOException(); })
        );
    }

    @Test
    void testRethrow_IOException() {
        Throwable t = new IOException();
        assertThrows(UncheckedIOException.class, () -> Failable.rethrow(t));
    }

    @Test
    void testRethrow_OtherException() {
        Throwable t = new Exception();
        assertThrows(UndeclaredThrowableException.class, () -> Failable.rethrow(t));
    }

    @Test
    void testRunFailableRunnable_Null() {
        Failable.run((FailableRunnable<Exception>) null);
    }

    @Test
    void testRunFailableRunnable_Normal() {
        Failable.run(() -> {});
    }

    @Test
    void testRunFailableRunnable_Exception() {
        assertThrows(UncheckedIOException.class, () -> 
            Failable.run(() -> { throw new IOException(); })
        );
    }
}