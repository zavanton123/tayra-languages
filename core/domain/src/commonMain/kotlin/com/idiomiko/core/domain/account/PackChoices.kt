package com.idiomiko.core.domain.account

/**
 * The course and book packs installed and removed on this device, which sync carries to the
 * account's other devices so they install and remove them too. A pack is named as [coursePack] and
 * [bookPack] name it.
 */
interface PackChoices {
    /** The pack was installed, or removed on purpose, now. */
    fun chose(pack: String, installed: Boolean)

    /** The pack is installed here: one never chosen counts as installed now, as it was before choices were kept. */
    fun found(pack: String)

    companion object {
        fun coursePack(id: String) = "course:$id"
        fun bookPack(id: String) = "book:$id"
    }
}
