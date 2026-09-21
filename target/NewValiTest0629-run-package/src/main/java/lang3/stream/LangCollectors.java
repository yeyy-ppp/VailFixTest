package lang3.stream;
import br.usp.each.saeg.commons.io.Streams;
import org.apache.maven.surefire.shared.lang3.StringUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

public final class LangCollectors {
    private static final class SimpleCollector<T, A, R> implements Collector<T, A, R> {
        private final BiConsumer<A, T> accumulator;
        private final Set<Characteristics> characteristics;
        private final BinaryOperator<A> combiner;
        private final Function<A, R> finisher;
        private final Supplier<A> supplier;
        private SimpleCollector(final Supplier<A> supplier, final BiConsumer<A, T> accumulator, final BinaryOperator<A> combiner, final Function<A, R> finisher,
            final Set<Characteristics> characteristics) {
            this.supplier = supplier;
            this.accumulator = accumulator;
            this.combiner = combiner;
            this.finisher = finisher;
            this.characteristics = characteristics;
        }
        @Override
        public BiConsumer<A, T> accumulator() {
            return accumulator;
        }
        @Override
        public Set<Characteristics> characteristics() {
            return characteristics;
        }
        @Override
        public BinaryOperator<A> combiner() {
            return combiner;
        }
        @Override
        public Function<A, R> finisher() {
            return finisher;
        }
        @Override
        public Supplier<A> supplier() {
            return supplier;
        }
    }
    private static final Set<Collector.Characteristics> CH_NOID = Collections.emptySet();
    public static Collector<Object, ?, String> joining() {
        return new SimpleCollector<>(StringBuilder::new, StringBuilder::append, StringBuilder::append, StringBuilder::toString, CH_NOID);
    }
    public static Collector<Object, ?, String> joining(final CharSequence delimiter) {
        return joining(delimiter, StringUtils.EMPTY, StringUtils.EMPTY);
    }
    public static Collector<Object, ?, String> joining(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix) {
        return joining(delimiter, prefix, suffix, Objects::toString);
    }
    public static Collector<Object, ?, String> joining(final CharSequence delimiter, final CharSequence prefix, final CharSequence suffix,
        final Function<Object, String> toString) {
        return new SimpleCollector<>(() -> new StringJoiner(delimiter, prefix, suffix), (a, t) -> a.add(toString.apply(t)), StringJoiner::merge,
            StringJoiner::toString, CH_NOID);
    }
    private LangCollectors() {
    }
}