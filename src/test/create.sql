BEGIN;

CREATE TABLE People (
    pid        INTEGER PRIMARY KEY AUTOINCREMENT,
    firstname  STRING,
    surname    STRING,
    dob        DATE
);

INSERT INTO People VALUES (null, "Mohandas", "Gandhi", "1869-10-02");
INSERT INTO People VALUES (null, "Winston", "Churchill", "1874-11-30");
INSERT INTO People VALUES (null, "Bertrand", "Russell", "1872-05-18");
INSERT INTO People VALUES (null, "Karl", "Marx", "1818-05-05");
INSERT INTO People VALUES (null, "Kurt", "Godel", "1906-04-28");
INSERT INTO People VALUES (null, "Ludwig", "Wittgenstein", "1889-05-26");

COMMIT;
