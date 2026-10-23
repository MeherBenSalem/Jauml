package tn.naizo.jauml.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A set of {@link Version}s written the way {@code mods.toml} or {@code fabric.mod.json} write dependency ranges.
 * <p>
 * Accepted syntax:
 * <ul>
 *   <li>{@code *} or an empty string: any version</li>
 *   <li>Maven intervals: {@code [2.1,3)}, {@code (,1.21]}, {@code [26.3]}, several joined by commas</li>
 *   <li>Comparators, space separated and all required: {@code >=2.1 <3}, {@code >1.20}, {@code <=26.3}, {@code =2.3.1}</li>
 *   <li>{@code ~2.3} (same major and minor), {@code ^2.3} (same major), {@code 2.x} or {@code 2.3.*} wildcards</li>
 *   <li>A bare version such as {@code 2.3.1}: exactly that version</li>
 *   <li>Alternatives joined by {@code ||}: {@code [1.20.1] || >=26.2}</li>
 * </ul>
 *
 * @since 3.0.0
 */
public final class VersionRange {

    private static final VersionRange ANY = new VersionRange("*", Collections.singletonList(Interval.UNBOUNDED));

    private final String raw;
    private final List<Interval> intervals;

    private VersionRange(String raw, List<Interval> intervals) {
        this.raw = raw;
        this.intervals = intervals;
    }

    /**
     * Parses a version range.
     *
     * @throws IllegalArgumentException if the range is malformed
     */
    public static VersionRange parse(String range) {
        if (range == null) {
            return ANY;
        }
        String text = range.trim();
        if (text.isEmpty() || text.equals("*")) {
            return ANY;
        }
        List<Interval> intervals = new ArrayList<>();
        for (String alternative : text.split("\\|\\|", -1)) {
            String alt = alternative.trim();
            if (alt.isEmpty()) {
                throw new IllegalArgumentException("Empty alternative in version range '" + range + "'");
            }
            if (alt.startsWith("[") || alt.startsWith("(")) {
                intervals.addAll(parseMaven(alt, range));
            } else {
                intervals.add(parseComparators(alt, range));
            }
        }
        return new VersionRange(text, Collections.unmodifiableList(intervals));
    }

    public static VersionRange any() {
        return ANY;
    }

    public boolean contains(Version version) {
        for (Interval interval : intervals) {
            if (interval.contains(version)) {
                return true;
            }
        }
        return false;
    }

    public boolean contains(String version) {
        Version parsed = Version.tryParse(version);
        return parsed != null && contains(parsed);
    }

    @Override
    public String toString() {
        return raw;
    }

