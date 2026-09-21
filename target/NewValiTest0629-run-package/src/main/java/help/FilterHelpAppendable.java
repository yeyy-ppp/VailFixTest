package help;
import java.io.IOException;
public abstract class FilterHelpAppendable implements HelpAppendable {
    protected final Appendable output;
    protected FilterHelpAppendable(final Appendable output) {
        this.output = output;
    }
    @Override
    public FilterHelpAppendable append(final char ch) throws IOException {
        output.append(ch);
        return this;
    }
    @Override
    public FilterHelpAppendable append(final CharSequence text) throws IOException {
        output.append(text);
        return this;
    }
    @Override
    public FilterHelpAppendable append(final CharSequence csq, final int start, final int end)
            throws IOException {
        output.append(csq, start, end);
        return this;
    }
}