package com.atomforge.fdo.dsl.atoms;

import com.atomforge.fdo.atom.AtomDefinition;
import com.atomforge.fdo.atom.AtomType;
import com.atomforge.fdo.atom.AtomFlags;
import java.util.EnumSet;

public interface DslAtom {

    AtomDefinition definition();

    default int protocol() {
        return definition().protocol();
    }

    default int atomNumber() {
        return definition().atomNumber();
    }

    default String atomName() {
        return definition().name();
    }

    default AtomType type() {
        return definition().type();
    }

    default EnumSet<AtomFlags> flags() {
        return definition().flags();
    }

    default boolean isIndent() {
        return definition().isIndent();
    }

    default boolean isOutdent() {
        return definition().isOutdent();
    }

    default boolean isEos() {
        return definition().isEos();
    }
}
