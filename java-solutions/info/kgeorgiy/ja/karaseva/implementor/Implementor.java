package info.kgeorgiy.ja.karaseva.implementor;

import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Implementation for {@link JarImpler}.
 */
public class Implementor implements JarImpler {

    /**
     * {@link String} equal line separator on current system.
     */
    private final static String NEWLINE = System.lineSeparator();

    /**
     * {@link String} contains 4 whitespace.
     */
    private final static String INDENT = "    ";

    /**
     * Default constructor.
     */
    public Implementor() {
    }

    /**
     * Run method {@code implement} or {@code implementJAR} depends on arguments.
     * <p> If arguments is {@code [realizable interface] [path to save file]}
     * create java-file with implementation of realizable interface and save on the path.
     * <p> If arguments is {@code [-jar] [realizable interface] [path to save file]}
     * create jar-file with implementation of realizable interface and save on the path.
     *
     * @param args arguments of command-line. Should contains
     *             {@code [realizable interface] [path to save file]}
     *             or {@code [-jar] [realizable interface] [path to save file]}.
     */
    public static void main(final String[] args) {
        if (args == null) {
            System.err.println("Expected not null argument");
            return;
        }

        if (args.length != 2 && args.length != 3) {
            System.err.println("Expected 2 or 3 arguments");
            return;
        }

        if (Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments mustn't be null");
            return;
        }

        if (args.length == 3 && !args[0].equals("-jar")) {
            System.err.println("Unknown command: " + args[0]);
            return;
        }

        final Implementor implementor = new Implementor();
        try {
            if (args[0].equals("-jar")) {
                implementor.implementJar(Class.forName(args[1]), Paths.get(args[2]));
            } else {
                implementor.implement(Class.forName(args[0]), Paths.get(args[1]));
            }
        } catch (final ClassNotFoundException e) {
            System.err.println("Not found class: " + e.getMessage());
        } catch (final ImplerException e) {
            System.err.println("Error during generation: " + e.getMessage());
        }
    }

    /**
     * Generate java-code with implementation of interface {@code token} and save result on the {@code root}.
     * <p> Call {@code buildClass} to generate class
     * and call {@code createDirs}, {@code getPath} to get right path and save {@code .java} file on it.
     * <p> Generated class classes name will be same as classes name of the type token with {@code Impl} suffix added.
     *
     * @param token interface to create implementation for. Shouldn't be private.
     * @throws ImplerException If {@code token} is incorrect or file can't be saved in {@code root} directory.
     */
    @Override
    public void implement(final Class<?> token, final Path root) throws ImplerException {
        if (!token.isInterface()) {
            throw new ImplerException("The token(" + token.getCanonicalName() + ") must be an interface");
        }

        if (Modifier.isPrivate(token.getModifiers())) {
            throw new ImplerException("Can't implement private interface: " + token.getCanonicalName());
        }

        try (final BufferedWriter writer = Files.newBufferedWriter(createDirs(getPath(token, root, ".java")))) {
            writer.write(encodingUnicode(buildClass(token)));
        } catch (final IOException e) {
            throw new ImplerException("IO error while saving result: " + e.getMessage(), e);
        }
    }

    /**
     * Translate international symbols from {@code string} to Unicode.
     *
     * @param string {@link String} to encoding for.
     * @return string that contains {@code string} converted to Unicode.
     */
    private static String encodingUnicode(final String string) {
        return string.chars()
                .mapToObj(ch -> (ch < 128) ? Character.toString(ch) : "\\u" + String.format("%04X", ch))
                .collect(Collectors.joining());
    }


    /**
     * Create {@link String} containing implementation of the interface {@code token}.
     * <p> Calls {@code addPackage} and {@code addMethod} methods to create the correct structure java-class.
     *
     * @param token realizable interface.
     * @return string that contains java-code with implementation of {@code token}.
     */
    private static String buildClass(final Class<?> token) {
        return String.format(
                "%spublic class %sImpl implements %s {%n%s%n}",
                !token.getPackageName().isEmpty() ? "package " + token.getPackageName() + ";" + NEWLINE + NEWLINE : "",
                token.getSimpleName(),
                token.getCanonicalName(),
                Arrays.stream(token.getMethods()).map(Implementor::addMethod).collect(Collectors.joining(NEWLINE))
        );
    }

    /**
     * Create {@link String} containing the java-code of current method.
     * <p>Call methods: {@code addMethodTitle}, {@code addMethodBody} to create java-code of the method with correct structure.
     *
     * @param method {@link Method} for which java-code is generated.
     * @return string that contains java-code of the current method.
     */
    private static String addMethod(final Method method) {
        return INDENT + addMethodTitle(method) + addMethodBody(method.getReturnType());
    }

    /**
     * Create title of current method.
     * <p>Add modifiers except abstract and transient. Also add return type and name of the method.
     * Call method {@code addParameters} to add parameters.
     *
     * @param method {@link Method} for which title is generated.
     * @return string that contains title of the current method.
     */
    private static String addMethodTitle(final Method method) {
        // :NOTE: Modifier.TRANSIENT
        return Modifier.toString((method.getModifiers() & (~(Modifier.ABSTRACT | Modifier.TRANSIENT)))) + " "
                + method.getReturnType().getCanonicalName() + " " + method.getName()
                + addParameters(method.getParameters());
    }

