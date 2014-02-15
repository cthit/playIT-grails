package playIT

import grails.converters.JSON
import groovy.sql.Sql

import java.sql.ResultSet;
import java.util.regex.*;

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import playIT.MediaItem;
import playIT.SpotifyItem;
import playIT.Vote;
import playIT.YoutubeItem;

import groovyx.net.*;

class MediaController {
	def dataSource
	private static final def nl = "<br />";
	private static int VIDEO_LIMIT = 18000; //60 sec * 60 min * 5 hours = 18000
	private static int TRACK_LIMIT = 480; //60 sec * 8 min = 480
	private static final String SC_ID = "a2cfca0784004b38b85829ba183327cb";
	private static final String adminGroup = "playITAdmin";

	private static MediaItem playingItem = null;

	def index() {
		render "It works!"
	}
	
	//Method to extract CID from cookie using the chalmers.it
	//auth system implemented by digIT13/14
	private String[] extractCID(){
		def cookie = request.cookies.find { it.name == 'chalmersItAuth' };
		System.out.println("extracting CID...")
		if(cookie != null){
			String token = cookie.value;
			def data = JSON.parse( new URL(
					'https://chalmers.it/auth/userInfo.php?token='+token ).text );
			String cid = data.get("cid");
			String nick = data.get("nick");
			System.out.println("cid: "+cid)
			return [cid, nick];
		} else {
			return null;
		}
	}
	
	//Adds a media item according to type selected
	def addMediaItem(){
		String[] cidNick = extractCID();
		if(cidNick == null){
			render "Fail: Authentication failed";
			return;
		} else {
			params.cid = cidNick[0];
			params.nick = cidNick[1];
		}
		
		addItem();
		
//		if(params.type.equals("spotify")){
//			addSpotifyItem();
//		} else if(params.type.equals("youtube")){
//			addYouTubeItem();
//		} else {
//			render "Fail: Type not specified correctly: spotify / youtube"
//		}
	}
	
	private void addItem(){
		MediaItem mi = MediaItem.find {externalID==params.id};
		if(mi == null){//Check if already added
			mi = createMediaItem();
			if(mi == null){
				return;
			}
			int limit;
			if(mi instanceof YoutubeItem){
				limit = VIDEO_LIMIT;
			} else {
				limit = TRACK_LIMIT;
			}
			
			if(mi.length > limit && !isAdmin()){//If length is longer then allowed limit
				render "Fail: Item length too long";
				return;
			} else {
				mi.save(flush:true);
				String message = "Success: Added item: "+mi.title+" by user: "+params.cid+nl;
				render message;
				System.out.println(message);
				params.upvote = "1";
				addVote();
			}
		} else {
				render "Fail: Item aldready exists."+nl;
		}
	}
	
	private MediaItem createMediaItem(){
		if(params.type.equals("spotify")){
			return createSpotifyItem();
		} else if(params.type.equals("youtube")){
			return createYouTubeItem();
		} else if(params.type.equals("soundcloud")){
			return createSoundCloudItem();
		} else {
			render "Fail: Type not specified correctly: spotify / youtube / soundcloud"
			return;
		}
	}
	
	private SpotifyItem createSpotifyItem(){
		def data;
		try {
			data = new URL(
				"http://ws.spotify.com/lookup/1/.json?uri=spotify:track:"+params.id).getText();
		} catch (IOException e) {
			render "Fail: Could not find spotify track with id: "+params.id
			return;
		}
		JSONObject jsData = JSON.parse(data);
		JSONObject track = jsData.get("track");
		return parseSpotifyTrackEntry(track, params.cid, params.nick);
	}
	
	private YoutubeItem createYouTubeItem(){
		def data;
		try {
			data = new URL("http://gdata.youtube.com/feeds/api/videos/"+params.id+"?alt=json").getText();
		} catch (IOException e) {
			render "Fail: Could not find youtube video with id: "+params.id
			return;
		}
		JSONObject jsData = JSON.parse(data);
		def entry = jsData.get("entry");
		return parseVideoEntry(entry, params.cid, params.nick);
	}
	
