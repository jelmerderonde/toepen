(ns toepen.server.logging
  "Logging config, JVM/app metrics, and a periodic metrics reporter.
   Used to diagnose OOMs by exposing heap, GC, game and connection counts."
  (:require [taoensso.timbre :as log]
            [toepen.server.state :as state]
            [toepen.server.ws :as ws])
  (:import (java.lang.management ManagementFactory
                                 GarbageCollectorMXBean)))

(defn- ^long bytes->mb [^long b]
  (quot b (* 1024 1024)))

(defn jvm-metrics
  "Snapshot of JVM heap / GC / thread state."
  []
  (let [mem-bean (ManagementFactory/getMemoryMXBean)
        heap (.getHeapMemoryUsage mem-bean)
        non-heap (.getNonHeapMemoryUsage mem-bean)
        threads (ManagementFactory/getThreadMXBean)
        gcs (ManagementFactory/getGarbageCollectorMXBeans)]
    {:heap-used-mb (bytes->mb (.getUsed heap))
     :heap-committed-mb (bytes->mb (.getCommitted heap))
     :heap-max-mb (bytes->mb (.getMax heap))
     :non-heap-used-mb (bytes->mb (.getUsed non-heap))
     :threads (.getThreadCount threads)
     :gc-count (reduce + (map #(.getCollectionCount ^GarbageCollectorMXBean %) gcs))
     :gc-time-ms (reduce + (map #(.getCollectionTime ^GarbageCollectorMXBean %) gcs))}))

(defn state-metrics
  "Snapshot of application-level state sizes."
  []
  (let [games @state/state
        connected @ws/connected]
    {:games (count games)
     :players-total (reduce + (map (fn [[_ g]] (count (:players g))) games))
     :connected-any (count (:any connected))
     :connected-ws (count (:ws connected))
     :connected-ajax (count (:ajax connected))}))

(defn configure!
  "Applies Timbre config. Min-level can be overridden with TOEPEN_LOG_LEVEL."
  []
  (let [level (keyword (or (System/getenv "TOEPEN_LOG_LEVEL") "info"))]
    (log/merge-config! {:min-level level})))

(defn wrap-request-logging
  "Ring middleware that emits one INFO log per request with method/uri/status/duration."
  [handler]
  (fn [request]
    (let [t0 (System/nanoTime)
          response (handler request)
          duration-ms (quot (- (System/nanoTime) t0) 1000000)]
      (log/info {:event :request
                 :method (:request-method request)
                 :uri (:uri request)
                 :status (:status response)
                 :duration-ms duration-ms
                 :remote-addr (:remote-addr request)})
      response)))

(defn start-metrics-reporter!
  "Starts a daemon thread that logs merged JVM + app metrics on an interval.
   Returns a no-arg stop fn."
  ([] (start-metrics-reporter! 30000))
  ([interval-ms]
   (let [stop? (volatile! false)
         t (Thread.
             ^Runnable
             (fn []
               (while (not @stop?)
                 (try
                   (log/info (merge {:event :metrics}
                                    (jvm-metrics)
                                    (state-metrics)))
                   (catch InterruptedException _
                     (vreset! stop? true))
                   (catch Throwable e
                     (log/warn e "metrics reporter iteration failed")))
                 (when-not @stop?
                   (try (Thread/sleep interval-ms)
                        (catch InterruptedException _
                          (vreset! stop? true))))))
             "toepen-metrics-reporter")]
     (.setDaemon t true)
     (.start t)
     (fn stop-reporter []
       (vreset! stop? true)
       (.interrupt t)))))
