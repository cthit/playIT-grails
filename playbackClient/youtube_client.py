#!/usr/bin/python
import json
import urllib.request
import time
import os
import argparse

SERVER = ""
SHOW_VIDEOS = "playIT/media/popQueue"
MONITOR_NUMBER = 1


def main():
    print("Initializing...")
    _init()
    print("Running main playback loop...")
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
        print("Server: "+SERVER)

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

    while True:
        print("Popping next queue item")
        item = _loadNext()
        if item is not None:
            if item[0] == "youtube":
                print("Playing video with id: " + item[1])
                _playVideo(item[1])
            elif item[0] == "spotify":
                print("Playing track with id: "+item[1])
                # Implement spotify track handling
        else:
            print("No item in queue, sleeping...")
            time.sleep(10)


def _loadNext():
    print(SERVER +SHOW_VIDEOS)
    url = urllib.request.urlopen(SERVER + SHOW_VIDEOS)
    raw_data = url.read().decode("utf8")

    if(raw_data == "[]"):
        return None
    else:
        data = json.loads(raw_data)

        return (data.get("type") , data.get("externalID") )


def _playVideo(youtubeID):
    print("_playVideo: " + youtubeID)
    youtubeURL = "'http://www.youtube.com/watch?v=" + youtubeID + "'"
    command = 'mplayer -cache 4096 -fs -xineramascreen '+str(MONITOR_NUMBER)+' "$(youtube-dl -g '+youtubeURL+')"'

    # https://github.com/mpv-player/mpv
    #command = 'mpv --fs --screen ' + str(MONITOR_NUMBER) + ' ' + youtubeURL
    print(command)
    os.system(command)

if __name__ == "__main__":
    main()
