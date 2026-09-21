package help;
import java.util.function.Supplier;
public final class TextStyle {
    public enum Alignment {
        LEFT,
        CENTER,
        RIGHT
    }
    public static final class Builder implements Supplier<TextStyle> {
        private Alignment alignment = Alignment.LEFT;
        private int leftPad;
        private int indent;
        private boolean scalable = true;
        private int minWidth;
        private int maxWidth = UNSET_MAX_WIDTH;
        private Builder() {}
        @Override
        public TextStyle get() {
            return new TextStyle(this);
        }
        public int getIndent() {
            return indent;
        }
        public int getLeftPad() {
            return leftPad;
        }
        public int getMaxWidth() {
            return maxWidth;
        }
        public int getMinWidth() {
            return minWidth;
        }
        public boolean isScalable() {
            return scalable;
        }
        public Builder setAlignment(final Alignment alignment) {
            this.alignment = alignment;
            return this;
        }
        public Builder setIndent(final int indent) {
            this.indent = indent;
            return this;
        }
        public Builder setLeftPad(final int leftPad) {
            this.leftPad = leftPad;
            return this;
        }
        public Builder setMaxWidth(final int maxWidth) {
            this.maxWidth = maxWidth;
            return this;
        }
        public Builder setMinWidth(final int minWidth) {
            this.minWidth = minWidth;
            return this;
        }
        public Builder setScalable(final boolean scalable) {
            this.scalable = scalable;
            return this;
        }
        public Builder setTextStyle(final TextStyle style) {
            this.alignment = style.alignment;
            this.leftPad = style.leftPad;
            this.indent = style.indent;
            this.scalable = style.scalable;
            this.minWidth = style.minWidth;
            this.maxWidth = style.maxWidth;
            return this;
        }
    }
    public static final int UNSET_MAX_WIDTH = Integer.MAX_VALUE;
    public static final TextStyle DEFAULT = builder().get();
    public static Builder builder() {
        return new Builder();
    }
    private final Alignment alignment;
    private final int leftPad;
    private final int indent;
    private final boolean scalable;
    private final int minWidth;
    private final int maxWidth;
    TextStyle(final Builder builder) {
        this.alignment = builder.alignment;
        this.leftPad = builder.leftPad;
        this.indent = builder.indent;
        this.scalable = builder.scalable;
        this.minWidth = builder.minWidth;
        this.maxWidth = builder.maxWidth;
    }
    public Alignment getAlignment() {
        return alignment;
    }
    public int getIndent() {
        return indent;
    }
    public int getLeftPad() {
        return leftPad;
    }
    public int getMaxWidth() {
        return maxWidth;
    }
    public int getMinWidth() {
        return minWidth;
    }
    public boolean isScalable() {
        return scalable;
    }
    public CharSequence pad(final boolean addIndent, final CharSequence text) {
        if (text.length() >= maxWidth) {
            return text;
        }
        String indentPad;
        String rest;
        final StringBuilder sb = new StringBuilder();
        switch (alignment) {
            case CENTER:
                int padLen;
                if (maxWidth == UNSET_MAX_WIDTH) {
                    padLen = addIndent ? indent : 0;
                } else {
                    padLen = maxWidth - text.length();
                }
                final int left = padLen / 2;
                indentPad = Util.repeatSpace(left);
                rest = Util.repeatSpace(padLen - left);
                sb.append(indentPad).append(text).append(rest);
                break;
            case LEFT:
            case RIGHT:
            default:
                if (maxWidth == UNSET_MAX_WIDTH) {
                    indentPad = addIndent ? Util.repeatSpace(indent) : "";
                    rest = "";
                } else {
                    int restLen = maxWidth - text.length();
                    if (addIndent && restLen > indent) {
                        indentPad = Util.repeatSpace(indent);
                        restLen -= indent;
                    } else {
                        indentPad = "";
                    }
                    rest = Util.repeatSpace(restLen);
                }
                if (alignment == Alignment.LEFT) {
                    sb.append(indentPad).append(text).append(rest);
                } else {
                    sb.append(indentPad).append(rest).append(text);
                }
                break;
        }
        return sb.toString();
    }
    @Override
    public String toString() {
        return String.format(
                "TextStyle{%s, l:%s, i:%s, %s, min:%s, max:%s}",
                alignment,
                leftPad,
                indent,
                scalable,
                minWidth,
                maxWidth == UNSET_MAX_WIDTH ? "unset" : maxWidth);
    }
}