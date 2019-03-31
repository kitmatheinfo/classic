package chrisliebaer.chrisliebot.command.until;

import chrisliebaer.chrisliebot.C;
import chrisliebaer.chrisliebot.abstraction.Message;
import chrisliebaer.chrisliebot.command.CommandExecutor;
import com.joestelmach.natty.DateGroup;
import com.joestelmach.natty.Parser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public class UntilCommand implements CommandExecutor {
	
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EE dd.MM.yyyy HH:mm:ss", Locale.GERMAN);
	
	private Parser parser = new Parser();
	
	@Override
	public synchronized void execute(Message m, String arg) {
		
		List<DateGroup> parse = parser.parse(arg);
		var dates = parse.stream().flatMap(in -> in.getDates().stream()).collect(Collectors.toList());
		
		if (dates.isEmpty()) {
			m.reply(C.error("Ich seh da nichts was einen Zeitpuntk darstellt"));
			return;
		}
		
		Date date = dates.get(0);
		long diff = date.getTime() - System.currentTimeMillis();
		
		String pre = diff < 0 ? "war vor" : "ist in";
		
		m.reply("Das Datum " + C.highlight(DATE_FORMAT.format(date)) + " " + pre +
				" " + C.highlight(C.durationToString(diff)));
		
	}
}
