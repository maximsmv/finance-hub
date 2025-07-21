INSERT INTO wallet_types (
    uid,
    created_at,
    modified_at,
    name,
    currency_code,
    status,
    archived_at,
    user_type,
    creator,
    modifier
) VALUES
      (
          'e32bd41e-bb27-4942-adce-f2b406aa5f3e',
          now(),
          NULL,
          'BASIC',
          'RUB',
          'ACTIVE',
          NULL,
          'USER',
          'init_script',
          NULL
      ),
      (
          '0866aad3-3b2d-4959-aaa7-5d4a33bb9e4a',
          now(),
          NULL,
          'BASIC',
          'USD',
          'ACTIVE',
          NULL,
          'USER',
          'init_script',
          NULL
      )

ON CONFLICT DO NOTHING;