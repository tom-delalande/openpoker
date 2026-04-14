CREATE TABLE  auth_tokens (
    token UUID NOT NULL PRIMARY KEY,
    payload JSONB NOT NULL
);

CREATE TABLE cash_game_players (
    id INTEGER NOT NULL PRIMARY KEY,
    payload JSONB NOT NULL
);

CREATE TABLE cash_games (
    id UUID NOT NULL PRIMARY KEY,
    payload JSONB NOT NULL
);

CREATE TABLE hand_history (
    hand_id UUID NOT NULL PRIMARY KEY,
    table_id UUID NOT NULL,
    payload JSONB NOT NULL
);

