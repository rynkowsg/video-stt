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
    {:input "/Users/greg/OneDrive/Content/Courses/Inteligentny Inwestor (Edycja 2021)/3 Metale szlachetne/07_Jak_inwestowa_w_z_oto.mp4"
     :output-dir "/Users/greg/OneDrive/Content/Courses/Inteligentny Inwestor (Edycja 2021)/3 Metale szlachetne/test/tmp"})
