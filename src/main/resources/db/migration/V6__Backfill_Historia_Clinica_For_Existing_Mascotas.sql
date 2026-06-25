INSERT INTO historia_clinica (numero_hc, mascota_id, activa, created_at, updated_at)
SELECT 'HC-' || LPAD(m.id::text, 6, '0'), m.id, true, NOW(), NOW()
FROM mascota m
LEFT JOIN historia_clinica hc ON hc.mascota_id = m.id
WHERE hc.id IS NULL;
