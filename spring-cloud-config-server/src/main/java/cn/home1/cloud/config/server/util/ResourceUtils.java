package cn.home1.cloud.config.server.util;

import static com.google.common.base.Preconditions.checkArgument;
import static lombok.AccessLevel.PRIVATE;
import static org.apache.commons.io.FileUtils.copyInputStreamToFile;
import static org.apache.commons.io.FileUtils.forceMkdir;

import cn.home1.cloud.config.server.ConfigServer;

import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;

@NoArgsConstructor(access = PRIVATE)
public abstract class ResourceUtils {

    /**
     * Find resource in classpath and on filesystem, return its path or null (not found)
     *
     * @param location        start with 'classpath:' or 'file:'
     * @param outputDirectory dump resource into 'outputDirectory + location', if found in classpath
     * @return resource file path or null (not found)
     */
    @SneakyThrows
    public static String findResourceFile(final String location, final String outputDirectory) {
        final String result;

        if (location.startsWith("classpath:")) {
            // see: http://www.baeldung.com/convert-input-stream-to-a-file
            // read resource from classpath
            final String fileName = StringUtils.replaceOnce(location, "classpath:", "");
            final InputStream streamIn = ConfigServer.class.getClassLoader().getResourceAsStream(fileName);
            checkArgument(streamIn != null,
                "resource '" + location + "' not found in classpath ('" + fileName + "')");

            // write resource to filesystem
            forceMkdir(new File(outputDirectory));
            final File fileOut = new File(outputDirectory + (location.startsWith("/") ? location : "/" + location));
            copyInputStreamToFile(streamIn, fileOut);
            checkArgument(fileOut.exists() && fileOut.canRead(),
                "File '" + fileOut.getPath() + "' not found or not readable");

            result = fileOut.getPath();
        } else if (location.startsWith("file:")) {
            result = StringUtils.replaceOnce(location, "file:", "");
        } else {
            result = location;
        }

        return result;
    }
}
