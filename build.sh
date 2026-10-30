#!/usr/bin/env bash
# ==============================================================================
# SamPlayer - Script de Build & Deploy Nativo Automatizado
# Autor: Sam Moreno
# Licença: BSD 2-Clause License
# Homologado para: Debian GNU/Linux & Samsung Galaxy A26
# ==============================================================================

set -e

# Paleta de cores para terminal
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # No Color

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${CYAN}${BOLD}"
echo "=========================================================="
echo "      🎵 SamPlayer - Automação de Build & Deploy          "
echo "        Minimalist Native Audio Player (MP3/WAV/MIDI)     "
echo "=========================================================="
echo -e "${NC}"

# ==============================================================================
# 1. DETECÇÃO E EXPORTAÇÃO AUTOMÁTICA DO ANDROID SDK (EVITA FALHAS NO GRADLE)
# ==============================================================================
detect_and_export_android_sdk() {
    # Se ANDROID_HOME ou ANDROID_SDK_ROOT já estiverem configurados e válidos, mantém
    if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME" ]; then
        export ANDROID_SDK_ROOT="$ANDROID_HOME"
        echo -e "${GREEN}✓ ANDROID_HOME detectado no ambiente:${NC} $ANDROID_HOME"
        return 0
    elif [ -n "$ANDROID_SDK_ROOT" ] && [ -d "$ANDROID_SDK_ROOT" ]; then
        export ANDROID_HOME="$ANDROID_SDK_ROOT"
        echo -e "${GREEN}✓ ANDROID_SDK_ROOT detectado no ambiente:${NC} $ANDROID_SDK_ROOT"
        return 0
    fi

    # Candidatos padrão no Linux (com ~/Android/Sdk como padrão no Debian/Ubuntu)
    local SDK_CANDIDATES=(
        "$HOME/Android/Sdk"
        "$HOME/.android/sdk"
        "/usr/lib/android-sdk"
        "/opt/android-sdk"
        "/var/lib/android-sdk"
    )

    for candidate in "${SDK_CANDIDATES[@]}"; do
        if [ -d "$candidate" ]; then
            export ANDROID_HOME="$candidate"
            export ANDROID_SDK_ROOT="$candidate"
            echo -e "${GREEN}✓ Android SDK detectado automaticamente:${NC} $ANDROID_HOME"
            
            # Adiciona platform-tools ao PATH da sessão se existir
            if [ -d "$ANDROID_HOME/platform-tools" ]; then
                export PATH="$ANDROID_HOME/platform-tools:$PATH"
            fi
            
            # Gera local.properties caso não exista localmente para blindar o Gradle
            if [ ! -f "local.properties" ]; then
                echo "sdk.dir=$ANDROID_HOME" > local.properties
                echo -e "${GREEN}✓ Arquivo local.properties gerado apontando para o SDK.${NC}"
            fi
            return 0
        fi
    done

    echo -e "${YELLOW}⚠ Atenção: Android SDK não localizado nos caminhos padrão (~/Android/Sdk).${NC}"
    echo -e "${YELLOW}  Caso o build falhe, configure ANDROID_HOME ou use a opção de instalar ferramentas.${NC}"
}

detect_and_export_android_sdk

# ==============================================================================
# 2. DETECTOR DE DISTRIBUIÇÃO LINUX E INSTALADOR DE DEPENDÊNCIAS
# ==============================================================================
detect_distro() {
    if [ -f /etc/os-release ]; then
        # shellcheck source=/dev/null
        . /etc/os-release
        DISTRO_ID="${ID:-unknown}"
        DISTRO_NAME="${NAME:-Linux}"
    else
        DISTRO_ID="unknown"
        DISTRO_NAME="Linux"
    fi
}

