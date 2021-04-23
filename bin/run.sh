#!/usr/bin/env bash

# Example:
#
#     ./bin/run.sh -c credentials.json -i video.mp4 -o transcription.txt
#
# or
#
#     GOOGLE_APPLICATION_CREDENTIALS=credentials.json ./bin/run.sh -i video.mp4 -o transcription.txt
#
#
# Notice:
# The script has to be executed from the root directory.

clojure -M:run-m "$@"
