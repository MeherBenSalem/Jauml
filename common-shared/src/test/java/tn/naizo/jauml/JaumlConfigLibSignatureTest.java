package tn.naizo.jauml;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pins the JaumlConfigLib methods that released mods (Bosses, better_farming_plus, fine_tuned_weaponry,
 * Ascendant-Mobs, RPG Attribute System) link against, directly or by reflection. Changing a parameter or return
 * type breaks those jars at runtime with NoSuchMethodError, so these signatures must not change in 3.x.
 */
public class JaumlConfigLibSignatureTest {

    private static void assertStatic(Class<?> returnType, String name, Class<?>... parameters) throws Exception {
        Method method = JaumlConfigLib.class.getMethod(name, parameters);
        assertEquals(returnType, method.getReturnType(), name + " return type");
        assertTrue(Modifier.isStatic(method.getModifiers()), name + " must stay static");
        assertTrue(Modifier.isPublic(method.getModifiers()), name + " must stay public");
    }

    @Test
    public void testSignaturesUsedByReleasedMods() throws Exception {
        Class<String> s = String.class;
        assertEquals("tn.naizo.jauml.JaumlConfigLib", JaumlConfigLib.class.getName());

        assertStatic(boolean.class, "createConfigFile", s, s);
        assertStatic(boolean.class, "arrayKeyExists", s, s, s);
        assertStatic(boolean.class, "setNumberValue", s, s, s, double.class);
        assertStatic(boolean.class, "setStringValue", s, s, s, s);
        assertStatic(double.class, "getNumberValue", s, s, s);
        assertStatic(String.class, "getStringValue", s, s, s);
        assertStatic(boolean.class, "getBooleanValue", s, s, s);
        assertStatic(boolean.class, "setBooleanValue", s, s, s, boolean.class);
        assertStatic(boolean.class, "addStringToArray", s, s, s, s);
        assertStatic(boolean.class, "stringExistsInArray", s, s, s, s);
        assertStatic(int.class, "getArrayLength", s, s, s);
        assertStatic(String.class, "getArrayElement", s, s, s, int.class);
        assertStatic(List.class, "getArrayAsList", s, s, s);
        assertStatic(boolean.class, "setIntValue", s, s, s, int.class);
    }
}
