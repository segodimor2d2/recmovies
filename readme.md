
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

```bash

e app/build.gradle.kts
e /home/segodimo/05android/recmovies/gradle/libs.versions.toml
e /home/segodimo/05android/recmovies/app/src/main/AndroidManifest.xml
e /home/segodimo/05android/recmovies/app/build.gradle.kts



meu arquivo ficou assim:

```

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


adb logcat -c && adb logcat -v threadtime | grep -E
adb logcat -c && adb logcat -v threadtime | grep -E "AudioInputDevice"

adb logcat -c && adb logcat -v threadtime | grep -E "AudioTest"

"AudioInputDevice"
"Bluetooth routing"
"AudioDevice"



./gradlew installDebug && adb shell am start -n com.rec.recmov/.MainActivity && adb logcat -c && adb logcat -v threadtime | grep -E "AudioTest"

```

---
$$$$
