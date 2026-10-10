package com.tayra.languages.core.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlSchema

/**
 * This schema at a downloaded pack's format. A pack file carries its format as `user_version`,
 * while a generated schema's version is 1 whatever the format; the Android and iOS drivers refuse
 * a file whose version is above the schema's, so they open a pack as the schema at the pack's own
 * format, which creates and migrates nothing.
 */
internal fun <T : QueryResult<Unit>> SqlSchema<T>.atFormat(format: Int): SqlSchema<T> =
    object : SqlSchema<T> by this {
        override val version: Long = format.toLong()
    }
