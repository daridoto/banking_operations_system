-- Datos fijos para las pruebas: dos cuentas (100 y 50 EUR) y los dos usuarios.
-- Separados de los de demostración para que cambiar la demo no rompa los tests.

INSERT INTO clientes (tipo, nombre, apellidos, documento) VALUES
    ('FISICA', 'Ana', 'García López', '12345678Z'),
    ('FISICA', 'Luis', 'Martín Pérez', 'X1234567L');

INSERT INTO cuentas (iban, cliente_id, saldo, moneda) VALUES
    ('ES6099990001410000123456', (SELECT id FROM clientes WHERE documento = '12345678Z'), 100.00, 'EUR'),
    ('ES2599990001450000234567', (SELECT id FROM clientes WHERE documento = 'X1234567L'), 50.00, 'EUR');

INSERT INTO usuarios (username, password_hash, nombre_completo, rol) VALUES
    ('empleado', '$2a$10$oJXwMo16jz8GGeB6mxLrpO956VZv/Yt4NbJZjYXwGr.W3jB6DivwO', 'Elena Sánchez Ruiz', 'EMPLEADO'),
    ('supervisor', '$2a$10$78PX.LwvzG8rhWuHdBRwGuVHFmwHO1His5Q.6e7w6rs2xU0ip6zuK', 'Javier Moreno Gil', 'SUPERVISOR');
