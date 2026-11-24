package com.voidnex.client.util;

import com.voidnex.client.core.ClientSettings;

/** Frame-rate independent exponential smoothing used for every UI animation. */
public final class Anim {
    public float value, target;
    private long last = System.nanoTime();

    public Anim(float v) { value = target = v; }

    public Anim update(float speed) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1_000_000_000f);
        last = now;
        if (!ClientSettings.animations.on()) {
            value = target;
            return this;
        }
        value += (target - value) * (1f - (float) Math.exp(-speed * dt));
        if (Math.abs(target - value) < 0.001f) value = target;
        return this;
    }
}
