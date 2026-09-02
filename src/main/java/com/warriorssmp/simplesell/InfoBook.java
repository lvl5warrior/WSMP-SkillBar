package com.warriorssmp.simplesell;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

public class InfoBook {

    public static ItemStack build() {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle("WSMP Shop Guide");
        meta.setAuthor("Warriors SMP");

        meta.addPage(
            "\u00a74\u00a7lWSMP Shop\n\n" +
            "\u00a77Welcome! This book explains everything the shop can do.\n\n" +
            "\u00a77Turn the page to learn more \u00bb"
        );

        meta.addPage(
            "\u00a76\u00a7lSelling Items\n\n" +
            "\u00a77/sell hand \u2013 sell what's in your hand\n\n" +
            "\u00a77/sell all \u2013 sell your whole inventory\n\n" +
            "\u00a77Or open \u00a7a/shop\u00a77 for buttons and a drag & drop bin!"
        );

        meta.addPage(
            "\u00a76\u00a7lLeveling Up\n\n" +
            "\u00a77Selling an item raises its Level (1-99). Higher levels give a small price bonus on that item.\n\n" +
            "\u00a77Check your held item's level in the action bar when you switch to it!"
        );

        meta.addPage(
            "\u00a76\u00a7lRarity Tiers\n\n" +
            "\u00a77Rare items need FEWER sold to max out than common ones \u2013 a Dragon Egg maxes way faster than Dirt!\n\n" +
            "\u00a7fCommon \u00a77\u2192 \u00a7aUncommon \u00a77\u2192 \u00a79Rare \u00a77\u2192 \u00a75Epic \u00a77\u2192 \u00a76Legendary"
        );

        meta.addPage(
            "\u00a76\u00a7lMilestones\n\n" +
            "\u00a77Reaching level \u00a7e25, 50, 75\u00a77, or \u00a7699\u00a77 on any item announces it in Discord!\n\n" +
            "\u00a77Hitting \u00a76LEVEL 99\u00a77 shouts it to the whole server!"
        );

        meta.addPage(
            "\u00a76\u00a7lBuy Back\n\n" +
            "\u00a77Sold something by accident? Open the shop's \u00a7dBuy Back\u00a77 button within \u00a7e30 minutes\u00a77 to buy it back for what you sold it for."
        );

        meta.addPage(
            "\u00a76\u00a7lBrowse Shop\n\n" +
            "\u00a77Click \u00a7bBrowse Shop Prices\u00a77 in the menu to see every item's price, rarity, and your level \u2013 like a full catalog!\n\n" +
            "\u00a77Happy selling!"
        );

        book.setItemMeta(meta);
        return book;
    }
}
