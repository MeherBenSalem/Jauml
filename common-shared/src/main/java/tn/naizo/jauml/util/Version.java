package tn.naizo.jauml.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A lenient, comparable version number covering the shapes Minecraft modding uses:
 * semantic versions ({@code 3.0.0}, {@code 2.1.0-beta.2}), Minecraft versions ({@code 1.20.1}, {@code 26.3}),
 * and loader builds ({@code 26.3.0.95-beta}, {@code 0.164.0+26.3}).
 * <p>
 * Numeric components compare numerically and missing components count as zero, so {@code 2.3} equals {@code 2.3.0}.
 * A pre-release ({@code -beta}) sorts before its release. Build metadata after {@code +} is kept but ignored when comparing.
 *
 * @since 3.0.0
 */
public final class Version implements Comparable<Version> {

    private final String raw;
    private final int[] components;
    private final String preRelease;
    private final String build;

    private Version(String raw, int[] components, String preRelease, String build) {
        this.raw = raw;
        this.components = components;
        this.preRelease = preRelease;
        this.build = build;
    }

    /**
     * Parses a version string.
     *
     * @throws IllegalArgumentException if the string has no leading numeric component
     */
    public static Version parse(String version) {
        if (version == null) {
            throw new IllegalArgumentException("Version cannot be null");
        }
        String raw = version.trim();
        String text = raw.startsWith("v") || raw.startsWith("V") ? raw.substring(1) : raw;

        String build = "";
        int plus = text.indexOf('+');
        if (plus >= 0) {
            build = text.substring(plus + 1);
            text = text.substring(0, plus);
        }
        String preRelease = "";
        int dash = text.indexOf('-');
        if (dash >= 0) {
            preRelease = text.substring(dash + 1);
            text = text.substring(0, dash);
        }

        String[] parts = text.split("\\.", -1);
        List<Integer> numbers = new ArrayList<>(parts.length);
        for (String part : parts) {
            if (part.isEmpty() || !part.chars().allMatch(Character::isDigit)) {
                break;
            }
            try {
                numbers.add(Integer.parseInt(part));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Version component too large in '" + version + "'", e);
            }
        }
        if (numbers.isEmpty()) {
            throw new IllegalArgumentException("Not a version: '" + version + "'");
        }
        int[] components = new int[numbers.size()];
        for (int i = 0; i < components.length; i++) {
            components[i] = numbers.get(i);
        }
        return new Version(raw, components, preRelease, build);
    }

    /**
     * Parses a version string, returning {@code null} instead of throwing when it is not a version.
     */
    public static Version tryParse(String version) {
        try {
            return parse(version);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int major() {
        return component(0);
    }

    public int minor() {
        return component(1);
    }

    public int patch() {
        return component(2);
    }

    /**
     * Returns the numeric component at the given index, or zero when the version has fewer components.
     */
    public int component(int index) {
        return index < components.length ? components[index] : 0;
    }

    /**
     * Returns how many numeric components were written ({@code 26.3} has two, {@code 1.20.1} has three).
     */
    public int componentCount() {
        return components.length;
    }

    public List<Integer> components() {
        List<Integer> list = new ArrayList<>(components.length);
        for (int component : components) {
            list.add(component);
        }
        return Collections.unmodifiableList(list);
    }

    public String preRelease() {
        return preRelease;
    }

    public boolean isPreRelease() {
        return !preRelease.isEmpty();
    }

    public String build() {
        return build;
    }

    public boolean isAtLeast(String other) {
        return compareTo(parse(other)) >= 0;
    }

    public boolean isOlderThan(String other) {
        return compareTo(parse(other)) < 0;
    }

    /**
     * Checks this version against a range; see {@link VersionRange#parse(String)} for the accepted syntax.
     */
    public boolean satisfies(String range) {
        return VersionRange.parse(range).contains(this);
    }

    @Override
    public int compareTo(Version other) {
        int length = Math.max(components.length, other.components.length);
        for (int i = 0; i < length; i++) {
            int cmp = Integer.compare(component(i), other.component(i));
            if (cmp != 0) {
                return cmp;
            }
        }
        return comparePreRelease(preRelease, other.preRelease);
    }

    private static int comparePreRelease(String a, String b) {
        if (a.equals(b)) {
            return 0;
        }
        if (a.isEmpty()) {
            return 1;
        }
        if (b.isEmpty()) {
            return -1;
        }
        String[] left = a.split("\\.");
        String[] right = b.split("\\.");
        int length = Math.min(left.length, right.length);
        for (int i = 0; i < length; i++) {
            String l = left[i];
            String r = right[i];
            boolean lNum = !l.isEmpty() && l.chars().allMatch(Character::isDigit);
            boolean rNum = !r.isEmpty() && r.chars().allMatch(Character::isDigit);
            int cmp;
            if (lNum && rNum) {
                cmp = new BigInteger(l).compareTo(new BigInteger(r));
            } else if (lNum) {
                cmp = -1;
            } else if (rNum) {
                cmp = 1;
            } else {
                cmp = l.compareTo(r);
            }
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(left.length, right.length);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Version)) {
            return false;
        }
        return compareTo((Version) o) == 0;
    }

    @Override
    public int hashCode() {
        int length = components.length;
        while (length > 0 && components[length - 1] == 0) {
            length--;
        }
        // Pre-release identifiers can be equal without being identical text ("01" vs "1"), so only components hash.
        int hash = 1;
        for (int i = 0; i < length; i++) {
            hash = 31 * hash + components[i];
        }
        return hash;
    }

    /**
     * Returns the string this version was parsed from.
     */
    @Override
    public String toString() {
        return raw;
    }
}
