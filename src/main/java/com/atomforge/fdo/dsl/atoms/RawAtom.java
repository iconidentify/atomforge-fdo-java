package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomFlags;
import com.atomforge.fdo.atom.AtomType;

import java.util.EnumSet;

public final class RawAtom implements DslAtom {

    private final String atomName;
    private final AtomDefinition definition;

    private RawAtom(String atomName) {
        this.atomName = atomName;
        // Create a placeholder definition for unknown atoms
        // Protocol 0 and atom 0 are placeholders - the actual encoding
        // will be looked up from the AtomTable by name
        this.definition = new AtomDefinition(0, 0, atomName, AtomType.RAW, EnumSet.noneOf(AtomFlags.class));
    }

    public static RawAtom of(String atomName) {
        return new RawAtom(atomName);
    }

    @Override
    public AtomDefinition definition() {
        return definition;
    }

    @Override
    public String atomName() {
        return atomName;
    }

    @Override
    public String toString() {
        return atomName;
    }
}
