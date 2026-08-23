CREATE DATABASE weapons;

DROP DATABASE weapons;

CREATE TABLE weapon (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	sku CHAR(20),
	name char(100) NOT NULL,
	description char(255),
	price NUMERIC(16,2),
	creation_date DATE
);

ALTER TABLE weapon ADD COLUMN creation_date date; 

SELECT * FROM weapon WHERE ID = 1


------

CREATE TABLE weapon_model (
	id BIGSERIAL NOT NULL PRIMARY KEY,
	creation_date DATE,
	birth DATE NOT NULL,
	name VARCHAR(100) NOT NULL,
	address VARCHAR(255) NOT NULL,
	cpf VARCHAR(14) NOT NULL,
	phone VARCHAR(14),
	email VARCHAR(100)
)