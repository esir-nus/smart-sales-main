#include <stdio.h>
#include <string.h>
#include "esp_http_server.h"

// Define global mock response
mock_response_t g_last_response;

// Define global bool needed by webserver-test.c
bool g_sdcard_mounted = false;

// Stub functions needed by linker
esp_err_t httpd_start(httpd_handle_t *handle, const httpd_config_t *config) { return ESP_OK; }
esp_err_t httpd_register_uri_handler(httpd_handle_t handle, const httpd_uri_t *uri_handler) { return ESP_OK; }
void httpd_stop(httpd_handle_t handle) {}

// Include the C file under test directly to access static functions
#include "../../reference-source/webserver-test.c"

int main() {
    printf("Running WebUI Logic Test...\n");

    // Mock request
    httpd_req_t req;
    memset(&req, 0, sizeof(req));
    req.method = HTTP_GET;
    req.uri = "/";

    // Clear response
    memset(&g_last_response, 0, sizeof(g_last_response));

    // Call the handler
    printf("Calling root_handler()...\n");
    esp_err_t res = root_handler(&req);

    if (res != ESP_OK) {
        printf("FAILED: root_handler returned error %d\n", res);
        return 1;
    }

    // Verify Content-Type
    printf("Content-Type: %s\n", g_last_response.content_type);
    if (strcmp(g_last_response.content_type, "application/json") != 0) {
        printf("FAILED: Expected content type 'application/json', got '%s'\n", g_last_response.content_type);
        return 1;
    }

    // Verify Body
    printf("Body: %s\n", g_last_response.body);
    const char *expected = "{\"status\":\"ok\",\"version\":\"1.1.0\"}";
    if (strcmp(g_last_response.body, expected) != 0) {
        printf("FAILED: Expected body '%s', got '%s'\n", expected, g_last_response.body);
        return 1;
    }

    printf("✅ TEST PASSED: WebUI is removed and API returns JSON.\n");
    return 0;
}
