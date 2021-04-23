(ns video-stt.main
  (:gen-class)
  (:require
    [clojure.spec.alpha :as spec]
    [integrant.core :as ig]
    [plumbing.core :refer [map-vals]]
    [video-stt.script :as script]
    [video-stt.system :as system]))

; https://clojure.org/guides/spec#_sequences
; https://www.pixelated-noise.com/blog/2020/09/10/what-spec-is/

(spec/def ::option (spec/alt :credentials (spec/cat :prop #{"-c" "--credentials"} :value string?)
                             :input (spec/cat :prop #{"-i" "--input"} :value string?)
                             :output (spec/cat :prop #{"-o" "--output"} :value string?)))
(spec/def ::args (spec/* ::option))
#_(spec/conform ::args ["-i" "dfdf" "--output" "patshsdfdsf"])

(defn handle-arg-parsing [args]
  (let [conformed-args (spec/conform ::args args)]
    (if (= ::spec/invalid conformed-args)
      (do (println "Bad commandline arguments")
          (spec/explain ::args args)
          nil)
      (map-vals :value conformed-args))))
#_(handle-arg-parsing  ["-i" "gs://tmp/sample_30sec_audio.flac" "-o" "sample.txt"])

(defn init-system [cred]
  (let [config (system/system-config cred)]
    (ig/load-namespaces config)
    (ig/init config [::script/fn])))

(defn halt-system [system]
  (ig/halt! system))

(defn -main [& args]
  (let [{:keys [credentials input output]} (handle-arg-parsing args)
        system (init-system credentials)
        process-fn (::script/fn system)]
    (try
      (process-fn {:video input :output output})
      (catch Exception e
        (println "EXCEPTION:" e)
        (halt-system system)))))
