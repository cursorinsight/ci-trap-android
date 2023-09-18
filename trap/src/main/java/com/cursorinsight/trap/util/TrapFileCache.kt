package com.cursorinsight.trap.util

import java.io.File
import java.util.UUID

/**
 * Represents the file system cache used to
 * cache failed data packets.
 *
 * @property cacheSize The quasi-maximum size of the file cache.
 * @constructor
 * Create the cache dir in the file system if not exists.
 *
 * @param baseCacheDir The file system directory for the cache.
 */
internal class TrapFileCache(
    baseCacheDir: File,
    private val cacheSize: Long
) {
    /**
     * The actual File cache dir.
     */
    private val cacheDir = File(baseCacheDir, "trap")

    init {
        cacheDir.mkdir()
    }

    /**
     * Push a new data packet to the file cache.
     *
     * @param data The content which needs to be cached.
     */
    fun push(data: String) {
        cleanup()

        val file = File(cacheDir, UUID.randomUUID().toString())
        file.createNewFile()
        file.writeText(data)

    }

    /**
     * Get all the cached records.
     *
     * @return The list of cache files in Record form.
     */
    fun getAll(): List<Record> {
        val files = cacheDir.listFiles() ?: arrayOf()
        return files.map { Record(it) }
    }

    /**
     * Clean up the cache directory if
     * the cache files are over max size.
     */
    private fun cleanup() {
        val files = cacheDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        var total: Long = 0

        for (file in files) {
            total += file.length()
            if (total > cacheSize) {
                // We are over max cache size, start deleting
                file.delete()
            }
        }
    }

    /**
     * Clears the file system cache completely.
     */
    fun clear() {
        val files = cacheDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
        for (file in files) {
            Record(file).delete()
        }
    }

    /**
     * Represents a cache record abstracted away
     * from the file system.
     *
     * @property file The file system file name this Record represents.
     */
    class Record(private val file: File) {
        /**
         * Delete this record from the cache.
         */
        fun delete() {
            file.delete()
        }

        /**
         * Reads the contents of this record.
         *
         * @return The content of this file.
         */
        fun content(): String {
            return file.readText()
        }
    }
}
