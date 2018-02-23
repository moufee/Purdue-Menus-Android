package com.moufee.purduemenus.menus;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Index;
import android.arch.persistence.room.PrimaryKey;
import android.support.annotation.Keep;
import android.support.annotation.NonNull;

/**
 * Created by Ben on 13/08/2017.
 * Represents one favorite, as returned by the dining API
 */
@Keep
@Entity(indices = @Index("itemId"))
public class Favorite {
    @NonNull
    public final String itemName;

    @PrimaryKey
    @NonNull
    public final String favoriteId;
    @NonNull
    public final String itemId;
    @NonNull
    public final Boolean isVegetarian;

    public Favorite(String itemName, @NonNull String favoriteId, String itemId, Boolean isVegetarian) {
        this.itemName = itemName;
        this.favoriteId = favoriteId;
        this.itemId = itemId;
        this.isVegetarian = isVegetarian;
    }

    @Override
    public String toString() {
        return itemName;
    }
}
