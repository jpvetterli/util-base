package ch.agent.util.ioc;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Before;
import org.junit.Test;

import ch.agent.util.args.Args;

public class Demo {

	private static final boolean DEBUG = false;

	public static class Hello extends AbstractModule<Object> {

		private Time time = null;
		private String greet = "hello";

		public Hello(String name) {
			super(name);
			addCommands();
		}

		@Override
		public void defineParameters(Args config) {
			super.defineParameters(config);
			config.def("greet");
		}

		@Override
		public void configure(Args config) {
			super.configure(config);
			greet = config.get("greet");
		}

		@Override
		public boolean add(Module<?> module) {
			if (module instanceof Time)
				time = (Time) module;
			return super.add(module);
		}

		private void addCommands() {
			add("say", new AbstractCommand<Object>() {
				@Override
				public void defineParameters(Args parameters) {
					parameters.def("").repeatable();
				}
				@Override
				public void execute(Args parameters) throws Exception {
					for (String any : parameters.split("")) {
						System.out.println(String.format("%s %s [%s]", greet, any, time.datetime()));
					}
				}
			});
		}
	}

	public static class Time extends AbstractModule<Object> {

		private DateFormat format = new SimpleDateFormat();

		public Time(String name) {
			super(name);
		}

		@Override
		public void defineParameters(Args config) {
			super.defineParameters(config);
			config.def("format");
		}

		@Override
		public void configure(Args config) {
			super.configure(config);
			format = new SimpleDateFormat(config.get("format"));
		}

		public String datetime(Date d) {
			return (format.format(d));
		}

		public String datetime() {
			return datetime(new Date(System.currentTimeMillis()));
		}

	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void demo1() {
		LogBuffer log = new LogBuffer();
		log.capture();
		Container c = new Container();
		try {
			c.run(new String[] { 
				"$greeting = Hoi $subject = [zäme [You all!]] ", 
				"$greeting = hello $subject = world ", 
				"$timeconf= [format=[yyyy-MM-dd HH:mm:ss]] ", 
				String.format("module=[name = hello config=[greet=$$greeting] require=time class=%s]", Hello.class.getName()), 
				String.format("module=[name = time config=[$$timeconf] class=%s]", Time.class.getName()), 
				"exec=[hello.say=[[$$subject] [you all] []=[all of you] [one more here]] ",
				" hello.say=[[the last one]]]",
				});
			c.shutdown();
			log.reset();
			String logged = log.toString();
			if (DEBUG)
				System.err.println(logged);
			int i1 = logged.indexOf("Hoi zäme [You all!]");
			int i2 = logged.indexOf("Hoi you all");
			int i3 = logged.indexOf("Hoi all of you");
			int i4 = logged.indexOf("Hoi one more here");
			assertTrue("phrase 1", i1 >= 0);
			assertTrue("phrase 2", i2 >= i1);
			assertTrue("phrase 3", i3 >= i2);
			assertTrue("phrase 4", i4 >= i3);
		} catch (Exception e) {
			log.reset();
			e.printStackTrace();
			fail("unexpected exception");
		}
	}

}
