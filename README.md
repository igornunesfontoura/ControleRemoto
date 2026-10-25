# Controle Remoto

App Android que transforma o celular em **teclado, touchpad e controle de mídia Bluetooth** para o computador. O celular aparece para o computador como um teclado e mouse Bluetooth comuns, então não é preciso instalar nada do outro lado.

<!-- DEMO: substitua a linha abaixo por um GIF do app controlando o computador -->
<p align="center"><em>🎬 GIF de demonstração em breve</em></p>

## Por que existe

Eu assisto filmes e séries no notebook ligado à TV por um cabo HDMI. O problema é que, toda vez que precisava colocar em tela cheia ou aumentar o volume, tinha que levantar do sofá e ir até o notebook.

Então criei um app que conecta o celular ao computador por Bluetooth, para controlar tudo à distância.

## Funcionalidades

- **Touchpad:** mover o cursor, clique com toque, clique direito com dois dedos, arrastar e rolagem com dois dedos, com sensibilidade ajustável
- **Teclado:** digitação livre, mais teclas especiais (Esc, Tab, Enter, Home, End, PgUp, PgDn, Del, F11) e modificadores Ctrl, Alt, Shift e Super que ficam presos até a próxima tecla, para fazer atalhos
- **Mídia:** play/pause, faixa anterior e próxima, volume e mudo pelas teclas de mídia, além de voltar, avançar e tela cheia (←, → e F), que funcionam no YouTube, mpv e VLC
- **Conexão:** reconecta sozinho ao último computador e mantém a conexão ativa em segundo plano, com uma notificação

## Como funciona

Desde o Android 9, o sistema oferece o perfil Bluetooth **HID Device**, que permite a um app se apresentar como teclado e mouse. O Controle Remoto registra esse perfil e envia os comandos no mesmo formato que um periférico Bluetooth de verdade usa. Por isso o computador não precisa de driver nem de app próprio.

Feito em **Kotlin** com **Jetpack Compose** e Material 3.

## Requisitos

- Celular com **Android 9** ou mais novo e Bluetooth
- Computador com Bluetooth
- Para compilar: **JDK 17** e o **Android SDK** (o Android Studio já instala os dois)

## Instalação

Clone o repositório e instale no celular conectado por USB, com a depuração USB ativada:

```sh
git clone https://github.com/igornunesfontoura/ControleRemoto.git
cd ControleRemoto
export ANDROID_HOME=~/Android/Sdk
./gradlew installDebug
```

Pelo Android Studio, abra a pasta do projeto e clique em **Run**.

Para rodar os testes unitários:

```sh
./gradlew testDebugUnitTest
```

## Uso

### Primeiro pareamento

1. Abra o app, conceda as permissões e toque em **Tornar visível**.
2. No computador, procure novos dispositivos Bluetooth e pareie com o celular. No Linux, pelo terminal:

   ```sh
   bluetoothctl
   scan on
   pair <MAC-do-celular>
   trust <MAC-do-celular>
   connect <MAC-do-celular>
   ```

3. Das próximas vezes, o app reconecta sozinho ao último computador. Você também pode escolher um dispositivo na lista e tocar em **Conectar**.

No Linux, para conferir se o computador está recebendo os comandos, rode `sudo libinput debug-events`. O teclado, o mouse e o controle de mídia devem aparecer.

### Gestos do touchpad

| Gesto | Ação |
|---|---|
| Arrastar com 1 dedo | mover o cursor |
| Toque | clique esquerdo |
| Toque com 2 dedos | clique direito |
| Toque e depois tocar e arrastar | arrastar |
| Arrastar com 2 dedos | rolagem |

## Limitações

- O teclado assume que o computador usa layout **US**. Letras acentuadas são enviadas sem acento.
- Alguns fabricantes removem o perfil HID Device do Android. Nesses aparelhos, o app mostra "Indisponível".

## Licença

[MIT](LICENSE)
