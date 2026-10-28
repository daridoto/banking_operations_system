-- V2: auditoría. Qué empleado registró cada transferencia y quién hizo cada cambio
-- de estado de una incidencia.
-- Las columnas admiten NULL a propósito: en una base que ya tenga datos, las filas
-- antiguas no tienen autor conocido y un NOT NULL haría fallar la migración.
-- Las filas nuevas siempre lo llevan: lo rellena el backend con el usuario del token.

ALTER TABLE transferencias ADD COLUMN creada_por VARCHAR(50) REFERENCES usuarios(username);

ALTER TABLE cambios_incidencia ADD COLUMN usuario VARCHAR(50) REFERENCES usuarios(username);
