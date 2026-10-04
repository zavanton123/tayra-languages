package com.tayra.languages.core.data.db

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.encoding.Base64
import kotlin.js.Promise

/** Loads sql.js, whose loader the web app serves beside the worker scripts. */
private fun loadSqlJs(): Promise<JsAny?> = js(
    """(window.initSqlJs ? Promise.resolve() : new Promise(function (resolve, reject) {
            var script = document.createElement('script');
            script.src = 'sql-wasm.js';
            script.onload = resolve;
            script.onerror = function () { reject(new Error('Could not load sql-wasm.js')); };
            document.head.appendChild(script);
        })).then(function () { return window.initSqlJs({ locateFile: function (file) { return file; } }); })""",
)

/** Builds the database in memory and answers with the file as base64. */
private fun buildSqlite(sqlJs: JsAny?, statementsJson: String): String = js(
    """(function () {
        var db = new sqlJs.Database();
        try {
            JSON.parse(statementsJson).forEach(function (sql) { db.run(sql); });
            var bytes = db.export();
            var binary = '';
            for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
            return btoa(binary);
        } finally {
            db.close();
        }
    })()""",
)

/** Runs the queries on the database in the base64 [file], answering their rows as JSON. */
private fun querySqlite(sqlJs: JsAny?, file: String, queriesJson: String): String = js(
    """(function () {
        var binary = atob(file);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        var db = new sqlJs.Database(bytes);
        try {
            return JSON.stringify(JSON.parse(queriesJson).map(function (sql) {
                var result = db.exec(sql);
                return result.length ? result[0].values : [];
            }));
        } finally {
            db.close();
        }
    })()""",
)

internal actual suspend fun newSqliteFile(statements: List<String>): ByteArray {
    val sqlJs = loadSqlJs().await<JsAny?>()
    return Base64.decode(buildSqlite(sqlJs, Json.encodeToString(statements)))
}

internal actual suspend fun readSqliteFile(file: ByteArray, queries: List<SqliteQuery>): List<List<List<String?>>> {
    val sqlJs = loadSqlJs().await<JsAny?>()
    val results = Json.parseToJsonElement(querySqlite(sqlJs, Base64.encode(file), Json.encodeToString(queries.map { it.sql }))).jsonArray
    return results.mapIndexed { i, rows ->
        rows.jsonArray.map { row -> List(queries[i].columns) { column -> row.jsonArray.getOrNull(column)?.jsonPrimitive?.contentOrNull } }
    }
}
