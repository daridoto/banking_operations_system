-- Insertar solo si no existe: reiniciar nunca restablece los saldos existentes.
INSERT INTO cuentas (id, titular, saldo, moneda)
SELECT 1, 'Ana', 100.00, 'EUR'
WHERE NOT EXISTS (SELECT 1 FROM cuentas WHERE id = 1);

INSERT INTO cuentas (id, titular, saldo, moneda)
SELECT 2, 'Luis', 50.00, 'EUR'
WHERE NOT EXISTS (SELECT 1 FROM cuentas WHERE id = 2);

-- Usuarios de login iniciales. Se guarda el hash BCrypt, nunca la contraseña.
INSERT INTO usuarios (username, password_hash, rol, activo)
SELECT 'empleado', '$2a$10$oJXwMo16jz8GGeB6mxLrpO956VZv/Yt4NbJZjYXwGr.W3jB6DivwO', 'EMPLEADO', TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'empleado');

INSERT INTO usuarios (username, password_hash, rol, activo)
SELECT 'supervisor', '$2a$10$78PX.LwvzG8rhWuHdBRwGuVHFmwHO1His5Q.6e7w6rs2xU0ip6zuK', 'SUPERVISOR', TRUE
WHERE NOT EXISTS (SELECT 1 FROM usuarios WHERE username = 'supervisor');
