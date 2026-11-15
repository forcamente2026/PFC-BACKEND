ALTER TABLE usuarios
    ADD COLUMN troca_email_dia DATE,
    ADD COLUMN troca_email_pedidos INTEGER NOT NULL DEFAULT 0;
