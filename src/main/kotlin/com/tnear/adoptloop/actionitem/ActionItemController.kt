package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.admin.auth.AdminContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin")
class ActionItemController(
    private val service: ActionItemService,
    private val adminContext: AdminContext,
) {
    @GetMapping("/adoptions/{adoptionId}/action-items")
    fun list(@PathVariable adoptionId: Long): List<ActionItemRes> =
        service.list(adminContext.require(), adoptionId).map(ActionItemRes::from)

    @PostMapping("/adoptions/{adoptionId}/action-items")
    @ResponseStatus(HttpStatus.CREATED)
    fun adopt(@PathVariable adoptionId: Long, @RequestBody items: List<ActionItemCreateReq>): List<ActionItemRes> =
        service.adopt(adminContext.require(), adoptionId, items).map(ActionItemRes::from)

    @PatchMapping("/action-items/{id}")
    fun update(@PathVariable id: Long, @RequestBody req: ActionItemUpdateReq): ActionItemRes =
        ActionItemRes.from(service.updateStatus(adminContext.require(), id, req))
}
