



CREATE OR REPLACE VIEW queue AS(
SELECT id, title, SUM(value) AS "value"
FROM 
	video_votes
GROUP BY id
ORDER BY Value DESC)


CREATE OR REPLACE VIEW video_votes AS (
SELECT video.id, title, value FROM video 
	INNER JOIN video_vote
	ON video.id = video_vote.video_votes_id
	INNER JOIN vote
	ON video_vote.vote_id = vote.id) 






