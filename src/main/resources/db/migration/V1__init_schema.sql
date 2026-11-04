-- Território Limpo — schema inicial (MVP)
-- Fonte da verdade do schema. Alterações futuras devem ser feitas em novas migrations.

CREATE TABLE ocorrencia (
    id                     uuid PRIMARY KEY,
    number                 bigint GENERATED ALWAYS AS IDENTITY NOT NULL UNIQUE,
    created_at             timestamptz NOT NULL,
    updated_at             timestamptz NOT NULL,
    type                   varchar(30) NOT NULL,
    estimated_volume       varchar(20) NOT NULL,
    collection_acesso      varchar(30) NOT NULL,
    drenagem_risk          varchar(10) NOT NULL,
    observacaos            varchar(1000),
    latitude               numeric(9, 6),
    longitude              numeric(9, 6),
    precisao_meters        numeric(8, 2),
    address_text           varchar(300),
    referencia_point       varchar(300),
    localizacao_source     varchar(30) NOT NULL,
    status                 varchar(30) NOT NULL,
    prioridade             varchar(10) NOT NULL,
    prioridade_reasons     text[] NOT NULL DEFAULT '{}',
    prioridade_set_manually boolean NOT NULL DEFAULT false,
    manual_prioridade_reason varchar(500),
    in_vulneravel_area     boolean,
    resolved_at            timestamptz,
    resolved_confirmed_by  varchar(255),
    resolution_observacao  varchar(2000),
    CONSTRAINT ck_ocorrencia_type CHECK (type IN ('LIXO_COMUM', 'ENTULHO', 'VOLUMOSO', 'DRENAGEM_OBSTRUIDA', 'OUTRO')),
    CONSTRAINT ck_ocorrencia_volume CHECK (estimated_volume IN ('PEQUENO', 'MEDIO', 'GRANDE')),
    CONSTRAINT ck_ocorrencia_access CHECK (collection_acesso IN ('NORMAL', 'DIFICIL', 'IMPOSSIVEL_CAMINHAO')),
    CONSTRAINT ck_ocorrencia_drainage CHECK (drenagem_risk IN ('SIM', 'NAO', 'NAO_SEI')),
    CONSTRAINT ck_ocorrencia_location_source CHECK (localizacao_source IN ('GPS', 'MANUAL', 'CONFIRMADA_PELA_EQUIPE')),
    CONSTRAINT ck_ocorrencia_status CHECK (status IN ('RECEBIDA', 'EM_ANALISE', 'ENCAMINHADA', 'AGUARDANDO_ATENDIMENTO', 'RESOLVIDA', 'NAO_SOLUCIONADA')),
    CONSTRAINT ck_ocorrencia_priority CHECK (prioridade IN ('BAIXA', 'MEDIA', 'ALTA')),
    CONSTRAINT ck_ocorrencia_coordinates_pair CHECK ((latitude IS NULL) = (longitude IS NULL)),
    CONSTRAINT ck_ocorrencia_latitude_range CHECK (latitude IS NULL OR (latitude >= -90 AND latitude <= 90)),
    CONSTRAINT ck_ocorrencia_longitude_range CHECK (longitude IS NULL OR (longitude >= -180 AND longitude <= 180)),
    CONSTRAINT ck_ocorrencia_location_present CHECK (
        latitude IS NOT NULL
        OR btrim(coalesce(address_text, '')) <> ''
        OR btrim(coalesce(referencia_point, '')) <> ''
    ),
    CONSTRAINT ck_ocorrencia_location_source_coordinates CHECK (
        (localizacao_source = 'GPS' AND latitude IS NOT NULL)
        OR (localizacao_source = 'MANUAL' AND latitude IS NULL)
        OR (localizacao_source = 'CONFIRMADA_PELA_EQUIPE' AND latitude IS NOT NULL)
    ),
    CONSTRAINT ck_ocorrencia_accuracy CHECK (precisao_meters IS NULL OR precisao_meters >= 0),
    CONSTRAINT ck_ocorrencia_resolved_fields CHECK (
        status <> 'RESOLVIDA'
        OR (
            resolved_at IS NOT NULL
            AND btrim(coalesce(resolved_confirmed_by, '')) <> ''
            AND btrim(coalesce(resolution_observacao, '')) <> ''
        )
    )
);

CREATE INDEX ix_ocorrencia_created_at ON ocorrencia (created_at DESC);
CREATE INDEX ix_ocorrencia_status ON ocorrencia (status);
CREATE INDEX ix_ocorrencia_type ON ocorrencia (type);
CREATE INDEX ix_ocorrencia_priority ON ocorrencia (prioridade);
CREATE INDEX ix_ocorrencia_coordinates ON ocorrencia (latitude, longitude) WHERE latitude IS NOT NULL;

