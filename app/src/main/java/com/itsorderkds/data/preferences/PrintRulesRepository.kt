package com.itsorderkds.data.preferences

import com.itsorderkds.data.model.PrintStatusRule
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repozytorium reguł drukowania – serializuje/deserializuje
 * [PrintStatusRule] do/z DataStore (przez [AppPreferencesManager]).
 */
@Singleton
class PrintRulesRepository @Inject constructor(
    private val prefs: AppPreferencesManager
) {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    suspend fun getRules(): List<PrintStatusRule> {
        val raw = prefs.getPrintStatusRulesJson()
        if (raw.isBlank()) return emptyList()
        return try {
            json.decodeFromString(ListSerializer(PrintStatusRule.serializer()), raw)
        } catch (e: Exception) {
            Timber.w(e, "PrintRulesRepository: błąd deserializacji reguł")
            emptyList()
        }
    }

    suspend fun saveRules(rules: List<PrintStatusRule>) {
        val encoded = json.encodeToString(ListSerializer(PrintStatusRule.serializer()), rules)
        prefs.setPrintStatusRulesJson(encoded)
    }

    suspend fun addRule(rule: PrintStatusRule) {
        val current = getRules().toMutableList()
        current.add(rule)
        saveRules(current)
    }

    suspend fun updateRule(rule: PrintStatusRule) {
        val current = getRules().map { if (it.id == rule.id) rule else it }
        saveRules(current)
    }

    suspend fun deleteRule(ruleId: String) {
        val current = getRules().filter { it.id != ruleId }
        saveRules(current)
    }
}

