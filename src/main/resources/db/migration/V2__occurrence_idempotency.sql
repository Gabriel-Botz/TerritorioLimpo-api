-- Idempotência de envio do registro público.
ALTER TABLE ocorrencia ADD COLUMN client_request_id uuid NULL;

CREATE UNIQUE INDEX ux_ocorrencia_client_request_id
    ON ocorrencia (client_request_id)
    WHERE client_request_id IS NOT NULL;
