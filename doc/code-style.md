# Code Style

## Formatting

### General

The applicable formatting settings are included in the project settings. The description below outlines the most
important elements of the code style in use.

### File Structure

A source file consists of, in order:

1. Package statement
1. Import statements
1. Exactly one top-level class

Exactly one blank line separates each section that is present.

### Line Length

The maximum line length shall be 120 characters. Exceeding lines are not permitted.

### Indentation

Block indentation shall be made using tabs exclusively. Spaces are not acceptable, except for use in Javadoc.

### Braces

Classes and method definitions shall use braces on separate lines. Inside method bodies, opening braces shall be placed
on the same line as the preceding statement.

```java
public class Foobar
{
	public Foobar()
	{
		try {
			if (foo == bar) {
				// ...
			} else {
				// ...
			}
		} catch (IOException e) {
			// ...
		} finally {
			// ...
		}
	}
}
```

There shall never be a blank line between an opening brace and before a closing brace.

### Extends/Implements/Throws

The `extends`, `implements` and `throws` keywords shall always be placed on a separate line with one level of
indentation.

```java
import java.io.IOException;

public class SomeTask
	extends DefaultTask
	implements Whatever
{
	public void foobar()
		throws IOException
	{
		// ...
	}
}
```

### Finals

The `final` keyword shall only be used for preventing classes from being extended and for member fields. More
specifically, it shall not be used for parameters and local variables.

### Javadoc

`public` classes, field and methods shall be documented.

Empty tags (`@param`, `@return`, `@throws`) are not permitted.

## Implementation

### `null` Avoidance

As a general rule, `null` shall be avoided. Unless required by an (external) API being consumed a method shall never
return `null`. Use `Option` to indicate the possibility of a value that might not exist.

### Comments

Generally, a comment indicates a lack of expressiveness. Before writing a comment, a refactoring shall be attempted to
overcome the need for a comment. Only if something is not really obvious, comments may be used.
