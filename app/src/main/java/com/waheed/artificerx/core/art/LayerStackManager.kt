package com.waheed.artificerx.core.art
data class LayerState(val id:String,val name:String,val visible:Boolean=true,val opacity:Float=1f,val locked:Boolean=false)
class LayerStackManager(initial:List<LayerState> = emptyList()) {
    private val layers=initial.toMutableList()
    fun all():List<LayerState> = layers.toList()
    fun add(layer:LayerState,index:Int=layers.size){require(layers.none{it.id==layer.id}); layers.add(index.coerceIn(0,layers.size),layer)}
    fun remove(id:String):LayerState?=layers.firstOrNull{it.id==id}?.also{layers.remove(it)}
    fun move(id:String,newIndex:Int):Boolean { val i=layers.indexOfFirst{it.id==id}; if(i<0)return false; val l=layers.removeAt(i); layers.add(newIndex.coerceIn(0,layers.size),l); return true }
    fun update(id:String,transform:(LayerState)->LayerState):Boolean { val i=layers.indexOfFirst{it.id==id}; if(i<0)return false; layers[i]=transform(layers[i]); return true }
}
