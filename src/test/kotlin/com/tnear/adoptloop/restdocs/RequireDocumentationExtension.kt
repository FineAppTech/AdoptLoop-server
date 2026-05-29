package com.tnear.adoptloop.restdocs

import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class RequireDocumentationExtension : BeforeEachCallback, AfterEachCallback {
    override fun beforeEach(ctx: ExtensionContext) { DocCallTracker.reset() }
    override fun afterEach(ctx: ExtensionContext) {
        if (ctx.executionException.isEmpty && !DocCallTracker.wasCalled()) {
            throw AssertionError(
                "Controller test '${ctx.displayName}' did not call documentApi(...). " +
                "REST Docs is enforced (ADR-0009)."
            )
        }
    }
}
