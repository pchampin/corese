/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fr.inria.corese.rdftograph.driver;

import com.thinkaurelius.titan.core.PropertyKey;
import com.thinkaurelius.titan.core.TitanFactory;
import com.thinkaurelius.titan.core.TitanGraph;
import com.thinkaurelius.titan.core.attribute.AttributeSerializer;
import com.thinkaurelius.titan.core.schema.Mapping;
import com.thinkaurelius.titan.core.schema.SchemaAction;
import com.thinkaurelius.titan.diskstorage.ScanBuffer;
import com.thinkaurelius.titan.diskstorage.WriteBuffer;
import com.thinkaurelius.titan.graphdb.database.management.ManagementSystem;
import fr.inria.acacia.corese.api.IDatatype;
import fr.inria.acacia.corese.cg.datatype.DatatypeMap;
import fr.inria.corese.rdftograph.RdfToGraph;
import static fr.inria.corese.rdftograph.RdfToGraph.*;
import java.io.File;
import java.io.UnsupportedEncodingException;
import static java.text.MessageFormat.format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;

/**
 *
 * @author edemairy
 */
public class TitanDriver extends GdbDriver {

	static class VertexValue implements Comparable<VertexValue>, AttributeSerializer<VertexValue> {

		private String kind;
		private String value;
		private Optional<String> type;
		private Optional<String> lang;

		public VertexValue() {
		}

		public VertexValue(Value v) {
			this.kind = RdfToGraph.getKind(v);
			switch (this.kind) {
				case BNODE:
				case IRI:
					value = v.stringValue();
					type = Optional.empty();
					lang = Optional.empty();
					break;
				case LITERAL:
					Literal l = (Literal) v;
					value = l.getLabel();
					type = Optional.of(l.getDatatype().toString());
					if (l.getLanguage().isPresent()) {
						lang = Optional.of(l.getLanguage().get());
					} else {
						lang = Optional.empty();
					}
					break;
			}
		}

		public VertexValue setKind(String kind) {
			this.kind = kind;
			return this;
		}

		public VertexValue setValue(String value) {
			this.value = value;
			return this;
		}

		public VertexValue setLang(String lang) {
			this.lang = Optional.of(lang);
			return this;
		}

		public VertexValue setType(String type) {
			this.type = Optional.of(type);
			return this;
		}

		@Override
		public int compareTo(VertexValue o) {
			if (this.kind.compareTo(o.kind) != 0) {
				return this.kind.compareTo(o.kind);
			} else {
				switch (kind) {
					case BNODE:
					case IRI:
						return this.value.compareTo(o.value);
					case LITERAL:
						IDatatype data_this = lang.isPresent() ? DatatypeMap.createLiteral(value, kind, lang.get()) : DatatypeMap.createLiteral(value, kind);
						IDatatype data_o = o.lang.isPresent() ? DatatypeMap.createLiteral(o.value, o.kind, o.lang.get()) : DatatypeMap.createLiteral(o.value, o.kind);
						return data_this.compareTo(data_o);
				}
			}
			throw new IllegalArgumentException(format("{0} and {1} vertex values seem incomparable.", this, o));
		}

		@Override
		public boolean equals(Object o) {
			if (!(o instanceof VertexValue)) {
				return false;
			}
			return this.compareTo((VertexValue) o) == 0;
		}

		@Override
		public VertexValue read(ScanBuffer buffer) {
			try {
				VertexValue result = new VertexValue();
				int nbAttributes = buffer.getInt();
				result.setKind(readString(buffer));
				result.setValue(readString(buffer));
				if (result.kind.equals(LITERAL)) {
					result.setType(readString(buffer));
					if (nbAttributes == 4) {
						result.setLang(readString(buffer));
					}
				}
				return result;
			} catch (UnsupportedEncodingException ex) {
				Logger.getLogger(TitanDriver.class.getName()).log(Level.SEVERE, null, ex);
			}
			return null;
		}

		private String readString(ScanBuffer buffer) throws UnsupportedEncodingException {
			int length = buffer.getInt();
			byte[] bytes = buffer.getBytes(length);
			return new String(bytes, "UTF-8");
		}

