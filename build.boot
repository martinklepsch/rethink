(set-env! :resource-paths #{"src"}
          :dependencies '[[org.clojure/clojure "1.7.0"]
                          [com.rethinkdb/rethinkdb-driver "2.2-beta-5"]
                          [clj-time "0.11.0"]])

(task-options!
 pom {:project 'rethinkdb
      :version "0.13.4-SNAPSHOT"
      :description "RethinkDB client"
      :url "http://github.com/apa512/clj-rethinkdb"
      :license {"EPL" "http://www.eclipse.org/legal/epl-v10.html"}})