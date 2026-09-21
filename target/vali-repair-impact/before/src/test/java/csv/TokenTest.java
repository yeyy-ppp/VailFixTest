package csv;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import static org.junit.jupiter.api.Assertions.*;

class TokenTest {

    private void setTokenType(Token token, Token.Type type) throws NoSuchFieldException, IllegalAccessException {
        Field typeField = Token.class.getDeclaredField("type");
        typeField.setAccessible(true);
        typeField.set(token, type);
    }
    
    private void setTokenContent(Token token, String content) throws NoSuchFieldException, IllegalAccessException {
        Field contentField = Token.class.getDeclaredField("content");
        contentField.setAccessible(true);
        StringBuilder sb = (StringBuilder) contentField.get(token);
        sb.setLength(0);
        sb.append(content);
    }
    
    private void setTokenIsReady(Token token, boolean ready) throws NoSuchFieldException, IllegalAccessException {
        Field field = Token.class.getDeclaredField("isReady");
        field.setAccessible(true);
        field.setBoolean(token, ready);
    }
    
    private void setTokenIsQuoted(Token token, boolean quoted) throws NoSuchFieldException, IllegalAccessException {
        Field field = Token.class.getDeclaredField("isQuoted");
        field.setAccessible(true);
        field.setBoolean(token, quoted);
    }
    
    private boolean getTokenIsReady(Token token) throws NoSuchFieldException, IllegalAccessException {
        Field field = Token.class.getDeclaredField("isReady");
        field.setAccessible(true);
        return field.getBoolean(token);
    }
    
    private boolean getTokenIsQuoted(Token token) throws NoSuchFieldException, IllegalAccessException {
        Field field = Token.class.getDeclaredField("isQuoted");
        field.setAccessible(true);
        return field.getBoolean(token);
    }

    @Test
    void testReset() throws NoSuchFieldException, IllegalAccessException {
        Token token = new Token();
        setTokenContent(token, "data");
        setTokenType(token, Token.Type.TOKEN);
        setTokenIsReady(token, true);
        setTokenIsQuoted(token, true);

        token.reset();

        assertEquals("INVALID []", token.toString());
        assertFalse(getTokenIsReady(token));
        assertFalse(getTokenIsQuoted(token));
    }

    @Test
    void testToString_InvalidTypeEmptyContent() throws NoSuchFieldException, IllegalAccessException {
        Token token = new Token();
        setTokenType(token, Token.Type.INVALID);
        assertEquals("INVALID []", token.toString());
    }

    @Test
    void testToString_CommentTypeWithContent() throws NoSuchFieldException, IllegalAccessException {
        Token token = new Token();
        setTokenType(token, Token.Type.COMMENT);
        setTokenContent(token, "note");
        assertEquals("COMMENT [note]", token.toString());
    }

    @Test
    void testToString_EORecordTypeWithContent() throws NoSuchFieldException, IllegalAccessException {
        Token token = new Token();
        setTokenType(token, Token.Type.EORECORD);
        setTokenContent(token, "end");
        assertEquals("EORECORD [end]", token.toString());
    }

    @Test
    void testToString_EOFTypeEmptyContent() throws NoSuchFieldException, IllegalAccessException {
        Token token = new Token();
        setTokenType(token, Token.Type.EOF);
        assertEquals("EOF []", token.toString());
    }

    @Test
    void testToString_TokenTypeWithContent() throws NoSuchFieldException, IllegalAccessException {
        Token token = new Token();
        setTokenType(token, Token.Type.TOKEN);
        setTokenContent(token, "value");
        assertEquals("TOKEN [value]", token.toString());
    }
}