# Decisões de implementação — API Território Limpo

Registro curto das decisões tomadas durante a implementação, uma linha por decisão.

- **Build**: projeto migrado para Maven, conforme o prompt. O scaffold tinha vindo com Gradle; os arquivos Gradle foram removidos e foi gerado o Maven Wrapper. Comandos: `./mvnw verify`, `./mvnw spring-boot:run`.
- **Java**: código compilado com `--release 21`, compatível com Java 21 ou superior. Motivo: o ambiente de desenvolvimento tem JDK 26 e o projeto exige Java 21.
- **Migration V1**: o repositório não continha `V1__init_schema.sql`, apesar de o prompt afirmar que existia. A V1 foi criada com as tabelas, colunas, CHECKs, índices, view e RLS descritos no prompt, e passa a ser a fonte da verdade.
- **Coluna extra**: `ocorrencia.manual_priority_reason` foi incluída na V1 para guardar o motivo opcional do ajuste manual de prioridade, que não tinha coluna especificada.
- **Postgres local**: `docker-compose.yml` com `postgres:17` para desenvolvimento; o `compose.yaml` original foi removido para evitar duplicidade e o suporte automático a Docker Compose foi retirado das dependências.
- **Storage em dev**: quando `SUPABASE_URL`/`SUPABASE_SERVICE_KEY` não estão configurados, o envio de fotos falha de forma controlada e a ocorrência é salva mesmo assim, conforme regra do produto.
- **Testes de integração**: Testcontainers com PostgreSQL real; quando não há Docker disponível, os testes de integração são pulados via `disabledWithoutDocker`. Testes unitários continuam rodando.
- **Sem Lombok/MapStruct**: DTOs são `record` e o mapeamento é manual, conforme o prompt.
- **`updated_at`**: atualizado pela aplicação através de `ocorrencia.touch(now)`, usando o `Clock` injetado nos services, em vez de `@PreUpdate`. Motivo: o listener JPA não tem acesso ao `Clock` gerenciado pelo Spring sem um `EntityListener` com injeção.
- **Motivos de prioridade**: armazenados como códigos (`DRENAGEM_COM_RISCO`, etc.) e convertidos para texto em português apenas na resposta/CSV, mantendo a coluna fiel ao contrato do banco.
- **Cálculo de prioridade**: drenagem `SIM` +3 e `NAO_SEI` +1; tipo `DRENAGEM_OBSTRUIDA` +2; volume `GRANDE` +2 e `MEDIO` +1; acesso `DIFICIL` +1 e `IMPOSSIVEL_CAMINHAO` +2; área vulnerável +2. Limiares: `>= 5` Alta, `3–4` Média, `< 3` Baixa.
- **Idempotência**: `Idempotency-Key` (UUID) gravada em `ocorrencia.client_request_id`. Reenvio com a mesma chave devolve `200` com o registro original; criação nova devolve `201`.
- **Fotos**: reorientadas, reduzidas a no máximo 1600 px no maior lado, reencodadas em JPEG (qualidade 0,82) e sem EXIF/GPS. Caminho `{ocorrenciaId}/{uuid}.jpg`. Falha de foto não impede a criação da ocorrência; a resposta informa `received/saved/failed`.
- **Fuso horário**: persistência e JSON em UTC (`Instant`); textos e CSV exibem `America/Sao_Paulo`.
- **CSV**: exportação em lotes paginados de 500 (`findAll(spec, PageRequest)`) em vez de `Stream` JPA, que não existe nesta versão do Spring Data. Arquivo UTF-8 com BOM, separador `;`, quebras CRLF e prefixo `'` em células que começam com `=`, `+`, `-`, `@`, tab ou CR. Observações internas nunca são exportadas.
- **Encaminhamentos**: o texto de solicitação é gerado quando omitido, mas o registro do encaminhamento **não** altera a situação automaticamente. `referred_at` usa o relógio do servidor quando não informado.
- **Indicadores**: lidos diretamente da view `v_indicators` com `JdbcTemplate`, em uma única consulta, sem mapeamento de entidade.
- **Turnstile**: quando ativo, token ausente/inválido retorna `400`; falha do serviço retorna `503` (fail-closed). O verificador constrói o próprio `RestClient` porque o Boot 4, com este conjunto de starters, não autoconfigura `RestClient.Builder`.
- **Rate limit**: Bucket4j em memória, sem Redis, com limites por IP e global. O IP vem apenas do cabeçalho configurado (`RATE_LIMIT_IP_HEADER`) e nunca é persistido ou registrado em log.
- **Status**: `RESOLVIDA` exige nota de confirmação; reabrir uma ocorrência resolvida também exige justificativa; mudar para o mesmo status retorna `409`. O histórico é sempre gravado.
- **springdoc**: desabilitado na configuração base e habilitado apenas no perfil `dev`. A segurança libera `/v3/api-docs/**` e `/swagger-ui/**` somente quando a documentação está habilitada.
- **Docker**: `Dockerfile` multi-stage com `maven:3.9-eclipse-temurin-21-alpine` no build e `eclipse-temurin:21-jre-alpine` na execução, usuário sem privilégios e `HEALTHCHECK` em `/actuator/health`.
- **Id atribuído + `refresh`**: como o UUID é gerado na aplicação, o Spring Data trata a entidade como existente e faz `merge`. Após salvar, é usada a instância gerenciada devolvida antes do `refresh` que relê o `number`. Esse problema foi encontrado na validação manual, pois os testes de integração são pulados sem Docker.
- **Validação manual**: fluxo público e administrativo validado contra PostgreSQL 18 local (criação, idempotência, `401`/`403`, rate limit `429`, texto de encaminhamento, encaminhamento, indicadores, CSV e transições de status).

