package com.idiomiko.core.domain.news

/** The few topics the news is sorted into, whatever the feeds call their sections. */
enum class NewsTopic { HOME, WORLD, POLITICS, ECONOMY, SPORT, CULTURE, SCIENCE, LIFESTYLE }

/**
 * Maps the sections feeds file their articles under ("Desporto", "NOTÍCIAS", "Ciência e Saúde", "Politik") onto
 * [NewsTopic]s, and names each topic as the news of a language does. A section is matched by the beginnings of its
 * words, letter case and accents aside, against words for each topic in the catalog's languages; a section of no
 * topic (a place, a person, the feed's own name for its front page) is left out. HOME is the country a language's
 * outlets mostly write about, and a language whose outlets have no one country has no HOME.
 */
object NewsTopics {
    /** The topics [category] stands for in a feed of the language [languageCode]; none for a section of no topic. */
    fun of(category: String, languageCode: String): Set<NewsTopic> {
        val text = fold(category.trim())
        if (text.isEmpty()) return emptySet()
        val found = NewsTopic.entries.filterTo(mutableSetOf()) { topic ->
            val words = if (topic == NewsTopic.HOME) homeWords[languageCode].orEmpty() else topicWords.getValue(topic)
            words.any { it.matches(text) } && blocked[topic].orEmpty().none { it.matches(text) }
        }
        return found
    }

    /** The topics of an article, in the order its feed lists the sections they come from. */
    fun of(article: NewsArticle): List<NewsTopic> =
        article.topics.flatMap { category -> of(category, article.source.languageCode).sortedBy { it.ordinal } }.distinct()

    /** The topic's name as the news in [languageCode] writes it; null where the language has no word for it in the app. */
    fun label(topic: NewsTopic, languageCode: String): String? = labels[languageCode]?.get(topic.ordinal)

    /** The languages the app names topics in. */
    val languages: Set<String> get() = labels.keys

    /** A word for a topic: [text] as the start of a word of the section, or, with [whole], a whole word of it. */
    private class Word(text: String, private val whole: Boolean) {
        private val text = fold(text)

        /** Scripts written without spaces (Chinese, Thai) are matched anywhere; the others from a word's start. */
        private val atWordStart = this.text.first().code < SPACELESS_FROM

        fun matches(section: String): Boolean {
            var at = section.indexOf(text)
            while (at >= 0) {
                val start = !atWordStart || at == 0 || !section[at - 1].isLetterOrDigit()
                val end = !whole || at + text.length == section.length || !section[at + text.length].isLetterOrDigit()
                if (start && end) return true
                at = section.indexOf(text, at + 1)
            }
            return false
        }
    }

    /** Words, each the start of a word unless it begins with "=", which makes it a whole word. */
    private fun words(vararg list: String): List<Word> = list.map { if (it.startsWith('=')) Word(it.drop(1), whole = true) else Word(it, whole = false) }

    /** Lower case, without the accents a section may drop in capitals ("POLíTICA", "ΑΘΛΗΤΙΣΜΟΣ"). */
    internal fun fold(text: String): String = buildString(text.length) {
        for (c in text.lowercase()) {
            if (c == '̇') continue
            append(FOLDED[c] ?: c)
        }
    }

    private val FOLDED: Map<Char, Char> = buildMap {
        fun put(from: String, to: Char) = from.forEach { put(it, to) }
        put("àáâãäåāăą", 'a'); put("çćčĉ", 'c'); put("ďđ", 'd'); put("èéêëēėęě", 'e'); put("ìíîïīįı", 'i')
        put("ľĺł", 'l'); put("ñńň", 'n'); put("òóôõöøōő", 'o'); put("ŕř", 'r'); put("śšşș", 's'); put("ťţț", 't')
        put("ùúûüūůűų", 'u'); put("ýÿ", 'y'); put("źżž", 'z'); put("ğ", 'g')
        put("ά", 'α'); put("έ", 'ε'); put("ή", 'η'); put("ίϊΐ", 'ι'); put("ό", 'ο'); put("ύϋΰ", 'υ'); put("ώ", 'ω'); put("ς", 'σ')
    }

    /** Hebrew and the scripts after it: Arabic, the Indian scripts, Thai, Chinese and the rest, matched anywhere in a section. */
    private const val SPACELESS_FROM = 0x0590

