#pragma once
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>

typedef struct {
    void *user_ctx;
    const char *uri;
    int method;
    size_t content_len;
} httpd_req_t;

typedef int esp_err_t;
typedef int httpd_err_code_t;
#define ESP_OK 0
#define ESP_FAIL -1

typedef enum {
    HTTP_GET,
    HTTP_POST,
    HTTP_DELETE,
    HTTP_HEAD,
    HTTP_PUT,
    HTTP_CONNECT,
    HTTP_OPTIONS,
    HTTP_TRACE,
    HTTP_COPY,
    HTTP_LOCK,
    HTTP_MKCOL,
    HTTP_MOVE,
    HTTP_PROPFIND,
    HTTP_PROPPATCH,
    HTTP_SEARCH,
    HTTP_UNLOCK,
    HTTP_BIND,
    HTTP_REBIND,
    HTTP_UNBIND,
    HTTP_ACL,
    HTTP_REPORT,
    HTTP_MKACTIVITY,
    HTTP_CHECKOUT,
    HTTP_MERGE,
    HTTP_MSEARCH,
    HTTP_NOTIFY,
    HTTP_SUBSCRIBE,
    HTTP_UNSUBSCRIBE,
    HTTP_PATCH,
    HTTP_PURGE,
    HTTP_MKCALENDAR
} http_method;

#define HTTPD_RESP_USE_STRLEN -1

// Struct to capture response for verification
typedef struct {
    char content_type[64];
    char body[1024];
    int status_code;
} mock_response_t;

extern mock_response_t g_last_response;

static inline esp_err_t httpd_resp_set_type(httpd_req_t *r, const char *type) {
    strncpy(g_last_response.content_type, type, sizeof(g_last_response.content_type)-1);
    return ESP_OK;
}

static inline esp_err_t httpd_resp_sendstr(httpd_req_t *r, const char *str) {
    strncpy(g_last_response.body, str, sizeof(g_last_response.body)-1);
    return ESP_OK;
}

static inline esp_err_t httpd_resp_send(httpd_req_t *r, const char *buf, ssize_t len) {
    if (len == HTTPD_RESP_USE_STRLEN) len = strlen(buf);
    strncpy(g_last_response.body, buf, sizeof(g_last_response.body)-1);
    return ESP_OK;
}

// Other stubs needed for compilation
typedef struct {
    const char *uri;
    int method;
    esp_err_t (*handler)(httpd_req_t *r);
    void *user_ctx;
} httpd_uri_t;

typedef void* httpd_handle_t;
typedef struct {
    int server_port;
    int ctrl_port;
    int lru_purge_enable;
    int max_uri_handlers;
    int stack_size;
} httpd_config_t;

#define HTTPD_DEFAULT_CONFIG() { .server_port = 80 }
esp_err_t httpd_start(httpd_handle_t *handle, const httpd_config_t *config);
esp_err_t httpd_register_uri_handler(httpd_handle_t handle, const httpd_uri_t *uri_handler);
void httpd_stop(httpd_handle_t handle);

// Error response stubs
#define HTTPD_400_BAD_REQUEST "400"
#define HTTPD_403_FORBIDDEN "403"
#define HTTPD_404_NOT_FOUND "404"
#define HTTPD_500_INTERNAL_SERVER_ERROR "500"
#define HTTPD_SOCK_ERR_TIMEOUT -2

static inline esp_err_t httpd_resp_send_err(httpd_req_t *req, const char* code, const char* msg) {
    snprintf(g_last_response.body, sizeof(g_last_response.body), "ERROR %s: %s", code, msg);
    g_last_response.status_code = atoi(code);
    return ESP_OK;
}
static inline esp_err_t httpd_resp_send_404(httpd_req_t *req) { return httpd_resp_send_err(req, "404", "Not Found"); }
static inline esp_err_t httpd_resp_send_500(httpd_req_t *req) { return httpd_resp_send_err(req, "500", "Internal Error"); }
static inline esp_err_t httpd_resp_set_hdr(httpd_req_t *req, const char *field, const char *value) { return ESP_OK; }
static inline esp_err_t httpd_req_get_hdr_value_str(httpd_req_t *req, const char *field, char *val, size_t val_size) { return ESP_FAIL; }
static inline int httpd_req_recv(httpd_req_t *req, char *buf, size_t len) { return 0; }
static inline esp_err_t httpd_resp_send_chunk(httpd_req_t *req, const char *buf, ssize_t len) { return ESP_OK; }
static inline esp_err_t httpd_req_get_url_query_str(httpd_req_t *req, char *buf, size_t len) { return ESP_FAIL; }
static inline esp_err_t httpd_query_key_value(const char *qry, const char *key, char *val, size_t val_len) { return ESP_FAIL; }
