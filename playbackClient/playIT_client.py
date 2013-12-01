#!/usr/bin/python
""" The client controller for the playIT backend. """
import json
import urllib.request
import time
import os
import argparse


def main():
    playIT = PlayIt()
    print("Running main playback loop...")
    playIT.start()


class PlayIt(object):
    def __init__(self):
        self.server = ""
        self.showVideos = "playIT/media/popQueue"
        self.monitorNumber = 1

        print("Initializing...")
        parser = argparse.ArgumentParser()
        parser.add_argument('-mn', '--monitorNumber')
        parser.add_argument('-s', '--server')
        args = parser.parse_args()

        if(args.server is None):
            print("Please supply a server by: -s http://example.com")
            exit(1)
        else:
            self.server = self._fixServerAdress(args.server)
            print("Server: " + self.server)

        if args.monitorNumber is not None:
            self.monitor_number = args.monitorNumber

    def _fixServerAdress(self, rawServer):
        if(not rawServer.endswith("/")):
            rawServer = rawServer + "/"
        if(not rawServer.startswith("http://")):
            rawServer = "http://" + rawServer
        return rawServer

    def start(self):
        """ Start the event-loop. """
        while True:
            print("Popping next queue item")
            item = self._loadNext()
            if item is not None:
                if item[0] == "youtube":
                    print("Playing video with id: " + item[1])
                    self._playVideo(item[1])
                elif item[0] == "spotify":
                    print("Playing track with id: " + item[1])
                    # Implement spotify track handling
                    self.__playSpotifyTrack(item[1])
            else:
                print("No item in queue, sleeping...")
                time.sleep(10)

    def _loadNext(self):
        print(self.server + self.showVideos)
        url = urllib.request.urlopen(self.server + self.showVideos)
        raw_data = url.read().decode("utf8")

        if(raw_data == "[]"):
            return None
        else:
            data = json.loads(raw_data)

            return data.get("type"), data.get("externalID")

    def _playVideo(self, youtubeID):
        print("_playVideo: " + youtubeID)
        youtubeURL = "'http://www.youtube.com/watch?v=" + youtubeID + "'"
        cmd = 'mplayer -cache 4096 -fs -xineramascreen ' + str(self.monitorNumber) + ' "$(youtube-dl -g ' + youtubeURL + ')"'

        # https://github.com/mpv-player/mpv
        #cmd = 'mpv --fs --screen ' + str(MONITOR_NUMBER) + ' ' + youtubeURL
        print(cmd)
        os.system(cmd)

    def __playSpotifyTrack(self, spotifyID):
        print("__playSpotifyTrack: " + spotifyID)
        #mpc and mopidy required set up to work
        command = 'mpc add spotify:track:' + spotifyID + ' && mpc play'

        print(command)
        os.system(command)

        checkCmd = 'mpc current'
        while True:
            status = os.popen(checkCmd).read()
            if not status:  # If song stopped playing
                break
            else:
                #print(status) # Causes a lot of spam
                time.sleep(5)


if __name__ == "__main__":
    main()
