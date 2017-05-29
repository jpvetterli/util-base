package ch.agent.util.ioc;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ch.agent.util.base.Misc;

/**
 * A directed acyclic graph is used to handle module dependencies in a
 * container. Nodes in the DAG are generic. It is VERY important to use
 * immutable objects like String or Integer for the actual type, because the
 * objects are used as keys in the algorithm.
 * 
 * @param <T>
 *            the type of nodes in the DAG
 */
public class DAG<T> {

	private interface Node<T> {
		/**
		 * Get the node payload.
		 * 
		 * @return a string
		 */
		public T getPayload();

		/**
		 * Return the set of nodes with a direct link from this node.
		 * 
		 * @return a set of nodes
		 */
		public Set<Node<T>> getLinks();

		/**
		 * Add a node pointed to by a link from this node. The link can mean,
		 * for example, that this node directly depends on the one being added.
		 * 
		 * @param n
		 *            a node
		 * @return true if it is a new link
		 */
		public boolean addLink(Node<T> n);

		/**
		 * Set the node as being visited.
		 * 
		 * @param visited
		 *            true or false
		 */
		public void setVisited(boolean visited);

		/**
		 * Test if the node is being visited.
		 * 
		 * @return true if the node is being visited
		 */
		public boolean isVisited();
	}

	private class DAGNode implements Node<T> {

		private final T payload;
		private Set<Node<T>> links;
		private boolean visited;

		public DAGNode(T payload) {
			Misc.nullIllegal(payload, "payload null");
			this.payload = payload;
			links = new LinkedHashSet<Node<T>>();
		}

		@Override
		public T getPayload() {
			return payload;
		}

		@Override
		public Set<Node<T>> getLinks() {
			return links;
		}

		@Override
		public boolean addLink(Node<T> n) {
			return links.add(n);
		}

		@Override
		public void setVisited(boolean mark) {
			this.visited = mark;
		}

		@Override
		public boolean isVisited() {
			return visited;
		}

		@Override
		public String toString() {
			return getPayload().toString();
		}
	}

	private Map<T, Node<T>> nodes; // key is node payload
	private int bugDetector;

	/**
	 * Constructor.
	 */
	public DAG() {
		nodes = new LinkedHashMap<T, Node<T>>();
	}

	/**
	 * Add nodes to the DAG.
	 * 
	 * @param nodes
	 *            zero or more nodes
	 * @throws IllegalArgumentException
	 *             if there is duplicate node in the input
	 */
	public void add(T... nodes) {
		for (T node : nodes) {
			add(new DAGNode(node));
		}
	}

	/**
	 * Add nodes to the DAG.
	 * 
	 * @param nodes
	 *            a collection of nodes
	 * @throws IllegalArgumentException
	 *             if there is duplicate node in the input
	 */
	public void add(Collection<T> nodes) {
		for (T node : nodes) {
			add(new DAGNode(node));
		}
	}

	/**
	 * Add links to an existing node. All nodes must already exist.
	 * 
	 * @param node
	 *            the node where the link starts
	 * @param links
	 *            zero or more nodes where the links end
	 * @throws IllegalArgumentException
	 *             if a node does not exist
	 */
	public void addLinks(T node, T... links) {
		Node<T> start = get(node);
		for (T link : links) {
			Node<T> end = get(link);
			start.addLink(end);
		}
	}

	/**
	 * Add a node to the DAG.
	 * 
	 * @param n
	 *            a node
	 * @throws IllegalArgumentException
	 *             if there is already a node with this payload in the DAG
	 */
	private boolean add(Node<T> n) {
		if (nodes.containsKey(n.getPayload()))
			throw new IllegalArgumentException("duplicate: " + n);
		return nodes.put(n.getPayload(), n) == null;
	}

	/**
	 * Get a node.
	 * 
	 * @param payload
	 *            the node payload
	 * @return the node
	 * @throws IllegalArgumentException
	 *             if there is no such node in the DAG
	 */
	private Node<T> get(T payload) {
		Node<T> n = nodes.get(payload);
		if (n == null)
			throw new IllegalArgumentException("not found: " + payload);
		return n;
	}

	/**
	 * Generate a list of node payloads with no forward links. This list can be
	 * interpreted as a list where a node does not depend directly or indirectly
	 * on any node not yet seen on the list.
	 * 
	 * @return a list of nodes (node payloads, actually)
	 * @throws IllegalArgumentException
	 *             if there is a cycle
	 */
	public List<T> sort() {
		bugDetector = 0;
		List<T> result = new ArrayList<T>();
		Set<Node<T>> work = new LinkedHashSet<Node<T>>(nodes.values());
		while (work.size() > 0) {
			visit(work.iterator().next(), work, result);
		}
		return result;
	}

	private void visit(Node<T> n, Set<Node<T>> work, List<T> result) {
		if (bugDetector++ > 1000000)
			throw new RuntimeException("bug found");
		if (n.isVisited())
			throw new IllegalArgumentException("cycle: " + n);
		if (work.contains(n)) {
			n.setVisited(true);
			for (Node<T> linked : n.getLinks()) {
				visit(linked, work, result);
			}
			n.setVisited(false);
			work.remove(n);
			result.add(n.getPayload());
		}
	}

	/**
	 * Print the DAG.
	 * 
	 * @param out
	 *            the output used for printing
	 */
	public void print(PrintStream out) {
		StringBuilder b = new StringBuilder();
		for (Node<T> n : nodes.values()) {
			b.setLength(0);
			for (Node<T> l : n.getLinks()) {
				if (b.length() == 0)
					b.append(": ");
				else
					b.append(" ");
				b.append(l);
			}
			out.println(n + b.toString());
		}
	}

}
