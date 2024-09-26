package org.ccode.asset.ctn.logging;

import java.util.ArrayList;
import java.util.List;
import java.util.MissingResourceException;
import java.util.logging.Handler;
import java.util.logging.Level;

public class Logger extends java.util.logging.Logger {
	private static final List<Logger> _allLoggers = new ArrayList<>();
	private java.util.logging.Logger base;
	public static final Logger Logger = LoggerBuilder.defaultLogger(Logger.class.getName());

	/**
	 * Protected method to construct a logger for a named subsystem.
	 * <p>
	 * The logger will be initially configured with a null Level
	 * and with useParentHandlers set to true.
	 *
	 * @param name               A name for the logger.  This should
	 *                           be a dot-separated name and should normally
	 *                           be based on the package name or class name
	 *                           of the subsystem, such as java.net
	 *                           or jakarta.swing.  It may be null for anonymous Loggers.
	 * @param resourceBundleName name of ResourceBundle to be used for localizing
	 *                           messages for this logger.  May be null if none
	 *                           of the messages require localization.
	 * @throws MissingResourceException if the resourceBundleName is non-null and
	 *                                  no corresponding resource can be found.
	 */
	protected Logger(String name, String resourceBundleName) {
		super(name, resourceBundleName);
		_allLoggers.add(this);
	}

	private Logger(java.util.logging.Logger base) {
		super(base.getName(), base.getResourceBundleName());
		_allLoggers.add(this);
	}

	public void logException(Throwable throwable) {
		logException("An error has occurred.", throwable);
	}

	public void logException(String message, Throwable throwable) {
		log(Level.SEVERE, message, throwable);
	}

	public void logException(String message, Throwable throwable, Object[] params) {
		log(Level.SEVERE, message, params);
		logException("", throwable);
	}

	/**
	 * {@inheritDoc}
	 */
	public static Logger getLogger(String name) {
		return new Logger(java.util.logging.Logger.getLogger(name));
	}

	public static void close() {
		for (Logger logger : _allLoggers) {
			logger.finalize();
		}
	}

	protected void finalize() {
		for (Handler handler : getHandlers()) {
			handler.close();
		}
		_allLoggers.remove(this);
	}

	/**
	 * {@inheritDoc}
	 */
	public static Logger getLogger(String name, String resourceBundleName) {
		return new Logger(java.util.logging.Logger.getLogger(name, resourceBundleName));
	}
}
