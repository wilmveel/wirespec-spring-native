package com.example.todo;

import community.flock.wirespec.integration.spring.shared.RawJsonBody;
import community.flock.wirespec.java.Wirespec;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;

@Configuration
@ImportRuntimeHints(NativeHints.WirespecHints.class)
public class NativeHints {

	private static final String GENERATED_PACKAGE = "com/example/todo/generated";

	static class WirespecHints implements RuntimeHintsRegistrar {
		@Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			var allMembers = MemberCategory.values();

			// Wirespec library classes (from dependency JARs, can't be auto-discovered)
			hints.reflection().registerType(RawJsonBody.class, allMembers);
			hints.reflection().registerType(Wirespec.RawRequest.class, allMembers);
			hints.reflection().registerType(Wirespec.RawResponse.class, allMembers);

			// Kotlin module metadata (needed for Kotlin reflection in native image)
			hints.resources().registerPattern("META-INF/*.kotlin_module");

			// Auto-discover all generated wirespec classes
			var resolver = new PathMatchingResourcePatternResolver(classLoader);
			try {
				Resource[] resources = resolver.getResources("classpath*:" + GENERATED_PACKAGE + "/**/*.class");
				for (Resource resource : resources) {
					String path = resource.getURL().getPath();
					int idx = path.indexOf(GENERATED_PACKAGE);
					if (idx == -1) continue;
					String className = path.substring(idx, path.length() - ".class".length())
							.replace('/', '.');
					Class<?> clazz = Class.forName(className, false, classLoader);
					hints.reflection().registerType(clazz, allMembers);
				}
			} catch (IOException | ClassNotFoundException e) {
				throw new RuntimeException("Failed to discover wirespec generated classes", e);
			}
		}
	}
}
