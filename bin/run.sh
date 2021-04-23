#!/usr/bin/env bash

# Example:
#
#     ./run.sh -c credentials.json -i video.mp4 -o transcription.txt
#
# or
#
#     GOOGLE_APPLICATION_CREDENTIALS=credentials.json ./run.sh -i video.mp4 -o transcription.txt
#

# discover project root
SCRIPT_PATH="$(cd "$(dirname "$0")"; pwd -P)"
ROOT_DIR=$(cd "$SCRIPT_PATH/.."; pwd)

(
  # always run cmd from project root
  # (that way the script can be run from any directory)
  cd "${ROOT_DIR}";
  clojure -M:run-m "$@";
)
