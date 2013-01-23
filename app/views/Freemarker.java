package views;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;

import freemarker.cache.*;
import freemarker.template.*;
import freemarker.ext.beans.*;

import play.*;
import play.api.templates.Html;

public class Freemarker {

	// Freemarker configuration

	static {
		try {
			freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_SLF4J); 
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static freemarker.template.Configuration cfg = new freemarker.template.Configuration();

	static {
		cfg.setClassForTemplateLoading(Freemarker.class, "/views/");  
		if(Play.isDev()) {
			cfg.setTemplateUpdateDelay(0);
		}
	}

	// Main API

	public static Html view(String template, Arg... args) {
		Map root = new HashMap();
		for(Arg arg: args) {
			root.put(arg.name, arg.value);
		}
		ClassLoader cl = Thread.currentThread().getContextClassLoader();
		try {
			Writer out = new StringWriter();
			Thread.currentThread().setContextClassLoader(Play.application().classloader());
			root.put("Router", new BeansWrapper().getStaticModels().get("controllers.routes"));
			cfg.getTemplate(template).process(root, out);
			out.flush();  
			return Html.apply(out.toString());
		} catch(FileNotFoundException e) {
			throw new TemplateNotFoundException(template, Thread.currentThread().getStackTrace());
		} catch(freemarker.core.ParseException e) {
			throw new ExceptionInTemplate(template, e.getLineNumber(), e.getColumnNumber(), e.getMessage(), e);
		} catch(IOException ex) {
			if(ex.getCause() instanceof freemarker.core.ParseException) {
				freemarker.core.ParseException e = (freemarker.core.ParseException)ex.getCause();
				throw new ExceptionInTemplate(template, e.getLineNumber(), e.getColumnNumber(), e.getMessage(), e);
			}
			throw new RuntimeException(ex);
		} catch(TemplateException ex) {
			String ftStack = ex.getFTLInstructionStack().replace('\n', ' ');
			Integer line = Integer.parseInt(grep("line ([0-9]+)", ftStack));
			Integer position = Integer.parseInt(grep("column ([0-9]+)", ftStack));
			String tmpl = grep("in ([^ \\]]+)", ftStack);
			throw new ExceptionInTemplate(tmpl, line, position, ex.getMessage(), ex);
		} finally {
			Thread.currentThread().setContextClassLoader(cl);
		}
	}

	// Utils

	private static String grep(String regex, String str) {
		Matcher m = Pattern.compile(regex).matcher(str);
		m.find();
		return m.group(1);
	}

	// Args

	public static class Arg {
		final String name;
		final Object value;

		public Arg(String name, Object value) {
			this.name = name;
			this.value = value;
		}
	}

	public static Arg _(String name, Object value) {
		return new Arg(name, value);
	}

	// Exceptions

	public static class TemplateNotFoundException extends RuntimeException {

		private final StackTraceElement[] callerStack;

		public TemplateNotFoundException(String template, StackTraceElement[] stack) {
			super("Template " + template + " is missing.");
			callerStack = new StackTraceElement[stack.length - 2];
			System.arraycopy(stack, 2, callerStack, 0, callerStack.length);
		}

		public StackTraceElement[] getStackTrace() {
			return callerStack;
		}

	}

	public static class ExceptionInTemplate extends play.api.PlayException.ExceptionSource {

		final String template;
		final Integer line;
		final Integer position;

		public ExceptionInTemplate(String template, Integer line, Integer position, String description, Throwable cause) {
			super("Freemarker error", description, cause);
			this.template = template;
			this.line = line;
			this.position = position;
		}

		public Integer line() {
			return line;
		}
		
		public Integer position() {
			return position;
		}
		
		public String input() {
			InputStream is = Play.application().resourceAsStream("/views/" + template);
			if(is != null) {
				try {
					StringBuilder c = new StringBuilder();
					byte[] b = new byte[1024];
					int read = -1;
					while((read = is.read(b)) > 0) {
						c.append(new String(b, 0, read));
					}
					return c.toString();
				} catch(Throwable e) {
					//
				}
			} 
			return "(source missing";
		}
		
		public String sourceName() {
			return template;
		}

	}

}