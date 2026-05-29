package com.tnear.adoptloop.survey.draft

import com.tnear.adoptloop.domain.QuestionType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper

class SurveyDraftParserTest {

    private val parser = SurveyDraftParser(ObjectMapper())

    @Test
    fun `머리말이 섞인 JSON에서 본문을 추출해 파싱한다`() {
        val raw = """
            여기 설문입니다:
            {"title":"Jira 설문","questions":[
                {"type":"SCALE","text":"사용 빈도","axis":"USAGE"},
                {"type":"SINGLE_CHOICE","text":"역할","options":["기획","개발"]}
            ]}
        """.trimIndent()

        val payload = parser.parse(raw)

        assertEquals("Jira 설문", payload.title)
        assertEquals(2, payload.questions.size)
        assertEquals(QuestionType.SCALE, payload.questions[0].type)
        assertEquals(2, payload.questions[1].options.size)
        assertEquals(1, payload.questions[1].options[0].orderIndex)
    }

    @Test
    fun `JSON 객체가 없으면 예외`() {
        assertThrows(RuntimeException::class.java) { parser.parse("모델이 응답하지 못했습니다") }
    }

    @Test
    fun `질문 배열이 비면 예외`() {
        assertThrows(RuntimeException::class.java) {
            parser.parse("""{"title":"t","questions":[]}""")
        }
    }

    @Test
    fun `title이 JSON null이면 예외 (NullNode asString은 빈 문자열)`() {
        assertThrows(RuntimeException::class.java) {
            parser.parse("""{"title":null,"questions":[{"type":"TEXT","text":"의견"}]}""")
        }
    }
}
