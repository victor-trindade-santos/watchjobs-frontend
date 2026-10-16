package com.watchjobs

import com.watchjobs.data.local.InMemoryKeyValueStore
import com.watchjobs.data.local.KeyValueStore
import com.watchjobs.data.local.TokenStorage
import com.watchjobs.data.local.createKeyValueStore
import com.watchjobs.data.remote.ApiClient
import com.watchjobs.data.remote.ApiJson
import com.watchjobs.data.remote.AppConfig
import com.watchjobs.data.remote.FakeBackendTransport
import com.watchjobs.data.remote.HttpTransport
import com.watchjobs.data.remote.createHttpTransport
import com.watchjobs.data.remote.platformDemoMode
import com.watchjobs.data.repository.ApplicationRepository
import com.watchjobs.data.repository.AuthRepository
import com.watchjobs.data.repository.SavedJobRepository

// Injeção de dependências manual: cria uma vez o cliente HTTP, o armazenamento e os repositórios.
// No modo demonstração, o HTTP é trocado pelo backend simulado em memória e o token fica só na memória.
class AppContainer(
    val demoMode: Boolean = platformDemoMode(),
    transport: HttpTransport = if (demoMode) FakeBackendTransport() else createHttpTransport(),
    store: KeyValueStore = if (demoMode) InMemoryKeyValueStore() else createKeyValueStore(),
    baseUrl: String = if (demoMode) "demo://watchjobs" else AppConfig.baseUrl
) {
    val tokenStorage = TokenStorage(store, ApiJson)
    val api = ApiClient(transport, tokenStorage, baseUrl)
    val authRepository = AuthRepository(api, tokenStorage)
    val savedJobRepository = SavedJobRepository(api)
    val applicationRepository = ApplicationRepository(api)
}
