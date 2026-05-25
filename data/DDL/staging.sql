USE dbms_project;

-- =============================================================
-- Staging Tables (Raw Data Loading)
-- Όλα τα πεδία είναι VARCHAR/TEXT για να αποφύγουμε Type Errors
-- κατά τη φόρτωση. Ο καθαρισμός γίνεται στο επόμενο βήμα.
-- =============================================================

-- 1. Staging για Conferences (CORE Rankings)
-- Βασισμένο στο: Icore26_killedColumnsForLoading.csv
DROP TABLE IF EXISTS Staging_Conference;
CREATE TABLE Staging_Conference (
    source_id VARCHAR(50),
    title TEXT,
    acronym VARCHAR(50),
    source VARCHAR(50),
    rank_val VARCHAR(50),
    dblp_status VARCHAR(20),
    primary_for_code VARCHAR(20)
);

-- 2. Staging για Journals (SJR Rankings)
-- Τυπική μορφή SJR αρχείου
DROP TABLE IF EXISTS Staging_Journal;
CREATE TABLE Staging_Journal (
    rank_val VARCHAR(50),
    source_id VARCHAR(50),
    title TEXT,
    type VARCHAR(50),
    issn VARCHAR(100),
    sjr VARCHAR(50),
    sjr_quartile VARCHAR(10),
    h_index VARCHAR(50),
    total_docs_3y VARCHAR(50),
    total_refs VARCHAR(50),
    total_cites_3y VARCHAR(50),
    citable_docs_3y VARCHAR(50),
    cites_doc_2y VARCHAR(50),
    ref_doc VARCHAR(50),
    country VARCHAR(100),
    publisher TEXT,
    categories TEXT
);

-- 3. Staging για DBLP (Publications)
DROP TABLE IF EXISTS Staging_DBLP;
CREATE TABLE Staging_DBLP (
    dblp_key VARCHAR(255),
    title TEXT,
    authors TEXT,      -- Θα γίνει split με το '|' αργότερα
    venue_name TEXT,
    year_val VARCHAR(10),
    pages VARCHAR(100),
    url TEXT,
    venue_id INT DEFAULT NULL, -- Added for ETL processing
    FULLTEXT INDEX idx_venue_name_fulltext (venue_name)
) ENGINE=InnoDB;

-- 4. Staging για FoR Codes (Subject Areas)
-- Συνήθως συνοδεύει τα CORE rankings
DROP TABLE IF EXISTS Staging_FoR;
CREATE TABLE Staging_FoR (
    name TEXT
);