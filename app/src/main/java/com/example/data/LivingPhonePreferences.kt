package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.model.ModuleType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LivingPhonePreferences(context: Context) {

    companion object {
        private const val PREFS_NAME = "living_phone_settings"
        private const val KEY_SERVICE_ENABLED = "key_service_enabled"
        private const val KEY_MODULE_PREFIX = "key_module_"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _modulesStateFlow = MutableStateFlow<Map<ModuleType, Boolean>>(loadAllModules())
    val modulesStateFlow: StateFlow<Map<ModuleType, Boolean>> = _modulesStateFlow.asStateFlow()

    private val _serviceEnabledFlow = MutableStateFlow(prefs.getBoolean(KEY_SERVICE_ENABLED, false))
    val serviceEnabledFlow: StateFlow<Boolean> = _serviceEnabledFlow.asStateFlow()

    private fun loadAllModules(): Map<ModuleType, Boolean> {
        val map = mutableMapOf<ModuleType, Boolean>()
        for (module in ModuleType.values()) {
            val key = "$KEY_MODULE_PREFIX${module.name}"
            map[module] = prefs.getBoolean(key, module.defaultEnabled)
        }
        return map
    }

    fun isModuleEnabled(module: ModuleType): Boolean {
        return _modulesStateFlow.value[module] ?: module.defaultEnabled
    }

    fun setModuleEnabled(module: ModuleType, enabled: Boolean) {
        prefs.edit().putBoolean("$KEY_MODULE_PREFIX${module.name}", enabled).apply()
        val current = _modulesStateFlow.value.toMutableMap()
        current[module] = enabled
        _modulesStateFlow.value = current
    }

    fun setServiceEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SERVICE_ENABLED, enabled).apply()
        _serviceEnabledFlow.value = enabled
    }
}
