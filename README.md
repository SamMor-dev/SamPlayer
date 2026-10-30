# 🎵 SamPlayer

> **Player de áudio local minimalista e de alta eficiência para Android, focado em performance bruta, baixo consumo de memória e zero rastreamento.**

---

### 📦 Recursos & Formatos Suportados
O motor de áudio foi projetado de forma enxuta para gerenciar arquivos locais no metal, sem travas de mercado ou dependências de serviços em nuvem:

* **WAV:** Execução de áudio linear sem compressão com latência mínima.
* **MP3:** Decodificação veloz e otimizada de áudio comprimido.
* **MIDI:** Síntese de som via software independente, garantindo renderização precisa de instrumentos (.mid) de forma isolada do framework do ecossistema Google.

---

### 🤖 Metodologia de Engenharia: *Human-Architected, AI-Forged*
Desenvolvido em tempo recorde utilizando a fundição avançada do **AI Studio** sob rigorosa governança humana. 

A arquitetura do motor, a lógica de concorrência nas threads de áudio, o gerenciamento cirúrgico de memória e a seleção estrita de APIs foram definidos pelo autor. A IA executou a sintaxe bruta sob validação estrita, garantindo um código leve, rápido e livre de inchaço (*bloatware*).

---

### ⚙️ Diferenciais Técnicos & Otimização
* **Bare-Metal Mobile:** Roda liso e de forma extremamente ágil em dispositivos físicos reais (como o meu Samsung Galaxy A26, pois testei nele), consumindo o mínimo de Megabytes de memória RAM.
* **Instalação Direta:** Provisionamento limpo via depuração USB, operando sem dependências de pacotes gigantes de telemetria ou frameworks redundantes.
* **Interface Técnica Otimizada:** Painel visual enxuto focado em navegação fluida de faixas e carregamento assíncrono de arquivos para evitar congelamentos na TUI/GUI.

---

## 🚀 Instalação & Teste Local

Se você possui o ambiente de desenvolvimento configurado e o dispositivo conectado via USB:

1. Clone o repositório:
   ```bash
   git clone https://github.com
   ```
2. Compile e instale diretamente no hardware via ADB:
   ```bash
   ./gradlew installDebug
   ```

---

## ⚖️ Licença e Governança

Distribuído sob os termos estáveis da **BSD 2-Clause License (Simplified)**.

Uma licença pacífica, comercialmente livre, neutra e totalmente limpa de contaminações ou atritos de licenças copyleft virais. Sinta-se livre para usar, embutir ou modificar o código mantendo o aviso de direitos autorais original.

Copyright (c) 2026, Sam Moreno. Todos os direitos reservados.
