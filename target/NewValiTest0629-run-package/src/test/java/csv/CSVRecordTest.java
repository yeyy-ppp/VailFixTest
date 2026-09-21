package csv;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class CSVRecordTest {

    private enum TestEnum { COLUMN1, COLUMN2 }

    @Test
    void testConstructorWithNullValues() {
        String[] values = null;
        CSVRecord record = new CSVRecord(null, values, "comment", 1, 2, 3);
        assertEquals(0, record.size());
        assertEquals("comment", record.getComment());
        assertEquals(1, record.getRecordNumber());
        assertEquals(2, record.getCharacterPosition());
        assertEquals(3, record.getBytePosition());
    }

    @Test
    void testGetByEnum() throws IOException {
        String[] values = {"val1"};
        Map<String, Integer> headerMap = Collections.singletonMap("COLUMN1", 0);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        CSVRecord record = new CSVRecord(parser, values, null, 1, 0, 0);
        assertEquals("val1", record.get(TestEnum.COLUMN1));
    }

    @Test
    void testGetByEnumNull() {
        CSVRecord record = new CSVRecord(null, new String[0], null, 0, 0, 0);
        assertThrows(IllegalStateException.class, () -> record.get((Enum<?>) null));
    }

    @Test
    void testGetByIndex() {
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(null, values, null, 0, 0, 0);
        assertEquals("val1", record.get(0));
        assertEquals("val2", record.get(1));
    }

    @Test
    void testGetByIndexOutOfBounds() {
        CSVRecord record = new CSVRecord(null, new String[]{"val"}, null, 0, 0, 0);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> record.get(-1));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> record.get(1));
    }

    @Test
    void testGetByName() throws IOException {
        String[] values = {"val1"};
        Map<String, Integer> headerMap = Collections.singletonMap("testCol", 0);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        CSVRecord record = new CSVRecord(parser, values, null, 0, 0, 0);
        assertEquals("val1", record.get("testCol"));
    }

    @Test
    void testGetByNameNoHeader() {
        CSVRecord record = new CSVRecord(null, new String[0], null, 0, 0, 0);
        assertThrows(IllegalStateException.class, () -> record.get("test"));
    }

    @Test
    void testGetByNameNotFound() throws IOException {
        Map<String, Integer> headerMap = new HashMap<>();
        headerMap.put("col1", 0);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        CSVRecord record = new CSVRecord(parser, new String[]{"val"}, null, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> record.get("invalid"));
    }

    @Test
    void testGetByNameIndexOutOfBounds() throws IOException {
        Map<String, Integer> headerMap = Collections.singletonMap("testCol", 1);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        CSVRecord record = new CSVRecord(parser, new String[]{"val"}, null, 0, 0, 0);
        assertThrows(IllegalArgumentException.class, () -> record.get("testCol"));
    }

    @Test
    void testHasComment() {
        CSVRecord withComment = new CSVRecord(null, new String[0], "comment", 0, 0, 0);
        assertTrue(withComment.hasComment());
        
        CSVRecord withoutComment = new CSVRecord(null, new String[0], null, 0, 0, 0);
        assertFalse(withoutComment.hasComment());
    }

    @Test
    void testIsConsistent() throws IOException {
        Map<String, Integer> headerMap = Collections.singletonMap("col", 0);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        
        CSVRecord consistent = new CSVRecord(parser, new String[]{"val"}, null, 0, 0, 0);
        assertTrue(consistent.isConsistent());
        
        CSVRecord inconsistent = new CSVRecord(parser, new String[0], null, 0, 0, 0);
        assertFalse(inconsistent.isConsistent());
        
        CSVRecord noHeader = new CSVRecord(null, new String[]{"val"}, null, 0, 0, 0);
        assertTrue(noHeader.isConsistent());
    }

    @Test
    void testIsMapped() throws IOException {
        Map<String, Integer> headerMap = Collections.singletonMap("col", 0);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        CSVRecord record = new CSVRecord(parser, new String[0], null, 0, 0, 0);
        
        assertTrue(record.isMapped("col"));
        assertFalse(record.isMapped("invalid"));
        
        CSVRecord noHeader = new CSVRecord(null, new String[0], null, 0, 0, 0);
        assertFalse(noHeader.isMapped("any"));
    }

    @Test
    void testIsSetByIndex() {
        String[] values = {"val"};
        CSVRecord record = new CSVRecord(null, values, null, 0, 0, 0);
        assertTrue(record.isSet(0));
        assertFalse(record.isSet(-1));
        assertFalse(record.isSet(1));
        assertFalse(record.isSet(2));
    }

    @Test
    void testIsSetByName() throws IOException {
        Map<String, Integer> headerMap = Collections.singletonMap("col", 0);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        CSVRecord record = new CSVRecord(parser, new String[]{"val"}, null, 0, 0, 0);
        assertTrue(record.isSet("col"));
        
//        headerMap.put("invalid", 1);
        CSVParser parser2 = createParserWithHeaderMap(headerMap);
        CSVRecord record2 = new CSVRecord(parser2, new String[]{"val"}, null, 0, 0, 0);
        assertFalse(record2.isSet("invalid"));
    }

    @Test
    void testIterator() {
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(null, values, null, 0, 0, 0);
        Iterator<String> it = record.iterator();
        assertEquals("val1", it.next());
        assertEquals("val2", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void testPutIn() throws IOException {
        Map<String, Integer> headerMap = new HashMap<>();
        headerMap.put("col1", 0);
        headerMap.put("col2", 1);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(parser, values, null, 0, 0, 0);
        
        Map<String, String> map = new HashMap<>();
        Map<String, String> result = record.putIn(map);
        
        assertEquals("val1", result.get("col1"));
        assertEquals("val2", result.get("col2"));
        assertSame(map, result);
    }

    @Test
    void testPutInNoHeader() {
        CSVRecord record = new CSVRecord(null, new String[]{"val"}, null, 0, 0, 0);
        Map<String, String> map = new HashMap<>();
        Map<String, String> result = record.putIn(map);
        assertTrue(result.isEmpty());
    }

    @Test
    void testSize() {
        assertEquals(0, new CSVRecord(null, new String[0], null, 0, 0, 0).size());
        assertEquals(1, new CSVRecord(null, new String[]{"val"}, null, 0, 0, 0).size());
        assertEquals(2, new CSVRecord(null, new String[]{"v1", "v2"}, null, 0, 0, 0).size());
    }

    @Test
    void testStream() {
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(null, values, null, 0, 0, 0);
        Stream<String> stream = record.stream();
        assertEquals(2, stream.count());
    }

    @Test
    void testToList() {
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(null, values, null, 0, 0, 0);
        List<String> list = record.toList();
        assertEquals(2, list.size());
        assertEquals("val1", list.get(0));
        assertEquals("val2", list.get(1));
    }

    @Test
    void testToMap() throws IOException {
        Map<String, Integer> headerMap = new HashMap<>();
        headerMap.put("col1", 0);
        headerMap.put("col2", 1);
        CSVParser parser = createParserWithHeaderMap(headerMap);
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(parser, values, null, 0, 0, 0);
        
        Map<String, String> map = record.toMap();
        assertEquals(2, map.size());
        assertEquals("val1", map.get("col1"));
        assertEquals("val2", map.get("col2"));
    }

    @Test
    void testToString() {
        CSVRecord record = new CSVRecord(null, new String[]{"val"}, "cmt", 1, 0, 0);
        assertTrue(record.toString().contains("comment='cmt'"));
        assertTrue(record.toString().contains("recordNumber=1"));
        assertTrue(record.toString().contains("values=[val]"));
    }

    @Test
    void testValues() {
        String[] values = {"val1", "val2"};
        CSVRecord record = new CSVRecord(null, values, null, 0, 0, 0);
        assertArrayEquals(values, record.values());
    }

    private CSVParser createParserWithHeaderMap(Map<String, Integer> headerMap) throws IOException {
        if (headerMap == null || headerMap.isEmpty()) {
            return CSVParser.parse("", CSVFormat.DEFAULT.withHeader((String[]) null).withSkipHeaderRecord(false));
        }
        int maxIndex = headerMap.values().stream().max(Integer::compare).get();
        String[] headers = new String[maxIndex + 1];
        for (int i = 0; i <= maxIndex; i++) {
            headers[i] = "col" + i;
        }
        for (Map.Entry<String, Integer> entry : headerMap.entrySet()) {
            headers[entry.getValue()] = entry.getKey();
        }
        CSVFormat format = CSVFormat.DEFAULT.withHeader(headers).withSkipHeaderRecord(false);
        return CSVParser.parse("", format);
    }
}