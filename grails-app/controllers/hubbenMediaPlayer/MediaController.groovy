package hubbenMediaPlayer

import grails.converters.JSON
import groovy.sql.Sql

import java.sql.ResultSet;
import java.util.regex.*;

import org.codehaus.groovy.grails.web.json.JSONArray
import org.codehaus.groovy.grails.web.json.JSONObject

import groovyx.net.*;
import hubbenMediaPlayer.YoutubeItem;
import hubbenMediaPlayer.Vote;



class MediaController {
	def dataSource
	def nl = "<br />";
	MediaItem playingItem = null;

	def index() {
		render "It works!"
	}

	 private String extractCID(){
                def cookie = request.cookies.find { it.name == 'chalmersItAuth' };
                for(def cok:request.cookies){
                        System.out.println(cok.name +" : "+cok.value)
                }
                System.out.println("extracting CID...")
                if(cookie != null){
                        String token = cookie.value;
                        def data = JSON.parse( new URL(
                                 'https://chalmers.it/auth/userInfo.php?token='+token ).text );
                        String cid = data.get("cid");
                        System.out.println("cid: "+cid)

                        return cid;
                } else {
                        return null;
                }
        }
	 
	 def addMediaItem(){
//		 String cid = extractCID();
//		 if(cid == null){
//			 render "Fail: Authentication failed";
//			 return;
//		 } else {
//		 	params.cid = cid;
//		 }
		 String cid = "jolinds";
		 params.cid = cid;
		 if(params.type.equals("Spotify")){
			 addSpotifyItem();
		 } else if(params.type.equals("YouTube")){
		 	addYouTubeItem();
		 }
	 }
 
	
	def addYouTubeItem(){
		
		

		String url = params.id;
		//String videoID = extractID(url);
		
		YoutubeItem video = YoutubeItem.find {externalID==params.id};
		
		if(video == null){//Check if already added

			def data = new URL("http://gdata.youtube.com/feeds/api/videos/"+params.id+"?alt=json").getText();
			JSONObject jsData = JSON.parse(data);
			def entry = jsData.get("entry");
			YoutubeItem v = parseVideoEntry(entry, params.cid);	

			if(v.length > 18000){//60 sec * 60 min * 5 hours = 18000
				render "Fail: Videolength too long";
				return;
			} else {
			
				v.save(flush:true);
			
				render "Success: Added video: "+v.title+" by user: "+params.cid+nl;
			
				params.upvote = "1";
				addVote();
			}
			
		} else {
			render "Fail: Video aldready exists."+nl;
		}
	}
	
	private YoutubeItem parseVideoEntry(def entry, def cid){
		
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
			externalID:		videoID,
			type:			"YouTube"
		);
		return v;
		
		
	}
	
	def addSpotifyItem(){
		
		SpotifyItem si = SpotifyItem.find {externalID == params.id};
		
		if(si == null){//Check if already added
		
			def data = new URL(
				"http://ws.spotify.com/lookup/1/.json?uri=spotify:track:"+params.id).getText();
			JSONObject jsData = JSON.parse(data);
			JSONObject track = jsData.get("track");
			si = parseTrackEntry(track, params.cid);
			
			si.save(flush:true);
			
			render "Success: Added SpotifyTrack: "+si.title+" by user: "+params.cid+nl;
			
			params.upvote = "1";
			addVote();		
		}else {
			render "Fail: SpotifyTrack aldready exists."+nl;
		}		
	}
	
	def SpotifyItem parseTrackEntry(def track, def cid){
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
			type:			"Spotify",
			cid:			cid
		)
		return si;
	}
	
	
	
	def addVote(){

		//		String cidString = extractCID();
		//		if(cidString == null){
		//			render "Fail: Authentication failed";
		//			return;
		//		}


		String cidString = params.cid;
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
				render "Success: Added vote by: "+vote.cid+" with value: "+
						voteValue+" to MediaItem: "+ mi.title+nl;
			} else {
				def oldVoteValue = oldVote.value;
				oldVote.value = voteValue;
				oldVote.save(flush:true);
				render "Success: Updated vote by: "+oldVote.cid+" from old: "+
						oldVoteValue+" to new: "+voteValue+" to SpotifyITem: "+mi.title+nl;
			}
			validateMediaItems();
		}

	}
	

	
	private def validateMediaItems(){
		def db = new Sql(dataSource)
		def results = db.rows("SELECT * FROM queue WHERE value <= -2");
		for(def res:results){
			 MediaItem.find {id == res.getAt(0)}.delete(flush:true);
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
			YoutubeItem v = YoutubeItem.find {id == res.getAt(0)}
			queue += v as JSON;
			queue = queue.substring(0,queue.length()-1);
			queue += ",\"weight\":"+queryValueFromQueue(v)+"},";
		}
		if(queue.length() == 0){
			render "[]";
		} else {
			render "["+queue.substring(0, queue.length()-1)+"]";
		}
	}
	
	private int queryValueFromQueue(YoutubeItem v){
		def vidID = v.id;
		def db = new Sql(dataSource);
		def result = db.rows("SELECT value FROM queue WHERE (Id="+vidID+")");
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
	
	def showVideos(){
		def allVideos = YoutubeItem.getAll();
		render allVideos as JSON;
	}
	
	def popQueue(){
		def result = queryQueue();
		def vidID;
		if(!result.empty){
			vidID = result[0].getAt(0);
		}
		YoutubeItem video = YoutubeItem.find {id == vidID};
		if(video != null){
			playingItem = video;
			render video as JSON
			video.delete(flush:true);
		} else {
			render "[]";
			video = null;
		}
	}
	
	def nowPlaying(){
		render playingItem as JSON;
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
