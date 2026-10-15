package com.tayra.languages.core.domain.morphology

import kotlin.test.Test
import kotlin.test.assertEquals

/** The furigana and pitch helpers ported from Yomitan, against Yomitan's own cases (test/japanese-util.test.js). */
class FuriganaTest {
    private val plain = listOf(
        Triple("有り難う", "ありがとう", listOf("有" to "あ", "り" to "", "難" to "がと", "う" to "")),
        Triple("方々", "かたがた", listOf("方々" to "かたがた")),
        Triple("お祝い", "おいわい", listOf("お" to "", "祝" to "いわ", "い" to "")),
        Triple("美味しい", "おいしい", listOf("美味" to "おい", "しい" to "")),
        Triple("食べ物", "たべもの", listOf("食" to "た", "べ" to "", "物" to "もの")),
        Triple("試し切り", "ためしぎり", listOf("試" to "ため", "し" to "", "切" to "ぎ", "り" to "")),
        Triple("飼い犬", "かいいぬ", listOf("飼い犬" to "かいいぬ")),
        Triple("長い間", "ながいあいだ", listOf("長い間" to "ながいあいだ")),
        Triple("飼い犬", "", listOf("飼い犬" to "")),
        Triple("かいいぬ", "かいいぬ", listOf("かいいぬ" to "")),
        Triple("かいぬ", "かいぬ", listOf("かいぬ" to "")),
        Triple("月", "か", listOf("月" to "か")),
        Triple("月", "カ", listOf("月" to "カ")),
        Triple("有り難う", "アリガトウ", listOf("有" to "ア", "り" to "リ", "難" to "ガト", "う" to "ウ")),
        Triple("ありがとう", "アリガトウ", listOf("ありがとう" to "アリガトウ")),
        Triple("カ月", "かげつ", listOf("カ" to "か", "月" to "げつ")),
        Triple("序ノ口", "じょのくち", listOf("序" to "じょ", "ノ" to "の", "口" to "くち")),
        Triple("スズメの涙", "すずめのなみだ", listOf("スズメ" to "すずめ", "の" to "", "涙" to "なみだ")),
        Triple("二カ所", "にかしょ", listOf("二" to "に", "カ" to "か", "所" to "しょ")),
        Triple("八ツ橋", "やつはし", listOf("八" to "や", "ツ" to "つ", "橋" to "はし")),
        Triple("八ツ橋", "やつはし", listOf("八" to "や", "ツ" to "つ", "橋" to "はし")),
        Triple("一カ月", "いっかげつ", listOf("一" to "いっ", "カ" to "か", "月" to "げつ")),
        Triple("一カ所", "いっかしょ", listOf("一" to "いっ", "カ" to "か", "所" to "しょ")),
        Triple("カ所", "かしょ", listOf("カ" to "か", "所" to "しょ")),
        Triple("数カ月", "すうかげつ", listOf("数" to "すう", "カ" to "か", "月" to "げつ")),
        Triple("くノ一", "くのいち", listOf("く" to "", "ノ" to "の", "一" to "いち")),
        Triple("くノ一", "くのいち", listOf("く" to "", "ノ" to "の", "一" to "いち")),
        Triple("数カ国", "すうかこく", listOf("数" to "すう", "カ" to "か", "国" to "こく")),
        Triple("数カ所", "すうかしょ", listOf("数" to "すう", "カ" to "か", "所" to "しょ")),
        Triple("壇ノ浦の戦い", "だんのうらのたたかい", listOf("壇" to "だん", "ノ" to "の", "浦" to "うら", "の" to "", "戦" to "たたか", "い" to "")),
        Triple("壇ノ浦の戦", "だんのうらのたたかい", listOf("壇" to "だん", "ノ" to "の", "浦" to "うら", "の" to "", "戦" to "たたかい")),
        Triple("序ノ口格", "じょのくちかく", listOf("序" to "じょ", "ノ" to "の", "口格" to "くちかく")),
        Triple("二カ国語", "にかこくご", listOf("二" to "に", "カ" to "か", "国語" to "こくご")),
        Triple("カ国", "かこく", listOf("カ" to "か", "国" to "こく")),
        Triple("カ国語", "かこくご", listOf("カ" to "か", "国語" to "こくご")),
        Triple("壇ノ浦の合戦", "だんのうらのかっせん", listOf("壇" to "だん", "ノ" to "の", "浦" to "うら", "の" to "", "合戦" to "かっせん")),
        Triple("一タ偏", "いちたへん", listOf("一" to "いち", "タ" to "た", "偏" to "へん")),
        Triple("ル又", "るまた", listOf("ル" to "る", "又" to "また")),
        Triple("ノ木偏", "のぎへん", listOf("ノ" to "の", "木偏" to "ぎへん")),
        Triple("一ノ貝", "いちのかい", listOf("一" to "いち", "ノ" to "の", "貝" to "かい")),
        Triple("虎ノ門事件", "とらのもんじけん", listOf("虎" to "とら", "ノ" to "の", "門事件" to "もんじけん")),
        Triple("教育ニ関スル勅語", "きょういくにかんするちょくご", listOf("教育" to "きょういく", "ニ" to "に", "関" to "かん", "スル" to "する", "勅語" to "ちょくご")),
        Triple("二カ年", "にかねん", listOf("二" to "に", "カ" to "か", "年" to "ねん")),
        Triple("三カ年", "さんかねん", listOf("三" to "さん", "カ" to "か", "年" to "ねん")),
        Triple("四カ年", "よんかねん", listOf("四" to "よん", "カ" to "か", "年" to "ねん")),
        Triple("五カ年", "ごかねん", listOf("五" to "ご", "カ" to "か", "年" to "ねん")),
        Triple("六カ年", "ろっかねん", listOf("六" to "ろっ", "カ" to "か", "年" to "ねん")),
        Triple("七カ年", "ななかねん", listOf("七" to "なな", "カ" to "か", "年" to "ねん")),
        Triple("八カ年", "はちかねん", listOf("八" to "はち", "カ" to "か", "年" to "ねん")),
        Triple("九カ年", "きゅうかねん", listOf("九" to "きゅう", "カ" to "か", "年" to "ねん")),
        Triple("十カ年", "じゅうかねん", listOf("十" to "じゅう", "カ" to "か", "年" to "ねん")),
        Triple("鏡ノ間", "かがみのま", listOf("鏡" to "かがみ", "ノ" to "の", "間" to "ま")),
        Triple("鏡ノ間", "かがみのま", listOf("鏡" to "かがみ", "ノ" to "の", "間" to "ま")),
        Triple("ページ違反", "ぺーじいはん", listOf("ペ" to "ぺ", "ー" to "", "ジ" to "じ", "違反" to "いはん")),
        Triple("サボる", "サボル", listOf("サボ" to "", "る" to "ル")),
        Triple("シック", "シック・ビルしょうこうぐん", listOf("シック" to "シック・ビルしょうこうぐん")),
        Triple("逸らす", "そらす", listOf("逸" to "そ", "らす" to "")),
        Triple("逸らす", "そらす", listOf("逸" to "そ", "らす" to "")),
    )

