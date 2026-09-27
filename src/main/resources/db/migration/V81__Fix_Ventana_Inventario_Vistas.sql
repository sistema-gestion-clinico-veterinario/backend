UPDATE vistas
SET ventana_id = (SELECT id FROM ventanas WHERE codigo = 'INVENTARIO')
WHERE codigo IN ('VISTA_PRODUCTOS', 'VISTA_CATEGORIAS_PRODUCTO')
  AND ventana_id IS NULL;
