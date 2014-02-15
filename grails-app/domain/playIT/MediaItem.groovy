package playIT

class MediaItem {
	
	String cid;
	String nick;
	static hasMany = [votes:Vote];
	String type;
	String externalID;
	Integer length;

    static constraints = {
    }
}
