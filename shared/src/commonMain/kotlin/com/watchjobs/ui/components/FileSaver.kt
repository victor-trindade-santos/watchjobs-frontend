package com.watchjobs.ui.components

import androidx.compose.runtime.Composable

// Salva um arquivo gerado pelo app (PDF/DOCX dos documentos): Desktop abre "Salvar como", Web baixa pelo navegador,
// Android abre o seletor de destino do sistema. onResult(false) = cancelado ou falhou.
@Composable
expect fun rememberFileSaver(onResult: (saved: Boolean) -> Unit): (fileName: String, mimeType: String, bytes: ByteArray) -> Unit
