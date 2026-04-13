package gson.internal.sql;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import gson.internal.sql.SqlTypesSupport;

public class SqlTypesSupportTest {

    @Test
    void testPrivateConstructorIsPrivate() throws NoSuchMethodException {
        Constructor<SqlTypesSupport> constructor = SqlTypesSupport.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
    }

    @Test
    void testSqlTypesSupportInitialization() {
        if (SqlTypesSupport.SUPPORTS_SQL_TYPES) {
            assertNotNull(SqlTypesSupport.DATE_FACTORY);
            assertNotNull(SqlTypesSupport.TIME_FACTORY);
            assertNotNull(SqlTypesSupport.TIMESTAMP_FACTORY);
            assertEquals(3, SqlTypesSupport.SQL_TYPE_FACTORIES.size());
        } else {
            assertNull(SqlTypesSupport.DATE_FACTORY);
            assertNull(SqlTypesSupport.TIME_FACTORY);
            assertNull(SqlTypesSupport.TIMESTAMP_FACTORY);
            assertTrue(SqlTypesSupport.SQL_TYPE_FACTORIES.isEmpty());
        }
    }

}