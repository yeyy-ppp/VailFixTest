package cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;

public class OptionGroupTest {

    private Option createOption(String key, String opt, String longOpt, String description) {
        return new Option(opt, longOpt, false, description);
    }

    @Test
    void testConstructor() {
        OptionGroup group = new OptionGroup();
        assertNotNull(group);
        assertTrue(group.getNames().isEmpty());
        assertFalse(group.isSelected());
        assertNull(group.getSelected());
        assertFalse(group.isRequired());
    }

    @Test
    void testAddOption() {
        OptionGroup group = new OptionGroup();
        Option option = createOption("a", "a", null, null);
        OptionGroup result = group.addOption(option);
        assertSame(group, result);
        assertEquals(1, group.getNames().size());
        assertTrue(group.getNames().contains("a"));
        assertTrue(group.getOptions().contains(option));
    }

    @Test
    void testAddMultipleOptions() {
        OptionGroup group = new OptionGroup();
        Option option1 = createOption("a", "a", null, null);
        Option option2 = createOption("b", null, "longB", null);
        group.addOption(option1).addOption(option2);
        assertEquals(2, group.getNames().size());
        assertTrue(group.getNames().contains("a"));
        assertTrue(group.getNames().contains("longB"));
        assertTrue(group.getOptions().contains(option1));
        assertTrue(group.getOptions().contains(option2));
    }

    @Test
    void testGetSelectedInitiallyNull() {
        OptionGroup group = new OptionGroup();
        assertNull(group.getSelected());
    }

    @Test
    void testIsRequiredInitiallyFalse() {
        OptionGroup group = new OptionGroup();
        assertFalse(group.isRequired());
    }

    @Test
    void testSetRequired() {
        OptionGroup group = new OptionGroup();
        group.setRequired(true);
        assertTrue(group.isRequired());
        group.setRequired(false);
        assertFalse(group.isRequired());
    }

    @Test
    void testIsSelectedInitiallyFalse() {
        OptionGroup group = new OptionGroup();
        assertFalse(group.isSelected());
    }

    @Test
    void testSetSelectedToNull() {
        OptionGroup group = new OptionGroup();
        //group.setSelected(null);
        assertNull(group.getSelected());
        assertFalse(group.isSelected());
    }

    @Test
    void testSetSelectedOption() throws AlreadySelectedException {
        OptionGroup group = new OptionGroup();
        Option option = createOption("a", "a", null, null);
        group.addOption(option);
        group.setSelected(option);
        assertEquals("a", group.getSelected());
        assertTrue(group.isSelected());
    }

    @Test
    void testSetSameOptionTwice() throws AlreadySelectedException {
        OptionGroup group = new OptionGroup();
        Option option = createOption("a", "a", null, null);
        group.addOption(option);
        group.setSelected(option);
        group.setSelected(option);
        assertEquals("a", group.getSelected());
    }

    @Test
    void testSetDifferentOptionThrowsException() {
        OptionGroup group = new OptionGroup();
        Option option1 = createOption("a", "a", null, null);
        Option option2 = createOption("b", "b", null, null);
        group.addOption(option1).addOption(option2);
        assertDoesNotThrow(() -> group.setSelected(option1));
        assertThrows(AlreadySelectedException.class, () -> group.setSelected(option2));
    }

    @Test
    void testSetSelectedAfterClear() throws AlreadySelectedException {
        OptionGroup group = new OptionGroup();
        Option option1 = createOption("a", "a", null, null);
        Option option2 = createOption("b", "b", null, null);
        group.addOption(option1).addOption(option2);
        group.setSelected(option1);
        group.setSelected(null);
        group.setSelected(option2);
        assertEquals("b", group.getSelected());
    }

    @Test
    void testToStringSingleOptionShort() {
        OptionGroup group = new OptionGroup();
        Option option = createOption("a", "a", null, null);
        group.addOption(option);
        assertEquals("[-a]", group.toString());
    }

    @Test
    void testToStringSingleOptionLong() {
        OptionGroup group = new OptionGroup();
        Option option = createOption("b", null, "longB", null);
        group.addOption(option);
        assertEquals("[--longB]", group.toString());
    }

    @Test
    void testToStringMultipleOptions() {
        OptionGroup group = new OptionGroup();
        Option option1 = createOption("a", "a", null, null);
        Option option2 = createOption("b", null, "longB", null);
        group.addOption(option1).addOption(option2);
        assertEquals("[-a, --longB]", group.toString());
    }

    @Test
    void testToStringWithDescription() {
        OptionGroup group = new OptionGroup();
        Option option = createOption("a", "a", null, "desc");
        group.addOption(option);
        assertTrue(group.toString().contains("-a desc"));
    }
}