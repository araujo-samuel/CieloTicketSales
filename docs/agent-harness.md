# Documentação do Harness do Agente

> Este documento atende ao requisito do teste técnico:
> *"Disponibilize a documentação do harness do agente, incluindo Specs, decisões arquiteturais, uso da IA (prompts e restrições) e os resultados que orientaram a implementação da solução"*

---

## 1. Visão Geral do Harness

Este projeto foi desenvolvido com auxílio de um **sistema multi-agente** chamado *Samuca Software House*, baseado no modelo Claude (Anthropic) e orquestrado via Claude Code CLI.

### Arquitetura do sistema de agentes

```
Usuário
   ↓
Product Manager Agent (PM)
   ├── demand-intake: identifica projeto, entrega, fatos disponíveis
   ├── Pesquisa de documentação (WebFetch)
   ├── Decisões arquiteturais
   └── General-purpose Agent
         └── Implementação do código
```

O **PM Agent** é o único ponto de contato com o usuário. Ele:
1. Leu e analisou o PDF do teste técnico
2. Pesquisou a documentação da Cielo Smart
3. Tomou as decisões arquiteturais com base nos achados
4. Escreveu toda a implementação

---

## 2. Specs — Especificações de entrada

### 2.1 Requisitos funcionais identificados (do PDF)

| # | Requisito | Implementado em |
|---|---|---|
| RF-01 | Visualizar eventos disponíveis | `EventListScreen` + `EventRepositoryImpl` |
| RF-02 | Selecionar quantidade de ingressos | `EventDetailScreen` + `EventDetailViewModel` |
| RF-03 | Iniciar e concluir pagamento via Cielo | `CheckoutScreen` + `CieloDeepLinkService` |
| RF-04 | Registrar resultado (aprovada/negada/cancelada) | `UpdatePurchaseResultUseCase` + Room |
| RF-05 | Exibir comprovante/resumo da compra | `PurchaseResultScreen` |
| RF-06 (opt) | Geração de QR Code do ingresso | `QrCodeGenerator` + `PurchaseResultScreen` |

### 2.2 Requisitos não-funcionais identificados

| # | Requisito | Estratégia de implementação |
|---|---|---|
| RNF-01 | Tratamento explícito de erros | `runCatching` em todos os pontos de integração; estados de erro nos ViewModels; mensagens ao usuário |
| RNF-02 | Prevenção de duplicidade de cobrança | UUID como orderId; verificação de PENDING antes de criar nova ordem; `PaymentResultChannel` com `extraBufferCapacity=1` |
| RNF-03 | Código organizado e de fácil manutenção | MVVM + Clean Architecture; separação domain/data/presentation |
| RNF-04 | Testes automatizados para cenários críticos | 4 suites de testes unitários (ver seção 5) |
| RNF-05 | Uso de IA documentado | Este documento |

### 2.3 Restrições reconhecidas

| Restrição | Fonte | Decisão |
|---|---|---|
| Kotlin (Android) | PDF do teste | Kotlin 2.0.21, minSdk 26 |
| Sem backend obrigatório | "Não será avaliado a construção ou não de um backend" | Eventos mockados |
| Executável com instruções no README | PDF do teste | README completo com setup |
| Sem WebView | Documentação Cielo Smart | Não utilizado |

---

## 3. Decisões Arquiteturais (ADRs)

### ADR-001: Deep Link vs SDK Local LIO

**Contexto**: Cielo oferece duas abordagens de integração: SDK local (via AAR) e Deep Link (via Intents).

**Evidência que motivou a decisão** (resultado de pesquisa — ver seção 4):
- A documentação oficial diz: *"SDK is being discontinued; only critical patches will be released. Deeplink integration recommended for future-proofing"*
- O prazo de migração obrigatória para Cielo Smart era outubro/2025

**Decisão**: Deep Link Integration

**Justificativa**:
1. Alinhado com a direção oficial da Cielo
2. Sem dependência de AAR externo — menor acoplamento
3. Funciona com o emulador sem configuração adicional
4. Mais simples de manter e testar

**Trade-off aceitado**: Menos controle sobre o fluxo de pagamento (a tela de checkout é do Cielo, não customizável)

---

### ADR-002: Jetpack Compose vs XML Views

**Contexto**: Escolha do toolkit de UI Android.

**Decisão**: Jetpack Compose

**Justificativa**:
1. Padrão atual recomendado pelo Google para novos apps Android
2. Código mais conciso e declarativo
3. Melhor integração com ViewModel/StateFlow
4. `@Composable` é intrinsecamente mais testável

**Trade-off aceitado**: Sem suporte a dark mode automático (simplificado para o escopo do teste)

