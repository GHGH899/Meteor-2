/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2020 Meteor Development.
 */

package minegame159.meteorclient.modules.player;

import minegame159.meteorclient.modules.Category;
import minegame159.meteorclient.modules.Module;

public class AntiCactus extends Module {
    public AntiCactus() {
        super(Category.Player, "Anti-Cactus", "Prevents you from taking damage from cacti.");
    }
}
