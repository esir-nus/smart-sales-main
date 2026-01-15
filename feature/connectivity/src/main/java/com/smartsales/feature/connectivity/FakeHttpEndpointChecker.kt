package com.smartsales.feature.connectivity

/**
 * Fake implementation for testing. Supports stubbing reachability.
 */
class FakeHttpEndpointChecker : HttpEndpointChecker {
    var stubReachable: Boolean = true
    val checkCalls = mutableListOf<String>()
    
    override suspend fun isReachable(baseUrl: String): Boolean {
        checkCalls.add(baseUrl)
        return stubReachable
    }
    
    fun reset() {
        stubReachable = true
        checkCalls.clear()
    }
}