	private SoundCloudItem createSoundCloudItem(){
		def data;
		try {
			data = new URL("http://api.soundcloud.com/tracks/"+params.id+".json?client_id="+SC_ID).getText();
		} catch (IOException e) {
			render "Fail: Could not find SoundCloud track with id: "+params.id
			return;
		}
		JSONObject track = JSON.parse(data);
		return parseSoundTrackEntry(track, params.cid, params.nick);
	}
	
	private SoundCloudItem parseSoundTrackEntry(def track, def cid, def nick){
		String title = track.get("title");
		String user = track.get("user").get("username");
		String thumbnail = track.get("artwork_url");
		int duration = track.get("duration")/1000; //To convert from miliseconds to seconds
		
		SoundCloudItem sci = new SoundCloudItem(
			title:			title,
			thumbnail: 		thumbnail,
			length: 		duration,
			user:			user,
			externalID: 	params.id,
			type:			"spotify",
			cid:			cid,
			nick:			nick
			)
		return sci;
	}
	
	
	

	//Add youtube-item specified by youtube-ID as param ID.
//	private def addYouTubeItem(){
//
//		//String url = params.id;
//		YoutubeItem video = YoutubeItem.find {externalID==params.id};
//
//		if(video == null){//Check if already added
//			def data;
//			try {
//				data = new URL("http://gdata.youtube.com/feeds/api/videos/"+params.id+"?alt=json").getText();
//			} catch (IOException e) {
//				render "Fail: Could not find youtube video with id: "+params.id
//				return;
//			}
//			
//			JSONObject jsData = JSON.parse(data);
//			def entry = jsData.get("entry");
//			YoutubeItem v = parseVideoEntry(entry, params.cid);
//
//			if(v.length > LIMIT){//If length is longer then alloweed limit
//				render "Fail: Videolength too long";
//				return;
//			} else {
//				v.save(flush:true);
//				String message = "Success: Added video: "+v.title+" by user: "+params.cid+nl;
//				render message;
//				System.out.println(message);
//				params.upvote = "1";
//				addVote();
//			}
//			
//		} else {
//			render "Fail: Video aldready exists."+nl;
//		}
//	}
	
	//Parsing info from youtube video using open API.
	private YoutubeItem parseVideoEntry(def entry, def cid, def nick){
		String url = entry.get("id").get("\$t");
		String videoID = url.substring(url.lastIndexOf('/')+1);
		String title = entry.get("title").get("\$t");
		String author = entry.get("author").get(0).get("name").get("\$t");
		String desc = entry.get("content").get("\$t");
		String thumbnail = entry.get("media\$group").get("media\$thumbnail").get(0).get("url");

		int duration;
		try {
			duration = Integer.parseInt(entry.get("media\$group").get("yt\$duration").get("seconds"));
		} catch (NumberFormatException e) {
			duration = 1337;
		}

		YoutubeItem v = new YoutubeItem(
				title:			title,
				thumbnail:		thumbnail,
				length:			duration,
				description:	desc,
				cid:			cid,
				nick:			nick,
				externalID:		videoID,
				type:			"youtube"
				);
		return v;

	}
	
	
//	//Add youtube-item specified by spotifyTrack-ID as param ID.
//	private def addSpotifyItem(){
//		SpotifyItem si = SpotifyItem.find {externalID == params.id};
//		if(si == null){//Check if already added
//			def data;
//			try {
//				data = new URL(
//					"http://ws.spotify.com/lookup/1/.json?uri=spotify:track:"+params.id).getText();
//			} catch (IOException e) {
//				render "Fail: Could not find spotify track with id: "+params.id
//				return;
//			}
//			
//			JSONObject jsData = JSON.parse(data);
//			JSONObject track = jsData.get("track");
//			si = parseSpotifyTrackEntry(track, params.cid);
//
//			si.save(flush:true);
//			String message = "Success: Added SpotifyTrack: "+si.title+" by user: "+params.cid+nl;
//			render message;
//			System.out.println(message);
//
//			params.upvote = "1";
//			addVote();
//		}else {
//			render "Fail: SpotifyTrack aldready exists."+nl;
//		}
//	}
	
