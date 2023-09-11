package com.brunoritz.gradle.servicecontrol.generic;

import com.brunoritz.gradle.servicecontrol.launch.CommandComputer;
import io.vavr.collection.List;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Provider;

/**
 * Computes the command needed to start a generic service. The command is assembled solely based on the information
 * found in the service's definition.
 * <p>
 * The computed command will contain the following elements:
 * <ol>
 *     <li>Executable</li>
 *     <li>Program arguments</li>
 * </ol>
 */
public class GenericCommandComputer
	implements CommandComputer
{
	private final Provider<CharSequence> executable;
	private final ListProperty<CharSequence> arguments;

	public GenericCommandComputer(Provider<CharSequence> executable, ListProperty<CharSequence> arguments)
	{
		this.executable = executable;
		this.arguments = arguments;
	}

	@Override
	public List<String> compute()
	{
		List<String> command = List.<String>empty()
			.append(executable.get().toString());

		return arguments.get().stream()
			.map(CharSequence::toString)
			.reduce(command, List::append, List::appendAll);
	}
}
