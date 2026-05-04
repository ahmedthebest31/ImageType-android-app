package com.ahmedsamy.imagetype.util

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

data class Template(
    val id: String,
    val name: String,
    val text: String,
    val fitText: Boolean,
    val fontSize: Float,
    val textPosition: String,
    val fontFamily: String,
    val fontStyle: String,
    val textColor: String,
    val textShadow: Boolean,
    val dimensions: String,
    val bgType: String,
    val bgColor: String,
    val imageQuality: String
)

class TemplateSerializer {
    private val templateListType = Types.newParameterizedType(List::class.java, Template::class.java)
    private val templatesAdapter = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
        .adapter<List<Template>>(templateListType)

    fun fromJson(json: String): List<Template> {
        return templatesAdapter.fromJson(json) ?: emptyList()
    }

    fun toJson(templates: List<Template>): String {
        return templatesAdapter.toJson(templates)
    }
}
