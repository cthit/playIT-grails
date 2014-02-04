package playIT

class MediaItem {
	
	String cid;
	static hasMany = [votes:Vote];
	String type;
	String externalID;

    static constraints = {
    }
}
