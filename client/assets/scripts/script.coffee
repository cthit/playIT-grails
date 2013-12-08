server_url 	= "http://hubben.chalmers.it:8080/youTubeInTheHubbServer/video/"


Handlebars.registerHelper 'ntobr', (string) -> 
	new Handlebars.safeString string.replace /\n/g, '<br>'

Handlebars.registerHelper 'limit', (weight) -> return '∞' if weight > 99; weight

Handlebars.registerHelper 'voted', (video_id) ->
	upvote = app.getVote video_id
	return if !upvote?
	return 'upvote' if upvote
	'downvote'

jQuery.ajaxPrefilter (options) ->
	options.xhrFields = { withCredentials: true }
	return

class MediaItem
	constructor: (@id, @isUpvoted) ->
	getURL: -> @id

class YouTubeItem extends MediaItem
	getURL: ->
		'youtu.be/' + super

class SpotifyItem extends MediaItem
	getURL: -> 'spotify:' + super # spotify:track:1sSHo60XPPqA9KaKlkpx2o

class App
	constructor: ->
		# @timeout = setTimeout @reloadData, 100
		@output = $('#videofeed')
		try
			@votedata = JSON.parse localStorage?.getItem('vote_data')
		catch e
			@votedata = {}
		@authenticate()

	authenticate: ->
		$.getJSON 'https://chalmers.it/auth/userInfo.php', (data) => @user = data; console.log @
		# $.getJSON('userInfo.json', (data) => @user = data; console.log @)
		.fail -> window.open 'http://chalmers.it/auth' if confirm 'You are not signed in, please grab a cookie from the cookie jar!'
		return

	addVideo: (opts) -> @queryBackend 'addVideo', opts, (data) -> Toast.toast 'Result: ' + data; saveVote opts; @reloadData()

	queryBackend: (method, opts, callback) ->
		opts.url = opts.item.getURL()
		opts.cid = @user.cid
		# full_url = server_url + method + '?' + $.param opts
		full_url = 'exampledata.json'
		$.get(full_url, callback)
		return

	saveVote: (item) ->
		@votedata[item.id] = up
		localStorage?.setItem 'vote_data', JSON.stringify(@vote_data)

	reloadData: ->
		@queryBackend 'showQueue', {}, (data) ->
			# data = JSON.parse data # hacky hacky Horv
			data = data.sort (v1, v2) -> v2.weight - v1.weight
			@output.html Handlebars.templates['video'](data)
			console.log 'reloaded with: ', data


String.prototype.toRed = ->
	'<span style="color:red">' + this + '</span>'

class ToastBar
	constructor: () ->
		@toaster = $('#toaster')
		@toaster.click @closeToast
	closeToast: ->
		@toaster.animate { 'height': 0 }, ->
			@toaster.hide()
	toast: (msg, err) ->
		msg = msg.toRed() if err?
		@toaster.show().html('<br>' + msg + '<br><small>(Click to dismiss)</small>')
			.animate { 'height': 100 }, -> setTimeout @closeToast, 2000
	
window.Toast = new ToastBar()
window.app = new App()

$('#insert_video').typeahead
	name: 'videos',
	valueKey: 'title',
	remote: 'exampledata.json',
	template: Handlebars.templates['thumbnail'],
	limit: 10
.on 'typeahead:selected', (obj, data) ->
	console.log data
	app.addVideo data.youtubeID

$('#videofeed').on 'click', '.votes a', ->
	$this = $(this)
	videoID = $this.parent().parent().data 'youtube-id'
	upvoted = $this.hasClass 'upvote'
	app.voteVideo videoID, upvoted