#!/usr/bin/env bash
# ==============================================================================
# SamPlayer - Assistente de Instalação do Android Studio & Build Automatizado
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
echo "======================================================================"
echo "    🛠️  InstallStudio.sh - Android Studio & Build do SamPlayer       "
echo "       Minimalist Native Audio Player (MP3/WAV/MIDI) • Sam Moreno     "
echo "======================================================================"
echo -e "${NC}"

# ==============================================================================
# 1. DETECÇÃO DA DISTRIBUIÇÃO LINUX
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

detect_distro

# ==============================================================================
# 2. DETECÇÃO E EXPORTAÇÃO AUTOMÁTICA DO ANDROID SDK
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

    # Candidatos padrão no Linux (com ~/Android/Sdk como padrão oficial do Android Studio)
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
            
            if [ -d "$ANDROID_HOME/platform-tools" ]; then
                export PATH="$ANDROID_HOME/platform-tools:$PATH"
            fi
            
            if [ ! -f "local.properties" ]; then
                echo "sdk.dir=$ANDROID_HOME" > local.properties
                echo -e "${GREEN}✓ Arquivo local.properties gerado apontando para o SDK.${NC}"
            fi
            return 0
        fi
    done

    # Se ainda não existir nenhuma pasta do SDK, prepara ~/Android/Sdk para o Android Studio
    mkdir -p "$HOME/Android/Sdk"
    export ANDROID_HOME="$HOME/Android/Sdk"
    export ANDROID_SDK_ROOT="$HOME/Android/Sdk"
    if [ ! -f "local.properties" ]; then
        echo "sdk.dir=$ANDROID_HOME" > local.properties
    fi
    echo -e "${GREEN}✓ Diretório padrão do usuário inicializado:${NC} $ANDROID_HOME"
}

detect_and_export_android_sdk

