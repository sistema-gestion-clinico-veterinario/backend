-- Evidencia de las revisiones periódicas de accesos privilegiados (RNF-36). Cada revisión y cada decisión
-- guardan instantáneas de los datos descriptivos, por eso no referencian con llaves foráneas a usuario ni a rol:
-- renombrar o eliminar una persona o un rol no debe alterar lo que se decidió en el pasado.
CREATE TABLE IF NOT EXISTS access_review (
    id BIGSERIAL PRIMARY KEY,
    company_id INTEGER NOT NULL,
    performed_at TIMESTAMP NOT NULL,
    performed_by_user_id INTEGER,
    performed_by_email VARCHAR(254),
    reviewed_count INTEGER NOT NULL,
    kept_count INTEGER NOT NULL,
    revoked_count INTEGER NOT NULL,
    notes VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_access_review_company
    ON access_review (company_id, performed_at DESC);

CREATE TABLE IF NOT EXISTS access_review_item (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES access_review (id) ON DELETE CASCADE,
    usuario_id INTEGER NOT NULL,
    user_name VARCHAR(200) NOT NULL,
    user_email VARCHAR(254),
    role_id INTEGER,
    role_name VARCHAR(100) NOT NULL,
    reasons VARCHAR(600) NOT NULL,
    decision VARCHAR(10) NOT NULL,
    comment VARCHAR(300),
    CONSTRAINT ck_access_review_item_decision CHECK (decision IN ('KEEP', 'REVOKE'))
);

CREATE INDEX IF NOT EXISTS idx_access_review_item_review
    ON access_review_item (review_id);
