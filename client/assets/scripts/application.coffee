
## Settings

SERVER = "http://hubben.chalmers.it:8080/playIT/media"
YOUTUBE = "http://gdata.youtube.com/feeds/api/videos?alt=json&q=%QUERY"
SPOTIFY = "http://ws.spotify.com/search/1/track.json?q=%QUERY"


TEMPLATES =
	spotify: Handlebars.compile $('#spotify-partial').html()
	youtube: Handlebars.compile $('#youtube-partial').html()
	typeahead: Handlebars.compile $('#typeahead').html()
	playing: Handlebars.compile $('#playing').html()
## Handlebars view-helpers

Handlebars.registerHelper 'join', (array) ->
	new Handlebars.SafeString array.join(", ")
Handlebars.registerHelper 'ntobr', (string) -> 
	new Handlebars.SafeString string.replace /\n/g, '<br>'
Handlebars.registerHelper 'desc', (string) -> 
	index = string.indexOf '\n', 140
	string = string.substr 0, index if index != -1
	new Handlebars.SafeString string #.replace /\n/g, '<br>'
Handlebars.registerHelper 'url', (type, id) ->
	urls =
		'spotify': "http://open.spotify.com/track/#{id}"
		'youtube': "http://youtu.be/#{id}"
	new Handlebars.SafeString urls[type]
Handlebars.registerHelper 'format_time', (seconds) ->
	hours = parseInt(seconds / 3600)
	seconds -= hours * 3600
	minutes = parseInt(seconds / 60)
	seconds -= minutes * 60
	new Handlebars.SafeString "#{if hours > 0 then hours + ':' else ''}#{(if minutes > 9 then '' else '0') + minutes}:#{(if seconds > 9 then '' else '0') + seconds}"


## Typeahead!

tracks = new Bloodhound
	datumTokenizer: (d) -> Bloodhound.tokenizers.whitespace d.value,
	queryTokenizer: Bloodhound.tokenizers.whitespace,
	remote: 
		url: SPOTIFY, 
		filter: (json) ->
			result = []
			for track in json.tracks
				result.push
					value: track.name,
					link: track.href,
					artists: track.artists.map( (a) -> a.name).join ', '
					album: track.album.name
			result

videos = new Bloodhound
	datumTokenizer: (d) -> Bloodhound.tokenizers.whitespace d.value,
	queryTokenizer: Bloodhound.tokenizers.whitespace,
	remote: 
		url: YOUTUBE, 
		filter: (json) ->
			result = []
			console.log json.feed.entry[0]
			for video in json.feed.entry
				result.push
					value: video.title.$t,
					link: 'youtu.be/' + video.link[2].href.split('=')[1],
					artists: video.author.map( (a) -> a.name.$t).join ', '
			result

videos.initialize()
tracks.initialize()

$ '#insert_video'
	.typeahead null,
		name: 'youtube',
		valueKey: 'value',
		source: tracks.ttAdapter(),
		templates: 
			suggestion: TEMPLATES.typeahead
			# suggestion: Handlebars.templates.typeahead
		limit: 15
	.on 'keydown', (e) ->
		if e.which == 17
			value = $(this).val()
			console.log value
			# app.parseInput(value);
	.on 'typeahead:selected', (obj, data) ->
		console.log obj, data
		app.parseInput data.link



## Basic detction if user has cookie, not fail-safe

cookie_data = document.cookie.split /; |=/

found = false
for key in cookie_data
	if key == 'chalmersItAuth'
		found = true
		$.ajax
			url: 'https://chalmers.it/auth/userInfo.php',
			xhrFields: { withCredentials: true },
			dataType: 'jsonp'
		.done (data) ->
			$('.admin').addClass('animated fadeInUp').removeClass('admin') if data.groups.indexOf 'playITAdmin' != -1


## MediaItem: base class for YouTube- and SpotifyItem

class MediaItem
	constructor: (@id, @type) ->

	@matches: (id, regex) ->
		result = id.match regex
		result[result.length - 1] if result?

class YouTubeItem extends MediaItem
	@REGEX: /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]+).*/	

	constructor: (url) ->
		@id = YouTubeItem.matches url.trim()
		if @id?
			super @id, 'youtube'
		else
			throw new Error "Cannot create YouTubeItem, invalid url: \"#{url}\""

	@matches: (id) ->
		super id, @REGEX

class SpotifyItem extends MediaItem
	@REGEX: /^spotify:track:([a-zA-Z0-9]+)$/

	constructor: (uri) ->
		@id = SpotifyItem.matches uri.trim()
		if @id?
			super @id, 'spotify'
		else
			throw new Error "Cannot create SpotifyItem, invalid uri: \"#{uri}\""

	@matches: (id) ->
		super id, @REGEX


## Main class for the application

class App
	$feed = $ '#videofeed'
	$status = $ '#nowPlaying .media'

	parseInput: (string) ->
		if YouTubeItem.matches string
			@addMediaItem new YouTubeItem string
		else if SpotifyItem.matches string
			@addMediaItem new SpotifyItem string
		else
			console.log string

	addMediaItem: (item) ->
		method = 'addMediaItem'
		query method, {id: item.id, type: item.type}, (body) ->
			index = body.indexOf "Fail:"
			if index == 0
				toaster.err body
			else
				toaster.toast body
		@showQueue()

	addVote: (item, up) ->
		method = 'addVote'
		query method, {id: item.id, up: up}, (body) ->
			console.log 'Voted!'

	query = (method, params, callback) ->
		url = "#{SERVER}/#{method}"
		if typeof params == "function"
			callback = params
			params = {}
		$.ajax
			url: url,
			xhrFields: { withCredentials: true },
			data: params
		.done callback

	nowPlaying: ->
		method = 'nowPlaying'
		query method, (data) ->
			$status.html TEMPLATES.playing(data)

	changeLimit: (limit) ->
		method = 'changeLimit'
		query method, {limit: limit}, (body) ->
			console.log body

	showQueue: ->
		method = 'showQueue'
		query method, (body) ->
			data = JSON.parse body
			$feed.html('')
			for item in data
				# element = Handlebars.templates["#{item.type}-partial"](item)
				element = TEMPLATES["#{item.type}"](item)
				$el = $ element
				$el.data 'item', item
				$feed.append($el)
			app.nowPlaying()
			# setTimeout app.showQueue, 10000

	removeItemFromQueue: (item) ->
		method = 'removeItemFromQueue'
		query method, {id: item.id}, (body) ->
			console.log body


## Class for displaying messages to the user

class Toaster
	toaster = $ '#toaster'
	queue = []

	constructor: ->
		toaster.on 'click', close

	close = ->
		queue.shift()
		if queue.length != 0
			toaster.animate height: 10, printNext
			return
		toaster.animate height: 0, ->
			toaster.hide()

	queuePrint = (string, err) ->
		queue.push {string: string, err: err}
		printNext() if queue.length == 1

	printNext = ->
		{string, err} = queue[0]
		if err
			string = "<span style=\"color: red\">#{string}</span>"
		toaster
			.show()
			.html "<br>#{string}<br><small>(Click to dismiss)</small>"
			.animate height: 100, ->
				setTimeout close, 2000

	err: (string) ->
		console.error string
		queuePrint string, true

	toast: (string) ->
		queuePrint string, false

window.app = new App()
window.toaster = new Toaster()
app.showQueue() #if found

# app.addMediaItem new YouTubeItem 'https://www.youtube.com/watch?v=moSFlvxnbgk'
# app.addMediaItem new SpotifyItem 'spotify:track:10Ip8PpzXoYQe4e3mSgoOy'