	//Parsing info from spotify track using open API.
	private def SpotifyItem parseSpotifyTrackEntry(def track, def cid, def nick){
		String title = track.get("name");

		JSONObject artists = new JSONObject();
		JSONArray arr = new JSONArray();

		for(def a:track.getJSONArray("artists")){
			arr.put(a.get("name"));
		}

		String album = track.get("album").get("name");
		int duration = track.get("length")+0.5;

		def tndata = new URL(
				"https://embed.spotify.com/oembed/?url=spotify:track:"+params.id).getText();
		JSONObject tnJsData = JSON.parse(tndata);

		String tn = tnJsData.get("thumbnail_url");

		SpotifyItem si = new SpotifyItem(
				title:			title,
				thumbnail: 		tn,
				length: 		duration,
				externalID: 	params.id,
				artist:			arr,
				album:			album,
				type:			"spotify",
				cid:			cid,
				nick:			nick
				)
		return si;
	}

	//Adds vote to specified ID
	def addVote(){

		String[] cidNick = extractCID();
		if(cidNick == null){
			render "Fail: Authentication failed";
			return;
		}


		String cidString = cidNick[0];
		
		//Determines if it's an up- or downvote.
		boolean upvote = ("1"==params.upvote);
		def voteValue = 0;
		if(upvote){
			voteValue = 1;
		} else {
			voteValue = -1;
		}

		MediaItem mi = MediaItem.find {externalID == params.id}
		if(mi != null){
			Vote oldVote = mi.votes.find{it.cid == cidString}
			if(oldVote == null){
				Vote vote = new Vote(cid:cidString, value:voteValue);
				mi.addToVotes(vote);
				mi.save(flush:true);
				String message = "Success: Added vote by: "+vote.cid+" with value: "+
						voteValue+" to MediaItem: "+ mi.title+nl;
				render message;
				System.out.println(message);
			} else {
				def oldVoteValue = oldVote.value;
				oldVote.value = voteValue;
				oldVote.save(flush:true);
				String message = "Success: Updated vote by: "+oldVote.cid+" from old: "+
						oldVoteValue+" to new: "+voteValue+" to SpotifyITem: "+mi.title+nl;
				render message;
				System.out.println(message);
			}
			validateMediaItems();
		} else {
			render "Fail: Could not find item with id"+params.id;
		}

	}

	private def validateMediaItems(){
		def db = new Sql(dataSource)
		def results = db.rows("SELECT * FROM queue WHERE value <= -2");
		for(def res:results){
//			MediaItem.find {id == res.getAt(0)}.delete(flush:true);
			deleteItem(res.getAt(0));
		}
	}
	
	private boolean deleteItem(def rmID){
		def item = MediaItem.find {id == rmID};
		if(item != null){
			item.delete(flush:true);
			return true;
		} else {
			return false;
		}
	}

	def searchVideo(){
		def searchQuery = params.q;
		searchQuery = URLEncoder.encode(searchQuery, "UTF-8");
		def data = new URL("http://gdata.youtube.com/feeds/api/videos?q="+
				searchQuery+"&alt=json").getText();
		//def parser = new XmlParser();
		//parser.setNamespaceAware(false);
		//def xmlVideos =  parser.parseText(data);
		JSONObject jsData = JSON.parse(data);
		if(jsData.get("feed").has("entry")){
			def entries = jsData.get("feed").get("entry");
			List<YoutubeItem> videos = new LinkedList<YoutubeItem>();
			for(def ent:entries){
				videos.add(parseVideoEntry(ent, ""));
			}
			render videos as JSON;
		}
		return "[]";
	}

	def showQueue(){

		def result = queryQueue();
		String queue = ""
		for(def res:result){
			//			 videos.add(Video.find {id == res.getAt(0)});
			// Beautiful fulhack
			MediaItem mi = MediaItem.find {id == res.getAt(0)}
			queue += mi as JSON;
			queue = queue.substring(0,queue.length()-1);
			queue += ",\"weight\":"+queryValueFromQueue(mi)+"},";
		}
		if(queue.length() == 0){
			render "[]";
		} else {
			render "["+queue.substring(0, queue.length()-1)+"]";
		}
	}
	
