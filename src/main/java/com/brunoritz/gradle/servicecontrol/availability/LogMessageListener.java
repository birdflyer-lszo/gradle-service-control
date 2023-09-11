package com.brunoritz.gradle.servicecontrol.availability;

import org.apache.commons.io.input.Tailer;
import org.apache.commons.io.input.TailerListener;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Waits for an expected log message to appear in a file. Once that message has been spotted, a given
 * {@code AtomicBoolean} will be set to true to signal that fact.
 */
class LogMessageListener
	implements TailerListener
{
	private final String expectedLogMessage;
	private final AtomicBoolean running;

	LogMessageListener(String expectedLogMessage, AtomicBoolean running)
	{
		this.expectedLogMessage = expectedLogMessage;
		this.running = running;
	}

	@Override
	public void handle(String line)
	{
		if (line.contains(expectedLogMessage)) {
			running.set(true);
		}
	}

	@Override
	public void handle(Exception ex)
	{
		// No operation - The absence of the log message will lead to an error ultimately
	}

	@Override
	public void init(Tailer tailer)
	{
		// No operation - We need no knowledge of the tailer
	}

	@Override
	public void fileNotFound()
	{
		// No operation - Log file may not exist at start immediately
	}

	@Override
	public void fileRotated()
	{
		// No operation - Logs are not being rotated
	}
}