# ==============================================================================
# 3. ACEITE AUTOMÁTICO DAS LICENÇAS DO ANDROID SDK (36 / 36.1)
# ==============================================================================
auto_accept_sdk_licenses() {
    if [ -z "$ANDROID_HOME" ] || [ ! -d "$ANDROID_HOME" ]; then
        return 0
    fi

    local LICENSES_DIR="$ANDROID_HOME/licenses"

    if [ ! -w "$ANDROID_HOME" ]; then
        echo -e "${YELLOW}ℹ O diretório do SDK ($ANDROID_HOME) pertence a root.${NC}"
        echo -e "${CYAN}==> Ajustando permissões para seu usuário ($(id -un)) gravar licenças e baixar componentes...${NC}"
        sudo chown -R "$(id -un):$(id -gn)" "$ANDROID_HOME"
    fi

    mkdir -p "$LICENSES_DIR"

    # Hashes oficiais do Google para pré-aceitar licenças do SDK 36 e Build-Tools 36
    cat << 'EOF' > "$LICENSES_DIR/android-sdk-license"
8933bad161af4178b1185d1a37fbf41ea5269c55
d56f5187479451eabf01fb78af6dfcb131a6481e
24333f8a63c6125ea1b458692f9f6e4766773dce
EOF

    cat << 'EOF' > "$LICENSES_DIR/android-sdk-preview-license"
84831b9409646a918e30573bab4c9c91346d8abd
EOF

    cat << 'EOF' > "$LICENSES_DIR/android-googletv-license"
601085b94cd77f0b54ff86406957099150008576
EOF

    cat << 'EOF' > "$LICENSES_DIR/android-sdk-arm-dbt-license"
859f317696f67ef3d7f30a50a5560e7834b43903
EOF

    cat << 'EOF' > "$LICENSES_DIR/google-gdk-license"
33b6a2b6492d66823cd1706d744c21217e24e33b
EOF

    cat << 'EOF' > "$LICENSES_DIR/mips-android-sysimage-license"
e9acab587f1749c4cb817b8662454f84e3391236
EOF

    # Se /usr/lib/android-sdk existir no Debian, grava nele também para compatibilidade total
    if [ -d "/usr/lib/android-sdk" ] && [ "$ANDROID_HOME" != "/usr/lib/android-sdk" ]; then
        if [ ! -w "/usr/lib/android-sdk" ]; then
            sudo chown -R "$(id -un):$(id -gn)" "/usr/lib/android-sdk" 2>/dev/null || true
        fi
        mkdir -p "/usr/lib/android-sdk/licenses" 2>/dev/null || sudo mkdir -p "/usr/lib/android-sdk/licenses"
        cp -f "$LICENSES_DIR"/* "/usr/lib/android-sdk/licenses/" 2>/dev/null || true
    fi

    # Executa também via sdkmanager CLI se estiver disponível
    local SDKMANAGER_BIN=""
    if command -v sdkmanager >/dev/null 2>&1; then
        SDKMANAGER_BIN="$(command -v sdkmanager)"
    elif [ -x "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" ]; then
        SDKMANAGER_BIN="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
    fi

    if [ -n "$SDKMANAGER_BIN" ]; then
        yes 2>/dev/null | "$SDKMANAGER_BIN" --sdk_root="$ANDROID_HOME" --licenses >/dev/null 2>&1 || true
    fi

    echo -e "${GREEN}✓ Licenças do Android SDK (36 / 36.1) aceitas com sucesso!${NC}"
}

auto_accept_sdk_licenses

# ==============================================================================
# 4. DETECÇÃO E VALIDAÇÃO DO COMPILADOR JAVA (JDK / JAVAC)
# ==============================================================================
detect_and_configure_jdk() {
    local FOUND_JDK=""

    if command -v javac >/dev/null 2>&1; then
        local JAVAC_REAL
        JAVAC_REAL=$(readlink -f "$(command -v javac)")
        local DIR
        DIR=$(dirname "$(dirname "$JAVAC_REAL")")
        if [ -x "$DIR/bin/javac" ]; then
            FOUND_JDK="$DIR"
        fi
    fi

    if [ -z "$FOUND_JDK" ] && [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/javac" ]; then
        FOUND_JDK="$JAVA_HOME"
    fi

    if [ -z "$FOUND_JDK" ]; then
        local CANDIDATES=(
            "/usr/lib/jvm/java-21-openjdk-amd64"
            "/usr/lib/jvm/java-17-openjdk-amd64"
            "/usr/lib/jvm/default-java"
            "/usr/lib/jvm/java-21-openjdk"
            "/usr/lib/jvm/java-17-openjdk"
            "/opt/android-studio/jbr"
            "/opt/jdk-21"
            "/opt/jdk-17"
        )
        for cand in "${CANDIDATES[@]}"; do
            if [ -x "$cand/bin/javac" ]; then
                FOUND_JDK="$cand"
                break
            fi
        done
    fi

    if [ -n "$FOUND_JDK" ]; then
        export JAVA_HOME="$FOUND_JDK"
        export PATH="$JAVA_HOME/bin:$PATH"
        echo -e "${GREEN}✓ Compilador Java (javac) pronto:${NC} $JAVA_HOME"
    else
        echo -e "${YELLOW}ℹ Compilador javac local não detectado. (No Android Studio o compilador já vem embutido).${NC}"
    fi
}

detect_and_configure_jdk

# ==============================================================================
# 5. INSTALADOR AUTOMATIZADO DO ANDROID STUDIO NO LINUX / DEBIAN
# ==============================================================================
install_android_studio() {
    echo -e "${CYAN}${BOLD}"
    echo "----------------------------------------------------------"
    echo "      Instalador Automatizado do Android Studio           "
    echo "----------------------------------------------------------"
    echo -e "${NC}"

    # 1. Verifica se já está instalado
    if [ -d "/opt/android-studio" ] && [ -x "/opt/android-studio/bin/studio.sh" ]; then
        echo -e "${GREEN}✓ O Android Studio já está instalado em /opt/android-studio!${NC}"
        echo -e "Para abri-lo, digite: ${BOLD}/opt/android-studio/bin/studio.sh${NC} ou use o menu de aplicativos."
        read -rp "Deseja abrir o Android Studio agora? (S/n): " LAUNCH_NOW
        if [[ ! "$LAUNCH_NOW" =~ ^[nN]$ ]]; then
            nohup /opt/android-studio/bin/studio.sh >/dev/null 2>&1 &
            echo -e "${GREEN}Android Studio iniciado em segundo plano.${NC}"
        fi
        return 0
    fi

    if command -v flatpak >/dev/null 2>&1 && flatpak list | grep -q "com.google.AndroidStudio"; then
        echo -e "${GREEN}✓ O Android Studio (Flatpak) já está instalado!${NC}"
        read -rp "Deseja abrir o Android Studio agora? (S/n): " LAUNCH_NOW
        if [[ ! "$LAUNCH_NOW" =~ ^[nN]$ ]]; then
            flatpak run com.google.AndroidStudio &
        fi
        return 0
    fi

    # 2. Instala dependências de sistema (especialmente bibliotecas essenciais no Debian)
    echo -e "${CYAN}==> Instalando dependências essenciais do sistema para o Android Studio...${NC}"
    case "$DISTRO_ID" in
        debian|ubuntu|linuxmint|pop)
            sudo apt-get update
            # Instala pacotes do sistema necessários para emulação e ferramentas do SDK
            sudo apt-get install -y wget curl unzip git adb libc6 libncurses5 libstdc++6 zlib1g openjdk-21-jdk 2>/dev/null || \
            sudo apt-get install -y wget curl unzip git adb openjdk-21-jdk
            ;;
        fedora|rhel|centos)
            sudo dnf install -y wget curl unzip git android-tools java-21-openjdk-devel zlib
            ;;
        arch|manjaro)
            sudo pacman -S --needed wget curl unzip git android-tools jdk-21-openjdk
            ;;
        *)
            echo -e "${YELLOW}Distribuição não identificada diretamente. Prosseguindo com download...${NC}"
            ;;
    esac

    # 3. Métodos de Instalação Disponíveis
    echo ""
    echo -e "${BOLD}Escolha o método de instalação preferido para o Android Studio:${NC}"
    echo -e "  ${CYAN}1)${NC} ${BOLD}Instalação Oficial Direta (Recomendada para Debian)${NC} (Baixa do Google e instala em /opt)"
    if command -v flatpak >/dev/null 2>&1; then
        echo -e "  ${CYAN}2)${NC} Instalação via Flatpak (Flathub)"
    fi
    if command -v snap >/dev/null 2>&1; then
        echo -e "  ${CYAN}3)${NC} Instalação via Snap"
    fi
    echo -e "  ${CYAN}4)${NC} Apenas abrir a página oficial de download no navegador"
    echo -e "  ${CYAN}5)${NC} Voltar ao menu principal"
    echo ""
    read -rp "Opção [1-5]: " METHOD_CHOICE

    case "$METHOD_CHOICE" in
        1)
            echo -e "${CYAN}==> Baixando o pacote oficial do Android Studio para Linux (.tar.gz)...${NC}"
            TMP_DIR="/tmp/android_studio_setup"
            mkdir -p "$TMP_DIR"
            cd "$TMP_DIR"

            STUDIO_URL="https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2024.3.1.13/android-studio-2024.3.1.13-linux.tar.gz"
            TAR_FILE="android-studio-linux.tar.gz"

            echo -e "Fazendo download de: ${BOLD}$STUDIO_URL${NC}"
            if command -v wget >/dev/null 2>&1; then
                wget -O "$TAR_FILE" "$STUDIO_URL"
            else
                curl -L -o "$TAR_FILE" "$STUDIO_URL"
            fi

            echo -e "${CYAN}==> Extraindo arquivos para /opt/android-studio (requer sudo)...${NC}"
            sudo rm -rf /opt/android-studio
            sudo tar -xzf "$TAR_FILE" -C /opt/
            sudo chown -R root:root /opt/android-studio
            sudo chmod -R 755 /opt/android-studio

            # Cria link simbólico para execução direta no terminal
            sudo ln -sf /opt/android-studio/bin/studio.sh /usr/local/bin/android-studio

            # Cria o atalho (.desktop) no menu do Debian/GNOME/KDE/XFCE
            echo -e "${CYAN}==> Criando atalho no menu de aplicativos do Linux...${NC}"
            cat << 'EOF' | sudo tee /usr/share/applications/android-studio.desktop >/dev/null
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Icon=/opt/android-studio/bin/studio.png
Exec=/opt/android-studio/bin/studio.sh %f
Comment=O IDE Oficial para Desenvolvimento Android
Categories=Development;IDE;
Terminal=false
StartupWMClass=jetbrains-studio
EOF

            rm -rf "$TMP_DIR"
            cd "$PROJECT_DIR"

            echo ""
            echo -e "${GREEN}${BOLD}✔ Android Studio instalado com sucesso no seu Debian!${NC}"
            echo -e "• Atalho disponível no menu de aplicativos do seu sistema."
            echo -e "• Também pode ser iniciado pelo terminal digitando: ${BOLD}android-studio${NC}"
            echo ""
            read -rp "Deseja iniciar o Android Studio agora? (S/n): " RUN_STUDIO
            if [[ ! "$RUN_STUDIO" =~ ^[nN]$ ]]; then
                nohup /opt/android-studio/bin/studio.sh >/dev/null 2>&1 &
                echo -e "${GREEN}Iniciando Android Studio...${NC}"
            fi
            ;;

        2)
            if command -v flatpak >/dev/null 2>&1; then
                echo -e "${CYAN}==> Instalando via Flatpak...${NC}"
                flatpak install -y flathub com.google.AndroidStudio
                echo -e "${GREEN}${BOLD}✔ Android Studio instalado via Flatpak!${NC}"
                flatpak run com.google.AndroidStudio &
            else
                echo -e "${RED}Flatpak não disponível no sistema.${NC}"
            fi
            ;;

        3)
            if command -v snap >/dev/null 2>&1; then
                echo -e "${CYAN}==> Instalando via Snap...${NC}"
                sudo snap install android-studio --classic
                echo -e "${GREEN}${BOLD}✔ Android Studio instalado via Snap!${NC}"
                android-studio &
            else
                echo -e "${RED}Snap não disponível no sistema.${NC}"
            fi
            ;;

        4)
            echo -e "Abrindo página de download: https://developer.android.com/studio"
            if command -v xdg-open >/dev/null 2>&1; then
                xdg-open "https://developer.android.com/studio"
            fi
            ;;

        *)
            echo "Retornando ao menu principal..."
            ;;
    esac
}

# ==============================================================================
# 6. INSTALADOR DE FERRAMENTAS VIA APT/LINUX (JDK 21, ADB)
# ==============================================================================
install_system_cli_tools() {
    detect_distro
    echo -e "${CYAN}==> Sistema detectado:${NC} ${BOLD}$DISTRO_NAME ($DISTRO_ID)${NC}"

    case "$DISTRO_ID" in
        debian|ubuntu|linuxmint|pop)
            echo -e "${CYAN}==> Instalando ferramentas para Debian/Ubuntu (apt-get)...${NC}"
            sudo apt-get update
            sudo apt-get install -y openjdk-21-jdk adb android-sdk-platform-tools-common unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no Debian!${NC}"
            ;;
        fedora|rhel|centos)
            echo -e "${CYAN}==> Instalando ferramentas para Fedora/RHEL (dnf)...${NC}"
            sudo dnf install -y java-21-openjdk-devel android-tools unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no Fedora!${NC}"
            ;;
        arch|manjaro)
            echo -e "${CYAN}==> Instalando ferramentas para Arch Linux (pacman)...${NC}"
            sudo pacman -S --needed jdk-21-openjdk android-tools unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no Arch!${NC}"
            ;;
        opensuse*|suse)
            echo -e "${CYAN}==> Instalando ferramentas para openSUSE (zypper)...${NC}"
            sudo zypper install -y java-21-openjdk-devel android-tools unzip curl wget git
            echo -e "${GREEN}${BOLD}✔ Ferramentas instaladas com sucesso no openSUSE!${NC}"
            ;;
        *)
            echo -e "${YELLOW}Distribuição não catalogada ($DISTRO_NAME). Instale openjdk-21-jdk e adb manualmente.${NC}"
            ;;
    esac
}

# ==============================================================================
# 7. VERIFICAÇÃO DO GRADLE WRAPPER
# ==============================================================================
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    chmod +x ./gradlew
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
else
    GRADLE_CMD="./gradlew"
fi

# ==============================================================================
# 8. VERIFICAÇÃO DE DISPOSITIVOS USB (ADB)
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
    echo -e "${YELLOW}ℹ Nenhum celular USB com depuração detectado no momento.${NC}"
fi

echo ""

# ==============================================================================
# 9. MENU DE AÇÕES E PARÂMETROS
# ==============================================================================
CHOICE=""

if [ "$1" == "--studio" ] || [ "$1" == "-s" ]; then
    CHOICE="1"
elif [ "$1" == "--build-only" ] || [ "$1" == "-b" ]; then
    CHOICE="2"
elif [ "$1" == "--install" ] || [ "$1" == "-i" ]; then
    CHOICE="3"
elif [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
    CHOICE="4"
elif [ "$1" == "--install-deps" ] || [ "$1" == "-d" ]; then
    CHOICE="5"
elif [ "$1" == "--test" ] || [ "$1" == "-t" ]; then
    CHOICE="6"
elif [ -n "$1" ]; then
    echo "Parâmetro desconhecido: $1"
    echo "Uso: ./InstallStudio.sh [--studio | --build-only | --install | --clean | --install-deps | --test]"
    exit 1
fi

if [ -z "$CHOICE" ]; then
    echo -e "${BOLD}Selecione a ação desejada:${NC}"
    echo -e "  ${CYAN}1)${NC} ${BOLD}Instalar o Android Studio no Linux / Debian${NC} (com atalhos e SDK configurado)"
    echo -e "  ${CYAN}2)${NC} Compilar APK Debug direto (-x test) (rápido, sem precisar de USB)"
    if [ "$DEVICE_CONNECTED" = true ]; then
        echo -e "  ${CYAN}3)${NC} Compilar e Instalar no celular via USB (${BOLD}${DEVICE_NAME}${NC})"
    else
        echo -e "  ${CYAN}3)${NC} Compilar e Instalar no celular via USB"
    fi
    echo -e "  ${CYAN}4)${NC} Limpar cache e recompilar tudo (clean assembleDebug -x test)"
    echo -e "  ${CYAN}5)${NC} Instalar ferramentas de linha de comando no Debian (OpenJDK 21, ADB)"
    echo -e "  ${CYAN}6)${NC} Executar testes unitários e Robolectric"
    echo -e "  ${CYAN}7)${NC} Sair"
    echo ""
    read -rp "Opção [1-7]: " USER_INPUT
    CHOICE="$USER_INPUT"
fi

echo ""

case "$CHOICE" in
    1)
        install_android_studio
        ;;

    2)
        auto_accept_sdk_licenses
        echo -e "${CYAN}==> Compilando APK Debug (assembleDebug -x test)...${NC}"
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

    3)
        auto_accept_sdk_licenses
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
            echo -e "Instale via 'sudo apt-get install adb' ou use a opção 5 deste script."
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
            read -rp "Deseja abrir o SamPlayer agora no celular? (S/n): " LAUNCH_APP
            if [[ ! "$LAUNCH_APP" =~ ^[nN]$ ]]; then
                echo -e "${CYAN}==> Abrindo o SamPlayer...${NC}"
                adb -s "$TARGET_DEVICE" shell am start -n "com.aistudio.prismaplayer.vnkz/com.example.MainActivity"
            fi
        fi
        ;;

    4)
        auto_accept_sdk_licenses
        echo -e "${CYAN}==> Limpando e recompilando tudo do zero (-x test)...${NC}"
        $GRADLE_CMD clean assembleDebug -x test
        echo -e "${GREEN}${BOLD}✔ Limpeza e recompilação concluídas com sucesso!${NC}"
        echo -e "APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
        ;;

    5)
        install_system_cli_tools
        ;;

    6)
        auto_accept_sdk_licenses
        echo -e "${CYAN}==> Executando suíte completa de testes unitários e Robolectric...${NC}"
        $GRADLE_CMD :app:testDebugUnitTest
        echo -e "${GREEN}${BOLD}✔ Todos os testes foram validados com êxito!${NC}"
        ;;

    7)
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