	//Query value from a vote.
	private int queryValueFromQueue(MediaItem mi){
		def mediaItemID = mi.id;
		def db = new Sql(dataSource);
		def result = db.rows("SELECT value FROM queue WHERE (Id="+mediaItemID+")");
		if(!result.empty){
			return (int)result[0].getAt(0);
		} else {
			return 0;
		}
	}

	private def queryQueue(){
		def db = new Sql(dataSource);
		return db.rows("SELECT * FROM queue");
	}

	//	def showVideos(){
	//		def allVideos = YoutubeItem.getAll();
	//		render allVideos as JSON;
	//	}

	def popQueue(){
		def result = queryQueue();
		def mediaItemID;
		if(!result.empty){
			mediaItemID = result[0].getAt(0);
		}
		MediaItem mi = MediaItem.find {id == mediaItemID};
		if(mi != null){
			this.playingItem = mi;
			render mi as JSON
			mi.delete(flush:true);
		} else {
			this.playingItem = null;
			render "[]";
		}
	}

	//Returns currently playing item. (latest popped if not null)
	def nowPlaying(){
		if(playingItem == null){
			render "[]"
		} else {
			render playingItem as JSON;
		}
	}
	
	//--------------Admin stuff below--------------------------------

	//Tell if a user belongs to required admin group
	def isAdmin(){
		def cookie = request.cookies.find { it.name == 'chalmersItAuth' };
		render "isAdmin";
		if(cookie != null){
			String token = cookie.value;
			JSONObject data = JSON.parse( new URL(
					'https://chalmers.it/auth/userInfo.php?token='+token ).text );
			JSONArray groups = data.getJSONArray("groups");
			for(def g:groups){
				if(g.equals(adminGroup)){
					System.out.println("User "+data.get("cid")+" authorized as admin");
					return true;
				}
			}
		} else {
			return false;
		}
	}

	//Allows admin changing limit of maximum media length.
	def changeLimit(){
		if(isAdmin()){
			try {
				int tmp = Integer.parseInt(params.limit);
				if(tmp > 0){
					int old;
					if(params.type.equals("videolimit")){
						old = VIDEO_LIMIT;
						VIDEO_LIMIT = tmp;
						String message = "Success: Updated old limit: "+old+ " with new: "+VIDEO_LIMIT;
						render message;
						System.out.println(message);
					} else if(params.type.equals("tracklimit")){
						old = TRACK_LIMIT;
						TRACK_LIMIT = tmp;
						String message = "Success: Updated old limit: "+old+ " with new: "+TRACK_LIMIT;
						render message;
						System.out.println(message);
					} else {
						render "Fail: type is invalid. Should be videolimit or tracklimit";
					}
				} else {
					render "Fail: new limit " + tmp + " is invalid. Still using old limit: "+VIDEO_LIMIT;
				}
			} catch(NumberFormatException e) {
				render "Fail: Invalid limit input"
			}
		} else {
			render "Fail: Admin authentication failed";
		}
	}
	
	//Allows admin removing an item from queue by external ID.
	def removeItemFromQueue(){
		if(isAdmin()){
			def item = MediaItem.find {externalID == params.id};
			if(item != null){
				String message = "Success: "+item.title +" deleted";
				render message;
				System.out.println(message);
				item.delete(flush:true);
			} else {
			render "Fail: id: "+params.id+" not found";
			}
		} else {
			render "Fail: Admin authentication failed";
		}
	}

	private def String extractID(String s){
		String regExp = /^.*(youtu.be\/|v\/|u\/\w\/|embed\/|watch\?v=|\&v=)([^#\&\?]*).*/;
		Matcher matcher = Pattern.compile(regExp).matcher(s);
		if ( matcher.find() ) {
			return matcher.group(2);
		} else {
			return "dQw4w9WgXcQ"; //Good one!
		}
	}

}
