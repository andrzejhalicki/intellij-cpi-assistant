package com.cpiassistant.toolWindow

import TenantStateComponent
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.services.CpiService
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import javax.swing.tree.DefaultMutableTreeNode

class MyTreeModel() {

    private val tenants = mutableListOf<Tenant>()
    private val project: Project? = ProjectManager.getInstance().openProjects.firstOrNull()

    fun getModels(): List<DefaultMutableTreeNode> {
        val roots = getTenantNodes()
        return roots
    }

    fun getTenantNodes(): List<DefaultMutableTreeNode> {
        val tenantStateComponent = project?.service<TenantStateComponent>()
        val tenantsState = tenantStateComponent?.getTenants()
        tenantsState?.forEach { tenant ->
            val newTenant = Tenant(tenant.name,tenant.name,CpiService(tenant.clientID, tenant.clientSecret, tenant.url, tenant.tokenUrl))
            val isAuthenticated = newTenant.service.authenticate();
            newTenant.isConnected = isAuthenticated
            this.tenants.add(newTenant)
        }

        return this.tenants.map { DefaultMutableTreeNode(it) }
    }

}