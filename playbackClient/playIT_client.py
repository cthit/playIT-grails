#!/usr/bin/python
""" The client controller for the playIT backend
by Horv and Eda - 2013, 2014

To add a new type of playback. Add a function called _play_TYPE(media_item)
and define how it's handled. It will be called automatically based on the
type parameter specified in the downloaded json

Requires Python >= 3.3
Depends on:
    1. mpc and mopidy for Spotify and Soundcloud playback.
            Note that you'll need both the spotify and soundcloud plugins
            Eg. aurget -S mopidy mopidy-spotify mopidy-soundcloud
    2. mpv for video/YouTube playback. http://mpv.io/
    3. requests (python library) for popping and checking server status.
            apt-get install python-requests    OR
            pacman -S python-requests          OR
            pip install requests


"""
import requests
import time
import argparse
import sys
from shutil import which
import subprocess
from subprocess import call

# Some settings and constants
POP_PATH = "/playIT/media/popQueue"
# Use verbose output
VERBOSE = False


def main():
    """ Init and startup goes here... """
    check_reqs()

    playit = PlayIt()
    vprint("Running main playback loop...")
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
    """ Prepend http://  there. """
    if not raw_server.startswith("http://"):
        raw_server = "http://" + raw_server
    return raw_server


def check_connection(url):
    """ Checks the connection to the backend """
    resp = requests.head(url + POP_PATH)

    if resp.status_code != 200:
        print("Unable to find backend at:", url,
              file=sys.stderr)
        exit(4)


def vprint(msg):
    """ Verbose print """
    if VERBOSE:
        print(msg)


class PlayIt(object):
    """ Defines the interface between the backend and actual playback. """
    def __init__(self):
        parser = argparse.ArgumentParser()
        parser.add_argument('-m', '--monitor-number', dest="monitor_number",
                            type=int, default=1)
        parser.add_argument('-s', '--server')
        #parser.add_argument('-v', '--verbose', action='store_true')
        args = parser.parse_args()

        if(args.server is None):
            print("Please supply a server by: -s http://www.example.org:port",
                  file=sys.stderr)
            exit(3)
        else:
            self.server = _fix_server_adress(args.server)
            vprint("Server: " + self.server)
            check_connection(self.server)

        self.monitor_number = args.monitor_number
        #verbose = args.verbose

    def start(self):
        """ Start the event-loop. """
        while True:
            item = self._get_next()
            if len(item) > 0:
                # Dynamically call the play function based on the media type
                func_name = "_play_" + item['type'].lower()
                func = getattr(self, func_name)
                func(item)
            else:
                vprint("No item in queue, sleeping...")
                time.sleep(7)

    def _get_next(self):
        """ Get the next item in queue from the backend. """
        vprint("Popping next item in the queue")
        resp = requests.get(self.server + POP_PATH)
        return resp.json()

    def _play_youtube(self, item):
        """ Play the supplied youtube video with mpv. """
        print("Playing youtube video: " + item['title'], "requested by", item['nick'])
        youtube_url = "https://youtu.be/" + item['externalID']

        cmd = ['mpv', '--fs', '--screen',
               str(self.monitor_number), youtube_url]
        call(cmd, stderr=subprocess.DEVNULL, stdout=subprocess.DEVNULL)

    def _play_spotify(self, item):
        """ Play the supplied spotify track using mopidy and mpc. """
        print("Playing", item['artist'], "-",
              item['title'], "requested by", item['nick'])
        self._add_to_mopidy('spotify:track:' + item['externalID'])

    def _play_soundcloud(self, item):
        """ Play SoundCloud items """
        print("Playing", item['artist'], "-",
              item['title'], "requested by", item['nick'])
        self._add_to_mopidy('soundcloud:song.' + item['externalID'])

    def _add_to_mopidy(self, track_id):
        """ Play a mopidy compatible track """
        call("mpc single on &>/dev/null && mpc consume on &>/dev/null",
             shell=True)
        cmd = 'mpc add ' + track_id
        cmd += ' && mpc play >/dev/null'

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
