# Controle Remoto

App Android (Kotlin + Compose) que transforma o celular em **teclado + touchpad + controle de mídia Bluetooth**
usando o perfil HID Device (Android 9+). Não precisa instalar nada no notebook.

## Build e instalação
```sh
export ANDROID_HOME=~/Android/Sdk
./gradlew installDebug          # celular via USB com depuração ativada
./gradlew testDebugUnitTest     # testes unitários
```

## Primeiro pareamento (Linux)
1. Abra o app, conceda as permissões e toque em **Tornar visível**.
2. No notebook:
   ```sh
   bluetoothctl
   scan on
   pair <MAC-do-celular>
   trust <MAC-do-celular>
   connect <MAC-do-celular>
   ```
3. Nas próximas vezes o app reconecta sozinho ao último notebook (ou toque em **Conectar** na lista).

Para conferir: `sudo libinput debug-events` deve mostrar o teclado, o mouse e o consumer control.

## Gestos do touchpad
- 1 dedo: mover · toque: clique esquerdo · toque com 2 dedos: clique direito
- toque + toque e arrastar: arrastar · 2 dedos arrastando: rolagem

## Limitações
- O teclado assume layout **US** no notebook; acentos são enviados sem acento.
- Alguns fabricantes removem o perfil HID Device; nesse caso o app mostra "Indisponível".
