package gson;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FieldNamingPolicyTest {

    @Test
    void separateCamelCase_emptyString() {
        String result = FieldNamingPolicy.separateCamelCase("", '-');
        assertEquals("", result);
    }

    @Test
    void separateCamelCase_singleLowerCase() {
        String result = FieldNamingPolicy.separateCamelCase("a", '_');
        assertEquals("a", result);
    }

    @Test
    void separateCamelCase_singleUpperCase() {
        String result = FieldNamingPolicy.separateCamelCase("A", '.');
        assertEquals("A", result);
    }

    @Test
    void separateCamelCase_twoWords() {
        String result = FieldNamingPolicy.separateCamelCase("camelCase", '_');
        assertEquals("camel_Case", result);
    }

    @Test
    void separateCamelCase_multipleWords() {
        String result = FieldNamingPolicy.separateCamelCase("oneTwoThreeFour", '-');
        assertEquals("one-Two-Three-Four", result);
    }

    @Test
    void separateCamelCase_leadingUpperCase() {
        String result = FieldNamingPolicy.separateCamelCase("CamelCase", '.');
        assertEquals("Camel.Case", result);
    }

    @Test
    void separateCamelCase_consecutiveUpper() {
        String result = FieldNamingPolicy.separateCamelCase("parseURL", '_');
        assertEquals("parse_U_R_L", result);
    }

    @Test
    void separateCamelCase_allUpperCase() {
        String result = FieldNamingPolicy.separateCamelCase("ABCD", '-');
        assertEquals("A-B-C-D", result);
    }

    @Test
    void upperCaseFirstLetter_emptyString() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("");
        assertEquals("", result);
    }

    @Test
    void upperCaseFirstLetter_singleCharLower() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("a");
        assertEquals("A", result);
    }

    @Test
    void upperCaseFirstLetter_singleCharUpper() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("A");
        assertEquals("A", result);
    }

    @Test
    void upperCaseFirstLetter_singleNonLetter() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("1");
        assertEquals("1", result);
    }

    @Test
    void upperCaseFirstLetter_startsWithLetter() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("hello");
        assertEquals("Hello", result);
    }

    @Test
    void upperCaseFirstLetter_startsWithNonLetter() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("_hello");
        assertEquals("_Hello", result);
    }

    @Test
    void upperCaseFirstLetter_startsWithNonLetterFollowedByUpper() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("_Hello");
        assertEquals("_Hello", result);
    }

    @Test
    void upperCaseFirstLetter_leadingNonLetters() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("123abc");
        assertEquals("123Abc", result);
    }

    @Test
    void upperCaseFirstLetter_upperAfterNonLetter() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("@World");
        assertEquals("@World", result);
    }

    @Test
    void upperCaseFirstLetter_noLetters() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("123!@#");
        assertEquals("123!@#", result);
    }

    @Test
    void upperCaseFirstLetter_midLetterChange() {
        String result = FieldNamingPolicy.upperCaseFirstLetter("helloWorld");
        assertEquals("HelloWorld", result);
    }
}