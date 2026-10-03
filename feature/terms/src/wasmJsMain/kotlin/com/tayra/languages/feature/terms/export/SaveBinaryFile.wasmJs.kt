package com.tayra.languages.feature.terms.export

import kotlin.io.encoding.Base64

private fun downloadBase64(fileName: String, base64: String): Unit = js(
    """{
        const binary = atob(base64);
        const bytes = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
        const blob = new Blob([bytes], { type: 'application/octet-stream' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    }""",
)

actual suspend fun saveBinaryFile(baseName: String, extension: String, bytes: ByteArray) {
    downloadBase64("$baseName.$extension", Base64.encode(bytes))
}
