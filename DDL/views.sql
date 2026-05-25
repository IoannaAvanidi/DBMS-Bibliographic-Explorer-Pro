USE dbms_project;

SELECT * FROM Publication_Author LIMIT 100;
SELECT * FROM Publication LIMIT 100;
SELECT * FROM Author LIMIT 100;
SELECT * FROM Journal LIMIT 100;
SELECT * FROM Publisher LIMIT 100;
SELECT * FROM Category LIMIT 100;
SELECT * FROM Venue LIMIT 100;



-- -- =================================================================
-- -- 1. VIEW: Author Profile
-- -- =================================================================
-- CREATE OR REPLACE VIEW AuthorProfileView AS
-- WITH AuthorVenueStats AS (
--     SELECT
--         pa.author_id,
--         p.venue_id,
--         COUNT(p.id) AS publications_in_venue,
--         ROW_NUMBER() OVER(PARTITION BY pa.author_id ORDER BY COUNT(p.id) DESC, p.venue_id) as rn
--     FROM Publication_Author pa
--     JOIN Publication p ON pa.publication_id = p.id
--     WHERE p.venue_id IS NOT NULL
--     GROUP BY pa.author_id, p.venue_id
-- ),
-- MostFrequentVenue AS (
--     SELECT
--         avs.author_id,
--         v.name AS most_frequent_venue_name,
--         v.type AS most_frequent_venue_type,
--         avs.publications_in_venue AS most_frequent_venue_publications
--     FROM AuthorVenueStats avs
--     JOIN Venue v ON avs.venue_id = v.id
--     WHERE avs.rn = 1
-- )
-- SELECT
--     a.name AS author_name,
--     COUNT(p.id) AS total_publications,
--     MIN(p.year) AS first_publication_year,
--     MAX(p.year) AS last_publication_year,
--     mfv.most_frequent_venue_name,
--     mfv.most_frequent_venue_type,
--     mfv.most_frequent_venue_publications
-- FROM Author a
-- JOIN Publication_Author pa ON a.id = pa.author_id
-- JOIN Publication p ON pa.publication_id = p.id
-- LEFT JOIN MostFrequentVenue mfv ON a.id = mfv.author_id
-- GROUP BY 
--     a.id, -- Προστέθηκε το a.id για 100% ασφάλεια στο GROUP BY
--     a.name, 
--     mfv.most_frequent_venue_name, 
--     mfv.most_frequent_venue_type, 
--     mfv.most_frequent_venue_publications;


-- -- =================================================================
-- -- 2. VIEW: Conference Profile
-- -- =================================================================
-- CREATE OR REPLACE VIEW ConferenceProfileView AS
-- WITH ConferenceAuthorStats AS (
--     SELECT
--         p.venue_id,
--         pa.author_id,
--         COUNT(p.id) AS publications_in_conference,
--         ROW_NUMBER() OVER(PARTITION BY p.venue_id ORDER BY COUNT(p.id) DESC, pa.author_id) as rn
--     FROM Publication p
--     JOIN Publication_Author pa ON p.id = pa.publication_id
--     WHERE p.venue_id IS NOT NULL
--     GROUP BY p.venue_id, pa.author_id
-- ),
-- MostProlificAuthor AS (
--     SELECT
--         cas.venue_id,
--         a.name AS most_prolific_author_name,
--         cas.publications_in_conference AS most_prolific_author_publications
--     FROM ConferenceAuthorStats cas
--     JOIN Author a ON cas.author_id = a.id
--     WHERE cas.rn = 1
-- )
-- SELECT
--     v.name AS conference_name,
--     c.`rank` AS conference_rank, -- FIX: Το rank μπήκε σε backticks γιατί είναι reserved keyword
--     cat.name AS primary_for_name,
--     COUNT(DISTINCT p.id) AS total_publications,
--     MIN(p.year) AS first_publication_year,
--     MAX(p.year) AS last_publication_year,
--     mpa.most_prolific_author_name,
--     mpa.most_prolific_author_publications
-- FROM Venue v
-- JOIN Conference c ON v.id = c.venue_id
-- LEFT JOIN Category cat ON c.primary_for = cat.id
-- JOIN Publication p ON v.id = p.venue_id
-- LEFT JOIN MostProlificAuthor mpa ON v.id = mpa.venue_id
-- WHERE v.type = 'CONFERENCE'
-- GROUP BY
--     v.id, -- Προστέθηκε το v.id για 100% ασφάλεια
--     v.name,
--     c.`rank`,
--     cat.name,
--     mpa.most_prolific_author_name,
--     mpa.most_prolific_author_publications;

-- -- =================================================================
-- -- ΕΚΤΕΛΕΣΗ (ΓΙΑ ΝΑ ΔΕΙΣ ΤΑ ΑΠΟΤΕΛΕΣΜΑΤΑ ΣΤΟ WORKBENCH)
-- -- =================================================================

-- -- Δες τα πρώτα 100 προφίλ συγγραφέων
-- SELECT * FROM AuthorProfileView LIMIT 100;

-- -- Δες τα πρώτα 100 προφίλ συνεδρίων
-- SELECT * FROM ConferenceProfileView LIMIT 100;