install_system_dependencies() {
    detect_distro
    echo -e "${CYAN}==> Sistema detectado:${NC} ${BOLD}$DISTRO_NAME ($DISTRO_ID)${NC}"

    case "$DISTRO_ID" in
        debian|ubuntu|linuxmint|pop)
            echo -e "${CYAN}==> Instalando ferramentas essenciais para Debian/Ubuntu (apt-get)...${NC}"
            echo -e "Pacotes: OpenJDK 17, ADB (Android Tools), Unzip, Curl, Wget, Git"
            sudo apt-get update
            sudo apt-get install -y openjdk-17-jdk adb android-sdk-platform-tools-common unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no Debian!${NC}"
            ;;
        fedora|rhel|centos)
            echo -e "${CYAN}==> Instalando ferramentas para Fedora/RHEL (dnf)...${NC}"
            sudo dnf install -y java-17-openjdk-devel android-tools unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no Fedora!${NC}"
            ;;
        arch|manjaro)
            echo -e "${CYAN}==> Instalando ferramentas para Arch Linux (pacman)...${NC}"
            sudo pacman -S --needed jdk17-openjdk android-tools unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no Arch!${NC}"
            ;;
        opensuse*|suse)
            echo -e "${CYAN}==> Instalando ferramentas para openSUSE (zypper)...${NC}"
            sudo zypper install -y java-17-openjdk-devel android-tools unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no openSUSE!${NC}"
            ;;
        *)
            echo -e "${YELLOW}Distribuição não catalogada automaticamente ($DISTRO_NAME).${NC}"
            echo -e "Recomendado instalar via seu gerenciador de pacotes: OpenJDK 17 e android-tools (adb)."
            ;;
    esac
}

# ==============================================================================
# 3. VERIFICAÇÃO DO GRADLE WRAPPER
# ==============================================================================
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    chmod +x ./gradlew
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
else
    echo -e "${RED}[ERRO] Nem o Gradle Wrapper (./gradlew) nem 'gradle' foram encontrados.${NC}"
    echo -e "Execute a opção de instalar dependências para configurar o ambiente."
    exit 1
fi

echo -e "${GREEN}✓ Engine de build:${NC} $GRADLE_CMD"

# ==============================================================================
# 4. VERIFICAÇÃO DE DISPOSITIVOS USB (ADB)
# ==============================================================================
HAS_ADB=false
DEVICE_CONNECTED=false
DEVICE_NAME=""

if command -v adb >/dev/null 2>&1; then
    HAS_ADB=true
    DEVICES=$(adb devices 2>/dev/null | grep -v "List of devices" | grep "device$" | awk '{print $1}' || true)
    if [ -n "$DEVICES" ]; then
        DEVICE_CONNECTED=true
        FIRST_DEVICE=$(echo "$DEVICES" | head -n 1)
        DEVICE_MODEL=$(adb -s "$FIRST_DEVICE" shell getprop ro.product.model 2>/dev/null | tr -d '\r' || true)
        if [ -n "$DEVICE_MODEL" ]; then
            DEVICE_NAME="$DEVICE_MODEL ($FIRST_DEVICE)"
        else
            DEVICE_NAME="$FIRST_DEVICE"
        fi
    fi
fi

if [ "$DEVICE_CONNECTED" = true ]; then
    echo -e "${GREEN}✓ Celular USB detectado:${NC} ${BOLD}$DEVICE_NAME${NC}"
else
    echo -e "${YELLOW}ℹ Nenhum celular detectado via USB/ADB (ou depuração desativada).${NC}"
fi

echo ""

# ==============================================================================
# 5. EXECUÇÃO DE ARGUMENTOS OU MENU INTERATIVO
# ==============================================================================
CHOICE=""

if [ "$1" == "--build-only" ] || [ "$1" == "-b" ]; then
    CHOICE="1"
elif [ "$1" == "--install" ] || [ "$1" == "-i" ]; then
    CHOICE="2"
