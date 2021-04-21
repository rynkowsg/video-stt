(ns video-stt.audio-cli
  (:require
    [clojure.java.shell :refer [sh]]
    [me.raynes.fs :as fs]))

(defn extract-audio [{:keys [input output output-dir] :as params}]
  (let [output (cond
                 (some? output)  output
                 (some? output-dir) (str output-dir "/" (fs/name input) ".flac")
                 true (throw (ex-info "Output not specified" params)))]
    {:input input
     :output output
     :result (sh "avconv" "-i" input "-ac" "1" "-ab" "128k" "-vn" "-y" output)}
    ))
#_(extract-audio
    {:input "/Users/greg/Desktop/sample.mp4"
     :output-dir "/Users/greg/Desktop"})
