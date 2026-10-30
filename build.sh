#!/usr/bin/env bash
# ==============================================================================
# SamPlayer - Script de Build & Deploy Nativo Automatizado
# Autor: Sam Moreno
# Licença: BSD 2-Clause License
# ==============================================================================

set -e

# Configurações de cores para o terminal
GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Localização do projeto
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"

echo -e "${CYAN}${BOLD}"
echo "=========================================================="
echo "      🎵 SamPlayer - Automação de Build & Deploy          "
echo "        Minimalist Native Audio Player (MP3/WAV/MIDI)     "
echo "=========================================================="
echo -e "${NC}"

# 1. Determina se usa ./gradlew ou gradle instalado globalmente
if [ -f "./gradlew" ]; then
    GRADLE_CMD="./gradlew"
    chmod +x ./gradlew
elif command -v gradle >/dev/null 2>&1; then
    GRADLE_CMD="gradle"
else
    echo -e "${RED}[ERRO] Nem o Gradle Wrapper (./gradlew) nem o comando 'gradle' foram encontrados no sistema.${NC}"
    echo -e "Certifique-se de ter o JDK (Java 17+) e o Gradle configurados."
    exit 1
fi

echo -e "${GREEN}✓ Build tool detectada:${NC} $GRADLE_CMD"

# 2. Verificação do ADB e dispositivos conectados via USB
HAS_ADB=false
DEVICE_CONNECTED=false
DEVICE_NAME=""

if command -v adb >/dev/null 2>&1; then
    HAS_ADB=true
    # Obtém dispositivos conectados autorizados
    DEVICES=$(adb devices 2>/dev/null | grep -v "List of devices" | grep "device$" | awk '{print $1}' || true)
    if [ -n "$DEVICES" ]; then
        DEVICE_CONNECTED=true
        DEVICE_COUNT=$(echo "$DEVICES" | wc -l)
        FIRST_DEVICE=$(echo "$DEVICES" | head -n 1)
        # Tenta pegar o modelo do celular (ex: Galaxy A26 / SM-A266...)
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

# 3. Tratamento de parâmetros ou exibição de menu interativo
CHOICE=""

if [ "$1" == "--build-only" ] || [ "$1" == "-b" ]; then
    CHOICE="1"
elif [ "$1" == "--install" ] || [ "$1" == "-i" ]; then
    CHOICE="2"
elif [ "$1" == "--clean" ] || [ "$1" == "-c" ]; then
    CHOICE="3"
elif [ "$1" == "--test" ] || [ "$1" == "-t" ]; then
    CHOICE="4"
elif [ -n "$1" ]; then
    echo "Parâmetro desconhecido: $1"
    echo "Uso: ./build.sh [--build-only | --install | --clean | --test]"
    exit 1
fi

if [ -z "$CHOICE" ]; then
    echo -e "${BOLD}Selecione a ação desejada:${NC}"
    echo -e "  ${CYAN}1)${NC} Apenas compilar o APK Debug (sem necessidade de celular conectado)"
    if [ "$DEVICE_CONNECTED" = true ]; then
        echo -e "  ${CYAN}2)${NC} ${BOLD}Compilar e Instalar no dispositivo via USB (${DEVICE_NAME})${NC}"
    else
        echo -e "  ${CYAN}2)${NC} Compilar e tentar instalar via USB (se conectar agora)"
    fi
    echo -e "  ${CYAN}3)${NC} Limpar cache e recompilar (clean assembleDebug)"
    echo -e "  ${CYAN}4)${NC} Executar suíte de testes unitários e Robolectric"
    echo -e "  ${CYAN}5)${NC} Sair"
    echo ""
    read -rp "Opção [1-5]: " USER_INPUT
    CHOICE="$USER_INPUT"
fi

echo ""

case "$CHOICE" in
    1)
        echo -e "${CYAN}==> Iniciando compilação do SamPlayer (APK Debug)...${NC}"
        $GRADLE_CMD :app:assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        if [ -f "$APK_PATH" ]; then
            APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
            echo ""
            echo -e "${GREEN}${BOLD}✔ Build concluído com sucesso!${NC}"
            echo -e "Arquivo APK gerado: ${BOLD}$APK_PATH${NC} ($APK_SIZE)"
            echo -e "Você pode transferir este arquivo diretamente para o seu aparelho."
        else
            echo -e "${RED}[ERRO] APK não localizado em $APK_PATH${NC}"
            exit 1
        fi
        ;;

    2)
        echo -e "${CYAN}==> Compilando o SamPlayer...${NC}"
        $GRADLE_CMD :app:assembleDebug
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        
        if [ ! -f "$APK_PATH" ]; then
            echo -e "${RED}[ERRO] Falha ao localizar o APK após compilação.${NC}"
            exit 1
        fi

        echo ""
        echo -e "${CYAN}==> Verificando conexão ADB com o celular...${NC}"
        if [ "$HAS_ADB" = false ]; then
            echo -e "${RED}[ERRO] O comando 'adb' não está instalado no PATH do seu terminal.${NC}"
            echo -e "Instale as ferramentas do Android Platform-Tools (adb) para usar o deploy USB."
            exit 1
        fi

        # Recarrega a checagem de dispositivos caso tenha plugado agora
        DEVICES=$(adb devices 2>/dev/null | grep -v "List of devices" | grep "device$" | awk '{print $1}' || true)
        if [ -z "$DEVICES" ]; then
            echo -e "${RED}[ERRO] Nenhum dispositivo Android conectado com a Depuração USB ativada.${NC}"
            echo -e "O APK compilado continua disponível em: ${BOLD}$APK_PATH${NC}"
            exit 1
        fi

        TARGET_DEVICE=$(echo "$DEVICES" | head -n 1)
        echo -e "${CYAN}==> Instalando SamPlayer no dispositivo [$TARGET_DEVICE]...${NC}"
        adb -s "$TARGET_DEVICE" install -r -d "$APK_PATH"

        echo ""
        echo -e "${GREEN}${BOLD}✔ SamPlayer instalado com sucesso no aparelho!${NC}"

        # Pergunta se deseja abrir o app imediatamente
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
        echo -e "${CYAN}==> Executando clean e nova compilação completa...${NC}"
        $GRADLE_CMD clean :app:assembleDebug
        echo -e "${GREEN}${BOLD}✔ Compilação limpa concluída com sucesso!${NC}"
        echo -e "APK gerado em: app/build/outputs/apk/debug/app-debug.apk"
        ;;

    4)
        echo -e "${CYAN}==> Executando suíte de testes unitários e testes Robolectric...${NC}"
        $GRADLE_CMD :app:testDebugUnitTest
        echo -e "${GREEN}${BOLD}✔ Todos os testes passaram com sucesso!${NC}"
        ;;

    5)
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
