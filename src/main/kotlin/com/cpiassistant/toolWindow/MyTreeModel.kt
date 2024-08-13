package com.cpiassistant.toolWindow

import TenantStateComponent
import com.cpiassistant.nodes.Tenant
import com.cpiassistant.services.CpiService
import com.cpiassistant.toolWindow.elements.CustomNode
import com.intellij.openapi.components.service
import com.intellij.util.ui.ColumnInfo
import org.jetbrains.annotations.Nullable
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

class MyTreeModel() {

    private val tenants = mutableListOf<Tenant>()

    fun getModels(): List<DefaultMutableTreeNode> {
        val roots = getTenantNodes()
        return roots
    }

    fun getTenantNodes(): List<DefaultMutableTreeNode> {
        val tenantStateComponent = service<TenantStateComponent>()
        val tenantsState = tenantStateComponent.getTenants()
        tenantsState.forEach { tenant ->
            val newTenant = Tenant(tenant.name,tenant.name,CpiService(tenant.clientID, tenant.clientSecret, tenant.url, tenant.tokenUrl))
            val isAuthenticated = newTenant.service.authenticate();
            newTenant.isConnected = isAuthenticated
            this.tenants.add(newTenant)
        }

        return this.tenants.map { DefaultMutableTreeNode(it) }
    }

}