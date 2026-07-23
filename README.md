# Anota+

App Android nativo (Kotlin) pra registrar gastos, recebimentos e
pensamentos rápidos com a menor fricção possível — a peça central é a
**Captura Rápida**, um modal translúcido que abre por cima da tela atual
sem precisar abrir o app inteiro.

Detalhes completos de arquitetura, decisões e histórico ficam em
[`PROJETO.md`](PROJETO.md). Este arquivo é só o essencial pra buildar e usar.

## Como buildar sem Android Studio (GitHub Actions)

Este repositório **não tem Gradle Wrapper commitado** e o usuário não tem
Android Studio nem SDK Android instalado localmente — o build só acontece
na nuvem, via GitHub Actions.

1. **Crie um repositório novo no GitHub** (pode ser privado) e suba o
   projeto:
   ```bash
   cd anotaplus
   git init
   git add .
   git commit -m "primeira versão do Anota+"
   git branch -M main
   git remote add origin https://github.com/SEU_USUARIO/anotaplus.git
   git push -u origin main
   ```
2. No GitHub, vá na aba **Actions**. O workflow "Build APK"
   (`.github/workflows/build.yml`) dispara sozinho a cada push na `main`.
   Se não disparar, clique em **Run workflow**.
3. Quando terminar (ícone verde ✅), abra o run, desça até **Artifacts** e
   baixe **anotaplus-debug** — um `.zip` contendo o `app-debug.apk`.
4. Transfira o APK pro celular (cabo USB, Google Drive, WhatsApp Web pra
   você mesmo — o que for mais fácil).
5. Se for a primeira vez instalando um APK fora da Play Store, o Android
   vai pedir pra habilitar **"Instalar apps desconhecidos"** pro app que
   você usou pra abrir o arquivo — autorize.
6. Toque no `.apk` baixado e instale.

Cada push na `main` gera um novo APK automaticamente — **por isso o fluxo
de trabalho aqui é sempre commitar e empurrar direto**, sem esperar por um
build local que não existe.

### Alternativa 100% no celular (sem PC)

Dá pra usar o app **AIDE** (IDE Android nativo, Play Store) apontando pra
essa mesma pasta de projeto, mas com suporte limitado a KSP/Room — pode
exigir ajustes. A rota GitHub Actions acima é a mais confiável hoje.

## Como abrir a Captura Rápida na hora

Não existe mais um único gesto obrigatório — escolha o que fizer sentido
pro seu aparelho (todos configuráveis em **Perfil > Suporte > Escolher
atalho rápido**, ou reveja em **Perfil > Suporte > Guia do gesto**):

- **Ladrilho de Configurações Rápidas** — funciona em qualquer Android,
  sem depender de fabricante. Puxe a barra de notificação, toque em editar
  ladrilhos e adicione "Anota+".
- **Gesto do fabricante** (ex.: Moto Actions > "Toque duas vezes na
  traseira" no Moto) — configurável só em aparelhos cujo fabricante
  permita escolher um app de terceiros pro gesto.
- **Widget de tela inicial** — 6 opções disponíveis (totais, atalho
  simples, atalho duplo Gasto/Anotação, "espelho" do modal, uma
  Ideia/checklist, gastos por categoria).
- **Atalho de ícone** — segure o ícone do app na tela inicial.

## Arquitetura (resumo — ver `PROJETO.md` para o resto)

- `QuickCaptureActivity`: única Activity `MAIN`/`LAUNCHER`, tema
  translúcido (`Theme.AnotaPlus.Modal`), `noHistory` + `excludeFromRecents`
  — aparece como card flutuante e some sem deixar rastro nos recentes.
- 4 abas na barra inferior: Anotações, Financeiro, Acompanhamento (Início)
  e Perfil (Conta) — cada uma sua própria Activity-raiz.
- `data/`: Room (SQLite local, versão 8) — `Entry`, `Category`, `Carteira`,
  `Meta`. Backup/restore na nuvem (não sync simultâneo) via backend próprio
  (NestJS/Prisma/Neon/Render), atrás de login Google e de um gate PRO
  (Play Billing).

## Próximos passos sugeridos

Ver a seção "Próximos passos conhecidos" em `PROJETO.md` — inclui contas
de desenvolvedor (Play Console/AdMob) ainda pendentes, exportar CSV, e
detalhes técnicos de features com limitação conhecida (widgets de 6
linhas fixas, categoria excluída não propaga pro backend).
