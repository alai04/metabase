(ns metabase.models.secret
  (:require [cheshire.generate :refer [add-encoder encode-map]]
            [metabase.models.interface :as i]
            [metabase.util :as u]
            [toucan.models :as models]
            [toucan.db :as db]))

;;; ----------------------------------------------- Entity & Lifecycle -----------------------------------------------

(models/defmodel Secret :secret)

(u/strict-extend (class Secret)
  models/IModel
  (merge models/IModelDefaults
         {;:hydration-keys (constantly [:database :db]) ; don't think there's any hydration going on since other models
                                                        ; won't have a direct secret-id column
          :types          (constantly {:value  :secret-value
                                       :kind   :keyword
                                       :source :keyword})
          :properties     (constantly {:timestamped? true})})
  i/IObjectPermissions
  (merge i/IObjectPermissionsDefaults
         {:can-read?         i/superuser?
          :can-write?        i/superuser?}))

;;; ---------------------------------------------- Hydration / Util Fns ----------------------------------------------
;; none yet

(def
  ^{:doc "The attributes of a secret which, if changed, will result in a version bump" :private true}
  bump-version-keys
  [:kind :source :value])

(defn upsert-secret-value! [existing-id nm kind source value]
  (let [insert-new     (fn [v]
                         (db/insert! Secret {:version  v
                                              :name    nm
                                              :kind    kind
                                              :source  source
                                              :value   value}))
        latest-version (if existing-id (db/select-one Secret :id existing-id {:order-by [[:version :desc]]}))]
    (if latest-version
      (if (= (select-keys latest-version bump-version-keys) [kind source value])
        (db/update-where! Secret {:id existing-id :version (:version latest-version)}
                                 :name nm)
        (insert-new (inc (:version latest-version))))
      (insert-new 1))))

;;; -------------------------------------------------- JSON Encoder --------------------------------------------------

(add-encoder SecretInstance (fn [secret json-generator]
                              (encode-map
                               (dissoc secret :value) ; never include the secret value in JSON
                               json-generator)))
