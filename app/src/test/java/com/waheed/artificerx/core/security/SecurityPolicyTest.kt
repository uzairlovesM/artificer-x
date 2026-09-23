package com.waheed.artificerx.core.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class SecurityPolicyTest {
    @Test
    fun blocksDangerousCommands() {
        assertFalse(SecurityPolicy.isShellAllowed("rm -rf /"))
        assertFalse(SecurityPolicy.isShellAllowed("reboot"))
        assertTrue(SecurityPolicy.isShellAllowed("mkdir -p demo"))
        assertFalse(SecurityPolicy.isShellAllowed("mkdir -p demo && echo hello"))
        assertFalse(SecurityPolicy.isShellAllowed("echo hello > demo/a.txt"))
        assertFalse(SecurityPolicy.isShellAllowed("sleep 60 &"))
        assertFalse(SecurityPolicy.isShellAllowed("nohup sleep 60"))
        assertFalse(SecurityPolicy.isShellAllowed("setsid sleep 60"))
    }

    @Test
    fun rejectsPathEscape() {
        val root = File(System.getProperty("java.io.tmpdir"), "artificer-security-test")
        val inside = SecurityPolicy.constrainPath(root, "a/b.txt")
        val outside = SecurityPolicy.constrainPath(root, "../../outside.txt")
        assertTrue(inside?.path?.startsWith(root.canonicalPath) == true)
        assertTrue(outside == null)
    }
}
