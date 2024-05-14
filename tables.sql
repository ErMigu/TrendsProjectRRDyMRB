CREATE TABLE Users (
    email TEXT PRIMARY KEY,
    truth DOUBLE PRECISION,
    nEmailsSent INTEGER,
    nPhishingEmails INTEGER
);

CREATE TABLE CompanyDomains(
    domain TEXT PRIMARY KEY,
    company TEXT
);

INSERT INTO CompanyDomains (company, domain) VALUES
                                                 ('Apple', '@apple.com'),
                                                 ('Microsoft', '@microsoft.com'),
                                                 ('Aramco', '@aramco.com'),
                                                 ('Alphabet', '@google.com'),
                                                 ('Nvidia', '@nvidia.com'),
                                                 ('Amazon', '@amazon.com'),
                                                 ('Meta', '@meta.com');

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE EXTENSION IF NOT EXISTS fuzzystrmatch;

