package youTubeScript

class Vote {

	static belongsTo = Video;
	boolean upvote = true;
	String cid;
	
    static constraints = {
    }
}
