package com.tnear.adoptloop.analysis

import com.tnear.adoptloop.admin.auth.AdminContext
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/surveys/{surveyId}")
class AnalysisController(
    private val aggregateService: AggregateService,
    private val analysisService: AnalysisService,
    private val adminContext: AdminContext,
) {
    @GetMapping("/aggregate")
    fun aggregate(@PathVariable surveyId: Long): AggregateRes =
        aggregateService.aggregate(adminContext.require(), surveyId)

    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    fun run(@PathVariable surveyId: Long): AnalysisRunRes =
        analysisService.run(adminContext.require(), surveyId)

    @GetMapping("/analyses/latest")
    fun latest(@PathVariable surveyId: Long): AnalysisRes =
        analysisService.latest(adminContext.require(), surveyId)
}
