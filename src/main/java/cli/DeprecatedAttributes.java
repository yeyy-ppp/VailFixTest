package cli;
import java.util.function.Supplier;
public final class DeprecatedAttributes {
    public static class Builder implements Supplier<DeprecatedAttributes> {
        private String description;
        private boolean forRemoval;
        private String since;
        @Deprecated
        public Builder() {
        }
        @Override
        public DeprecatedAttributes get() {
            return new DeprecatedAttributes(description, since, forRemoval);
        }
        public Builder setDescription(final String description) {
            this.description = description;
            return this;
        }
        public Builder setForRemoval(final boolean forRemoval) {
            this.forRemoval = forRemoval;
            return this;
        }
        public Builder setSince(final String since) {
            this.since = since;
            return this;
        }
    }
    static final DeprecatedAttributes DEFAULT = new DeprecatedAttributes("", "", false);
    private static final String EMPTY_STRING = "";
    public static Builder builder() {
        return new Builder();
    }
    private final String description;
    private final boolean forRemoval;
    private final String since;
    private DeprecatedAttributes(
            final String description, final String since, final boolean forRemoval) {
        this.description = toEmpty(description);
        this.since = toEmpty(since);
        this.forRemoval = forRemoval;
    }
    public String getDescription() {
        return description;
    }
    public String getSince() {
        return since;
    }
    public boolean isForRemoval() {
        return forRemoval;
    }
    private String toEmpty(final String since) {
        return since != null ? since : EMPTY_STRING;
    }
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder("Deprecated");
        if (forRemoval) {
            builder.append(" for removal");
        }
        if (!since.isEmpty()) {
            builder.append(" since ");
            builder.append(since);
        }
        if (!description.isEmpty()) {
            builder.append(": ");
            builder.append(description);
        }
        return builder.toString();
    }
}

