-- =================================================================================
-- =================================================================================
-- ETL SCRIPT (Εξαγωγή - Μετασχηματισμός - Φόρτωση)
-- REVISED & SCALABLE VERSION: Materialized Indexed Joins (Αποφυγή Error 2013)
-- =================================================================================
-- =================================================================================

USE dbms_project;

-- =========================================
-- Step 0: Clean Up Production Tables
-- Απενεργοποιούμε τα foreign keys για να καθαρίσουμε τους πίνακες
-- =========================================
SET FOREIGN_KEY_CHECKS=0;
TRUNCATE TABLE Publication_Author;
TRUNCATE TABLE Author;
TRUNCATE TABLE Publication;
TRUNCATE TABLE Journal;
TRUNCATE TABLE Conference;
TRUNCATE TABLE Venue;
TRUNCATE TABLE Category;
TRUNCATE TABLE Publisher;
SET FOREIGN_KEY_CHECKS=1;

-- =========================================
-- Step 1: Populate Publisher Table
-- =========================================
INSERT INTO Publisher (name)
SELECT DISTINCT publisher 
FROM Staging_Journal
WHERE publisher IS NOT NULL AND publisher != '';

-- =========================================
-- Step 2: Populate Category Table
-- Description: Extracts names from Staging_FoR and adds common CS categories.
-- =========================================
INSERT INTO Category (id, name)
SELECT 
    ROW_NUMBER() OVER(ORDER BY name) AS id,
    name
FROM (
    SELECT DISTINCT TRIM(name) as name FROM Staging_FoR WHERE name != ''
    UNION SELECT 'Artificial Intelligence'
    UNION SELECT 'Computer Science'
    UNION SELECT 'Software Engineering'
    UNION SELECT 'Information Systems'
    UNION SELECT 'Theoretical Computer Science'
) t;

-- =========================================
-- Step 3: Populate Venue Superclass Table
-- =========================================
-- Insert Conferences
INSERT INTO Venue (name, type)
SELECT DISTINCT title, 'CONFERENCE'
FROM Staging_Conference
WHERE title IS NOT NULL AND title != '';

-- Insert Journals
INSERT INTO Venue (name, type)
SELECT DISTINCT title, 'JOURNAL'
FROM Staging_Journal
WHERE title IS NOT NULL AND title != '';

-- =========================================
-- Step 4: Populate Conference & Journal Subclass Tables
-- =========================================
-- Populate Conference table with robust Category mapping
INSERT INTO Conference (venue_id, `rank`, dblp, primary_for)
SELECT 
    v.id,
    MIN(sc.rank_val),
    MIN(sc.source),
    (SELECT id FROM Category WHERE name = 
        CASE 
            WHEN sc.primary_for_code LIKE '4602%' THEN 'Artificial Intelligence'
            WHEN sc.primary_for_code LIKE '4601%' THEN 'Computer Science'
            WHEN sc.primary_for_code LIKE '4612%' THEN 'Software Engineering'
            WHEN sc.primary_for_code LIKE '4605%' THEN 'Information Systems'
            WHEN sc.primary_for_code LIKE '46%' THEN 'Computer Science' -- Generic fallback for CS
            ELSE NULL 
        END
    LIMIT 1)
FROM Staging_Conference sc
JOIN Venue v ON sc.title = v.name AND v.type = 'CONFERENCE'
GROUP BY v.id, sc.primary_for_code;

-- Populate Journal table 
INSERT INTO Journal (venue_id, sjr, cite_score, h_index, best_quartile, publisher_id)
SELECT 
    v.id,
    MAX(CAST(REPLACE(sj.sjr, ',', '.') AS DOUBLE)),
    MAX(CAST(REPLACE(sj.cites_doc_2y, ',', '.') AS DOUBLE)), 
    MAX(CASE WHEN sj.h_index REGEXP '^[0-9]+$' THEN CAST(sj.h_index AS UNSIGNED) ELSE NULL END),
    MIN(sj.sjr_quartile),
    MIN(p.id)
