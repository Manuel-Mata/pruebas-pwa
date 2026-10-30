-- Guarda el JSON exacto (sin encriptar) que se envió en sendTx.do, para poder
-- reenviar un payload idéntico al reconciliar con confirmTx.do (mismo upc y datos).
ALTER TABLE transacciones ADD COLUMN payload_original TEXT NOT NULL DEFAULT '';
