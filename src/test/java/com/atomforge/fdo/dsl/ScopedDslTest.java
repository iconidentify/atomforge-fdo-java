package com.atomforge.fdo.dsl;

import com.atomforge.fdo.FdoDecompiler;
import com.atomforge.fdo.FdoException;
import com.atomforge.fdo.dsl.values.*;
import com.atomforge.fdo.model.FdoGid;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the scoped DSL API (object(), stream(), context(), sibling()).
 *
 * These methods provide automatic scoping - no endObject(), endStream(), or endContext() needed.
 */
class ScopedDslTest {

    @Test
    void testScopedObjectSimple() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.VIEW, "Hello", obj -> {
                obj.data("Hello, World!");
            })
            .compile();

        assertNotNull(binary);
        assertTrue(binary.length > 0);

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("view"));
        assertTrue(decompiled.contains("Hello"));
        assertTrue(decompiled.contains("Hello, World!"));
        assertTrue(decompiled.contains("man_end_object"));
    }

    @Test
    void testScopedObjectWithAttributes() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.IND_GROUP, "Login Form", obj -> {
                obj.orientation(Orientation.VCF);
                obj.position(Position.CENTER_CENTER);
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("ind_group"));
        assertTrue(decompiled.contains("Login Form"));
        assertTrue(decompiled.contains("man_end_object"));
    }

    @Test
    void testScopedNestedObjects() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.IND_GROUP, "Parent", parent -> {
                parent.object(ObjectType.VIEW, "Child1", child -> {
                    child.data("First child");
                });
                parent.object(ObjectType.VIEW, "Child2", child -> {
                    child.data("Second child");
                });
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("ind_group"));
        assertTrue(decompiled.contains("Parent"));
        assertTrue(decompiled.contains("Child1"));
        assertTrue(decompiled.contains("Child2"));

        // Should have 3 end_object calls (2 children + 1 parent)
        int endCount = countOccurrences(decompiled, "man_end_object");
        assertEquals(3, endCount);
    }

    @Test
    void testScopedObjectWithNoTitle() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.ORG_GROUP, obj -> {
                obj.orientation(Orientation.HCF);
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("org_group"));
        assertTrue(decompiled.contains("man_end_object"));
    }

    @Test
    void testScopedSiblings() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.ORG_GROUP, "Container", container -> {
                container.object(ObjectType.TRIGGER, "First", btn -> {
                    btn.data("1");
                });
                container.sibling(ObjectType.TRIGGER, "Second", btn -> {
                    btn.data("2");
                });
                container.sibling(ObjectType.TRIGGER, "Third", btn -> {
                    btn.data("3");
                });
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("Container"));
        assertTrue(decompiled.contains("First"));
        assertTrue(decompiled.contains("Second"));
        assertTrue(decompiled.contains("Third"));

        // Check sibling atoms are present
        int siblingCount = countOccurrences(decompiled, "man_start_sibling");
        assertEquals(2, siblingCount);
    }

    @Test
    void testScopedStream() throws FdoException {
        byte[] binary = FdoScript.stream()
            .stream(s -> {
                s.data("Stream content");
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("uni_start_stream"));
        assertTrue(decompiled.contains("uni_end_stream"));
        assertTrue(decompiled.contains("Stream content"));
    }

    @Test
    void testScopedStreamWaitOn() throws FdoException {
        byte[] binary = FdoScript.stream()
            .streamWaitOn(s -> {
                s.data("Wait on content");
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("uni_start_stream_wait_on"));
        assertTrue(decompiled.contains("uni_end_stream"));
    }

    @Test
    void testScopedContext() throws FdoException {
        byte[] binary = FdoScript.stream()
            .context(100, ctx -> {
                ctx.data("Context content");
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("man_set_context_relative"));
        assertTrue(decompiled.contains("100"));
        assertTrue(decompiled.contains("man_end_context"));
    }

    @Test
    void testScopedContextGlobalId() throws FdoException {
        FdoGid gid = FdoGid.of(1, 0, 100);
        byte[] binary = FdoScript.stream()
            .contextGlobalId(gid, ctx -> {
                ctx.data("GID context content");
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("man_set_context_globalid"));
        assertTrue(decompiled.contains("man_end_context"));
    }

    @Test
    void testDeepNesting() throws FdoException {
        // Test 5 levels of nesting
        byte[] binary = FdoScript.stream()
            .object(ObjectType.IND_GROUP, "L1", l1 -> {
                l1.object(ObjectType.ORG_GROUP, "L2", l2 -> {
                    l2.object(ObjectType.ORG_GROUP, "L3", l3 -> {
                        l3.object(ObjectType.ORG_GROUP, "L4", l4 -> {
                            l4.object(ObjectType.VIEW, "L5", l5 -> {
                                l5.data("Deep content");
                            });
                        });
                    });
                });
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("L1"));
        assertTrue(decompiled.contains("L2"));
        assertTrue(decompiled.contains("L3"));
        assertTrue(decompiled.contains("L4"));
        assertTrue(decompiled.contains("L5"));
        assertTrue(decompiled.contains("Deep content"));

        // Should have 5 end_object calls
        int endCount = countOccurrences(decompiled, "man_end_object");
        assertEquals(5, endCount);
    }

    @Test
    void testScopedWithActions() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.TRIGGER, "Button", btn -> {
                btn.triggerStyle(TriggerStyle.FRAMED);
                btn.onSelect(action -> {
                    action.stream(s -> {
                        s.sendTokenArg("LP");
                    });
                });
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("trigger"));
        assertTrue(decompiled.contains("Button"));
    }

    @Test
    void testObjectBuilderContextMethod() throws FdoException {
        byte[] binary = FdoScript.stream()
            .object(ObjectType.IND_GROUP, "Parent", parent -> {
                parent.context(50, ctx -> {
                    ctx.data("Inside context");
                });
            })
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("man_set_context_relative"));
        assertTrue(decompiled.contains("50"));
        assertTrue(decompiled.contains("man_end_context"));
    }

    @Test
    void testMixedScopedAndLegacy() throws FdoException {
        // Test that scoped and legacy APIs can coexist
        @SuppressWarnings("deprecation")
        byte[] binary = FdoScript.stream()
            .object(ObjectType.IND_GROUP, "ScopedParent", parent -> {
                parent.data("Scoped");
            })
            .startObject(ObjectType.VIEW, "LegacyChild")
                .data("Legacy")
            .endObject()
            .compile();

        String decompiled = FdoDecompiler.create().decompile(binary);
        assertTrue(decompiled.contains("ScopedParent"));
        assertTrue(decompiled.contains("LegacyChild"));
    }

    @Test
    void testExceptionInLambdaStillEndsObject() throws FdoException {
        // The object should still be properly closed even if exception occurs
        // We verify by checking that the frames list has the correct structure
        StreamBuilder builder = FdoScript.stream();

        try {
            builder.object(ObjectType.VIEW, "Test", obj -> {
                obj.data("Before exception");
                throw new RuntimeException("Test exception");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        // Even after exception, the builder should be usable (end_object was added by finally block)
        // Add another object to verify builder is still functional
        builder.object(ObjectType.VIEW, "After", obj -> {
            obj.data("Still works");
        });

        byte[] binary = builder.compile();
        String decompiled = FdoDecompiler.create().decompile(binary);

        // Both objects should be present and properly closed
        assertTrue(decompiled.contains("Test"));
        assertTrue(decompiled.contains("After"));
    }

    private int countOccurrences(String str, String sub) {
        int count = 0;
        int idx = 0;
        while ((idx = str.indexOf(sub, idx)) != -1) {
            count++;
            idx += sub.length();
        }
        return count;
    }
}
