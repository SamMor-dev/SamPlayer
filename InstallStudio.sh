#!/usr/bin/env bash
# InstallStudio.sh - Gerenciador Interativo de IDE Android Studio & Aceleração KVM
# Licença: BSD 2-Clause (Sam Moreno)

set -euo pipefail

INSTALL_DIR="$HOME/.local/share/android-studio"
DESKTOP_ENTRY="$HOME/.local/share/applications/android-studio.desktop"

# ==================== MÓDULO KVM & HARDWARE ====================
setup_kvm() {
    echo ""
    echo "=== [KVM] Verificando e Configurando Aceleração de Hardware ==="

    # 1. Checa suporte a virtualização no processador
    if ! grep -E -q '(vmx|svm)' /proc/cpuinfo; then
        echo "[AVISO] Sua CPU não possui suporte a virtualização (VMX/SVM) ou ela está desativada na BIOS."
        echo "O emulador Android poderá rodar mais lento."
        return
    fi
    echo "[OK] Suporte a virtualização de hardware detectado na CPU."

    # 2. Identifica o gerenciador de pacotes e instala dependências do KVM
    if command -v apt &>/dev/null; then
        echo "[KVM] Instalando pacotes KVM via apt..."
        sudo apt update -y && sudo apt install -y qemu-kvm libvirt-daemon-system build-essential
    elif command -v dnf &>/dev/null; then
        echo "[KVM] Instalando pacotes KVM via dnf..."
        sudo dnf install -y qemu-kvm libvirt
    elif command -v pacman &>/dev/null; then
        echo "[KVM] Instalando pacotes KVM via pacman..."
        sudo pacman -S --needed --noconfirm qemu-full virt-manager
    else
        echo "[AVISO] Gerenciador de pacotes não reconhecido automaticamente. Verifique se o KVM está instalado."
    fi

    # 3. Adiciona o usuário atual ao grupo kvm (para permissões do emulador)
    if grep -q '^kvm:' /etc/group; then
        echo "[KVM] Adicionando o usuário '$USER' ao grupo 'kvm'..."
        sudo usermod -aG kvm "$USER"
        echo "[OK] Usuário adicionado ao grupo 'kvm'. (Nota: Pode ser necessário fazer logoff para aplicar a permissão de grupo)."
    fi

    # 4. Ajusta permissões do nó /dev/kvm se ele existir
    if [ -e /dev/kvm ]; then
        sudo chown root:kvm /dev/kvm || true
    fi

    echo "=== [KVM] Aceleração de Emulação Pronta! ==="
}

# ==================== MÓDULO DE INSTALAÇÃO ====================
install_studio() {
    echo ""
    echo "=== Modo de Instalação/Configuração ==="
    
    local archive
    archive=$(ls android-studio-*.tar.gz 2>/dev/null | head -n 1 || true)

    if [ -z "$archive" ]; then
        echo "[ERRO] Nenhum arquivo 'android-studio-*.tar.gz' foi encontrado nesta pasta."
        echo "Por favor, baixe o pacote do Android Studio e coloque-o ao lado deste script."
        exit 1
    fi

    # Configura o KVM antes de extrair
    setup_kvm

    echo ""
    echo "[1/3] Extraindo '$archive' para $INSTALL_DIR..."
    mkdir -p "$INSTALL_DIR"
    tar -xzf "$archive" -C "$INSTALL_DIR" --strip-components=1

    echo "[2/3] Criando atalho no menu de aplicativos (.desktop)..."
    mkdir -p "$HOME/.local/share/applications"

    cat <<EOF > "$DESKTOP_ENTRY"
[Desktop Entry]
Version=1.0
Type=Application
Name=Android Studio
Icon=$INSTALL_DIR/bin/studio.png
Exec="$INSTALL_DIR/bin/studio.sh" %f
Comment=The Official IDE for Android
Categories=Development;IDE;
Terminal=false
StartupWMClass=jetbrains-studio
EOF

    echo "[3/3] Aplicando permissões de execução..."
    chmod +x "$INSTALL_DIR/bin/studio.sh"
    chmod +x "$DESKTOP_ENTRY"

    echo ""
    echo "=== Instalação e Otimização Concluídas com Sucesso! ==="
    echo "Você já pode abrir o Android Studio e criar Emuladores Acelerados por KVM!"
}

# ==================== MÓDULO DE DESINSTALAÇÃO ====================
uninstall_studio() {
    echo ""
    echo "=== Modo de Desinstalação ==="

    if [ -d "$INSTALL_DIR" ]; then
        echo "[1/2] Removendo arquivos da IDE em $INSTALL_DIR..."
        rm -rf "$INSTALL_DIR"
    else
        echo "[1/2] Pasta de instalação não encontrada. Pulando..."
    fi

    if [ -f "$DESKTOP_ENTRY" ]; then
        echo "[2/2] Removendo atalho do menu..."
        rm -f "$DESKTOP_ENTRY"
    else
        echo "[2/2] Atalho do menu não encontrado. Pulando..."
    fi

    echo ""
    echo "=== Desinstalação concluída! O sistema está limpo. ==="
}

# ==================== MENU INTERATIVO ====================
echo "===================================================="
echo "  Gerenciador Android Studio & KVM (Sam Dev Suite)  "
echo "===================================================="
echo "Escolha uma opção digitando o número:"
echo ""

PS3="Digite o número da sua opção (1-3): "
options=("Instalar Studio + Aceleração KVM" "Desinstalar Studio" "Sair")

select opt in "${options[@]}"; do
    case "$opt" in
        "Instalar Studio + Aceleração KVM")
            install_studio
            break
            ;;
        "Desinstalar Studio")
            uninstall_studio
            break
            ;;
        "Sair")
            echo "Saindo sem alterações."
            exit 0
            ;;
        *)
            echo "Opção inválida: $REPLY. Escolha 1, 2 ou 3."
            ;;
    esac
done
