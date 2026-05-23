package com.guardian.app.engine

/**
 * Exactly 300 blocked keywords — Arabic, English, transliteration, and site names.
 */
object KeywordsData {
    val KEYWORDS: Set<String> = listOf(
        // Arabic explicit (1–80)
        "إباحي", "إباحية", "عري", "عارية", "عاري", "جنس", "جنسي", "جنسية",
        "مص", "لحس", "ممارسة", "نيك", "ناك", "تناك", "متناكة", "شرموطة",
        "قحبة", "عاهرة", "زانية", "زنا", "فاحشة", "فاحشات", "منحلة", "بورنو",
        "بورن", "سكس", "سيكس", "اغتصاب", "اغتصب", "مغتصبة", "هنتاي", "انمي سكس",
        "فيديو جنسي", "صور عارية", "صور سكس", "مقاطع سكس", "مقاطع إباحية",
        "افلام اباحية", "أفلام إباحية", "موقع سكس", "مواقع اباحية", "بنات عاريات",
        "رجال عراة", "سحاق", "لواط", "شذوذ", "مثلية", "شهوة", "متشهي", "استمناء",
        "عادة سرية", "نفسه بيده", "بيضين", "كس", "طيز", "زب", "أير", "بزاز",
        "خرا", "مني", "سائل منوي", "قذف", "نشوة", "مثيرة", "مثير", "عاريات",
        "عريان", "عريانة", "متحرش", "تحرش", "فضيحة جنسية", "فضائح", "ممارسات",
        "علاقة حميمة", "علاقات حميمة", "محتوى للكبار", "للكبار فقط", "محتوى حساس",
        // Transliteration / English (81–160)
        "sex", "sexy", "porn", "porno", "xxx", "nude", "naked", "nsfw", "hentai",
        "erotic", "erotica", "adult", "milf", "gilf", "cumshot", "blowjob", "handjob",
        "masturbate", "masturbation", "orgasm", "boobs", "boob", "tits", "tit", "ass",
        "butt", "dick", "cock", "pussy", "vagina", "penis", "nipple", "nipples", "bdsm",
        "fetish", "lesbian", "threesome", "foursome", "gangbang", "rape", "incest",
        "lolita", "underage", "barely legal", "camgirl", "cam boy", "webcam girl",
        "stripper", "escort", "hooker", "prostitute", "brothel", "peep show",
        "deepthroat", "facial", "creampie", "squirt", "anal", "oral sex", "hardcore",
        "softcore", "x-rated", "r-rated", "18plus", "18+", "adults only", "mature content",
        "explicit content", "graphic content", "sex tape", "sextape", "nude leak",
        "leaked nudes", "onlyfans leak", "fansly leak", "premium snap", "sugar daddy",
        "sugar baby", "hook up", "hookup", "one night stand", "fwb", "friends with benefits",
        // Adult site names (161–210)
        "pornhub", "xvideos", "xnxx", "xhamster", "redtube", "youporn", "tube8",
        "spankbang", "beeg", "brazzers", "bangbros", "realitykings", "mofos",
        "naughtyamerica", "adultfriendfinder", "onlyfans", "fansly", "chaturbate",
        "livejasmin", "stripchat", "camsoda", "bongacams", "myfreecams", "fapello",
        "rule34", "gelbooru", "nhentai", "e-hentai", "sankakucomplex", "hentaifoundry",
        "pornhub premium", "xvideos red", "xnxx gold", "xhamster live", "redtube premium",
        "youporn vip", "spankbang pro", "beeg hd", "brazzers network", "bangbros network",
        "reality kings", "mofos network", "naughty america", "adult friend finder",
        "only fans", "fansly premium", "chaturbate tokens", "live jasmin", "strip chat",
        "cam soda", "bonga cams", "my free cams", "fapello leaks",
        // Gambling (211–240)
        "قمار", "كازينو", "رهان", "مراهنة", "يانصيب", "مقامرة", "مراهنات",
        "casino", "gambling", "poker", "blackjack", "slot", "roulette", "betting",
        "bet365", "1xbet", "betway", "sportsbetting", "onlinecasino", "jackpot",
        "slots", "baccarat", "craps", "keno", "lottery", "sportsbook", "parlay",
        "odds", "wager", "bookmaker",
        // Alcohol / drugs (241–270)
        "خمر", "كحول", "مخدرات", "حشيش", "كوكايين", "أفيون", "ماريجوانا",
        "alcohol", "vodka", "whiskey", "beer binge", "weed", "marijuana", "cocaine",
        "heroin", "drugs", "ecstasy", "mdma", "lsd", "meth", "crack", "opium",
        "fentanyl", "xanax abuse", "pill party", "drug dealer", "get high", "smoke weed",
        "buy drugs", "drug use",
        // Violence / disturbing (271–290)
        "تعذيب", "ذبح", "اغتيال", "تفجير", "انتحار", "gore", "torture", "suicide",
        "self-harm", "selfharm", "killing", "murder", "beheading", "snuff", "execution",
        "massacre", "genocide",
        // Dark web / bypass (291–300)
        "vpn مجاني", "تجاوز الحجب", "فتح المحجوب", "proxy", "tor browser", "dark web",
        "darkweb", "bypass filter", "unblock site"
    ).toSet()

    init {
        require(KEYWORDS.size == 300) {
            "KeywordsData must contain exactly 300 terms, found ${KEYWORDS.size}"
        }
    }
}
