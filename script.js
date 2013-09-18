var currentState = JSON.parse(localStorage.getItem('vote_data')) || [];
var reloadTimeout;
console.log('currentState:', currentState);

localStorage.getItem('cid') || showModal();

Handlebars.registerHelper('ntobr', function(string) {
	return new Handlebars.SafeString(string.replace(/\n/g, '<br>'));
});
Handlebars.registerHelper('limit', function(weight) {
	return weight > 99 ? '∞' : weight;
});
Handlebars.registerHelper('voted', function(video_id) {
	return currentState[video_id]?'upvote':'downvote';
});
var source     = $('#video').html();
var video      = Handlebars.compile(source);
var $videofeed = $('#videofeed');

var template = [
	'<p>{{name}}</p>',
	'<img class="mini-icon" src="{{thumbnail}}">',
	'<p class="repo-description">{{description}}</p>'
].join('');

function showModal() {
	$('#modal-box').show();
	$('#cid-input').focus().on('keydown', function(e) {
		if (e.which == 13 && this.value.length > 4) {
			localStorage.setItem('cid', this.value.toLowerCase());
			$('#modal-box').hide();
		}
	});
}

function toURL(id) {
	return 'youtu.be/' + id;
}

function saveVote(id, up) {
	currentState[id] = up;
	localStorage.setItem('vote_data', JSON.stringify(currentState));
}

function addVideo(id) {
	queryBackend('<?=ADD_URL?>', id, true);
}

function voteVideo(id, up) {
	queryBackend('<?=ADD_VOTE?>', id, up);
}
function queryBackend(url, id, up) {
	var queryString = '?url=' + toURL(id) + '&cid=' + localStorage.getItem('cid') + '&upvote=' + (up?'1':'-1');
	//$.get(url + queryString, function(result) {
		var result = "OFFLINE";
		toast('Result: ' + result);
		//console.log('result:', result);
		saveVote(id, up);
		reloadData();
	//});
}

$('#insert_video').typeahead({
	name: 'videos',
	valueKey: 'title',
	remote: '<?=SEARCH_URL?>?q=%QUERY',
	template: Handlebars.compile(template),
	limit: 10
}).on('typeahead:selected', function(obj, data) {
	addVideo(data.youtubeID);
});

$('#videofeed').on('click', 'a.upvote, a.downvote', function() {
	var $this = $(this);
	var videoID = $this.parent().parent().data('video-id');
	var upvoted = $this.hasClass('upvote');
	voteVideo(videoID, upvoted);

	console.log(videoID);
});

function sortByVoteCount(videoA, videoB) {
	return videoB.weight - videoA.weight;
}

function reloadData() {
	var data = [{"class":"youTubeScript.Video","id":3,"cid":"jolinds","description":"This is the one and only official version of Rebecca Black's \"Friday\"  music video.","length":228,"playing":-1,"thumbnail":"http://i1.ytimg.com/vi/kfVsfOSbJY0/0.jpg","title":"Friday - Rebecca Black - Official Music Video","votes":[{"class":"Vote","id":3}],"youtubeID":"kfVsfOSbJY0","weight":3},{"class":"youTubeScript.Video","id":1,"cid":"jolinds","description":"Music video by Rick Astley performing Never Gonna Give You Up. YouTube view counts pre-VEVO: 2,573,462 (C) 1987 PWL","length":213,"playing":-1,"thumbnail":"http://i1.ytimg.com/vi/dQw4w9WgXcQ/0.jpg","title":"Rick Astley - Never Gonna Give You Up","votes":[{"class":"Vote","id":1}],"youtubeID":"dQw4w9WgXcQ","weight":5},{"class":"youTubeScript.Video","id":2,"cid":"jolinds","description":"Frida (2002) trailer","length":145,"playing":-1,"thumbnail":"http://i1.ytimg.com/vi/zudfarZ-ZNk/0.jpg","title":"Frida (2002) HQ trailer","votes":[{"class":"Vote","id":2}],"youtubeID":"zudfarZ-ZNk","weight":1}];
	//$.get('<?=SHOW_URL?>', function(data) {
		//data = JSON.parse(data); // @TODO: HORV!!! SKICKA DATAN MED Content-Type: application/json
		data = data.sort(sortByVoteCount);
		$videofeed.html(video(data));
	//});
	console.log('reloaded with result:', data);
	clearTimeout(reloadTimeout);
	reloadTimeout = setTimeout(reloadData, 60000);
}
var timeout = setTimeout(reloadData, 100);

function toast(string) {
	$('#toaster').show().html('<br>' + string + '<br><small>(Click to dismiss)</small>').animate({
		'height': 100
	}).on('click', function() {
		$(this).animate({'height': 0}, function() {
			$(this).hide();
		});
	});
}
