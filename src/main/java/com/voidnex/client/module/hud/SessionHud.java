package com.voidnex.client.module.hud;

import com.voidnex.client.module.TextHud;
import com.voidnex.client.setting.BoolSetting;
import com.voidnex.client.util.Info;
import com.voidnex.client.util.SessionStats;

import java.util.ArrayList;
import java.util.List;

public class SessionHud extends TextHud {
    private final BoolSetting time = add(new BoolSetting("Show Playtime", "Time since you launched the game.", true));
    private final BoolSetting deaths = add(new BoolSetting("Show Deaths", "Deaths this session.", true));

    public SessionHud() {
        super("Session Stats", "Playtime and deaths for this session.", 0.01f, 0.36f);
    }

    @Override protected List<String> lines(boolean preview) {
        List<String> l = new ArrayList<>();
        if (time.on()) l.add("Session: " + Info.duration(System.currentTimeMillis() - SessionStats.START));
        if (deaths.on()) l.add("Deaths: " + SessionStats.deaths);
        return l;
    }
}
