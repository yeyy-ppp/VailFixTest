package cli;
@Deprecated
public class BasicParser extends Parser {
    public BasicParser() {
    }
    @Override
    protected String[] flatten(
            @SuppressWarnings("unused") final Options options,
            final String[] arguments,
            @SuppressWarnings("unused") final boolean stopAtNonOption) {
        return arguments;
    }
}

