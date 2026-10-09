ALTER TABLE cuentas DROP CONSTRAINT ck_cuentas_estatus;
DROP INDEX IF EXISTS idx_cuentas_estatus;
ALTER TABLE cuentas ALTER COLUMN estatus DROP DEFAULT;
ALTER TABLE cuentas ALTER COLUMN estatus TYPE BOOLEAN USING (estatus = 'ACTIVA');
ALTER TABLE cuentas RENAME COLUMN estatus TO activa;
ALTER TABLE cuentas ALTER COLUMN activa SET DEFAULT true;
