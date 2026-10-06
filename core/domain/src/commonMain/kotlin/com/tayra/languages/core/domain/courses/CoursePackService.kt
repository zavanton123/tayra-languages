package com.tayra.languages.core.domain.courses

import com.tayra.languages.core.domain.dictionary.PackState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Installs and removes course packs. Installing writes the pack's courses into the database for
 * every language it teaches; removing deletes them again, with the texts read from their lessons.
 */
class CoursePackService(private val store: CoursePackStore, private val courses: CourseService) {
    private val _packs = MutableStateFlow(CoursePacks.all.map { CoursePackStatus(it, PackState.NotInstalled) })

    /** Every known pack with its current state, in catalog order. */
    val packs: StateFlow<List<CoursePackStatus>> = _packs.asStateFlow()

    /** Reads which packs are on the device; call once at start. */
    suspend fun refresh() {
        for (pack in CoursePacks.all) {
            if (stateOf(pack) is PackState.Downloading) continue
            val size = store.installedSize(pack)
            setState(pack, if (size != null) PackState.Installed(size) else PackState.NotInstalled)
        }
    }

    /** Downloads a pack and writes its courses; failures end in [PackState.Failed] rather than an exception. */
    suspend fun download(pack: CoursePack) {
        if (stateOf(pack) is PackState.Downloading) return
        setState(pack, PackState.Downloading(null))
        try {
            store.install(pack) { progress -> setState(pack, PackState.Downloading(progress)) }
            if (store.courseIds(pack).isEmpty()) {
                store.remove(pack)
                error("The downloaded file has no courses this version of the app can read")
            }
            courses.seedSamples()
            setState(pack, PackState.Installed(store.installedSize(pack) ?: 0))
        } catch (e: Exception) {
            setState(pack, PackState.Failed(e.message ?: "Download failed"))
        }
    }

    /** Deletes the pack's courses, with their lessons and the texts read from them, and then the file. */
    suspend fun remove(pack: CoursePack) {
        courses.removeSamples(store.courseIds(pack))
        store.remove(pack)
        setState(pack, PackState.NotInstalled)
    }

    private fun stateOf(pack: CoursePack): PackState? = _packs.value.firstOrNull { it.pack.id == pack.id }?.state

    private fun setState(pack: CoursePack, state: PackState) {
        _packs.update { list -> list.map { if (it.pack.id == pack.id) it.copy(state = state) else it } }
    }
}
