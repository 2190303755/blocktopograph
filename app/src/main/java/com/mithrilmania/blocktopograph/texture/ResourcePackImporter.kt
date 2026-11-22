package com.mithrilmania.blocktopograph.texture

import kotlinx.serialization.Serializable
import java.io.BufferedOutputStream
import java.io.File
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/** Shared buffer for SAF copies and zip entries; fewer syscalls. */
internal const val ZIP_BUFFER_SIZE = 64 * 1024

/** Already compressed, so store them instead of deflating again. */
private val ALREADY_COMPRESSED = hashSetOf("png", "jpg", "jpeg")

@Serializable
data class PackManifest(
    val header: PackHeader? = null,
    val modules: List<PackModule> = emptyList(),
) {
    val isResourcePack: Boolean get() = modules.any { it.type == "resources" }
}

@Serializable
data class PackHeader(val uuid: String? = null)

@Serializable
data class PackModule(val type: String? = null)

/** Empty [root] is the archive root; [uuid] may be missing. */
data class DiscoveredPack(val root: String, val uuid: String?)

/**
 * Finds packs inside mcaddon / mcpack / zip and rewrites them without the parts we do not need.
 * Plain JVM, no Android, so it stays testable.
 */
object ResourcePackImporter {

    /** Root manifest wins; otherwise look one level down. Other packs are skipped. */
    fun locatePacks(source: ZipFile): List<DiscoveredPack> {
        source.getEntry("manifest.json")?.let { manifest ->
            val parsed = readManifest(source, manifest)
            require(parsed.isResourcePack) { "Root manifest does not declare a resource pack" }
            return listOf(DiscoveredPack("", parsed.header?.uuid))
        }

        val roots = LinkedHashSet<String>()
        val entries = source.entries()
        while (entries.hasMoreElements()) {
            val name = entries.nextElement().name
            val slash = name.indexOf('/')
            if (slash > 0) roots.add(name.substring(0, slash))
        }
        val packs = roots.mapNotNull { root ->
            val manifest = source.getEntry(entryPath(root, "manifest.json")) ?: return@mapNotNull null
            val parsed = readManifest(source, manifest)
            if (parsed.isResourcePack) DiscoveredPack(root, parsed.header?.uuid) else null
        }
        require(packs.isNotEmpty()) { "Archive contains no resource pack" }
        return packs
    }

    /** manifest uuid, or a hash of the source filename when it is missing or unusable. */
    fun packId(discovered: DiscoveredPack, fallback: String): String {
        val raw = discovered.uuid?.takeIf { it.isNotBlank() } ?: fallback
        val sanitized = raw.replace(Regex("[^A-Za-z0-9._-]"), "_").trim('_')
        return sanitized.ifBlank { raw.hashCode().toUInt().toString(16) }
    }

    /** Keeps manifest.json, pack_icon.png and textures/ only. */
    fun repack(source: ZipFile, discovered: DiscoveredPack, target: File) {
        val manifest = source.getEntry(entryPath(discovered.root, "manifest.json"))
            ?: throw IllegalArgumentException("Resource pack is missing manifest.json: ${discovered.root}")
        val parent = target.parentFile
        if (parent != null && !parent.isDirectory && !parent.mkdirs()) {
            throw IOException("Cannot create resource pack directory: $parent")
        }
        val temp = File(parent, "${target.name}.tmp")
        ZipOutputStream(BufferedOutputStream(temp.outputStream(), ZIP_BUFFER_SIZE)).use { out ->
            copy(source, manifest, out, "manifest.json")

            source.getEntry(entryPath(discovered.root, "pack_icon.png"))?.let { icon ->
                copy(source, icon, out, "pack_icon.png")
            }

            val prefix = entryPath(discovered.root, "textures") + "/"
            val entries = source.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.startsWith(prefix)) continue
                val relative = entry.name.substring(prefix.length)
                if (relative.isEmpty()) continue
                copy(source, entry, out, "textures/$relative")
            }
        }

        if (target.exists() && !target.delete()) throw IOException("Cannot overwrite the existing resource pack: $target")
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IOException("Cannot write resource pack: $target")
        }
    }

    private fun entryPath(root: String, name: String) = if (root.isEmpty()) name else "$root/$name"

    private fun readManifest(source: ZipFile, entry: ZipEntry): PackManifest = runCatching {
        lenientTextureJson.decodeFromString<PackManifest>(
            source.getInputStream(entry).use { it.readBytes().decodeToString() }
        )
    }.getOrElse { PackManifest() }

    private fun copy(source: ZipFile, entry: ZipEntry, out: ZipOutputStream, name: String) {
        val target = ZipEntry(name)
        val extension = name.substringAfterLast('.', "").lowercase()
        if (extension in ALREADY_COMPRESSED && entry.size >= 0 && entry.crc >= 0) {
            // size and crc are already in the source entry, so STORED can copy it through untouched
            target.setMethod(ZipEntry.STORED)
            target.setSize(entry.size)
            target.setCompressedSize(entry.size)
            target.setCrc(entry.crc)
        }
        out.putNextEntry(target)
        source.getInputStream(entry).use { it.copyTo(out, ZIP_BUFFER_SIZE) }
        out.closeEntry()
    }
}
