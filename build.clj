(ns build
  (:require [clojure.tools.build.api :as b]))

;; Set headless property as early as possible
(System/setProperty "java.awt.headless" "true")

(def lib 'bt-controller)
(def version (format "1.2.%s" (b/git-count-revs nil)))
(def class-dir "target/classes")
(def uber-file (format "target/%s-%s-standalone.jar" (name lib) version))

;; Add headless property to JVM options for basis creation
(def basis (delay (b/create-basis {:project "deps.edn"
                                  :java-opts ["-Djava.awt.headless=true"]})))

(defn clean [_]
  (b/delete {:path "target"}))

(defn uber [_]
  (clean nil)
  (b/copy-dir {:src-dirs ["src" "resources"]
               :target-dir class-dir})
  (b/compile-clj {:basis @basis
                  :src-dirs ["src"]
                  :class-dir class-dir
                  :jvm-opts ["-Djava.awt.headless=true"]
                  :ns-compile '[bt-controller.core]})
  (b/uber {:class-dir class-dir
           :uber-file uber-file
           :basis @basis
           :main 'bt-controller.core
           :main-opts ["-Djava.awt.headless=true"]}))