package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum IfAtom implements DslAtom {
    NUMA_FALSE_THEN,
    NUMB_TRUE_THEN,
    NUMB_FALSE_THEN,
    NUMA_EQ_ONE_THEN,
    NUMA_GT_ONE_THEN,
    NUMB_EQ_ONE_THEN,
    NUMB_GT_ONE_THEN,
    STRA_EQ_NULL_THEN,
    STRB_EQ_NULL_THEN,
    STRA_NEQ_NULL_THEN,
    STRB_NEQ_NULL_THEN,
    DATA_EQ_ZERO_THEN,
    DATB_EQ_ZERO_THEN,
    DATA_NEQ_ZERO_THEN,
    DATB_NEQ_ZERO_THEN,
    FREE_AREA_THEN,
    TEST_INDEX_TRUE_THEN,
    TEST_INDEX_FALSE_THEN,
    LAST_RETURN_TRUE_THEN,
    LAST_RETURN_FALSE_THEN,
    LAST_RETURN_TRUE_EXIT,
    LAST_RETURN_FALSE_EXIT,
    PAID_AREA_THEN,
    NUMA_EQ_NUMB_THEN,
    NUMA_NEQ_NUMB_THEN,
    NUMA_GT_NUMB_THEN,
    NUMA_GTE_NUMB_THEN,
    NUMA_LT_NUMB_THEN,
    NUMA_LTE_NUMB_THEN,
    NUMA_AND_NUMB_THEN,
    NUMA_OR_NUMB_THEN,
    NUMA_TRUE_THEN,
    STRA_EQ_STRB_THEN,
    STRA_NEQ_STRB_THEN,
    STRA_IN_STRB_PREFIX_THEN,
    STRB_IN_STRA_PREFIX_THEN,
    DATA_EQ_DATB_THEN,
    DATA_NEQ_DATB_THEN,
    DATA_IN_DATB_PREFIX_THEN,
    DATB_IN_DATA_PREFIX_THEN,
    ONLINE_THEN,
    OFFLINE_THEN,
    GUEST_THEN,
    OWNER_THEN,
    NEWUSER_THEN,
    AM_THEN,
    PM_THEN,
    DEBUG_TRUE_THEN,
    DEBUG_FALSE_THEN;

    private final AtomDefinition def;

    IfAtom() {
        String atomName = "if_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static IfAtom fromAtomNumber(int atomNumber) {
        for (IfAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static IfAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (IfAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
