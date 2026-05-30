// data/Set17Data.kt
// Set 17: Space Gods 게임 데이터
// [VERIFIED: patch 17.3, mobalytics + bunnymuffins + tftactics]
package com.tftcoach.advisor.data

// ── 챔피언 데이터 ─────────────────────────────────────────────────────────────
data class ChampionData(
    val cost: Int,
    val traits: List<String>,
    val damageType: DamageType,
    val isCarry: Boolean,
    val bisItems: List<String>,
    val acceptableItems: List<String> = emptyList(),
    val badItems: List<String> = emptyList(),
    val notes: String = ""
)

object Set17Data {

    val CHAMPIONS: Map<String, ChampionData> = mapOf(
        // 1코스트
        "Poppy"    to ChampionData(1, listOf("Shepherd","Voyager"), DamageType.PHYSICAL, false,
                       listOf("BrambleVest","WarmogsArmor")),
        "Veigar"   to ChampionData(1, listOf("Shepherd","Psionic"), DamageType.MAGIC, false,
                       listOf("BlueBuff","IonicSpark")),
        "Riven"    to ChampionData(1, listOf("Dark Star","Challenger"), DamageType.PHYSICAL, false,
                       listOf("SteraksCage","WarmogsArmor","LastWhisper"),
                       notes="17.3: AD+AP 스케일, Corki와 시너지"),
        "Leona"    to ChampionData(1, listOf("Arbiter","Vanguard"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","Warmogs")),
        "Zoe"      to ChampionData(1, listOf("Arbiter","Oracle"), DamageType.MAGIC, false,
                       listOf("BlueBuff","IonicSpark")),
        "Kindred"  to ChampionData(1, listOf("Primordian","Psionic"), DamageType.PHYSICAL, true,
                       listOf("GuinsoosRageblade","HandofJustice","TitansResolve")),
        "Briar"    to ChampionData(1, listOf("Anima","Brawler"), DamageType.PHYSICAL, false,
                       listOf("BrambleVest","DragonsClaw"),
                       notes="17.2 너프: Ability Damage 130→120 AD"),
        "Pyke"     to ChampionData(1, listOf("Psionic","Timebreaker"), DamageType.PHYSICAL, false,
                       listOf("EdgeofNight")),
        "Talon"    to ChampionData(1, listOf("NOVA","Challenger"), DamageType.PHYSICAL, false,
                       listOf("EdgeofNight","GuinsoosRageblade"),
                       notes="17.2 너프"),

        // 2코스트
        "Gnar"     to ChampionData(2, listOf("Shepherd","Brawler"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","BrambleVest"),
                       notes="17.2 버프"),
        "Meepsie"  to ChampionData(2, listOf("Shepherd","Voyager"), DamageType.MAGIC, false,
                       listOf("BlueBuff")),
        "Nasus"    to ChampionData(2, listOf("Oracle","Vanguard"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","WarmogsArmor")),
        "Gwen"     to ChampionData(2, listOf("Dark Star","Groovian"), DamageType.MAGIC, true,
                       listOf("GuinsoosRageblade","JeweledGauntlet","SpearofShojin")),
        "Teemo"    to ChampionData(2, listOf("Eradicator","Oracle"), DamageType.MAGIC, false,
                       listOf("IonicSpark","BlueBuff")),
        "Jinx"     to ChampionData(2, listOf("Anima","Groovian"), DamageType.PHYSICAL, true,
                       listOf("GuinsoosRageblade","RunaansHurricane","InfinityEdge"),
                       notes="17.2 버프, 17.3 6 Anima 가능"),
        "Lissandra" to ChampionData(2, listOf("Dark Star","Timebreaker"), DamageType.MAGIC, false,
                       listOf("BrambleVest","IonicSpark")),
        "LeBlanc"  to ChampionData(2, listOf("Arbiter","Channeler"), DamageType.MAGIC, true,
                       listOf("GuinsoosRageblade","SpearofShojin","NashorsTooth")),
        "Gragas"   to ChampionData(2, listOf("Brawler","Groovian"), DamageType.MAGIC, false,
                       listOf("GargoyleStoneplate","WarmogsArmor"),
                       notes="17.2 버프"),
        "TwistedFate" to ChampionData(2, listOf("Oracle","Timebreaker"), DamageType.MAGIC, false,
                       listOf("BlueBuff"),
                       notes="17.2 버프"),
        "Ezreal"   to ChampionData(2, listOf("Sniper","Eradicator"), DamageType.PHYSICAL, true,
                       listOf("LastWhisper","HandofJustice"),
                       notes="17.2 버프"),

        // 3코스트
        "Fizz"     to ChampionData(3, listOf("Shepherd","Challenger"), DamageType.PHYSICAL, false,
                       listOf("BrambleVest")),
        "Diana"    to ChampionData(3, listOf("Arbiter","Bulwark"), DamageType.MAGIC, false,
                       listOf("GargoyleStoneplate","DragonsClaw"),
                       notes="17.2 버프"),
        "Akali"    to ChampionData(3, listOf("Challenger","NOVA"), DamageType.PHYSICAL, true,
                       listOf("InfinityEdge","EdgeofNight","GuinsoosRageblade")),
        "Aurora"   to ChampionData(3, listOf("Anima","Oracle"), DamageType.MAGIC, true,
                       listOf("JeweledGauntlet","RabadonsDeathcap")),
        "Illaoi"   to ChampionData(3, listOf("Anima","Bulwark"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","WarmogsArmor")),
        "Yi"       to ChampionData(3, listOf("Psionic","Challenger"), DamageType.PHYSICAL, true,
                       listOf("EdgeofNight","GuinsoosRageblade","HandofJustice")),
        "Rhaast"   to ChampionData(3, listOf("Brawler","Marauder"), DamageType.PHYSICAL, false,
                       listOf("BrambleVest","GargoyleStoneplate")),
        "Urgot"    to ChampionData(3, listOf("Mecha","Brawler"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate")),
        "Lulu"     to ChampionData(3, listOf("Shepherd","Channeler"), DamageType.MAGIC, false,
                       listOf("BlueBuff"),
                       notes="17.2 버프"),
        "ChoGath"  to ChampionData(3, listOf("Bulwark","Marauder"), DamageType.MAGIC, false,
                       listOf("GargoyleStoneplate","WarmogsArmor","DragonsClaw"),
                       notes="17.2 버프"),
        "Milio"    to ChampionData(3, listOf("Groovian","Channeler"), DamageType.MAGIC, false,
                       listOf("BlueBuff","ArchangelsStaff"),
                       notes="17.2 버프"),
        "Graves"   to ChampionData(3, listOf("Sniper","NOVA"), DamageType.PHYSICAL, true,
                       listOf("LastWhisper","InfinityEdge"),
                       notes="17.2 조정"),
        "Samira"   to ChampionData(3, listOf("Sniper","Challenger"), DamageType.PHYSICAL, true,
                       listOf("InfinityEdge","HandofJustice"),
                       notes="17.2 너프"),
        "Kai'Sa"   to ChampionData(3, listOf("Sniper","Challenger"), DamageType.PHYSICAL, true,
                       listOf("GuinsoosRageblade","InfinityEdge"),
                       notes="17.3 너프: Missile damage 감소"),

        // 4코스트
        "Corki"    to ChampionData(4, listOf("Shepherd","Sniper"), DamageType.MIXED, true,
                       listOf("LastWhisper","InfinityEdge","HandofJustice"),
                       notes="17.2 버프"),
        "Rammus"   to ChampionData(4, listOf("Shepherd","Bulwark"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","WarmogsArmor")),
        "Xayah"    to ChampionData(4, listOf("Dark Star","Challenger"), DamageType.PHYSICAL, true,
                       listOf("GuinsoosRageblade","InfinityEdge","Bloodthirster"),
                       notes="17.3: Titanic Hydra 제한"),
        "AurelionSol" to ChampionData(4, listOf("Mecha","Channeler"), DamageType.MAGIC, true,
                       listOf("JeweledGauntlet","SpearofShojin","ArchangelsStaff")),
        "Vex"      to ChampionData(4, listOf("Psionic","Channeler"), DamageType.MAGIC, true,
                       listOf("JeweledGauntlet","RabadonsDeathcap"),
                       notes="17.2 버프"),
        "Caitlyn"  to ChampionData(4, listOf("Eradicator","Sniper"), DamageType.PHYSICAL, true,
                       listOf("LastWhisper","InfinityEdge","GuinsoosRageblade"),
                       notes="17.2 버프"),
        "Fiora"    to ChampionData(4, listOf("Anima","Arbiter"), DamageType.PHYSICAL, true,
                       listOf("InfinityEdge","EdgeofNight","SteraksCage")),
        "John"     to ChampionData(4, listOf("Groovian","Marauder"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","WarmogsArmor"),
                       notes="17.2 버프"),
        "TahmKench" to ChampionData(4, listOf("Brawler","Groovian"), DamageType.MAGIC, false,
                       listOf("GargoyleStoneplate","WarmogsArmor"),
                       notes="17.2 버프"),
        "Maokai"   to ChampionData(4, listOf("Bulwark","Marauder"), DamageType.MAGIC, false,
                       listOf("WarmogsArmor","DragonsClaw"),
                       notes="17.2 버프"),
        "MissFortune" to ChampionData(4, listOf("Flexible"), DamageType.MIXED, true,
                       listOf("GuinsoosRageblade","InfinityEdge"),
                       notes="Gun Goddess: Conduit/Challenger/Replicator 모드"),
        "Viktor"   to ChampionData(4, listOf("Psionic","Timebreaker"), DamageType.MAGIC, true,
                       listOf("JeweledGauntlet","RabadonsDeathcap"),
                       notes="17.2 너프"),
        "Morgana"  to ChampionData(4, listOf("Dark Star","Vanguard"), DamageType.MAGIC, false,
                       listOf("GargoyleStoneplate","DragonsClaw","IonicSpark"),
                       notes="17.3 리워크: 5코스트 Fighter → 4코스트 Tank"),

        // 5코스트
        "Bard"     to ChampionData(5, listOf("Shepherd","Oracle"), DamageType.MAGIC, false,
                       listOf("BlueBuff","ArchangelsStaff")),
        "BelVeth"  to ChampionData(5, listOf("Primordian","Challenger","Marauder"), DamageType.PHYSICAL, true,
                       listOf("GuinsoosRageblade","TitansResolve","HandofJustice")),
        "Yasuo"    to ChampionData(5, listOf("Arbiter","Timebreaker"), DamageType.PHYSICAL, true,
                       listOf("InfinityEdge","EdgeofNight"),
                       notes="17.2 너프"),
        "Jhin"     to ChampionData(5, listOf("Dark Star","Eradicator","Sniper"), DamageType.PHYSICAL, true,
                       listOf("InfinityEdge","LastWhisper","GuinsoosRageblade"),
                       notes="최원거리 코너 배치 필수. Dark Star 처형+Eradicator 방관+Sniper 거리 3중 시너지"),
        "Nunu"     to ChampionData(5, listOf("Brawler","Marauder"), DamageType.PHYSICAL, false,
                       listOf("GargoyleStoneplate","WarmogsArmor"),
                       notes="17.2 너프"),
        "Zed"      to ChampionData(5, listOf("NOVA","Challenger"), DamageType.PHYSICAL, true,
                       listOf("EdgeofNight","GuinsoosRageblade"),
                       notes="Hero Augment 'Invader Zed' 전용")
    )

    // ── 컴프 시그니처 [bunnymuffins patch 17.3] ────────────────────────────────
    val COMP_SIGNATURES: Map<String, List<String>> = mapOf(
        "Dark Star Jhin"     to listOf("Jhin","Xayah","Gwen","Riven","Lissandra","Morgana"),
        "Primordian Reroll"  to listOf("Kindred","BelVeth","Briar"),
        "Anima Cashout"      to listOf("Briar","Jinx","Aurora","Illaoi","Fiora"),
        "Psionic Yi"         to listOf("Yi","Kindred","Vex","Viktor","Lulu"),
        "Arbiter LeBlanc"    to listOf("LeBlanc","Diana","Leona","Zoe"),
        "Mecha Aurelion"     to listOf("AurelionSol","Urgot"),
        "Groovian"           to listOf("Milio","Jinx","Gragas","John","TahmKench"),
        "Challenger BelVeth" to listOf("BelVeth","Akali","Samira","Fiora"),
        "Shepherd Corki"     to listOf("Corki","Bard","Gnar","Fizz","Riven"),
        "NOVA Zed"           to listOf("Zed","Talon","Akali","Graves"),
        "Marauder Rhaast"    to listOf("Rhaast","ChoGath","Maokai","Nunu")
    )

    // ── 아이템 조합표 ─────────────────────────────────────────────────────────
    val ITEM_RECIPES: Map<Pair<String,String>, String> = mapOf(
        Pair("BFSword","BFSword")             to "Deathblade",
        Pair("BFSword","RecurveBow")          to "GuinsoosRageblade",
        Pair("BFSword","NeedlesslyLargeRod")  to "InfinityEdge",
        Pair("BFSword","TearoftheGoddess")    to "SpearofShojin",
        Pair("BFSword","ChainVest")           to "EdgeofNight",
        Pair("BFSword","NegatronCloak")       to "Bloodthirster",
        Pair("BFSword","GiantsBelt")          to "GiantSlayer",
        Pair("BFSword","SparringGloves")      to "Quicksilver",
        Pair("RecurveBow","RecurveBow")       to "StatikkShiv",
        Pair("RecurveBow","NeedlesslyLargeRod") to "GiantSlayer",
        Pair("RecurveBow","TearoftheGoddess") to "RunaansHurricane",
        Pair("RecurveBow","ChainVest")        to "TitansResolve",
        Pair("RecurveBow","NegatronCloak")    to "RedBuff",
        Pair("RecurveBow","GiantsBelt")       to "Morellonomicon",
        Pair("NeedlesslyLargeRod","NeedlesslyLargeRod") to "RabadonsDeathcap",
        Pair("NeedlesslyLargeRod","TearoftheGoddess")   to "ArchangelsStaff",
        Pair("NeedlesslyLargeRod","ChainVest")          to "Evenshroud",
        Pair("NeedlesslyLargeRod","NegatronCloak")      to "IonicSpark",
        Pair("NeedlesslyLargeRod","GiantsBelt")         to "Morellonomicon",
        Pair("NeedlesslyLargeRod","SparringGloves")     to "JeweledGauntlet",
        Pair("TearoftheGoddess","TearoftheGoddess")     to "BlueBuff",
        Pair("TearoftheGoddess","ChainVest")            to "Crownguard",
        Pair("TearoftheGoddess","NegatronCloak")        to "AdaptiveHelm",
        Pair("TearoftheGoddess","GiantsBelt")           to "Redemption",
        Pair("TearoftheGoddess","SparringGloves")       to "HextechGunblade",
        Pair("ChainVest","ChainVest")         to "BrambleVest",
        Pair("ChainVest","NegatronCloak")     to "GargoyleStoneplate",
        Pair("ChainVest","GiantsBelt")        to "SteraksCage",
        Pair("NegatronCloak","NegatronCloak") to "DragonsClaw",
        Pair("NegatronCloak","GiantsBelt")    to "GargoyleStoneplate",
        Pair("GiantsBelt","GiantsBelt")       to "WarmogsArmor",
        Pair("GiantsBelt","SparringGloves")   to "SteraksCage",
        Pair("SparringGloves","SparringGloves") to "ThiefsGloves",
        Pair("RecurveBow","SparringGloves")   to "ThiefsGloves"
    )

    fun findRecipe(item1: String, item2: String): String? =
        ITEM_RECIPES[Pair(item1, item2)] ?: ITEM_RECIPES[Pair(item2, item1)]

    // ── 증강 프로파일 ─────────────────────────────────────────────────────────
    data class AugmentProfile(
        val name: String,
        val category: AugCategory,
        val baseScore: Float,
        val requiresHpAbove: Int = 0,
        val requiresHpBelow: Int = 100,
        val synergyTraits: List<String> = emptyList(),
        val synergyComps: List<String> = emptyList(),
        val avoidReason: String = ""
    )

    enum class AugCategory { ECONOMY, COMBAT, ITEMS, TRAIT, TEMPO, AVOID }

    val AUGMENTS: Map<String, AugmentProfile> = mapOf(
        "Component Grab Bag"  to AugmentProfile("Component Grab Bag", AugCategory.ITEMS, 8.5f),
        "Component Grab Bag+" to AugmentProfile("Component Grab Bag+", AugCategory.ITEMS, 9.0f),
        "Buried Treasures"    to AugmentProfile("Buried Treasures", AugCategory.ITEMS, 8.0f),
        "Artifact Anvil"      to AugmentProfile("Artifact Anvil", AugCategory.ITEMS, 8.5f),
        "Grab Bag"            to AugmentProfile("Grab Bag", AugCategory.ITEMS, 8.0f),
        "Hustler"             to AugmentProfile("Hustler", AugCategory.ECONOMY, 7.5f, requiresHpAbove=40),
        "Thrill of the Hunt"  to AugmentProfile("Thrill of the Hunt", AugCategory.ECONOMY, 7.5f, requiresHpAbove=50),
        "Blue Battery"        to AugmentProfile("Blue Battery", AugCategory.ECONOMY, 6.0f),
        "Last Stand"          to AugmentProfile("Last Stand", AugCategory.COMBAT, 8.0f, requiresHpBelow=35),
        "You Have My Bow"     to AugmentProfile("You Have My Bow", AugCategory.TRAIT, 7.5f,
                                synergyTraits=listOf("Sniper"), synergyComps=listOf("Dark Star Jhin")),
        "Hold the Line"       to AugmentProfile("Hold the Line", AugCategory.COMBAT, 7.5f,
                                synergyComps=listOf("Dark Star Jhin")),
        "Gotta Go Fast"       to AugmentProfile("Gotta Go Fast", AugCategory.TEMPO, 7.5f),
        "Tri Force"           to AugmentProfile("Tri Force", AugCategory.TRAIT, 7.5f,
                                synergyTraits=listOf("Shepherd")),
        "InfiniTeam"          to AugmentProfile("InfiniTeam", AugCategory.TRAIT, 7.5f),
        "Axiom Arc"           to AugmentProfile("Axiom Arc", AugCategory.TRAIT, 7.5f,
                                synergyTraits=listOf("Channeler")),
        "Combat Training"     to AugmentProfile("Combat Training", AugCategory.COMBAT, 6.0f),
        "Electrocharge"       to AugmentProfile("Electrocharge", AugCategory.COMBAT, 6.5f),
        "Sunfire Board"       to AugmentProfile("Sunfire Board", AugCategory.TRAIT, 7.0f),
        // 기피
        "Stationary Support"  to AugmentProfile("Stationary Support", AugCategory.AVOID, 0f,
                                avoidReason="이동 불가"),
        "Escort Quest"        to AugmentProfile("Escort Quest", AugCategory.AVOID, 0f,
                                avoidReason="조건 달성 어려움"),
        "Scapegoat"           to AugmentProfile("Scapegoat", AugCategory.AVOID, 0.5f,
                                avoidReason="디버프 위험"),
        "Recombobulator"      to AugmentProfile("Recombobulator", AugCategory.AVOID, 1.0f,
                                avoidReason="랜덤성 과다")
    )

    // ── 신 정보 ───────────────────────────────────────────────────────────────
    val GOD_PROFILES: Map<String, String> = mapOf(
        "Kayle"        to "아이템 지향. 안정적. 초반 컴포넌트 부족 시 강함. [VERIFIED_OFFICIAL]",
        "Ahri"         to "골드/경제 지향. 17.3에서 Wealth blessing 추가. [VERIFIED_OFFICIAL]",
        "Aurelion Sol" to "퀘스트 조건부 강력. 순위 높을 때 유리. [INFERRED]",
        "Yasuo"        to "헥스 강화. 헥스 2개면 골드 12 제공. [INFERRED]",
        "Fiora"        to "결투 메카닉. Challenger/Duelist 컴프 유리. [INFERRED]",
        "Leona"        to "탱킹/방어 지향. [INFERRED]",
        "Nunu"         to "특수 효과. [UNCERTAIN]",
        "Zac"          to "특수 효과. [UNCERTAIN]",
        "Pengu"        to "범용 보상. 항상 컴포넌트 제공. [VERIFIED_OFFICIAL]"
    )

    // ── 레벨링 타이밍 [bunnymuffins + mobalytics Set 17] ──────────────────────
    data class LevelTiming(val targetLevel: Int, val source: String)

    val LEVEL_TIMINGS: Map<Pair<Int,Int>, LevelTiming> = mapOf(
        Pair(2,1) to LevelTiming(4, "mobalytics Set 17 leveling guide"),
        Pair(3,2) to LevelTiming(5, "mobalytics Set 17 leveling guide"),
        Pair(4,1) to LevelTiming(7, "bunnymuffins Challenger guide"),
        Pair(4,2) to LevelTiming(8, "bunnymuffins — roll on 4-2 for 4-costs"),
        Pair(5,2) to LevelTiming(9, "bunnymuffins — push 9 on 5-2"),
        Pair(5,5) to LevelTiming(9, "bunnymuffins — latest acceptable")
    )

    val LEVEL_XP: Map<Int,Int> = mapOf(
        2 to 2, 3 to 2, 4 to 6, 5 to 10,
        6 to 20, 7 to 36, 8 to 56, 9 to 80, 10 to 84
    )
}
