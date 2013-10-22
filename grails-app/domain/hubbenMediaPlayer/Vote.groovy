package hubbenMediaPlayer

class Vote {

	static belongsTo = MediaItem;
	Integer value = 1;
	String cid;
	
    static constraints = {
    }
}
