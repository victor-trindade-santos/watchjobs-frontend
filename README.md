# WatchJobs: frontend

## Sumário

- [1. Projeto e grupo](#1-projeto-e-grupo)
- [2. Repositórios](#2-repositórios)
- [3. Integrante e entidades](#3-integrante-e-entidades)
- [4. O que a aplicação faz](#4-o-que-a-aplicação-faz)
   - [4.1 Vaga salva × candidatura: qual é a diferença?](#41-vaga-salva--candidatura-qual-é-a-diferença)
- [5. Tecnologias](#5-tecnologias)
- [6. Camadas e pastas](#6-camadas-e-pastas)
- [7. Instalação, execução e acesso](#7-instalação-execução-e-acesso)
   - [7.0 Pré-requisitos](#70-pré-requisitos)
   - [7.1 Integrado ao backend (dados reais no PostgreSQL)](#71-integrado-ao-backend-dados-reais-no-postgresql)
   - [7.2 Sem o backend (modo demonstração)](#72-sem-o-backend-modo-demonstração)
   - [7.3 Testes](#73-testes)
- [Checklist dos requisitos da prova](#checklist-dos-requisitos-da-prova)
   - [Parte 2: frontend](#parte-2-frontend)
   - [Critérios de avaliação](#critérios-de-avaliação)

## 1. Projeto e grupo

**WatchJobs.** Grupo: individual.

## 2. Repositórios

- Backend: https://github.com/victor-trindade-santos/watchjobs-backend
- Frontend: https://github.com/victor-trindade-santos/watchjobs-frontend

## 3. Integrante e entidades

| Integrante | Responsável por |
|---|---|
| Victor Trindade dos Santos | Entidades **SavedJob** (vaga salva) e **Application** (candidatura), além da autenticação |

## 4. O que a aplicação faz

Quem procura emprego perde o controle de quais vagas lhe interessaram, onde já se candidatou, em que etapa está cada processo e quando é a próxima entrevista. O WatchJobs centraliza isso: registra as vagas de interesse e as candidaturas, com etapa, status, datas e entrevistas, e tem busca nas duas listas.

O mesmo código roda no **Android**, no **Desktop** e na **Web**:

- **Login e cadastro**, com os erros de validação do backend mostrados embaixo de cada campo.
- **Buscar vagas:** vagas reais coletadas pelo motor do backend (Greenhouse e Adzuna), com busca por cargo ou empresa e filtros de local, modo, nível e contrato na lateral esquerda (no celular, acima da lista). Cada cartão mostra a fonte ("via ...") e quando a vaga sai da busca ("Expira em N dias"). **Visualizar** abre os Detalhes da vaga e **Salvar** copia a vaga para Vagas salvas, onde ela não expira.
- **Detalhes da vaga:** a vaga completa dentro do WatchJobs, sem sair do app: descrição dividida em seções (sobre a vaga, o que você vai fazer, requisitos, diferenciais, benefícios), skills obrigatórias e desejáveis e outras informações da fonte. Tem **Salvar vaga** e **Acessar página original** (o link do anúncio).
- **Vagas salvas:** lista com busca por cargo ou empresa, indicadores e formulário com **Gravar** e **Limpar Campos**, além de editar e excluir. Na edição de uma vaga salva pela busca, aparecem também a descrição, as skills e as demais informações que vieram da fonte.
- **Candidaturas:** quadro com as colunas **Vagas salvas** (vagas ainda sem candidatura), Candidatadas, Em processo, Propostas e Encerradas, com busca e formulário com **Gravar** e **Limpar Campos**, além de editar e excluir. Tocar num cartão da coluna Vagas salvas abre a edição da vaga. Criar ou editar uma candidatura atualiza o status da vaga.

### 4.1 Vaga salva × candidatura: qual é a diferença?

São duas etapas diferentes da busca por emprego, por isso cada uma tem a sua aba e o seu formulário.

| | **Vaga salva** (aba **Vagas salvas**) | **Candidatura** (aba **Candidaturas**) |
|---|---|---|
| O que representa | Uma **oportunidade** que chamou sua atenção. Você ainda pode nem ter se candidatado | O **processo seletivo** que começou quando você se candidatou àquela vaga |
| Pergunta que responde | "Que vaga é essa?" | "Como está indo a minha candidatura?" |
| O que se preenche | Cargo, empresa, link, local, modo de trabalho, nível, contrato, faixa salarial e anotações | Etapa (Triagem → Proposta), status, data de início e de término, próxima entrevista, notas e feedback da empresa |
| Quando é criada | Assim que você encontra a vaga (botão **+ Nova vaga**) | Só quando você se candidata a uma vaga **já salva** (**+ Nova candidatura** ou **Candidatar-se** na lista de vagas) |
| Como aparece | Lista com busca, faixa salarial e etiquetas de status; sem candidatura, também na coluna Vagas salvas do quadro | Quadro com colunas Candidatadas, Em processo, Propostas e Encerradas |

**Como as duas se relacionam (1 vaga → no máximo 1 candidatura):**

1. **Salve a vaga.** Ela aparece em **Vagas salvas** e na primeira coluna do quadro de **Candidaturas**, com o status "Salva" ou "Planejando se candidatar".
2. **Candidate-se.** No formulário de candidatura, só aparecem as vagas que ainda não têm candidatura. Ao gravar, a vaga ganha a etiqueta "Com candidatura" e passa para "Candidatado".
3. **Acompanhe o processo.** Editar a candidatura (etapa, entrevista, status) também atualiza o status da vaga. Mudar para "Em processo" move o cartão para a coluna seguinte do quadro.
4. **Exclua a candidatura.** A vaga continua salva e volta para "Salva".
5. **Exclua a vaga.** A candidatura dela é excluída junto. O app avisa isso antes de confirmar.

**Exemplo:** você vê a vaga "Desenvolvedor Kotlin" na ACME e **salva**. Uma semana depois, envia o currículo e cria a **candidatura**. A partir daí, a candidatura registra a entrevista com o RH no dia 22, a mudança para "Teste técnico" e o feedback recebido. Na lista de vagas, você vê de relance em que pé está cada uma.

## 5. Tecnologias

Kotlin Multiplatform 2.4, Compose Multiplatform 1.11 (Material 3), ViewModel multiplataforma (`lifecycle-viewmodel-compose`), kotlinx.serialization, kotlinx-datetime e kotlinx.coroutines. O HTTP usa **clientes nativos, sem Ktor**: `HttpURLConnection` no Android, `java.net.http.HttpClient` no Desktop e `fetch()` na Web. Gradle Kotlin DSL com Android Gradle Plugin 9.

## 6. Camadas e pastas

Código comum em `shared/src/commonMain/kotlin/com/watchjobs/`:

| Camada | Pasta | O que tem |
|---|---|---|
| Apresentação | `ui/auth/`, `ui/jobs/`, `ui/savedjob/`, `ui/application/`, `ui/components/`, `ui/theme/` | Telas, formulários, componentes (logo, campos, listas de seleção, quadro) e o tema |
| Estado | `viewmodel/` | ViewModels com `StateFlow` (estado do formulário, erros por campo, carregamento) |
| Dados | `data/repository/`, `data/remote/`, `data/dto/`, `data/local/` | Repositórios, `ApiClient` e `HttpTransport`, DTOs e `TokenStorage` |
| Domínio | `model/` | `SavedJob`, `Application`, `ApplicationStage`, `JobPosting` (vaga do corpus), `User` e enums com rótulos em português |
| Navegação e injeção | `App.kt`, `AppContainer.kt` | `sealed interface Screen` + `when`, e a criação manual das dependências |

Código de cada plataforma (`expect`/`actual`):

| Plataforma | Pasta | O que tem |
|---|---|---|
| Android | `shared/src/androidMain/`, `androidApp/` | `HttpURLConnection`, `SharedPreferences`, `MainActivity` e o `AndroidManifest.xml` |
| Desktop | `shared/src/jvmMain/`, `desktopApp/` | `java.net.http.HttpClient`, `java.util.prefs` e a janela |
| Web | `shared/src/wasmJsMain/`, `webApp/` | `fetch()`, `localStorage` e o `index.html` |

## 7. Instalação, execução e acesso

### 7.0 Pré-requisitos

1. **Java 21.** Confira com `java -version`.
2. **Android SDK**, só para rodar no Android. Informe o caminho pela variável `ANDROID_HOME` ou pelo `sdk.dir` no `local.properties`.
3. **VS Code com a extensão REST Client** (autor: Huachao Mao), opcional. O app não precisa dela, mas ela executa o `requests.http` do backend para testar a API diretamente. Para instalar:
   - No VS Code, abra **Extensões** (`Ctrl+Shift+X`), busque **REST Client** e clique em **Instalar**.
   - Pelo terminal: `code --install-extension humao.rest-client`.
4. **Clonar o repositório:**

```powershell
git clone https://github.com/victor-trindade-santos/watchjobs-frontend.git
cd watchjobs-frontend
```

O app pode rodar de duas formas.

### 7.1 Integrado ao backend (dados reais no PostgreSQL)

É a forma completa: o app conversa com a API e os dados ficam gravados no banco.

**1º passo: suba o backend antes do app.** É ele que acessa o banco de dados. Sem ele no ar, o app mostra "Não foi possível conectar ao servidor". No repositório [watchjobs-backend](https://github.com/victor-trindade-santos/watchjobs-backend), com o Docker aberto:

```powershell
cd watchjobs-backend
.\gradlew.bat bootRun
```

Espere aparecer `Tomcat started on port 8081` no log. O `bootRun` também sobe o PostgreSQL pelo Docker Compose e aplica as migrations. Para a aba **Buscar vagas** ter conteúdo, rode também uma coleta do motor (seção 7.3 do README do backend).

**2º passo: em outro terminal, rode o app** na plataforma desejada:

```powershell
cd watchjobs-frontend

# Desktop
.\gradlew.bat :desktopApp:run

# Web (abre em http://localhost:8080, a origem liberada no CORS do backend)
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun

# Android: gera o APK e instala no emulador já ligado
.\gradlew.bat :androidApp:assembleDebug
adb install -r androidApp\build\outputs\apk\debug\androidApp-debug.apk
```

No emulador Android, o app acessa o backend pelo endereço `10.0.2.2:8081`, que é o `localhost` do PC visto de dentro do emulador. Num celular de verdade, troque o endereço em `shared/src/androidMain/.../UrlConnectionTransport.kt` pelo IP do PC na mesma rede Wi-Fi.

**Primeiro acesso:** na tela de login, clique em **Criar conta**, cadastre-se e entre com o e-mail e a senha.

### 7.2 Sem o backend (modo demonstração)

Para conhecer o app sem instalar Docker nem subir o backend. O app usa um **backend simulado em memória** (`FakeBackendTransport`), com as mesmas rotas, validações, regras de status e mensagens de erro da API. Uma faixa vermelha no topo indica o modo. Os dados somem ao fechar o app.

```powershell
# Desktop
.\gradlew.bat :desktopApp:run -Pdemo

# Web: rode o comando abaixo e abra http://localhost:8080/?demo no navegador
.\gradlew.bat :webApp:wasmJsBrowserDevelopmentRun
```

**Acesso pronto:** `demo@watchjobs.com`, senha `demo1234`, que já tem vagas e candidaturas de exemplo. Também dá para criar uma conta nova em **Criar conta**.

O modo demonstração existe no Desktop e na Web. O Android sempre usa o backend. Ele cobre o escopo da prova e uma amostra fixa de 4 vagas na aba **Buscar vagas**; as demais funcionalidades novas só aparecem com o backend real.

### 7.3 Testes

```powershell
.\gradlew.bat :shared:jvmTest
```

Esse comando roda os testes de formatação (datas e valores), o fluxo de **Buscar vagas** no modo demonstração (`JobSearchFlowTest`) e o **teste integrado**, que abre o app de verdade e faz o fluxo completo: cadastro, login, CRUD das duas entidades, busca, regras de status, exclusões e logout. As capturas de tela ficam em `shared/build/screenshots`.

- **Em modo demonstração**, o fluxo roda sempre.
- **Contra o backend real**, o fluxo só roda se o backend estiver no ar em `localhost:8081`. Com o backend fora do ar, essa parte é ignorada.

## Checklist dos requisitos da prova

Como este repositório atende à **Parte 2 (frontend, 2,0 pontos)** e aos critérios de avaliação. Os caminhos partem de `shared/src/commonMain/kotlin/com/watchjobs/`.

### Parte 2: frontend

- [x] **Kotlin Multiplatform + Compose Multiplatform para Android, Desktop e Web:** as telas, o estado e a lógica ficam no código comum (`shared/src/commonMain`). Cada plataforma tem o próprio módulo: [`androidApp/`](androidApp), [`desktopApp/`](desktopApp) e [`webApp/`](webApp) (Wasm). O HTTP e o armazenamento do token são nativos de cada plataforma, por `expect`/`actual`, sem Ktor.

- [x] **2a. Entidades do projeto (0,5):** duas entidades do integrante, cada uma com mais de 5 propriedades além do id e pelo menos 2 tipos da lista além do tipo do id.
  - [`SavedJob`](shared/src/commonMain/kotlin/com/watchjobs/model/SavedJob.kt): 21 propriedades além do id (`Long`). Tipos **String** (cargo, empresa, URL, local, moeda, notas, descrição), **Double** (`salaryMin`, `salaryMax`), **LocalDateTime** (`savedAt`, `statusUpdatedAt`) e **List** (`skills`).
  - [`Application`](shared/src/commonMain/kotlin/com/watchjobs/model/Application.kt): 11 propriedades além do id (`Int`). Tipos **String** (status, notas, feedback), **Int** (`stageId`), **LocalDate** (`startDate`, `endDate`) e **LocalDateTime** (`interviewAt`).
  - As duas são `@Serializable` (kotlinx.serialization) e usam as datas do kotlinx-datetime. Os enums têm rótulos em português.

- [x] **2b. Formulário por entidade com Gravar e Limpar Campos (1,5):**
  - [`SavedJobFormScreen`](shared/src/commonMain/kotlin/com/watchjobs/ui/savedjob/SavedJobFormScreen.kt): cargo, empresa, URL, cidade, estado, país, modo de trabalho, nível, contrato, salários, moeda, status e notas, com listas de seleção para os enums.
  - [`ApplicationFormScreen`](shared/src/commonMain/kotlin/com/watchjobs/ui/application/ApplicationFormScreen.kt): vaga, etapa, status, datas de início e término, entrevista, notas e feedback.
  - Nos dois formulários:
    - **Gravar** envia `POST` (novo) ou `PUT` (edição) ao backend e volta para a lista com uma confirmação.
    - **Limpar Campos** esvazia os campos editáveis e os erros.
    - Os erros de validação do backend (400) aparecem embaixo de cada campo.
  - Cada entidade tem também a sua lista, com busca, edição e exclusão com confirmação:
    - [`SavedJobListScreen`](shared/src/commonMain/kotlin/com/watchjobs/ui/savedjob/SavedJobListScreen.kt).
    - [`ApplicationListScreen`](shared/src/commonMain/kotlin/com/watchjobs/ui/application/ApplicationListScreen.kt), em formato de quadro por status, com a coluna Vagas salvas para as vagas ainda sem candidatura.
  - O cadastro de usuário ([`SignupScreen`](shared/src/commonMain/kotlin/com/watchjobs/ui/auth/AuthScreens.kt)) também tem **Cadastrar** e **Limpar Campos**.

- [x] **Integração com o backend da Parte 1:**
  - Login (`/signin`) e cadastro (`/signup`), com o token JWT enviado no cabeçalho `Authorization` das rotas protegidas.
  - Quando o token expira (401), o app volta para o login.
  - O [`ApiClient`](shared/src/commonMain/kotlin/com/watchjobs/data/remote/ApiClient.kt) usa os mesmos DTOs e o mesmo formato de erro `{ code, message, details }` da API.

### Critérios de avaliação

- [x] **3a. Professor como colaborador:** `antoniorcn@hotmail.com` foi convidado neste repositório e no do backend.
- [x] **3b e 3c. Autoria no histórico Git:** o histórico tem commits por etapa, feitos pelo integrante.
- [x] **3d. Compila e roda:**
  - Integrado ao backend, conforme a seção 7.1.
  - Sem backend, em modo demonstração, conforme a seção 7.2.
  - O `.\gradlew.bat :shared:jvmTest` abre o app e faz o fluxo completo: cadastro, login, CRUD das duas entidades, busca, Gravar, Limpar Campos, exclusões e logout. As capturas de tela ficam em `shared/build/screenshots`.
- [x] **3e. README com os 7 itens:**
  - i. Nome do projeto e do grupo: seção 1.
  - ii. Links dos repositórios: seção 2.
  - iii. Integrantes e entidades: seção 3.
  - iv. O que a aplicação faz: seção 4.
  - v. Tecnologias: seção 5.
  - vi. Camadas e pastas: seção 6 (a do backend está no README do backend).
  - vii. Instalação, execução e acesso: seção 7.

