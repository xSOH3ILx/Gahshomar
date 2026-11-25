package com.soh3il.core.updater

import java.net.URI

object UpdateUrlPolicy {

    private val ALLOWED_HOSTS = setOf("github.com", "api.github.com")

    fun isAllowed(url: String): Boolean {
        val uri = try {
            URI(url)
        } catch (_: Exception) {
            java.util.logging.Logger.getLogger("UpdateUrlPolicy").warning("unparsable update URL rejected")
            return false
        }
        if (!uri.scheme.equals("https", ignoreCase = true)) return false
        val host = uri.host?.lowercase() ?: return false
        return host in ALLOWED_HOSTS
    }
}
