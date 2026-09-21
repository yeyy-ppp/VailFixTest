package cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

class AmbiguousOptionExceptionTest {

    @Test
    void testCreateMessageWithEmptyCollection() {
        String message = AmbiguousOptionException.createMessage("opt", Collections.emptyList());
        assertEquals("Ambiguous option: 'opt'  (could be: )", message);
    }

    @Test
    void testCreateMessageWithSingleOption() {
        List<String> options = Collections.singletonList("option1");
        String message = AmbiguousOptionException.createMessage("opt", options);
        assertEquals("Ambiguous option: 'opt'  (could be: 'option1')", message);
    }

    @Test
    void testCreateMessageWithTwoOptions() {
        List<String> options = Arrays.asList("option1", "option2");
        String message = AmbiguousOptionException.createMessage("opt", options);
        assertEquals("Ambiguous option: 'opt'  (could be: 'option1', 'option2')", message);
    }

    @Test
    void testCreateMessageWithThreeOptions() {
        List<String> options = Arrays.asList("optA", "optB", "optC");
        String message = AmbiguousOptionException.createMessage("opt", options);
        assertEquals("Ambiguous option: 'opt'  (could be: 'optA', 'optB', 'optC')", message);
    }

    @Test
    void testConstructorWithEmptyCollection() {
        Collection<String> matchingOptions = new HashSet<>();
        AmbiguousOptionException exception = new AmbiguousOptionException("opt", matchingOptions);
        assertEquals(matchingOptions, exception.getMatchingOptions());
        assertEquals("opt", exception.getOption());
        assertEquals("Ambiguous option: 'opt'  (could be: )", exception.getMessage());
    }

    @Test
    void testConstructorWithSingleOption() {
        Collection<String> matchingOptions = Collections.singleton("optionX");
        AmbiguousOptionException exception = new AmbiguousOptionException("opt", matchingOptions);
        assertEquals(matchingOptions, exception.getMatchingOptions());
        assertEquals("opt", exception.getOption());
        assertTrue(exception.getMessage().contains("'opt'"));
        assertTrue(exception.getMessage().contains("'optionX'"));
    }

    @Test
    void testConstructorWithMultipleOptions() {
        Collection<String> matchingOptions = Arrays.asList("alpha", "beta", "gamma");
        AmbiguousOptionException exception = new AmbiguousOptionException("opt", matchingOptions);
        assertEquals(new HashSet<>(matchingOptions), new HashSet<>(exception.getMatchingOptions()));
        assertEquals("opt", exception.getOption());
        assertTrue(exception.getMessage().contains("'alpha'"));
        assertTrue(exception.getMessage().contains("'beta'"));
        assertTrue(exception.getMessage().contains("'gamma'"));
    }

    @Test
    void testGetMatchingOptions() {
        Collection<String> matchingOptions = Arrays.asList("A", "B");
        AmbiguousOptionException exception = new AmbiguousOptionException("opt", matchingOptions);
        Collection<String> result = exception.getMatchingOptions();
        assertEquals(new HashSet<>(matchingOptions), new HashSet<>(result));
    }
}