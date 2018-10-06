(ns material.component.list
  (:require [material.icon :as icon]
            [material.component :as cmp]
            [swarmpit.url :refer [dispatch!]]
            [swarmpit.utils :refer [select-keys* map-values]]
            [sablono.core :refer-macros [html]]
            [rum.core :as rum]))

(def active-panel (atom nil))

(defn- primary-key
  [render-metadata]
  (->> (filter #(= true (:primary %)) render-metadata)
       (first)
       :key))

(defn- render-keys
  [render-metadata]
  (->> render-metadata
       (map :key)
       (into [])))

(defn filter
  [items query]
  (if (or (empty? query)
          (< (count query) 2))
    items
    (clojure.core/filter
      (fn [item]
        (->> (map-values item)
             (clojure.core/filter #(clojure.string/includes? % query))
             (empty?)
             (not))) items)))

(defn data-table-head
  [render-metadata]
  (cmp/table-head
    {:key "Swarmpit-data-table-head"}
    (cmp/table-row
      {:key "Swarmpit-data-table-head-row"}
      (map-indexed
        (fn [index header]
          (cmp/table-cell
            {:key       (str "Swarmpit-data-table-head-cell-" index)
             :className "Swarmpit-data-table-head-cell"}
            (:name header))) render-metadata))))

(defn data-table-body
  [render-metadata items type action-handler-fn]
  (cmp/table-body
    {:key "Swarmpit-data-table-body"}
    (map-indexed
      (fn [index item]
        (cmp/table-row
          (merge {:key (str "Swarmpit-data-table-body-row-" index)}
                 (case type
                   :list {:onClick #(action-handler-fn item)
                          :hover   true}
                   :info {:hover true}
                   nil))
          (->> (select-keys* item (render-keys render-metadata))
               (map-indexed
                 (fn [coll-index coll]
                   (cmp/table-cell
                     {:key       (str "Swarmpit-data-table-row-cell-" index "-" coll-index)
                      :className (case type
                                   :edit "Swarmpit-data-table-row-cell-edit"
                                   :info "Swarmpit-data-table-row-cell-info"
                                   nil)}
                     (let [render-fn (:render-fn (nth render-metadata coll-index))
                           value (val coll)]
                       (if render-fn
                         (render-fn value item index)
                         value))))))
          (when (= :edit type)
            (cmp/table-cell
              {:className "Swarmpit-data-table-row-cell-delete"}
              (cmp/tooltip
                {:title     "Delete"
                 :placement "top-start"}
                (cmp/icon-button
                  {:color   "secondary"
                   :onClick #(action-handler-fn index)} (cmp/svg icon/trash))))))) items)))

(defn data-table-raw
  [render-metadata items type action-handler-fn]
  (cmp/table
    {:key       "Swarmpit-data-table"
     :className "Swarmpit-data-table"}
    (when (not (= :edit type))
      (data-table-head render-metadata))
    (data-table-body render-metadata items type action-handler-fn)))

(defn data-table
  [render-metadata items type action-handler-fn]
  (if (= :edit type)
    (data-table-raw render-metadata items type action-handler-fn)
    (cmp/paper
      {:className "Swarmpit-paper"}
      (data-table-raw render-metadata items type action-handler-fn))))

(defn data-list-expandable-item-detail
  [name value type]
  (if (= :edit type)
    (cmp/grid
      {:container true
       :className "Swarmpit-form-item"}
      (cmp/grid
        {:item true
         :xs   12} value))
    (cmp/grid
      {:container true
       :className "Swarmpit-form-item"}
      (cmp/grid
        {:item      true
         :xs        6
         :className "Swarmpit-form-item-label"} name)
      (cmp/grid
        {:item true
         :xs   6} value))))

(defn data-list-expandable-item-details
  [render-metadata type item index]
  (let [labels (map :name render-metadata)]
    (cmp/grid
      {:container true
       :direction "column"
       :xs        12
       :sm        6}
      (->> (select-keys* item (render-keys render-metadata))
           (map-indexed
             (fn [coll-index coll]
               (let [render-fn (:render-fn (nth render-metadata coll-index))
                     value (val coll)
                     name (nth labels coll-index)]
                 (if render-fn
                   (data-list-expandable-item-detail name (render-fn value item index) type)
                   (data-list-expandable-item-detail name value type)))))))))

(defn data-list-expandable-item
  [render-metadata index item summary-status type action-handler-fn]
  (let [expanded (rum/react active-panel)
        summary (get-in item (primary-key render-metadata))]
    (cmp/expansion-panel
      {:key      (str "Swarmpit-data-list-expandable-panel-" index)
       :expanded (= expanded index)
       :onChange #(if (= expanded index)
                    (reset! active-panel false)
                    (reset! active-panel index))}
      (cmp/expansion-panel-summary
        {:key        (str "Swarmpit-data-list-expandable-panel-summary-" index)
         :className  "Swarmpit-data-list-expandable-panel-summary"
         :expandIcon icon/expand-more}
        (html
          [:div.Swarmpit-data-list-expandable-panel-summary-content summary-status
           [:div
            (cmp/typography
              {:key          (str "Swarmpit-data-list-expandable-panel-summary-text-" index)
               :className    "Swarmpit-data-list-expandable-panel-summary-text"
               :gutterBottom true
               :noWrap       true
               :variant      "subheading"} summary)]]))
      (cmp/expansion-panel-details
        {:key (str "Swarmpit-data-list-expandable-panel-details-" index)}
        (data-list-expandable-item-details render-metadata type item index))
      (cmp/divider)
      (case type
        :edit (cmp/expansion-panel-actions
                {}
                (cmp/button {:size    "small"
                             :onClick #(action-handler-fn index)
                             :color   "primary"} "Delete"))
        :list (cmp/expansion-panel-actions
                {}
                (cmp/button {:size    "small"
                             :onClick #(action-handler-fn item)
                             :color   "primary"} "Details"))
        nil))))

(defn data-list
  [render-metadata render-status-fn items type action-handler-fn]
  (html
    [:div.Swarmpit-data-list
     (map-indexed
       (fn [index item]
         (data-list-expandable-item
           render-metadata
           index
           item
           (when render-status-fn (render-status-fn item))
           type
           action-handler-fn)) items)]))

(rum/defc view < rum/reactive
  [render-metadata render-status-fn items onclick-handler-fn]
  (html
    [:div
     (cmp/hidden
       {:only           ["xs" "sm" "md"]
        :implementation "js"}
       (data-table render-metadata items :list onclick-handler-fn))
     (cmp/hidden
       {:only           ["lg" "xl"]
        :implementation "js"}
       (data-list render-metadata render-status-fn items :list onclick-handler-fn))]))

;(rum/defc edit < rum/reactive
;  [render-metadata items ondelete-handler-fn]
;  (html
;    [:div
;     (cmp/hidden
;       {:only           ["xs" "sm" "md"]
;        :implementation "js"}
;       (data-table render-metadata items :edit ondelete-handler-fn))
;     (cmp/hidden
;       {:only           ["lg" "xl"]
;        :implementation "js"}
;       (data-list render-metadata nil items :edit ondelete-handler-fn))]))

;(rum/defc info < rum/reactive
;  [render-metadata items]
;  (html
;    [:div
;     (cmp/hidden
;       {:only           ["xs" "sm" "md"]
;        :implementation "js"}
;       (data-table render-metadata items :info nil))
;     (cmp/hidden
;       {:only           ["lg" "xl"]
;        :implementation "js"}
;       (data-list render-metadata nil items :info nil))]))