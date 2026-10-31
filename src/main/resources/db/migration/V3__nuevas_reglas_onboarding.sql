ALTER TABLE clientes ADD CONSTRAINT ck_clientes_nacionalidad CHECK (nacionalidad = 'MEXICANA');
ALTER TABLE domicilios ALTER COLUMN pais SET DEFAULT 'MEXICO';
ALTER TABLE domicilios ADD CONSTRAINT ck_domicilios_pais CHECK (pais = 'MEXICO');
