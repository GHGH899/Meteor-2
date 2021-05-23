/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client/).
 * Copyright (c) 2021 Meteor Development.
 */

package minegame159.meteorclient.systems.modules.render;

import minegame159.meteorclient.settings.DoubleSetting;
import minegame159.meteorclient.settings.EnumSetting;
import minegame159.meteorclient.settings.Setting;
import minegame159.meteorclient.settings.SettingGroup;
import minegame159.meteorclient.systems.modules.Categories;
import minegame159.meteorclient.systems.modules.Module;

public class HandView extends Module {

    public enum SwingMode {
        Offhand,
        Mainhand,
        None
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSwing = settings.createGroup("Swing");

    public final Setting<Double> rotationX = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-x")
            .description("The X rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.2)
            .sliderMax(0.2)
            .build()
    );

    public final Setting<Double> rotationY = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-y")
            .description("The Y rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.2)
            .sliderMax(0.2)
            .build()
    );

    public final Setting<Double> rotationZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("rotation-z")
            .description("The Z rotation of your hands.")
            .defaultValue(0.00)
            .sliderMin(-0.25)
            .sliderMax(0.25)
            .build()
    );

    public final Setting<Double> scaleX = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale-x")
            .description("The X scale of the items rendered in your hands.")
            .defaultValue(0.75)
            .sliderMin(0)
            .sliderMax(1.5)
            .build()
    );

    public final Setting<Double> scaleY = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale-y")
            .description("The Y scale of the items rendered in your hands.")
            .defaultValue(0.60)
            .sliderMin(0)
            .sliderMax(2)
            .build()
    );

    public final Setting<Double> scaleZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale-z")
            .description("The Z scale of the items rendered in your hands.")
            .defaultValue(1.00)
            .sliderMin(0)
            .sliderMax(5)
            .build()
    );

    public final Setting<Double> posX = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-x")
            .description("The X offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> posY = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-y")
            .description("The Y offset of your hands.")
            .defaultValue(0.00)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> posZ = sgGeneral.add(new DoubleSetting.Builder()
            .name("pos-z")
            .description("The Z offset of your hands.")
            .defaultValue(-0.10)
            .sliderMin(-3)
            .sliderMax(3)
            .build()
    );

    public final Setting<Double> mainSwing = sgSwing.add(new DoubleSetting.Builder()
            .name("main-swing-progress")
            .description("The swing progress of your mainhand.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    public final Setting<Double> offSwing = sgSwing.add(new DoubleSetting.Builder()
            .name("off-swing-progress")
            .description("The swing progress of your offhand.")
            .defaultValue(0)
            .sliderMin(0)
            .sliderMax(1)
            .build()
    );

    public final Setting<SwingMode> swingMode = sgSwing.add(new EnumSetting.Builder<SwingMode>()
            .name("swing-mode")
            .description("Modifies your client & server hand swinging.")
            .defaultValue(SwingMode.None)
            .build()
    );

    public HandView() {
        super(Categories.Render, "HandView", "Alters the way items are rendered in your hands.");
    }
}