


CREATE DATABASE IF NOT EXISTS dbms_project;
USE dbms_project;

-- =========================================
-- Clean up old tables (Drop in reverse dependency order)
-- =========================================
DROP TABLE IF EXISTS Publication_Author;
DROP TABLE IF EXISTS Publication;
DROP TABLE IF EXISTS Author;
DROP TABLE IF EXISTS Journal;
DROP TABLE IF EXISTS Conference;
DROP TABLE IF EXISTS Venue;
DROP TABLE IF EXISTS Category;
DROP TABLE IF EXISTS Publisher;

-- =========================================
-- 1. Publisher (Ο Εκδότης)
-- =========================================
CREATE TABLE Publisher (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    UNIQUE(name)
);

-- =========================================
-- 2. Category (Κατηγοριοποίηση PrimaryFoR/Subject Areas)
-- =========================================
CREATE TABLE Category (
    id INT PRIMARY KEY, 
    name VARCHAR(255) NOT NULL
);

-- =========================================
-- 3. Venue (Γενικός χώρος Δημοσίευσης - Superclass)
-- =========================================
CREATE TABLE Venue (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type ENUM('CONFERENCE','JOURNAL') NOT NULL,
    UNIQUE(name, type)
);

-- =========================================
-- 4. Conference (Συνέδριο - Subclass)
-- =========================================
CREATE TABLE Conference (
    venue_id INT PRIMARY KEY,
    `rank` VARCHAR(50),
    dblp VARCHAR(50), -- Αυξημένο μέγεθος για ασφάλεια
    primary_for INT, 
    
    CONSTRAINT fk_conf_venue 
        FOREIGN KEY (venue_id) REFERENCES Venue(id) ON DELETE CASCADE,
    CONSTRAINT fk_conf_category 
        FOREIGN KEY (primary_for) REFERENCES Category(id) ON DELETE SET NULL
);

-- =========================================
-- 5. Journal (Περιοδικό - Subclass)
-- =========================================
CREATE TABLE Journal (
    venue_id INT PRIMARY KEY,
    sjr DOUBLE,
    cite_score DOUBLE,
    h_index INT,
    best_quartile VARCHAR(10),
    publisher_id INT,
    
    CONSTRAINT fk_journal_venue 
        FOREIGN KEY (venue_id) REFERENCES Venue(id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_publisher 
        FOREIGN KEY (publisher_id) REFERENCES Publisher(id) ON DELETE SET NULL
);

-- =========================================
-- 6. Publication (Άρθρο/Δημοσίευση)
-- =========================================
CREATE TABLE Publication (
    id VARCHAR(100) PRIMARY KEY,        -- Changed from INT to VARCHAR for DBLP keys (e.g. conf/vldb/Smith90)
    title TEXT NOT NULL,
    year INT NOT NULL,
    pages VARCHAR(50),
    url VARCHAR(500),
    venue_id INT,
    
    CONSTRAINT fk_publication_venue
        FOREIGN KEY (venue_id)
        REFERENCES Venue(id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);


-- =========================================
-- 7. Author
-- =========================================
CREATE TABLE Author (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    UNIQUE(name)
);

-- =========================================
-- 8. Publication_Author (M:N)
-- =========================================
CREATE TABLE Publication_Author (
    publication_id VARCHAR(100), -- Must match Publication.id type
    author_id INT,
    
    PRIMARY KEY (publication_id, author_id),
    
    CONSTRAINT fk_pa_publication
        FOREIGN KEY (publication_id)
        REFERENCES Publication(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
        
    CONSTRAINT fk_pa_author
        FOREIGN KEY (author_id)
        REFERENCES Author(id)
        ON DELETE CASCADE
        ON UPDATE CASCADE
);



-- =========================================
-- INDEXES (Προστέθηκαν για βελτιστοποίηση Phase 3)
-- =========================================
CREATE INDEX idx_publication_year ON Publication(year);
CREATE INDEX idx_author_name ON Author(name);
CREATE INDEX idx_venue_name ON Venue(name);
CREATE INDEX idx_journal_quartile ON Journal(best_quartile);

-- Επιταχύνει το "Top Authors" κατά 100x
CREATE INDEX idx_pa_author_id ON Publication_Author(author_id);
-- Επιταχύνει τα JOIN με τα Venues
CREATE INDEX idx_publication_venue_id ON Publication(venue_id);
-- Επιταχύνει το Scatter Plot
CREATE INDEX idx_journal_metrics ON Journal(sjr, cite_score);

-- =========================================
-- ADDITIONAL OPTIMIZATIONS (Phase 4 Tuning)
-- =========================================
-- Επιταχύνει το φιλτράρισμα ανά έτος και το JOIN με Venues ταυτόχρονα
CREATE INDEX idx_pub_year_venue ON Publication(year, venue_id);
-- Επιταχύνει το φιλτράρισμα ανά Venue και έτος (για EXISTS και Profile queries)
CREATE INDEX idx_pub_venue_year ON Publication(venue_id, year);
-- Επιταχύνει τις αναζητήσεις με LIKE στο τέλος (prefix search)
CREATE INDEX idx_venue_name_prefix ON Venue(name(50));
