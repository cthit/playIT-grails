import 'package:web_ui/web_ui.dart';

const SERVER_URL = 'http://129.16.187.87:8080/youTubeInTheHubbServer/video/';

class VideoComponent extends WebComponent {
  VideoComponent() {
    @observable
    int votes = 0;
    @observable
    String imgurl = this.getimgurl(0);
    @observable
    String title = "";
  }

  String getimgurl(int videoNumber) {
    HttpRequest.getString(SERVER_URL + "showQueue").then((response) {
      List parsedList = parse(response);
      return parsedList[videoNumber]['url'];
    });
  }

  void upvote() {
    votes++;
  }
}