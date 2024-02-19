package com.ebicep.warlords.database.repositories.player.pojos.interception.classes;


import com.ebicep.warlords.database.repositories.player.pojos.StatsWarlordsSpecs;
import com.ebicep.warlords.database.repositories.player.pojos.interception.DatabaseBaseInterception;

public class DatabaseArcanistInterception implements StatsWarlordsSpecs<DatabaseBaseInterception> {

    private DatabaseBaseInterception conjurer = new DatabaseBaseInterception();
    private DatabaseBaseInterception sentinel = new DatabaseBaseInterception();
    private DatabaseBaseInterception luminary = new DatabaseBaseInterception();

    public DatabaseArcanistInterception() {
        super();
    }

    @Override
    public DatabaseBaseInterception[] getSpecs() {
        return new DatabaseBaseInterception[]{conjurer, sentinel, luminary};
    }


    public DatabaseBaseInterception getConjurer() {
        return conjurer;
    }

    public DatabaseBaseInterception getSentinel() {
        return sentinel;
    }

    public DatabaseBaseInterception getLuminary() {
        return luminary;
    }

}
