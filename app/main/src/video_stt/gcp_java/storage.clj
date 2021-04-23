(ns video-stt.gcp-java.storage
  (:require
    [clojure.java.io :as io]
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig]
    [me.raynes.fs :as fs]
    [utils.regex :refer [named-groups]]
    [utils.fs :refer [->path content-type]]
    [video-stt.gcp-java.cred :as gcp-cred])
  (:import
    (java.nio ByteBuffer)
    (java.nio.file Files)
    (com.google.cloud.storage
      Blob Blob$BlobSourceOption BlobId BlobInfo Bucket Bucket$BucketSourceOption BucketInfo
      Storage Storage$BlobListOption Storage$BlobTargetOption Storage$BlobWriteOption
      Storage$BucketListOption Storage$BucketTargetOption Storage$BucketGetOption
      StorageClass StorageOptions BlobInfo$Builder StorageOptions$Builder)
    (com.google.auth.oauth2 GoogleCredentials)))

;; https://github.com/googleapis/java-storage
;; https://github.com/googleapis/nodejs-storage
;; https://github.com/googleapis/google-cloud-java/blob/master/google-cloud-examples/src/main/java/com/google/cloud/examples/storage/snippets/UpdateBlob.java

;; ---- SPEC --------------------

(spec/def ::storage (partial instance? Storage))

;; ---- BODY --------------------

(defn storageOptions [^GoogleCredentials cred]
  (as-> (StorageOptions/newBuilder) $
        (.setCredentials ^StorageOptions$Builder $ cred)
        (.build ^StorageOptions$Builder $)))

(defn storage [cred]
  (.getService (storageOptions cred)))

(defn ->bucket [s name]
  (.get s name (make-array Storage$BucketGetOption 0)))

(defn ->blob [s bucket path & generation]
  (let [blob-id (if (some? generation)
                  (BlobId/of bucket path generation)
                  (BlobId/of bucket path))]
    (.get s ^BlobId blob-id)))

(defn ->node
  ([^Storage s ^Object {:keys [bucket path] :as params}]
   (cond
     (and bucket path) (->blob s bucket path)
     (and bucket) (->bucket s bucket)
     true s)))

(defn- parse-gs-uri [uri]
  (let [r #"(?<schema>[a-zA-Z]+)://((?<bucket>[a-zA-Z-]+)(/(?<path>[a-zA-Z/-\\.]*))?)?"
        {:keys [schema] :as res} (named-groups r uri)]
    ;; TODO: replace by some 3rd party library
    (when (not= schema "gs")
      (throw (ex-info "Wrong schema:" schema)))
    res))

(defn uri->node
  [^Storage s uri]
  (->node s (parse-gs-uri uri)))

(defn- bucket-info
  [{:keys [name storage-class location] :as params}]
  (prn params)
  (-> (BucketInfo/newBuilder name)
      (.setStorageClass (StorageClass/valueOfStrict storage-class))
      (.setLocation location)
      (.build)))

;; https://cloud.google.com/storage/docs/creating-buckets#storage-create-bucket-code_samples
(defn create-bucket
  [^Storage s name & params]
  (let [params (merge {:name name :storage-class "STANDARD" :location "EUROPE-WEST2"} params)
        bucket (.create s (bucket-info params) (make-array Storage$BucketTargetOption 0))]
    {:name (.getName bucket)}))

;; https://cloud.google.com/storage/docs/deleting-buckets#storage-delete-bucket-java
(defn delete-bucket [^Storage s name]
  (when-let [bucket (->bucket s name)]
    (when (.delete bucket (make-array Bucket$BucketSourceOption 0))
      :deleted)))

(defn list-buckets [^Storage s]
  (->> (.list s (make-array Storage$BucketListOption 0))
       (.iterateAll)
       (map #(.getName ^Bucket %))))

(defn list-items [^Storage s bucket]
  (->> (.list s bucket (make-array Storage$BlobListOption 0))
       (.iterateAll)
       (map #(.getName ^Blob %))))

(defn list-blobs [^Storage s bucket]
  (->> (.list s bucket (make-array Storage$BlobListOption 0))
       (.iterateAll)
       (map #(.getBlobId ^Blob %))
       (map (fn [b] {:name (.getName b)
                     :bucket (.getBucket b)
                     :generation (.getGeneration b)}))
       (into [])))

(defn content
  [^Storage s bucket name]
  (let [b (->blob s bucket name)]
    (.getContent ^Blob b (make-array Blob$BlobSourceOption 0))))

(defn upload-file-small [^Storage s blobInfo filepath]
  (let [path (->path filepath)
        bytes (Files/readAllBytes path)]
    (.create s blobInfo bytes (make-array Storage$BlobTargetOption 0))))

(defn upload-file-large [^Storage s blobInfo filepath]
  (with-open [is     (io/input-stream filepath)
              writer (.writer s blobInfo (make-array Storage$BlobWriteOption 0))]
      (let [buffer  (byte-array (* 1024 1000))
            size    (fs/size filepath)
            read-is #(.read is buffer)]
        (loop [bytes-read (read-is), sum 0]
          (when (> bytes-read 0)
            (.write writer (ByteBuffer/wrap buffer 0 bytes-read))
            (printf "read: %d, total-read: %d, progress: %.2f%%\n" bytes-read sum (-> (float sum) (/ size) (* 100)))
            (recur (read-is) (+ sum bytes-read))))
        blobInfo)))

;; https://stackoverflow.com/a/55451089
;; https://github.com/googleapis/google-cloud-java/blob/master/google-cloud-examples/src/main/java/com/google/cloud/examples/storage/snippets/StorageSnippets.java
;; https://stackoverflow.com/a/30047468
(defn upload-file [s filepath bucket]
  (let [file (io/file filepath)
        name (.getName file)
        generation (* (fs/mod-time filepath) 1000)          ;; TODO: correct
        size (fs/size filepath)
        blobId (BlobId/of bucket name generation)
        blobInfo (as-> (BlobInfo/newBuilder blobId) $
                       (.setContentType ^BlobInfo$Builder $ (content-type file))
                       (.build $)
                     )]
    (if (< size 1000000)
      (upload-file-small s blobInfo filepath)
      (upload-file-large s blobInfo filepath))))

;; ---- DI ----------------------

(spec/def ::instance ::storage)
(spec/def ::cred ::gcp-cred/google-credentials)

(defmethod ig/pre-init-spec ::instance [_]
  (spec/keys :req-un [::cred]))

(defmethod ig/init-key ::instance [_ {:keys [cred]}]
  (storage cred))

;; ---- EXAMPLE -----------------

(comment
  (do (require '[dev])
      (def s (-> (dev/state) (::instance)))
      s)

  (uri->node s "gs://")
  (uri->node s "gs://rynkowski-tmp-audio")
  (uri->node s "gs://rynkowski-tmp-audio/sample.mp4")
  (uri->node s "gs://rynkowski-tmp-audio/temp/sample.flac")

  (create-bucket s "greg-test123456789")
  (delete-bucket s "greg-test123456789")
  (list-buckets s)
  (list-items s "rynkowski-tmp-audio")
  (list-blobs s "rynkowski-tmp-audio")

  ;; preview README.md
  (slurp (content s "rynkowski-tmp-audio" "README.md"))

  ;; upload small file
  (upload-file s "README.md" "rynkowski-tmp-audio")

  ;; upload large file
  (upload-file s "sample.mp4" "rynkowski-tmp-audio"))
