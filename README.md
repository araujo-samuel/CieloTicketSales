# Cielo Ticket Sales

App Android para venda de ingressos de eventos com pagamento integrado via Cielo Smart.

---

## Instruções de execução

### Pré-requisitos

- Android Studio Ladybug (2024.2.1) ou superior
- JDK 11+
- Dispositivo ou emulador Android com **minSdk 26** (Android 8.0+)
- **Emulador Cielo Smart** instalado no mesmo dispositivo/emulador

### Instalar o Emulador Cielo Smart

1. Baixe o APK do emulador: https://docs.cielo.com.br/cielo-smart/docs/baixando-o-emulador-cielo
2. Instale no dispositivo/emulador Android (permitir fontes desconhecidas se necessário)
3. Abra o emulador uma vez antes de testar pagamentos

### Executar o app

```bash
git clone https://github.com/araujo-samuel/CieloTicketSales.git
cd CieloTicketSales
# Abra no Android Studio e clique em Run App
```

Ou via linha de comando:

```bash
./gradlew installDebug
```

### Executar os testes

```bash
./gradlew test          # Testes unitários
./gradlew connectedTest # Testes instrumentados (requer dispositivo)
```

---

## Arquitetura

### Padrão: MVI + Clean Architecture

```
Presentation (Compose + ViewModel + sealed Intent)
      ↓
  Domain (UseCases + Entities)
      ↓
  Data (Room + Repositories + Cielo)
```

**Presentation**: Telas Jetpack Compose + ViewModels com StateFlow. Cada tela define uma `sealed class XIntent` como contrato único de entrada; a UI apenas chama `processIntent()` e observa o `UiState` imutável. Navegação via Navigation Compose com `hiltViewModel()`.

**Domain**: Entidades puras (`Event`, `Purchase`) e Use Cases que encapsulam a lógica de negócio (`CreatePurchaseOrderUseCase`, `UpdatePurchaseResultUseCase`). Sem dependência de frameworks Android.

**Data**: Repositórios que abstraem as fontes de dados. `EventRepositoryImpl` fornece eventos mockados. `PurchaseRepositoryImpl` persiste no Room. `CieloDeepLinkService` encapsula toda a integração com a Cielo.

**DI via Hilt**: `@HiltViewModel` em todos os ViewModels; `SavedStateHandle` injetado automaticamente para navegação type-safe; verificação de dependências em tempo de compilação.

---

## Integração com Cielo Smart

### Abordagem: Deep Link Integration

O app utiliza Deep Link, a abordagem **recomendada pela Cielo** para aplicativos na plataforma Cielo Smart. O SDK local LIO está em processo de descontinuação.

### Fluxo de pagamento

```
1. Usuário confirma compra no CheckoutScreen
       ↓
2. CieloDeepLinkService monta JSON do pedido:
   { accessToken, clientID, paymentCode, value (reais), items, installments: 0 }
       ↓
3. JSON é codificado em Base64
       ↓
4. Intent lançada com URI:
   lio://payment?request={base64}&urlCallback=order://response
       ↓
5. Cielo Smart processa o pagamento (tela do terminal)
       ↓
6. Cielo chama de volta via: order://response?response={base64}
   ou em caso de erro:       order://response?error={base64}
       ↓
7. ResponseActivity captura a URI e emite no PaymentResultChannel
       ↓
8. CheckoutViewModel processa o resultado via CieloDeepLinkService
       ↓
9. UpdatePurchaseResultUseCase persiste o status no Room
       ↓
10. Navegação automática para PurchaseResultScreen
```

### Configuração do AndroidManifest

```xml
<!-- Permite detectar o serviço Cielo Smart (Android 11+) -->
<queries>
    <package android:name="com.ads.lio.uriappclient" />
</queries>

<!-- Captura o retorno do pagamento -->
<activity android:name=".presentation.ResponseActivity">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <data android:scheme="order" android:host="response" />
    </intent-filter>
</activity>
```

### Credenciais

