(ns video-stt.gcp-java.storage
  (:require
    [clojure.java.io :as io]
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig]
    [me.raynes.fs :as fs]
    [video-stt.gcp-java.cred :as gcp-cred])
  (:import
    (java.nio ByteBuffer)
    (java.nio.file Files Paths)
    (com.google.cloud.storage StorageOptions Storage BlobId BlobInfo Storage$BlobTargetOption Storage$BlobWriteOption)
    (com.google.auth.oauth2 GoogleCredentials)))

;; https://github.com/googleapis/java-storage
;; https://github.com/googleapis/nodejs-storage
;; https://github.com/googleapis/google-cloud-java/blob/master/google-cloud-examples/src/main/java/com/google/cloud/examples/storage/snippets/UpdateBlob.java

;; https://lethain.com/reading-file-in-clojure/


;; ---- SPEC --------------------

(spec/def ::storage (partial instance? Storage))

;; ---- BODY --------------------


(defn storageOptions [^GoogleCredentials cred]
  (as-> (StorageOptions/newBuilder) $
        (.setCredentials $ cred)
        (.build $)))

(defn storage [cred]
  (.getService (storageOptions cred)))


;; check the type (DI direct)
#_(->> (gcp-cred/credentials "stt-playground-310715-193109fadcd0-stt-processor-4.json")
       storage
       (instance? Storage)
       )
;; check the type (DI with Integrant)
#_(->> (do (require '[dev]) (dev/state))
       ::instance
       (instance? Storage)
       )
;; list all buckets
#_(->> (do (require '[dev]) (dev/state))
       ::instance
       (#(.list ^Storage % (make-array com.google.cloud.storage.Storage$BucketListOption 0)))
       .iterateAll
       (map #(.getName ^com.google.cloud.storage.Bucket %))
       )
;; list items at "rynkowski-tmp-audio" bucket (names)
#_(->> (do (require '[dev]) (dev/state))
       ::instance
       (#(.list ^Storage % "rynkowski-tmp-audio" (make-array com.google.cloud.storage.Storage$BlobListOption 0)))
       .iterateAll
       (map #(.getName ^com.google.cloud.storage.Blob %))
       )
;; list items at "rynkowski-tmp-audio" bucket (by BlobId)
#_(->> (do (require '[dev]) (dev/state))
       ::instance
       (#(.list ^Storage % "rynkowski-tmp-audio" (make-array com.google.cloud.storage.Storage$BlobListOption 0)))
       .iterateAll
       (map #(.getBlobId ^com.google.cloud.storage.Blob %))
       (map #(.toString ^BlobId %))
       )
;; preview README.md
#_(->> (do (require '[dev]) (dev/state))
       ::instance
       (#(.list ^Storage % "rynkowski-tmp-audio" (make-array com.google.cloud.storage.Storage$BlobListOption 0)))
       .iterateAll
       (filter #(= (.getName ^com.google.cloud.storage.Blob %) "README.md"))
       first
       (#(.getContent ^com.google.cloud.storage.Blob % (make-array com.google.cloud.storage.Blob$BlobSourceOption 0)))
       slurp
       )

(defn upload-file-small [^Storage s blobInfo filepath]
  (let [path (Paths/get filepath (make-array String 0))
        bytes (Files/readAllBytes path)]
    (.create s blobInfo bytes (make-array Storage$BlobTargetOption 0))))

(defn upload-file-large [^Storage s blobInfo filepath]
  (with-open [is     (io/input-stream filepath)
              writer (.writer s blobInfo (make-array Storage$BlobWriteOption 0))]
      (let [buffer   (byte-array (* 1024 10))
            size     (fs/size filepath)
            read-is #(.read is buffer)]
        (loop [bytes-read (read-is), sum 0]
          (when (> bytes-read 0)
            (.write writer (ByteBuffer/wrap buffer 0 bytes-read))
            (printf "read: %d, total-read: %d, progress: %.2f%%\n" bytes-read sum (-> (float sum) (/ size) (* 100)))
            (recur (read-is) (+ sum bytes-read)))))))

;; https://stackoverflow.com/a/55451089
;; https://github.com/googleapis/google-cloud-java/blob/master/google-cloud-examples/src/main/java/com/google/cloud/examples/storage/snippets/StorageSnippets.java
;; https://stackoverflow.com/a/30047468
(defn upload-file [s filepath bucket]
  (let [name (fs/name filepath)
        _ (println "exist? " (fs/exists? filepath))
        generation (* (fs/mod-time filepath) 1000)          ;; TODO: correct
        blobId (BlobId/of bucket name generation)
        blobInfo (-> (BlobInfo/newBuilder blobId)
                     (.setContentType "audio/flac")
                     (.build)
                     )
        size (fs/size filepath)]
    (if (< size 1000000)
      (upload-file-small s blobInfo filepath)
      (upload-file-large s blobInfo filepath))
    )
  )
;; send small file
#_(-> (gcp-cred/credentials "stt-playground-310715-193109fadcd0-stt-processor-4.json")
      storage
      (upload-file "README.md" "rynkowski-tmp-audio" )
      )
;; send large file
#_(-> (gcp-cred/credentials "stt-playground-310715-193109fadcd0-stt-processor-4.json")
      storage
      (upload-file "sample.mp4" "rynkowski-tmp-audio" )
      )

;; ---- DI ----------------------

(spec/def ::instance ::storage)
(spec/def ::cred ::gcp-cred/google-credentials)

(defmethod ig/pre-init-spec ::instance [_]
  (spec/keys :req-un [::cred]))

(defmethod ig/init-key ::instance [_ {:keys [cred]}]
  (storage cred))

; TODO: take GOOGLE_APPLICATION_CREDENTIALS if not provided
