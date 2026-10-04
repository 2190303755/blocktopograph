package com.mithrilmania.blocktopograph.map.edit

fun SnrConfig.perform(target: EditTarget): EditResultCode {
    target.setMaxError(Int.MAX_VALUE)
    target.forEachXyz(SnrEdit(this))
    return EditResultCode.SUCCESS
}