    private val topicWords: Map<NewsTopic, List<Word>> = mapOf(
        NewsTopic.WORLD to words(
            "world", "=global", "international", "internacional", "internacia", "internațional", "internaţional", "internazional", "internasional",
            "mundo", "welt", "ausland", "monde", "étranger", "mondo", "esteri", "świat", "zagranic", "svět", "=svet", "svijet", "zahranič", "tujina",
            "ulkomaa", "utrikes", "världen", "verden", "utenriks", "udland", "wereld", "buitenland", "külföld", "világ", "dünya", "dunia", "bota",
            "pasaul", "ārzem", "välismaa", "erlent", "=byd", "domhan", "idirnáisiúnta", "mundu", "=món", "=mondo", "kimataifa", "wêreld", "buiteland",
            "dinja", "útheim", "saoghal", "europ", "afric", "áfrica", "afrik", "asia", "ásia", "asie", "asien", "americ", "améric", "amerik",
            "avropa", "asiya", "ulaya", "mashariki ya kati", "châu", "oriente médio", "medio oriente", "moyen-orient", "middle east", "nahost", "balkan",
            "=мир", "=свет", "свят", "світ", "замежж", "міжнарод", "международ", "меѓународ", "зарубеж", "әлем", "дэлхий", "гадаад",
            "европ", "європ", "афри", "азия", "азі", "америк", "балкан", "ближн",
            "κόσμ", "διεθν", "ευρώπ",
            "العالم", "عالم", "دولي", "جهان", "दुनिया", "विदेश", "বিশ্ব", "আন্তর্জাতিক", "国际", "國際", "世界", "全球", "国際", "국제", "세계",
            "ต่างประเทศ", "รอบโลก", "quốc tế", "thế giới", "աշխարհ", "միջազգային", "მსოფლიო", "საერთაშორისო", "ವಿದೇಶ", "អន្តរជាតិ", "ຕ່າງປະເທດ",
            "ዓለም", "עולם", "អាស៊ី", "អាមេរិក", "អឺរ៉ុប", "اروپا", "آسيا", "এশিয়া", "亚太", "亞太", "ਦੁਨੀਆ", "ਵਿਦੇਸ਼", "འཛམ་གླིང", "རྒྱལ་སྤྱི",
        ),
        NewsTopic.POLITICS to words(
            "polit", "poliitik", "belpolitik", "külpolitik", "eleições", "eleição", "elecc", "élection", "election", "=wahl", "wahlen", "wybor", "izbor",
            "volby", "választás", "seçim", "hauteskunde", "etholiad", "governo", "gobierno", "govern", "regierung", "rząd", "kormány", "hallitus",
            "parlament", "congresso", "kongres", "senado", "siasa", "pulitika", "siyas", "stjórnmál", "gwleidyddiaeth", "polaitíocht", "poilitig",
            "полит", "політ", "палітык", "саясат", "улс төр", "уряд", "правительств", "πολιτικ", "βουλ",
            "سياس", "سیاست", "राजनीति", "রাজনীতি", "সরকার", "政治", "정치", "การเมือง", "chính trị", "քաղաքակ", "პოლიტიკ", "ರಾಜಕೀಯ", "នយោបាយ", "ການເມືອງ",
            "ፖለቲካ", "פוליטי", "ਸਿਆਸਤ", "ਰਾਜਨੀਤੀ", "ཆབ་སྲིད",
        ),
        NewsTopic.ECONOMY to words(
            "econom", "ekonom", "wirtschaft", "business", "bisnis", "biznes", "biznis", "bizness", "busnes", "negócio", "negocio", "negosyo", "finan",
            "gospodar", "gazdaság", "talous", "økonomi", "næringsliv", "verslas", "majandus", "vállalkoz", "geilleagar", "=gnó", "iqtisad", "maliyyə",
            "uchumi", "biashara", "=affaires", "afaceri", "privred", "trabalho", "traballo", "trabajo", "=mercados", "=markets", "=money", "börse", "børs",
            "=bolsa", "viðskipt",
            "экономи", "економ", "иконом", "эканом", "бизнес", "финанс", "қаржы", "эдийн засаг", "οικονομ", "επιχειρ",
            "اقتصاد", "कारोबार", "बिज़नेस", "बिजनेस", "अर्थव्यवस्था", "অর্থনীতি", "বাণিজ্য", "经济", "經濟", "财经", "財經", "商业", "商業", "経済", "ビジネス",
            "경제", "เศรษฐกิจ", "ธุรกิจ", "kinh tế", "kinh doanh", "տնտես", "ეკონომ", "ბიზნეს", "ವಾಣಿಜ್ಯ", "ಆರ್ಥಿಕ", "សេដ្ឋកិច្ច", "ເສດຖະກິດ", "ທຸລະກິດ",
            "ቢዝነስ", "ኢኮኖሚ", "כלכל", "עסקים", "ਆਰਥਿਕ", "ਕਾਰੋਬਾਰ", "དཔལ་འབྱོར",
        ),
        NewsTopic.SPORT to words(
            "sport", "esport", "deport", "desport", "=spor", "futebol", "futbol", "fútbol", "football", "fußball", "fussball", "fotball", "fodbold",
            "voetbal", "futbal", "jalgpall", "nogomet", "fudbal", "piłka", "calcio", "soccer", "=sacar", "pêl-droed", "cricket", "basket", "košark", "kosárlabda",
            "koszyk", "tennis", "tenis", "rugby", "rygbi", "hockey", "hokej", "=nba", "formula 1", "formula1", "olymp", "olimp", "urheilu", "idrott",
            "idræt", "idrett", "ítrótt", "íþrótt", "olahraga", "michezo", "=dimba", "chwaraeon", "spórt", "kirol", "spòrs", "idman", "boxe", "boxing",
            "ciclismo", "seiclo", "cycling", "euroleague", "evroliga",
            "спорт", "футбол", "хоккей", "хокей", "баскет", "теннис", "теніс", "олимп", "олімп", "αθλητ", "ποδόσφαιρ", "μπάσκετ",
            "رياضة", "ورزش", "فوتبال", "खेल", "क्रिकेट", "খেলা", "ফুটবল", "ক্রিকেট", "体育", "體育", "足球", "スポーツ", "サッカー", "野球", "스포츠", "축구",
            "야구", "กีฬา", "ฟุตบอล", "มวย", "thể thao", "bóng đá", "սպորտ", "სპორტ", "ಕ್ರೀಡ", "ಕ್ರಿಕೆಟ್", "កីឡា", "ກິລາ", "ስፖርት", "ספורט", "כדורגל", "ਖੇਡ",
            "རྩེད་མོ",
        ),
        NewsTopic.CULTURE to words(
            "cultur", "kultur", "cultúr", "kultúr", "kultūr", "kultuur", "kulturë", "diwylliant", "menning", "mentan", "cultar", "utamaduni", "budaya",
            "kültür", "mədəniyyət", "=arte", "=artes", "=arts", "=art", "kunst", "musi", "muzi", "muzyk", "glasba", "=ceol", "cinema", "cinéma",
            "=cine", "film", "=kino", "=mozi", "zinema", "sinema", "teatr", "theat", "teater", "teatro", "liter", "=livros", "=libros", "=books", "boeke",
            "bücher", "książk", "entreten", "entertain", "divertis", "unterhaltung", "showbiz", "celebr", "famosos", "=fama", "promi", "viihde",
            "izklaide", "nöje", "underholdning", "zábav", "zabav", "szórakoz", "eğlence", "hiburan", "burudani", "rampelys", "=kult", "bollywood",
            "hollywood",
            "культур", "култур", "мәдениет", "соёл", "кино", "музык", "музик", "искусств", "мистецтв", "развлеч", "забав", "шоу-биз",
            "πολιτισμ", "τέχνες", "τέχνη", "κινηματογρ", "μουσικ", "βιβλί", "θέατρ",
            "ثقاف", "فنون", "=فن", "سينما", "موسيق", "فرهنگ", "هنر", "मनोरंजन", "संस्कृति", "सिनेमा", "সংস্কৃতি", "বিনোদন", "বলিউড", "ঢালিউড",
            "文化", "娱乐", "娛樂", "艺术", "藝術", "电影", "電影", "音乐", "音樂", "文学", "文學", "芸能", "エンタメ", "映画", "音楽", "문화", "연예", "엔터",
            "영화", "음악", "บันเทิง", "วัฒนธรรม", "ดารา", "ภาพยนตร์", "văn hóa", "văn hoá", "giải trí", "điện ảnh", "âm nhạc", "մշակույթ", "արվեստ",
            "კულტურ", "ხელოვნ", "ಸಂಸ್ಕೃತಿ", "ಸಿನಿಮಾ", "ಮನರಂಜನೆ", "ಬಾಲಿವುಡ್", "វប្បធម៌", "កម្សាន្ត", "ວັດທະນະທຳ", "ບັນເທີງ", "ባህል", "መዝናኛ", "תרבות",
            "בידור", "ਸੱਭਿਆਚਾਰ", "ਮਨੋਰੰਜਨ", "རིག་གཞུང",
        ),
        NewsTopic.SCIENCE to words(
            "scien", "ciênc", "cienc", "ciènc", "wissenschaft", "=wissen", "nauk", "znanost", "=věda", "=veda", "tech", "tecnolog", "tecnolox",
            "tehnolog", "tehnik", "teknolog", "teknik", "texnolog", "technoleg", "tegnolog", "teicneol", "=it", "=ict", "=ikt", "digitaln", "netzwelt",
            "internet", "interret", "=ai", "inteligência artificial", "inteligencia artificial", "intelligence artificielle", "künstliche intelligenz",
            "artificial intelligence", "mesterséges intelligencia", "artificiell intelligens", "umetna inteligenca", "artefarita intelekto",
            "intelligenza artificiale", "sztuczna inteligencja", "tudomány", "tiede", "vetenskap", "vitenskap", "videnskab", "wetenschap", "wetenskap",
            "vísind", "bilim", "sains", "sayansi", "agham", "gwyddoniaeth", "eolaíocht", "zientzia", "știin", "shkenc", "mokslas", "zinātn", "teadus",
            "=elm", "xjenza", "universum", "astronom",
            // Health, and the natural world, are science's too.
            "saúde", "=salud", "=santé", "gesundheit", "health", "zdrav", "zdrow", "sveikat", "veselīb", "tervis", "terveys", "iechyd", "egészség",
            "sağlık", "=salute", "sănătate", "medicin", "medizin", "afya", "kalusugan", "kesehatan", "heilsa",
            "ambiente", "medioambiente", "umwelt", "environ", "climat", "clima", "klima", "ecolog", "ekolog", "écolog", "loodus", "prostred",
            "amgylchedd", "milieu", "miljø", "miljö", "ympäristö", "környezet", "çevre", "lingkungan", "mazingira", "kalikasan", "ingurumen", "mjedis",
            "наук", "науч", "навук", "технолог", "ғылым", "шинжлэх", "здоров", "здраве", "здравје", "медицин", "экол", "экалог", "екол", "климат",
            "природ", "прырод", "байгал", "επιστήμ", "τεχνολογ", "υγεί", "περιβάλλ",
            "علوم", "=علم", "تكنولوجيا", "تقنية", "صحة", "بيئة", "دانش", "فناوری", "تندرستی", "سلامت", "محیط زیست", "विज्ञान", "टेक", "स्वास्थ्य", "सेहत",
            "পরিবেশ", "বিজ্ঞান", "প্রযুক্তি", "স্বাস্থ্য", "科技", "科学", "科學", "健康", "环境", "環境", "生态", "生態", "サイエンス", "テクノロジー", "과학",
            "기술", "건강", "วิทยาศาสตร์", "ไอที", "เทคโนโลยี", "สุขภาพ", "khoa học", "công nghệ", "sức khỏe", "գիտ", "առողջ", "მეცნიერ", "ტექნოლოგ",
            "ჯანმრთ", "ಆರೋಗ್ಯ", "ವಿಜ್ಞಾನ", "ತಂತ್ರಜ್ಞಾನ", "បរិស្ថាន", "វិទ្យាសាស្ត្រ", "បច្ចេកវិទ្យា", "សុខភាព", "ວິທະຍາສາດ", "ເຕັກໂນໂລຊີ", "ສຸຂະພາບ", "ጤና",
            "ሳይንስ", "ቴክኖሎጂ", "מדע", "טכנולוג", "בריאות", "ਵਿਗਿਆਨ", "ਤਕਨੀਕ", "ਸਿਹਤ", "ཚན་རིག",
        ),
        NewsTopic.LIFESTYLE to words(
            "lifestyle", "life style", "=life", "living", "estilo", "=vida", "=style", "stil", "styl", "livsstil", "lebensart", "=leben", "gastronom",
            "culin", "comida", "=food", "receit", "recet", "cuisine", "kulinar", "rezept", "travel", "viage", "viaje", "voyage", "reise", "=reis",
            "reizen", "turism", "turizm", "=moda", "fashion", "=mode", "beleza", "belleza", "beauté", "beauty", "família", "familia", "family",
            "bem-estar", "bienestar", "wellness", "=auto", "=autos", "carros", "=motor", "=motori", "=fit", "ljubav", "shopping", "forbruk", "=arki",
            "dzīvesstil", "laisvalaik", "élet", "életmód", "življenjsk", "ciekawostk", "zanimivost", "curiosidad", "curiosit", "insólit",
            "=interesting", "yaşam", "gaya hidup", "maisha", "mtindo", "ffordd o fyw", "stíl mhaireachtála", "bizimodu", "leefstijl", "leefstyl",
            "elustiil", "elämäntap", "lífsstíl", "=astro",
            "стиль", "стил", "лайфстайл", "образ жизни", "лад жыцця", "начин на живот", "любопитно", "өмір салты", "амьдрал", "γαστρονομ", "ταξίδ", "μόδα",
            "τρόπος ζωής",
            "أسلوب", "منوعات", "سياحة", "سبک زندگی", "گوناگون", "लाइफस्टाइल", "जीवनशैली", "জীবনযাপন", "লাইফস্টাইল", "生活", "时尚", "時尚", "旅游", "旅遊",
            "美食", "ライフ", "暮らし", "くらし", "라이프", "생활", "ไลฟ์สไตล์", "ท่องเที่ยว", "đời sống", "du lịch", "կենսակերպ", "ცხოვრების", "ಜೀವನಶೈಲಿ",
            "របៀបរស់នៅ", "ວິຖີຊີວິດ", "אורח חיים", "סגנון חיים", "ਜੀਵਨ ਸ਼ੈਲੀ",
        ),
    )

