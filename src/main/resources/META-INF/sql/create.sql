DROP TABLE IF EXISTS "user";
CREATE TABLE IF NOT EXISTS "user" ( user_id int NOT NULL PRIMARY KEY,pass_word varchar(60) NOT NULL,roles varchar(200) NOT NULL,introduction varchar(200) NOT NULL);