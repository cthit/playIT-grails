package youTubeScript

class MediaItem {
	
	String cid;
	static hasMany = [votes:Vote];
	String type;

    static constraints = {
    }
}