    /** Sections a topic's word would take for it but are another thing: the police in Norwegian, Danish and Dutch, a site's or a game's name. */
    private val blocked: Map<NewsTopic, List<Word>> = mapOf(
        NewsTopic.POLITICS to words("=politi", "=politie", "=politiet"),
        NewsTopic.WORLD to words("global voices"),
        NewsTopic.LIFESTYLE to words("grand theft auto"),
        // Newspapers that file every article under their own name.
        NewsTopic.HOME to words("егемен", "superbalita"),
    )

    /** The home country's words in each language: its name, and the sections for news from home ("Inland", "Kotimaa"). */
    private val homeWords: Map<String, List<Word>> = mapOf(
        "af" to words("suid-afrika"),
        "am" to words("የሀገር ውስጥ", "ኢትዮጵያ"),
        "az" to words("azərbaycan"),
        "be" to words("беларус"),
        "bg" to words("българ"),
        "bn" to words("বাংলাদেশ", "রাজধানী", "জেলা"),
        "bo" to words("བོད།", "བོད་ནང", "བོད་ཀྱི"),
        "bs" to words("=bih", "bosna i hercegovina", "bosni"),
        "ca" to words("catalunya", "=principat", "país valencià", "=illes"),
        "ceb" to words("cebu", "sugbo", "pilipinas"),
        "cs" to words("domácí", "česko", "česká republika"),
        "cy" to words("cymru"),
        "da" to words("danmark", "indland"),
        "de" to words("deutschland", "inland"),
        "el" to words("ελλάδα", "ελλάδ"),
        "es" to words("españa"),
        "et" to words("eesti"),
        "eu" to words("euskal herria", "bizkaia", "gipuzkoa", "nafarroa", "araba"),
        "fa" to words("ایران"),
        "fi" to words("kotimaa", "suomi"),
        "fo" to words("føroy"),
        "fr" to words("=france"),
        "ga" to words("=náisiúnta", "éire", "=eire"),
        "gd" to words("=alba"),
        "gl" to words("galicia", "galiza"),
        "he" to words("ישראל", "בארץ"),
        "hi" to words("=india", "=state", "pradesh", "bihar", "rajasthan", "delhi", "भारत"),
        "hr" to words("hrvatsk"),
        "hu" to words("belföld", "itthon", "magyarország"),
        "hy" to words("հայաստան"),
        "id" to words("indonesia", "=nasional"),
        "is" to words("innlent", "=ísland"),
        "it" to words("italia", "=cronaca", "=interni"),
        "ja" to words("国内", "社会"),
        "ka" to words("საქართველო", "თბილის"),
        "kk" to words("қазақстан"),
        "km" to words("កម្ពុជា", "ព័ត៌មានជាតិ"),
        "kn" to words("ಕರ್ನಾಟಕ", "ಬೆಂಗಳೂರು", "ರಾಜ್ಯ", "ರಾಷ್ಟ್ರೀಯ"),
        "ko" to words("국내", "한국"),
        "lo" to words("ລາວ"),
        "lt" to words("lietuv"),
        "lv" to words("latvij"),
        "mk" to words("македониј"),
        "mn" to words("монгол"),
        "mt" to words("=malta"),
        "nl" to words("binnenland", "nederland", "belgië"),
        "no" to words("norge", "innenriks"),
        "pa" to words("ਪੰਜਾਬ", "ਭਾਰਤ"),
        "pl" to words("polska", "=kraj"),
        "pt" to words("brasil", "rio grande do", "são paulo", "rio de janeiro", "brasília"),
        "ro" to words("românia"),
        "ru" to words("росси"),
        "sk" to words("slovensk", "domáce"),
        "sl" to words("slovenij"),
        "sq" to words("=vendi", "shqipëri", "kosov"),
        "sr" to words("srbij"),
        "sv" to words("sverige", "inrikes"),
        "sw" to words("kitaifa", "kenya"),
        "th" to words("ในประเทศ", "ทั่วไทย"),
        "tl" to words("=nation", "bansa", "pilipinas", "philippines"),
        "tr" to words("türkiye"),
        "uk" to words("україн", "київ"),
        "vi" to words("việt nam", "trong nước"),
        "yue" to words("香港", "本地"),
        "zh" to words("中国", "中國", "国内", "國內"),
    )

