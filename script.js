var SERVER = "http://129.16.180.11:8080/",
	BASE = SERVER + "youTubeInTheHubbServer/video/",
	SHOW_URL = BASE + "showQueue",
	SEARCH_URL = BASE + "searchVideo",
	ADD_URL = BASE + "addVideo",
	ADD_VOTE = BASE + "addVote";
var APP;
if ((cid = localStorage.getItem('cid'))) {
	APP = new App(cid);
} else {
	showModal();
}

Handlebars.registerHelper('ntobr', function(string) {
	return new Handlebars.SafeString(string.replace(/\n/g, '<br>'));
});
Handlebars.registerHelper('limit', function(weight) {
	return weight > 99 ? '∞' : weight;
});
Handlebars.registerHelper('voted', function(video_id) {
	return currentState[video_id]?'upvote':'downvote';
});

var template = [
	'<p>{{name}}</p>',
	'<img class="mini-icon" src="{{thumbnail}}">',
	'<p class="repo-description">{{description}}</p>'
].join('');

function showModal() {
	$('#modal-box').show();
	$('#cid-input').focus().on('keydown', function(e) {
		if (e.which == 13 && this.value.length > 4) {
			var cid = this.value.toLowerCase();
			localStorage.setItem('cid', cid);
			APP = new App(cid);
			$('#modal-box').hide();
		}
	});
}
function App(cid) {
	this.cid = cid;
	this.voteData = JSON.parse(localStorage.getItem('vote_data')) || [];
	timeout = setTimeout(this.reload, 100);
}
App.prototype = (function() {
	var timeout,
		that = this,
		templateSrc = $('#video').html(),
		videoTemplate = Handlebars.compile(templateSrc),
		$videofeed = $('#videofeed');

	function queryBackend(url, id, up) {
		var queryString = '?url=' + toURL(id) + '&cid=' + this.cid + '&upvote=' + (up?'1':'-1');
		//$.get(url + queryString, function(result) {
			var result = "OFFLINE";
			toast('Result: ' + result);
			console.log('result:', result);
			saveVote(id, up);
			reloadData();
		//});
	}
	function saveVote(id, up) {
		that.voteData[id] = up;
		localStorage.setItem('vote_data', JSON.stringify(that.voteData));
	}
	function videoSort(vid1, vid2) {
		return vid2.weight - vid1.weight;
	}

	return {
		constructor: App,
		addVideo: function(video) {
			queryBackend(ADD_URL, video.getURL(), true);
		},
		voteVideo: function(video, up) {
			queryBackend(ADD_VOTE, video.getURL(), up);
			saveVote(video.id, up);
		},
		reload: function() {
			var data = [{"class":"youTubeScript.Video","id":3,"cid":"jolinds","description":"This is the one and only official version of Rebecca Black's \"Friday\"  music video.","length":228,"playing":-1,"thumbnail":"http://i1.ytimg.com/vi/kfVsfOSbJY0/0.jpg","title":"Friday - Rebecca Black - Official Music Video","votes":[{"class":"Vote","id":3}],"youtubeID":"kfVsfOSbJY0","weight":3},{"class":"youTubeScript.Video","id":1,"cid":"jolinds","description":"Music video by Rick Astley performing Never Gonna Give You Up. YouTube view counts pre-VEVO: 2,573,462 (C) 1987 PWL","length":213,"playing":-1,"thumbnail":"http://i1.ytimg.com/vi/dQw4w9WgXcQ/0.jpg","title":"Rick Astley - Never Gonna Give You Up","votes":[{"class":"Vote","id":1}],"youtubeID":"dQw4w9WgXcQ","weight":5},{"class":"youTubeScript.Video","id":2,"cid":"jolinds","description":"Frida (2002) trailer","length":145,"playing":-1,"thumbnail":"http://i1.ytimg.com/vi/zudfarZ-ZNk/0.jpg","title":"Frida (2002) HQ trailer","votes":[{"class":"Vote","id":2}],"youtubeID":"zudfarZ-ZNk","weight":1}];
			//$.get(SHOW_URL, function(data) {
				//data = JSON.parse(data); // @TODO: HORV!!! SKICKA DATAN MED Content-Type: application/json
				data = data.sort(videoSort);
				$videofeed.html(videoTemplate(data));
			//});
			console.log('reloaded with result:', data);
			clearTimeout(timeout);
			timeout = setTimeout(reload, 1000);//60000);
		}
	};
})();

function Video(youtubeID, upvoted) {
	this.id = youtubeID;
	this.upvoted = upvoted;
}
Video.prototype.getURL = function() {
	return 'youtu.be/' + this.id;
};

$('#insert_video').typeahead({
	name: 'videos',
	valueKey: 'title',
	remote: SEARCH_URL + '?q=%QUERY',
	template: Handlebars.compile(template),
	limit: 10
}).on('typeahead:selected', function(obj, data) {
	console.log(data);
	APP.addVideo(data.youtubeID);
});

$('#videofeed').on('click', 'a.upvote, a.downvote', function() {
	var $this = $(this);
	var videoID = $this.parent().parent().data('video-id');
	var upvoted = $this.hasClass('upvote');
	APP.voteVideo(videoID, upvoted);

	console.log(videoID);
});

function Toast() {
	var $toaster = $('#toaster'),
		that = this;

	function closeToast() {
		$toaster.animate({'height': 0}, function() {
			$toaster.hide();
		});
	}
	function toRed(string) {
		return '<span style="color: red">' + string + '</span>';
	}
	$toaster.click(closeToast);
}
Toast.prototype.toast = function(string, err) {
	if (err) {
		string = toRed(string);
	}
	$toaster.show().html('<br>' + string + '<br><small>(Click to dismiss)</small>').animate({
		'height': 100
	}, function() {
		setTimeout(closeToast, 2000);
	});
};
