package com.waheed.artificerx.core.scene

data class SceneNode(
    val id: String,
    val name: String,
    val children: List<SceneNode> = emptyList(),
    val visible: Boolean = true,
    val locked: Boolean = false
)

class SceneGraph(private val root: SceneNode) {
    fun flatten(): List<SceneNode> {
        val output = mutableListOf<SceneNode>()
        fun visit(node: SceneNode) { output += node; node.children.forEach(::visit) }
        visit(root)
        return output
    }

    fun find(id: String): SceneNode? = flatten().firstOrNull { it.id == id }
    fun visibleNodes(): List<SceneNode> = flatten().filter { it.visible }
}
