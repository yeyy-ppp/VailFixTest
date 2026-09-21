package help;
import java.util.List;
public interface TableDefinition {
    static TableDefinition from(
            final String caption,
            final List<TextStyle> columnStyle,
            final List<String> headers,
            final Iterable<List<String>> rows) {
        return new TableDefinition() {
            @Override
            public String caption() {
                return caption;
            }
            @Override
            public List<TextStyle> columnTextStyles() {
                return columnStyle;
            }
            @Override
            public List<String> headers() {
                return headers;
            }
            @Override
            public Iterable<List<String>> rows() {
                return rows;
            }
        };
    }
    String caption();
    List<TextStyle> columnTextStyles();
    List<String> headers();
    Iterable<List<String>> rows();
}