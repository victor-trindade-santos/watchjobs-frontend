package com.watchjobs.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

@OptIn(ExperimentalWasmJsInterop::class)
private fun openFileInput(accept: JsString, onFile: (JsString, JsAny) -> Unit): Unit = js(
    """(() => {
        const input = document.createElement('input');
        input.type = 'file';
        input.accept = accept;
        input.onchange = () => {
            const file = input.files && input.files[0];
            if (!file) return;
            file.arrayBuffer().then(buffer => onFile(file.name, new Uint8Array(buffer)));
        };
        input.click();
    })()"""
)

@OptIn(ExperimentalWasmJsInterop::class)
private fun lengthOf(array: JsAny): Int = js("array.length")

@OptIn(ExperimentalWasmJsInterop::class)
private fun byteAt(array: JsAny, index: Int): Int = js("array[index]")

@OptIn(ExperimentalWasmJsInterop::class)
@Composable
actual fun rememberFilePicker(extensions: List<String>, onPicked: (PickedFile?) -> Unit): () -> Unit {
    val callback by rememberUpdatedState(onPicked)
    val accept = extensions.joinToString(",") { ".$it" }
    return remember(accept) {
        {
            openFileInput(accept.toJsString()) { name, array ->
                val bytes = ByteArray(lengthOf(array)) { byteAt(array, it).toByte() }
                callback(PickedFile(name.toString(), bytes))
            }
        }
    }
}
