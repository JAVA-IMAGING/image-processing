package org.ccode.asset.ctn.logging;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LoggerBuilder {
	private final Logger logger;
	private static final Formatter DEFAULT_FORMATTER = new LoggingFormatter();
	private static FileHandler DEFAULT_FILE_HANDLER;

	public LoggerBuilder(String name) {
		logger = Logger.getLogger(name);
	}

	public LoggerBuilder useParentHandlers(boolean value) {
		logger.setUseParentHandlers(value);
		return this;
	}

	public LoggerBuilder addConsoleHandler() {
		return addConsoleHandler(DEFAULT_FORMATTER);
	}

	public LoggerBuilder addConsoleHandler(Formatter formatter) {
		Handler handler = new ConsoleHandler();

		logger.addHandler(handler);
		logger.setLevel(Level.ALL); // FINER
		handler.setLevel(Level.ALL);
		handler.setFormatter(new LoggingFormatter());
		return this;
	}

	public LoggerBuilder addFileHandler() {
		return addFileHandler(DEFAULT_FILE_HANDLER, DEFAULT_FORMATTER);
	}

	public LoggerBuilder addFileHandler(String filename) {
		return addFileHandler(filename, DEFAULT_FORMATTER);
	}

	public LoggerBuilder addFileHandler(String filename, Formatter formatter) {
		FileHandler handler;
		try {
			handler = new FileHandler(filename);
			return addFileHandler(handler, formatter);
		} catch (IOException e) {
			logger.logException(e);
			return this;
		}
	}

	public LoggerBuilder addFileHandler(FileHandler handler, Formatter formatter) {
		logger.addHandler(handler);
		logger.setLevel(Level.ALL); // FINER
		handler.setLevel(Level.ALL);
		handler.setFormatter(formatter);
		return this;
	}

	public Logger build() {
		return logger;
	}

	public static Logger defaultLogger(Class _class) { return defaultLogger(_class.getName()); }
	public static Logger defaultLogger(String name) {// TODO: Fix the logs not writing to the files
		if (DEFAULT_FILE_HANDLER == null) {
			try {
				File directory = getDefaultLogLocation(name);
				System.out.println("The log directory is: " + directory.getAbsolutePath());
				DEFAULT_FILE_HANDLER = new FileHandler(String.format("%s%s%s.log", directory.getAbsolutePath(),
						File.separator,
						LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss")))); // TODO: Initialize the DEFAULT_FILE_HANDLER in case an IOException happens.
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return new LoggerBuilder(name)
				.useParentHandlers(false)
				.addFileHandler()
				.addConsoleHandler()
				.build();
	}

	public static File getDefaultLogLocation(String name) throws IOException {
		Pattern packageRegex = Pattern.compile("org.ccode.asset.ctn.(\\w+)\\..+");
		Matcher match = packageRegex.matcher(name);
		String dir;

		if (!match.matches()) {
			dir = "./logs/"; // TODO: Make this directory be within each project's folder.
		} else {
			dir = String.format("./logs/%s/", match.group(1));
		}

		File directory = new File(dir);
		if (!directory.isDirectory()) {
			if (!directory.mkdirs()) {
				throw new IOException(String.format("Could not create directory `%s` for the log files.", dir));
			}
		}
		return directory;
	}
}
