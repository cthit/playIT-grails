package youTubeScript

class Video {

	String title;
	String url;
	String thumbnail;
	Integer length;
	String description;
	String cid;
	Integer playing;
	static hasMany = [votes:Vote]
	
		
	static constraints = {
	}
}
	
	


	

