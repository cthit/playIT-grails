#!/usr/bin/python
import json
import urllib.request
import time
import os
import sys
import argparse

SERVER = ""
SHOW_VIDEOS = "youTubeInTheHubbServer/video/popQueue"
MONITOR_NUMBER = 1


def main():
    _init()
    _mainLoop()


def _init():
    parser = argparse.ArgumentParser()
    parser.add_argument('-mn', '--monitorNumber')
    parser.add_argument('-s', '--server')
    args = parser.parse_args()

    if(args.server is None):
        print("Please supply a server by: -s http://example.com")
        exit(1)
    else:
        global SERVER
        SERVER = _fixServerAdress(args.server)

    if args.monitorNumber is not None:
        global MONITOR_NUMBER
        MONITOR_NUMBER = args.monitorNumber


def _fixServerAdress(rawServer):
    if(not rawServer.endswith("/")):
        rawServer = rawServer + "/"
    if(not rawServer.startswith("http://")):
        rawServer = "http://" + rawServer
    return rawServer


def _mainLoop():

    while(True):
        youtubeID = _loadYTID()
        if youtubeID is not None:
            print("Playing video with id: " + youtubeID)
            _playVideo(youtubeID)
        else:
            print("No item in queue, sleeping...")
            time.sleep(10)


def _loadYTID():
    url = urllib.request.urlopen(SERVER + SHOW_VIDEOS)
    raw_data = url.read().decode("utf8")

    if(raw_data == "[]"):
        return None
    else:
        data = json.loads(raw_data)
        return data.get("youtubeID")


def _playVideo(youtubeID):
    print("_playVideo: " + youtubeID)
    youtubeURL = "'http://www.youtube.com/watch?v=" + youtubeID + "'"
    command = 'mplayer -cache 4096 -fs -xineramascreen '+str(MONITOR_NUMBER)+' "$(youtube-dl -g '+youtubeURL+')"'
    print(command)
    os.system(command)

if __name__ == "__main__":
    main()
