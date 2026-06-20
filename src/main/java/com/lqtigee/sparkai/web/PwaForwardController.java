package com.lqtigee.sparkai.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PwaForwardController {

    @GetMapping({
            "/",
            "/sessions",
            "/control",
            "/runs",
            "/settings"
    })
    public String forwardPwaRoute() {
        return "forward:/index.html";
    }
}
