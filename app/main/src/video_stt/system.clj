(ns video-stt.system
  (:require
    [video-stt.gcp-java.cred :as gcp-cred]
    [video-stt.gcp-java.speech :as gcp-speech]
    [video-stt.gcp-java.storage :as gcp-storage]
    [video-stt.script :as script]
    [integrant.core :as ig]))

(defn system-config [cred]
  {::gcp-cred/instance    {:path cred}
   ::gcp-speech/instance {:cred (ig/ref ::gcp-cred/instance)}
   ::gcp-storage/instance {:cred (ig/ref ::gcp-cred/instance)}
   ::script/fn {:speech (ig/ref ::gcp-speech/instance)
                :storage (ig/ref ::gcp-storage/instance)}})
