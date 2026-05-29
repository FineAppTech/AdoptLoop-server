package com.tnear.adoptloop.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class PublicViewController {
    @GetMapping("/s/{slug}") fun survey(@PathVariable slug: String) = "public/survey"
}
