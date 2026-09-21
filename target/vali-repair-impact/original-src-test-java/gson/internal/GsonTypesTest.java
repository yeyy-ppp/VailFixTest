package gson.internal;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class GsonTypesTest {

    @Test
    void testPrivateConstructor() throws Exception {
        Constructor<GsonTypes> constructor = GsonTypes.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        assertThrows(InvocationTargetException.class, constructor::newInstance);
    }

    @Test
    void testNewParameterizedTypeWithOwner() {
        Type ownerType = String.class;
        Class<?> rawType = List.class;
        Type[] typeArgs = {String.class};
        
        ParameterizedType pt = GsonTypes.newParameterizedTypeWithOwner(ownerType, rawType, typeArgs);
        assertSame(ownerType, pt.getOwnerType());
        assertEquals(rawType, pt.getRawType());
        assertArrayEquals(typeArgs, pt.getActualTypeArguments());
        
        assertThrows(IllegalArgumentException.class, () -> 
            GsonTypes.newParameterizedTypeWithOwner(null, InnerClass.class, new Type[0])
        );
    }

    class InnerClass {}

    @Test
    void testArrayOf() {
        Type componentType = String.class;
        GenericArrayType arrayType = GsonTypes.arrayOf(componentType);
        assertSame(componentType, arrayType.getGenericComponentType());
    }

    @Test
    void testSubtypeOf() {
        Type bound = Number.class;
        WildcardType wildcard = GsonTypes.subtypeOf(bound);
        assertArrayEquals(new Type[]{bound}, wildcard.getUpperBounds());
        assertArrayEquals(new Type[0], wildcard.getLowerBounds());

        WildcardType nested = GsonTypes.subtypeOf(wildcard);
        assertArrayEquals(new Type[]{bound}, nested.getUpperBounds());
    }

    @Test
    void testSupertypeOf() {
        Type bound = Number.class;
        WildcardType wildcard = GsonTypes.supertypeOf(bound);
        assertArrayEquals(new Type[]{Object.class}, wildcard.getUpperBounds());
        assertArrayEquals(new Type[]{bound}, wildcard.getLowerBounds());

        WildcardType nested = GsonTypes.supertypeOf(wildcard);
        assertArrayEquals(new Type[]{bound}, nested.getLowerBounds());
    }

    @Test
    void testCanonicalize() {
        Type orig = String.class;
        assertSame(orig, GsonTypes.canonicalize(orig));

        Type paramType = GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class);
        assertNotSame(paramType, GsonTypes.canonicalize(paramType));

        Type arrayType = GsonTypes.arrayOf(String.class);
        assertNotSame(arrayType, GsonTypes.canonicalize(arrayType));
    }

    @Test
    void testGetRawType() {
        assertEquals(String.class, GsonTypes.getRawType(String.class));
        
        ParameterizedType pt = GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class);
        assertEquals(List.class, GsonTypes.getRawType(pt));
        
        Type arrayType = GsonTypes.arrayOf(String.class);
        assertEquals(String[].class, GsonTypes.getRawType(arrayType));
        
        WildcardType wildcard = GsonTypes.subtypeOf(Number.class);
        assertEquals(Number.class, GsonTypes.getRawType(wildcard));
        
        class TypeVarHolder<T> {
            TypeVariable<?> getTypeVar() {
                return getClass().getTypeParameters()[0];
            }
        }
        TypeVariable<?> tv = new TypeVarHolder<String>().getTypeVar();
        assertEquals(Object.class, GsonTypes.getRawType(tv));
        
        assertThrows(IllegalArgumentException.class, () -> GsonTypes.getRawType(null));
    }

    @Test
    void testEquals() {
        Type stringType = String.class;
        assertTrue(GsonTypes.equals(stringType, stringType));
        
        assertTrue(GsonTypes.equals(String.class, String.class));
        assertFalse(GsonTypes.equals(String.class, Integer.class));
        
        ParameterizedType pt1 = GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class);
        ParameterizedType pt2 = GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class);
        ParameterizedType pt3 = GsonTypes.newParameterizedTypeWithOwner(null, List.class, Integer.class);
        assertTrue(GsonTypes.equals(pt1, pt2));
        assertFalse(GsonTypes.equals(pt1, pt3));
        
        Type array1 = GsonTypes.arrayOf(String.class);
        Type array2 = GsonTypes.arrayOf(String.class);
        Type array3 = GsonTypes.arrayOf(Integer.class);
        assertTrue(GsonTypes.equals(array1, array2));
        assertFalse(GsonTypes.equals(array1, array3));
        
        WildcardType w1 = GsonTypes.subtypeOf(Number.class);
        WildcardType w2 = GsonTypes.subtypeOf(Number.class);
        WildcardType w3 = GsonTypes.supertypeOf(Number.class);
        assertTrue(GsonTypes.equals(w1, w2));
        assertFalse(GsonTypes.equals(w1, w3));
        
        assertFalse(GsonTypes.equals(pt1, array1));
    }

    @Test
    void testTypeToString() {
        assertEquals("java.lang.String", GsonTypes.typeToString(String.class));
        
        ParameterizedType pt = GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class);
        assertEquals("java.util.List<java.lang.String>", GsonTypes.typeToString(pt));
    }

    @Test
    void testGetSupertype() {
        Type supertype = GsonTypes.getSupertype(ArrayList.class, ArrayList.class, Collection.class);
        assertEquals(Collection.class, ((ParameterizedType) supertype).getRawType());
        
        assertThrows(IllegalArgumentException.class, () -> 
            GsonTypes.getSupertype(String.class, String.class, List.class)
        );
    }

    @Test
    void testGetArrayComponentType() {
        assertEquals(String.class, GsonTypes.getArrayComponentType(String[].class));
        
        GenericArrayType arrayType = GsonTypes.arrayOf(String.class);
        assertEquals(String.class, GsonTypes.getArrayComponentType(arrayType));
    }

    @Test
    void testGetCollectionElementType() {
        Type listType = GsonTypes.newParameterizedTypeWithOwner(null, List.class, String.class);
        assertEquals(String.class, GsonTypes.getCollectionElementType(listType, List.class));
        
        assertEquals(Object.class, GsonTypes.getCollectionElementType(Collection.class, Collection.class));
    }

    @Test
    void testGetMapKeyAndValueTypes() {
        Type mapType = GsonTypes.newParameterizedTypeWithOwner(null, Map.class, String.class, Integer.class);
        Type[] kvTypes = GsonTypes.getMapKeyAndValueTypes(mapType, Map.class);
        assertArrayEquals(new Type[]{String.class, Integer.class}, kvTypes);
        
        assertArrayEquals(new Type[]{String.class, String.class}, 
            GsonTypes.getMapKeyAndValueTypes(Properties.class, Properties.class));
        
        assertArrayEquals(new Type[]{Object.class, Object.class}, 
            GsonTypes.getMapKeyAndValueTypes(Map.class, Map.class));
    }

    static class Holder<T> {
        List<T> list;
    }

    @Test
    void testResolve() {
        TypeVariable<?> tv = Holder.class.getTypeParameters()[0];
        
        Type resolved = GsonTypes.resolve(
            GsonTypes.newParameterizedTypeWithOwner(null, Holder.class, String.class), 
            Holder.class, 
            tv
        );
        assertEquals(String.class, resolved);
    }

 /*   @Test
    void testIndexOf() throws Exception {
        String[] arr = {"a", "b", "c"};
        assertEquals(1, (Integer) TestHelper.invokePrivateStatic(GsonTypes.class, "indexOf", new Object[]{arr, "b"}));
        assertThrows(NoSuchElementException.class, () -> 
            TestHelper.invokePrivateStatic(GsonTypes.class, "indexOf", new Object[]{arr, "d"})
        );
    }*/

