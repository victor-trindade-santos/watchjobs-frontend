package com.watchjobs.ui.components

import androidx.compose.runtime.Composable

class PickedFile(val name: String, val bytes: ByteArray)

// Abre o seletor de arquivos da plataforma (Android: documentos; Desktop: janela; Web: input type=file).
// extensions: sem ponto e em minúsculas ("pdf", "docx"). Devolve a função que abre o seletor;
// o resultado chega em onPicked (null = cancelado).
@Composable
expect fun rememberFilePicker(extensions: List<String>, onPicked: (PickedFile?) -> Unit): () -> Unit
