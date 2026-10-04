package com.tayra.languages.core.data.backup

import kotlinx.coroutines.await
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlin.io.encoding.Base64
import kotlin.js.Promise

private fun opfsList(): Promise<JsAny?> = js(
    """navigator.storage.getDirectory().then(function (root) { return root.getDirectoryHandle('backups', { create: true }); })
        .then(async function (folder) {
            var files = [];
            for await (const handle of folder.values()) {
                if (handle.kind === 'file') files.push({ name: handle.name, size: (await handle.getFile()).size });
            }
            return JSON.stringify(files);
        })""",
)

private fun opfsRead(name: String): Promise<JsAny?> = js(
    """navigator.storage.getDirectory().then(function (root) { return root.getDirectoryHandle('backups', { create: true }); })
        .then(function (folder) { return folder.getFileHandle(name); })
        .then(function (handle) { return handle.getFile(); })
        .then(function (file) { return file.arrayBuffer(); })
        .then(function (buffer) {
            var bytes = new Uint8Array(buffer);
            var binary = '';
            for (var i = 0; i < bytes.length; i++) binary += String.fromCharCode(bytes[i]);
            return btoa(binary);
        })""",
)

private fun opfsWrite(name: String, base64: String): Promise<JsAny?> = js(
    """navigator.storage.getDirectory().then(function (root) { return root.getDirectoryHandle('backups', { create: true }); })
        .then(function (folder) { return folder.getFileHandle(name, { create: true }); })
        .then(function (handle) { return handle.createWritable(); })
        .then(function (writable) {
            var binary = atob(base64);
            var bytes = new Uint8Array(binary.length);
            for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
            return writable.write(bytes).then(function () { return writable.close(); });
        })
        .then(function () { return null; })""",
)

private fun opfsDelete(name: String): Promise<JsAny?> = js(
    """navigator.storage.getDirectory().then(function (root) { return root.getDirectoryHandle('backups', { create: true }); })
        .then(function (folder) { return folder.removeEntry(name); })
        .catch(function () { return null; })
        .then(function () { return null; })""",
)

/** Browsers have no folders of their own, so backups live in the origin private file system. */
actual class BackupFiles {
    actual suspend fun list(): List<StoredBackupFile> {
        val json = opfsList().await<JsAny?>().toString()
        return Json.parseToJsonElement(json).jsonArray.map {
            val file = it.jsonObject
            StoredBackupFile(file.getValue("name").jsonPrimitive.content, file.getValue("size").jsonPrimitive.long)
        }
    }

    actual suspend fun read(name: String): ByteArray = Base64.decode(opfsRead(name).await<JsAny?>().toString())

    actual suspend fun write(name: String, bytes: ByteArray) {
        opfsWrite(name, Base64.encode(bytes)).await<JsAny?>()
    }

    actual suspend fun delete(name: String) {
        opfsDelete(name).await<JsAny?>()
    }
}
