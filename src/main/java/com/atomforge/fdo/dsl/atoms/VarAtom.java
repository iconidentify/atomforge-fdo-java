package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomTable;

public enum VarAtom implements DslAtom {
    NUMBER_SAVE,
    NUMBER_SET,
    NUMBER_SET_FROM_ATOM,
    NUMBER_GET,
    NUMBER_SET_ID_ONE_VALUE,
    STRING_SET,
    STRING_NULL,
    STRING_SAVE,
    STRING_SET_FROM_ATOM,
    STRING_GET,
    STRING_SET_ID_ONE_VALUE,
    DATA_SET,
    DATA_ZERO,
    DATA_SAVE,
    DATA_SET_FROM_ATOM,
    DATA_GET,
    DATA_SET_ID_ONE_VALUE,
    LOOKUP_BY_ID,
    NUMA_ZERO,
    NUMA_ONES,
    NUMA_INCREMENT,
    NUMA_DECREMENT,
    NUMBER_INCREMENT_SAVE,
    NUMBER_DECREMENT_SAVE,
    NUMBER_ZERO_SAVE,
    NUMBER_ONES_SAVE,
    NUMBER_ADD,
    NUMBER_SUB,
    NUMBER_MUL,
    NUMBER_DIV,
    NUMBER_ANDING,
    NUMBER_ORING,
    NUMBER_SHL,
    NUMBER_SHR,
    NUMBER_COPY_BETWEEN_REGS,
    NUMBER_SWAP_BETWEEN_REGS,
    NUMBER_CLEAR_ID,
    NUMBER_INCREMENT,
    NUMBER_DECREMENT,
    NUMBER_ZERO,
    NUMBER_ONES,
    STRING_COPY_STRA_TO_STRB,
    STRING_COPY_STRB_TO_STRA,
    STRING_COPY_BETWEEN_REGS,
    STRING_SWAP_BETWEEN_REGS,
    STRING_CLEAR_ID,
    STRING_TRIM_SPACES,
    STRING_TRIM_SPACES_SAFELY,
    DATA_COPY_DATA_TO_DATB,
    DATA_COPY_DATB_TO_DATA,
    DATA_COPY_BETWEEN_REGS,
    DATA_SWAP_BETWEEN_REGS,
    DATA_CLEAR_ID,
    RESET,
    RANDOM_NUMBER,
    STRING_CONCATENATE_REGS,
    CHECK_STRING_TYPE_AND_CONVERT,
    GET_STRING_CONVERT_LEN,
    NUMBER,
    STRING,
    DATA;

    private final AtomDefinition def;

    VarAtom() {
        String atomName = "var_" + name().toLowerCase();
        this.def = AtomTable.loadDefault().findByName(atomName)
            .orElseThrow(() -> new IllegalStateException(
                "Atom not found in registry: " + atomName + 
                ". This indicates a mismatch between DslEnumGenerator and AtomRegistry."));
    }

    @Override
    public AtomDefinition definition() {
        return def;
    }

    public static VarAtom fromAtomNumber(int atomNumber) {
        for (VarAtom atom : values()) {
            if (atom.atomNumber() == atomNumber) {
                return atom;
            }
        }
        return null;
    }

    public static VarAtom fromName(String name) {
        if (name == null) return null;
        String lower = name.toLowerCase();
        for (VarAtom atom : values()) {
            if (atom.atomName().equals(lower)) {
                return atom;
            }
        }
        return null;
    }
}
