package net.sergeych.tools;

import net.sergeych.biserializer.DefaultBiMapper;
import org.junit.Test;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.Assert.*;

/**
 * Created by sergeych on 24.12.16.
 */
public class BinderTest {
    @Test
    public void isFrozen() throws Exception {
        Binder b = new Binder();
        b.put("hello", "world");
        Binder x = b.getOrCreateBinder("inner");
        x.put("foo", "bar");
        assertFalse(b.isFrozen());
        assertFalse(b.getBinder("inner").isFrozen());

        b.freeze();
        assertTrue(b.isFrozen());
        assertTrue(b.getBinder("inner").isFrozen());
        assertEquals("bar", b.getBinder("inner").get("foo"));

        Binder e = Binder.EMPTY;
        assertTrue(e.isFrozen());
        x = new Binder();
        assertEquals(e, x);
        assertFalse(b.equals(e));
    }

    @Test
    public void getInt() throws Exception {
        Binder b = Binder.fromKeysValues(
                "i1", 100,
                "i2", "101",
                "l1", "1505774997427",
                "l2", 1505774997427L
        );
        assertEquals(100, (int) b.getInt("i1",222));
        assertEquals(101, (int) b.getInt("i2",222));
        assertEquals(100, b.getIntOrThrow("i1"));
        assertEquals(101, b.getIntOrThrow("i2"));
        assertEquals(1505774997427L, b.getLongOrThrow("l1"));
        assertEquals(1505774997427L, b.getLongOrThrow("l2"));
    }

    @Test
    public void timeIssues() throws Exception {
        Binder x = Binder.fromKeysValues(
                "tt", ZonedDateTime.now()
        );
        x = (Binder) DefaultBiMapper.serialize(x);
//        System.out.println(x);
        x.getBinderOrThrow("tt").put("seconds", 0);
        DefaultBiMapper.deserializeInPlace(x);
        ZonedDateTime t = x.getZonedDateTimeOrThrow("tt");
        assertEquals( "1970-01-01T00:00Z", t.withZoneSameInstant(ZoneOffset.UTC).toString());
//        System.out.println(""+Boss.serializeToBinder(x));
    }
}