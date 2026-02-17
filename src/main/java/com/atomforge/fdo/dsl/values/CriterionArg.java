package com.atomforge.fdo.dsl.values;

/**
 * Type-safe wrapper for criterion arguments that accepts either:
 * <ul>
 *   <li>A known {@link Criterion} enum value for common criteria</li>
 *   <li>A raw string or numeric code for unknown/less-common criteria</li>
 * </ul>
 *
 * <p>This provides compile-time type safety and IDE autocomplete while still
 * supporting the full range of FDO criterion values.
 *
 * <p>Usage examples:
 * <pre>{@code
 * // Using known enum values
 * .setCriterion(CriterionArg.of(Criterion.SELECT))
 * .setCriterion(CriterionArg.of(Criterion.CLOSE))
 *
 * // Using raw values for unknown criteria
 * .setCriterion(CriterionArg.of(130))
 * .setCriterion(CriterionArg.of("custom_criterion"))
 * }</pre>
 */
public sealed interface CriterionArg permits CriterionArg.Known, CriterionArg.Raw {

    /**
     * Get the FDO text representation of this criterion.
     * @return The criterion name or numeric code as a string
     */
    String fdoName();

    /**
     * Create a CriterionArg from a known Criterion enum value.
     * @param criterion The criterion enum value
     * @return A typed CriterionArg
     */
    static CriterionArg of(Criterion criterion) {
        return new Known(criterion);
    }

    /**
     * Create a CriterionArg from a raw string value.
     * Use this for criterion keywords not yet in the Criterion enum.
     * @param value The raw criterion string (e.g., "close", "gain_focus")
     * @return A raw CriterionArg
     */
    static CriterionArg of(String value) {
        // Try to match to a known Criterion first
        Criterion known = Criterion.fromName(value);
        if (known != null) {
            return new Known(known);
        }
        return new Raw(value);
    }

    /**
     * Create a CriterionArg from a numeric code.
     * Use this for numeric criterion codes that don't have named equivalents.
     * @param code The numeric criterion code
     * @return A raw CriterionArg with the code as its value
     */
    static CriterionArg of(int code) {
        // Try to match to a known Criterion first
        Criterion known = Criterion.fromCode(code);
        if (known != null) {
            return new Known(known);
        }
        return new Raw(String.valueOf(code));
    }

    /**
     * A CriterionArg wrapping a known Criterion enum value.
     */
    record Known(Criterion criterion) implements CriterionArg {
        @Override
        public String fdoName() {
            return criterion.fdoName();
        }

        @Override
        public String toString() {
            return "CriterionArg.Known(" + criterion.name() + ")";
        }
    }

    /**
     * A CriterionArg wrapping a raw string or numeric value.
     */
    record Raw(String value) implements CriterionArg {
        @Override
        public String fdoName() {
            return value;
        }

        @Override
        public String toString() {
            return "CriterionArg.Raw(\"" + value + "\")";
        }
    }
}
