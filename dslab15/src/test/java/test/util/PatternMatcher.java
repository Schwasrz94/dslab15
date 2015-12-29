package test.util;

import java.util.regex.Pattern;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

public class PatternMatcher extends BaseMatcher<String> {
	Pattern pattern;

	public PatternMatcher(String regex) {
		this.pattern = Pattern.compile(regex, Pattern.DOTALL);
	}

	@Override
	public boolean matches(Object item) {
		return pattern.matcher(String.valueOf(item != null ? item : "")).matches();
	}

	@Override
	public void describeTo(Description description) {
		description.appendText("matches pattern " + pattern);
	}
}
