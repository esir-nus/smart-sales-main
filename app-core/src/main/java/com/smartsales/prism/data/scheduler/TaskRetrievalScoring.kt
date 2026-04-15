package com.smartsales.prism.data.scheduler

/**
 * 活跃任务检索评分支持。
 * 说明：统一全局 follow-up 目标匹配的标准化与打分规则，避免多处漂移。
 */
internal data class TaskRetrievalCandidate(
    val id: String,
    val title: String,
    val participants: List<String> = emptyList(),
    val location: String? = null,
    val notes: String? = null
)

internal object TaskRetrievalScoring {
    const val MIN_RESOLUTION_SCORE = 55
    const val MIN_MARGIN_SCORE = 12

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw
            .lowercase()
            .replace("“", " ")
            .replace("”", " ")
            .replace("\"", " ")
            .replace("'", " ")
            .replace("，", " ")
            .replace(",", " ")
            .replace("。", " ")
            .replace("：", " ")
            .replace(":", " ")
            .replace("？", " ")
            .replace("?", " ")
            .replace("！", " ")
            .replace("!", " ")
            .replace("跟", " ")
            .replace("那个", " ")
            .replace("这个", " ")
            .replace("一下", " ")
            .replace("帮我", " ")
            .replace("把", " ")
            .replace("给我", " ")
            .replace("改到", " ")
            .replace("改成", " ")
            .replace("改期", " ")
            .replace("挪到", " ")
            .replace("延期", " ")
            .replace("延后", " ")
            .replace("时间", " ")
            .replace("reschedule", " ")
            .replace("move", " ")
            .replace("to", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    fun scoreCandidate(
        query: String,
        person: String,
        location: String,
        candidate: TaskRetrievalCandidate
    ): Int {
        val normalizedTitle = normalize(candidate.title)
        val normalizedParticipants = candidate.participants
            .map(::normalize)
            .filter { it.isNotBlank() }
        val normalizedLocation = normalize(candidate.location)
        val normalizedNotes = normalize(candidate.notes)
        if (
            normalizedTitle.isBlank() &&
            normalizedParticipants.isEmpty() &&
            normalizedLocation.isBlank() &&
            normalizedNotes.isBlank()
        ) {
            return 0
        }

        var score = 0
        if (query.isNotBlank()) {
            if (normalizedTitle == query) score += 120
            if (normalizedTitle.contains(query)) score += 80
            if (query.contains(normalizedTitle) && normalizedTitle.isNotBlank()) score += 35
            score += (diceCoefficient(query, normalizedTitle) * 45f).toInt()
            score += (tokenOverlap(query, normalizedTitle) * 35f).toInt()
        }

        normalizedParticipants
            .maxOfOrNull { participant ->
                var participantScore = 0
                if (query.isNotBlank()) {
                    participantScore += (tokenOverlap(query, participant) * 18f).toInt()
                    participantScore += (diceCoefficient(query, participant) * 12f).toInt()
                }
                if (person.isNotBlank()) {
                    if (participant == person) participantScore += 70
                    participantScore += (tokenOverlap(person, participant) * 26f).toInt()
                    participantScore += (diceCoefficient(person, participant) * 18f).toInt()
                }
                participantScore
            }
            ?.let { score += it }

        normalizedLocation.takeIf { it.isNotBlank() }?.let { candidateLocation ->
            if (query.isNotBlank()) {
                score += (tokenOverlap(query, candidateLocation) * 12f).toInt()
                score += (diceCoefficient(query, candidateLocation) * 10f).toInt()
            }
            if (location.isNotBlank()) {
                if (candidateLocation == location) score += 50
                score += (tokenOverlap(location, candidateLocation) * 20f).toInt()
                score += (diceCoefficient(location, candidateLocation) * 14f).toInt()
            }
        }

        if (query.isNotBlank() && normalizedNotes.isNotBlank()) {
            score += (tokenOverlap(query, normalizedNotes) * 8f).toInt()
            score += (diceCoefficient(query, normalizedNotes) * 6f).toInt()
        }
        return score
    }

    private fun tokenOverlap(a: String, b: String): Float {
        if (a.isBlank() || b.isBlank()) return 0f
        val aTokens = a.split(" ").filter { it.isNotBlank() }.toSet()
        val bTokens = b.split(" ").filter { it.isNotBlank() }.toSet()
        if (aTokens.isEmpty() || bTokens.isEmpty()) return 0f
        val overlap = aTokens.intersect(bTokens).size.toFloat()
        return overlap / maxOf(aTokens.size, bTokens.size).toFloat()
    }

    private fun diceCoefficient(a: String, b: String): Float {
        if (a.isBlank() || b.isBlank()) return 0f
        if (a == b) return 1f
        if (a.length < 2 || b.length < 2) return 0f
        val aBigrams = a.windowed(2).groupingBy { it }.eachCount()
        val bBigrams = b.windowed(2).groupingBy { it }.eachCount()
        var overlap = 0
        aBigrams.forEach { (bigram, count) ->
            overlap += minOf(count, bBigrams[bigram] ?: 0)
        }
        val total = a.length - 1 + b.length - 1
        if (total <= 0) return 0f
        return (2f * overlap) / total.toFloat()
    }
}
