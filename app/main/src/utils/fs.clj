(ns utils.fs
  (:require
    [clojure.java.io :as io])
  (:import
    (java.io Writer)
    (java.nio.file Paths)
    (org.apache.tika Tika)))

(defn ->path [filepath]
  (Paths/get filepath (make-array String 0)))

(defn content-type [file]
  (.detect (Tika.) file))

(comment
  (content-type "sample.mp4")
  (content-type (io/file "sample.mp4")))

(defn write-lines [file-path lines]
  (with-open [wtr (io/writer file-path)]
    (doseq [line lines] (.write ^Writer wtr ^String line))))

(defn write-text [file-path text]
  (with-open [wtr (io/writer file-path)]
    (.write ^Writer wtr ^String text)))
