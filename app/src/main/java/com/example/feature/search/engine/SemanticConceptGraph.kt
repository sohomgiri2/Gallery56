package com.example.feature.search.engine

import java.util.Locale

object SemanticConceptGraph {

    private val conceptClusters: Map<String, List<String>> = mapOf(
        "pet" to listOf("dog", "cat", "puppy", "kitten", "animal", "mammal", "paw", "fur", "canine", "feline"),
        "dog" to listOf("puppy", "canine", "pet", "animal", "mammal", "hound", "terrier"),
        "cat" to listOf("kitten", "feline", "pet", "animal", "mammal", "whiskers"),
        "food" to listOf("meal", "dish", "cuisine", "restaurant", "plate", "tableware", "snack", "dessert", "fruit", "vegetable", "coffee", "drink", "baked", "pizza", "bread"),
        "coffee" to listOf("cafe", "espresso", "latte", "cup", "mug", "drink", "beverage", "breakfast"),
        "nature" to listOf("sky", "mountain", "tree", "forest", "landscape", "outdoor", "plant", "leaf", "flower", "grass", "water", "lake", "river", "ocean", "hill", "rock"),
        "travel" to listOf("mountain", "beach", "ocean", "sunset", "airplane", "architecture", "landmark", "road", "bridge", "city", "hotel", "sky", "outdoor", "luggage"),
        "beach" to listOf("ocean", "sea", "sand", "coast", "shore", "water", "wave", "summer", "vacation"),
        "sunset" to listOf("sunrise", "dusk", "dawn", "sky", "cloud", "horizon", "afterglow", "golden"),
        "night" to listOf("dark", "evening", "moon", "star", "neon", "street", "light", "cityscape"),
        "city" to listOf("building", "architecture", "skyscraper", "street", "urban", "road", "bridge", "traffic"),
        "car" to listOf("vehicle", "automobile", "wheel", "tire", "road", "sedan", "suv", "truck", "motorcycle", "transport"),
        "receipt" to listOf("invoice", "total", "subtotal", "tax", "payment", "store", "bill", "paper", "text", "document"),
        "document" to listOf("paper", "text", "receipt", "invoice", "letter", "page", "form", "note", "id", "certificate", "book"),
        "work" to listOf("laptop", "computer", "desk", "keyboard", "monitor", "office", "meeting", "document", "screen"),
        "people" to listOf("person", "face", "portrait", "smile", "human", "selfie", "crowd", "man", "woman", "child"),
        "portrait" to listOf("face", "person", "selfie", "headshot", "smile", "hair", "eye"),
        "sport" to listOf("ball", "stadium", "athlete", "running", "bicycle", "fitness", "gym", "team", "grass", "field"),
        "party" to listOf("celebration", "cake", "candle", "balloon", "event", "music", "crowd", "night"),
        "flower" to listOf("petal", "blossom", "plant", "garden", "rose", "botanical", "spring"),
        "water" to listOf("ocean", "sea", "lake", "river", "waterfall", "pool", "rain", "wave"),
    )

    fun tokenize(raw: String): List<String> {
        return raw.lowercase(Locale.US)
            .split(Regex("[^a-z0-9_]+"))
            .map { it.trim() }
            .filter { it.length >= 2 }
            .distinct()
    }

    fun expandTokens(tokens: List<String>, enabled: Boolean): List<String> {
        if (!enabled || tokens.isEmpty()) return tokens
        val expanded = LinkedHashSet<String>(tokens)
        for (token in tokens) {
            val stem = simpleStem(token)
            if (stem != token) expanded.add(stem)

            conceptClusters[token]?.forEach { expanded.add(it) }
            conceptClusters[stem]?.forEach { expanded.add(it) }
            // Reverse cluster lookup: if user types "puppy", also include "pet" & "dog"
            for ((key, related) in conceptClusters) {
                if (token in related || stem in related) {
                    expanded.add(key)
                    related.take(5).forEach { expanded.add(it) }
                }
            }
        }
        return expanded.toList()
    }

    fun buildSafeFtsMatchQuery(tokens: List<String>): String? {
        val clean = tokens
            .map { it.replace(Regex("[^a-zA-Z0-9_]"), "") }
            .filter { it.length >= 2 }
            .distinct()
            .take(24)
        if (clean.isEmpty()) return null
        return clean.joinToString(" OR ") { "${it}*" }
    }

    private fun simpleStem(word: String): String {
        return when {
            word.endsWith("ies") && word.length > 4 -> word.dropLast(3) + "y"
            word.endsWith("es") && word.length > 4 -> word.dropLast(2)
            word.endsWith("s") && word.length > 3 && !word.endsWith("ss") -> word.dropLast(1)
            word.endsWith("ing") && word.length > 5 -> word.dropLast(3)
            else -> word
        }
    }
}
