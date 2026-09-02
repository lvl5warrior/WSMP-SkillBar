package com.warriorssmp.simplesell;

import org.bukkit.Material;

import java.util.Set;

/**
 * Best-effort rarity classification based on how items are normally obtained in survival.
 * This is a heuristic system, not an exhaustive per-item verified study - server owners
 * can correct any individual item's tier via the "rarity-overrides" section in config.yml.
 */
public class RarityClassifier {

    // Truly unique / near-impossible-to-farm items
    private static final Set<String> LEGENDARY_SET = Set.of(
            "DRAGON_EGG", "NETHER_STAR", "ENCHANTED_GOLDEN_APPLE"
    );

    // End-game / boss-tier / very rare structure loot
    private static final Set<String> EPIC_SET = Set.of(
            "NETHERITE_INGOT", "NETHERITE_SCRAP", "ELYTRA", "TOTEM_OF_UNDYING",
            "HEART_OF_THE_SEA", "WITHER_SKELETON_SKULL", "TRIAL_KEY", "OMINOUS_TRIAL_KEY",
            "ECHO_SHARD", "BREEZE_ROD", "HEAVY_CORE", "MACE", "SHULKER_SHELL",
            "NETHERITE_HORSE_ARMOR", "DISC_FRAGMENT_5", "MUSIC_DISC_PIGSTEP",
            "MUSIC_DISC_OTHERSIDE", "MUSIC_DISC_5"
    );

    // Solid mid-late game items - not everyday drops, but farmable with effort
    private static final Set<String> RARE_SET = Set.of(
            "DIAMOND", "EMERALD", "BLAZE_ROD", "ENDER_PEARL", "PHANTOM_MEMBRANE",
            "GHAST_TEAR", "ANCIENT_DEBRIS", "TRIDENT", "NAUTILUS_SHELL", "SPEAR",
            "SADDLE", "NAME_TAG", "ENCHANTED_BOOK", "END_CRYSTAL", "DRAGON_HEAD",
            "DRAGON_BREATH", "SPONGE", "WET_SPONGE", "TURTLE_HELMET", "SCUTE"
    );

    public static Rarity classify(Material material, SimpleSellPlugin plugin) {
        String name = material.name();

        // 1. Server owner override takes priority
        String override = plugin.getConfig().getString("rarity-overrides." + name);
        if (override != null) {
            try {
                return Rarity.valueOf(override.toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fall through to automatic classification if the override was misspelled
            }
        }

        // 2. Curated sets for well-known special items
        if (LEGENDARY_SET.contains(name)) return Rarity.LEGENDARY;
        if (EPIC_SET.contains(name)) return Rarity.EPIC;
        if (RARE_SET.contains(name)) return Rarity.RARE;

        // 3. Curated sets covered exact known items. Now catch entire equipment tiers by
        // their material prefix (covers every sword/pickaxe/axe/shovel/hoe/armor piece
        // automatically, including new ones like the 1.21.11 spear).
        if (name.startsWith("NETHERITE_")) return Rarity.EPIC;
        if (name.startsWith("DIAMOND_") || name.startsWith("CHAINMAIL_")) return Rarity.RARE;

        // 4. Pattern-based classification for everything else
        if (isTrivial(name)) return Rarity.TRIVIAL;
        if (isUncommon(name)) return Rarity.UNCOMMON;
        if (isCommon(name)) return Rarity.COMMON;

        // 5. Safe default for anything unrecognized
        return Rarity.COMMON;
    }

    private static boolean isTrivial(String name) {
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_PLANKS")
                || name.endsWith("_LEAVES") || name.endsWith("_SAPLING") || name.endsWith("_SLAB")
                || name.endsWith("_STAIRS") || name.endsWith("_FENCE")
                || name.equals("DIRT") || name.equals("GRASS_BLOCK") || name.equals("COARSE_DIRT")
                || name.equals("COBBLESTONE") || name.equals("STONE") || name.equals("SAND")
                || name.equals("RED_SAND") || name.equals("GRAVEL") || name.equals("NETHERRACK")
                || name.equals("ANDESITE") || name.equals("DIORITE") || name.equals("GRANITE")
                || name.equals("DEEPSLATE") || name.equals("COBBLED_DEEPSLATE") || name.equals("TUFF")
                || name.equals("SNOW") || name.equals("SNOWBALL") || name.equals("ICE")
                || name.equals("SEAGRASS") || name.equals("KELP") || name.equals("BAMBOO");
    }

    private static boolean isCommon(String name) {
        return name.endsWith("_ORE") || name.equals("COAL") || name.equals("RAW_IRON")
                || name.equals("RAW_COPPER") || name.equals("RAW_GOLD") || name.equals("ROTTEN_FLESH")
                || name.equals("BONE") || name.equals("FEATHER") || name.equals("LEATHER")
                || name.equals("GUNPOWDER") || name.equals("SPIDER_EYE") || name.equals("RABBIT_HIDE")
                || name.equals("STRING") || name.equals("WHEAT") || name.equals("BREAD")
                || name.equals("APPLE") || name.equals("CARROT") || name.equals("POTATO")
                || name.equals("BEETROOT") || name.equals("EGG") || name.equals("COD")
                || name.equals("SALMON") || name.equals("PORKCHOP") || name.equals("BEEF")
                || name.equals("CHICKEN") || name.equals("MUTTON") || name.equals("RABBIT")
                || name.startsWith("COOKED_") || name.startsWith("WOODEN_") || name.startsWith("STONE_")
                || name.startsWith("LEATHER_")
                || name.equals("SANDSTONE") || name.equals("RED_SANDSTONE");
    }

    private static boolean isUncommon(String name) {
        return name.endsWith("_INGOT") || name.equals("REDSTONE") || name.equals("LAPIS_LAZULI")
                || name.equals("GLOWSTONE_DUST") || name.equals("QUARTZ")
                || name.startsWith("PRISMARINE") || name.equals("GLOW_INK_SAC")
                || name.equals("INK_SAC") || name.equals("RABBIT_FOOT")
                || name.startsWith("IRON_") || name.startsWith("GOLDEN_")
                || name.equals("NETHER_WART") || name.equals("COCOA_BEANS")
                || name.equals("SUGAR") || name.equals("SUGAR_CANE");
    }
}