    /**
     * Each language's names for the topics, in [NewsTopic]'s order: home, world, politics, economy, sport, culture,
     * science and technology, lifestyle. Portuguese is Brazil's, as the app teaches it.
     */
    private val labels: Map<String, List<String?>> = mapOf(
        "af" to listOf("Suid-Afrika", "Wêreld", "Politiek", "Ekonomie", "Sport", "Kultuur", "Wetenskap & tegnologie", "Leefstyl"),
        "am" to listOf("ኢትዮጵያ", "ዓለም", "ፖለቲካ", "ኢኮኖሚ", "ስፖርት", "ባህል", "ሳይንስና ቴክኖሎጂ", "አኗኗር"),
        "ar" to listOf(null, "العالم", "سياسة", "اقتصاد", "رياضة", "ثقافة", "علوم وتكنولوجيا", "أسلوب حياة"),
        "az" to listOf("Azərbaycan", "Dünya", "Siyasət", "İqtisadiyyat", "İdman", "Mədəniyyət", "Elm və texnologiya", "Həyat tərzi"),
        "be" to listOf("Беларусь", "Свет", "Палітыка", "Эканоміка", "Спорт", "Культура", "Навука і тэхналогіі", "Лад жыцця"),
        "bg" to listOf("България", "Свят", "Политика", "Икономика", "Спорт", "Култура", "Наука и технологии", "Начин на живот"),
        "bn" to listOf("বাংলাদেশ", "বিশ্ব", "রাজনীতি", "অর্থনীতি", "খেলা", "সংস্কৃতি", "বিজ্ঞান ও প্রযুক্তি", "জীবনযাপন"),
        "bo" to listOf("བོད།", "འཛམ་གླིང་།", "ཆབ་སྲིད།", "དཔལ་འབྱོར།", "རྩེད་མོ།", "རིག་གཞུང་།", "ཚན་རིག", "འཚོ་བའི་སྟངས།"),
        "bs" to listOf("BiH", "Svijet", "Politika", "Ekonomija", "Sport", "Kultura", "Nauka i tehnologija", "Životni stil"),
        "ca" to listOf("Catalunya", "Món", "Política", "Economia", "Esports", "Cultura", "Ciència i tecnologia", "Estil de vida"),
        "ceb" to listOf("Pilipinas", "Kalibutan", "Politika", "Ekonomiya", "Sports", "Kultura", "Siyensya ug teknolohiya", "Estilo sa kinabuhi"),
        "cs" to listOf("Česko", "Svět", "Politika", "Ekonomika", "Sport", "Kultura", "Věda a technika", "Životní styl"),
        "cy" to listOf("Cymru", "Y byd", "Gwleidyddiaeth", "Economi", "Chwaraeon", "Diwylliant", "Gwyddoniaeth a thechnoleg", "Ffordd o fyw"),
        "da" to listOf("Danmark", "Udland", "Politik", "Økonomi", "Sport", "Kultur", "Videnskab & tech", "Livsstil"),
        "de" to listOf("Deutschland", "Welt", "Politik", "Wirtschaft", "Sport", "Kultur", "Wissen & Technik", "Leben"),
        "el" to listOf("Ελλάδα", "Κόσμος", "Πολιτική", "Οικονομία", "Αθλητισμός", "Πολιτισμός", "Επιστήμη & τεχνολογία", "Τρόπος ζωής"),
        "en" to listOf(null, "World", "Politics", "Business", "Sport", "Culture", "Science & tech", "Lifestyle"),
        "eo" to listOf(null, "Mondo", "Politiko", "Ekonomio", "Sporto", "Kulturo", "Scienco kaj tekniko", "Vivstilo"),
        "es" to listOf("España", "Internacional", "Política", "Economía", "Deportes", "Cultura", "Ciencia y tecnología", "Estilo de vida"),
        "et" to listOf("Eesti", "Välismaa", "Poliitika", "Majandus", "Sport", "Kultuur", "Teadus ja tehnika", "Elustiil"),
        "eu" to listOf("Euskal Herria", "Mundua", "Politika", "Ekonomia", "Kirola", "Kultura", "Zientzia eta teknologia", "Bizimodua"),
        "fa" to listOf("ایران", "جهان", "سیاست", "اقتصاد", "ورزش", "فرهنگ", "علم و فناوری", "سبک زندگی"),
        "fi" to listOf("Kotimaa", "Ulkomaat", "Politiikka", "Talous", "Urheilu", "Kulttuuri", "Tiede ja teknologia", "Elämäntapa"),
        "fo" to listOf("Føroyar", "Útheimurin", "Politikkur", "Búskapur", "Ítróttur", "Mentan", "Vísindi og tøkni", "Lívsstílur"),
        "fr" to listOf("France", "Monde", "Politique", "Économie", "Sport", "Culture", "Sciences & tech", "Art de vivre"),
        "ga" to listOf("Éire", "An Domhan", "Polaitíocht", "Geilleagar", "Spórt", "Cultúr", "Eolaíocht & teicneolaíocht", "Stíl mhaireachtála"),
        "gd" to listOf("Alba", "An Saoghal", "Poilitigs", "Eaconamaidh", "Spòrs", "Cultar", "Saidheans is teicneòlas", "Dòigh-beatha"),
        "gl" to listOf("Galicia", "Internacional", "Política", "Economía", "Deportes", "Cultura", "Ciencia e tecnoloxía", "Estilo de vida"),
        "he" to listOf("ישראל", "עולם", "פוליטיקה", "כלכלה", "ספורט", "תרבות", "מדע וטכנולוגיה", "סגנון חיים"),
        "hi" to listOf("भारत", "दुनिया", "राजनीति", "कारोबार", "खेल", "मनोरंजन", "विज्ञान-टेक", "लाइफस्टाइल"),
        "hr" to listOf("Hrvatska", "Svijet", "Politika", "Gospodarstvo", "Sport", "Kultura", "Znanost i tehnologija", "Životni stil"),
        "hu" to listOf("Belföld", "Külföld", "Politika", "Gazdaság", "Sport", "Kultúra", "Tudomány & tech", "Életmód"),
        "hy" to listOf("Հայաստան", "Աշխարհ", "Քաղաքականություն", "Տնտեսություն", "Սպորտ", "Մշակույթ", "Գիտություն և տեխնոլոգիա", "Կենսակերպ"),
        "id" to listOf("Indonesia", "Dunia", "Politik", "Ekonomi", "Olahraga", "Budaya", "Sains & teknologi", "Gaya hidup"),
        "is" to listOf("Innlent", "Erlent", "Stjórnmál", "Viðskipti", "Íþróttir", "Menning", "Vísindi og tækni", "Lífsstíll"),
        "it" to listOf("Italia", "Mondo", "Politica", "Economia", "Sport", "Cultura", "Scienza & tech", "Stile di vita"),
        "ja" to listOf("国内", "国際", "政治", "経済", "スポーツ", "文化", "科学・IT", "暮らし"),
        "ka" to listOf("საქართველო", "მსოფლიო", "პოლიტიკა", "ეკონომიკა", "სპორტი", "კულტურა", "მეცნიერება და ტექნოლოგია", "ცხოვრების სტილი"),
        "kk" to listOf("Қазақстан", "Әлем", "Саясат", "Экономика", "Спорт", "Мәдениет", "Ғылым және технология", "Өмір салты"),
        "km" to listOf("កម្ពុជា", "អន្តរជាតិ", "នយោបាយ", "សេដ្ឋកិច្ច", "កីឡា", "វប្បធម៌", "វិទ្យាសាស្ត្រ និងបច្ចេកវិទ្យា", "របៀបរស់នៅ"),
        "kn" to listOf("ದೇಶ", "ವಿದೇಶ", "ರಾಜಕೀಯ", "ವಾಣಿಜ್ಯ", "ಕ್ರೀಡೆ", "ಸಂಸ್ಕೃತಿ", "ವಿಜ್ಞಾನ-ತಂತ್ರಜ್ಞಾನ", "ಜೀವನಶೈಲಿ"),
        "ko" to listOf("국내", "국제", "정치", "경제", "스포츠", "문화", "과학·IT", "라이프"),
        "lo" to listOf("ລາວ", "ຕ່າງປະເທດ", "ການເມືອງ", "ເສດຖະກິດ", "ກິລາ", "ວັດທະນະທຳ", "ວິທະຍາສາດ ແລະ ເຕັກໂນໂລຊີ", "ວິຖີຊີວິດ"),
        "lt" to listOf("Lietuva", "Pasaulis", "Politika", "Ekonomika", "Sportas", "Kultūra", "Mokslas ir IT", "Gyvenimo būdas"),
        "lv" to listOf("Latvija", "Pasaulē", "Politika", "Ekonomika", "Sports", "Kultūra", "Zinātne un tehnoloģijas", "Dzīvesstils"),
        "mk" to listOf("Македонија", "Свет", "Политика", "Економија", "Спорт", "Култура", "Наука и технологија", "Животен стил"),
        "mn" to listOf("Монгол", "Дэлхий", "Улс төр", "Эдийн засаг", "Спорт", "Соёл", "Шинжлэх ухаан, технологи", "Амьдралын хэв маяг"),
        "mt" to listOf("Malta", "Dinja", "Politika", "Ekonomija", "Sport", "Kultura", "Xjenza u teknoloġija", "Stil ta' ħajja"),
        "nl" to listOf("Binnenland", "Buitenland", "Politiek", "Economie", "Sport", "Cultuur", "Wetenschap & tech", "Leefstijl"),
        "no" to listOf("Innenriks", "Utenriks", "Politikk", "Økonomi", "Sport", "Kultur", "Vitenskap og teknologi", "Livsstil"),
        "pa" to listOf("ਭਾਰਤ", "ਦੁਨੀਆ", "ਸਿਆਸਤ", "ਆਰਥਿਕਤਾ", "ਖੇਡਾਂ", "ਸੱਭਿਆਚਾਰ", "ਵਿਗਿਆਨ ਤੇ ਤਕਨੀਕ", "ਜੀਵਨ ਸ਼ੈਲੀ"),
        "pl" to listOf("Polska", "Świat", "Polityka", "Gospodarka", "Sport", "Kultura", "Nauka i technologie", "Styl życia"),
        "pt" to listOf("Brasil", "Mundo", "Política", "Economia", "Esporte", "Cultura", "Ciência & tech", "Estilo de vida"),
        "ro" to listOf("România", "Internațional", "Politică", "Economie", "Sport", "Cultură", "Știință și tehnologie", "Stil de viață"),
        "ru" to listOf("Россия", "Мир", "Политика", "Экономика", "Спорт", "Культура", "Наука и технологии", "Образ жизни"),
        "sk" to listOf("Slovensko", "Svet", "Politika", "Ekonomika", "Šport", "Kultúra", "Veda a technika", "Životný štýl"),
        "sl" to listOf("Slovenija", "Svet", "Politika", "Gospodarstvo", "Šport", "Kultura", "Znanost in tehnologija", "Življenjski slog"),
        "sq" to listOf("Vendi", "Bota", "Politikë", "Ekonomi", "Sport", "Kulturë", "Shkencë & teknologji", "Stil jetese"),
        "sr" to listOf("Srbija", "Svet", "Politika", "Ekonomija", "Sport", "Kultura", "Nauka i tehnologija", "Lajfstajl"),
        "sv" to listOf("Sverige", "Världen", "Politik", "Ekonomi", "Sport", "Kultur", "Vetenskap & teknik", "Livsstil"),
        "sw" to listOf("Kitaifa", "Kimataifa", "Siasa", "Uchumi", "Michezo", "Utamaduni", "Sayansi na teknolojia", "Mtindo wa maisha"),
        "th" to listOf("ในประเทศ", "ต่างประเทศ", "การเมือง", "เศรษฐกิจ", "กีฬา", "วัฒนธรรม", "วิทย์ & เทคโนโลยี", "ไลฟ์สไตล์"),
        "tl" to listOf("Bansa", "Mundo", "Pulitika", "Ekonomiya", "Sports", "Kultura", "Agham at teknolohiya", "Lifestyle"),
        "tr" to listOf("Türkiye", "Dünya", "Siyaset", "Ekonomi", "Spor", "Kültür", "Bilim ve teknoloji", "Yaşam"),
        "uk" to listOf("Україна", "Світ", "Політика", "Економіка", "Спорт", "Культура", "Наука й технології", "Стиль життя"),
        "vi" to listOf("Việt Nam", "Thế giới", "Chính trị", "Kinh tế", "Thể thao", "Văn hóa", "Khoa học & công nghệ", "Đời sống"),
        "yue" to listOf("香港", "國際", "政治", "經濟", "體育", "文化", "科技", "生活"),
        "zh" to listOf("中国", "国际", "政治", "经济", "体育", "文化", "科技", "生活"),
    )
}
