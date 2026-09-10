package com.waheed.artificerx.core.ui
sealed interface UiEvent { data object ClearError:UiEvent; data class ShowMessage(val text:String):UiEvent; data class SetBusy(val value:Boolean):UiEvent }
data class UiState(val busy:Boolean=false,val message:String?=null,val error:String?=null)
class UiEventReducer {
    fun reduce(state:UiState,event:UiEvent):UiState=when(event){
        UiEvent.ClearError->state.copy(error=null)
        is UiEvent.ShowMessage->state.copy(message=event.text,error=null)
        is UiEvent.SetBusy->state.copy(busy=event.value)
    }
}
