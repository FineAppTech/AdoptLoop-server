package com.tnear.adoptloop.restdocs

import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation
import org.springframework.restdocs.snippet.Snippet
import org.springframework.test.web.servlet.ResultHandler

// 표준 `document(...)` 대신 반드시 이걸 호출한다.
// DocCallTracker가 호출 여부를 추적하므로 우회 시 RequireDocumentationExtension이 fail시킨다.
fun documentApi(identifier: String, vararg snippets: Snippet): ResultHandler {
    DocCallTracker.mark()
    return MockMvcRestDocumentation.document(identifier, *snippets)
}
