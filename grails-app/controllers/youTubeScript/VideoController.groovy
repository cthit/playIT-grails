package youTubeScript

import grails.converters.JSON
import groovy.sql.Sql

import java.awt.List;
import java.util.regex.*;
import org.apache.tomcat.jdbc.pool.DataSource
import org.hibernate.Query
import org.hibernate.classic.Session;



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
			
			
			boolean suc = v.save(failOnError: true);
			System.out.println(suc);
			
			
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
			v.save();
			def allVotes = Vote.getAll();
			render allVotes;
			render "<br />";
			def allVideos = Video.getAll();
			render allVideos;
		} else {
			render "Could not find video.";
		}
		
	}
	
	def showQueue(){

		def db = new Sql(dataSource)
		def result = db.rows("SELECT * FROM queue") // Perform the query
		def videos = [];
		for(def res:result){
			 videos.add(Video.find {id == res.getAt(0)});
		}
		render videos as JSON;
	}
	
	def showVideos(){
		def allVideos = Video.getAll();
		render allVideos as JSON;
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
