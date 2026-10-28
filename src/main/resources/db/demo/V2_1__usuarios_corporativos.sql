-- Migración de datos (2.1): los usuarios de demo pasan a tener nombre corporativo
-- (inicial + primer apellido). No cambia ninguna tabla; transforma datos existentes.
-- Las contraseñas no cambian: se copia el mismo hash BCrypt.

-- 1. Crear los usuarios nuevos copiando los datos de los antiguos.
INSERT INTO usuarios (username, password_hash, nombre_completo, rol, activo)
SELECT 'esanchez', password_hash, nombre_completo, rol, activo FROM usuarios WHERE username = 'empleado';

INSERT INTO usuarios (username, password_hash, nombre_completo, rol, activo)
SELECT 'jmoreno', password_hash, nombre_completo, rol, activo FROM usuarios WHERE username = 'supervisor';

-- 2. Pasar la auditoría ya registrada a los nombres nuevos. Hay que hacerlo antes de
--    borrar: las claves ajenas no permiten borrar un usuario que aún se referencia.
UPDATE transferencias SET creada_por = 'esanchez' WHERE creada_por = 'empleado';
UPDATE transferencias SET creada_por = 'jmoreno' WHERE creada_por = 'supervisor';
UPDATE cambios_incidencia SET usuario = 'esanchez' WHERE usuario = 'empleado';
UPDATE cambios_incidencia SET usuario = 'jmoreno' WHERE usuario = 'supervisor';

-- 3. Borrar los usuarios antiguos, que ya no tienen nada que dependa de ellos.
DELETE FROM usuarios WHERE username IN ('empleado', 'supervisor');
