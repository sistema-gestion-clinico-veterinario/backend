export function clinicaDeCuenta(ids, cuenta) {
  const lista = ids.clinicas;
  if (!lista || lista.length === 0) return ids;
  return lista[(cuenta - 1) % lista.length];
}
