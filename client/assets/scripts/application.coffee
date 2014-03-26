
## Settings

SERVER = "http://hubben.chalmers.it:8080/playIT/media"
YOUTUBE = "http://gdata.youtube.com/feeds/api/videos?alt=json&q=%QUERY"
SPOTIFY = "http://ws.spotify.com/search/1/track.json?q=%QUERY"
TIMEOUT = null


TEMPLATES =
	spotify: Handlebars.compile $('#spotify-partial').html()
	youtube: Handlebars.compile $('#youtube-partial').html()
	'spotify-typeahead': Handlebars.compile $('#typeahead-spotify-partial').html()
	'youtube-typeahead': Handlebars.compile $('#typeahead-youtube-partial').html()
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
		'soundcloud': "http://soundcloud.com/#{id}"
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
			if json.feed.entry?
				for video in json.feed.entry
					link = video.id.$t.split('/')
					result.push
						value: video.title.$t,
						link: 'youtu.be/' + link[link.length - 1],
						artists: video.author.map( (a) -> a.name.$t).join ', '
			result

videos.initialize()
tracks.initialize()

$ '#videofeed'
	.on 'click', '.x-button', ->
		$el = $(this).parent()
		item = $el.data 'item'
		if confirm "Confirm deletion of \"#{item.title}\"?"
			app.removeItemFromQueue(item)
			$el.remove()
	.on 'click', '.upvote, .downvote', ->
		# If trying to up-/downvote when already up-/downvoted
		return if $(this).hasClass 'active'

		$el = $(this).parent().parent()
		up = $(this).hasClass 'upvote'
		item = $el.data 'item'
		app.addVote item, up
		rate = if item.id of app.get_list() then 2 else 1
		$(this).parent().find('.rating').html(item.weight + if up then rate else -rate)
		if up
			$el.find('.upvote').addClass 'active'
			$el.find('.downvote').removeClass 'active'
		else
			$el.find('.upvote').removeClass 'active'
			$el.find('.downvote').addClass 'active'



# Bind events for parsing input url/id

$insert_video = $ '#insert_video'

read_searchfield = (e) ->
	value = $insert_video.val()
	app.parseInput(value);

$ '#searchfield button'
	.on 'click', read_searchfield

$insert_video
	.on 'keydown', (e) -> read_searchfield() if e.which == 13
	.typeahead {
		minLength: 4
	}, {
		name: 'youtube',
		displayKey: 'value',
		source: videos.ttAdapter(),
		templates:
			suggestion: TEMPLATES['youtube-typeahead']
			# suggestion: Handlebars.templates.typeahead
		limit: 15
	}, {
		name: 'spotify',
		displayKey: 'value',
		source: tracks.ttAdapter(),
		templates:
			suggestion: TEMPLATES['spotify-typeahead']
			# suggestion: Handlebars.templates.typeahead
		limit: 15
	}
	.on 'typeahead:selected', (obj, data) ->
		if app.parseInput data.link
			$(this).val('')

## Basic detction if user has cookie, not fail-safe

cookie_data = document.cookie.split /; |=/

ADMIN = false
for key in cookie_data
	if key == 'chalmersItAuth'
		$.ajax
			url: 'https://chalmers.it/auth/userInfo.php',
			xhrFields: { withCredentials: true },
			dataType: 'jsonp'
		.done (data) ->
			ADMIN = 'playITAdmin' in data.groups
			$('#videofeed').addClass('admin') if ADMIN



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
	@REGEX: /^(.*open\.)?spotify(\.com)?[\/:]track[\/:]([a-zA-Z0-9]+)$/

	constructor: (uri) ->
		@id = SpotifyItem.matches uri.trim()
		if @id?
			super @id, 'spotify'
		else
			throw new Error "Cannot create SpotifyItem, invalid uri: \"#{uri}\""

	@matches: (id) ->
		super id, @REGEX

