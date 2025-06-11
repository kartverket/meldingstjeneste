package meldingstjeneste.microsoft

import com.microsoft.graph.serviceclient.GraphServiceClient
import com.azure.identity.ClientSecretCredentialBuilder
import meldingstjeneste.auth.EntraConfig

interface MicrosoftService {
    fun getMemberGroups(userId: String): Set<String>
}

class MicrosoftServiceImpl(private val graphClient: GraphServiceClient) : MicrosoftService {
    override fun getMemberGroups(userId: String): Set<String> {
        val groups = graphClient.users().byUserId(userId).memberOf().graphGroup().get().value

        return groups.map {
            it.id
        }.toSet()
    }

    companion object {
        fun load(config: EntraConfig): MicrosoftService {
            val scopes = "https://graph.microsoft.com/.default"
            val credential = ClientSecretCredentialBuilder()
                .clientId(config.clientId)
                .tenantId(config.tenantId)
                .clientSecret(config.clientSecret)
                .build()
            val graphClient = GraphServiceClient(credential, scopes)

            return MicrosoftServiceImpl(graphClient)
        }
    }
}