package youTubeScript

class Video {

	String title;
	String url;
	String thumbnail;
	Integer length;
	String description;
	String cid;
	Integer playing = -1;
	String youtubeID;
	static hasMany = [votes:Vote]
	
		
	static constraints = {
		description(maxSize:100000)
	}
}
	
	


	

