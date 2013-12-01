#!/usr/bin/python
""" The client controller for the playIT backend.

Requires python3.3
Depends on mpc(optional), mopidy(optional),
mplayer/youtube-dl and/or mpv

https://github.com/mpv-player/mpv

"""
import json
import urllib.request
import time
import os
import argparse
from shutil import which
import subprocess


def main():
    playIT = PlayIt()
    print("Running main playback loop...")
    playIT.start()


def checkReqs():
    failed = False
    if which("mopidy") is None:
        print("(optional) mopidy is missing")
    if which("mpc") is None:
        print("(optional) mpc is missing")

    mplayer = which("mplayer")
    mpv = which("mpv")

    if mpv is None or mplayer is None:
        print("mpv or mplayer missing")
        failed = True

    if mplayer is not None and which("youtube-dl") is None and mpv is None:
        print("Missing youtube-dl")
        failed = True

    if failed:
        print("Resolve the above missing requirements")
        exit(1)


def process_exists(proc_name):
    """ http://stackoverflow.com/a/7008599 ."""

    import re
    ps = subprocess.Popen("ps ax -o pid= -o args= ",
                          shell=True, stdout=subprocess.PIPE)
    ps_pid = ps.pid
    output = ps.stdout.read()
    ps.stdout.close()
    ps.wait()

    for line in output.decode().split("\n"):
        res = re.findall(r"(\d+) (.*)", line)
        if res:
            pid = int(res[0][0])
            if proc_name in res[0][1] and pid != os.getpid() and pid != ps_pid:
                return True
    return False


def _fixServerAdress(rawServer):
    if(not rawServer.endswith("/")):
        rawServer = rawServer + "/"
    if(not rawServer.startswith("http://")):
        rawServer = "http://" + rawServer
    return rawServer


class PlayIt(object):
    def __init__(self):
        checkReqs()
        if not process_exists("mopidy"):
            print("FYI: mopidy does not seem to be running")

        self.showVideos = "playIT/media/popQueue"

        print("Initializing...")
        parser = argparse.ArgumentParser()
        parser.add_argument('-m', '--monitor-number', dest="monitorNumber",
                            type=int, default=1)
        parser.add_argument('-s', '--server')
        args = parser.parse_args()

        if(args.server is None):
            print("Please supply a server by: -s http://example.com")
            exit(1)
        else:
            self.server = _fixServerAdress(args.server)
            print("Server: " + self.server)

        self.monitorNumber = args.monitorNumber

    def start(self):
        """ Start the event-loop. """
        if which("mpc") is not None:
            os.system("mpc single on >/dev/null && mpc consume on >/dev/null")
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

        if which("mpv") is not None:
            cmd = ['mpv', "--quiet", '--fs', '--screen',
                   str(self.monitorNumber), youtubeURL]
        else:
            cmd = ['mplayer', '-cache', '4096', '-fs',
                   '-xineramascreen', str(self.monitorNumber),
                   '"$(youtube-dl -g ' + youtubeURL + ')"']

        print(cmd)
        os.system(" ".join(cmd))

    def __playSpotifyTrack(self, spotifyID):
        print("__playSpotifyTrack: " + spotifyID)
        #mpc and mopidy required set up to work
        command = 'mpc add spotify:track:' + spotifyID + ' && mpc play'

        #print(command)
        os.system(command)

        checkCmd = 'mpc current'
        while os.popen(checkCmd).read():
            try:
                # Blocks until some event happens to mpd
                subprocess.call(["mpc", "idle"], stdout=subprocess.DEVNULL)
            except KeyboardInterrupt:
                # on ctrl-c, stop playback
                os.system("mpc stop >/dev/null")
                os.system("mpc del 1")


if __name__ == "__main__":
    main()