    /**
     * Create {@link String} that contains parameters of current method.
     *
     * @param parameters array of parameters of the current method.
     * @return string that contains parameters of the current method listed in comma-separated parentheses.
     */
    private static String addParameters(final Parameter[] parameters) {
        return "(" +
                Arrays.stream(parameters)
                        .map(parameter -> parameter.getType().getCanonicalName() + " " + parameter.getName())
                        .collect(Collectors.joining(", "))
                + ")";
    }

    /**
     * Create {@link String} that contains body of current method.
     * <p> In this code method ignore all parameters and return default value of its return type.
     *
     * @param returnType return type of current method.
     * @return string that contains java-code body of the current method.
     */
    private static String addMethodBody(final Class<?> returnType) {
        return "{"
                +
                ((returnType != void.class) ?
                        NEWLINE + INDENT + INDENT + "return "
                                + (!returnType.isPrimitive() ? "null" :
                                    returnType == boolean.class ? "false" :
                                            "0")
                                + ";"
                        : "")
                + NEWLINE + INDENT + "}" + NEWLINE + NEWLINE;
    }

    /**
     * Return new path, append to {@code path} package and file name.
     *
     * @param token  realizable interface.
     * @param path   path for saving file.
     * @param suffix {@link String} contains suffix ({@code .java} or {@code .class}) of the file to which we get the path.
     * @return {@code path} completed package and class name.
     * @throws ImplerException if {@code path} is invalid.
     */
    private static Path getPath(final Class<?> token, final Path path, final String suffix) throws ImplerException {
        try {
            return path.resolve(token.getPackageName().replace('.', File.separatorChar))
                    .resolve(token.getSimpleName() + "Impl" + suffix);
        } catch (final InvalidPathException e) {
            throw new ImplerException("Path is invalid", e);
        }
    }

    /**
     * Create missing directories on the {@code path}.
     *
     * @param path path to creating directories
     * @return path for saving file.
     * @throws ImplerException If can't create directories.
     */
    private static Path createDirs(final Path path) throws ImplerException {
        if (path.getParent() != null) {
            try {
                Files.createDirectories(path.getParent());
            } catch (final IOException e) {
                throw new ImplerException("Can't create directories: " + e.getMessage(), e);
            }
        }
        return path;
    }

    /**
     * Generate java-code with implementation of interface {@code token} and save result in jar-file by {@code jarFile}.
     * <p> Call {@code implement} to generate class and save result in {@code .java} file in temporary directory.
     * Then compile it and add in {@code .jar} file which saved on the {@code jarFile}.
     *
     * @param token interface to create implementation for. Shouldn't be private.
     * @throws ImplerException If can't create temporary directory, or compile error,
     *                         or error during method {@code implement}, or error during creating {@code .jar}.
     */
    @Override
    public void implementJar(final Class<?> token, final Path jarFile) throws ImplerException {
        final Path temp;
        try {
            temp = Files.createTempDirectory(jarFile.toAbsolutePath().getParent(), "temp");
        } catch (final IOException e) {
            throw new ImplerException("Error during creating temporary directory: " + e.getMessage(), e);
        }

        implement(token, temp);
        compile(token, temp);

        try (final JarOutputStream writer = new JarOutputStream(Files.newOutputStream(jarFile), getManifest())) {
            writer.putNextEntry(new ZipEntry(token.getPackageName().replace('.', '/') + "/" + token.getSimpleName() + "Impl.class"));
            Files.copy(getPath(token, temp, ".class"), writer);
        } catch (final InvalidPathException e) {
            throw new ImplerException("Path is invalid: " + e.getMessage(), e);
        } catch (final IOException e) {
            throw new ImplerException("IO error: " + e.getMessage(), e);
        }
    }

    private static void compile(final Class<?> token, final Path temp) throws ImplerException {
        final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Can't create java compiler");
        }
        final String classPath;
        try {
            classPath = Path.of(token.getProtectionDomain().getCodeSource().getLocation().toURI()).toString();
        } catch (final URISyntaxException e) {
            throw new ImplerException("Can't get URI", e);
        }
        final String[] args = new String[]{"-cp", classPath, getPath(token, temp, ".java").toString()};
        if (compiler.run(null, null, null, args) != 0) {
            throw new ImplerException("Compile error");
        }
    }

    /**
     * Create {@link Manifest} with attributes version = 1.0 and vendor = Karaseva Ekaterina.
     *
     * @return {@link Manifest} with the specified attributes.
     */
    private static Manifest getManifest() {
        final Manifest manifest = new Manifest();
        final Attributes attributes = manifest.getMainAttributes();
        attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
        attributes.put(Attributes.Name.IMPLEMENTATION_VENDOR, "Karaseva Ekaterina");
        return manifest;
    }
}
