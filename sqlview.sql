


CREATE OR REPLACE VIEW media_votes AS (
SELECT media_item.id, title, value FROM media_item 
	INNER JOIN media_item_vote
	ON media_item.id = media_item_vote.media_item_votes_id
	INNER JOIN vote
	ON media_item_vote.vote_id = vote.id);



CREATE OR REPLACE VIEW queue AS(
SELECT id, title, SUM(value) AS "value"
FROM 
	media_votes
GROUP BY id ASC
ORDER BY Value, id DESC
);







