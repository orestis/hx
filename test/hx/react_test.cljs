(ns hx.react-test
  (:require [cljs.test :as t :include-macros true]
            [hx.react :as hx]
            [goog.dom :as dom]
            [goog.object :as gobj]
            [clojure.string :as str]
            ["react-testing-library" :as rtl]))

(t/use-fixtures :each
  {:after rtl/cleanup})

;;
;; Utils
;;

(def render rtl/render)

(defn root [result]
  (-> result
      (.-container)
      (.-firstChild)))

(defn node= [x y]
  (.isEqualNode x y))

(defn html [s]
  (let [template (dom/createElement "template")]
    (gobj/set template "innerHTML" (str/trim s))
    (gobj/getValueByKeys template "content" "firstChild")))

(defn func []
  (let [call-count (atom 0)
        f (fn spy-func [& x]
            (swap! call-count inc))]
    (set! (.-callCount f) call-count)
    f))

(defn call-count [f]
  @(.-callCount f))

(defn click [node]
  (.click rtl/fireEvent node))

(defn change [node data]
  (.change rtl/fireEvent node data))


;;
;; Tests
;;

(t/deftest create-element
  (t/is (node= (html "<div>hi</div>")
               (root (render (hx/f [:div "hi"])))))
  (t/is (node= (html "<div><span>hi</span><span>bye</span></div>")
               (root (render (hx/f [:div
                                           [:span "hi"]
                                           [:span "bye"]]))))))

(t/deftest create-fragment
  ;; for fragments, the firstChild of the container is the first element
  (t/is (node= (html "<div>hi</div>")
               (root (render (hx/f [:<> [:div "hi"]])))))

  ;; so here we test to see if the container matches
  (t/is (node= (html "<div><span>hi</span><span>bye</span></div>")
               (.-container (render (hx/f [:<>
                                           [:span "hi"]
                                           [:span "bye"]]))))))

(t/deftest create-provider
  (let [c (hx/create-context)]
  (t/is (node= (html "<div>hi</div>")
               (-> (hx/f [:provider {:context c}
                          [:div "hi"]])
                   (render)
                   (root))))))

(t/deftest style-prop
  (t/is (node= (html "<div style=\"color: red;\">hi</div>")
               (root (render (hx/f [:div {:style {:color "red"}} "hi"])))))

  (t/is (node= (html "<div style=\"color: red; background: green;\">hi</div>")
               (root (render (hx/f [:div {:style {:color "red"
                                                         :background "green"}} "hi"]))))))

(t/deftest class-prop
  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (render (hx/f [:div {:class "foo"} "hi"])))))

  (t/is (node= (html "<div class=\"foo\">hi</div>")
               (root (render (hx/f [:div {:class ["foo"]} "hi"])))))

  (t/is (node= (html "<div class=\"foo bar\">hi</div>")
               (root (render (hx/f [:div {:class ["foo" "bar"]} "hi"]))))))

(t/deftest on-click-prop
  (let [on-click (func)
        node (-> [:div {:on-click on-click}]
                 (hx/f)
                 (render)
                 (root))]
    (click node)
    (t/is (= 1 (call-count on-click)))))

(t/deftest input-on-change-prop
  (let [on-change (func)
        node (-> [:input {:on-change on-change}]
                 (hx/f)
                 (render)
                 (root))]
    (change node #js {:target #js {:value "a"}})
    (t/is (= 1 (call-count on-change)))))