---

### ADR-003: DI Manual vs Hilt

**Contexto**: Estratégia de injeção de dependências.

**Decisão**: Manual DI via `AppContainer`

**Justificativa**:
1. Simplicidade — todas as dependências em um arquivo
2. Sem annotation processing extra (já temos KSP para Room)
3. Mais explícito para code review
4. Escopo adequado ao tamanho do projeto

**Trade-off aceitado**: Sem gerenciamento automático de escopos; menos escalável

---

### ADR-004: Room como banco de dados

**Contexto**: Persistência do histórico de compras.

**Evidência**: Necessidade de queries por `eventId` e `status` (para idempotência) — SharedPreferences não atende.

**Decisão**: Room (SQLite ORM oficial Android)

**Justificativa**:
1. Suporte nativo a Coroutines e Flow
2. Type-safe com annotations
3. Testável com banco em memória
4. `@Query` SQL permite as queries necessárias para idempotência

---

### ADR-005: Estratégia de idempotência

**Contexto**: Prevenção de duplicidade de cobrança (requisito explícito do teste).

**Decisão**: UUID gerado localmente como chave de idempotência em dois níveis

**Nível 1 — Local (antes de lançar o Deep Link)**:
- Ao criar uma ordem, verifica se já existe `PENDING` para o mesmo evento
- Se sim, reutiliza a mesma ordem — não cria nova

**Nível 2 — Cielo (na requisição)**:
- O mesmo UUID é enviado como `orderId` ao Cielo
- Reenvios com o mesmo `orderId` são identificados pelo terminal como duplicata

**Resultado**: Duplo bloqueio contra cobranças duplicadas

---

### ADR-006: `PaymentResultChannel` como comunicação entre Activities

**Contexto**: `ResponseActivity` recebe o resultado do Cielo e precisa passar para `CheckoutViewModel`.

**Opções consideradas**:
1. Intent extras na MainActivity → acoplamento entre Activities
2. EventBus (Otto, GreenBus) → biblioteca extra desnecessária
3. SharedViewModel → exige escopo compartilhado de Activity
4. `SharedFlow` singleton → simples, sem dependências extras

**Decisão**: `SharedFlow` singleton (`PaymentResultChannel`)

**Justificativa**: Padrão Kotlin idiomático, sem biblioteca adicional, `extraBufferCapacity=1` garante que o evento não seja perdido se o ViewModel ainda não está coletando.

---

## 4. Uso da IA — Prompts e Resultados de Pesquisa

### 4.1 Entrada inicial (usuário → PM Agent)

```
"Rode a software house no projeto que está aberto. Iremos desenvolver um teste
tecnico. É importante seguir todas as regras e orientações do teste explicitamente.
Incluindo a parte sobre documentação."
```

### 4.2 Análise do PDF (PM Agent → WebFetch)

O PM Agent leu o arquivo `Case Android.pdf` e extraiu:
- Requisitos funcionais (5 fluxos obrigatórios + QR Code opcional)
- Requisitos não-funcionais (erros, idempotência, testes, documentação)
- Restrições técnicas (Kotlin, sem backend, emulador Cielo)
- Materiais obrigatórios (URLs de documentação Cielo)

### 4.3 Pesquisa de documentação Cielo (PM Agent → WebFetch)

**Consultas realizadas**:

| URL consultada | Achado relevante | Impacto na implementação |
|---|---|---|
| `docs.cielo.com.br/cielo-smart/docs/conheca-a-cielo-smart` | Plataforma Android com 4 modelos de integração | Confirmou viabilidade de Deep Link |
| `docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo` | Emulador Android 7/10, APK em S3 da Cielo | Instruções para o README |
| `docs.cielo.com.br/cielo-smart/llms.txt` | Índice completo da documentação | Mapeou todas as páginas disponíveis |
| `docs.cielo.com.br/cielo-smart/docs/configurando-o-android-manifest` | `<queries>` para `com.ads.lio.uriappclient`; `ResponseActivity` com scheme `order` | Configuração do AndroidManifest |
| `docs.cielo.com.br/cielo-smart/docs/codigos-de-erro` | Erros em Base64 JSON: code 1=cancelado, 2=genérico, 3=falha, 4=autenticação | Tratamento de erros em `CieloDeepLinkService` |
| `docs.cielo.com.br/cielo-smart/docs/modelos-de-integracao` | Dois modelos: Deep Link e Remote API | Confirmou Deep Link como melhor opção |
| `github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local` | Format do JSON de request: `{ value (String em reais), items (obrigatório), installments: 0 (à vista) }`. URL format `lio://payment?request={base64}&urlCallback=order://response`. **Nota**: formato do response diverge do sample — ver linha abaixo | Modelagem de CieloPaymentRequest; URI format |
| Logcat com emulador v1.61.8 (análise em runtime) | O Cielo Smart retorna um objeto `Order` completo no parâmetro `response`: `{ "status": "PAID", "payments": [{ "authCode", "cieloCode", "brand" }] }` — sem campo `statusCode` no topo. Base64 gerado com quebras de linha (`\n`), requer `Base64.DEFAULT` ao decodificar. | Reformulação completa de `CieloOrderResponse`; atualização de `parsePaymentResponse()` |

