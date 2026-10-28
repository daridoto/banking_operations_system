-- Datos de demostración (versión 1.1). Viven fuera de db/migration para que un
-- despliegue real pueda arrancar sin ellos (spring.flyway.locations).
-- Datos ficticios: DNI, NIE, CIF e IBAN válidos pero inventados. La entidad 9999
-- representa a Countercheck; un IBAN con otra entidad será de otro banco.

INSERT INTO clientes (tipo, nombre, apellidos, documento) VALUES
    ('FISICA', 'Ana', 'García López', '12345678Z'),
    ('FISICA', 'Luis', 'Martín Pérez', 'X1234567L'),
    ('FISICA', 'Marta', 'Fernández Díaz', '87654321X');

-- La empresa que enviará remesas de nóminas.
INSERT INTO clientes (tipo, razon_social, documento) VALUES
    ('JURIDICA', 'Talleres Ruiz S.L.', 'B12345674');

-- El cliente se busca por documento: no depende del ID que le tocó.
INSERT INTO cuentas (iban, cliente_id, saldo, moneda) VALUES
    ('ES6099990001410000123456', (SELECT id FROM clientes WHERE documento = '12345678Z'), 100.00, 'EUR'),
    ('ES2599990001450000234567', (SELECT id FROM clientes WHERE documento = 'X1234567L'), 50.00, 'EUR'),
    ('ES0299990002990000345678', (SELECT id FROM clientes WHERE documento = 'B12345674'), 25000.00, 'EUR'),
    ('ES5599990001420000456789', (SELECT id FROM clientes WHERE documento = '87654321X'), 1800.00, 'EUR'),
    -- Segunda cuenta de Ana: un cliente puede tener varias.
    ('ES8099990001400000567890', (SELECT id FROM clientes WHERE documento = '12345678Z'), 2500.00, 'EUR');

-- Empleados de Countercheck. Se guarda el hash BCrypt, nunca la contraseña.
INSERT INTO usuarios (username, password_hash, nombre_completo, rol) VALUES
    ('empleado', '$2a$10$oJXwMo16jz8GGeB6mxLrpO956VZv/Yt4NbJZjYXwGr.W3jB6DivwO', 'Elena Sánchez Ruiz', 'EMPLEADO'),
    ('supervisor', '$2a$10$78PX.LwvzG8rhWuHdBRwGuVHFmwHO1His5Q.6e7w6rs2xU0ip6zuK', 'Javier Moreno Gil', 'SUPERVISOR');
