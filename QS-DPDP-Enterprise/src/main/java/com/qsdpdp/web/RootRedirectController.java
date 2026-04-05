package com.qsdpdp.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Serves the main SPA pages directly from classpath.
 * This ensures the desktop app always loads correctly.
 */
@Controller
public class RootRedirectController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String serveIndex() throws Exception {
        return loadResource("static/index.html");
    }

    @GetMapping(value = "/index.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String serveIndexHtml() throws Exception {
        return loadResource("static/index.html");
    }

    @GetMapping(value = "/portal.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String servePortal() throws Exception {
        return loadResource("static/portal.html");
    }

    @GetMapping(value = "/install.html", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public String serveInstall() throws Exception {
        return loadResource("static/install.html");
    }

    private String loadResource(String path) throws Exception {
        ClassPathResource res = new ClassPathResource(path);
        try (InputStream is = res.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
