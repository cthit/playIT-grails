var SERVER = "http://hubben.chalmers.it:8080/",
	BASE = SERVER + "youTubeInTheHubbServer/video/",
	SHOW_URL = BASE + "showQueue",
	SEARCH_URL = BASE + "searchVideo",
	ADD_URL = BASE + "addVideo",
	ADD_VOTE = BASE + "addVote";

jQuery.ajaxPrefilter(function(options) {
	options.xhrFields = { withCredentials: true };
});


Handlebars.registerHelper('ntobr', function(string) {
	return new Handlebars.SafeString(string.replace(/\n/g, '<br>'));
});
Handlebars.registerHelper('limit', function(weight) {
	return weight > 99 ? '∞' : weight;
});
Handlebars.registerHelper('voted', function(video_id) {
	var upvote = APP.getVote(video_id);
	if (upvote === null || upvote === undefined) {
		return;
	} else {
		return upvote?'upvote':'downvote';
	}
});

var template = [
	'<p>{{name}}</p>',
	'<img class="mini-icon" src="{{thumbnail}}">',
	'<p class="repo-description">{{description}}</p>'
].join('');

function App(user) {
	var timeout = setTimeout(reloadData, 100),
		templateSrc = $('#video').html(),
		videoTemplate = Handlebars.compile(templateSrc),
		$videofeed = $('#videofeed');
	var voteData = [];
	if (localStorage.getItem('vote_data') && localStorage.getItem('vote_data') != 'undefined') {
		voteData = JSON.parse(localStorage.getItem('vote_data'));
	}

	function queryBackend(url, id, up) {
		try {
			var queryString = '?url=' + toURL(id) + '&cid=' + user.cid + '&upvote=' + (up?'1':'-1');
			$.get(url + queryString, function(result) {
				Toast.toast('Result: ' + result);
				saveVote(id, up);
				reloadData();
			});
		} catch (e) {
			if (confirm("You are not signed in, please grab a cookie from the cookie jar!")) {
				window.open("https://chalmers.it/auth");
			}
		}
	}
	function toURL(id) {
		return 'youtu.be/' + id;
	}
	function saveVote(id, up) {
		voteData[id] = up;
		localStorage.setItem('vote_data', JSON.stringify(voteData));
	}
	function videoSort(vid1, vid2) {
		return vid2.weight - vid1.weight;
	}
	function reloadData() {
		$.get(SHOW_URL, function(data) {
			data = JSON.parse(data); // TODO: HORV!!! SKICKA DATAN MED Content-Type: application/json (aka, fixa ditt hack)
			data = data.sort(videoSort);
			$videofeed.html(videoTemplate(data));
			console.log('reloaded with result:', data);
			clearTimeout(timeout);
			timeout = setTimeout(reloadData, 60000); // Every minute
		});
	}

	return {
		addVideo: function(videoID) {
			queryBackend(ADD_URL, videoID, true);
		},
		voteVideo: function(videoID, up) {
			queryBackend(ADD_VOTE, videoID, up);
		},
		getVote: function(videoID) {
			return voteData[videoID];
		}
	};
}

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

$('#videofeed').on('click', '.votes a', function() {
	var $this = $(this);
	var videoID = $this.parent().parent().data('youtube-id');
	var upvoted = $this.hasClass('upvote');
	APP.voteVideo(videoID, upvoted);
});

function ToastBar() {
	var $toaster = $('#toaster');

	function closeToast() {
		$toaster.animate({'height': 0}, function() {
			$toaster.hide();
		});
	}
	function toRed(string) {
		return '<span style="color: red">' + string + '</span>';
	}
	$toaster.click(closeToast);

	return {
		toast: function(string, err) {
			if (err) {
				string = toRed(string);
			}
			$toaster.show().html('<br>' + string + '<br><small>(Click to dismiss)</small>').animate({
				'height': 100
			}, function() {
				setTimeout(closeToast, 2000);
			});
		}
	};
}
var Toast = new ToastBar();


var APP = new App(user);
