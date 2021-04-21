(ns dev
  (:require
    [clojure.tools.namespace.repl :as tn]
    [integrant.core :as ig]
    [integrant.repl :as ig-repl]
    [integrant.repl.state :as ig-state]
    [video-stt.system :as system]))

;; Tell tools.namespace which directories to refresh.
(clojure.tools.namespace.repl/set-refresh-dirs "app/dev/src" "app/main/src" "app/test/src")

(defn init-system [ig-config]
  (ig/load-namespaces ig-config)
  (ig-repl/set-prep! (constantly ig-config))
  (ig-repl/go))

(defn go []
  (init-system (system/system-config)))

(defn reset
  "Suspends system, reloads any files which have changed, resumes system"
  []
  ;(setup-repl-logging!)
  (ig-repl/reset))

(defn reset-all
  "Suspends system, reloads all clj files, resumes system"
  []
  (ig-repl/reset-all))

(defn refresh-all
  "Clojure tools.namespace refresh-all, use this to reload all namespaces when you don't have a system running."
  []
  (tn/refresh-all))

(defn halt
  "Stops system"
  []
  (ig-repl/halt))

(defn clear
  "Clears system state"
  []
  (ig-repl/clear))

(defn restart
  "Restart system"
  []
  (halt)
  (go))

(defn state []
  ig-state/system)
