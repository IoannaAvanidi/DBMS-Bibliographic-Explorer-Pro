-- =============================================================
-- Load Data Scripts
--
-- ΠΡΟΣΟΧΗ: Αν τρέχετε αυτό το script από MySQL Workbench και πάρετε Error 2068:
-- Πρέπει να κάνετε Edit Connection -> Advanced -> Others: OPT_LOCAL_INFILE=1
-- 
-- =============================================================
SET GLOBAL net_read_timeout = 6000;
SET GLOBAL connect_timeout = 6000;

SET GLOBAL local_infile = 1; -- Required for local file loading

USE dbms_project; -- Corrected database name

-- 1. Load Conferences (CORE) from iCore26_KilledColumnsForLoading.csv
-- Note: The original script expected a .tsv file, but the available file is a .csv.
-- Changed delimiter to ','
-- LOAD DATA LOCAL INFILE 'C:\\Users\\Administrator\\Documents\\GitHub\\MYE030-DataBases3\\input\\iCore26_KilledColumnsForLoading.csv' 
LOAD DATA LOCAL INFILE 'C:\\Users\\ioann\\Documents\\DB_III\\project\\MYE030-DataBases3\\input\\icore26_data\\iCore26_KilledColumnsForLoading.csv' 
INTO TABLE Staging_Conference 
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' 
OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(source_id, title, acronym, source, rank_val, dblp_status, @raw_primary)
SET primary_for_code = TRIM(@raw_primary);

-- 2. Load Journals (SJR) from journal_ranking_data_raw.csv
-- =========================================================
LOAD DATA LOCAL INFILE 'C:\\Users\\ioann\\Documents\\DB_III\\project\\MYE030-DataBases3\\input\\journal_ranking_data_raw\\journal_ranking_data_raw.csv' 
INTO TABLE Staging_Journal 
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' 
OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
IGNORE 1 LINES
-- Χαρτογραφούμε ΑΚΡΙΒΩΣ τις πρώτες 19 στήλες (τις υπόλοιπες 37 τις αγνοεί η MySQL)
(@rank, @title, @dummy, @country, @sjr, @dummy, @h_index, @quartile, @categories, @dummy, @dummy, @dummy, @total_docs_3y, @total_refs, @total_cites_3y, @citable_docs_3y, @cites_doc_2y, @ref_doc, @publisher)
SET 
    rank_val = @rank,
    title = TRIM(@title),
    country = @country,
    sjr = @sjr,
    h_index = @h_index,
    sjr_quartile = @quartile,
    categories = @categories,
    total_docs_3y = @total_docs_3y,
    total_refs = @total_refs,
    total_cites_3y = @total_cites_3y,
    citable_docs_3y = @citable_docs_3y,
    cites_doc_2y = @cites_doc_2y,
    ref_doc = @ref_doc,
    publisher = TRIM(@publisher);

-- =========================================================
-- 3A. Φόρτωση Άρθρων Περιοδικών (dblp_input_article.csv)
-- =========================================================
LOAD DATA LOCAL INFILE 'C:\\Users\\ioann\\Documents\\DB_III\\project\\MYE030-DataBases3\\input\\dblp_dataset\\dblp_input_article.csv' 
INTO TABLE Staging_DBLP 
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ';' 
OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
IGNORE 1 LINES
-- Χαρτογράφηση των 24 στηλών του CSV
(dblp_key, authors, venue_name, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, pages, @dummy, @dummy, @dummy, title, @dummy, @dummy, url, @raw_year)
SET year_val = TRIM(@raw_year);

-- =========================================================
-- 3B. Φόρτωση Πρακτικών Συνεδρίων (dblp_input_inproceedings.csv)
-- =========================================================
LOAD DATA LOCAL INFILE 'C:\\Users\\ioann\\Documents\\DB_III\\project\\MYE030-DataBases3\\input\\dblp_dataset\\dblp_input_inproceedings.csv' 
INTO TABLE Staging_DBLP 
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ';' 
OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
IGNORE 1 LINES
-- Χαρτογράφηση των 24 στηλών του CSV
(dblp_key, authors, venue_name, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, @dummy, pages, @dummy, @dummy, @dummy, title, @dummy, @dummy, url, year_val);


-- 4. Load FoR Codes (Categories)
-- The source file 'bestSubjectArea.csv' contains the top-level SJR subject areas.
LOAD DATA LOCAL INFILE 'C:\\Users\\ioann\\Documents\\DB_III\\project\\MYE030-DataBases3\\input\\journal_ranking_data_raw\\bestSubjectArea.csv' 
INTO TABLE Staging_FoR 
CHARACTER SET utf8mb4
FIELDS TERMINATED BY ',' 
OPTIONALLY ENCLOSED BY '"' 
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(name);


-- Check loaded records
SELECT 'Conferences' as TableName, COUNT(*) as RowsLoaded FROM Staging_Conference
UNION ALL
SELECT 'Journals', COUNT(*) FROM Staging_Journal
UNION ALL
SELECT 'DBLP', COUNT(*) FROM Staging_DBLP
UNION ALL
SELECT 'Categories', COUNT(*) FROM Staging_FoR;
