package com.cpiassistant.services

import FileNodeStateComponent
import com.cpiassistant.nodes.CpiArtifact
import com.cpiassistant.nodes.CpiPackage
import com.cpiassistant.nodes.CpiResource
import com.cpiassistant.nodes.CpiScriptCollection
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.components.service
import kotlinx.serialization.*
import okhttp3.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.serialization.json.*
import net.minidev.json.JSONObject
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okio.IOException

class CpiService(
    var clientID: String,
    var clientSecret: String,
    var url: String,
    var tokenUrl: String
) {

    private var accessToken: String? = null
    private var csrfToken: String? = null
    private val fileNodeStateComponent = service<FileNodeStateComponent>()
    private val fileNodes = fileNodeStateComponent.getFileNodes()

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
                    Notifications.Bus.notify(
                        Notification(
                            "Custom Notification Group",
                            "Authentication Error",
                            "Failed to authenticate: ${response.code}",
                            NotificationType.ERROR
                        )
                    )
                    return false
                }

                val responseBody = response.body
                responseBody?.let {
                    val tokenResponse = it.string()
                    accessToken = extractAccessToken(tokenResponse)
                    return true
                }
            }
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Authentication Error",
                    "An error occurred during authentication: ${e.message}",
                    NotificationType.ERROR
                )
            )
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
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "CSRF Token Error",
                    "Failed to fetch CSRF token: ${e.message}",
                    NotificationType.ERROR
                )
            )
            callback(null)
        }
    }

    fun getPackages(callback: (List<CpiPackage>) -> Unit) {
        try {
            val packages = mutableListOf<CpiPackage>()
            val call = this.makeAuthenticatedRequest("GET", "/IntegrationPackages")
            val response = call.execute()
            if (!response.isSuccessful) {
                Notifications.Bus.notify(
                    Notification(
                        "Custom Notification Group",
                        "Failed to get packages: ${response.code}",
                        NotificationType.ERROR
                    )
                )
                callback(emptyList())
                return
            }
            val results = getResultFromJson(response)
            response.body.close()
            results.forEach {
                val packageId = it.jsonObject["Id"].toString().replace("\"", "")
                val cpiPackage = CpiPackage(packageId, it.jsonObject["Name"].toString().replace("\"", ""), this)
                packages.add(cpiPackage)
            }
            this.fetchCSRFToken { token ->
                this.csrfToken = token
            }
            callback(packages)
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Error",
                    "An error occurred while getting packages: ${e.message}",
                    NotificationType.ERROR
                )
            )
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
                Notifications.Bus.notify(
                    Notification(
                        "Custom Notification Group",
                        "Failed to get artifacts: ${response.code}",
                        NotificationType.ERROR
                    )
                )
                callback(emptyList())
                return
            }
            val results = getResultFromJson(response)
            response.body.close()
            results.forEach {
                val artifactId = it.jsonObject["Id"].toString().replace("\"", "")
                val artifact = CpiArtifact(artifactId, it.jsonObject["Name"].toString().replace("\"", ""), this)
                //artifact.setResources(getResources(artifactId))
                artifacts.add(artifact)
            }
            callback(artifacts)
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "Error",
                    "An error occurred while getting artifacts: ${e.message}",
                    NotificationType.ERROR
                )
            )
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
                Notifications.Bus.notify(
                    Notification(
                        "Custom Notification Group",
                        "Failed to get script collections: ${response.code}",
                        NotificationType.ERROR
                    )
                )
                callback(emptyList())
                return
            }
            val results = getResultFromJson(response)
            response.body.close()
            results.forEach {
                val collectionId = it.jsonObject["Id"].toString().replace("\"", "")
                val collection =
                    CpiScriptCollection(collectionId, it.jsonObject["Name"].toString().replace("\"", ""), this)
                //artifact.setResources(getResources(artifactId))
                collections.add(collection)
            }
            callback(collections)
        } catch (e: Exception) {
            Notifications.Bus.notify(
                Notification(
                    "Custom Notification Group",
                    "An error occurred while getting script collections: ${e.message}",
                    NotificationType.ERROR
                )
            )
            callback(emptyList())
        }
    }

    fun getResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        val resources = this.getResourcesInternal(
            artifactId,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources?\$filter=ResourceType eq 'groovy'"
        )
        callback(resources)
    }

    fun getScriptCollectionResources(artifactId: String, callback: (List<CpiResource>) -> Unit) {
        val resources = this.getResourcesInternal(
            artifactId,
            "/ScriptCollectionDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources"
        )
        callback(resources)
    }

    fun createResource(artifactId: String, name: String, content: String, callback: (Boolean) -> Unit) {
        this.createResourceInternal(
            artifactId,
            name,
            content,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources"
        ) { res ->
            callback(res)
        }
    }

    fun createScriptCollectionResource(artifactId: String, name: String, content: String, callback: (Boolean) -> Unit) {
        this.createResourceInternal(
            artifactId,
            name,
            content,
            "/ScriptCollectionDesigntimeArtifacts(Id='${artifactId}',Version='active')/Resources"
        ) { res ->
            callback(res)
        }
    }

    fun updateResource(artifactId: String, name: String, content: String, callback: (Boolean,String) -> Unit) {
        this.updateResourceInternal(
            artifactId,
            name,
            content,
            "/IntegrationDesigntimeArtifacts(Id='${artifactId}',Version='active')/\$links/Resources(Name='${name}',ResourceType='groovy')"
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
            callback(d["Status"].toString().replace("\"", ""), true)
        } else {
            val error = jsonObject["error"] as JsonObject
            val message = error["message"] as JsonObject
            val value = message["value"].toString().replace("\"", "")
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

    @Throws(IOException::class)
    fun makeAuthenticatedRequest(method: String, endpoint: String, requestBody: RequestBody? = null): Call {
        if (accessToken == null) {
            throw IllegalStateException("Not authenticated. Call authenticate() first.")
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
            val resourceName = it.jsonObject["Name"].toString().replace("\"", "")
            val resource = CpiResource(it.jsonObject["Id"].toString().replace("\"", ""), resourceName, "", artifactId)
            val path = fileNodes.find { node -> node.artifactId == artifactId && node.name == resourceName }?.path
            resource.path = path ?: ""
            resources.add(resource)
        }
        return resources
    }

    fun createResourceInternal(
        artifactId: String,
        name: String,
        content: String,
        endpoint: String,
        callback: (Boolean) -> Unit
    ) {
        val jsonObject = JSONObject()
        jsonObject.put("Name", name)
        jsonObject.put("ResourceType", "groovy")
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
        artifactId: String,
        name: String,
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
        val res = response.isSuccessful
        response.body.close()
        callback(response.isSuccessful,response.message)
    }

}