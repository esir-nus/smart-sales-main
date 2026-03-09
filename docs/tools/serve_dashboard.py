import http.server
import socketserver
import os
import mimetypes

PORT = 8000

# Ensure .md files are served as UTF-8 so the browser doesn't garble Chinese characters
mimetypes.add_type('text/markdown; charset=utf-8', '.md')
mimetypes.add_type('text/html; charset=utf-8', '.html')

class UTF8RequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        super().end_headers()

if __name__ == "__main__":
    # Change directory to docs/cerb so relative links work
    cerb_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "cerb")
    os.chdir(cerb_dir)
    
    # SimpleHTTPServer with forced UTF-8 for Markdown
    with socketserver.TCPServer(("", PORT), UTF8RequestHandler) as httpd:
        print(f"Serving Dashboards at http://localhost:{PORT}")
        httpd.serve_forever()
