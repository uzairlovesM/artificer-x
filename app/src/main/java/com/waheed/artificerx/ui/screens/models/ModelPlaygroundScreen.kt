package com.waheed.artificerx.ui.screens.models

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.waheed.artificerx.data.repository.ProviderConfigRepository
import com.waheed.artificerx.domain.model.AiProviderConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class ModelPlaygroundViewModel @Inject constructor(repo: ProviderConfigRepository) : ViewModel() {
    val providers: StateFlow<List<AiProviderConfig>> = repo.configs.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

@Composable
fun ModelPlaygroundScreen(onBack: () -> Unit, vm: ModelPlaygroundViewModel = hiltViewModel()) {
    val providers by vm.providers.collectAsState()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth()) { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }; Text("Model Playground", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 10.dp)) }
        Text("Routing-aware provider inventory")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(providers, key = { it.id }) { p -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp)) { Text(p.displayName, style = MaterialTheme.typography.titleMedium); Text("${p.type} • ${p.defaultModelId ?: "automatic model"}"); Text("Vision ${p.supportsVision} • Tools ${p.supportsToolCalling} • quota ${p.usageTodayCallCount}/${p.knownDailyQuota ?: "∞"}") } } } } }
}
