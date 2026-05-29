package com.tnear.adoptloop.adoption

import com.tnear.adoptloop.admin.auth.AdminContext
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/admin/adoptions")
class AdoptionController(
    private val service: AdoptionService,
    private val adminContext: AdminContext,
) {
    @GetMapping
    fun list(): List<AdoptionRes> =
        service.listForAdmin(adminContext.require()).map(AdoptionRes::from)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@Valid @RequestBody req: AdoptionCreateReq): AdoptionRes =
        AdoptionRes.from(service.create(adminContext.require(), req))

    @GetMapping("/{id}")
    fun get(@PathVariable id: Long): AdoptionRes =
        AdoptionRes.from(service.get(adminContext.require(), id))

    @PatchMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: AdoptionUpdateReq): AdoptionRes =
        AdoptionRes.from(service.update(adminContext.require(), id, req))
}
