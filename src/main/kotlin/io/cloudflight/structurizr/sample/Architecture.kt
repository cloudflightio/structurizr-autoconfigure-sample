package io.cloudflight.structurizr.sample

import com.structurizr.Workspace
import com.structurizr.model.Location
import com.structurizr.model.Model
import com.structurizr.model.Tags
import com.structurizr.view.Shape
import com.structurizr.view.ViewSet
import io.cloudflight.architecture.structurizr.SpringStructurizr
import io.cloudflight.architecture.structurizr.ViewProvider
import io.cloudflight.architecture.structurizr.kotlin.addWithCustomTags
import io.cloudflight.architectureicons.azure.AzureMonoIcons
import io.cloudflight.architectureicons.tupadr3.DevIcons2
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.stereotype.Component

/**
 * @author Klaus Lehner, Cloudflight
 */
@SpringBootApplication
class Architecture

fun main() {
    SpringStructurizr.run(Architecture::class.java)
}

@Component
class ViewConfigurer(workspace: Workspace) : ViewProvider {

    init {
        with(workspace.views.configuration.styles) {
            addElementStyle(MyTags.Database).shape(Shape.Cylinder)
            addElementStyle(MyTags.FileSystem).shape(Shape.Folder)
            addElementStyle(Tags.PERSON).shape(Shape.Person)
            addElementStyle(DevIcons2.SPRING.name).background("#6DB33F").color("#000000")
        }

        with(workspace.views.configuration) {
            addTheme(AzureMonoIcons.STRUCTURIZR_THEME_URL)
            addTheme(DevIcons2.STRUCTURIZR_THEME_URL)
        }
    }

    override fun createViews(viewSet: ViewSet) {
        viewSet.createSystemLandscapeView("codingcontest", "").also {
            it.addAllElements()
        }
    }
}

@Component
class Personas(model: Model) {
    val user = model.addPerson(Location.External, "Contest Participant", "")
    val admin = model.addPerson(Location.Internal, "Administrator", "")
}

@Component
class CodingContest(model: Model, personas: Personas) : ViewProvider {

    private val platform = model.addSoftwareSystem("Coding Contest Platform")

    init {
        val registration =
            platform.addContainer("Registration", "maintains all users and contests, provides SSO").apply {
                url = "https://register.codingcontest.org/"
                addTags(DevIcons2.SPRING.name)
            }
        val catCoder = platform.addContainer("CatCoder", "provides the possibility to solve coding challenges").apply {
            url = "https://catcoder.codingcontest.org/"

            addTags(DevIcons2.SPRING.name)
            registration.uses(this, "fetches contests", "REST")
        }

        val catCoderDb = platform.addContainer("CatCoder-DB", "", "MariaDB").apply {
            addTags(
                MyTags.Database,
                AzureMonoIcons.Databases.AZURE_DATABASE_FOR_MARIA_DB.name,
                MyTags.AzureInfrastructure
            )
            catCoder.uses(this, "reads and updates contest data", "JDBC")
        }

        val registrationDb = platform.addContainer("Registration-DB", "", "MariaDB").apply {
            addTags(
                MyTags.Database,
                AzureMonoIcons.Databases.AZURE_DATABASE_FOR_MARIA_DB.name,
                MyTags.AzureInfrastructure
            )
            registration.uses(this, "reads and updates user data", "JDBC")
        }

        val storage = platform.addContainer("Azure Storage", "", "Azure Storage").apply {
            addTags(AzureMonoIcons.Storage.AZURE_BLOB_STORAGE.name, MyTags.AzureInfrastructure)
            catCoder.uses(this, "provides access links to game input")
        }

        val redis = platform.addContainer("Redis", "", "Redis").apply {
            registration.uses(this, "stores sessions")
            catCoder.uses(this, "stores sessions")
            addTags(AzureMonoIcons.Databases.AZURE_REDIS_CACHE.name, MyTags.AzureInfrastructure)
        }

        val insights = platform.addContainer("Application Insights", "", "").apply {
            addTags(AzureMonoIcons.DevOps.AZURE_APPLICATION_INSIGHTS.name, MyTags.AzureInfrastructure)
        }

        val containerRegistry = platform.addContainer("Container Registry", "", "").apply {
            addTags(AzureMonoIcons.Containers.AZURE_CONTAINER_REGISTRY.name, MyTags.AzureInfrastructure)
        }

        val vault = platform.addContainer("Azure Key Vault", "", "").apply {
            addTags(AzureMonoIcons.Security.AZURE_KEY_VAULT.name, MyTags.AzureInfrastructure)
            registration.uses(this, "reads secrets")
            catCoder.uses(this, "reads secrets")
        }

        val ingress = platform.addContainer("NGINX Ingress", "Loadbalancer").apply {
            addTags(MyTags.AzureInfrastructure)
            this.uses(registration, "forwards requests", "HTTP")
            this.uses(catCoder, "forwards requests", "HTTP")
        }
        val grafana = platform.addContainer("Grafana", "").apply {
            addTags(MyTags.AzureInfrastructure)
        }

        val dns = platform.addContainer("DNS Zone").apply {
            addTags(AzureMonoIcons.Networking.AZURE_DNS.name, MyTags.AzureInfrastructure)
        }

        val publicIp = platform.addContainer("Public IP").apply {
            addTags(AzureMonoIcons.Networking.AZURE_PUBLIC_IPADDRESS.name, MyTags.AzureInfrastructure)
        }


        with(personas.user) {
            uses(registration, "performs login")
            uses(catCoder, "solves coding challenges")
            uses(storage, "downloads contest input data", "HTTPS")
        }
        with(personas.admin) {
            uses(registration, "creates public contests")
            uses(catCoder, "maintains coding games")
        }

        val azure = model.addDeploymentNode("Azure", "", "").apply {
            addTags(AzureMonoIcons.General.AZURE.name)
        }

        val aks = azure.addDeploymentNode("AKS", "", "AKS").apply {
            addTags(AzureMonoIcons.Containers.AZURE_KUBERNETES_SERVICE.name)
            add(catCoder).apply { addTags(DevIcons2.SPRING.name) }
            add(registration).apply { addTags(DevIcons2.SPRING.name) }
            add(ingress)
        }

        azure.addWithCustomTags(containerRegistry)
        azure.addWithCustomTags(storage)
        azure.addWithCustomTags(insights)
        azure.addWithCustomTags(vault)
        azure.addWithCustomTags(redis)
        azure.addWithCustomTags(grafana)
        azure.addWithCustomTags(dns)
        azure.addWithCustomTags(publicIp)

        azure.addDeploymentNode("managedMariaDb", "", "MariaDB").apply {
            addTags(MyTags.Database, AzureMonoIcons.Databases.AZURE_DATABASE_FOR_MARIA_DB.name)
            add(catCoderDb)
            add(registrationDb)
        }
    }

    override fun createViews(viewSet: ViewSet) {
        viewSet.createContainerView(platform, "ccp", "Coding Contest Platform").apply {
            addAllContainersAndInfluencers()
            addAllPeople()
        }

        viewSet.createContainerView(platform, "ccpNoAzure", "Coding Contest Platform without DB").apply {
            addAllContainersAndInfluencers()
            addAllPeople()
            removeElementsWithTag(MyTags.AzureInfrastructure)
        }

        viewSet.createDeploymentView(platform, "deployment", "").apply {
            addAllDeploymentNodes()
        }
    }
}

object MyTags {
    const val FileSystem = "FileSystem"

    const val Database = "Database"
    const val AzureInfrastructure = "AzureInfrastructure"
}
