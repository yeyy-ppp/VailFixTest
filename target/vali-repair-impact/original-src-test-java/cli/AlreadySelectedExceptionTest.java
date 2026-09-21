package cli;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class AlreadySelectedExceptionTest {

    @Test
    public void testConstructorWithOptionGroupAndOption() {
        OptionGroup group = new OptionGroup();
        Option selectedOption = Option.builder("selected").build();
        Option conflictOption = Option.builder("conflict").build();
        
        try {
            group.setSelected(selectedOption);
        } catch (Exception e) {
            fail("Setup failed");
        }
        
        AlreadySelectedException exception = new AlreadySelectedException(group, conflictOption);
        
        assertEquals(
            String.format(
                "The option '%s' was specified but an option from this group has already been selected: '%s'",
                conflictOption.getKey(), group.getSelected()),
            exception.getMessage()
        );
        assertEquals(conflictOption, exception.getOption());
        assertEquals(group, exception.getOptionGroup());
    }

    @Test
    public void testConstructorWithMessage() {
        String message = "Custom message";
        AlreadySelectedException exception = new AlreadySelectedException(message);
        
        assertEquals(message, exception.getMessage());
        assertNull(exception.getOption());
        assertNull(exception.getOptionGroup());
    }

    @Test
    public void testConstructorWithMessageGroupAndOption() {
        String message = "Detailed message";
        OptionGroup group = new OptionGroup();
        Option option = Option.builder("testOption").build();
        
        AlreadySelectedException exception = new AlreadySelectedException(message, group, option);
        
        assertEquals(message, exception.getMessage());
        assertEquals(group, exception.getOptionGroup());
        assertEquals(option, exception.getOption());
    }

    @Test
    public void testGetOptionWhenNull() {
        AlreadySelectedException exception = new AlreadySelectedException("Test");
        assertNull(exception.getOption());
    }

    @Test
    public void testGetOptionGroupWhenNull() {
        AlreadySelectedException exception = new AlreadySelectedException("Test");
        assertNull(exception.getOptionGroup());
    }

    @Test
    public void testConstructorWithNullGroup() {
        Option option = Option.builder("test").build();
        AlreadySelectedException exception = new AlreadySelectedException("Test message", null, option);
        
        assertNull(exception.getOptionGroup());
        assertEquals(option, exception.getOption());
    }

    @Test
    public void testConstructorWithNullOption() {
        OptionGroup group = new OptionGroup();
        AlreadySelectedException exception = new AlreadySelectedException("Test message", group, null);
        
        assertEquals(group, exception.getOptionGroup());
        assertNull(exception.getOption());
    }
}