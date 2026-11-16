# Documentação do Harness do Agente

> Este documento atende ao requisito do teste técnico:
> *"Disponibilize a documentação do harness do agente, incluindo Specs, decisões arquiteturais, uso da IA (prompts e restrições) e os resultados que orientaram a implementação da solução"*

---

## 1. Visão Geral do Harness

Este projeto foi desenvolvido com auxílio de um **assistente de IA** (Claude — Anthropic), operando como parceiro de desenvolvimento orientado a produto.

### Modelo de desenvolvimento

O processo seguiu um fluxo PM-driven:

1. **Leitura e análise** do PDF do teste técnico para extração de requisitos
2. **Pesquisa de documentação** da Cielo Smart (WebFetch na documentação oficial)
3. **Tomada de decisões arquiteturais** baseada nos achados da pesquisa
4. **Implementação iterativa** com feedback contínuo do desenvolvedor
5. **Revisão e refinamento** arquitetural ao longo do desenvolvimento

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
| RNF-03 | Código organizado e de fácil manutenção | MVI + Clean Architecture; separação domain/data/presentation |
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

### ADR-003: Hilt como biblioteca de DI

**Contexto**: Estratégia de injeção de dependências.

**Opções avaliadas**:

| Critério | Manual DI | Koin | Hilt |
|---|---|---|---|
| Curva de aprendizado | Nenhuma | Baixa | Média |
| Verbosidade | Alta (AppContainer) | Baixa (DSL) | Média (annotations) |
| Verificação em compile-time | ❌ | ❌ | ✅ |
| Integração nativa com ViewModel | ❌ Manual (`Factory`) | ✅ Própria (`koinViewModel()`) | ✅ Nativa (`@HiltViewModel`) |
| `SavedStateHandle` automático | ❌ | Manual | ✅ Automático |
| Integração com Navigation Compose | Manual | Manual | ✅ `hiltViewModel()` |
| Padrão Android / Google recomendado | ❌ | ❌ | ✅ |

**Decisão**: Hilt

**Justificativa**:
1. Verificação de dependências em tempo de compilação — erros de DI não chegam em runtime
2. `@HiltViewModel` com `SavedStateHandle` automático — elimina `ViewModelProvider.Factory` em todos os ViewModels
3. `hiltViewModel()` no Navigation Compose injeta o ViewModel já com os navigation args via `SavedStateHandle`
4. Padrão recomendado pelo Android Jetpack

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

**Contexto**: `ResponseActivity` recebe o URI de retorno do Cielo e precisa entregar o resultado ao `CheckoutViewModel`, que roda em outro escopo de Activity.

**Decisão**: Intent de `ResponseActivity` → `MainActivity` + `SharedFlow` singleton (`PaymentResultChannel`)

**Fluxo implementado**:
1. `ResponseActivity` captura a URI e redireciona para `MainActivity` via Intent com a URI como extra
2. `MainActivity` recebe a Intent em `onNewIntent` e emite a URI no `PaymentResultChannel`
3. `CheckoutViewModel` coleta o `PaymentResultChannel` e processa o resultado

**Por que centralizar a emissão na MainActivity**:
- `ResponseActivity` inicialmente emitia diretamente no canal *e* enviava a Intent — causando double emission: o `CheckoutViewModel` poderia receber o mesmo resultado duas vezes, e o segundo evento ficaria bufferizado para o próximo checkout
- Centralizar em `MainActivity` garante fonte única de emissão, eliminando o risco de processar o resultado de um pagamento anterior em uma compra futura

**Por que SharedFlow e não apenas Intent extras**:
- `CheckoutViewModel` não tem acesso direto às Intents da Activity
- `SharedFlow` com `extraBufferCapacity=1` desacopla o emissor (MainActivity) do consumidor (ViewModel), sem exigir escopo compartilhado de Activity

**Alternativas descartadas**:
- EventBus (Otto, GreenBus) → biblioteca extra desnecessária
- SharedViewModel → exige escopo de Activity compartilhado explicitamente gerenciado

---

### ADR-007: MVI como padrão de arquitetura de apresentação

**Contexto**: Revisão arquitetural durante o desenvolvimento identificou que os ViewModels já seguiam parcialmente os princípios de MVI (estado imutável via `data class`, `StateFlow` unidirecional), mas expunham métodos públicos sem um contrato de entrada explícito.

**Decisão**: MVI (Model-View-Intent) completo

**Mudanças implementadas**:
1. ViewModels com ações do usuário (`EventListViewModel`, `EventDetailViewModel`, `CheckoutViewModel`, `PurchaseHistoryViewModel`) definem uma `sealed class XIntent` como contrato único de entrada
2. Um único método `processIntent(intent: XIntent)` substitui os métodos públicos dispersos
3. Funções de lógica interna tornadas `private` — a UI só acessa `processIntent()` e o `StateFlow`
4. `UiState` como `data class` imutável — sem mutações parciais expostas
5. ViewModels somente de leitura (`PurchaseResultViewModel`) não definem Intent — expõem apenas estado; não há ação do usuário além de observar
6. Efeitos colaterais de um disparo (`CheckoutViewModel`) entregues via `Channel<CheckoutSideEffect>` + `receiveAsFlow()` — o ViewModel **não** acessa `Context`, `startActivity` nem `packageManager`; emite apenas uma `sealed class CheckoutSideEffect` que a UI coleta em `LaunchedEffect(Unit)` e executa

