package uk.gov.homeoffice.unhcr.version;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Resources;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.artifact.versioning.ComparableVersion;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

public class GitHubVersionChecker {

    static final private String GET_LATEST_VERSION_API_URL = "https://api.github.com/repos/UKHomeOffice/unhcr-erff-validation-tool/releases/latest";

    static final public String GET_LATEST_VERSION_URL = "https://github.com/UKHomeOffice/unhcr-erff-validation-tool/releases";
    static final public String VERSION_REGEX = "^(\\d+)\\.(\\d+)\\.(\\d+)$";

    //keep latest version for one day
    static private LoadingCache<String, ComparableVersion> latestVersionCache   = CacheBuilder.newBuilder()
            .refreshAfterWrite(1, TimeUnit.DAYS)
            .build(new CacheLoader<String, ComparableVersion>() {
                @Override
                public ComparableVersion load(String url) throws Exception {
                    return getLatestReleaseVersion(url);
                }
            });

    static public ComparableVersion getLatestReleaseVersionCached() throws IOException {
        try {
            return latestVersionCache.get(GET_LATEST_VERSION_API_URL);
        } catch (ExecutionException exception) {
            IOException ioException = ExceptionUtils.throwableOfType(exception, IOException.class); //unwrap exception
            if (ioException==null)
                throw new RuntimeException("Illegal exception type %s", exception);

            throw ioException;
        }
    }

    static private ComparableVersion getLatestReleaseVersion(String url) throws IOException {
        String response = Resources.toString(new URL(url), Charsets.UTF_8);

        JsonNode responseJsonNode  = new ObjectMapper().readTree(response);
        String latestReleaseTag = responseJsonNode.path("tag_name").asText();

        //clear non-digits
        latestReleaseTag = latestReleaseTag.replaceAll("[^\\d.]", "");

        if (!latestReleaseTag.matches(VERSION_REGEX))
            throw new IOException(String.format("Incorrect release tag: %s. It must match regex %s", latestReleaseTag, VERSION_REGEX));

        return new ComparableVersion(latestReleaseTag);
    }

    static public boolean checkReleaseVersionNewer() throws IOException {
        return getLatestReleaseVersionCached().compareTo(getCurrentVersion())>0;
    }

    public static ComparableVersion getCurrentVersion() {
        String implementationVersion = StringUtils.defaultString(
                GitHubVersionChecker.class.getPackage().getImplementationVersion(),
                "999.999.999");  //default for testing in IDE

        //clear non-digits
        implementationVersion = implementationVersion.replaceAll("[^\\d.]", "");

        ComparableVersion currentVersion = new ComparableVersion(implementationVersion);

        if (!currentVersion.toString().matches(VERSION_REGEX))
            throw new RuntimeException(String.format("Incorrect current version: %s. It must match regex %s", currentVersion, VERSION_REGEX));

        return currentVersion;
    }
}
