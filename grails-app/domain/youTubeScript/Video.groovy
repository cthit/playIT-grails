package youTubeScript

class Video {

	String title;
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
	
	


	

