-- Insertar solo si no existe: reiniciar nunca restablece los saldos existentes.
INSERT INTO cuentas (id, titular, saldo, moneda)
SELECT 1, 'Ana', 100.00, 'EUR'
WHERE NOT EXISTS (SELECT 1 FROM cuentas WHERE id = 1);

INSERT INTO cuentas (id, titular, saldo, moneda)
SELECT 2, 'Luis', 50.00, 'EUR'
WHERE NOT EXISTS (SELECT 1 FROM cuentas WHERE id = 2);
