import 'dart:html';
import 'dart:json';
import 'package:web_ui/web_ui.dart';
import 'dart:async';

const SERVER_URL = 'http://129.16.177.148:8080/youTubeInTheHubbServer/video/';

@observable
String title = "";
@observable
String imgurl = "Lolbolol";


void main() {
  query("#add_button")
    ..onClick.listen(addVideo);
  new Timer.periodic(new Duration(seconds:1), (Timer t) =>
      showVideos());
}

void addVideo(MouseEvent event) {
  var videoURL = query("#url_field").value;
  HttpRequest.getString(SERVER_URL + 'addVideo?cid=' + 'wildahl' +
      '&url=' + videoURL).then((response) {
    print(response);
  });
  showVideos();
}

void showVideos() {
  HttpRequest.getString(SERVER_URL + "showQueue").then((response) {
    List parsedList = parse(response);
    //fore(parsedList[0]);
    int t=parsedList[0].length;
    print("$t\n");
    //print(parsedList[0].length+"\n");
    parsedList[0].forEach((key,val) => print("$key  $val"));
    //print(parsedList[0]["title"]);

    //print(parsedList[0].get("title"));
 /*   for(var a in parsedList[0]){
      print(" | "+a);
    }*/
  });
}
