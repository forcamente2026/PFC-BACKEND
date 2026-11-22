ALTER TABLE usuarios ALTER COLUMN email TYPE varchar(500);
ALTER TABLE usuarios ALTER COLUMN email_pendente TYPE varchar(500);

ALTER TABLE usuarios ADD COLUMN email_hash varchar(64);

ALTER TABLE usuarios DROP CONSTRAINT uk_usuarios_email;
ALTER TABLE usuarios ADD CONSTRAINT uk_usuarios_email_hash UNIQUE (email_hash);
