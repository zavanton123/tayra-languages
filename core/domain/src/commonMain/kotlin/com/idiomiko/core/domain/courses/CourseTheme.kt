package com.idiomiko.core.domain.courses

/**
 * What a course is about, read from its topic, for the picture on its tile. The app's courses give their topic in
 * English ("Travel and places in Brazil"), as tools/courses/<language>/themes.tsv lists them; a reader's own course
 * may have none, or one in another language, and then [GENERAL] stands in.
 *
 * The themes are tried in this order, each by its [keywords], so the more particular comes first: "Everyday
 * technology" is [TECHNOLOGY], not [DAILY_LIFE]; "Getting around the city" is [TRANSPORT].
 */
enum class CourseTheme(private vararg val keywords: String) {
    TRANSPORT("getting around", "transport*", "mobility", "road", "car", "train", "cycling", "traffic", "bus", "buses"),
    HOME("home", "house", "housing", "decoration", "moving", "settling", "furniture"),
    FAMILY("family", "families", "generation", "parent", "child", "children", "wedding"),
    FOOD("food", "cook*", "cuisine", "kitchen", "recipe", "meal", "eating", "tea", "coffee", "flavour", "flavor", "nutrition", "table", "restaurant"),
    SHOPPING("shop*", "market", "store", "consum*", "spending"),
    CLOTHES("fashion", "cloth*", "dress"),
    MONEY("money", "econom*", "financ*", "bank*", "price"),
    WORK("work*", "job", "career", "profession*", "business*", "entrepreneur*", "labour", "labor", "office", "employ*", "trade"),
    SCHOOL("school*", "educat*", "stud*", "learning", "universit*", "training", "teach*", "exam"),
    LEISURE("free time", "hobb*", "leisure", "game", "play"),
    SPORT("sport*", "olympic*", "football", "soccer", "exercis*", "fitness", "skiing"),
    HEALTH("health*", "body", "medic*", "doctor", "hospital", "wellbeing", "well-being", "caring", "care"),
    SAFETY("disaster", "prevention", "safety", "emergenc*", "resilien*"),
    TRAVEL("travel*", "trip", "journey", "holiday", "places", "abroad", "touris*", "vacation", "explor*", "countries", "immigra*", "migra*"),
    MAPS("map", "navigation", "geograph*"),
    CITY("city", "cities", "urban*", "town", "neighbourhood", "neighborhood", "errand", "public space"),
    MIND("mind", "psycholog*", "emotion*", "feeling", "philosoph*", "ethic*", "reflection", "essay", "idea", "thought", "happiness"),
    ART("art", "artist*", "paint*", "visual", "design", "craft*", "photograph*"),
    MUSIC("music*", "song", "instrument", "concert", "danc*"),
    FILM("film", "cinema", "television", "movie"),
    THEATRE("theatre", "theater", "circus", "performance", "stage"),
    BOOKS("book", "literature", "word", "writing", "poet*", "poem", "stories", "story", "tales", "legend", "letter", "memoir", "reading"),
    LANGUAGE("language", "dialect", "speaking", "translat*", "grammar", "esperanto"),
    NEWS("news", "newspaper", "journalism", "media", "press", "opinion", "debate", "advertising", "communication", "column", "message"),
    CELEBRATION("celebrat*", "festival", "party", "parties", "birthday", "fair", "custom", "folk"),
    HISTORY("history", "histories", "past", "heritage", "archaeolog*", "tradition*", "ancient", "museum"),
    ANIMALS("animal", "pet", "wildlife", "bird", "bichos", "zoo"),
    NATURE("nature", "natural", "environment*", "plant", "garden", "botan*", "forest", "outdoors", "landscape", "countryside", "country life", "farm*", "agricultur*", "rural", "village", "biome", "amazon*", "rainforest", "mountain", "hill", "desert", "cave"),
    SEA("sea", "coast", "ocean", "beach", "beaches", "river", "lake", "water", "island", "fjord", "archipelago", "delta"),
    WEATHER("weather", "climate", "season", "winter", "summer", "energy"),
    SPACE("astronom*", "universe", "star", "space", "planet"),
    TECHNOLOGY("technolog*", "computer", "digital", "internet", "device", "phone", "innovat*", "robot*"),
    SCIENCE("scien*", "research", "discover*", "chemi*", "physic*", "material", "biolog*", "invent*", "number", "math*", "reasoning"),
    BUILDING("architect*", "engineering", "construction", "building", "mining", "rock"),
    LAW("law", "rights", "citizen*", "state", "politic*", "government", "paperwork", "contract"),
    SOCIETY("society", "community", "together", "people", "culture", "friend*", "relationship", "neighbour", "neighbor", "social"),
    TIME("time", "clock"),
    DAILY_LIFE("everyday", "daily", "routine", "day", "life", "living"),

    /** A topic that names none of the above, or a course without one. */
    GENERAL;

    // "cook*" starts a word ("cooking"); any other keyword is a whole word, or its plural ("arts", not "article").
    private val patterns: List<Regex> by lazy {
        keywords.map { word ->
            val stem = word.removeSuffix("*")
            Regex("""(?<!\p{L})""" + Regex.escape(stem) + if (word.endsWith("*")) "" else """(?:s|es)?(?!\p{L})""")
        }
    }

    companion object {
        /** The theme of [course]: by its topic, else by its title. */
        fun of(course: Course): CourseTheme = of(course.topic) ?: of(course.title) ?: GENERAL

        /** The first theme a word of [text] starts as one of its keywords, or null. */
        fun of(text: String): CourseTheme? {
            val lower = text.lowercase()
            if (lower.isBlank()) return null
            return entries.firstOrNull { theme -> theme.patterns.any { it.containsMatchIn(lower) } }
        }
    }
}
