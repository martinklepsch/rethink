(ns rethink
  (:require [clojure.walk :as walk])
  (:import [com.rethinkdb RethinkDB]))

(def r com.rethinkdb.RethinkDB/r)

(defn connect [opts]
  (-> r .connection .connect))

(def conn (connect {}))

(defn use-db [conn table]
  (.use conn table))

(comment 
  (def db "driver_tests")
  (def tbl "cities")

  (.close conn)

  (use-db conn db)

  ;; DATABASES ============================
  ;; List dbs
  (-> (.dbList r) (.run conn))

  ;; Create a db
  (-> (.dbCreate r db) (.run conn))

  ;; Drop a db
  (-> (.dbDrop r db) (.run conn))

  ;; TABLES =================================
  ;; List tables
  (-> r (.db db) (.tableList) (.run conn))

  ;; Create a table
  (-> r (.db db) (.tableCreate tbl) (.run conn))

  ;; Drop a table
  (-> r (.db db) (.tableDrop tbl) (.run conn))

  ;; RETURN VALUE HANDLING =======================================================================

  (defn transform-keys
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (string? k) [(keyword k) v] [k v]))]
    (walk/postwalk (fn [x]
                     (cond
                       (map? x) (into {} (map f x))
                       (vector? x) (do (prn (class x)) (mapv transform-keys x))
                       (list? x) (map transform-keys x)
                       :else x)) m)))

  (def tbl-drop-resp (-> r (.db db) (.tableDrop tbl) (.run conn)))

  (clojure.repl/source walk/keywordize-keys)

  (transform-keys tbl-drop-resp)
  (walk/keywordize-keys tbl-drop-resp)

  (walk/keywordize-keys (into {} tbl-drop-resp))
  (transform-keys (into {} tbl-drop-resp))
  (walk/postwalk-demo (into {} tbl-drop-resp))

  (walk/postwalk (fn [x] (if (number? x) (inc x) x)) [1 2 3])

  ;; --- TODO make postwalking with keywordization of maps within vectors work

  ;; INSERTION ====================================================================================

  ;; --- TODO investigate transformation of ReQL AST before/after passing to `run`
  ;; This is handy in many places, when reading data but also when inserting, updating, filtering
  ;; Instead of extending all the functions receiving Hashmaps etc using `run` as the extension
  ;; point would also make things significantly simpler and reduce surface area
  ;; Hook into this would help: https://github.com/rethinkdb/rethinkdb/blob/next/drivers/java/src/main/java/com/rethinkdb/ast/Util.java#L45

  ;; --- TODO result of `insert` command may contain `first_error` field if primary key is already used
  ;; Throw exception?

  (def tibilisi {:id 1 :title "Tbilisi" :population 1173000 :country "Georgia" :founded (.time r 1420 04 1 "Z")})
  (def batumi {:id 2 :title "Batumi" :population 124000 :country "Georgia" :founded (.time r 1505 11 3 "Z")})
  (def baku {:id 3 :title "Baku" :population 2137000 :country "Azerbaijan" :founded (.now r)})

  (-> r (.db db) (.table tbl) (.insert (walk/stringify-keys tibilisi)) (.run conn))
  (-> r (.db db) (.table tbl) (.insert (walk/stringify-keys batumi)) (.run conn))
  (-> r (.db db) (.table tbl) (.insert (walk/stringify-keys baku)) (.run conn))

  ;; DELETION =========================================================================================

  (-> r (.db db) (.table tbl) (.delete) (.run conn))

  ;; UPDATING =========================================================================================

  (-> r (.db db) (.table tbl) (.update {"population" 120000}) (.run conn))
  (-> r (.db db) (.table tbl) (.update {"population" #(do (prn %) 1)}) (.run conn)) ; TODO lambda stuff doesn't work

  ;; RETRIEVAL ======================================================================================
  
  (for [doc (-> r (.db db) (.table tbl) (.run conn))] ; lazy
    (prn doc))

  (-> r (.db db) (.table tbl) (.run conn) (.toList)) ; eager
  (take 2 (-> r (.db db) (.table tbl) (.run conn))) ; generic seq ops

  ;; BASIC QUERIES ==================================================================================
  ;; Querying using secondary indexes via getAll not covered

  (-> r (.db db) (.table tbl) (.filter {"population" 1200000}) (.count) (.run conn))
  ;; TODO again lambda stuff doesn't seem to work
  (-> r (.db db) (.table tbl) (.filter #(-> % (.getField "population") (.gt 50))) (.count) (.run conn))

  )

;; OTHER NOTES ============================================================================================
;; WTF: com.rethinkdb.gen.exc.ReqlQueryLogicError: Error in time logic: Year is out of valid range: 1400..10000.