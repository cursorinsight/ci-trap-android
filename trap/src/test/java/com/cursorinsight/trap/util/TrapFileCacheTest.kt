package com.cursorinsight.trap.util

import io.mockk.junit5.MockKExtension
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
class TrapFileCacheTest {
    private var baseCacheDir = File(System.getProperty("java.io.tmpdir"))

    private var cache = TrapFileCache(baseCacheDir, 64)

    @BeforeEach
    fun setUp() {
        cache.clear()
    }

    @Test
    fun `test store and load`() {
        cache.push("[[999], ]")
        val data = cache.getAll()
        assert(data.size == 1)
        assert(data.first().content() == "[[999], ]")
    }

    @Test
    fun `test overcapacity`() {
        cache.push("[[999], ]")
        cache.push("[[888], ]")
        cache.push("[[777], ]")
        cache.push("[[666], ]")
        cache.push("[[555], ]")
        cache.push("[[444], ]")
        cache.push("[[333], ]")
        cache.push("[[222], ]")
        cache.push("[[111], ]")

        val data = cache.getAll()
        assertSame(data.size, 8)

    }
}