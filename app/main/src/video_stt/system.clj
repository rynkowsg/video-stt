(ns video-stt.system
  (:require
    [video-stt.gcp-java.cred :as gcp-cred]
    [video-stt.gcp-java.speech :as gcp-speech]
    [video-stt.gcp-java.storage :as gcp-storage]
    [integrant.core :as ig]))

(defn system-config []
  {::gcp-cred/instance    {:path "stt-playground-310715-193109fadcd0-stt-processor-4.json"}
   ::gcp-speech/instance {:cred (ig/ref ::gcp-cred/instance)}
   ::gcp-storage/instance {:cred (ig/ref ::gcp-cred/instance)}}
  )
