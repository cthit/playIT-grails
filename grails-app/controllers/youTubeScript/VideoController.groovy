package youTubeScript





class VideoController {

	def index() {
	}

	def addVideo(){

		String data = new URL("http://gdata.youtube.com/feeds/api/videos/1zAvdPip3xk").getText();

		def parser = new XmlParser();
		parser.setNamespaceAware(false);
		def xmlVideo =  parser.parseText(data);
		def author = xmlVideo.author.name;
		
		String title = xmlVideo.title[0].value()[0];
		String thumbnail = xmlVideo.getAt("group").getAt("thumbnail")[0].attribute("url");
		int duration;
		try {
			duration = Integer.parseInt(xmlVideo.getAt("group").getAt("duration")[0].attribute("seconds"));
		} catch (NumberFormatException e) {
			duration = 1337;
		}
		String desc = xmlVideo.getAt("group").getAt("description")[0].text();

		String nl = "<br />";
		render title + nl + thumbnail + nl + duration + nl + desc;
		


	}
}
