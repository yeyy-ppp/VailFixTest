package cli.help;

import cli.Option;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HelpFormatter extends AbstractHelpFormatter {
    public static class Builder extends AbstractHelpFormatter.Builder<Builder, HelpFormatter> {
        public boolean showSince = true;
        protected Builder() {
        }
        @Override
        public HelpFormatter get() {
            return new HelpFormatter(this);
        }
        public Builder setShowSince(final boolean showSince) {
            this.showSince = showSince;
            return this;
        }
    }
    public static final int DEFAULT_WIDTH = 74;
    public static final int DEFAULT_LEFT_PAD = 1;
    public static final int DEFAULT_COLUMN_SPACING = 5;
    public static Builder builder() {
        return new Builder();
    }
    public final boolean showSince;
    protected HelpFormatter(final Builder builder) {
        super(builder);
        this.showSince = builder.showSince;
    }
    @Override
    public TableDefinition getTableDefinition(final Iterable<Option> options) {
        final TextStyle.Builder builder =
                TextStyle.builder()
                        .setAlignment(TextStyle.Alignment.LEFT)
                        .setIndent(DEFAULT_LEFT_PAD)
                        .setScalable(false);
        final List<TextStyle> styles = new ArrayList<>();
        styles.add(builder.get());
        builder.setScalable(true).setLeftPad(DEFAULT_COLUMN_SPACING);
        if (showSince) {
            builder.setAlignment(TextStyle.Alignment.CENTER);
            styles.add(builder.get());
        }
        builder.setAlignment(TextStyle.Alignment.LEFT);
        styles.add(builder.get());
        final List<List<String>> rows = new ArrayList<>();
        final StringBuilder sb = new StringBuilder();
        for (final Option option : options) {
            final List<String> row = new ArrayList<>();
            final OptionFormatter formatter = getOptionFormatBuilder().build(option);
            sb.setLength(0);
            sb.append(formatter.getBothOpt());
            if (option.hasArg()) {
                sb.append(" ").append(formatter.getArgName());
            }
            row.add(sb.toString());
            if (showSince) {
                row.add(formatter.getSince());
            }
            row.add(formatter.getDescription());
            rows.add(row);
        }
        return TableDefinition.from(
                "",
                styles,
                showSince
                        ? Arrays.asList("Options", "Since", "Description")
                        : Arrays.asList("Options", "Description"),
                rows);
    }
}
