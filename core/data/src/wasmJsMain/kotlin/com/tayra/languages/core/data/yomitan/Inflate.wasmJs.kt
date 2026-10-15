package com.tayra.languages.core.data.yomitan

import kotlinx.coroutines.await
import kotlin.io.encoding.Base64
import kotlin.js.Promise

/** Inflates base64 raw deflate data with the browser's DecompressionStream, answering base64. */
private fun inflateBase64(data: String): Promise<JsString> = js(
    """(function () {
        var binary = atob(data);
        var bytes = new Uint8Array(binary.length);
        for (var i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        var stream = new Blob([bytes]).stream().pipeThrough(new DecompressionStream('deflate-raw'));
        return new Response(stream).arrayBuffer().then(function (buffer) {
            var out = new Uint8Array(buffer), text = '';
            for (var j = 0; j < out.length; j += 0x8000) text += String.fromCharCode.apply(null, out.subarray(j, j + 0x8000));
            return btoa(text);
        });
    })()""",
)

internal actual suspend fun inflateRaw(data: ByteArray, size: Int): ByteArray =
    Base64.decode(inflateBase64(Base64.encode(data)).await<JsString>().toString())
