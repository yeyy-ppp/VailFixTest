package gson.reflect;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;

import org.junit.jupiter.api.Test;

class TypeTokenTest {

    @Test
    void testTypeEqualsWithMap() throws Exception {
        TypeVariable<Class<Map>>[] variables = Map.class.getTypeParameters();
        Map<String, Type> typeMap = new HashMap<>();
        typeMap.put(variables[0].getName(), String.class);
        typeMap.put(variables[1].getName(), Number.class);

        ParameterizedType fromType = (ParameterizedType) new TypeToken<Map<String, Number>>() {}.getType();
        
        ParameterizedType toType = new ParameterizedType() {
            @Override
            public Type[] getActualTypeArguments() {
                return new Type[] { variables[0], variables[1] };
            }
            @Override
            public Type getRawType() {
                return Map.class;
            }
            @Override
            public Type getOwnerType() {
                return null;
            }
        };

        Method matchesMethod = TypeToken.class
            .getDeclaredMethod("matches", Type.class, Type.class, Map.class);
        matchesMethod.setAccessible(true);
        boolean result = (boolean) matchesMethod.invoke(null, toType.getActualTypeArguments()[0], fromType.getActualTypeArguments()[0], typeMap);
        assertTrue(result);
    }

    @Test
    void testIsCapturingTypeVariablesForbidden() throws Exception {
        Method method = TypeToken.class.getDeclaredMethod("isCapturingTypeVariablesForbidden");
        method.setAccessible(true);
        boolean result = (boolean) method.invoke(null);
        assertTrue(result);
    }

    @Test
    void testGetTypeTokenTypeArgument() throws Exception {
        TypeToken<String> token = new TypeToken<String>() {};
        Method method = TypeToken.class.getDeclaredMethod("getTypeTokenTypeArgument");
        method.setAccessible(true);
        Type type = (Type) method.invoke(token);
        assertEquals(String.class, type);
    }

    @Test
    void testVerifyNoTypeVariable() throws Exception {
        Method method = TypeToken.class.getDeclaredMethod("verifyNoTypeVariable", Type.class);
        method.setAccessible(true);
        method.invoke(null, String.class);
    }

    @Test
    void testBuildUnsupportedTypeException() throws Exception {
        Method method = TypeToken.class.getDeclaredMethod("buildUnsupportedTypeException", Type.class, Class[].class);
        method.setAccessible(true);
        IllegalArgumentException ex = (IllegalArgumentException) method.invoke(null, Integer.class, new Class[]{String.class, Number.class});
        assertFalse(ex.getMessage().contains("Integer") && ex.getMessage().contains("String, Number"));
    }

    @Test
    void testMatches() throws Exception {
        Method method = TypeToken.class.getDeclaredMethod("matches", Type.class, Type.class, Map.class);
        method.setAccessible(true);
        Map<String, Type> typeMap = new HashMap<>();
        boolean result = (boolean) method.invoke(null, String.class, String.class, typeMap);
        assertTrue(result);
    }
}