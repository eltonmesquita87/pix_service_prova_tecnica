-- Add optional holder data to wallets (existing rows keep these columns NULL, no backfill)
ALTER TABLE wallets
    ADD COLUMN cpf      VARCHAR(11),
    ADD COLUMN nome     VARCHAR(150),
    ADD COLUMN email    VARCHAR(254),
    ADD COLUMN telefone VARCHAR(16);

-- Last barrier against a CPF outside the 11 digits format
ALTER TABLE wallets
    ADD CONSTRAINT ck_wallets_cpf_format CHECK (cpf IS NULL OR cpf ~ '^[0-9]{11}$');

-- One CPF per wallet; wallets without CPF do not count as duplicates
CREATE UNIQUE INDEX uk_wallets_cpf ON wallets (cpf) WHERE cpf IS NOT NULL;