class SoundCloudItem extends MediaItem
	@REGEX: /^https?:\/\/soundcloud.com\/([\w-]+\/\w+)$/

	constructor: (url) ->
		temp_id = SoundCloudItem.matches url.trim()
		if temp_id?
			@resolve temp_id
			super @id, 'soundcloud'
		else
			throw new Error "Cannot create SoundCloudItem, invalid url: \"#{url}\""

	resolve: (id) ->
		$.getJSON 'http://api.soundcloud.com/resolve.json', {
			url: "http://soundcloud.com/#{id}",
			client_id: 'a2cfca0784004b38b85829ba183327cb' }, (data) =>
				@id = data.id

	@matches: (id) ->
		super id, @REGEX


## Main class for the application

class App
	$feed = $ '#videofeed'
	$status = $ '#nowPlaying .media'
	LS_KEY = 'items'
	list = {}
	sent = false
	updates = 0

	get_list: ->
		return list

	constructor: ->
		if window.localStorage && window.localStorage.hasOwnProperty(LS_KEY)
			data = window.localStorage.getItem(LS_KEY)
			list = JSON.parse data

	parseInput: (string) ->
		if YouTubeItem.matches string
			@addMediaItem new YouTubeItem string
			return true
		else if SpotifyItem.matches string
			@addMediaItem new SpotifyItem string
			return true
		else if SoundCloudItem.matches string
			@addMediaItem new SoundCloudItem string
			return true
		else
			console.log string
			return false


	addMediaItem: (item) ->
		method = 'addMediaItem'
		updates++
		query method, {id: item.id, type: item.type}, (body) =>
			index = body.indexOf "Fail:"
			if index == 0
				toaster.err body
			else
				toaster.toast body
				list[item.id] = true
				saveList()
				@showQueue()

	addVote: (item, up) ->
		method = 'addVote'
		updates++
		query method, {id: item.id, upvote: if up then 1 else 0}, (body) =>
			console.log body
			list[item.id] = up
			saveList()
			@showQueue()

	query = (method, params, callback) ->
		url = "#{SERVER}/#{method}"
		if typeof params == "function"
			callback = params
			params = {}
		$.ajax
			url: url,
			xhrFields: { withCredentials: true },
			beforeSend: ->
				progressJs().start().autoIncrease(4, 500)
			data: params
		.done (body) ->
			progressJs().end()
			updates--
			callback(body)

	nowPlaying: ->
		method = 'nowPlaying'
		query method, (data) ->
			unless data.length > 0
				$status.html TEMPLATES.playing(data)
	changeLimit: (limit) ->
		method = 'changeLimit'
		query method, {limit: limit}, (body) ->
			console.log body

	saveList = ->
		window.localStorage.setItem LS_KEY, JSON.stringify list

	showQueue: ->
		unless TIMEOUT == null
			clearTimeout TIMEOUT
			TIMEOUT = null
		updates++
		method = 'showQueue'
		query method, (body) =>
			return if updates > 0
			data = JSON.parse body
			$feed.find('div.media').each (index, elem) ->
				elem = $(elem)
				item = elem.data 'item'
				data_item = null
				for i in data
					if i.externalID == item.id
						data_item = i
						break
				if data_item == null || item.weight == data_item.weight
					return
				else
					item.weight = data_item.weight
					elem.find('.rating').html(item.weight)
			app.nowPlaying()
			if data.length == $feed.find('div.media').length
				TIMEOUT = setTimeout(app.showQueue, 10000)
				return

			items = []
			savedIds = []
			for item in data
				if list[item.externalID]?
					element_class = if list[item.externalID] then 'upvoted' else 'downvoted'
				# element = Handlebars.templates["#{item.type}-partial"](item)
				element = TEMPLATES["#{item.type}"](item)
				savedIds.push item.externalID
				$el = $ element
					.data 'item', id: item.externalID, title: item.title, weight: item.weight
					.addClass element_class
				items.push $el
			$feed.html(items)
			for key of list
				delete list[key] if key not in savedIds
			saveList()
			TIMEOUT = setTimeout(app.showQueue, 10000)

	removeItemFromQueue: (item) ->
		method = 'removeItemFromQueue'
		updates++
		query method, {id: item.id}, (body) =>
			console.log body
			# @showQueue()

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
