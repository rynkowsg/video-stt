(ns video-stt.gcp-java.speech
  (:require
    [clojure.string :as str]
    [clojure.core.async :refer [go-loop <! timeout]]
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig]
    [video-stt.gcp-java.cred :as gcp-cred])
  (:import
    (com.google.api.gax.core FixedCredentialsProvider)
    (com.google.cloud.speech.v1 SpeechSettings SpeechClient RecognitionConfig
                                RecognitionConfig$AudioEncoding RecognitionAudio LongRunningRecognizeResponse)))

;; ---- SPEC --------------------

(spec/def ::speech (partial instance? SpeechClient))

;; ---- BODY --------------------=

(defn speech-api [cred]
  (let [cp       (FixedCredentialsProvider/create cred)
        settings (as-> (SpeechSettings/newBuilder) $
                       (.setCredentialsProvider $ cp)
                       (.build $))]
    (SpeechClient/create ^SpeechSettings settings)))

(def recognition-config
  (-> (RecognitionConfig/newBuilder)
      (.setEncoding RecognitionConfig$AudioEncoding/FLAC)
      (.setSampleRateHertz 48000)
      (.setLanguageCode "pl-PL")
      (.build)))

(defn recognition-audio [audio-uri]
  (-> (RecognitionAudio/newBuilder)
      (.setUri audio-uri)
      (.build)))

(defn to-text [speech audio-uri]
  (->> (.recognize speech recognition-config (recognition-audio audio-uri))
       .getResultsList
       (map #(-> % .getAlternativesList (.get 0) .getTranscript))
       ;(str/join "\n")
       )
)
;; call to-text for 30 sec audio
#_(-> (do (require '[dev]) (dev/state))
       ::instance
       (to-text "gs://rynkowski-tmp-audio/sample_30sec_audio.flac")
       )

(defn wait-until-then [until-fn then-fn]
  (go-loop [seconds 1]
    (<! (timeout 1000))
    (println "waited" seconds "seconds")
    (if (until-fn)
      (then-fn)
      (recur (inc seconds)))
    ))

(defn long-to-text [speech audio-uri]
  (let [fut (.longRunningRecognizeAsync speech recognition-config (recognition-audio audio-uri))
        res (promise)]
    (wait-until-then #(.isDone fut) #(deliver res (.get fut)))
    (->> @res
         (#(.getResultsList ^LongRunningRecognizeResponse %))
         (map #(-> % .getAlternativesList (.get 0) .getTranscript))
         (str/join "\n"))))

;; call to-text for 30 sec audio
#_(-> (do (require '[dev]) (dev/state))
      ::instance
      (long-to-text "gs://rynkowski-tmp-audio/sample_30sec_audio.flac")
      )

;; ---- DI ----------------------

(spec/def ::instance ::speech)
(spec/def ::cred ::gcp-cred/google-credentials)

(defmethod ig/pre-init-spec ::instance [_]
  (spec/keys :req-un [::cred]))

(defmethod ig/init-key ::instance [_ {:keys [cred]}]
  (speech-api cred))

;; ---- DOCS --------------------

;; https://github.com/googleapis/java-speech
;; https://github.com/googleapis/nodejs-speech

;; https://github.com/GoogleCloudPlatform/java-docs-samples/blob/abef7ff95dc742d3c5d575dc6bd0ea14e6077f2b/speech/cloud-client/src/main/java/com/example/speech/Recognize.java - old example
;; https://github.com/GoogleCloudPlatform/java-docs-samples/issues/819 - example of taking Metadata info
;; https://github.com/googleapis/java-speech/tree/master/samples - new examples
