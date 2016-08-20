package io.advantageous.discovery.impl;

import io.advantageous.discovery.utils.UriUtils;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class UriUtilsTest {

    @Test(expected = InvocationTargetException.class)
    public void enforceConstructionException() throws Exception {
        Constructor constructor = UriUtils.class.getDeclaredConstructor();
        Assert.assertNotNull(constructor);
        Assert.assertFalse(constructor.isAccessible());
        constructor.setAccessible(true);
        constructor.newInstance();
    }

    @Test
    public void testNoPublicConstructor() throws Exception {
        Constructor[] constructors = UriUtils.class.getConstructors();
        Assert.assertEquals(0, constructors.length);
    }

    @Test
    public void testSplitQueryByAmpersand() throws Exception {
        Map<String, String> map = UriUtils.splitQuery("foo=bar&baz=gak");
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertEquals("gak", map.get("baz"));
    }

    @Test
    public void testSplitQueryBySemicolon() throws Exception {
        Map<String, String> map = UriUtils.splitQuery("foo=bar;baz=gak");
        Assert.assertNotNull(map);
        Assert.assertEquals(2, map.size());
        Assert.assertEquals("gak", map.get("baz"));
    }

}
