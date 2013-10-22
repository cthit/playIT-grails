package youTubeScript

class SpotifyItem extends MediaItem {

	String title;
	String thumbnail;
	Integer length;
	String description;
	String spotifyURI;
	
		
	static constraints = {
		description(maxSize:100000)
	}
	
	
}
