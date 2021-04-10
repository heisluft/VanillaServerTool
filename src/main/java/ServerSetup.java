import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Optional;

public class ServerSetup {
  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.out.println("FreddysServerSetup version 1.0.0\nA simple vanilla Server Download tool");
      System.out.println("Usage: setupServer <directory> [version]");
      System.out.println("version supports 'latest' and 'latest-snapshot', defaults to latest if unset");
      return;
    }
    Path dir = Path.of(args[0]);
    if (!Files.isDirectory(dir)) {
      System.err.println(args[0] + " is not a directory");
      return;
    }
    String request = args.length > 1 ? args[1] : "latest";
    InputStreamReader manifestReader = new InputStreamReader(new URL("https://launchermeta.mojang.com/mc/game/version_manifest.json").openConnection().getInputStream());
    JSONObject versionManifest = (JSONObject) new JSONParser().parse(manifestReader);
    manifestReader.close();
    String dlV = ("latest".equals(request) || "latest-snapshot".equals(request)) ? versionManifest.getObject("latest").getString(request.equals("latest") ? "release" : "snapshot") : request;
    Optional<JSONObject> queriedVersion = versionManifest.getArray("versions").stream().map(JSONObject.class::cast).filter(o -> o.getString("id").equals(dlV)).findFirst();
    if (queriedVersion.isEmpty()) {
      System.out.println("Version '" + dlV + "' could not be found.");
      return;
    }
    System.out.println("Downloading server version " + dlV);
    try (InputStreamReader versionReader = new InputStreamReader(new URL(queriedVersion.get().getString("url")).openStream())) {
      JSONObject serverDownload = ((JSONObject) new JSONParser().parse(versionReader)).getObject("downloads").getObject("server");
      try (InputStream serverJarInputStream = new URL(serverDownload.getString("url")).openStream()) {
        byte[] serverJarBytes = serverJarInputStream.readAllBytes();
        StringBuilder calcSha1 = new StringBuilder();
        byte[] sha1Bytes = MessageDigest.getInstance("SHA-1").digest(serverJarBytes);
        for (byte b : sha1Bytes) calcSha1.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        if (!calcSha1.toString().equals(serverDownload.getString("sha1"))) {
          System.err.println("Download failed, checksum mismatch. Expected '" + serverDownload.getString("sha1") + "' but got '" + calcSha1.toString() + "'. Please try again");
          return;
        }
        Files.write(dir.resolve("server.jar"), serverJarBytes);
        Files.write(dir.resolve("eula.txt"), "eula=true".getBytes(StandardCharsets.UTF_8));
        System.out.println("All done.");
      }
    }

  }
}
