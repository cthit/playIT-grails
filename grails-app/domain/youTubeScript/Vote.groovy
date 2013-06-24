package youTubeScript

class Vote {

	static belongsTo = Video;
	Integer value = 1;
	String cid;
	
    static constraints = {
    }
}
