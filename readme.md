
---
$$$$

Perfeito. Consegui acessar o repositório **`segodimor2d2/recmovies`**. Ele está realmente no estágio inicial: branch `main`, apenas **1 commit**, e já tem a estrutura Android/Gradle (`app`, `gradle`, `build.gradle.kts`, `settings.gradle.kts` etc.). ([GitHub][1])

[Abrir o repositório recmovies no GitHub](https://github.com/segodimor2d2/recmovies?utm_source=chatgpt.com)

Então podemos trabalhar **diretamente em cima desse projeto**, sem criar outro projeto.

### Eu faria assim

Vamos construir em pequenos passos:

```text
recmovies
│
├── UI — Jetpack Compose
│
├── ViewModel
│   └── CameraViewModel
│
├── Camera
│   ├── CameraX Preview
│   ├── CameraX VideoCapture
│   └── orientação
│
├── Audio
│   ├── microfone interno
│   ├── headset com fio
│   └── Bluetooth
│
└── Storage
    └── arquivos de vídeo
```

E o fluxo será:

```text
CameraScreen
      │
      ▼
CameraViewModel
      │
      ├── estado da câmera
      ├── estado da gravação
      └── entrada de áudio
      │
      ▼
Camera/Audio layer
      │
      ▼
Android CameraX / AudioManager
```

### Primeira etapa

Eu não colocaria ainda Bluetooth, gravação, orientação etc.

Primeiro vamos fazer somente:

**Hello World → Preview da câmera funcionando.**

Depois:

1. Preview.
2. Gravar/parar.
3. Salvar vídeo.
4. Vertical/horizontal.
5. Detectar dispositivos de áudio.
6. Selecionar fone com cabo.
7. Selecionar Bluetooth.
8. Tratar conexão/desconexão dos dispositivos.

Isso evita chegarmos a uma situação em que não sabemos se o problema está na câmera, no encoder ou no áudio.

**Próximo passo:** me mande o conteúdo atual de:

```bash
cat app/build.gradle.kts
find app/src/main -type f | sort
```

Com isso eu te passo **exatamente a primeira alteração no `recmovies`**, incluindo os arquivos que vamos criar e o comando para testar no celular.

[1]: https://github.com/segodimor2d2/recmovies "GitHub - segodimor2d2/recmovies: recmovies · GitHub"

---
$$$$

quero fazer um app em android usando jetpack compose, com uma arquitetura MVVM,
o app é um projeto para gravar vídeos em um celular,
eu quero poder gravar vídeos na horizontal e na vertical,
e quero que a entrada do microfone eu possa escolher para ser feita via cabo de fones de ouvido e via bluetooth,
meu porjeto esta iniciado ja com hello world


---
$$$$


Esse resultado fecha uma parte importante do diagnóstico.

### O que aconteceu

Com `VOICE_COMMUNICATION`:

```text
setCommunicationDevice=false
setPreferredDevice=true
preferredDevice=T10-L
activeDevice=null
```

Ou seja, **a seleção do `AudioRecord` aceita o T10-L como preferido, mas o Android não está estabelecendo o T10-L como dispositivo de comunicação**.

Isso é coerente com a API: `setCommunicationDevice()` só aceita dispositivos retornados por `getAvailableCommunicationDevices()`. ([Android Developers][1])

E anteriormente nós tínhamos visto o T10-L nessa lista. Agora o `AudioDeviceInfo` encontrado pelo `GET_DEVICES_INPUTS` tem `id=16536`, e a chamada retorna `false`. Portanto, não devemos concluir que o `AudioRecord` está capturando áudio real do T10-L apenas porque `preferredDevice=T10-L`.

Mais importante: a própria documentação oficial mostra a sequência `VOICE_COMMUNICATION` + `setPreferredDevice()` + `setCommunicationDevice()` para Bluetooth HFP. ([Android Developers][2])

### Acho que encontramos o ponto que precisamos investigar

Não é mais necessário ficar alterando `AudioSource`.

O próximo teste deve responder uma pergunta simples:

**O T10-L está presente na lista de dispositivos de comunicação no momento em que `testBluetoothInput()` roda?**

No começo de `testBluetoothInput()`, antes de procurar o dispositivo de entrada, coloque:

```kotlin
val communicationDevices =
    audioManager.getAvailableCommunicationDevices()

communicationDevices.forEach { communicationDevice ->
    println(
        "AudioTest: CommunicationDevice: " +
            "id=${communicationDevice.id}, " +
            "type=${communicationDevice.type}, " +
            "name=${communicationDevice.productName}"
    )
}
```

Então o início da função ficará assim:

```kotlin
fun testBluetoothInput() {

    val communicationDevices =
        audioManager.getAvailableCommunicationDevices()

    communicationDevices.forEach { communicationDevice ->
        println(
            "AudioTest: CommunicationDevice: " +
                "id=${communicationDevice.id}, " +
                "type=${communicationDevice.type}, " +
                "name=${communicationDevice.productName}"
        )
    }

    val device =
        audioManager
            .getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull {
                it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            }

    // resto permanece igual
```

**Não mude mais nada.**

Rode novamente:

```bash
./gradlew installDebug && adb shell am start -n com.rec.recmov/.MainActivity
```

e:

```bash
adb logcat -c
adb logcat -v threadtime | grep -E "AudioTest"
```

Quero ver principalmente se aparece:

```text
AudioTest: CommunicationDevice: ... type=7 ... name=T10-L
```

ou se **não aparece**.

Isso vai explicar diretamente por que:

```text
setCommunicationDevice=false
```

e aí seguimos para o próximo passo.

[1]: https://developer.android.com/reference/kotlin/android/media/AudioManager.html?authuser=451499271&utm_source=chatgpt.com "AudioManager  |  API reference  |  Android Developers"
[2]: https://developer.android.com/develop/xr/jetpack-xr-sdk/access-hardware-projected-context?authuser=5&hl=pt-br&utm_source=chatgpt.com "Usar um contexto projetado para acessar hardware em óculos de áudio e óculos de exibição  |  Android XR for Jetpack XR SDK  |  Android Developers"

---
$$$$


Perfeito. Esse resultado é **muito importante**:

```text
Stream #0:0: Video: h264
Stream #0:1: Audio: aac (LC), 48000 Hz, stereo
```

Ou seja, **o CameraX está gravando áudio no MP4 mesmo sem `.withAudioEnabled()`**.

Então descartamos a hipótese de que precisamos criar agora um pipeline separado:

```text
AudioRecord → AAC → MediaMuxer
```

### O que já sabemos

1. ✅ Vídeo H.264 está sendo gravado.
2. ✅ O MP4 contém áudio AAC.
3. ✅ O áudio está em `48 kHz`, estéreo.
4. ❌ Pelos logs anteriores, a origem escolhida pelo CameraX continua sendo:

```text
AUDIO_SOURCE_CAMCORDER
IN_MICROPHONE_BACK
```

Portanto, **o problema agora é exclusivamente descobrir se conseguimos fazer essa entrada do CameraX usar o Bluetooth T10-L**.

E temos uma evidência muito boa de que o Android consegue capturar pelo T10-L, porque nosso `AudioRecord` separado conseguiu:

```text
preferredDevice=T10-L
```

e apresentou sinal real.

### Próximo teste

Não vamos mexer no código ainda.

Quero confirmar uma coisa: **o áudio que está dentro desse MP4 é realmente o microfone interno**, ou eventualmente o T10-L.

Faça um teste simples:

1. Conecte o **T10-L**.
2. Grave novamente uns 5 segundos.
3. Durante a gravação, **fale bem perto do T10-L** e mantenha o telefone afastado da sua boca.
4. Pare a gravação.
5. Pegue o novo arquivo com:

```bash
adb shell 'ls -lt /sdcard/Movies/*.mp4 2>/dev/null | head'
```

Depois faça o `adb pull` e `ffprobe` como fizemos antes.

Mas, principalmente, **escute o arquivo**:

```bash
ffplay /tmp/recmov-test.mp4
```

Se o áudio estiver claramente vindo do microfone interno, confirmamos que o CameraX está ignorando nossa seleção de dispositivo.

**Não altere o código ainda.** O próximo passo será atacar especificamente o roteamento do áudio do CameraX, agora que sabemos que o áudio realmente está sendo muxado no MP4.

---
$$$$


Sim. Com o que já testamos, podemos deixar essa inspeção de lado.

As opções restantes, em ordem de complexidade, são:

1. **Continuar usando CameraX `Recorder` e tentar influenciar o roteamento pelo `AudioManager`**

   * Já fizemos `MODE_IN_COMMUNICATION`.
   * Já fizemos `setCommunicationDevice(T10-L)`.
   * O log provou que o Bluetooth SCO é ativado.
   * O `CAMCORDER` continua escolhendo `IN_MICROPHONE_BACK`.
   * Portanto, eu não insistiria muito nessa linha.

2. **Usar `AudioRecord` para capturar o Bluetooth e manter CameraX para o vídeo**

   * É a opção que temos mais evidência de que funciona.
   * Já comprovamos que:

     ```kotlin
     audioRecord.setPreferredDevice(T10-L)
     ```

     consegue capturar o áudio do T10-L.
   * O problema passa a ser **combinar esse áudio com o vídeo**.

3. **Usar CameraX apenas para vídeo e fazer nossa própria gravação/muxing**

   Arquitetura:

   ```text
   CameraX
      │
      └── vídeo H.264 ───────┐
                             │
   AudioRecord               ├──> MP4
      │                      │
      └── áudio Bluetooth ───┘
   ```

   Aqui poderíamos:

   * deixar CameraX gerar o vídeo;
   * capturar o T10-L com `AudioRecord`;
   * codificar áudio em AAC;
   * muxar vídeo + áudio em MP4.

   **É uma mudança maior**, mas nos dá controle real sobre o dispositivo de áudio.

4. **Usar Camera2 + MediaRecorder/MediaCodec em vez de CameraX Recorder**

   Poderíamos controlar mais diretamente a captura, mas não significa automaticamente que o `MediaRecorder` permitirá escolher `T10-L`. Teríamos que testar a interação entre `AudioSource` e roteamento Bluetooth.

5. **Usar `AudioRecord` + MediaCodec para áudio e continuar CameraX para vídeo**

   É uma variante da opção 3. Em vez de depender de uma API de gravação pronta, controlaríamos explicitamente:

   ```text
   AudioRecord → AAC encoder
   CameraX → H.264
   ```

   e depois mux.

### O que eu faria agora

Eu seguiria pela **opção 2 primeiro**, mas sem implementar o mux ainda.

Já sabemos que `AudioRecord` consegue enxergar o T10-L. O próximo passo pequeno seria provar:

```text
T10-L
  ↓
AudioRecord.setPreferredDevice()
  ↓
AAC/PCM capturado durante uma gravação real
```

Enquanto isso, o CameraX continuaria responsável pelo vídeo.

**Não mexeríamos no fluxo atual de gravação ainda.** Primeiro provaríamos que conseguimos capturar o áudio Bluetooth durante o mesmo período em que a câmera está gravando. Depois decidimos como juntar os dois.

---
$$$$


Exatamente. **Se não estivermos usando Bluetooth, não precisamos perder o áudio normal do CameraX.**

A opção 3 pode ser feita de forma híbrida.

### Com Bluetooth selecionado

Em vez de:

```kotlin
prepareRecording(...)
    .withAudioEnabled()
    .start(...)
```

faríamos:

```text
CameraX
   └── vídeo

AudioRecord
   └── T10-L Bluetooth

        ↓

     MP4 final
```

Nesse caso, o `AudioRecord` substitui completamente o áudio do CameraX.

### Sem Bluetooth

Mantemos exatamente o fluxo atual:

```text
CameraX
   ├── vídeo
   └── áudio normal
        ↓
       MP4
```

Ou seja:

```kotlin
if (bluetoothSelecionado) {
    // CameraX sem áudio
    // AudioRecord captura T10-L
} else {
    // CameraX normal
    .withAudioEnabled()
}
```

Isso é perfeitamente coerente com a API: `withAudioEnabled()` é justamente a configuração que habilita o áudio do `Recorder`; sem ela, a gravação não terá a faixa de áudio do CameraX. ([Android Developers][1])

### E tem uma vantagem importante

Não precisamos transformar o aplicativo inteiro.

Podemos preservar:

* CameraX;
* Preview;
* orientação horizontal/vertical;
* `MediaStore`;
* qualidade FHD;
* botão de gravação;
* fluxo atual de `Recording`;
* áudio interno funcionando como hoje.

Só criamos **um caminho alternativo quando o usuário escolhe Bluetooth**.

```text
                  ┌── Bluetooth?
                  │
                  ├── NÃO ──→ CameraX + withAudioEnabled()
                  │                 ↓
                  │                MP4
                  │
                  └── SIM ──→ CameraX vídeo
                              +
                              AudioRecord → T10-L
                                      ↓
                              mux → MP4
```

E temos evidência concreta de que `AudioRecord` é adequado para isso: a API possui `setPreferredDevice(AudioDeviceInfo)`, especificamente para direcionar a entrada do `AudioRecord` para determinado dispositivo. ([Android Developers][2])

**Então eu manteria `.withAudioEnabled()` para o caminho normal.** Só não o usaria no caminho Bluetooth.

O ponto que precisamos resolver agora é **como gerar a faixa AAC e fazer o mux com o vídeo CameraX**. Isso pode ser feito incrementalmente, sem mexer no caminho que já funciona.

[1]: https://developer.android.google.cn/media/camera/camerax/video-capture?hl=pt&utm_source=chatgpt.com "Arquitetura de captura de vídeo do CameraX  |  Android media  |  Android Developers"
[2]: https://developer.android.com/reference/android/media/AudioRecord?utm_source=chatgpt.com "AudioRecord  |  API reference  |  Android Developers"


---
$$$$


A primeira execução confirmou que o arquivo existe:

```text
-rw------- ... 288K ... cache/recmov_bluetooth_audio.wav
```

Então **a gravação do WAV está funcionando**. O segundo comando falhou novamente porque o ADB perdeu o dispositivo.

Agora não precisamos testar mais nada no código. Primeiro estabilize a conexão ADB:

```bash
adb devices
```

Se aparecer vazio, faça:

```bash
adb kill-server
adb start-server
adb devices
```

Se ainda não aparecer, reconecte o USB/Wi-Fi do Motorola e rode novamente:

```bash
adb devices
```

Quando aparecer:

```text
XXXXXXXX    device
```

execute **uma vez só**:

```bash
adb shell run-as com.rec.recmov \
    cat cache/recmov_bluetooth_audio.wav > /tmp/recmov_bluetooth_audio.wav
```

Depois, no PC:

```bash
file /tmp/recmov_bluetooth_audio.wav
```

e:

```bash
ffprobe /tmp/recmov_bluetooth_audio.wav
```

O arquivo de **288 KiB** também é compatível com vários segundos de PCM 8 kHz mono 16-bit, então até aqui está tudo consistente.

**Próximo marco:** confirmar que o `ffprobe` reconhece o WAV corretamente. Aí partimos para colocar esse áudio Bluetooth no MP4.

---
$$$$


Sim — **isso é uma observação importante**. Se você está ouvindo/verificando o MP4 e consegue confirmar que **o áudio gravado dentro do vídeo é realmente o Bluetooth T10-L**, então nossa conclusão anterior precisa ser revista.

O fato de o log do CameraX mostrar:

```text
IN_MICROPHONE
IN_MICROPHONE_BACK
source CAMCORDER
```

**não prova sozinho que o áudio final veio do microfone interno**.

O que pode estar acontecendo é:

```text
Bluetooth T10-L
      │
      │ setCommunicationDevice()
      ▼
Android Audio Policy
      │
      ├── AudioRecord Bluetooth → nosso WAV
      │
      └── CameraX Recorder → áudio Bluetooth
```

Ou seja, o `setCommunicationDevice()` pode estar alterando a rota de captura usada pelo sistema de uma maneira que o CameraX acaba respeitando, mesmo que a configuração interna da câmera continue aparecendo como `CAMCORDER`.

### Vamos confirmar antes de mudar qualquer código

Não precisamos especular. O teste mais importante agora é comparar os dois áudios.

Pegue o MP4 que acabou de gravar e rode:

```bash
ffprobe -hide_banner "SEU_VIDEO.mp4"
```

Depois extraia o áudio:

```bash
ffmpeg -y -i "SEU_VIDEO.mp4" -vn -acodec pcm_s16le /tmp/video_audio.wav
```

E compare com o nosso WAV Bluetooth:

```bash
ffprobe -hide_banner /tmp/video_audio.wav
```

Se quiser uma confirmação ainda mais objetiva, podemos comparar a duração e o conteúdo dos dois áudios.

**Não mexa no código ainda.** Se o áudio dentro do MP4 realmente for o T10-L, isso muda bastante a estratégia: talvez **não seja necessário substituir o áudio do CameraX**, e podemos manter a implementação muito mais simples.

---
$$$$

```bash

e app/build.gradle.kts
e /home/segodimo/05android/recmovies/gradle/libs.versions.toml
e /home/segodimo/05android/recmovies/app/src/main/AndroidManifest.xml
e /home/segodimo/05android/recmovies/app/build.gradle.kts



meu arquivo ficou assim:

```

---
$$$$



---
$$$$

```bash


cd /home/segodimo/05android/recmovies/ | term



adb shell am start -n com.rec.recmov/.MainActivity

./gradlew assembleRelease

./gradlew installDebug && adb shell am start -n com.rec.recmov/.MainActivity 

feat: entrada de camera e preview funcionando
feat: rec stop funcionando
feat: mic device funcionando
feat: mic bluetooth logs
feat: mic bluetooth funcionando 


adb logcat -c && adb logcat -v threadtime | grep -E
adb logcat -c && adb logcat -v threadtime | grep -E "AudioInputDevice"
AudioDevice
adb logcat -c && adb logcat -v threadtime | grep -E "AudioTest"
adb logcat -c && adb logcat -v threadtime | grep -E "AudioDevice"

"AudioInputDevice"
"Bluetooth routing"
"AudioDevice"


adb shell 'ls -lt /sdcard/Movies/*.mp4 2>/dev/null | head'
adb pull /sdcard/Movies/recmov_1790196680934.mp4 /tmp/recmov-test.mp4
ffprobe -hide_banner /tmp/recmov-test.mp4

ffprobe -hide_banner "/tmp/recmov-test.mp4"
ffmpeg -y -i "/tmp/recmov-test.mp4" -vn -acodec pcm_s16le /tmp/video_audio.wav
ffprobe -hide_banner /tmp/video_audio.wav



AudioTest
./gradlew installDebug && adb shell am start -n com.rec.recmov/.MainActivity
./gradlew installDebug && adb shell am start -n com.rec.recmov/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "AudioTest"
./gradlew installDebug && adb shell am start -n com.rec.recmov/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "AudioDevice"
adb logcat -c && adb logcat -v threadtime | grep -E "AudioDevice"
adb logcat -c && adb logcat -v threadtime | grep -E "AudioTest"
adb logcat -c && adb logcat -v threadtime | grep -E "AudioInputDevice|CommunicationDevice"
"AudioRecord:|AudioDevice:"
"AudioDevice:|prepareToOpenStream|IN_MICROPHONE_BACK|source: CAMCORDER|usecase:.*CAMCORDER"
"AudioDevice|prepareToOpenStream|IN_MICROPHONE|CAMCORDER"
"AudioDevice|setCommunicationDevice|AudioPolicy|CAMCORDER|IN_MICROPHONE"
"Hal2AidlMapper|AHAL_Module|CAMCORDER|IN_MICROPHONE"
adb logcat -c
adb logcat -v threadtime | grep -E

adb logcat -v threadtime | grep "AudioDevice:"

adb logcat -v threadtime | grep -E

adb logcat -v threadtime | grep -E


```

---
$$$$
