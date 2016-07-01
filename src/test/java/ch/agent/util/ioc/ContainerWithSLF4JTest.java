package ch.agent.util.ioc;



public class ContainerWithSLF4JTest extends ContainerTest {
	
	static {
		System.setProperty("LoggerBridgeFactory", ch.agent.util.logging.SLF4JLoggerBridgeFactory.class.getName());
	}

}
