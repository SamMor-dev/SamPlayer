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

## 📦 Como Compilar e Instalar

### Opção 1: Usando o Script Automatizado `build.sh` (Recomendado)

O repositório inclui o script [`build.sh`](build.sh) na raiz do projeto. Ele detecta se o seu dispositivo (ex: Galaxy A26) está conectado via USB com depuração ativada e oferece opções para compilar e instalar automaticamente ou apenas gerar o APK:

```bash
# 1. Torne o script executável (caso ainda não esteja)
chmod +x build.sh

# 2. Execute o assistente de compilação
./build.sh
```

O script oferece um menu interativo:
- `1) Compilar APK Debug`
- `1) Compilar APK Debug direto (-x test)` (pula testes redundantes e gera o APK rapidamente)
- `2) Compilar e Instalar no celular via USB (adb install)`
- `3) Limpar cache e recompilar tudo (clean assembleDebug -x test)`
- `4) Instalar ferramentas do sistema (Debian / Linux: JDK 17, ADB)`
- `5) Executar suíte de testes unitários e Robolectric`

O script detecta automaticamente o diretório padrão do Android SDK no Linux (como `~/Android/Sdk`), exporta as variáveis `ANDROID_HOME` e `ANDROID_SDK_ROOT` e cria o arquivo `local.properties` caso necessário.

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
