package youTubeScript

import grails.converters.JSON
import groovy.sql.Sql

import java.sql.ResultSet;
import java.util.regex.*;
import org.codehaus.groovy.grails.web.json.JSONObject
import groovyx.net.*;



class VideoController {
	def dataSource
	def nl = "<br />";
	Video playingVideo = null;

	def index() {
		render "It works!"
	}

	
	private String extractCID(){
		//System.out.println("extractCID");
		def cookie = request.cookies.find { it.name == 'chalmersItAuth' };
		//for(def cok:request.cookies){
		///	System.out.println(cok.name +" - " +cok.value);
		//}
		if(cookie != null){
			String token = cookie.value;
			def data = JSON.parse( new URL(
				 'https://chalmers.it/auth/userInfo.php?token='+token ).text );

			System.out.println(data.get("cid"));
			return data.get("cid");
		} else {
			return null;
		}
	} 
	
	def addVideo(){
		
		/*def cookie = request.cookies.find { it.name == 'chalmersItAuth' };
		for(def kak:request.cookies){
			System.out.println(kak.name);
		}*/

		//if (request.cookies.find { it.name == 'chalmersItAuth' }) {
		/*
		if(cookie != null){
			String value = cookie.value;
			System.out.println(value);
		}else{
			System.out.println("no cookie2");
		}*/
		/*
		def cookie = cookie(name: "chalmersItAuth");
		if(cookie != null){
			String value = cookie.value;
			System.out.println(value);
		}else{
			System.out.println("no cookie");
		}*/
		
		String cid = extractCID();
		if(cid == null){
			render "Fail: Authentication failed";
			return;
		}

		String url = params.url;
		String videoID = extractID(url);
		
		Video video = Video.find {youtubeID == videoID};
		
		if(video == null){//Check if already added

			def data = new URL("http://gdata.youtube.com/feeds/api/videos/"+videoID+"?alt=json").getText();
			JSONObject jsData = JSON.parse(data);
			def entry = jsData.get("entry");
			Video v = parseVideoEntry(entry, cid);	
			
			v.save();
			
			render "Success: Added video: "+v.title+" by user: "+cid+nl;
			
			params.upvote = "1";
			addVote();
			
		} else {
			render "Fail: Video aldready exists."+nl;
		}
	}
	
	private Video parseVideoEntry(def entry, def cid){
		
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

		if(duration > 18000){//60 sec * 60 min * 5 hours = 18000
			render "Fail: Videolength too long";
			return;
		}

		
		Video v = new Video(
			title:			title,
			thumbnail:		thumbnail,
			length:			duration,
			description:	desc,
			cid:			cid,
			youtubeID:		videoID
		);
		return v;
		
		
	}
	
//	private Video parseVideoEntry(def entry, def cid){
//		def author = entry.author.name;
//		
//		String title = entry.title[0].value()[0];
//		String thumbnail = entry.getAt("group").getAt("thumbnail")[0].attribute("url");
//		int duration;
//		try {
//			duration = Integer.parseInt(entry.getAt("group").getAt("duration")[0].attribute("seconds"));
//		} catch (NumberFormatException e) {
//			duration = 1337;
//		}
//		String desc = entry.getAt("group").getAt("description")[0].text();
//		
//		//Extracting videoID from xml
//		String url = entry.id[0].value()[0].toString();
//		String videoID = url.substring(url.lastIndexOf('/')+1);
//	
//		
//		Video v = new Video(
//			title:			title,
//			thumbnail:		thumbnail,
//			length:			duration,
//			description:	desc,
//			cid:			cid,
//			youtubeID:		videoID
//		);
//		return v;
//	}
	
	def addVote(){

		String cidString = extractCID();
		if(cidString == null){
			render "Fail: Authentication failed";
			return;
		}

		String url = params.url;
		boolean upvote = ("1"==params.upvote);
		String videoID = extractID(url);
		def voteValue = 0;
		if(upvote){
			voteValue = 1;
		} else {
			voteValue = -1;
		}
		
		Video v = Video.find {youtubeID == videoID};
		if(v != null){
			Vote oldVote = v.votes.find { it.cid == cidString}
			if(oldVote == null){
				Vote vote = new Vote(cid:cidString, value:voteValue);
				v.addToVotes(vote);
				v.save(flush:true);
				render "Success: Added vote by: "+vote.cid+" with value: "+
						voteValue+" to video: "+ v.title+nl;
			} else {
				def oldVoteValue = oldVote.value;
				oldVote.value = voteValue;
				oldVote.save(flush:true);
				render "Success: Updated vote by: "+oldVote.cid+" from old: "+
				oldVoteValue+" to new: "+voteValue+" to video: "+v.title+nl;
			}
			validateVideos(); //Removes videos with summed votevalue < -1
		} else {
			render "Fail: Could not find video."+nl;
		}
		
	}
	

	
	private def validateVideos(){
		def db = new Sql(dataSource)
		def results = db.rows("SELECT * FROM queue WHERE value <= -2");
		for(def res:results){
			 Video.find {id == res.getAt(0)}.delete(flush:true);
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
			List<Video> videos = new LinkedList<Video>();
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
			Video v = Video.find {id == res.getAt(0)}
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
	
	private int queryValueFromQueue(Video v){
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
		def allVideos = Video.getAll();
		render allVideos as JSON;
	}
	
	def popQueue(){
		def result = queryQueue();
		def vidID;
		if(!result.empty){
			vidID = result[0].getAt(0);
		}
		Video video = Video.find {id == vidID};
		if(video != null){
			playingVideo = video;
			render video as JSON
			video.delete(flush:true);
		} else {
			render "[]";
			video = null;
		}
	}
	
	def nowPlaying(){
		render playingVideo as JSON;
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
