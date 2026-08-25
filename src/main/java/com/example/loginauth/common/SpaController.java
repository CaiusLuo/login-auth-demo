package com.example.loginauth.common;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaController {

    @GetMapping({"/login", "/register", "/app", "/admin"})
    String spa() {
        return "forward:/index.html";
    }
}
