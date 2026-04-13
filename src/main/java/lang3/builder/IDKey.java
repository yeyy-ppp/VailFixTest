package lang3.builder;
final class IDKey {
    private final Object value;
    private final int id;
    IDKey(final Object value) {
        this.id = System.identityHashCode(value);
        this.value = value;
    }
    @Override
    public boolean equals(final Object other) {
        if (!(other instanceof IDKey)) {
            return false;
        }
        final IDKey idKey = (IDKey) other;
        if (id != idKey.id) {
            return false;
        }
        return value == idKey.value;
    }
    @Override
    public int hashCode() {
        return id;
    }
}