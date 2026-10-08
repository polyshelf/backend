# БД

Особенности:
- при добавлении нового товара необходимо сохранить его в product_candidate и в user_inventory.
В user_inventory он сохраняется с custom_name и expiration_date, но без product_id
- при добавлении товара без штрихкода, он сохраняется только в user_inventory
- в product_candidate поле barcode неуникально, чтобы разные пользователи могли предлагать разные названия для продукта