FROM Staging_Journal sj
JOIN Venue v ON sj.title = v.name AND v.type = 'JOURNAL'
LEFT JOIN Publisher p ON sj.publisher = p.name
GROUP BY v.id;

-- =========================================
-- Step 5: Match Venues in Staging_DBLP
-- =========================================
SET SQL_SAFE_UPDATES = 0;
UPDATE Staging_DBLP SET venue_id = NULL;

-- Step 5.1: Direct Match (Ακριβές ταίριασμα ονόματος)
UPDATE Staging_DBLP s
JOIN Venue v ON s.venue_name = v.name
SET s.venue_id = v.id
WHERE s.venue_id IS NULL;

-- Step 5.2: Conference Acronym Match 
DROP TEMPORARY TABLE IF EXISTS Temp_Conf_Match;
CREATE TEMPORARY TABLE Temp_Conf_Match (
    acronym VARCHAR(255) PRIMARY KEY,
    venue_id INT
);

INSERT IGNORE INTO Temp_Conf_Match (acronym, venue_id)
SELECT sc.acronym, MIN(v.id)
FROM Staging_Conference sc
JOIN Venue v ON sc.title = v.name AND v.type = 'CONFERENCE'
WHERE sc.acronym IS NOT NULL AND sc.acronym != ''
GROUP BY sc.acronym;

UPDATE Staging_DBLP s
JOIN Temp_Conf_Match t ON s.venue_name = t.acronym
SET s.venue_id = t.venue_id
WHERE s.venue_id IS NULL;

DROP TEMPORARY TABLE IF EXISTS Temp_Conf_Match;

-- ==============================================================================
-- Step 5.3: Journal Title Match (Απόλυτη Βελτιστοποίηση - Physical Tuning)
-- ==============================================================================

-- Υπο-Βήμα Α: Υλικοποιούμε τα DISTINCT Journals με μια έξτρα στήλη για το prefix_8
DROP TEMPORARY TABLE IF EXISTS Temp_Distinct_Journals;
CREATE TEMPORARY TABLE Temp_Distinct_Journals (
    venue_name_exact VARCHAR(500) PRIMARY KEY,
    prefix_8 VARCHAR(8),
    venue_id INT
);

INSERT IGNORE INTO Temp_Distinct_Journals (venue_name_exact, prefix_8)
SELECT DISTINCT venue_name, LEFT(venue_name, 8)
FROM Staging_DBLP
WHERE venue_id IS NULL AND venue_name IS NOT NULL AND venue_name != '';

-- Φτιάχνουμε INDEX πάνω στο prefix για να τρέξει αστραπιαία το επόμενο join!
CREATE INDEX idx_temp_prefix ON Temp_Distinct_Journals(prefix_8);

-- Υπο-Βήμα Β: Φτιάχνουμε έναν μικρό προσωρινό πίνακα για τα Venue Journals, ΕΠΙΣΗΣ με prefix_8
DROP TEMPORARY TABLE IF EXISTS Temp_Venue_Journals;
CREATE TEMPORARY TABLE Temp_Venue_Journals (
    id INT PRIMARY KEY,
    name VARCHAR(255),
    prefix_8 VARCHAR(8)
);

INSERT INTO Temp_Venue_Journals (id, name, prefix_8)
SELECT id, name, LEFT(name, 8)
FROM Venue
WHERE type = 'JOURNAL' AND LENGTH(name) >= 8;

-- INDEX και εδώ!
CREATE INDEX idx_venue_prefix ON Temp_Venue_Journals(prefix_8);

-- Υπο-Βήμα Γ: Το Βαρύ Update τώρα γίνεται ΑΠΕΥΘΕΙΑΣ πάνω στα Indexes! (Χωρίς Timeouts)
UPDATE Temp_Distinct_Journals t 
JOIN Temp_Venue_Journals v ON t.prefix_8 = v.prefix_8 
    AND t.venue_name_exact LIKE CONCAT(v.name, '%')
