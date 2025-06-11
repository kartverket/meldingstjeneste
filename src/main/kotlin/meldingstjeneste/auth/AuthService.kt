package meldingstjeneste.auth

import meldingstjeneste.microsoft.MicrosoftService

interface AuthService {
    fun hasAccess(userId: String): Boolean
}

class AuthServiceImpl(
    private val superUserGroupId: String,
    private val microsoftService: MicrosoftService
) : AuthService {
    override fun hasAccess(userId: String): Boolean {
        return hasTeamAccess(userId, superUserGroupId)
    }

    private fun hasTeamAccess(userId: String, teamId: String): Boolean {
        val userTeams = microsoftService.getMemberGroups(userId)
        return userTeams.any { it == teamId }
    }
}