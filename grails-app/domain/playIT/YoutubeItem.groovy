package playIT

class YoutubeItem extends MediaItem{

	String title;
	String thumbnail;
	String description;
	//String youtubeID;
	
		
	static constraints = {
		description(maxSize:100000)
	}
}
	
	


	

