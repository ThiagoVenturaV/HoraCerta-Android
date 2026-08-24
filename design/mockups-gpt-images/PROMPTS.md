# Hora Certa - conjunto de prompts do GPT Images

Modo utilizado: ferramenta integrada de geração de imagens, categoria `ui-mockup`.

## Direção visual compartilhada

Mockup Android de alta fidelidade, pronto para orientar implementação em Jetpack Compose. Interface Material 3 personalizada, sem moldura de aparelho, em tela vertical. Fundo marfim quente, azul-cobalto profundo `#1746A2`, azul principal `#1D4ED8`, azul-claro `#DCE9FF`, texto azul-marinho e coral apenas para urgência. Tipografia grande, acessível, cartões arredondados, sombras suaves e ícone original combinando cápsula e relógio. Evitar verde, teal, menta, cruz médica, glassmorphism, texto pequeno, aparência hospitalar, marcas e watermark.

## Prompts por tela

1. **Onboarding - nome**: apresentar Hora Certa, perguntar "Como podemos chamar você?", campo "Seu nome" preenchido com "Thiago", explicação de privacidade, botão "Continuar" e progresso "1 de 5".
2. **Onboarding - como funciona**: ensinar o fluxo em três passos — "Cadastre", "Receba o alarme" e "Confirme a dose" — explicando a repetição em 15 minutos e que uma confirmação pode ser corrigida. Botão "Continuar" e progresso "2 de 5".
3. **Onboarding - notificações**: explicar os benefícios "Avisar na hora certa", "Mostrar nome e dosagem" e "Repetir a cada 15 min, se necessário". Botões "Permitir notificações" e "Agora não". Representar a explicação anterior à permissão real do Android, sem imitar o diálogo do sistema.
4. **Onboarding - alarmes exatos**: explicar que o acesso a "Alarmes e lembretes" permite tocar no horário exato com o app fechado e reagendar após reiniciar. Botão "Permitir alarmes exatos", nota "Abriremos as configurações do Android." e progresso "4 de 5".
5. **Onboarding - tela bloqueada**: explicar som, vibração, medicamento, horário e abertura da confirmação sobre a tela bloqueada. Botão "Permitir e concluir", nota sobre revisão nas configurações e progresso "5 de 5".
6. **Hoje**: saudação "Bom dia, Thiago", próxima dose "Losartana 50 mg" às "10:00", progresso diário e linha do tempo de Metformina, Losartana e Vitamina D. Navegação inferior com "Hoje" selecionado.
7. **Calendário**: calendário de "Agosto 2026", dia 24 selecionado, pontos de situação e lista das quatro doses do dia. Navegação inferior com "Calendário" selecionado.
8. **Medicamentos**: lista pesquisável com três medicamentos ativos, dosagem, frequência, botão "Adicionar", ações de pausa e navegação inferior com "Remédios" selecionado.
9. **Novo medicamento**: formulário com "Amoxicilina", "500 mg", frequência "A cada X horas", intervalo de 8 horas, primeira dose às 08:00 e prévia automática 08:00, 16:00 e 00:00. Datas, som, vibração e botão "Salvar medicamento".
10. **Progresso**: destaque "12 dias seguidos", adesão de 94%, últimos sete dias, 28 doses no horário, 3 com atraso e 0 perdidas. Reforço positivo, sem culpa ou premiações infantis.
11. **Alarme**: tela cheia azul de alta legibilidade, horário 10:00, "Losartana 50 mg", botões "Desligar e confirmar" e "Adiar 15 min", informando que o lembrete continuará até a confirmação.
12. **Confirmar dose**: cartão da dose das 10:00, pergunta "Você tomou este medicamento?", controle reversível "Remédio tomado", botões "Confirmar" e "Ainda não" e aviso de novo alarme às 10:15.

## Regra de edição aplicada

Os rascunhos iniciais em verde foram recoloridos preservando layout, textos e componentes: superfícies verde-escuras passaram a azul-cobalto, superfícies menta passaram a azul-claro e estados de confirmação passaram a azul acessível. A tela final de permissão da tela bloqueada recebeu uma correção textual pontual para "Para você não perder a dose.".
