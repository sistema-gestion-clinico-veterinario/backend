
ALTER TABLE sesion_caja ADD COLUMN IF NOT EXISTS cerrada_por_usuario_id INTEGER REFERENCES usuario(id);
