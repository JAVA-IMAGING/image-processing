package org.ccode.asset.ctn.logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.logging.Formatter;
import java.util.logging.LogRecord;

public class LoggingFormatter extends Formatter {
	private DateTimeFormatter dateTimeFormatter;

	public LoggingFormatter() {
		this.dateTimeFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
	}

	public LoggingFormatter(DateTimeFormatter dateTimeFormatter) {
		this.dateTimeFormatter = dateTimeFormatter;
	}

	@Override
	public String format(final LogRecord record) {
		// TODO: Make this function for anything that has record.getThrown (errors)
		// TODO: Get the line numbers working
		StackTraceElement[] info = Thread.currentThread().getStackTrace();
		Throwable throwable = record.getThrown();
		String errorMsg = "";

		if (throwable != null) {
			StringWriter sw = new StringWriter();
			PrintWriter pw = new PrintWriter(sw);
			throwable.printStackTrace(pw);
			errorMsg = "\n" + sw; // stack trace as a string
		}

		String message = formatMessage(record);

		return String.format(Locale.getDefault(), "%s %s - %s - Function: %s.%s - %s%s\n",
				record.getLevel(), Instant.ofEpochMilli(record.getMillis()).atZone(ZoneId.systemDefault()).format(dateTimeFormatter),
				record.getLoggerName(), record.getSourceClassName(), record.getSourceMethodName(), message, errorMsg);
	} //return Formatter("%(levelname)5s %(asctime)s.%(msecs)03d - PID: %(process)s - Thread: %(thread)d - %(name)s - Function: %(funcName)s() in %(filename)s on line %(lineno)d - %(message)s", "%m/%d/%Y %H:%M:%S")
}