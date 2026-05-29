package com.tnear.adoptloop.survey.publish

import com.tnear.adoptloop.domain.Survey
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager

@Component
class SurveyPublisher(private val notifier: SlackNotifier) {
    // 트랜잭션 커밋 이후에만 Slack 알림을 전송한다. 발행이 롤백되면 알림도 나가지 않는다.
    fun scheduleAnnouncement(survey: Survey) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCommit() { notifier.publishAnnouncement(survey) }
        })
    }
}
