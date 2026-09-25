# Pendências da app Suds & Shine

Este documento contém apenas o que continua por decidir, preparar ou validar. As alterações já implementadas foram retiradas da lista.

## 1. Decisões de negócio por fechar

### Horários

- Confirmar o horário final: segunda a quinta das 10h às 19h e sexta a domingo das 10h às 20h.
- Confirmar que o domingo deve aceitar marcações.
- Depois da confirmação, usar o mesmo horário na agenda e na informação pública da app.

### Serviços ainda incompletos

- Definir separadamente o tempo de trabalho e o tempo de secagem da **Aspiração** (11,50 € para passageiros e 14 € para SUV).
- Definir separadamente o tempo de trabalho e o tempo de secagem da **Lavagem de Bagageira**, atualmente indicada como “1h+” (22,50 € para passageiros e 25,50 € para SUV).
- Definir separadamente o tempo de trabalho e o tempo de secagem da **Lavagem de Estofos em Tecido** (75 € para passageiros e 85 € para SUV).
- Confirmar se a Lavagem de Estofos em Tecido só pode ser marcada às 10h.
- Definir separadamente o tempo de trabalho e o tempo de secagem da lavagem de estofos por unidade.
- Confirmar se as 4 horas da **Lavagem de Estofos em Pele** são apenas trabalho ou se incluem secagem e, nesse caso, indicar a divisão exata.

Os tempos de secagem ainda não confirmados permanecem a zero. Não devem ser usados valores estimados.

### Polimento de faróis

Confirmar com o João se o **Polimento de Faróis** deve ser:

- um serviço independente de 1h30 e 37 €;
- um extra;
- ou ambas as opções.

### Convite de amigo

Confirmar com o João quem recebe o selo extra depois da primeira lavagem paga do amigo:

- quem fez o convite;
- o novo cliente;
- ou ambos.

### Emails

Confirmar se o cliente deve receber emails além das notificações push.

Se forem necessários emails, falta definir:

- em que momentos são enviados;
- qual o endereço de contacto a usar;
- se o cliente pode corrigir o email recebido da conta Apple ou Google antes de concluir a marcação.

## 2. Preparação de produção

- Atualizar no Firestore os serviços e extras já confirmados. Os novos valores predefinidos só são usados quando não existem registos de catálogo.
- Rever os registos antigos do catálogo para evitar serviços ou extras duplicados depois da atualização.
- Decidir como tratar marcações antigas que aparecem como concluídas, mas não têm o pagamento registado.
- Manter os selos históricos até existir uma decisão explícita para os alterar.
- Publicar primeiro as funções Firebase e depois as novas versões Android e iOS.

## 3. Validação em dispositivos reais

Validar o percurso completo com uma conta de cliente e uma conta de administrador:

1. O cliente cria uma marcação e o administrador recebe um único push.
2. O administrador aceita ou recusa e o cliente recebe o push correspondente.
3. Ao selecionar **Trabalho realizado**, o cliente recebe o aviso para levantar o carro.
4. A marcação fica a aguardar pagamento e ainda não atribui selo.
5. Ao selecionar **Marcar como pago**, o pagamento fica registado e é atribuído exatamente um selo.
6. Repetir ações ou voltar a abrir a app não duplica notificações nem selos.
7. Tocar em cada notificação abre a área correta da app.
8. Confirmar o comportamento em Android e iOS, com a app aberta, em segundo plano e fechada.

Também deve ser verificado que todos os dispositivos usados pelos administradores ficam registados para receber novas marcações.
