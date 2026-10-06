ALTER TABLE categoria_producto
    ADD COLUMN descripcion VARCHAR(300),
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN updated_by VARCHAR(255);

ALTER TABLE unidad_medida
    ADD COLUMN descripcion VARCHAR(300),
    ADD COLUMN created_by VARCHAR(255),
    ADD COLUMN updated_by VARCHAR(255);
