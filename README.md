# Anota+

App Android nativo (Kotlin) pra registrar gastos e pensamentos rápidos, pensado
pra abrir instantaneamente via gesto "toque duas vezes na traseira" do Moto G75.

## Como buildar sem Android Studio (GitHub Actions)

O projeto já vem com um workflow (`.github/workflows/build.yml`) que compila
o APK na nuvem, sem precisar instalar SDK nem IDE na sua máquina.

1. **Crie um repositório novo no GitHub** (pode ser privado) e suba o projeto:
   ```bash
   cd anotaplus
   git init
   git add .
   git commit -m "primeira versão do Anota+"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/anotaplus.git
   git push -u origin main
   ```
2. No GitHub, vá na aba **Actions** do repositório. O workflow "Build APK"
   já deve estar rodando automaticamente (ele dispara a cada push na `main`).
   Se não disparar sozinho, clique em **Run workflow**.
3. Quando terminar (ícone verde ✅), abra o run, desça até **Artifacts** e
   baixe **anotaplus-debug** — é um `.zip` contendo o `app-debug.apk`.
4. **Transfira o APK pro celular** (cabo USB, Google Drive, WhatsApp Web
   pra você mesmo, o que for mais fácil).
5. No Moto G75, se for a primeira vez instalando um APK fora da Play Store,
   o Android vai pedir pra habilitar **"Instalar apps desconhecidos"** pro
   app que você usou pra abrir o arquivo (Arquivos, Chrome, etc.) — autorize.
6. Toque no `.apk` baixado no celular e instale.

Isso te dá o app instalado sem precisar de Android Studio nem PC com SDK.
Cada vez que você (ou eu) alterar o código, um novo push gera um novo APK
automaticamente.

### Alternativa 100% no celular (sem PC)

Se quiser editar/buildar direto no Android, dá pra usar o app **AIDE**
(IDE Android nativo, disponível na Play Store) apontando pra essa mesma
pasta de projeto — só que ele tem suporte limitado a KSP/Room, então pode
exigir ajustes. A rota GitHub Actions acima é a mais confiável pro projeto
como está.

## Como configurar o gesto da traseira

No Moto G75:

1. Vá em **Configurações > Moto > Moto Actions** (ou "Gestos Moto").
2. Toque em **"Toque duas vezes na parte de trás"** (Quick Capture / Rear Tap,
   o nome varia conforme a versão do Android).
3. Selecione **abrir um app** e escolha **Anota+**.
4. Pronto — dois toques na traseira abrem o card por cima da tela atual.

## Arquitetura

- `QuickCaptureActivity`: única Activity `MAIN/LAUNCHER`, com tema translúcido
  (`Theme.AnotaPlus.Modal`), `noHistory` e `excludeFromRecents` — por isso ela
  aparece como um card flutuante e some sem deixar rastro nos apps recentes.
- `HistoryActivity`: lista tudo que foi salvo, acessível pelo botão "Histórico"
  dentro do modal.
- `data/`: Room (SQLite local) — `Entry`, `EntryDao`, `AppDatabase`. Sem sync
  com backend por enquanto (fica pra uma v2, se você quiser puxar pro seu
  NestJS depois).

## Próximos passos sugeridos

- Sync com um backend NestJS (endpoint `POST /entries`), reaproveitando o
  parser de notificações que você já tinha desenhado pro Tasker.
- Exportar histórico em CSV.
- Widget de tela inicial mostrando o total de gastos do dia/mês.
