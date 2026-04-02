-- Fix ratings.score column type: SMALLINT → INTEGER (matches Rating entity Integer field)
-- Must drop and recreate the view that depends on this column
DROP VIEW IF EXISTS trip_rating_summary;

ALTER TABLE ratings ALTER COLUMN score TYPE INTEGER;

CREATE VIEW trip_rating_summary AS
SELECT
    trip_id,
    ROUND(AVG(score)::numeric, 2) AS avg_score,
    COUNT(*)                       AS total_ratings,
    COUNT(CASE WHEN score = 5 THEN 1 END) AS five_stars,
    COUNT(CASE WHEN score = 4 THEN 1 END) AS four_stars,
    COUNT(CASE WHEN score = 3 THEN 1 END) AS three_stars,
    COUNT(CASE WHEN score = 2 THEN 1 END) AS two_stars,
    COUNT(CASE WHEN score = 1 THEN 1 END) AS one_star
FROM ratings
GROUP BY trip_id;
