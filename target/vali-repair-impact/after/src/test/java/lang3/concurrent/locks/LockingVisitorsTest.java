package lang3.concurrent.locks;

import org.junit.jupiter.api.Test;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import static org.junit.jupiter.api.Assertions.*;

public class LockingVisitorsTest {

    @Test
    void testCreateReadWriteLockVisitor() {
        String object = "test";
        ReadWriteLock lock = new ReentrantReadWriteLock();
        LockingVisitors.ReadWriteLockVisitor<String> visitor = LockingVisitors.create(object, lock);
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testCreateReadWriteLockVisitorNullObject() {
        ReadWriteLock lock = new ReentrantReadWriteLock();
        assertThrows(NullPointerException.class, () -> LockingVisitors.create(null, lock));
    }

    @Test
    void testCreateReadWriteLockVisitorNullLock() {
        String object = "test";
        assertThrows(NullPointerException.class, () -> LockingVisitors.create(object, (ReadWriteLock) null));
    }

    @Test
    void testCreateReentrantLockVisitor() {
        String object = "test";
        ReentrantLock lock = new ReentrantLock();
        LockingVisitors.ReentrantLockVisitor<String> visitor = LockingVisitors.create(object, lock);
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testCreateReentrantLockVisitorNullObject() {
        ReentrantLock lock = new ReentrantLock();
        assertThrows(NullPointerException.class, () -> LockingVisitors.create(null, lock));
    }

    @Test
    void testCreateReentrantLockVisitorNullLock() {
        String object = "test";
        assertThrows(NullPointerException.class, () -> LockingVisitors.create(object, (ReadWriteLock) null));
    }

    @Test
    void testReadWriteLockVisitorBuilder() {
        String object = "test";
        ReadWriteLock lock = new ReentrantReadWriteLock();
        LockingVisitors.ReadWriteLockVisitor.Builder<String> builder = LockingVisitors.ReadWriteLockVisitor.builder();
        builder.setObject(object);
        builder.setLock(lock);
        LockingVisitors.ReadWriteLockVisitor<String> visitor = builder.get();
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testReadWriteLockVisitorBuilderNullObject() {
        LockingVisitors.ReadWriteLockVisitor.Builder<String> builder = LockingVisitors.ReadWriteLockVisitor.builder();
        builder.setLock(new ReentrantReadWriteLock());
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testReadWriteLockVisitorBuilderNullLock() {
        LockingVisitors.ReadWriteLockVisitor.Builder<String> builder = LockingVisitors.ReadWriteLockVisitor.builder();
        builder.setObject("test");
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testReentrantLockVisitorBuilder() {
        String object = "test";
        ReentrantLock lock = new ReentrantLock();
        LockingVisitors.ReentrantLockVisitor.Builder<String> builder = LockingVisitors.ReentrantLockVisitor.builder();
        builder.setObject(object);
        builder.setLock(lock);
        LockingVisitors.ReentrantLockVisitor<String> visitor = builder.get();
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testReentrantLockVisitorBuilderNullObject() {
        LockingVisitors.ReentrantLockVisitor.Builder<String> builder = LockingVisitors.ReentrantLockVisitor.builder();
        builder.setLock(new ReentrantLock());
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testReentrantLockVisitorBuilderNullLock() {
        LockingVisitors.ReentrantLockVisitor.Builder<String> builder = LockingVisitors.ReentrantLockVisitor.builder();
        builder.setObject("test");
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testLockingVisitorsConstructor() {
        new LockingVisitors();
        assertTrue(true);
    }

    @Test
    void testReadWriteLockVisitorDirectConstructor() {
        String object = "test";
        ReadWriteLock lock = new ReentrantReadWriteLock();
        LockingVisitors.ReadWriteLockVisitor<String> visitor = new LockingVisitors.ReadWriteLockVisitor<>(object, lock);
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testReentrantLockVisitorDirectConstructor() {
        String object = "test";
        ReentrantLock lock = new ReentrantLock();
        LockingVisitors.ReentrantLockVisitor<String> visitor = new LockingVisitors.ReentrantLockVisitor<>(object, lock);
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testReadWriteLockVisitorBuilderWithCustomSuppliers() {
        String object = "test";
        ReadWriteLock lock = new ReentrantReadWriteLock();
        Lock customReadLock = new ReentrantLock();
        Lock customWriteLock = new ReentrantLock();

        LockingVisitors.ReadWriteLockVisitor.Builder<String> builder = LockingVisitors.ReadWriteLockVisitor.builder();
        builder.setObject(object);
        builder.setLock(lock);
        builder.setReadLockSupplier(() -> customReadLock);
        builder.setWriteLockSupplier(() -> customWriteLock);

        LockingVisitors.ReadWriteLockVisitor<String> visitor = builder.get();
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testReadWriteLockVisitorBuilderWithNullReadSupplier() {
        LockingVisitors.ReadWriteLockVisitor.Builder<String> builder = LockingVisitors.ReadWriteLockVisitor.builder();
        builder.setObject("test");
        builder.setLock(new ReentrantReadWriteLock());
        builder.setReadLockSupplier(null);
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testReadWriteLockVisitorBuilderWithNullWriteSupplier() {
        LockingVisitors.ReadWriteLockVisitor.Builder<String> builder = LockingVisitors.ReadWriteLockVisitor.builder();
        builder.setObject("test");
        builder.setLock(new ReentrantReadWriteLock());
        builder.setWriteLockSupplier(null);
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testReentrantLockVisitorBuilderWithCustomSuppliers() {
        String object = "test";
        ReentrantLock lock = new ReentrantLock();
        Lock customReadLock = new ReentrantLock();
        Lock customWriteLock = new ReentrantLock();

        LockingVisitors.ReentrantLockVisitor.Builder<String> builder = LockingVisitors.ReentrantLockVisitor.builder();
        builder.setObject(object);
        builder.setLock(lock);
        builder.setReadLockSupplier(() -> customReadLock);
        builder.setWriteLockSupplier(() -> customWriteLock);

        LockingVisitors.ReentrantLockVisitor<String> visitor = builder.get();
        assertSame(object, visitor.getObject());
        assertSame(lock, visitor.getLock());
    }

    @Test
    void testReentrantLockVisitorBuilderWithNullReadSupplier() {
        LockingVisitors.ReentrantLockVisitor.Builder<String> builder = LockingVisitors.ReentrantLockVisitor.builder();
        builder.setObject("test");
        builder.setLock(new ReentrantLock());
        builder.setReadLockSupplier(null);
        assertThrows(NullPointerException.class, builder::get);
    }

    @Test
    void testReentrantLockVisitorBuilderWithNullWriteSupplier() {
        LockingVisitors.ReentrantLockVisitor.Builder<String> builder = LockingVisitors.ReentrantLockVisitor.builder();
        builder.setObject("test");
        builder.setLock(new ReentrantLock());
        builder.setWriteLockSupplier(null);
        assertThrows(NullPointerException.class, builder::get);
    }

}
