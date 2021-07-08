package com.ebicep.warlords.classes.shaman.specs.earthwarden;

import com.ebicep.warlords.classes.abilties.*;
import com.ebicep.warlords.classes.shaman.AbstractShaman;

public class Earthwarden extends AbstractShaman {

    public Earthwarden() {
        super(5530, 355, 10,
                new EarthenSpike(),
                new Boulder(),
                new Earthliving(),
                new Chain("Chain Heal", 474, 633, 7.99f, 40, 20, 175),
                new Totem.TotemEarthwarden());
    }

}