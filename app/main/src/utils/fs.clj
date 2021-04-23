(ns utils.fs
  (:require
    [clojure.java.io :as io])
  (:import
    (java.nio.file Paths)
    (org.apache.tika Tika)))

(defn ->path [filepath]
  (Paths/get filepath (make-array String 0)))

(defn content-type [file]
  (.detect (Tika.) file))

(comment
  (content-type "sample.mp4")
  (content-type (io/file "sample.mp4")))
