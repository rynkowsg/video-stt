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

(defn credentials [filepath]
  (-> filepath
      io/input-stream
      GoogleCredentials/fromStream))
#_(->> "stt-playground-310715-193109fadcd0-stt-processor-4.json" io/resource credentials (instance? GoogleCredentials))

;; ---- DI ----------------------

(spec/def ::instance ::google-credentials)

(defmethod ig/init-key ::instance [_ {:keys [path] :as c}]
  (let [path1 (if (nil? path) (System/getenv "GOOGLE_APPLICATION_CREDENTIALS") path)]
    (when (nil? path1) (throw (ex-info "Path can't be null" c)))
    (credentials path1)))
