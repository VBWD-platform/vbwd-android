package com.vbwd.plugin.meinchatplus.domain

import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class DowngradeGuardTest {
    @Test
    fun `a non-e2e protocol response is vetoed (raises, never silently downgrades)`() {
        val error = runCatching { DowngradeGuard.assertE2e("plain") }.exceptionOrNull()
        assertInstanceOf(E2eGuardError.ProtocolDowngrade::class.java, error)
    }

    @Test
    fun `an e2e_v1 response passes`() {
        assertNull(runCatching { DowngradeGuard.assertE2e("e2e_v1") }.exceptionOrNull())
    }
}
