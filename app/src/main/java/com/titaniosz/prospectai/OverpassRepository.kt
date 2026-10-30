package com.titaniosz.prospectai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class BusinessLead(
    val id: String,
    val name: String,
    val category: String,
    val phone: String?,
    val address: String,
    val website: String?,
    val latitude: Double,
    val longitude: Double
) {
    val hasWebsite: Boolean get() = !website.isNullOrBlank()
}

object OverpassRepository {
    private const val ENDPOINT = "https://overpass-api.de/api/interpreter"

    suspend fun findBusinesses(latitude: Double, longitude: Double, radiusMeters: Int = 30_000): List<BusinessLead> =
        withContext(Dispatchers.IO) {
            val query = """
                [out:json][timeout:45];
                (
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["shop"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["office"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["craft"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["amenity"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["healthcare"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["tourism"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["leisure"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["industrial"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["landuse"="industrial"];
                  nwr(around:$radiusMeters,$latitude,$longitude)["name"]["man_made"="works"];
                );
                out center tags;
            """.trimIndent()

            val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 20_000
                readTimeout = 60_000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                setRequestProperty("User-Agent", "ProspectAI/0.1 (Android)")
            }

            try {
                val body = "data=" + URLEncoder.encode(query, "UTF-8")
                connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val response = stream.bufferedReader().use { it.readText() }
                if (status !in 200..299) {
                    throw IllegalStateException("A fonte de mapas respondeu com HTTP $status. Tente novamente mais tarde.")
                }

                val elements = JSONObject(response).optJSONArray("elements") ?: return@withContext emptyList()
                val leads = ArrayList<BusinessLead>()
                for (i in 0 until elements.length()) {
                    val item = elements.optJSONObject(i) ?: continue
                    val tags = item.optJSONObject("tags") ?: continue
                    val name = tags.optString("name").trim()
                    if (name.isBlank()) continue

                    val lat = if (item.has("lat")) item.optDouble("lat") else item.optJSONObject("center")?.optDouble("lat") ?: Double.NaN
                    val lon = if (item.has("lon")) item.optDouble("lon") else item.optJSONObject("center")?.optDouble("lon") ?: Double.NaN
                    if (!lat.isFinite() || !lon.isFinite()) continue

                    val website = tags.optString("website").takeIf { it.isNotBlank() }
                        ?: tags.optString("contact:website").takeIf { it.isNotBlank() }
                    val phone = tags.optString("phone").takeIf { it.isNotBlank() }
                        ?: tags.optString("contact:phone").takeIf { it.isNotBlank() }
                    val street = tags.optString("addr:street")
                    val number = tags.optString("addr:housenumber")
                    val suburb = tags.optString("addr:suburb")
                    val city = tags.optString("addr:city")
                    val address = listOf(
                        listOf(street, number).filter { it.isNotBlank() }.joinToString(", "),
                        suburb, city
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

                    leads += BusinessLead(
                        id = item.optString("type") + "/" + item.optString("id"),
                        name = name,
                        category = category,
                        phone = phone,
                        address = address,
                        website = website,
                        latitude = lat,
                        longitude = lon
                    )
                }
                leads.distinctBy { it.id }.sortedBy { it.name.lowercase() }
            } finally {
                connection.disconnect()
            }
        }
}
