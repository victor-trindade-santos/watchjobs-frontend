package com.watchjobs.push

// Pede o token de push do aparelho. Só o Android com Firebase configurado entrega um token; nas outras plataformas não faz nada.
expect fun requestPushToken(onToken: (String) -> Unit)
