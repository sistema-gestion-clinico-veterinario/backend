UPDATE vistas
SET nombre = 'Configuración'
WHERE codigo = 'VISTA_CATEGORIAS_PRODUCTO';

UPDATE vistas
SET activo = false
WHERE codigo = 'VISTA_UNIDADES_MEDIDA';
