package com.waheed.artificerx.art.layer

data class LayerNode(val id:String,val name:String,val parentId:String?,val visible:Boolean=true,val locked:Boolean=false,val opacity:Float=1f,val blendMode:String="NORMAL")
class LayerGraph(private val nodes:MutableMap<String,LayerNode> = linkedMapOf()) {
    fun upsert(node:LayerNode){nodes[node.id]=node}
    fun children(parentId:String?):List<LayerNode> = nodes.values.filter { it.parentId==parentId }
    fun snapshot():List<LayerNode> = nodes.values.toList()
}
