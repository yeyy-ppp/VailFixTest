package lang3;
import java.util.Comparator;
public class NumberRange<N extends Number> extends Range<N> {
    private static final long serialVersionUID = 1L;
    public NumberRange(final N number1, final N number2, final Comparator<N> comp) {
        super(number1, number2, comp);
    }
}