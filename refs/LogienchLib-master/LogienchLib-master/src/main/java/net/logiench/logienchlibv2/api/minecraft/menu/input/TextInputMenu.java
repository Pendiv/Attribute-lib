package net.logiench.logienchlibv2.api.minecraft.menu.input;

import com.google.common.base.Strings;
import net.logiench.logienchlibv2.api.minecraft.item.SuperItemStack;
import net.logiench.logienchlibv2.api.minecraft.menu.anvil.AnvilMenu;
import net.logiench.logienchlibv2.api.minecraft.menu.inventory.util.BasicListener;
import net.logiench.logienchlibv2.api.minecraft.player.EditionCheck;
import net.logiench.logienchlibv2.api.minecraft.time.Delay;
import net.logiench.logienchlibv2.api.minecraft.time.Task;
import net.logiench.logienchlibv2.base.minecraft.menu.MenuEventException;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@SuppressWarnings("unused")
public class TextInputMenu {
	private final boolean isBedrock;
	private final Player p;
	private final CustomForm.Builder form;
	private final String title;
	private final String text;
	private String secondText = null;
	private final String defaultValue;
	private String placeHolder = "";
	private final List<Consumer<InputMenuEvent>> listeners = new ArrayList<>();

	public TextInputMenu(Player p, String title, String text) {
		this(p, title, text, null);
	}

	public TextInputMenu(Player p, @NotNull String title, @Nullable String text, @Nullable String defaultValue) {
		this.p = p;
		this.title = title;
		this.text = Strings.nullToEmpty(text);
		this.defaultValue = Strings.nullToEmpty(defaultValue);
		this.isBedrock = EditionCheck.isBedrockPlayer(p);
		if (isBedrock) {
			form = CustomForm.builder();
		} else {
			form = null;
		}
	}

	public TextInputMenu setSecondText(String text) {
		secondText = text;
		return this;
	}

	/// この要素は統合版限定です。Java版には反映されません
	public TextInputMenu setPlaceHolder(String placeHolder) {
		this.placeHolder = placeHolder;
		return this;
	}

	public TextInputMenu addListener(Consumer<InputMenuEvent> listener) {
		listeners.add(listener);
		return this;
	}

	public void send() {
		if (isBedrock) {
			sendBedrock();
		} else {
			sendJava();
		}
	}

	private void sendJava() {
		AnvilMenu menu = new AnvilMenu(title)
			.addAllListener(BasicListener.ANY_CHANGE_CANCEL)
			.addEnterListener(ev -> {
				p.playSound(p, Sound.UI_BUTTON_CLICK, 1f, 1f);
				ev.close();
				Task.on(() -> callEvents(ev.getText()));
			}).setItem(0,
				SuperItemStack.init(Material.WOODEN_PICKAXE)
					.setName(defaultValue == null ? " " : defaultValue)
					.setLore(text.isEmpty() ? null : text)
					.setDamage(secondText == null ? 0 : 1)
					.setShowOnly()
			);
		if (secondText != null) {
			List<String> second = List.of(secondText.split("\\n"));
			SuperItemStack item = SuperItemStack.init(Material.OAK_PLANKS);
			item.setShowOnly();
			item.setName(second.getFirst());
			if (second.size() > 1) {
				item.setLore(second.subList(1, second.size()));
			}
			menu.setItem(1, item);
		}
		menu.send(p);
	}

	private void sendBedrock() {
		p.closeInventory();
		if (defaultValue != null) {
			form.input(text + (secondText == null ? "" : "\n\n" + secondText), placeHolder, defaultValue);
		} else {
			form.input(text);
		}
		form.title(title).validResultHandler(result -> callEvents(result.asInput()));
		Delay.on(p, () -> FloodgateApi.getInstance().sendForm(p.getUniqueId(), form.build()), 2);
	}

	private void callEvents(String text) {
		InputMenuEvent ev = new InputMenuEvent(p, text);
		for (Consumer<InputMenuEvent> listener : listeners) {
			MenuEventException.accept(p, "TextInputMenu: EnterEvent", listener, ev);
		}
	}

}
