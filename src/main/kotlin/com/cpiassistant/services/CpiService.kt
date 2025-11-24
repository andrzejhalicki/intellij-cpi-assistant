package com.cpiassistant.services

import FileNodeStateComponent
import com.cpiassistant.nodes.artifact.CpiArtifact
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.resource.CpiResource
import com.cpiassistant.nodes.artifact.CpiScriptCollection
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import okhttp3.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.*
import net.minidev.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException
import java.time.Instant

class CpiService(
    var clientID: String,
    var clientSecret: String,
    var url: String,
    var tokenUrl: String
) {

    private var accessToken: String? = null
    private var expirationTime: Instant? = null
    private var csrfToken: String? = null
    private val project: Project? by lazy { ProjectManager.getInstance().openProjects.firstOrNull() }
    private val fileNodeStateComponent by lazy { project?.service<FileNodeStateComponent>() }
    private val fileNodes by lazy { fileNodeStateComponent?.getFileNodes() }

    @OptIn(ExperimentalEncodingApi::class)
    fun authenticate(): Boolean {
        try {
            val formBody = FormBody.Builder()
                .add("grant_type", "client_credentials")
                .build()

            val usernameAndPassword = clientID + ":" + clientSecret
            val bytes = usernameAndPassword.toByteArray(charset("ISO-8859-1"))
            val encoded: String = Base64.encode(bytes)

            val request = Request.Builder()
                .url(tokenUrl)
                .header("Authorization", "Basic " + encoded)
                .header("AcceptEncoding", "gzip, deflate, br")
                .header("Accept", "*/*")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(formBody)
                .build()

            val client = OkHttpClient()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    NotificationService.getInstance()?.showError("Authentication Error", "Failed to authenticate: ${response.code}")
                    return false
                }

                val responseBody = response.body
                val tokenResponse = responseBody.string()
                accessToken = extractAccessToken(tokenResponse)
                expirationTime = extractExpirationTime(tokenResponse)
                return true
            }
        } catch (e: Exception) {
            NotificationService.getInstance()?.showError("Authentication Error", "An error occurred during authentication: ${e.message}")
        }
        return false
    }

    private fun fetchCSRFToken(callback: (String?) -> Unit) {
        try {
            val call = this.makeAuthenticatedRequest("GET", "/")
            val response = call.execute()
            val token = response.headers["X-CSRF-Token"]
            callback(token)
        } catch (e: Exception) {
            NotificationService.getInstance()?.showError("CSRF Token Error", "Failed to fetch CSRF token: ${e.message}")
            callback(null)
        }
    }

    fun getPackages(callback: (List<CpiPackage>) -> Unit) {
        try {
            val packages = mutableListOf<CpiPackage>()
            val call = this.makeAuthenticatedRequest("GET", "/IntegrationPackages")
            val response = call.execute()
            if (!response.isSuccessful) {
                NotificationService.getInstance()?.showError("Failed to get packages: ${response.code}")
                callback(emptyList())
                return
            }
            val results = getResultFromJson(response)
            response.body.close()
            results.forEach {
                val packageId = it.jsonObject["Id"]?.jsonPrimitive?.content ?: ""
                val cpiPackage = CpiPackage(packageId, it.jsonObject["Name"]?.jsonPrimitive?.content ?: "", this)
                packages.add(cpiPackage)
            }
            this.fetchCSRFToken { token ->
                this.csrfToken = token
            }
            callback(packages)
        } catch (e: Exception) {
            NotificationService.getInstance()?.showError("Error", "An error occurred while getting packages: ${e.message}")
            callback(emptyList())
        }

    }

    fun getArtifacts(packageId: String, callback: (List<CpiArtifact>) -> Unit) {
        try {
            val artifacts = mutableListOf<CpiArtifact>()
            val call =
                this.makeAuthenticatedRequest(
                    "GET",
                    "/IntegrationPackages('${packageId}')/IntegrationDesigntimeArtifacts"
                )
            val response = call.execute()
            if (!response.isSuccessful) {
                NotificationService.getInstance()?.showError("Failed to get artifacts: ${response.code}")
                callback(emptyList())
                return
            }
            val results = getResultFromJson(response)
            response.body.close()
            results.forEach {
                val artifactId = it.jsonObject["Id"]?.jsonPrimitive?.content ?: ""
                val artifact = CpiArtifact(artifactId, it.jsonObject["Name"]?.jsonPrimitive?.content ?: "", this)
                //artifact.setResources(getResources(artifactId))
                artifacts.add(artifact)
            }
            callback(artifacts)
        } catch (e: Exception) {
            NotificationService.getInstance()?.showError("Error", "An error occurred while getting artifacts: ${e.message}")
            callback(emptyList())
        }
    }

    fun getScriptCollections(packageId: String, callback: (List<CpiScriptCollection>) -> Unit) {
        try {
            val collections = mutableListOf<CpiScriptCollection>()
            val call = this.makeAuthenticatedRequest(
                "GET",
                "/IntegrationPackages('${packageId}')/ScriptCollectionDesigntimeArtifacts"
            )
            val response = call.execute()
            if (!response.isSuccessful) {
                NotificationService.getInstance()?.showError("Failed to get script collections: ${response.code}")
                callback(emptyList())
                return
            }
            val results = getResultFromJson(response)
            response.body.close()
            results.forEach {
                val collectionId = it.jsonObject["Id"]?.jsonPrimitive?.content ?: ""
                val collection =
                    CpiScriptCollection(collectionId, it.jsonObject["Name"]?.jsonPrimitive?.content ?: "", this)
                //artifact.setResources(getResources(artifactId))
                collections.add(collection)
            }
            callback(collections)
        } catch (e: Exception) {
            NotificationService.getInstance()?.showError("Error", "An error occurred while getting script collections: ${e.message}")
            callback(emptyList())
        }
    }

    fun getResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        val scripts = this.getResourcesInternal(
            artifactId,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources?\$filter=ResourceType eq 'groovy'"
        )
        val xslts = this.getResourcesInternal(
            artifactId,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources?\$filter=ResourceType eq 'xslt'"
        )
        callback(scripts + xslts)
    }

    fun getResource(artifactId: String, resource: CpiResource, callback: (String) -> Unit) {
        val downloadedResource = this.getResourceInternal(
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources(Name='${resource.name}',ResourceType='${resource.resourceType.value}')/\$value"
        )
        callback(downloadedResource)
    }

    fun getScriptCollectionResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        val resources = this.getResourcesInternal(
            artifactId,
            "/ScriptCollectionDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources"
        )
        callback(resources)
    }

    fun getScriptCollectionResource(artifactId: String, resourceName: String, callback: (String) -> Unit) {
        val resource = this.getResourceInternal(
            "/ScriptCollectionDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources(Name='${resourceName}',ResourceType='groovy')/\$value"
        )
        callback(resource)
    }

    fun createResource(artifactId: String, resource: CpiResource, content: String, callback: (Boolean) -> Unit) {
        this.createResourceInternal(
            artifactId,
            resource.name,
            resource.resourceType.value,
            content,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources"
        ) { res ->
            callback(res)
        }
    }

    fun createScriptCollectionResource(artifactId: String, resource: CpiResource, content: String, callback: (Boolean) -> Unit) {
        this.createResourceInternal(
            artifactId,
            resource.name,
            resource.resourceType.value,
            content,
            "/ScriptCollectionDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources"
        ) { res ->
            callback(res)
        }
    }

    fun updateResource(artifactId: String, resource: CpiResource, content: String, callback: (Boolean,String) -> Unit) {
        this.updateResourceInternal(
            artifactId,
            resource.name,
            content,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/\$links/Resources(Name='${resource.name}',ResourceType='${resource.resourceType.value}')"
        ) { success, message ->
            callback(success, message)
        }
    }

    fun updateScriptCollectionResource(artifactId: String, name: String, content: String, callback: (Boolean,String) -> Unit) {
        this.updateResourceInternal(
            artifactId,
            name,
            content,
            "/ScriptCollectionDesigntimeArtifacts(Id='${artifactId}',Version='active')/\$links/Resources(Name='${name}',ResourceType='groovy')"
        ) { success, message ->
            callback(success, message)
        }
    }

    fun deployArtifact(artifactId: String, callback: (String) -> Unit) {
        val call = this.makeAuthenticatedRequest(
            "POST",
            "/DeployIntegrationDesigntimeArtifact?Id='${artifactId}'&Version='active'"
        )
        val response = call.execute()
        val taskId = response.body.string()
        response.body.close()
        callback(taskId)
    }

    fun deployScriptCollection(artifactId: String, callback: (String) -> Unit) {
        val call = this.makeAuthenticatedRequest(
            "POST",
            "/DeployScriptCollectionDesigntimeArtifact?Id='${artifactId}'&Version='active'"
        )
        val response = call.execute()
        val taskId = response.body.string()
        response.body.close()
        callback(taskId)
    }

    fun checkDeploymentStatus(taskId: String, callback: (String, Boolean) -> Unit) {
        val call = this.makeAuthenticatedRequest("GET", "/BuildAndDeployStatus(TaskId='${taskId}')")
        val response = call.execute()
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = json.parseToJsonElement(response.body.string()).jsonObject
        response.body.close()
        if (response.isSuccessful) {
            val d = jsonObject["d"] as JsonObject
            callback(d["Status"]?.jsonPrimitive?.content ?: "", true)
        } else {
            val error = jsonObject["error"] as JsonObject
            val message = error["message"] as JsonObject
            val value = message["value"]?.jsonPrimitive?.content ?: ""
            callback(value, false)
        }
    }

    companion object {
        fun getResultFromJson(response: Response): JsonArray {
            val json = Json { ignoreUnknownKeys = true }
            val jsonObject = json.parseToJsonElement(response.body.string()).jsonObject
            response.body.close()
            val d = jsonObject["d"] as? JsonObject ?: throw IllegalStateException("Unexpected response format")
            return d["results"] as? JsonArray ?: throw IllegalStateException("Results not found in response")
        }
    }

    private fun extractAccessToken(tokenResponse: String): String {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = json.parseToJsonElement(tokenResponse).jsonObject
        return jsonObject["access_token"]?.jsonPrimitive?.content
            ?: throw IllegalStateException("Access token not found in response")
    }

    private fun extractExpirationTime(tokenResponse: String): Instant {
        val json = Json { ignoreUnknownKeys = true }
        val jsonObject = json.parseToJsonElement(tokenResponse).jsonObject
        val expiresIn = jsonObject["expires_in"]?.jsonPrimitive?.content?.toLong()
        return Instant.now().plusSeconds(expiresIn!!)
    }

    private fun isTokenExpired(): Boolean {
        return expirationTime?.isBefore(Instant.now()) ?: true
    }

    @Throws(IOException::class)
    fun makeAuthenticatedRequest(method: String, endpoint: String, requestBody: RequestBody? = null): Call {
        if (accessToken == null) {
            throw IllegalStateException("Not authenticated. Call authenticate() first.")
        }

        if (isTokenExpired()) {
            authenticate()
        }

        val requestBuilder = Request.Builder()
            .url("${url}/api/v1" + endpoint)
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")

        if (method == "POST") {
            this.csrfToken?.let { requestBuilder.header("X-CSRF-Token", it) }
            requestBuilder.method("POST", requestBody ?: FormBody.Builder().build())
        } else {
            requestBuilder.method(method, requestBody)
        }

        val client = OkHttpClient()
        return client.newCall(requestBuilder.build())
    }

    fun getResourcePath(artifactId: String, resourceName: String): String? {
        return fileNodes?.find { node -> node.artifactId == artifactId && node.name == resourceName }?.path
    }

    private fun getResourcesInternal(artifactId: String, endpoint: String): List<CpiResource> {
        val resources = mutableListOf<CpiResource>()
        val call = this.makeAuthenticatedRequest(
            "GET",
            endpoint
        )
        val response = call.execute()
        val results = getResultFromJson(response)
        response.body.close()
        results.forEach {
            val resourceName = it.jsonObject["Name"]?.jsonPrimitive?.content ?: ""
            val resource = CpiResource.create(it.jsonObject["Id"]?.jsonPrimitive?.content ?: "", resourceName, "", artifactId)
            val path = getResourcePath(artifactId,resourceName)
            resource.path = path ?: ""
            resources.add(resource)
        }
        return resources
    }

    private fun getResourceInternal(endpoint: String): String {
        val call = this.makeAuthenticatedRequest(
            "GET",
            endpoint
        )
        val response = call.execute()
        val result = response.body.string()
        response.body.close()
        return result
    }

    fun createResourceInternal(
        @Suppress("UNUSED_PARAMETER") artifactId: String,
        name: String,
        suffix: String,
        content: String,
        endpoint: String,
        callback: (Boolean) -> Unit
    ) {
        val jsonObject = JSONObject()
        jsonObject.put("Name", name)
        jsonObject.put("ResourceType", suffix)
        jsonObject.put("ResourceContent", content)
        val jsonString = jsonObject.toString()

        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
        val call = this.makeAuthenticatedRequest("POST", endpoint, requestBody)
        val response = call.execute()
        val res = response.isSuccessful
        response.body.close()
        callback(res)
    }

    fun updateResourceInternal(
        @Suppress("UNUSED_PARAMETER") artifactId: String,
        @Suppress("UNUSED_PARAMETER") name: String,
        content: String,
        endpoint: String,
        callback: (Boolean,String) -> Unit
    ) {
        val jsonObject = JSONObject()
        jsonObject.put("ResourceContent", content)
        val jsonString = jsonObject.toString()

        val requestBody = jsonString.toRequestBody("application/json".toMediaTypeOrNull())
        val call = this.makeAuthenticatedRequest("PUT", endpoint, requestBody)
        val response = call.execute()
        response.body.close()
        callback(response.isSuccessful,response.message)
    }

}