CREATE TABLE ocorrencia_foto (
    id            uuid PRIMARY KEY,
    ocorrencia_id uuid NOT NULL REFERENCES ocorrencia (id) ON DELETE CASCADE,
    storage_path  varchar(500) NOT NULL UNIQUE,
    content_type  varchar(100) NOT NULL,
    size_bytes    bigint NOT NULL,
    created_at    timestamptz NOT NULL,
    CONSTRAINT ck_ocorrencia_foto_size CHECK (size_bytes > 0)
);

CREATE INDEX ix_ocorrencia_foto_ocorrencia ON ocorrencia_foto (ocorrencia_id);

CREATE TABLE ocorrencia_status_historico (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocorrencia_id uuid NOT NULL REFERENCES ocorrencia (id) ON DELETE CASCADE,
    from_status   varchar(30),
    to_status     varchar(30) NOT NULL,
    changed_by    varchar(255),
    observacao    varchar(2000),
    changed_at    timestamptz NOT NULL,
    CONSTRAINT ck_history_from_status CHECK (
        from_status IS NULL
        OR from_status IN ('RECEBIDA', 'EM_ANALISE', 'ENCAMINHADA', 'AGUARDANDO_ATENDIMENTO', 'RESOLVIDA', 'NAO_SOLUCIONADA')
    ),
    CONSTRAINT ck_history_to_status CHECK (
        to_status IN ('RECEBIDA', 'EM_ANALISE', 'ENCAMINHADA', 'AGUARDANDO_ATENDIMENTO', 'RESOLVIDA', 'NAO_SOLUCIONADA')
    )
);

CREATE INDEX ix_historico_ocorrencia ON ocorrencia_status_historico (ocorrencia_id, changed_at);

CREATE TABLE ocorrencia_encaminhamento (
    id              bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocorrencia_id   uuid NOT NULL REFERENCES ocorrencia (id) ON DELETE CASCADE,
    destination     varchar(200) NOT NULL,
    channel         varchar(100) NOT NULL,
    referred_at     timestamptz NOT NULL,
    protocol_number varchar(100),
    request_text    varchar(8000) NOT NULL,
    referred_by     varchar(255) NOT NULL,
    created_at      timestamptz NOT NULL
);

CREATE INDEX ix_encaminhamento_ocorrencia ON ocorrencia_encaminhamento (ocorrencia_id, referred_at);

CREATE TABLE ocorrencia_internal_observacao (
    id            bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    ocorrencia_id uuid NOT NULL REFERENCES ocorrencia (id) ON DELETE CASCADE,
    author_id     varchar(255) NOT NULL,
    body          varchar(2000) NOT NULL,
    created_at    timestamptz NOT NULL
);

CREATE INDEX ix_internal_observacao_ocorrencia ON ocorrencia_internal_observacao (ocorrencia_id, created_at);

CREATE VIEW v_indicadores AS
SELECT
    count(*)::bigint AS total_ocorrencias,
    count(*) FILTER (WHERE status = 'RECEBIDA')::bigint AS received,
    count(*) FILTER (WHERE status = 'EM_ANALISE')::bigint AS in_analysis,
    count(*) FILTER (WHERE status IN ('RECEBIDA', 'EM_ANALISE'))::bigint AS pending_analysis,
    count(*) FILTER (WHERE status IN ('ENCAMINHADA', 'AGUARDANDO_ATENDIMENTO'))::bigint AS forwarded_or_awaiting,
    count(*) FILTER (WHERE status = 'ENCAMINHADA')::bigint AS forwarded,
    count(*) FILTER (WHERE status = 'AGUARDANDO_ATENDIMENTO')::bigint AS awaiting_service,
    count(*) FILTER (WHERE status = 'RESOLVIDA')::bigint AS resolved,
    count(*) FILTER (WHERE status = 'NAO_SOLUCIONADA')::bigint AS unresolved,
    count(*) FILTER (WHERE status IN ('RECEBIDA', 'EM_ANALISE') AND prioridade = 'ALTA')::bigint AS high_prioridade_pending,
    (SELECT count(*) FROM ocorrencia_encaminhamento)::bigint AS total_encaminhamentos,
    (SELECT count(*) FROM ocorrencia_encaminhamento WHERE protocol_number IS NOT NULL AND btrim(protocol_number) <> '')::bigint AS encaminhamentos_with_protocol,
    (SELECT count(DISTINCT ocorrencia_id) FROM ocorrencia_encaminhamento)::bigint AS ocorrencias_with_encaminhamento
FROM ocorrencia;

ALTER TABLE ocorrencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocorrencia_foto ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocorrencia_status_historico ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocorrencia_encaminhamento ENABLE ROW LEVEL SECURITY;
ALTER TABLE ocorrencia_internal_observacao ENABLE ROW LEVEL SECURITY;
