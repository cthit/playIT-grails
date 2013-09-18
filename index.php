<?php

	define("SERVER", "http://129.16.180.11:8080/");
	define("BASE", SERVER . "youTubeInTheHubbServer/video/");
	define("SHOW_URL" , BASE . "showQueue");
	define("SEARCH_URL" , BASE . "searchVideo");
	define("ADD_URL", BASE . "addVideo");
	define("ADD_VOTE", BASE . "addVote");

?>

<!DOCTYPE html>
<html>
	<head>
		<title>YouTube in the Hubb</title>
		<meta charset="utf-8">
		<link rel="stylesheet" type="text/css" href="style.css">
	</head>
	<body>
		<div id="toaster"></div>
		<div id="modal-box">
			<div id="modal-front">
				<label for="cid-input">Enter cid:</label> <input type="text" id="cid-input" />
			</div>
		</div>
		<script id="video" type="text/x-handlebars-template">
		<table>
			<tbody>
		{{#each this}}
			<tr class="video" data-video-id="{{id}}">
				<td class="votes {{voted id}}">
				<a class="upvote">▲</a><br>
				{{limit weight}}<br>
				<a class="downvote">▼</a>
				</td>
				<td><img src="{{thumbnail}}" /></td>
				<td>
					<header><a href="http://youtu.be/{{youtubeID}}"><h3>{{title}}</h3></a><small class="cid-box">{{cid}}</small></header>
					<p>{{ntobr description}}</p>
				</td>
			</tr>
		{{/each}}
			</tbody>
		</table>
		</script>
		<div id="search_video">
			<input id="insert_video" placeholder="Search for youtube video or enter URL">
		</div>
		<div id="videofeed">
			<em>Loading content...</em>
		<script type="text/javascript" src="//ajax.googleapis.com/ajax/libs/jquery/2.0.3/jquery.min.js"></script>
		</div>
		<script type="text/javascript" src="handlebars.js"></script>
		<script type="text/javascript" src="typeahead.min.js"></script>
		<script type="text/javascript" src="script.js"></script>
	</body>
</html>