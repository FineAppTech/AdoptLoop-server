package com.tnear.adoptloop.actionitem

import com.tnear.adoptloop.domain.ActionItem
import com.tnear.adoptloop.domain.Priority
import com.tnear.adoptloop.domain.TodoStatus
import java.time.Instant

data class ActionItemCreateReq(
    val analysisId: Long,
    val title: String,
    val description: String? = null,
    val priority: Priority,
)

data class ActionItemUpdateReq(val status: TodoStatus? = null)

data class ActionItemRes(
    val id: Long,
    val adoptionId: Long,
    val analysisId: Long,
    val title: String,
    val description: String?,
    val priority: Priority,
    val status: TodoStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    companion object {
        fun from(actionItem: ActionItem) = ActionItemRes(
            actionItem.id!!, actionItem.adoptionId, actionItem.analysisId, actionItem.title,
            actionItem.description, actionItem.priority, actionItem.status, actionItem.createdAt, actionItem.updatedAt,
        )
    }
}
