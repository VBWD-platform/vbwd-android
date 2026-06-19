package com.vbwd.core.plugins

/**
 * `MAJOR.MINOR.PATCH` semantic version. Port of the iOS `SemanticVersion` /
 * the version handling behind the web `satisfiesVersion`.
 */
data class SemanticVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
) : Comparable<SemanticVersion> {

    override fun compareTo(other: SemanticVersion): Int =
        compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        /** Parses `"1.2.3"`; throws [PluginError.InvalidVersion] otherwise. */
        fun parse(string: String): SemanticVersion =
            parseOrNull(string) ?: throw PluginError.InvalidVersion(string)

        private const val PART_COUNT = 3

        /** Lenient parse used by [VersionConstraint] (Swift `try?` parity). */
        fun parseOrNull(string: String): SemanticVersion? {
            val parts = string.trim().split(".")
            if (parts.size != PART_COUNT) return null
            val major = parts[0].toIntOrNull() ?: return null
            val minor = parts[1].toIntOrNull() ?: return null
            val patch = parts[2].toIntOrNull() ?: return null
            if (major < 0 || minor < 0 || patch < 0) return null
            return SemanticVersion(major, minor, patch)
        }
    }
}

/**
 * A dependency version constraint. Supports the npm-style operators the plugin
 * registry accepts: `^`, `~`, `>=`, `>`, `<=`, `<`, `x`-ranges, `*`/empty (any),
 * and exact. Exact port of the iOS `VersionConstraint`.
 */
data class VersionConstraint(private val rawInput: String) {
    private val raw: String = rawInput.trim()

    @Suppress("ReturnCount")
    fun isSatisfiedBy(version: SemanticVersion): Boolean {
        if (raw.isEmpty() || raw == "*" || raw == "x" || raw == "latest") return true

        if (raw.startsWith("^")) return caret(raw.drop(1), version)
        if (raw.startsWith("~")) return tilde(raw.drop(1), version)
        if (raw.startsWith(">=")) return compareTo(raw.drop(2)) { version >= it }
        if (raw.startsWith("<=")) return compareTo(raw.drop(2)) { version <= it }
        if (raw.startsWith(">")) return compareTo(raw.drop(1)) { version > it }
        if (raw.startsWith("<")) return compareTo(raw.drop(1)) { version < it }

        if (raw.contains("x") || raw.contains("*")) return xRange(raw, version)

        return SemanticVersion.parseOrNull(raw)?.let { version == it } ?: false
    }

    private inline fun compareTo(spec: String, predicate: (SemanticVersion) -> Boolean): Boolean =
        SemanticVersion.parseOrNull(spec.trim())?.let(predicate) ?: false

    // ^1.2.3 → >=1.2.3 <2.0.0 ; ^0.2.3 → >=0.2.3 <0.3.0 ; ^0.0.3 → >=0.0.3 <0.0.4
    private fun caret(spec: String, version: SemanticVersion): Boolean {
        val base = SemanticVersion.parseOrNull(spec.trim()) ?: return false
        val upper = when {
            base.major > 0 -> SemanticVersion(base.major + 1, 0, 0)
            base.minor > 0 -> SemanticVersion(0, base.minor + 1, 0)
            else -> SemanticVersion(0, 0, base.patch + 1)
        }
        return version >= base && version < upper
    }

    // ~1.2.3 → >=1.2.3 <1.3.0
    private fun tilde(spec: String, version: SemanticVersion): Boolean {
        val base = SemanticVersion.parseOrNull(spec.trim()) ?: return false
        return version >= base && version < SemanticVersion(base.major, base.minor + 1, 0)
    }

    // 1.x / 1.2.x / 1.* — fixed prefix, wildcard tail
    @Suppress("ReturnCount")
    private fun xRange(spec: String, version: SemanticVersion): Boolean {
        val parts = spec.split(".")
        fun isWild(part: String) = part == "x" || part == "*" || part.isEmpty()
        val major = parts.getOrNull(0)?.toIntOrNull() ?: return false
        if (major != version.major) return false
        if (parts.size >= MINOR_PRESENT) {
            if (isWild(parts[1])) return true
            if (parts[1].toIntOrNull() != version.minor) return false
        }
        if (parts.size >= PATCH_PRESENT) {
            if (isWild(parts[2])) return true
            if (parts[2].toIntOrNull() != version.patch) return false
        }
        return true
    }

    private companion object {
        const val MINOR_PRESENT = 2
        const val PATCH_PRESENT = 3
    }
}
