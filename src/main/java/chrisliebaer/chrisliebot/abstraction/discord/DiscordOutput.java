package chrisliebaer.chrisliebot.abstraction.discord;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.ChrislieOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutput;
import chrisliebaer.chrisliebot.abstraction.PlainOutputImpl;
import chrisliebaer.chrisliebot.command.ChrislieListener;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.utils.MarkdownSanitizer;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
public class DiscordOutput implements ChrislieOutput {
	
	private MessageChannel channel;
	private EmbedBuilder embedBuilder = new EmbedBuilder();
	private DiscordPlainOutput plain = new DiscordPlainOutput(DiscordOutput::escape4Discord, DiscordFormatter::format);
	private PlainOutputImpl descrption = new PlainOutputImpl(DiscordOutput::escape4Discord, DiscordFormatter::format);
	
	private String authorName, authorUrl, authorIcon;
	
	public DiscordOutput(@NonNull MessageChannel channel) {
		this.channel = channel;
		
		// derive color from calling command listener, if any
		colorFromCallstack().ifPresent(this::color);
	}
	
	@Override
	public DiscordOutput title(String title, String url) {
		embedBuilder.setTitle(title, url);
		return this;
	}
	
	@Override
	public DiscordOutput image(String url) {
		embedBuilder.setImage(url);
		return this;
	}
	
	@Override
	public DiscordOutput thumbnail(String url) {
		embedBuilder.setThumbnail(url);
		return this;
	}
	
	@Override
	public @NonNull PlainOutput description() {
		return descrption;
	}
	
	@Override
	public DiscordOutput color(Color color) {
		embedBuilder.setColor(color);
		return this;
	}
	
	@Override
	public DiscordOutput color(int color) {
		embedBuilder.setColor(color);
		return this;
	}
	
	@Override
	public DiscordOutput author(String name) {
		authorName = name;
		embedBuilder.setAuthor(authorName, authorUrl, authorIcon);
		return this;
	}
	
	@Override
	public DiscordOutput authorUrl(String url) {
		authorUrl = url;
		embedBuilder.setAuthor(authorName, authorUrl, authorIcon);
		return this;
	}
	
	@Override
	public DiscordOutput authorIcon(String url) {
		authorIcon = url;
		embedBuilder.setAuthor(authorName, authorUrl, authorIcon);
		return this;
	}
	
	@Override
	public DiscordOutput field(String field, String value, boolean inline) {
		embedBuilder.addField(field, value, inline);
		return this;
	}
	
	@Override
	public DiscordOutput footer(String text, String iconUrl) {
		embedBuilder.setFooter(text, iconUrl);
		return this;
	}
	
	@Override
	public PlainOutput plain() {
		return plain;
	}
	
	@Override
	public PlainOutput.PlainOutputSubstituion convert() {
		return PlainOutput.dummy();
	}
	
	@Override
	public PlainOutput replace() {
		return PlainOutput.dummy();
	}
	
	@Override
	public void send() {
		embedBuilder.setDescription(descrption.string());
		MessageBuilder mb = new MessageBuilder();
		
		// block all mentions by default and apply collected mention rules from output instance
		mb.setAllowedMentions(List.of());
		plain.applyMentionRules(mb);
		
		if (!embedBuilder.isEmpty())
			mb.setEmbed(embedBuilder.build());
		
		try {
			mb.append(plain.string());
			channel.sendMessage(mb.build()).queue(m -> {}, error -> log.error("failed to send message", error));
		} catch (IllegalArgumentException e) { // if the message is too long or other undocumented shit inside jda
			log.error("failed to queue message", e);
		}
	}
	
	private static String escape4Discord(String s) {
		return MarkdownSanitizer.escape(s);
	}
	
	// highly illegal method of creating command dependant colors (please don't tell anyone)
	private static Optional<Color> colorFromCallstack() {
		var st = Thread.currentThread().getStackTrace();
		
		// now we walk up the stacktrace until we find something that implements ChrislieListener interface
		try {
			for (var e : st) {
				var clazz = Class.forName(e.getClassName());
				while (clazz != null) {
					
					// ChrislieListener is of part of invocation, so we need to exclude it
					if (clazz != ChrislieListener.class && ChrislieListener.class.isAssignableFrom(clazz))
						return Optional.of(C.hashColor(clazz.getSimpleName().getBytes(StandardCharsets.UTF_8)));
					
					// walk up to outer class
					clazz = clazz.getEnclosingClass();
				}
			}
		} catch (ClassNotFoundException e) {
			log.warn("you won't believe it but we can't even find the class that called us", e);
		}
		return Optional.empty();
	}
}
