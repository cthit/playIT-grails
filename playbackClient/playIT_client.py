#!/usr/bin/python
""" The client controller for the playIT backend
by Horv and Eda - 2013, 2014

Requires Python >= 3.3
Depends on:
    1. mpc and mopidy for Spotify and Soundcloud playback.
    2. mpv for video/YouTube playback. http://mpv.io/

"""
import json
import urllib.request
import time
import argparse
import sys
from shutil import which
import subprocess
from subprocess import call


def main():
    """ Init and startup goes here... """
    check_reqs()

    playit = PlayIt()
    print("Running main playback loop...")
    playit.start()


def check_reqs():
    """ Verify that all dependencies exists. """
    depends = ["mopidy", "mpc", "mpv"]
    failed = False

    for dep in depends:
        if which(dep) is None:
            print("Requirement", dep, "is missing (from PATH at least...)", file=sys.stderr)
            failed = True

    if failed:
        print("Resolve the above missing requirements", file=sys.stderr)
        exit(1)
    else:
        if not process_exists("mopidy"):
            print("mopidy does not seem to be running. Please launch it beforehand :)", file=sys.stderr)
            exit(2)


def process_exists(proc_name):
    """ http://stackoverflow.com/a/7008599 ."""

    import re
    ps = subprocess.Popen("ps ax -o pid= -o args= ",
                          shell=True, stdout=subprocess.PIPE)
    ps_pid = ps.pid
    output = ps.stdout.read()
    ps.stdout.close()
    ps.wait()

    from os import getpid
    for line in output.decode().split("\n"):
        res = re.findall(r"(\d+) (.*)", line)
        if res:
            pid = int(res[0][0])
            if proc_name in res[0][1] and pid != getpid() and pid != ps_pid:
                return True
    return False


def _fix_server_adress(raw_server):
    """ Prepend http:// and append / if they're not there. """
    #Seems to be bugging, try to do better check?
    if not raw_server.endswith("/"):
        raw_server += "/"
    if not raw_server.startswith("http://"):
        raw_server = "http://" + raw_server
    return raw_server


class PlayIt(object):
    """ Defines the interface between the backend and actual playback. """
    def __init__(self):
        self.show_videos = "playIT/media/popQueue"

        parser = argparse.ArgumentParser()
        parser.add_argument('-m', '--monitor-number', dest="monitor_number",
                            type=int, default=1)
        parser.add_argument('-s', '--server')
        args = parser.parse_args()

        if(args.server is None):
            print("Please supply a server by: -s http://example.com")
            exit(1)
        else:
            self.server = _fix_server_adress(args.server)
            print("Server: " + self.server)

        self.monitor_number = args.monitor_number

    def start(self):
        """ Start the event-loop. """
        if which("mpc") is not None:
            call("mpc single on &>/dev/null && mpc consume on &>/dev/null",
                 shell=True)
        while True:
            print("Popping next queue item")
            item = self._load_next()
            if item is not None:
                if item[0] == "youtube":
                    print("Playing video with id: " + item[1])
                    self._play_video(item[1])
                elif item[0] == "spotify":
                    print("Playing track with id: " + item[1])
                    self._play_spotify_track(item[1])
            else:
                print("No item in queue, sleeping...")
                # TODO: use websockets to notify of new queue item
                time.sleep(10)

    def _load_next(self):
        """ Get the next item in queue from the backend. """
        try:
            url = urllib.request.urlopen(self.server + self.show_videos)
        except (urllib.error.HTTPError, urllib.error.URLError) as err:
            print("Error while fetching queue: ", err)
            return None
        raw_data = url.read().decode("utf8")

        if raw_data == "[]":
            return None
        else:
            data = json.loads(raw_data)
            return data.get("type"), data.get("externalID")

    def _play_video(self, youtube_id):
        """ Play the supplied youtube video with mpv or mplayer. """
        print("[_play_video] Video id: " + youtube_id)
        youtube_url = "http://www.youtube.com/watch?v=" + youtube_id

        if which("mpv") is not None:
            cmd = ['mpv', "--quiet", '--fs', '--screen',
                   str(self.monitor_number), youtube_url]
        else:
            cmd = ['mplayer', '-cache', '4096', '-fs',
                   '-xineramascreen', str(self.monitor_number),
                   '"$(youtube-dl -g ' + youtube_url + ')"']

        print(cmd)
        call(cmd)

    def _play_spotify_track(self, spotify_id):
        """ Play the supplied spotify track using mopidy and mpc. """
        if not process_exists("mopidy"):
            print("[spotify] Start mopidy...")
            return

        print("__playSpotifyTrack: " + spotify_id)
        #mpc and mopidy required set up to work
        cmd = 'mpc add spotify:track:' + spotify_id + ' && mpc play'

        #print(command)
        call(cmd, shell=True)

        current_cmd = ['mpc', 'current']
        while subprocess.check_output(current_cmd):
            try:
                # Blocks until some event happens to mpd
                call(["mpc", "idle"], stdout=subprocess.DEVNULL)
            except KeyboardInterrupt:
                # on ctrl-c, stop playback
                call("mpc stop >/dev/null", shell=True)
                call("mpc del 1", shell=True)


if __name__ == "__main__":
    main()
