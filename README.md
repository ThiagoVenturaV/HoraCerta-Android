# Hora Certa

Aplicativo Android nativo para organizar medicamentos, criar alarmes exatos e acompanhar a adesão ao tratamento. Os dados ficam apenas no aparelho; esta versão não exige conta nem conexão com a internet para funcionar.

## Funcionalidades

- onboarding com nome e permissões necessárias;
- medicamentos em horários fixos e em intervalos de horas;
- criação automática das doses e dos alarmes;
- alarme em tela cheia, som e vibração;
- soneca automática a cada 15 minutos até a confirmação;
- restauração dos alarmes após reiniciar o celular;
- confirmação reversível para corrigir toques acidentais;
- estoque opcional por medicamento, com baixa por dose, alerta de estoque baixo e reposição rápida;
- lista diária, calendário e sequência de dias sem doses perdidas;
- armazenamento local em SQLite.

## APK pronto

O artefato assinado é gerado em `release/HoraCerta-v0.2.0.apk`.

Para instalar manualmente, transfira o arquivo para o celular, abra-o e autorize a instalação de apps desconhecidos para o aplicativo usado na abertura do arquivo. Na primeira execução, conclua as permissões de notificações, alarmes exatos e tela cheia.

## Recompilar

No PowerShell, execute:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\build-release.ps1
```

O projeto usa Kotlin, Jetpack Compose, Android API 36, AGP 9.2.0 e Gradle 9.4.1. A chave de release é gerada na primeira compilação e deve ser mantida em backup seguro conforme `signing/README-SEGURANCA.md`.

## Executar no emulador deste PC

O aparelho virtual `HoraCerta_Pixel7_API36` usa um perfil Pixel 7, resolução 1080×2400 e Android 16/API 36. Para abri-lo novamente e iniciar o app:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-emulator.ps1
```

O perfil Pixel testa o Android padrão. Alarmes com o aparelho bloqueado, reinicialização e restrições do HyperOS devem ser validados também no Poco M6 Pro físico.

## Observação importante

O Hora Certa é uma ferramenta de organização e não substitui orientação médica. Horários, doses e alterações no tratamento devem seguir a prescrição de um profissional de saúde.