elif [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
    CHOICE="3"
elif [ "$1" == "--install-deps" ] || [ "$1" == "-d" ]; then
    CHOICE="4"
elif [ "$1" == "--test" ] || [ "$1" == "-t" ]; then
    CHOICE="5"
elif [ -n "$1" ]; then
    echo "Parâmetro desconhecido: $1"
    echo "Uso: ./build.sh [--build-only | --install | --clean | --install-deps | --test]"
    exit 1
fi

if [ -z "$CHOICE" ]; then
    echo -e "${BOLD}Selecione a ação desejada:${NC}"
    echo -e "  ${CYAN}1)${NC} ${BOLD}Compilar APK Debug direto (-x test)${NC} (rápido, sem USB)"
    if [ "$DEVICE_CONNECTED" = true ]; then
        echo -e "  ${CYAN}2)${NC} ${BOLD}Compilar e Instalar no celular via USB (${DEVICE_NAME})${NC}"
    else
        echo -e "  ${CYAN}2)${NC} Compilar e Instalar via USB (detectar ao conectar)"
    fi
    echo -e "  ${CYAN}3)${NC} Limpar cache e recompilar tudo (clean assembleDebug -x test)"
    echo -e "  ${CYAN}4)${NC} ${YELLOW}Instalar ferramentas do sistema (Debian / Linux: JDK 17, ADB)${NC}"
    echo -e "  ${CYAN}5)${NC} Executar suíte de testes unitários e Robolectric"
    echo -e "  ${CYAN}6)${NC} Sair"
    echo ""
    read -rp "Opção [1-6]: " USER_INPUT
    CHOICE="$USER_INPUT"
fi

echo ""

case "$CHOICE" in
    1)
        echo -e "${CYAN}==> Fundindo APK Debug direto no metal (pulando testes redundantes via -x test)...${NC}"
        $GRADLE_CMD assembleDebug -x test
        
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$APK_PATH" ]; then
            APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
            echo ""
            echo -e "${GREEN}${BOLD}✔ APK do SamPlayer gerado com sucesso!${NC}"
            echo -e "Arquivo: ${BOLD}$APK_PATH${NC} ($APK_SIZE)"
            echo -e "Pronto para transferência ou sideload direto no seu aparelho."
        else
            echo -e "${RED}[ERRO] Arquivo APK não encontrado em $APK_PATH${NC}"
            exit 1
        fi
        ;;

    2)
        echo -e "${CYAN}==> Compilando o APK do SamPlayer (assembleDebug -x test)...${NC}"
        $GRADLE_CMD assembleDebug -x test
        
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        if [ ! -f "$APK_PATH" ]; then
            echo -e "${RED}[ERRO] Falha ao encontrar o APK após compilação.${NC}"
            exit 1
        fi

        echo ""
        echo -e "${CYAN}==> Verificando conexão ADB...${NC}"
        if [ "$HAS_ADB" = false ]; then
            echo -e "${RED}[ERRO] 'adb' não está instalado ou não está no PATH.${NC}"
            echo -e "Instale via 'sudo apt-get install adb' ou use a opção 4 deste script."
            exit 1
        fi

        DEVICES=$(adb devices 2>/dev/null | grep -v "List of devices" | grep "device$" | awk '{print $1}' || true)
        if [ -z "$DEVICES" ]; then
            echo -e "${RED}[ERRO] Nenhum celular Android detectado com Depuração USB ativada.${NC}"
            echo -e "O APK compilado continua disponível em: ${BOLD}$APK_PATH${NC}"
            exit 1
        fi

        TARGET_DEVICE=$(echo "$DEVICES" | head -n 1)
        echo -e "${CYAN}==> Instalando SamPlayer no dispositivo [$TARGET_DEVICE]...${NC}"
        adb -s "$TARGET_DEVICE" install -r -d "$APK_PATH"

        echo ""
        echo -e "${GREEN}${BOLD}✔ SamPlayer instalado com sucesso no aparelho!${NC}"

        if [ -t 0 ]; then
            echo ""
            read -rp "Deseja abrir o SamPlayer agora no celular? (s/N): " LAUNCH_APP
            if [[ "$LAUNCH_APP" =~ ^[sSyY]$ ]]; then
                echo -e "${CYAN}==> Abrindo o SamPlayer...${NC}"
                adb -s "$TARGET_DEVICE" shell am start -n "com.aistudio.prismaplayer.vnkz/com.example.MainActivity"
            fi
        fi
        ;;

    3)
        echo -e "${CYAN}==> Limpando e recompilando tudo do zero (-x test)...${NC}"
        $GRADLE_CMD clean assembleDebug -x test
        echo -e "${GREEN}${BOLD}✔ Limpeza e recompilação concluídas com sucesso!${NC}"
        echo -e "APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
        ;;

    4)
        install_system_dependencies
        ;;

    5)
        echo -e "${CYAN}==> Executando suíte completa de testes unitários e Robolectric...${NC}"
        $GRADLE_CMD :app:testDebugUnitTest
        echo -e "${GREEN}${BOLD}✔ Todos os testes foram validados com êxito!${NC}"
        ;;

    6)
        echo "Operação cancelada."
        exit 0
        ;;

    *)
        echo -e "${RED}Opção inválida.${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${GREEN}Pronto!${NC}"