Para o emulador Cielo, são usados valores de placeholder (`CLIENT_ID_EMULADOR`). Em produção, as credenciais devem ser obtidas no [Developer Portal da Cielo](https://desenvolvedores.cielo.com.br/api-portal/) e armazenadas de forma segura via BuildConfig + CI secrets.

---

## Prevenção de duplicidade de cobrança

**Estratégia**: UUID por pedido como chave de idempotência.

1. Ao clicar em "Comprar", um UUID é gerado e salvo no Room com status `PENDING`
2. O UUID é enviado como `orderId` na requisição Cielo
3. Se o usuário tentar comprar o mesmo evento enquanto há uma compra `PENDING`, o sistema reutiliza a mesma ordem (sem novo UUID → sem nova cobrança)
4. O Cielo identifica reenvios pelo `orderId` e não processa duplicatas

---

## Bibliotecas externas e justificativas

| Biblioteca | Versão | Justificativa |
|---|---|---|
| **Jetpack Compose BOM** | 2024.12.01 | UI moderna e declarativa — padrão atual de desenvolvimento Android. Menos boilerplate que XML Views, melhor suporte a testes |
| **Navigation Compose** | 2.9.0 | Navegação declarativa integrada ao Compose. Type-safe com as rotas definidas em `Routes` |
| **Room** | 2.7.1 | ORM oficial Android. Type-safe, suporte nativo a Coroutines/Flow, fácil de testar com banco em memória |
| **ZXing Core** | 3.5.3 | Geração de QR Code. Versão "core" sem dependência da UI Android — mais leve e sem Activities extras |
| **Gson** | 2.11.0 | Serialização/deserialização JSON para o protocolo Cielo Deep Link. Estável, sem annotation processing |
| **Kotlin Coroutines** | 1.9.0 | Programação assíncrona idiomática em Kotlin |
| **MockK** | 1.13.12 | Mocking idiomático para Kotlin. Suporta mock de objetos Kotlin nativamente (vs Mockito que tem limitações com classes `final`) |

---

## Trade-offs considerados

### Deep Link vs SDK local LIO
**Escolha**: Deep Link  
**Motivo**: O SDK local está sendo descontinuado. Deep Link é o caminho recomendado para Cielo Smart, sem dependência de AAR externo.  
**Contra**: Menos controle sobre a tela de pagamento; fluxo depende do app Cielo instalado.

### Jetpack Compose vs XML Views
**Escolha**: Compose  
**Motivo**: Padrão atual do ecossistema Android; mais testável; código mais conciso.  
**Contra**: Curva de aprendizado; interoperabilidade com bibliotecas legadas pode ser complexa.

### Hilt vs DI manual
**Escolha**: Hilt  
**Motivo**: Verificação de dependências em compile-time; `@HiltViewModel` com `SavedStateHandle` automático; padrão recomendado pelo Android Jetpack.  
**Contra**: Annotation processing adicional (via KSP); requer `@HiltAndroidApp` / `@AndroidEntryPoint` em todos os pontos de entrada.

### Room vs SharedPreferences
**Escolha**: Room  
**Motivo**: Consultas por status e eventId exigem SQL; SharedPreferences não atende.  
**Contra**: Setup mais verboso para armazenar apenas histórico simples.

### Eventos mockados vs Backend
**Escolha**: Mockados (hardcoded)  
**Motivo**: O teste técnico especifica explicitamente: "Não será avaliado a construção ou não de um backend de apoio".

---

## O que faria com mais tempo

1. **Backend real**: API REST para eventos e validação de ingressos
2. **Autenticação de usuário**: login para associar compras ao perfil
3. **Múltiplas formas de pagamento**: PIX, parcelado (já suportado pelo protocolo Cielo)
4. **Validação do QR Code**: endpoint para validar ingressos na entrada do evento
5. **Notificações push**: confirmação de compra e lembretes do evento
6. **Testes de integração com Room**: banco em memória para testar repositórios
7. **Testes instrumentados**: fluxo completo da UI com Compose Testing
8. **Cancelamento de compra**: integração com a API de cancelamento Cielo
9. **Offline-first**: sincronização quando a conexão for reestabelecida
10. **Acessibilidade**: talkback, contrastes, semântica nas telas Compose

---

## Documentação do Harness de IA

Ver: [`docs/agent-harness.md`](docs/agent-harness.md)