		@Override
		public int hashCode() {
			switch (kind) {
				case BNODE:
				case IRI:
					return kind.hashCode() ^ value.hashCode();
				case LITERAL:
					if (lang.isPresent()) {
						return kind.hashCode() ^ value.hashCode() ^ type.hashCode() ^ lang.hashCode();
					} else {
						return kind.hashCode() ^ value.hashCode() ^ type.hashCode();
					}
			}
			return -1;
		}

		@Override
		public void write(WriteBuffer buffer, VertexValue attribute) {
			if (!(attribute instanceof VertexValue)) {
				throw new IllegalArgumentException();
			}
			switch (kind) {
				case BNODE:
				case IRI:
					buffer.putInt(2);
					buffer.putInt(kind.getBytes().length);
					buffer.putBytes(kind.getBytes());
					buffer.putInt(value.getBytes().length);
					buffer.putBytes(value.getBytes());
					break;
				case LITERAL:
					if (lang.isPresent()) {
						buffer.putInt(4);
					} else {
						buffer.putInt(3);
					}
					buffer.putInt(kind.getBytes().length);
					buffer.putBytes(kind.getBytes());
					buffer.putInt(value.getBytes().length);
					buffer.putBytes(value.getBytes());
					buffer.putInt(type.get().getBytes().length);
					buffer.putBytes(type.get().getBytes());
					if (lang.isPresent()) {
						buffer.putInt(lang.get().getBytes().length);
						buffer.putBytes(lang.get().getBytes());
					}
					break;
			}
		}

	}

	TitanGraph g;

	@Override
	public void openDb(String dbPath) {
		PropertiesConfiguration configuration = null;
		File confFile = new File(dbPath + "/conf.properties");
		try {
			configuration = new PropertiesConfiguration(confFile);

		} catch (ConfigurationException ex) {
			Logger.getLogger(TitanDriver.class
				.getName()).log(Level.SEVERE, null, ex);
		}
//		configuration.setProperty("storage.batch-loading", false);
		configuration.setProperty("storage.backend", "berkeleyje");
		configuration.setProperty("storage.directory", dbPath + "/db");
		configuration.setProperty("index.search.backend", "elasticsearch");
		configuration.setProperty("index.search.directory", dbPath + "/es");
		configuration.setProperty("index.search.elasticsearch.client-only", false);
		configuration.setProperty("index.search.elasticsearch.local-mode", true);
		configuration.setProperty("storage.buffer-size", 50_000);
		configuration.setProperty("ids.block-size", 1_000_000);
		try {
			configuration.save();

		} catch (ConfigurationException ex) {
			Logger.getLogger(TitanDriver.class
				.getName()).log(Level.SEVERE, null, ex);
		}

		g = TitanFactory.open(configuration);
		makeIfNotExistProperty(EDGE_P);
//		makeIfNotExistProperty(VERTEX_VALUE, VertexValue.class);
		makeIfNotExistProperty(EDGE_G);
		makeIfNotExistProperty(EDGE_S);
		makeIfNotExistProperty(EDGE_O);
		createIndexes();

	}

	void makeIfNotExistProperty(String propertyName) {
		makeIfNotExistProperty(propertyName, String.class
		);
	}

	void makeIfNotExistProperty(String propertyName, Class<?> c) {
		g.tx().rollback();
		ManagementSystem manager = (ManagementSystem) g.openManagement();
		if (!manager.containsPropertyKey(propertyName)) {
			manager.makePropertyKey(propertyName).dataType(c).make();
		}
		manager.commit();
	}

