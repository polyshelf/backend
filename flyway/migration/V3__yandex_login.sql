ALTER TABLE app_user ALTER COLUMN password_hash DROP NOT NULL;
ALTER TABLE app_user ADD COLUMN IF NOT EXISTS yandex_id VARCHAR(64) NULL;
CREATE UNIQUE INDEX IF NOT EXISTS app_user_yandex_id_key ON app_user (yandex_id) WHERE yandex_id IS NOT NULL;
ALTER TABLE app_user DROP CONSTRAINT IF EXISTS app_user_has_credential;
ALTER TABLE app_user ADD CONSTRAINT app_user_has_credential CHECK (password_hash IS NOT NULL OR yandex_id IS NOT NULL);
