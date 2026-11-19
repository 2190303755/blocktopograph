package com.mithrilmania.blocktopograph.storage

import com.mithrilmania.blocktopograph.PreparedWorkspace
import java.util.UUID

class Workspace(
    @JvmField val uuid: UUID,
    @JvmField val file: JvmFile
)

val PreparedWorkspace.uuid: UUID
    get() = UUID(uuidMostSignificantBits, uuidLeastSignificantBits)

fun PreparedWorkspace.resolve(parent: JvmFile): Workspace {
    val uuid = this.uuid
    return Workspace(uuid, JvmFile(parent, uuid.toString()))
}
