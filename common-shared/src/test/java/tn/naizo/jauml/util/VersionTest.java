package tn.naizo.jauml.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class VersionTest {

    @Test
    public void testParsesModdingVersionShapes() {
        Version semver = Version.parse("3.0.0");
        assertEquals(3, semver.major());
        assertEquals(0, semver.minor());
        assertEquals(0, semver.patch());

        Version minecraft = Version.parse("26.3");
        assertEquals(26, minecraft.major());
        assertEquals(3, minecraft.minor());
        assertEquals(0, minecraft.patch());
        assertEquals(2, minecraft.componentCount());

        Version neoforge = Version.parse("26.3.0.95-beta");
        assertEquals(95, neoforge.component(3));
        assertEquals("beta", neoforge.preRelease());
        assertTrue(neoforge.isPreRelease());

        Version fabricApi = Version.parse("0.165.0+26.3");
        assertEquals(165, fabricApi.minor());
        assertEquals("26.3", fabricApi.build());
        assertEquals("0.165.0+26.3", fabricApi.toString());

        assertEquals(2, Version.parse("v2.1").major());
    }

    @Test
    public void testRejectsNonVersions() {
        assertThrows(IllegalArgumentException.class, () -> Version.parse("beta"));
        assertThrows(IllegalArgumentException.class, () -> Version.parse(""));
        assertThrows(IllegalArgumentException.class, () -> Version.parse(null));
        assertNull(Version.tryParse("not-a-version"));
    }

    @Test
    public void testOrdering() {
        assertTrue(Version.parse("1.20.1").compareTo(Version.parse("1.21.1")) < 0);
        assertTrue(Version.parse("1.21.1").compareTo(Version.parse("26.2")) < 0);
        assertTrue(Version.parse("2.10.0").compareTo(Version.parse("2.9.9")) > 0);
        assertEquals(0, Version.parse("2.3").compareTo(Version.parse("2.3.0")));
        assertEquals(Version.parse("2.3"), Version.parse("2.3.0"));
        assertEquals(Version.parse("2.3").hashCode(), Version.parse("2.3.0").hashCode());

        // A pre-release sorts before its release; numeric identifiers compare numerically.
        assertTrue(Version.parse("3.0.0-beta").compareTo(Version.parse("3.0.0")) < 0);
        assertTrue(Version.parse("3.0.0-beta.2").compareTo(Version.parse("3.0.0-beta.10")) < 0);
        assertTrue(Version.parse("3.0.0-alpha").compareTo(Version.parse("3.0.0-beta")) < 0);

        // Build metadata does not affect ordering.
        assertEquals(0, Version.parse("0.165.0+26.3").compareTo(Version.parse("0.165.0+26.2")));
    }

    @Test
    public void testConvenienceChecks() {
        Version version = Version.parse("2.3.1");
        assertTrue(version.isAtLeast("2.3"));
        assertTrue(version.isOlderThan("3.0.0"));
        assertTrue(version.satisfies(">=2.1 <3"));
        assertFalse(version.satisfies("[3.0,)"));
    }
}
