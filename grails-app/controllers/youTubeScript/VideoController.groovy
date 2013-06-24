package youTubeScript

import grails.converters.JSON
import groovy.sql.Sql
import java.util.regex.*;



class VideoController {
	def dataSource

	def index() {
	}

	def addVideo(){
		
		String url = params.url;
		String cid = params.cid;
		String videoID = extractID(url);
		
		Video video = Video.find {youtubeID == videoID};
		if(video == null){
			
			String data = new URL("http://gdata.youtube.com/feeds/api/videos/"+videoID).getText();
	
			def parser = new XmlParser();
			parser.setNamespaceAware(false);
			def xmlVideo =  parser.parseText(data);
			def author = xmlVideo.author.name;
			
			String title = xmlVideo.title[0].value()[0];
			String thumbnail = xmlVideo.getAt("group").getAt("thumbnail")[0].attribute("url");
			int duration;
			try {
				duration = Integer.parseInt(xmlVideo.getAt("group").getAt("duration")[0].attribute("seconds"));
			} catch (NumberFormatException e) {
				duration = 1337;
			}
			String desc = xmlVideo.getAt("group").getAt("description")[0].text();
	
			String nl = "<br />";
			render title + nl + thumbnail + nl + duration + nl + desc;
			
			Video v = new Video(
				title:			title,
				url:			url,
				thumbnail:		thumbnail,
				length:			duration,
				description:	desc,
				cid:			cid,
				youtubeID:		videoID
			);
			
			
			v.save();
			addVote();
			
			def allVideos = Video.getAll();
			render allVideos as JSON;
			
		} else {
			render "Already added."
		}
	}
	
	def addVote(){
		String cid = params.cid;
		String url = params.url;
		String videoID = extractID(url);
		Video v = Video.find {youtubeID == videoID};
		if(v != null){
			Vote vote = new Vote(cid:cid, value:1);
			v.addToVotes(vote);
			v.save(flush:true);
			def allVotes = Vote.getAll();
			render allVotes;
			render "<br />";
			def allVideos = Video.getAll();
			render allVideos;
			validateVideos();
		} else {
			render "Could not find video.";
		}
		
	}
	
	private def validateVideos(){
		def db = new Sql(dataSource)
		def results = db.rows("SELECT * FROM queue WHERE value <= -2");
		for(def res:results){
			 Video.find {id == res.getAt(0)}.delete(flush:true);
		}
		
	}
	
	def showQueue(){

		def result = queryQueue();  
		def videos = [];
		for(def res:result){
			 videos.add(Video.find {id == res.getAt(0)});
		}
		render videos as JSON;
	}
	
	private def queryQueue(){
		def db = new Sql(dataSource)
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
			render video as JSON
			video.delete(flush:true);
		} else {
			render "[]";
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