/*    @Test
    void testDeclaringClassOf() throws Exception {
        class Holder<T> {}
        TypeVariable<?> tv = Holder.class.getTypeParameters()[0];
        Class<?> declaringClass = (Class<?>) TestHelper.invokePrivateStatic(GsonTypes.class, "declaringClassOf", tv);
        assertEquals(Holder.class, declaringClass);
    }*/

  /*  @Test
    void testCheckNotPrimitive() throws Exception {
        TestHelper.invokePrivateStatic(GsonTypes.class, "checkNotPrimitive", String.class);
        assertThrows(IllegalArgumentException.class, () -> 
            TestHelper.invokePrivateStatic(GsonTypes.class, "checkNotPrimitive", int.class)
        );
    }
*/
    @Test
    void testRequiresOwnerType() {
        assertTrue(GsonTypes.requiresOwnerType(InnerClass.class));
        assertFalse(GsonTypes.requiresOwnerType(String.class));
    }

    static class TestHelper {
        static Object invokePrivateStatic(Class<?> clazz, String methodName, Object arg) throws Exception {
            Method method = clazz.getDeclaredMethod(methodName, arg.getClass());
            method.setAccessible(true);
            try {
                return method.invoke(null, arg);
            } catch (InvocationTargetException e) {
                throw (Exception) e.getCause();
            }
        }

        static Object invokePrivateStatic(Class<?> clazz, String methodName, Object[] args) throws Exception {
            Class<?>[] paramTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                paramTypes[i] = args[i].getClass();
            }
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            try {
                return method.invoke(null, args);
            } catch (InvocationTargetException e) {
                throw (Exception) e.getCause();
            }
        }
    }
}