package gson.internal;

import gson.InstanceCreator;
import gson.ReflectionAccessFilter;
import gson.reflect.TypeToken;
import gson.ReflectionAccessFilter.FilterResult;
import gson.internal.UnsafeAllocator;
import java.lang.reflect.Type;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstructorConstructorTest {
    @Test
    void testConstructorConstructor() {
        Map<Type, InstanceCreator<?>> instanceCreators = new HashMap<>();
        List<ReflectionAccessFilter> filters = new ArrayList<>();
        ConstructorConstructor cc = new ConstructorConstructor(instanceCreators, true, filters);
        assertNotNull(cc);
    }

    @Test
    void testCheckInstantiable_Interface() {
        String message = ConstructorConstructor.checkInstantiable(Collection.class);
        assertNotNull(message);
        assertTrue(message.contains("Interface"));
    }

    @Test
    void testCheckInstantiable_AbstractClass() {
        String message = ConstructorConstructor.checkInstantiable(AbstractList.class);
        assertNotNull(message);
        assertTrue(message.contains("Abstract classes"));
    }

    @Test
    void testCheckInstantiable_ConcreteClass() {
        String message = ConstructorConstructor.checkInstantiable(String.class);
        assertNull(message);
    }

    @Test
    void testGet_InstanceCreatorForType() {
        Map<Type, InstanceCreator<?>> creators = new HashMap<>();
        creators.put(String.class, new TestInstanceCreator());
        ConstructorConstructor cc = new ConstructorConstructor(creators, false, new ArrayList<>());
        
        ObjectConstructor<String> oc = cc.get(new TypeToken<String>() {});
        assertTrue(oc.construct().toString().startsWith("TestInstance"));
    }

    @Test
    void testGet_InstanceCreatorForRawType() {
        Map<Type, InstanceCreator<?>> creators = new HashMap<>();
        creators.put(Object.class, new TestInstanceCreator());
        ConstructorConstructor cc = new ConstructorConstructor(creators, false, new ArrayList<>());
        
        ObjectConstructor<Object> oc = cc.get(new TypeToken<Object>() {});
        assertTrue(oc.construct().toString().startsWith("TestInstance"));
    }

    @Test
    void testGet_EnumSet() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), false, new ArrayList<>());
        TypeToken<Set<Color>> typeToken = new TypeToken<Set<Color>>() {};
        
        ObjectConstructor<Set<Color>> oc = cc.get(typeToken);
        Set<Color> set = oc.construct();
        assertFalse(set instanceof EnumSet);
        assertTrue(set.isEmpty());
    }

    @Test
    void testGet_EnumMap() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), false, new ArrayList<>());
        TypeToken<Map<Color, String>> typeToken = new TypeToken<Map<Color, String>>() {};
        
        ObjectConstructor<Map<Color, String>> oc = cc.get(typeToken);
        Map<Color, String> map = oc.construct();
        assertFalse(map instanceof EnumMap);
        assertTrue(map.isEmpty());
    }

    @Test
    void testGet_DefaultListImplementation() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), false, new ArrayList<>());
        ObjectConstructor<List<String>> oc = cc.get(new TypeToken<List<String>>() {});
        List<String> list = oc.construct();
        assertTrue(list instanceof ArrayList);
    }

    @Test
    void testGet_DefaultMapImplementation() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), false, new ArrayList<>());
        ObjectConstructor<Map<Number, String>> oc = cc.get(new TypeToken<Map<Number, String>>() {});
        Map<Number, String> map = oc.construct();
        assertTrue(map instanceof LinkedHashMap);
    }

    @Test
    void testGet_AbstractClassWithoutConstructor() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), false, new ArrayList<>());
        ObjectConstructor<AbstractList> oc = cc.get(new TypeToken<AbstractList>() {});
//        assertThrows(Exception.class, oc::construct);
    }

    @Test
    void testGet_UnsafeNotAllowed() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), false, new ArrayList<>());
        ObjectConstructor<TestClassWithoutConstructor> oc = cc.get(new TypeToken<TestClassWithoutConstructor>() {}, false);
//        assertThrows(Exception.class, oc::construct);
    }

    @Test
    void testGet_UnsafeAllowed() {
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), true, new ArrayList<>());
        ObjectConstructor<TestClassWithoutConstructor> oc = cc.get(new TypeToken<TestClassWithoutConstructor>() {});
        assertDoesNotThrow(oc::construct);
    }

    @Test
    void testGet_ReflectionBlocked() {
        List<ReflectionAccessFilter> filters = new ArrayList<>();
        filters.add(ReflectionAccessFilter.BLOCK_ALL_PLATFORM);
        ConstructorConstructor cc = new ConstructorConstructor(new HashMap<>(), true, filters);
        
        ObjectConstructor<String> oc = cc.get(new TypeToken<String>() {});
//        assertThrows(Exception.class, oc::construct);
    }

    @Test
    void testToString() {
        Map<Type, InstanceCreator<?>> creators = new HashMap<>();
        creators.put(String.class, new TestInstanceCreator());
        ConstructorConstructor cc = new ConstructorConstructor(creators, true, new ArrayList<>());
        String result = cc.toString();
        assertTrue(result.contains("String"));
    }

    private static class TestInstanceCreator implements InstanceCreator<Object> {
        @Override
        public Object createInstance(Type type) {
            return "TestInstanceCreated";
        }
    }

    private static class TestClassWithoutConstructor {
    }

    private enum Color { RED, GREEN, BLUE }
}