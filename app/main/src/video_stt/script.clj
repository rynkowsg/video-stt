(ns video-stt.script
  (:require
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig]
    [video-stt.gcp-java.speech :as gcp-speech]
    [video-stt.gcp-java.storage :as gcp-storage]))

;; ---- SPEC --------------------

(spec/def ::process-fn fn?)

;; ---- BODY --------------------=

(defn process
  [speech storage
   {:keys [video output] :as params}]
  (println "Processing..." params)
  {})

;; ---- DI ----------------------

(spec/def ::fn ::process-fn)
(spec/def ::speech ::gcp-speech/instance)
(spec/def ::storage ::gcp-storage/instance)

(defmethod ig/pre-init-spec ::fn [_]
  (spec/keys :req-un [::speech ::storage]))

(defmethod ig/init-key ::fn [_ {:keys [speech storage]}]
  (partial process speech storage))