    private val inflected = listOf(
        listOf("美味しい", "おいしい", "美味しかた") to listOf("美味" to "おい", "しかた" to ""),
        listOf("食べる", "たべる", "食べた") to listOf("食" to "た", "べた" to ""),
        listOf("迄に", "までに", "までに") to listOf("までに" to ""),
        listOf("行う", "おこなう", "おこなわなかった") to listOf("おこなわなかった" to ""),
        listOf("いい", "いい", "イイ") to listOf("イイ" to ""),
        listOf("否か", "いなか", "否カ") to listOf("否" to "いな", "カ" to "か"),
    )

    @Test
    fun readingsAreSplitOverTheKanjiAsYomitanSplitsThem() {
        for ((term, reading, expected) in plain) {
            assertEquals(expected.map { Japanese.Furigana(it.first, it.second) }, Japanese.distributeFurigana(term, reading), "$term $reading")
        }
        for ((input, expected) in inflected) {
            val (term, reading, source) = input
            assertEquals(expected.map { Japanese.Furigana(it.first, it.second) }, Japanese.distributeFuriganaInflected(term, reading, source), "$term $reading $source")
        }
    }

    @Test
    fun pitchFollowsTheDownstep() {
        assertEquals(listOf("きょ", "う"), Japanese.morae("きょう"))
        assertEquals(listOf(false, true, true), (0..2).map { Japanese.isMoraPitchHigh(it, 0) })
        assertEquals(listOf(true, false, false), (0..2).map { Japanese.isMoraPitchHigh(it, 1) })
        assertEquals("heiban", Japanese.pitchCategory("つかう", 0, isVerbOrAdjective = true))
        assertEquals("odaka", Japanese.pitchCategory("はな", 2, isVerbOrAdjective = false))
        assertEquals("nakadaka", Japanese.pitchCategory("あなた", 2, isVerbOrAdjective = false))
    }
}
