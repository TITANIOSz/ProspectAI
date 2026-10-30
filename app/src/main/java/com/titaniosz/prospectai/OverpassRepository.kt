package com.titaniosz.prospectai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class BusinessLead(
    val id: String, val name: String, val category: String, val phone: String?,
    val address: String, val website: String?, val latitude: Double, val longitude: Double,
    val imageUrl: String?
) {
    val hasWebsite: Boolean get() = !website.isNullOrBlank()
}

object OverpassRepository {
    private val endpoints = listOf(
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass-api.de/api/interpreter"
    )

    suspend fun findBusinesses(latitude: Double, longitude: Double, radiusMeters: Int = 30_000): List<BusinessLead> =
        withContext(Dispatchers.IO) {
            val query = """
                [out:json][timeout:10];
                (
                  nwr(around:$radiusMeters,$latitude,$longitude)[name][~"^(shop|office|craft|amenity|healthcare|tourism|leisure|industrial)$"~".",i];
                  nwr(around:$radiusMeters,$latitude,$longitude)[name][landuse=industrial];
                  nwr(around:$radiusMeters,$latitude,$longitude)[name][man_made=works];
                );
                out center tags 300;
            """.trimIndent()
            var lastError: Exception? = null
            for (endpoint in endpoints) {
                var connection: HttpURLConnection? = null
                try {
                    connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "POST"
                        connectTimeout = 4_000
                        readTimeout = 10_000
                        doOutput = true
                        setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        setRequestProperty("User-Agent", "ProspectAI/0.3 (Android)")
                    }
                    val body = "data=" + URLEncoder.encode(query, "UTF-8")
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    val status = connection.responseCode
                    if (status !in 200..299) throw IllegalStateException("Servidor de mapas indisponível (HTTP $status).")
                    val response = connection.inputStream.bufferedReader().use { it.readText() }
                    val elements = JSONObject(response).optJSONArray("elements")
                        ?: throw IllegalStateException("A fonte de mapas retornou uma resposta inválida.")
                    val leads = ArrayList<BusinessLead>(elements.length())
                    for (i in 0 until elements.length()) {
                        val item = elements.optJSONObject(i) ?: continue
                        val tags = item.optJSONObject("tags") ?: continue
                        val name = tags.optString("name").trim()
                        if (name.isBlank()) continue
                        val center = item.optJSONObject("center")
                        val lat = if (item.has("lat")) item.optDouble("lat") else center?.optDouble("lat") ?: Double.NaN
                        val lon = if (item.has("lon")) item.optDouble("lon") else center?.optDouble("lon") ?: Double.NaN
                        if (!lat.isFinite() || !lon.isFinite()) continue
                        val website = tags.optString("website").takeIf { it.isNotBlank() }
                            ?: tags.optString("contact:website").takeIf { it.isNotBlank() }
                        val phone = tags.optString("phone").takeIf { it.isNotBlank() }
                            ?: tags.optString("contact:phone").takeIf { it.isNotBlank() }
                        val street = tags.optString("addr:street")
                        val number = tags.optString("addr:housenumber")
                        val address = listOf(
                            listOf(street, number).filter { it.isNotBlank() }.joinToString(", "),
                            tags.optString("addr:suburb"), tags.optString("addr:city")
                        ).filter { it.isNotBlank() }.joinToString(" — ").ifBlank { "Endereço não informado" }
                        val category = when {
                            tags.has("shop") -> "Comércio: " + tags.optString("shop")
                            tags.has("office") -> "Escritório: " + tags.optString("office")
                            tags.has("craft") -> "Serviço: " + tags.optString("craft")
                            tags.has("healthcare") -> "Saúde: " + tags.optString("healthcare")
                            tags.has("tourism") -> "Turismo: " + tags.optString("tourism")
                            tags.has("leisure") -> "Lazer: " + tags.optString("leisure")
                            tags.has("industrial") -> "Indústria: " + tags.optString("industrial")
                            tags.optString("landuse") == "industrial" -> "Área industrial"
                            tags.optString("man_made") == "works" -> "Unidade industrial"
                            else -> "Estabelecimento: " + tags.optString("amenity", "outro")
                        }
                        val rawImage = tags.optString("image").takeIf { it.startsWith("https://") || it.startsWith("http://") }
                        val commons = tags.optString("wikimedia_commons").takeIf { it.startsWith("File:") }
                        val imageUrl = rawImage ?: commons?.let {
                            "https://commons.wikimedia.org/wiki/Special:FilePath/" + URLEncoder.encode(it.removePrefix("File:"), "UTF-8")
                        }
                        leads += BusinessLead(
                            id = item.optString("type") + "/" + item.optString("id"),
                            name = name, category = category, phone = phone, address = address,
                            website = website, latitude = lat, longitude = lon, imageUrl = imageUrl
                        )
                    }
                    return@withContext leads.distinctBy { it.id }
                        .sortedWith(compareBy<BusinessLead> { it.hasWebsite }.thenBy { it.name.lowercase() })
                } catch (error: Exception) {
                    lastError = error
                } finally {
                    connection?.disconnect()
                }
            }
            throw IllegalStateException(
                "A pesquisa excedeu o tempo nos servidores de mapas. Confira a internet e tente novamente.",
                lastError
            )
        }
}
