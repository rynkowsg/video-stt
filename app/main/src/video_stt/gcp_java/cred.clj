(ns video-stt.gcp-java.cred
  (:require
    [clojure.java.io :as io]
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig])
  (:import
    (com.google.auth.oauth2 GoogleCredentials)))

;; https://cloud.google.com/docs/authentication/production

;; ---- SPEC --------------------

(spec/def ::google-credentials (partial instance? GoogleCredentials))

;; ---- BODY --------------------=

(defn credentials [res]
  (-> res
      io/resource
      io/input-stream
      GoogleCredentials/fromStream))
#_(->> "stt-playground-310715-193109fadcd0-stt-processor-4.json" credentials (instance? GoogleCredentials))

;; ---- DI ----------------------

(spec/def ::instance ::google-credentials)

(defmethod ig/init-key ::instance [_ {:keys [path]}]
  (credentials path))

; TODO: take GOOGLE_APPLICATION_CREDENTIALS if not provided
