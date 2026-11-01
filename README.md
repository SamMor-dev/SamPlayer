# SamPlayer 🎵

[![License: BSD 2-Clause](https://img.shields.io/badge/License-BSD_2--Clause-blue.svg)](LICENSE)
[![Platform: Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?logo=android)](https://www.android.com/)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin_2.x-7F52FF.svg?logo=kotlin)](https://kotlinlang.org/)
[![UI: Jetpack Compose](https://img.shields.io/badge/UI-Jetpack_Compose_M3-4285F4.svg)](https://developer.android.com/jetpack/compose)

Bem-vindo ao **SamPlayer**! Este é um reprodutor de música local nativo para Android, ultra-minimalista, desenvolvido para oferecer **altíssimo desempenho, consumo mínimo de memória RAM e zero bloatware**.

O SamPlayer reproduz arquivos de áudio de alta fidelidade (**MP3**, **WAV**, **FLAC**, **M4A/AAC** e **OGG**) com uma interface limpa, moderna e reativa construída 100% com **Jetpack Compose (Material Design 3)**.

---

## ⚡ Começo Rápido (Para Novos Desenvolvedores)

Se você é novo no projeto e quer testá-lo ou modificá-lo rapidamente, siga estes 3 passos simples:

1. **Abra no Android Studio:**
   - Inicie o Android Studio, selecione **File > Open** e escolha a pasta raiz `SamPlayer`.
2. **Aguarde o Gradle Sync:**
   - O Android Studio baixará automaticamente as dependências e o SDK 36.
3. **Execute no Aparelho ou Emulador:**
   - Conecte seu celular via USB (com a Depuração USB ativada) ou abra um emulador e clique no botão verde **Play** (`Shift + F10`).

---

## 🗺️ Mapa do Código (Onde Encontrar Cada Coisa)

Para facilitar a navegação no projeto, aqui está a estrutura principal dos arquivos em `app/src/main/java/com/example/`:

```text
com/example/
├── data/
│   ├── local/        # Banco de dados Room (AppDatabase, DAOs, Entities)
│   ├── repository/   # Repositório de faixas e indexação do MediaStore
│   └── sample/       # Gerador de áudios de demonstração (SampleAudioGenerator)
├── model/
│   ├── AudioFormat.kt# Formatos suportados (MP3, WAV, FLAC, M4A, OGG)
│   └── Track.kt      # Modelo de domínio de cada faixa musical
├── player/
│   └── AudioPlayerEngine.kt # Motor de reprodução nativo (MediaPlayer)
└── ui/
    ├── screens/      # Telas principais (MainScreen)
    ├── components/   # Componentes visuais reutilizáveis (MiniPlayer, FullPlayer, Chips)
    └── theme/        # Temas, Cores e Tipografia (Theme.kt, Color.kt)
```

---

## 🚀 Filosofia de Engenharia (Anti-Bloat)

O SamPlayer foi construído seguindo princípios rigorosos de eficiência:
1. **Memória RAM Mínima:** Armazena apenas dados primitivos leves no banco local, evitando o carregamento pesado de capas de álbuns na heap da JVM.
2. **MediaStore Otimizado:** Lê apenas colunas essenciais (`_ID`, `TITLE`, `ARTIST`, `DURATION`, `DATA`, etc.) com fechamento imediato de cursores.
3. **Background Zero:** Nenhum serviço em segundo plano consome bateria ou CPU quando o app está fechado ou pausado.
4. **MVVM Limpo:** Sem injeção de dependência complexa; utiliza factories nativas e Coroutines mapeadas para `Dispatchers.IO`.
5. **100% Offline e Privado:** Sem anúncios, telemetria ou chamadas de rede.

---

## 🎧 Formatos de Áudio Suportados

| Formato | Extensão | Decodificador Nativo |
|:---|:---:|:---|
| **MP3** | `.mp3` | MPEG Layer III / MediaCodec |
| **WAV** | `.wav` | Linear PCM RIFF Uncompressed |
| **FLAC** | `.flac` | Free Lossless Audio Codec |
| **M4A / AAC** | `.m4a`, `.aac` | Advanced Audio Coding |
| **OGG** | `.ogg` | Ogg Vorbis / Opus Codec |

---

## 🎨 Guia de Customização (Modding)

Quer alterar o visual ou o comportamento do app? Aqui estão os arquivos que você pode editar:

| O que você quer mudar? | Onde mexer |
|:---|:---|
| 🎨 **Cores e Temas** | [`ui/theme/Color.kt`](app/src/main/java/com/example/ui/theme/Color.kt) e [`Theme.kt`](app/src/main/java/com/example/ui/theme/Theme.kt) |
| 🖥️ **Tela Principal** | [`ui/screens/MainScreen.kt`](app/src/main/java/com/example/ui/screens/MainScreen.kt) |
| 🎛️ **Player (Mini & Tela Cheia)** | [`ui/components/MiniPlayerBar.kt`](app/src/main/java/com/example/ui/components/MiniPlayerBar.kt) e [`FullPlayerDialog.kt`](app/src/main/java/com/example/ui/components/FullPlayerDialog.kt) |
| 🎵 **Lógica de Áudio** | [`player/AudioPlayerEngine.kt`](app/src/main/java/com/example/player/AudioPlayerEngine.kt) |

---

## 📦 Como Compilar via Terminal

Se preferir usar o terminal, você pode compilar o APK de debug diretamente:

```bash
# Dar permissão ao wrapper do Gradle
chmod +x gradlew

# Compilar o APK (pulando testes redundantes para ganho de velocidade)
./gradlew assembleDebug -x test

# O APK gerado estará em:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📄 Licença

Distribuído sob a licença **BSD 2-Clause**. Consulte o arquivo [`LICENSE`](LICENSE) para mais detalhes.

---

**Desenvolvido com 💙 por Sam Moreno**  
[Repositório no GitHub](https://github.com/morenodelimasamuel80)
