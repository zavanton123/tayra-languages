package com.tayra.languages.core.data.export

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64
import kotlin.js.Promise

/**
 * Builds the database in memory with sql.js, whose loader the web app serves beside the
 * worker scripts, and answers with the file as base64.
 */
private fun ankiSqlite(statementsJson: String): Promise<JsAny?> = js(
    """(function () {
        var load = window.initSqlJs ? Promise.resolve() : new Promise(function (resolve, reject) {
            var script = document.createElement('script');
            script.src = 'sql-wasm.js';
            script.onload = resolve;
            script.onerror = function () { reject(new Error('Could not load sql-wasm.js')); };
            document.head.appendChild(script);
        });
        return load.then(function () { return window.initSqlJs({ locateFile: function (file) { return file; } }); }).then(function (SQL) {
            var db = new SQL.Database();
            JSON.parse(statementsJson).forEach(function (sql) { db.run(sql); });
            var bytes = db.export();
            db.close();
            var binary = '';
            for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
            return btoa(binary);
        });
    })()""",
)

internal actual suspend fun ankiSqliteBytes(statements: List<String>): ByteArray {
    val base64 = ankiSqlite(Json.encodeToString(statements)).await<JsAny?>() as? JsString ?: error("Could not build the Anki collection")
    return Base64.decode(base64.toString())
}
