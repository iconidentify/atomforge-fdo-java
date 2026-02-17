package com.atomforge.fdo.text.ast;

/**
 * Base interface for all FDO AST nodes.
 */
public sealed interface FdoNode permits StreamNode, AtomNode, ArgumentNode {

    /**
     * Get the source line number for error reporting.
     */
    int line();

    /**
     * Get the source column number for error reporting.
     */
    int column();
}
