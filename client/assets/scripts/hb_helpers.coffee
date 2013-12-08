Handlebars.registerHelper 'ntobr', (string) -> 
	new Handlebars.safeString string.replace /\n/g, '<br>'

Handlebars.registerHelper 'limit', (weight) -> '∞' if weight > 99; weight

Handlebars.registerHelper 'voted', (video_id) ->
	upvote = app.getVote video_id
	return if !upvote?
	return 'upvote' if upvote
	'downvote'