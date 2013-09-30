package com.mitchellbosecke.pebble.compiler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.security.SecureClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;

import com.mitchellbosecke.pebble.template.PebbleTemplate;

public class ClassFileManager extends ForwardingJavaFileManager<JavaFileManager> {

	private static ClassFileManager instance;

	private final StandardJavaFileManager fileManager;

	/**
	 * Instance of JavaClassObject that will store the compiled bytecode of our
	 * class
	 */
	private final Map<String, ByteArrayJavaFileObject> classObjects = new HashMap<>();

	/**
	 * Will initialize the manager with the specified standard java file manager
	 * 
	 * @param standardManger
	 */
	private ClassFileManager(StandardJavaFileManager standardManager) {
		super(standardManager);
		fileManager = standardManager;
	}

	public static ClassFileManager getInstance(StandardJavaFileManager manager) {
		if (instance == null) {
			instance = new ClassFileManager(manager);
		}
		return instance;
	}

	@Override
	public ClassLoader getClassLoader(Location location) {

		return new SecureClassLoader() {
			@Override
			protected Class<?> findClass(String name) throws ClassNotFoundException {
				byte[] b = classObjects.get(name).getBytes();
				return super.defineClass(name, b, 0, b.length);
			}
		};
	}

	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling)
			throws IOException {
		if (kind == Kind.CLASS) {
			ByteArrayJavaFileObject buffer = new ByteArrayJavaFileObject(className, kind);
			classObjects.put(className, buffer);
			return buffer;
		} else {
			return super.getJavaFileForInput(location, className, kind);
		}
	}

	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<JavaFileObject.Kind> kinds,
			boolean recurse) throws IOException {

		if (packageName.startsWith(PebbleTemplate.COMPILED_PACKAGE_NAME)) {
			return new ArrayList<JavaFileObject>(classObjects.values());
		} else {
			return fileManager.list(location, packageName, kinds, recurse);
		}
	}

	@Override
	public String inferBinaryName(Location location, JavaFileObject file) {
		if (file instanceof ByteArrayJavaFileObject) {
			return ((ByteArrayJavaFileObject) file).getBinaryName();
		} else { 
			return fileManager.inferBinaryName(location, file);
		}
	}

	
}