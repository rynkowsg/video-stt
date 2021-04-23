(ns video-stt.script
  (:require
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig]
    [me.raynes.fs :as fs]
    [utils.fs :refer [write-lines]]
    [video-stt.gcp-java.speech :as gcp-speech]
    [video-stt.gcp-java.storage :as gcp-storage]
    [video-stt.audio-cli :as audio-cli]))

;; ---- SPEC --------------------

(spec/def ::process-fn fn?)

;; ---- BODY --------------------=

;; 1. take audio from video
;; 2. copy audio to Google Storage
;; 3. call Speech API
;; 4. write down the txt

(defn process [speech storage {:keys [video output]}]
  (println "Processing...")
  (let [
        tmp-dir  (fs/temp-dir "video-stt")
        _ (prn "+ tmp dir created:" tmp-dir)
        audio-res (audio-cli/extract-audio {:input video :output-dir (str tmp-dir)})
        _ (prn "+ audio-res:" audio-res)
        audio-file (:output audio-res)
        upload-res (gcp-storage/upload-file storage audio-file "rynkowski-tmp-audio")
        _ (prn "+ upload-res:" upload-res)
        speech-res (gcp-speech/long-to-text speech (gcp-storage/->uri upload-res))
        _ (prn "+ speech-res:" speech-res)
        _ (write-lines output speech-res)
        ]

    (gcp-storage/delete-blob (gcp-storage/->blob storage upload-res))
    _ (prn "+ GCP file removed:" (gcp-storage/->uri upload-res))
    (fs/delete audio-file)
    _ (prn "+ local file removed:" audio-file)
    (fs/delete-dir tmp-dir)
    _ (prn "+ tmp dir removed:" tmp-dir)))

(comment
  (do (require '[dev])
      (def f (-> (dev/state) (::fn)))
      f)
  (f {:video "/Users/greg/Desktop/sample.mp4"
      :output "/Users/greg/Desktop/sample.txt"})
  )

;; ---- DI ----------------------

(spec/def ::fn ::process-fn)
(spec/def ::speech ::gcp-speech/instance)
(spec/def ::storage ::gcp-storage/instance)

(defmethod ig/pre-init-spec ::fn [_]
  (spec/keys :req-un [::speech ::storage]))

(defmethod ig/init-key ::fn [_ {:keys [speech storage]}]
  (partial process speech storage))
