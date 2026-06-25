# VeroPrinterStandalone

Projeto Delphi FMX simples e independente, criado apenas para impressao na Vero por imagem.

O objetivo deste pacote e ser autocontido:

- app Delphi Android;
- interface pronta no form principal;
- wrapper Delphi local;
- wrapper Java local;
- `.aar`, `.so` e `.jar` dentro do proprio projeto;
- geracao do JAR feita por um `.bat` dentro de `src/Java`.

Voce pode zipar a pasta `VeroPrinterStandalone` inteira e enviar para outra pessoa sem depender do restante do projeto.

## Objetivo

Este projeto faz somente o essencial:

- recebe um texto na tela principal;
- converte o texto em bitmap no Java;
- chama a SDK da Vero para imprimir o bitmap;
- mostra um status simples na tela.

Nada aqui depende do fluxo principal do DFeBrasil.

## Estrutura

```text
VeroPrinterStandalone/
|-- README.md
|-- assets/
|   `-- libs/
|       `-- vero/
|           |-- bcnewland-release-279-20241009.aar
|           |-- posmpapi-1.01.21-partnersRelease.aar
|           |-- positivo-printer-1.00.00.aar
|           |-- veroprinter-custom.jar
|           `-- so/
|               |-- libemvjni.so
|               |-- libintelligentLib.so
|               |-- libjniICServer.so
|               |-- libndkapi.so
|               |-- libnlprintex.so
|               `-- libpubjni.so
`-- src/
    |-- AndroidManifest.template.xml
    |-- VeroPrinterStandalone.dpr
    |-- VeroPrinterStandalone.dproj
    |-- VeroPrinterStandalone.deployproj
    |-- Services/
    |   `-- uVeroPrinter.Service.pas
    |-- View/
    |   |-- uMain.View.pas
    |   `-- uMain.View.fmx
    `-- Java/
        |-- build-custom-java.bat
        `-- com/
            `-- veroprinterstandalone/
                |-- PrintBitmapRenderer.java
                `-- VeroPrinterRouter.java
```

## Namespace e package

Este projeto usa um namespace proprio e isolado no wrapper novo.

O package Android e Java usado aqui e:

```text
com.veroprinterstandalone
```

## JAR customizado

O nome do JAR local deste projeto e:

```text
assets/libs/vero/veroprinter-custom.jar
```

Esse nome foi mantido limpo para nao carregar nomenclatura antiga.

## Tela principal

A interface foi montada no form principal, sem criacao dinamica em runtime.

Componentes principais:

- `TMemo` para digitar o texto;
- `TButton` para imprimir;
- labels de titulo e status.

Arquivo visual:

```text
src/View/uMain.View.fmx
```

Arquivo de codigo:

```text
src/View/uMain.View.pas
```

## Fluxo de impressao

O fluxo deste projeto e:

1. o usuario digita o texto;
2. o Delphi chama `TVeroPrinterService.ImprimirTexto`;
3. o wrapper Delphi envia o texto para o Java;
4. o Java renderiza em `Bitmap`;
5. o Java chama `Printer.print(bitmap, listener)`.

Isso segue exatamente o caminho que foi validado para a Vero: impressao por imagem.

## Wrapper Delphi

Arquivo:

```text
src/Services/uVeroPrinter.Service.pas
```

Responsabilidades:

- normalizar o texto;
- impedir envio de texto vazio;
- chamar a classe Java correta;
- escrever logs Android com tag `VeroStandalone`.

Assinatura Java usada pelo Delphi:

```text
com/veroprinterstandalone/VeroPrinterRouter
```

## Wrapper Java

Os fontes Java ficam em:

```text
src/Java/com/veroprinterstandalone/
```

Arquivos:

- `VeroPrinterRouter.java`
- `PrintBitmapRenderer.java`

### VeroPrinterRouter.java

Responsavel por:

- validar o modelo;
- abrir a impressora;
- enviar o bitmap;
- fechar a impressora no retorno do listener.

### PrintBitmapRenderer.java

Responsavel por:

- transformar texto em bitmap;
- suportar protocolo em bitmap;
- manter largura adequada para impressora termica.

## BAT para gerar o JAR

O arquivo de geracao fica em:

```text
src/Java/build-custom-java.bat
```

Ele foi colocado dentro de `src/Java` para ficar ao lado dos fontes Java e facilitar manutencao.

### O que o BAT faz

Ele:

- tenta localizar `JAVA_HOME`;
- tenta localizar `ANDROID_HOME` ou `ANDROID_SDK_ROOT`;
- encontra o `android.jar`;
- extrai os `classes.jar` dos `.aar`;
- compila os `.java` do namespace `com.veroprinterstandalone`;
- gera `assets/libs/vero/veroprinter-custom.jar`.

### Como usar

No Windows:

1. abra um terminal na pasta `src\Java`;
2. execute:

```bat
build-custom-java.bat
```

Se tudo estiver correto, o arquivo abaixo sera atualizado:

```text
assets/libs/vero/veroprinter-custom.jar
```

## Bibliotecas locais

Todas as dependencias da Vero ficam dentro do projeto.

### AAR

- `bcnewland-release-279-20241009.aar`
- `posmpapi-1.01.21-partnersRelease.aar`
- `positivo-printer-1.00.00.aar`

### SO

- `libemvjni.so`
- `libintelligentLib.so`
- `libjniICServer.so`
- `libndkapi.so`
- `libnlprintex.so`
- `libpubjni.so`

### JAR

- `veroprinter-custom.jar`

## Projeto Delphi

Abra no Delphi:

```text
src/VeroPrinterStandalone.dproj
```

Depois:

1. selecione Android;
2. confirme o device target;
3. faça `Clean`;
4. faça `Build`;
5. gere e instale o APK.

## Manifest, deploy e icones

O projeto foi mantido com icones padrao do Delphi.

Arquivos principais:

- `src/AndroidManifest.template.xml`
- `src/VeroPrinterStandalone.dproj`
- `src/VeroPrinterStandalone.deployproj`

## Instalacao do APK

Depois de gerar o APK, voce pode instalar com `adb`.

Exemplo:

```bash
adb install -r caminho/do/apk-gerado.apk
```

## Teste rapido

1. instale o APK;
2. abra o app no terminal Vero;
3. digite um texto no campo principal;
4. toque em `Imprimir`;
5. confira a saida na impressora.

## Logs

Para debug no Android, filtre por:

```text
VeroStandalone
```

## O que este projeto nao faz

Este projeto nao inclui:

- pagamento;
- servidor HTTP;
- integracao com TEF;
- integracao com o projeto principal;
- configuracao de varios providers.

Ele foi montado para ser uma base minima, simples e funcional de impressao Vero no Delphi.

## Fluxo recomendado para outra pessoa

Se outra pessoa receber este projeto zipado, o caminho mais simples e:

1. descompactar a pasta `VeroPrinterStandalone`;
2. abrir `src/VeroPrinterStandalone.dproj` no Delphi;
3. se alterar Java, rodar `src/Java/build-custom-java.bat`;
4. fazer `Clean` e `Build`;
5. instalar o APK na Vero.
