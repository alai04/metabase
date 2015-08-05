(ns metabase.api.revision
  (:require [compojure.core :refer [GET POST]]
            [metabase.api.common :refer :all]
            [metabase.db :refer [exists?]]
            (metabase.models [card :refer [Card]]
                             [revision :as revision])))

(def ^:private ^:const entity-kw->entity
  {:card Card})

(defannotation Entity
  "Option must be a valid revisionable entity name. Returns corresponding entity."
  [symb value]
  (let [entity (entity-kw->entity (keyword value))]
    (checkp entity symb (format "Invalid entity: %s" value))
    entity))

(defendpoint GET "/"
  "Get revisions of an object."
  [entity id]
  {entity Entity, id Integer}
  (check-404 (exists? entity :id id))
  (revision/revisions+details entity id))

(defendpoint POST "/revert"
  "Revert an object to a prior revision."
  [entity id revision-id]
  {entity Entity, id Integer, revision-id Integer}
  (revision/revert :entity entity, :id id, :revision-id revision-id))

(define-routes)
