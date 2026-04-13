package lang3.builder;
@FunctionalInterface
public interface Builder<T> {
    T build();
}