<?php
	define("IS_ACTIVE", true);

	if (!IS_ACTIVE) {
		die("<h1>FAILED: The YouTube-queue server is not available right now.</h1>If you feel that it should, contact a nearby member of digIT");
	}
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
		<script id="video" type="text/x-handlebars-template">
		<table>
			<tbody>
		{{#each this}}
			<tr class="video" data-video-id="{{id}}" data-youtube-id="{{youtubeID}}">
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
		<script type="text/javascript" src="it_auth.php"></script>
		<script type="text/javascript" src="script.js"></script>
	</body>
</html>
