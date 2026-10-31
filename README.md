# SamPlayer 🎵

[![License: BSD 2-Clause](https://img.shields.io/badge/License-BSD_2--Clause-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android)](https://www.android.com/)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin_2.x-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg)](https://developer.android.com/jetpack/compose)

**SamPlayer** é um reprodutor de música local nativo para Android, ultra-minimalista e focado em **altíssimo desempenho, consumo mínimo de memória RAM e zero bloatware de segundo plano**. 

Projetado especificamente para reproduzir arquivos **MP3**, **WAV** e **MIDI (.mid)** sem sobrecarregar o hardware e separando as faixas estritamente por formato em uma interface limpa, moderna e reativa construída com **Jetpack Compose (Material Design 3)**.

> 📱 **Dispositivo Homologado & Testado:**
> O aplicativo foi exaustivamente testado e otimizado no **Samsung Galaxy A26** (One UI / Android 14+ / 120Hz), garantindo rolagem a 60/120 FPS cravados, tempo de resposta instantâneo, consumo de RAM residual na casa de megabytes de um dígito e reprodução fluida do sintetizador General MIDI nativo do Android.

---

## 🚀 Filosofia de Engenharia (Anti-Bloat)

1. **Memória RAM Praticamente Nula (Sub-Kilobyte por Faixa):**
   - Reprodutores convencionais alocam dezenas de megabytes na heap da JVM decodificando capas de álbum (`Bitmap`, tags ID3 pesadas).
   - O **SamPlayer** armazena apenas dados primitivos leves (`Long`, `String`) no banco de dados e utiliza renderização vetorial e Canvas acelerado por hardware para os visualizadores de áudio.
2. **Projeção Estrita no MediaStore (`ContentResolver`):**
   - As varreduras de armazenamento solicitam exclusivamente 7 colunas primitivas (`_ID`, `TITLE`, `ARTIST`, `DURATION`, `DATA`, `SIZE`, `DATE_MODIFIED`), fechando os cursores imediatamente em blocos seguros `use {}` para evitar vazamentos de memória (`CursorWindow` leaks).
3. **Background Zero & Liberação de Codecs de Hardware:**
   - Nenhum serviço persistente, daemon ou Worker consome ciclos de CPU sem necessidade.
   - Quando a reprodução é pausada ou o app é fechado, o `MediaPlayer` nativo executa `release()` imediatamente e as corrotinas de polling são canceladas, devolvendo o controle total de áudio ao sistema operacional.
4. **Arquitetura Vanilla MVVM (Sem Dependências Pesadas):**
   - Sem frameworks de injeção de dependência complexos (Dagger/Hilt) que aumentam o tempo de compilação e o consumo de memória na inicialização.
   - Utilização de `ViewModelProvider.Factory` nativo, Kotlin Coroutines e `StateFlow` estritamente mapeados para `Dispatchers.IO` nas operações de I/O (zero ANRs).
5. **Privacidade Total & Modo Offline:**
   - 100% offline. Zero telemetria, zero anúncios, zero conexões de rede externas.
   - Suporte completo ao **Storage Access Framework (SAF)** do Android, permitindo importar faixas individuais sem necessitar de permissões amplas de armazenamento.

---

## 🎧 Formatos de Áudio Suportados

| Formato | Extensão | Decodificador / Mecanismo | Características no SamPlayer |
|:---|:---:|:---|:---|
| **MP3** | `.mp3` | Android MediaCodec / MPEG Layer III | Indexação rápida, identificação de tags essenciais |
| **WAV** | `.wav` | Linear PCM RIFF Uncompressed | Reprodução de áudio sem perdas (lossless) sem delay |
| **MIDI** | `.mid`, `.midi` | Android Internal Synth (General MIDI) | Arquivos ultra-leves (~1-50 KB), sintetizados em tempo real |

As músicas são segregadas em abas dedicadas (**All**, **MP3**, **WAV**, **MIDI**, **Favorites** e **Folders**), mantendo a biblioteca sempre organizada.

---

## 🛠️ Tecnologias & Arquitetura

- **Linguagem:** 100% Kotlin (versão 2.x)
- **UI & Design:** Jetpack Compose + Material Design 3 (Tema escuro Obsidian com destaque Neon Cyan/Violet)
- **Persistência Local:** AndroidX Room Database com índices dedicados em `format_name`, `is_favorite` e `folder_name`
- **Áudio Nativo:** `android.media.MediaPlayer` e `android.media.AudioManager` (gerenciamento robusto de `AudioFocusRequest`)
- **Concorrência:** Kotlin Coroutines (`Dispatchers.IO` & `Dispatchers.Main.immediate`) + `StateFlow`
- **Build System:** Gradle (Kotlin DSL - `.gradle.kts`) com Version Catalog (`libs.versions.toml`)

---

## 🛠️ Como Abrir, Compilar e Criar Mods no Android Studio (Recomendado)

O **Android Studio** é a forma mais prática, visual e rápida de compilar o SamPlayer e criar suas próprias modificações (**mods**), pois ele já inclui seu próprio JDK integrado e gerencia as licenças do SDK 36 graficamente.

### 1. Instalando o Android Studio no Linux (Debian / Ubuntu)

Você pode instalar o Android Studio automaticamente usando o script incluso no projeto:

```bash
chmod +x InstallStudio.sh
./InstallStudio.sh
```
Selecione a opção **`1) Instalar o Android Studio no Linux / Debian`**. O assistente fará o download da versão oficial mais recente, configurará os atalhos no seu menu de aplicativos e inicializará o SDK.

*(Alternativamente, você pode baixar pelo site oficial: [developer.android.com/studio](https://developer.android.com/studio)).*

### 2. Abrindo o Projeto no Android Studio

1. Inicie o Android Studio.
2. Na tela inicial (ou no menu superior), clique em **File > Open**.
3. Selecione a **pasta raiz do SamPlayer** (onde está o arquivo `settings.gradle.kts` e `build.gradle.kts`).
4. O Android Studio iniciará o **Gradle Sync** automático. Se aparecer um aviso em azul no canto inferior (`Install missing platform(s) and sync project`), clique nele para baixar o SDK 36.1 com apenas 1 clique.

### 3. Executando no Celular (ex: Samsung Galaxy A26)

1. Conecte seu smartphone via cabo USB e certifique-se de que a **Depuração USB** está ativada nas Opções do Desenvolvedor.
2. Seu aparelho aparecerá na barra superior da IDE (ex: `Samsung SM-A266...`).
3. Clique no botão verde de **Play** (`Shift + F10`) no topo para compilar e iniciar o SamPlayer diretamente no celular.
4. Para apenas gerar o arquivo de instalação, vá em **Build > Build Bundle(s) / APK(s) > Build APK(s)**. O APK final ficará em `app/build/outputs/apk/debug/app-debug.apk`.

### 🎨 Guia Rápido de Modding (Como Modificar o Projeto)

O SamPlayer foi escrito com código limpo e desacoplado para você poder customizar à vontade:

| O que você quer customizar? | Arquivo para editar |
|:---|:---|
| **Cores, Temas e Paleta Neon** | `app/src/main/java/com/example/ui/theme/Color.kt` e `Theme.kt` |
| **Telas e Interface do Usuário (Compose)** | `app/src/main/java/com/example/ui/screens/MainScreen.kt` |
| **Mini Player e Player Expandido** | `app/src/main/java/com/example/ui/components/MiniPlayerBar.kt` e `FullPlayerDialog.kt` |
| **Lógica do Motor de Áudio e Sintetizador** | `app/src/main/java/com/example/player/AudioPlayerEngine.kt` |
| **Formatos e Filtros de Áudio (MP3/WAV/MIDI)** | `app/src/main/java/com/example/model/AudioFormat.kt` |
| **Estrutura do Banco de Dados Local** | `app/src/main/java/com/example/data/local/` (Room Entities & DAOs) |
| **Ícone Vetorial Aberto (SVG)** | [`app_icon.svg`](app_icon.svg) (100% editável no Figma, Inkscape ou Illustrator) |

---

## 📦 Como Compilar via Terminal

### Opção 1: Usando o Script Automatizado `InstallStudio.sh`

```bash
chmod +x InstallStudio.sh
./InstallStudio.sh
```

Menu de opções do script:
- `1) Instalar o Android Studio no Linux / Debian`
- `2) Compilar APK Debug direto (-x test)` (pula testes redundantes e gera o APK rapidamente)
- `3) Compilar e Instalar no celular via USB (Samsung Galaxy A26)`
- `4) Limpar cache e recompilar tudo (clean assembleDebug -x test)`
- `5) Instalar ferramentas de linha de comando no Debian (OpenJDK 21, ADB)`
- `6) Executar testes unitários e Robolectric`

### Opção 2: Compilação Manual via Gradle Wrapper

```bash
# Dar permissão de execução ao wrapper
chmod +x gradlew

# Compilar o APK em modo Debug pulando testes unitários (-x test)
./gradlew assembleDebug -x test

# Instalar no dispositivo conectado via ADB
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

O arquivo gerado estará localizado em:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 🧪 Testes Automatizados

O projeto conta com uma suíte de testes unitários rápidos e testes de integração de contexto via **Robolectric**:

```bash
./gradlew :app:testDebugUnitTest
```

---

## 📄 Licença

Este projeto é software livre distribuído sob a licença **BSD 2-Clause**.

```text
Copyright (c) 2026, Sam Moreno
All rights reserved.
```

Consulte o arquivo [`LICENSE`](LICENSE) para obter o texto completo dos termos legais.

---

**Autor:** Sam Moreno  
**Repositório:** [SamPlayer no GitHub](https://github.com/morenodelimasamuel80)
