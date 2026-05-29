package com.tnear.adoptloop.web

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable

@Controller
class AdminViewController {
    @GetMapping("/admin") fun root() = "redirect:/admin/adoptions"
    @GetMapping("/admin/login") fun login() = "admin/login"
    @GetMapping("/admin/adoptions") fun list() = "admin/adoptions/list"
    @GetMapping("/admin/adoptions/new") fun new() = "admin/adoptions/new"
    @GetMapping("/admin/adoptions/{id}") fun detail(@PathVariable id: Long) = "admin/adoptions/detail"
    @GetMapping("/admin/surveys/{id}/edit") fun edit(@PathVariable id: Long) = "admin/surveys/edit"
    @GetMapping("/admin/surveys/{id}/analyze") fun analyze(@PathVariable id: Long) = "admin/surveys/analyze"
}
