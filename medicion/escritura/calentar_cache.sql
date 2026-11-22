CREATE EXTENSION IF NOT EXISTS pg_prewarm;
SELECT sum(pg_prewarm(c.oid::regclass)) AS bloques_cargados
  FROM pg_class c
  JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public' AND c.relkind IN ('r', 'i') AND pg_relation_size(c.oid) > 0;