SET t.venue_id = v.id 
WHERE t.venue_id IS NULL;

-- Υπο-Βήμα Δ: Ενημέρωση του γιγάντιου Staging_DBLP (Άμεσο Join χωρίς LIKE/LEFT)
CREATE INDEX idx_temp_venue_name ON Staging_DBLP(venue_name(255));

UPDATE Staging_DBLP s 
JOIN Temp_Distinct_Journals t ON s.venue_name = t.venue_name_exact 
SET s.venue_id = t.venue_id 
WHERE s.venue_id IS NULL AND t.venue_id IS NOT NULL;

-- Καθάρισμα του memory state
DROP INDEX idx_temp_venue_name ON Staging_DBLP;
DROP TEMPORARY TABLE IF EXISTS Temp_Venue_Journals;
DROP TEMPORARY TABLE IF EXISTS Temp_Distinct_Journals;

-- =========================================
-- Step 5.4: Populate Publication Table 
-- =========================================
INSERT IGNORE INTO Publication (id, title, year, pages, url, venue_id)
SELECT 
    dblp_key,
    title,
    CAST(year_val AS UNSIGNED),
    pages,
    url,
    venue_id
FROM Staging_DBLP
WHERE venue_id IS NOT NULL;

-- =========================================
-- Step 6: Populate Author and Publication_Author Tables
-- =========================================
SET SESSION cte_max_recursion_depth = 10000;

-- Εισαγωγή μοναδικών συγγραφέων (Recursive CTE για split του '|')
INSERT IGNORE INTO Author (name)
WITH RECURSIVE AuthorSplit (publication_id, author_name, remaining_authors) AS (
    SELECT
        dblp_key,
        SUBSTRING_INDEX(authors, '|', 1),
        IF(LOCATE('|', authors) > 0, SUBSTRING(authors, LOCATE('|', authors) + 1), '')
    FROM Staging_DBLP
    WHERE authors IS NOT NULL AND authors != ''
    UNION ALL
    SELECT
        publication_id,
        SUBSTRING_INDEX(remaining_authors, '|', 1),
        IF(LOCATE('|', remaining_authors) > 0, SUBSTRING(remaining_authors, LOCATE('|', remaining_authors) + 1), '')
    FROM AuthorSplit
    WHERE remaining_authors != ''
)
SELECT DISTINCT TRIM(author_name) 
FROM AuthorSplit
WHERE author_name IS NOT NULL AND TRIM(author_name) != '';

-- Διασύνδεση Δημοσιεύσεων - Συγγραφέων (M:N)
INSERT IGNORE INTO Publication_Author (publication_id, author_id)
WITH RECURSIVE AuthorSplit (publication_id, author_name, remaining_authors) AS (
    SELECT
        dblp_key,
        SUBSTRING_INDEX(authors, '|', 1),
        IF(LOCATE('|', authors) > 0, SUBSTRING(authors, LOCATE('|', authors) + 1), '')
    FROM Staging_DBLP
    WHERE authors IS NOT NULL AND authors != ''
    UNION ALL
    SELECT
        publication_id,
        SUBSTRING_INDEX(remaining_authors, '|', 1),
        IF(LOCATE('|', remaining_authors) > 0, SUBSTRING(remaining_authors, LOCATE('|', remaining_authors) + 1), '')
    FROM AuthorSplit
    WHERE remaining_authors != ''
)
SELECT 
    s.publication_id,
    a.id
FROM AuthorSplit s
JOIN Author a ON TRIM(s.author_name) = a.name
WHERE s.publication_id IN (SELECT id FROM Publication);

SET SQL_SAFE_UPDATES = 1;

SELECT 'ETL Process Completed Successfully' as Status;