### 4.4 Restrições configuradas para a implementação

```
1. Sem backend — apenas armazenamento local (Room)
2. Sem WebView — proibido pela Cielo Smart
3. Sem SDK local LIO — usar apenas Deep Link
4. minSdk 26, targetSdk 36, Kotlin 2.0.21
5. Kotlin idiomático: coroutines, flow, data classes, sealed classes
6. Idempotência obrigatória em dois níveis
7. Tratamento de erros em todos os pontos de integração
8. Documentação como artefato de primeira classe (não afterthought)
```

### 4.5 Decisões tomadas durante a implementação

| Ponto de decisão | Opções consideradas | Escolha | Motivo |
|---|---|---|---|
| Integração Cielo | SDK local vs Deep Link | Deep Link | SDK depreciado; recomendação oficial |
| UI toolkit | Compose vs XML | Compose | Padrão atual; mais testável |
| DI | Hilt vs Koin vs Manual | Manual | Simplicidade para o escopo |
| Banco de dados | Room vs SharedPreferences vs SQLite direto | Room | Queries SQL necessárias para idempotência |
| Comunicação Activities | Intent vs EventBus vs SharedFlow | SharedFlow singleton | Idiomatic Kotlin; sem deps extras |
| QR Code | ZXing embedded vs ZXing core vs Google MLKit | ZXing core | Leve; sem UI extra; sem Activity |
| Serialização JSON | Kotlin Serialization vs Gson vs Moshi | Gson | Simples; sem annotation processing |
| Testes | Mockito vs MockK | MockK | Nativo para Kotlin; suporta classes `final` |

---

## 5. Cobertura de Testes

### Suites implementadas

| Arquivo | Cenários testados |
|---|---|
| `CreatePurchaseOrderUseCaseTest` | Criação com UUID único; idempotência (reutiliza PENDING); cálculo correto do total; validação de quantidade inválida |
| `CieloDeepLinkServiceTest` | JSON decodificável; `value` em reais como String; `installments=0` para à vista; campo `items` obrigatório com nome do evento; SKU derivado do orderId; constante `CALLBACK_URI`; parsing de aprovação (`status=PAID`→statusCode=1); parsing de cancelamento pelo usuário; parsing de erro genérico; URI sem parâmetros |
| `UpdatePurchaseResultUseCaseTest` | statusCode 1→APPROVED; statusCode 2→CANCELLED; statusCode 3→DENIED; statusCode 4→DENIED; dados de aprovação persistidos; motivo do erro persistido |
| `PurchaseEntityMappingTest` | Purchase→Entity preserva campos; Entity→Purchase recupera enum; round-trip consistente; formatação de preço (R$100,00; R$12,50; R$0,99) |

### Estratégia de testes

Foco em **cenários críticos de negócio**, não cobertura de linha:
1. Idempotência — o ponto mais crítico para evitar cobranças duplicadas
2. Mapeamento de status Cielo → domínio — erros aqui resultam em status incorretos
3. Serialização/deserialização do protocolo Cielo — erros aqui quebram a integração

---

## 6. Pontos de Melhoria Identificados (para apresentação)

### O que funcionaria diferente em produção

1. **Credenciais**: `CLIENT_ID` e `ACCESS_TOKEN` em BuildConfig via Gradle secrets, não hardcoded
2. **Idempotência mais robusta**: checar também no Cielo se o orderId já foi processado (via API de consulta)
3. **Retry policy**: retentar automaticamente em erros de rede (não em erro de pagamento)
4. **Logging estruturado**: `Timber` + analytics para rastrear falhas de pagamento em produção
5. **Testes de integração**: Room em memória para testar os repositórios de verdade
6. **Testes instrumentados**: Compose UI Testing para o fluxo completo

### Trade-offs que se tornaria diferente

- **DI manual → Hilt**: em um app que crescesse, o gerenciamento de escopos do Hilt valeria a complexidade
- **Eventos mockados → API**: com um backend, poderíamos ter disponibilidade em tempo real e controle de estoque