	private void createIndexes() {
		try {
			g.tx().commit();
			g.tx().rollback();
			ManagementSystem manager = (ManagementSystem) g.openManagement();
			if (!manager.containsGraphIndex("byVertexValue") && !manager.containsGraphIndex("byEdgeValue")) {
				PropertyKey vertexValue = manager.getPropertyKey(VERTEX_VALUE);
				PropertyKey kindValue = manager.getPropertyKey(KIND);

				PropertyKey graphKey = manager.getPropertyKey(EDGE_G);
				PropertyKey subjectKey = manager.getPropertyKey(EDGE_S);
				PropertyKey predicateKey = manager.getPropertyKey(EDGE_P);
				PropertyKey objectKey = manager.getPropertyKey(EDGE_O);

				manager.
					buildIndex("vertices", Vertex.class).
					addKey(vertexValue).
					buildMixedIndex("search");
				manager.
					buildIndex("allIndex", Edge.class).
					addKey(predicateKey, Mapping.STRING.asParameter()).
					addKey(subjectKey, Mapping.STRING.asParameter()).
					addKey(objectKey, Mapping.STRING.asParameter()).
					addKey(graphKey, Mapping.STRING.asParameter()).
					buildMixedIndex("search");
				manager.commit();

				String[] indexNames = {
					//					"byVertexValue", 
					//					"byEdgeValue", 
					//					"byContextValue", 
					"vertices",
					"allIndex"
				};
				for (String indexName : indexNames) {
					manager.awaitGraphIndexStatus(g, indexName).call();
					manager = (ManagementSystem) g.openManagement();
					manager.updateIndex(manager.getGraphIndex(indexName), SchemaAction.REINDEX).get();
					manager.commit();

				}
			}

		} catch (Exception ex) {
			Logger.getLogger(TitanDriver.class
				.getName()).log(Level.SEVERE, null, ex);
		}
	}

	@Override
	public void closeDb() {

		g.close();
	}

	Map<VertexValue, Object> alreadySeen = new HashMap<>();

	String nodeId(Value v) {
		StringBuilder result = new StringBuilder();
		String kind = RdfToGraph.getKind(v);
		switch (kind) {
			case IRI:
			case BNODE:
				result.append("label=" + v.stringValue() + ";");
				result.append("value=" + v.stringValue() + ";");
				result.append("kind=" + kind);
				break;
			case LITERAL:
				Literal l = (Literal) v;
				result.append("label=" + l.getLabel() + ";");
				result.append("value=" + l.getLabel() + ";");
				result.append("type=" + l.getDatatype().toString() + ";");
				result.append("kind=" + kind);
				if (l.getLanguage().isPresent()) {
					result.append("lang=" + l.getLanguage().get() + ";");
				}
				break;
		}
		return result.toString();
	}

	@Override
	public Object createNode(Value v) {
		VertexValue newVV = new VertexValue(v);
		if (alreadySeen.containsKey(newVV)) {
			return alreadySeen.get(newVV);
		} else {
			Object result = null;
			Vertex newVertex = null;
			switch (RdfToGraph.getKind(v)) {
				case IRI:
				case BNODE: {
					newVertex = g.addVertex(RDF_VERTEX_LABEL);
					newVertex.property(VERTEX_VALUE, v.stringValue());
					newVertex.property(KIND, RdfToGraph.getKind(v));
					result = newVertex.id();
					break;
				}
				case LITERAL: {
					Literal l = (Literal) v;
					newVertex = g.addVertex(RDF_VERTEX_LABEL);
					newVertex.property(VERTEX_VALUE, l.getLabel());
					newVertex.property(TYPE, l.getDatatype().toString());
					newVertex.property(KIND, RdfToGraph.getKind(v));
					if (l.getLanguage().isPresent()) {
						newVertex.property(LANG, l.getLanguage().get());
					}
					result = newVertex.id();
					break;
				}
			}
			alreadySeen.put(newVV, result);
			return result;
		}
	}

	@Override
	public Object createRelationship(Object source, Object object, String predicate, Map<String, Object> properties) {
		Object result = null;
		Vertex vSource = g.vertices(source).next();
		Vertex vObject = g.vertices(object).next();
		ArrayList<Object> p = new ArrayList<>();
		properties.keySet().stream().forEach((key) -> {
			p.add(key);
			p.add(properties.get(key));
		});
		p.add(EDGE_P);
		p.add(predicate);
		p.add(EDGE_S);
		p.add(vSource.property(VERTEX_VALUE).value());
		p.add(EDGE_O);
		p.add(vObject.property(VERTEX_VALUE).value());

		Edge e = vSource.addEdge(RDF_EDGE_LABEL, vObject, p.toArray());
		result = e.id();
		return result;
	}

	@Override
	public void commit() {
		g.tx().commit();
	}
}