    private static List<Interval> parseMaven(String text, String original) {
        List<Interval> result = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char open = text.charAt(i);
            if (open == ',' || Character.isWhitespace(open)) {
                i++;
                continue;
            }
            if (open != '[' && open != '(') {
                throw new IllegalArgumentException("Expected '[' or '(' in version range '" + original + "'");
            }
            int close = indexOfClose(text, i + 1);
            if (close < 0) {
                throw new IllegalArgumentException("Unclosed interval in version range '" + original + "'");
            }
            boolean minInclusive = open == '[';
            boolean maxInclusive = text.charAt(close) == ']';
            String body = text.substring(i + 1, close);
            int comma = body.indexOf(',');
            if (comma < 0) {
                // [1.0] means exactly 1.0
                if (!minInclusive || !maxInclusive) {
                    throw new IllegalArgumentException("Single-version interval must use [] in '" + original + "'");
                }
                Version exact = Version.parse(body.trim());
                result.add(new Interval(exact, true, exact, true));
            } else {
                String lower = body.substring(0, comma).trim();
                String upper = body.substring(comma + 1).trim();
                result.add(new Interval(
                        lower.isEmpty() ? null : Version.parse(lower), minInclusive,
                        upper.isEmpty() ? null : Version.parse(upper), maxInclusive));
            }
            i = close + 1;
        }
        if (result.isEmpty()) {
            throw new IllegalArgumentException("No intervals in version range '" + original + "'");
        }
        return result;
    }

    private static int indexOfClose(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ']' || c == ')') {
                return i;
            }
        }
        return -1;
    }

    private static Interval parseComparators(String text, String original) {
        Interval result = Interval.UNBOUNDED;
        for (String token : text.trim().split("\\s+")) {
            result = result.intersect(parseComparator(token, original));
        }
        return result;
    }

    private static Interval parseComparator(String token, String original) {
        if (token.equals("*")) {
            return Interval.UNBOUNDED;
        }
        if (token.startsWith(">=")) {
            return new Interval(Version.parse(token.substring(2)), true, null, false);
        }
        if (token.startsWith("<=")) {
            return new Interval(null, false, Version.parse(token.substring(2)), true);
        }
        if (token.startsWith(">")) {
            return new Interval(Version.parse(token.substring(1)), false, null, false);
        }
        if (token.startsWith("<")) {
            return new Interval(null, false, Version.parse(token.substring(1)), false);
        }
        if (token.startsWith("=")) {
            Version exact = Version.parse(token.substring(1));
            return new Interval(exact, true, exact, true);
        }
        if (token.startsWith("~")) {
            Version base = Version.parse(token.substring(1));
            return new Interval(base, true, Version.parse(base.major() + "." + (base.minor() + 1)), false);
        }
        if (token.startsWith("^")) {
            Version base = Version.parse(token.substring(1));
            return new Interval(base, true, Version.parse(String.valueOf(base.major() + 1)), false);
        }
        String lower = token.toLowerCase();
        if (lower.endsWith(".x") || lower.endsWith(".*")) {
            String prefix = token.substring(0, token.length() - 2);
            Version base = Version.parse(prefix);
            int last = base.componentCount() - 1;
            StringBuilder next = new StringBuilder();
            for (int i = 0; i < last; i++) {
                next.append(base.component(i)).append('.');
            }
            next.append(base.component(last) + 1);
            return new Interval(base, true, Version.parse(next.toString()), false);
        }
        Version parsed = Version.tryParse(token);
        if (parsed == null) {
            throw new IllegalArgumentException("Bad comparator '" + token + "' in version range '" + original + "'");
        }
        return new Interval(parsed, true, parsed, true);
    }

    private static final class Interval {

        static final Interval UNBOUNDED = new Interval(null, false, null, false);

        final Version min;
        final boolean minInclusive;
        final Version max;
        final boolean maxInclusive;

        Interval(Version min, boolean minInclusive, Version max, boolean maxInclusive) {
            this.min = min;
            this.minInclusive = minInclusive;
            this.max = max;
            this.maxInclusive = maxInclusive;
        }

        boolean contains(Version version) {
            if (min != null) {
                int cmp = version.compareTo(min);
                if (cmp < 0 || (cmp == 0 && !minInclusive)) {
                    return false;
                }
            }
            if (max != null) {
                int cmp = version.compareTo(max);
                if (cmp > 0 || (cmp == 0 && !maxInclusive)) {
                    return false;
                }
            }
            return true;
        }

        Interval intersect(Interval other) {
            Version newMin = min;
            boolean newMinIncl = minInclusive;
            if (other.min != null) {
                int cmp = newMin == null ? -1 : newMin.compareTo(other.min);
                if (cmp < 0 || (cmp == 0 && !other.minInclusive)) {
                    newMin = other.min;
                    newMinIncl = other.minInclusive;
                }
            }
            Version newMax = max;
            boolean newMaxIncl = maxInclusive;
            if (other.max != null) {
                int cmp = newMax == null ? 1 : newMax.compareTo(other.max);
                if (cmp > 0 || (cmp == 0 && !other.maxInclusive)) {
                    newMax = other.max;
                    newMaxIncl = other.maxInclusive;
                }
            }
            return new Interval(newMin, newMinIncl, newMax, newMaxIncl);
        }
    }
}
