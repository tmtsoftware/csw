CREATE TABLE language (
id NUMERIC(7) NOT NULL PRIMARY KEY,
cd CHAR(2) NOT NULL,
description VARCHAR(50)
);

INSERT into language VALUES (1,'En');
INSERT into language VALUES (2,'Hi');
INSERT into language VALUES (3,'Fr');

CREATE TABLE author (
id NUMERIC(7) NOT NULL PRIMARY KEY,
first_name VARCHAR(50),
last_name VARCHAR(50) NOT NULL,
date_of_birth DATE,
year_of_birth NUMERIC(7),
distinguished NUMERIC(1)
);

INSERT into author VALUES (1,'John','Rogue','1991-03-11',1991,5);
INSERT into author VALUES (2,'Lin','Rogue','1992-05-21',1992,4);
INSERT into author VALUES (3,'Jay','Rogue','1989-12-11',1989,3);
INSERT into author VALUES (4,'Jack','Rogue','1991-02-04',1991,2);

CREATE TABLE book (
id NUMERIC(7) NOT NULL PRIMARY KEY,
author_id NUMERIC(7) NOT NULL,
title VARCHAR(400) NOT NULL,
published_in NUMERIC(7) NOT NULL,
language_id NUMERIC(7) NOT NULL,

CONSTRAINT fk_book_author FOREIGN KEY (author_id) REFERENCES author(id),
CONSTRAINT fk_book_language FOREIGN KEY (language_id) REFERENCES language(id)
);

INSERT into book VALUES (1,1,'book1',1942,1);
INSERT into book VALUES (2,1,'book2',1948,2);
INSERT into book VALUES (3,2,'book3',1952,2);
INSERT into book VALUES (4,2,'book4',1956,3);

CREATE TABLE book_store (
name VARCHAR(400) NOT NULL UNIQUE
);

CREATE TABLE book_to_book_store (
name VARCHAR(400) NOT NULL,
book_id NUMERIC NOT NULL,
stock NUMERIC,

PRIMARY KEY(name, book_id),
CONSTRAINT fk_b2bs_book_store FOREIGN KEY (name) REFERENCES book_store (name) ON DELETE CASCADE,
CONSTRAINT fk_b2bs_book FOREIGN KEY (book_id) REFERENCES book (id) ON DELETE CASCADE
);
