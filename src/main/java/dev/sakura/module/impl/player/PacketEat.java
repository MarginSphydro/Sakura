package dev.sakura.module.impl.player;

import dev.sakura.events.packet.PacketEvent;
import dev.sakura.events.type.EventType;
import dev.sakura.module.Category;
import dev.sakura.module.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;

public class PacketEat extends Module {
	public PacketEat() {
		super("PacketEat", Category.Player);
	}

	@EventHandler
	public void onPacket(PacketEvent event) {
        if (event.getType() != EventType.SEND) return;
        if (event.getPacket() instanceof PlayerActionC2SPacket packet && packet.getAction() == PlayerActionC2SPacket.Action.RELEASE_USE_ITEM && mc.player.getActiveItem().get(DataComponentTypes.FOOD) != null) {
            event.cancel();
        }
	}
}