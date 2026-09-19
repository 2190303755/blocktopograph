package com.mithrilmania.blocktopograph.storage

import com.mithrilmania.blocktopograph.util.rpc
import kotlinx.coroutines.runBlocking
import okio.FileHandle
import okio.FileMetadata
import okio.FileNotFoundException
import okio.FileSystem
import okio.IOException
import okio.Path
import okio.Path.Companion.toPath
import okio.Sink
import okio.Source
import java.io.InterruptedIOException

/**
 * @see FileService
 * @see okio.NioSystemFileSystem
 */
object ShizukuFileSystem : FileSystem() {
    override fun canonicalize(path: Path): Path = runBlocking { awaitFileService() }?.rpc {
        it.canonicalize(path.toString())
    }?.toPath(false) ?: throw FileNotFoundException("No such file")

    override fun metadataOrNull(path: Path): FileMetadata? {
        val metadata = runBlocking { awaitFileService() }?.rpc {
            it.metadata(path.toString())
        } ?: return null
        val link = metadata.symlinkTarget
        return FileMetadata(
            isRegularFile = metadata.isRegularFile,
            isDirectory = metadata.isDirectory,
            symlinkTarget = if (link === null) null else link.toPath(false),
            size = metadata.size,
            createdAtMillis = metadata.createdAtMillis.takeIf { it != 0L },
            lastModifiedAtMillis = metadata.lastModifiedAtMillis.takeIf { it != 0L },
            lastAccessedAtMillis = metadata.lastAccessedAtMillis.takeIf { it != 0L }
        )
    }

    override fun list(dir: Path): List<Path> {
        return emptyList() // TODO: Not yet implemented
    }

    override fun listOrNull(dir: Path): List<Path> {
        return emptyList() // TODO: Not yet implemented
    }

    override fun openReadOnly(file: Path): FileHandle = runBlocking { awaitFileService() }?.rpc {
        it.openReadOnly(file.toString())
    }?.fileHandle(false) ?: throw FileNotFoundException()

    override fun openReadWrite(
        file: Path,
        mustCreate: Boolean,
        mustExist: Boolean
    ): FileHandle {
        require(!mustCreate || !mustExist) {
            "Cannot require mustCreate and mustExist at the same time."
        }
        return runBlocking { awaitFileService() }?.rpc {
            it.openReadWrite(file.toString(), mustCreate, mustExist)
        }?.fileHandle(true) ?: throw IOException("Failed to open $file")
    }

    override fun source(file: Path): Source =
        this.openReadOnly(file).source()

    override fun sink(file: Path, mustCreate: Boolean): Sink =
        this.openReadWrite(file, mustCreate, false).sink()

    override fun appendingSink(file: Path, mustExist: Boolean): Sink =
        this.openReadWrite(file, false, mustExist).appendingSink()

    override fun createDirectory(dir: Path, mustCreate: Boolean) {
        val service = runBlocking { awaitFileService() }
        val path = dir.toString()
        if (service?.rpc { it.createDirectory(path) } != true) {
            if (service?.rpc { it.metadata(path) }?.isDirectory == true) {
                if (mustCreate) {
                    throw IOException("$dir already exists.")
                } else {
                    return
                }
            }
            throw IOException("Failed to create directory: $dir")
        }
    }

    override fun atomicMove(source: Path, target: Path) {
        val succeed = runBlocking { awaitFileService() }?.rpc {
            it.atomicMove(source.toString(), target.toString())
        }
        if (succeed === null) {
            throw NullPointerException()
        } else if (!succeed) {
            throw IOException("Failed to atomic move")
        }
    }

    override fun delete(path: Path, mustExist: Boolean) {
        if (Thread.interrupted()) {
            // If the current thread has been interrupted.
            throw InterruptedIOException("interrupted")
        }
        val target = path.toString()
        val service = runBlocking { awaitFileService() }
        val deleted = service?.rpc { it.delete(target) }
        if (deleted === null) {
            throw NullPointerException()
        } else if (!deleted) {
            if (service.rpc { it.metadata(target) } !== null) {
                throw IOException("Failed to delete $path")
            }
            if (mustExist) {
                throw FileNotFoundException("No such file: $path")
            }
        }
    }

    @Deprecated("Unsupported Operation")
    override fun createSymlink(source: Path, target: Path): Nothing {
        throw IOException("Unsupported Operation")
    }

    override fun toString() = "ShizukuFileSystem"
}