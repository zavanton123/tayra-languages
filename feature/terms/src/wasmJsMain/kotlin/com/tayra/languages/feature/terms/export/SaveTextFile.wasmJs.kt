package com.tayra.languages.feature.terms.export

private fun downloadText(fileName: String, content: String): Unit = js(
    """{
        const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
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

actual suspend fun saveTextFile(baseName: String, extension: String, content: String) {
    downloadText("$baseName.$extension", content)
}
