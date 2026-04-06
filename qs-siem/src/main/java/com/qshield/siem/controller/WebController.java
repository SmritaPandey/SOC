package com.qshield.siem.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class WebController {
    @GetMapping("/")
    public String index() { return "redirect:/dashboard"; }

    @GetMapping("/dashboard")
    public String dashboard() { return "forward:/index.html"; }
}
