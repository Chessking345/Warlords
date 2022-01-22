package com.ebicep.warlords.classes.rogue.specs.apothecary;

import com.ebicep.warlords.classes.abilties.*;
import com.ebicep.warlords.classes.rogue.AbstractRogue;

public class Apothecary extends AbstractRogue {

    public Apothecary() {
        super("Apothecary", 5750, 375, 0,
                new ImpalingStrike(),
                new HealingRemedy(),
                new Acupressure(),
                new RemedicChains(),
                new DrainingMiasma()
        );
    }
}
