(ns basic-pedestal-sqs-example.service
  (:require [io.pedestal.http :as http]
            [io.pedestal.http.route :as route]
            [io.pedestal.http.body-params :as body-params]
            [pedestal.sqs :as sqs]
            [pedestal.sqs.messaging :as messaging]
            [pedestal.sqs.interceptors :as sqs.interceptors]
            [pedestal.sqs.queue :as queue]
            [ring.util.response :as ring-resp]))

(defn home-page
  [request]
  (let [sqs-client (:sqs-client request)]
    (messaging/send-message!
      sqs-client
      (queue/get-queue-id sqs-client "bar-queue")
      (messaging/to-json {:example "example"}))
    (ring-resp/response "Hello from pedestal.sqs!")))

;; Defines "/" and "/about" routes with their associated :get handlers.
;; The interceptors defined after the verb map (e.g., {:get home-page}
;; apply to / and its children (/about).
(def common-interceptors [(body-params/body-params) http/html-body])

;; Tabular routes
(def routes #{["/send" :get (conj common-interceptors `home-page)]})

;; Map-based routes
;(def routes `{"/" {:interceptors [(body-params/body-params) http/html-body]
;                   :get home-page
;                   "/about" {:get about-page}}})

;; Terse/Vector-based routes
;(def routes
;  `[[["/" {:get home-page}
;      ^:interceptors [(body-params/body-params) http/html-body]
;      ["/about" {:get about-page}]]]])

(defn foo-listener
  [{:keys [message]}]
  (prn (:Body message)))

(defn bar-listener
  [{:keys [message]}]
  (prn message))

(defn egg-listener
  [{:keys [message]}]
  (prn message))

;; Consumed by basic-pedestal-sqs-example.server/create-server
;; See http/default-interceptors for additional options you can configure
(def service {:env                     :prod
              ;; You can bring your own non-default interceptors. Make
              ;; sure you include routing and set it up right for
              ;; dev-mode. If you do, many other keys for configuring
              ;; default interceptors will be ignored.
              ;; ::http/interceptors []
              ::http/routes            routes

              ::sqs/client             {:region            "us-east-1"
                                        :endpoint-override {:protocol :http
                                                            :hostname "localhost"
                                                            :port     9324}}

              ::sqs/configurations     {:auto-create-queue? true}

              ;; Arguments
              ;; queue-name (e.g. foo-queue)
              ;; listener function (e.g. foo-listener)
              ;; queue/listener configurations of library and aws-api (here a shortcut to (aws/doc :ReceiveMessage))
              ;;
              ;; Comments about listeners
              ;; reference of ::sqs/deletion-policy https://github.com/spring-cloud/spring-cloud-aws/blob/v2.1.2.RELEASE/spring-cloud-aws-messaging/src/main/java/org/springframework/cloud/aws/messaging/listener/SqsMessageDeletionPolicy.java#L45
              ::sqs/listeners          #{["foo-queue" foo-listener {:WaitTimeSeconds      20
                                                                    ::sqs/deletion-policy :always
                                                                    ::sqs/response-type   :json}]
                                         ["bar-queue" bar-listener {::sqs/deletion-policy       :on-success
                                                                    ::sqs/response-interceptors [sqs.interceptors/json-parser]}]
                                         ["egg-queue" egg-listener {:WaitTimeSeconds 10}]}

              ;; Uncomment next line to enable CORS support, add
              ;; string(s) specifying scheme, host and port for
              ;; allowed source(s):
              ;;
              ;; "http://localhost:8080"
              ;;
              ;;::http/allowed-origins ["scheme://host:port"]

              ;; Tune the Secure Headers
              ;; and specifically the Content Security Policy appropriate to your service/application
              ;; For more information, see: https://content-security-policy.com/
              ;;   See also: https://github.com/pedestal/pedestal/issues/499
              ;;::http/secure-headers {:content-security-policy-settings {:object-src "'none'"
              ;;                                                          :script-src "'unsafe-inline' 'unsafe-eval' 'strict-dynamic' https: http:"
              ;;                                                          :frame-ancestors "'none'"}}

              ;; Root for resource interceptor that is available by default.
              ::http/resource-path     "/public"

              ;; Either :jetty, :immutant or :tomcat (see comments in project.clj)
              ;;  This can also be your own chain provider/server-fn -- http://pedestal.io/reference/architecture-overview#_chain_provider
              ::http/type              :jetty
              ;;::http/host "localhost"
              ::http/port              8080
              ;; Options to pass to the container (Jetty)
              ::http/container-options {:h2c? true
                                        :h2?  false
                                        ;:keystore "test/hp/keystore.jks"
                                        ;:key-password "password"
                                        ;:ssl-port 8443
                                        :ssl? false
                                        ;; Alternatively, You can specify you're own Jetty HTTPConfiguration
                                        ;; via the `:io.pedestal.http.jetty/http-configuration` container option.
                                        ;:io.pedestal.http.jetty/http-configuration (org.eclipse.jetty.server.HttpConfiguration.)
                                        }})
