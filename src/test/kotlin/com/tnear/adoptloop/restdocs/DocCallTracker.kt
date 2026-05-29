package com.tnear.adoptloop.restdocs

object DocCallTracker {
    private val called = ThreadLocal.withInitial { false }
    fun reset() { called.set(false) }
    fun mark() { called.set(true) }
    fun wasCalled(): Boolean = called.get()
}