**Justificativa**:
1. Fluxo de dados unidirecional: Intent → ViewModel → State → UI; efeitos colaterais via SideEffect Channel
2. Facilita o rastreamento de bugs — cada evento tem origem e destino claros
3. Mais testável: um teste por intent; sem necessidade de testar combinações de chamadas de métodos
4. Separação explícita entre o que a UI pode fazer (intents) e o que ela não pode (implementação privada)
5. ViewModel sem `Context` — sem referência ao ciclo de vida de Activity; sem risco de memory leak

**Trade-off aceitado**: Um pouco mais de boilerplate (sealed class por tela + sealed class de SideEffect onde necessário)

---

## 4. Uso da IA — Pesquisa e Restrições

### 4.1 Análise do PDF

O assistente de IA leu o arquivo `Case Android.pdf` e extraiu:
- Requisitos funcionais (5 fluxos obrigatórios + QR Code opcional)
- Requisitos não-funcionais (erros, idempotência, testes, documentação)
- Restrições técnicas (Kotlin, sem backend, emulador Cielo)
- Materiais obrigatórios (URLs de documentação Cielo)

### 4.2 Pesquisa de documentação Cielo

**Consultas realizadas**:

| URL consultada | Achado relevante | Impacto na implementação |
|---|---|---|
| `docs.cielo.com.br/cielo-smart/docs/conheca-a-cielo-smart` | Plataforma Android com 4 modelos de integração | Confirmou viabilidade de Deep Link |
| `docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo` | Emulador Android 7/10, APK em S3 da Cielo | Instruções para o README |
| `docs.cielo.com.br/cielo-smart/llms.txt` | Índice completo da documentação | Mapeou todas as páginas disponíveis |
| `docs.cielo.com.br/cielo-smart/docs/configurando-o-android-manifest` | `<queries>` para `com.ads.lio.uriappclient`; `ResponseActivity` com scheme `order` | Configuração do AndroidManifest |
| `docs.cielo.com.br/cielo-smart/docs/codigos-de-erro` | Erros em Base64 JSON: code 1=cancelado, 2=genérico, 3=falha, 4=autenticação | Tratamento de erros em `CieloDeepLinkService` |
| `docs.cielo.com.br/cielo-smart/docs/modelos-de-integracao` | Dois modelos: Deep Link e Remote API | Confirmou Deep Link como melhor opção |
| `github.com/DeveloperCielo/LIO-SDK-Sample-Integracao-Local` | Formato do JSON de request; campos de response (`authCode`, `cieloCode`); URL format `lio://payment?request={base64}&urlCallback=order://response` | Modelagem dos modelos de dados; URI format |
| Logcat (análise em runtime com emulador v1.61.8) | Resposta real do Cielo: objeto `Order` com `status:"PAID"`, não `statusCode:1`; Base64 com quebras de linha requer `Base64.DEFAULT` ao decodificar | Corrigiu o parsing de resposta em `CieloDeepLinkService` e `CieloOrderResponse` |

### 4.3 Restrições configuradas para a implementação

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

### 4.4 Decisões tomadas durante a implementação

| Ponto de decisão | Opções consideradas | Escolha | Motivo |
|---|---|---|---|
| Integração Cielo | SDK local vs Deep Link | Deep Link | SDK depreciado; recomendação oficial |
| UI toolkit | Compose vs XML | Compose | Padrão atual; mais testável |
| Arquitetura de apresentação | MVVM vs MVI | MVI | Fluxo unidirecional; sealed intents como contrato explícito |
| DI | Hilt vs Koin vs Manual | Hilt | Verificação compile-time; `@HiltViewModel` nativo; padrão Jetpack |
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
| `CieloDeepLinkServiceTest` | JSON válido no request; value em reais (não centavos); installments=0; items com nome do evento; SKU dos últimos 8 chars; urlCallback correto; parsing de aprovação (`status:"PAID"`); parsing de cancelamento; parsing de erro; Base64 encoding correto |
| `UpdatePurchaseResultUseCaseTest` | statusCode 1→APPROVED; statusCode 2→CANCELLED; statusCode 3→DENIED; statusCode 4→DENIED; dados de aprovação persistidos; motivo do erro persistido |
| `PurchaseEntityMappingTest` | Purchase→Entity preserva campos; Entity→Purchase recupera enum; round-trip consistente; formatação de preço (R$100,00; R$12,50; R$0,99) |

### Estratégia de testes

Foco em **cenários críticos de negócio**, não cobertura de linha:
1. Idempotência — o ponto mais crítico para evitar cobranças duplicadas
2. Mapeamento de status Cielo → domínio — erros aqui resultam em status incorretos no histórico
3. Serialização/deserialização do protocolo Cielo — erros aqui quebram a integração de pagamento

---

## 6. Pontos de Melhoria Identificados (para apresentação)

### O que funcionaria diferente em produção

1. **Credenciais**: `CLIENT_ID` e `ACCESS_TOKEN` em BuildConfig via Gradle secrets, não hardcoded
2. **Idempotência mais robusta**: checar também no Cielo se o orderId já foi processado (via API de consulta)
3. **Retry policy**: retentar automaticamente em erros de rede (não em erro de pagamento)
4. **Logging estruturado**: `Timber` + analytics para rastrear falhas de pagamento em produção
5. **Testes de integração**: Room em memória para testar os repositórios de verdade
6. **Testes instrumentados expandidos**: Compose UI Testing para navegação e fluxo de pagamento end-to-end (atualmente coberto o smoke test de inicialização)

### Trade-offs que seriam diferentes em um app de produção

- **Eventos mockados → API**: com um backend, poderíamos ter disponibilidade em tempo real e controle de estoque
- **Hilt → Manter Hilt**: à medida que o app cresce, o gerenciamento de escopos e a verificação em compile-time do Hilt se tornam ainda mais valiosos
