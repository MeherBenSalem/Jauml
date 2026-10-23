package tn.naizo.jauml.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionRangeTest {

    @Test
    public void testAnyVersion() {
        assertTrue(VersionRange.parse("*").contains("1.0"));
        assertTrue(VersionRange.parse("").contains("99"));
        assertTrue(VersionRange.parse(null).contains("0.1"));
        assertFalse(VersionRange.parse("*").contains("not-a-version"));
    }

    @Test
    public void testMavenIntervals() {
        VersionRange range = VersionRange.parse("[2.1,3)");
        assertTrue(range.contains("2.1"));
        assertTrue(range.contains("2.9.9"));
        assertFalse(range.contains("3.0"));
        assertFalse(range.contains("2.0.9"));

        assertTrue(VersionRange.parse("[1.20.1]").contains("1.20.1"));
        assertFalse(VersionRange.parse("[1.20.1]").contains("1.20.2"));

        VersionRange open = VersionRange.parse("(,26.2]");
        assertTrue(open.contains("1.20.1"));
        assertTrue(open.contains("26.2"));
        assertFalse(open.contains("26.3"));

        VersionRange minecraft = VersionRange.parse("[26.3,26.4)");
        assertTrue(minecraft.contains("26.3"));
        assertTrue(minecraft.contains("26.3.1"));
        assertFalse(minecraft.contains("26.4"));

        VersionRange union = VersionRange.parse("[1.20.1],[26.2,)");
        assertTrue(union.contains("1.20.1"));
        assertFalse(union.contains("1.21.1"));
        assertTrue(union.contains("26.3"));
    }

    @Test
    public void testComparators() {
        VersionRange range = VersionRange.parse(">=2.1 <3");
        assertTrue(range.contains("2.1.0"));
        assertTrue(range.contains("2.99"));
        assertFalse(range.contains("3.0.0"));
        assertFalse(range.contains("2.0"));

        assertTrue(VersionRange.parse(">1.20").contains("1.20.1"));
        assertFalse(VersionRange.parse(">1.20").contains("1.20"));
        assertTrue(VersionRange.parse("<=26.3").contains("26.3"));
        assertTrue(VersionRange.parse("=2.3.1").contains("2.3.1"));
        assertTrue(VersionRange.parse("2.3.1").contains("2.3.1"));
        assertFalse(VersionRange.parse("2.3.1").contains("2.3.2"));
    }

    @Test
    public void testTildeCaretAndWildcards() {
        assertTrue(VersionRange.parse("~2.3").contains("2.3.9"));
        assertFalse(VersionRange.parse("~2.3").contains("2.4.0"));
        assertTrue(VersionRange.parse("^2.3").contains("2.9"));
        assertFalse(VersionRange.parse("^2.3").contains("3.0"));
        assertFalse(VersionRange.parse("^2.3").contains("2.2"));

        assertTrue(VersionRange.parse("2.x").contains("2.7.1"));
        assertFalse(VersionRange.parse("2.x").contains("3.0"));
        assertTrue(VersionRange.parse("1.21.*").contains("1.21.1"));
        assertFalse(VersionRange.parse("1.21.*").contains("1.22"));
    }

    @Test
    public void testAlternatives() {
        VersionRange range = VersionRange.parse("[1.20.1] || >=26.2");
        assertTrue(range.contains("1.20.1"));
        assertFalse(range.contains("1.21.1"));
        assertTrue(range.contains("26.3"));
    }

    @Test
    public void testMalformedRanges() {
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("[2.1,3"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(">=banana"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("(2.1)"));
        assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("1.0 ||"));
    }
}
