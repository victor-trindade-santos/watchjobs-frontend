package com.watchjobs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@OptIn(ExperimentalWasmJsInterop::class)
private fun newBytes(size: Int): JsAny = js("new Uint8Array(size)")

@OptIn(ExperimentalWasmJsInterop::class)
private fun setByte(array: JsAny, index: Int, value: Int): Unit = js("array[index] = value")

@OptIn(ExperimentalWasmJsInterop::class)
private fun downloadBytes(array: JsAny, name: JsString, mime: JsString): Unit = js(
    """(() => {
        const blob = new Blob([array], { type: mime });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = name;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        setTimeout(() => URL.revokeObjectURL(url), 1000);
    })()"""
)

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun rememberFileSaver(onResult: (saved: Boolean) -> Unit): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit {
    val callback by rememberUpdatedState(onResult)
    return remember {
        { fileName, mimeType, bytes ->
            val array = newBytes(bytes.size)
            bytes.forEachIndexed { index, byte -> setByte(array, index, byte.toInt() and 0xFF) }
            val saved = runCatching { downloadBytes(array, fileName.toJsString(), mimeType.toJsString()) }.isSuccess
            callback(saved)
        }
    }
}
